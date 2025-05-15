/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */
package io.jans.as.server.register.ws.rs;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import io.jans.as.client.RegisterRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.json.JsonApplier;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.register.RegisterErrorResponseType;
import io.jans.as.model.register.RegisterRequestParam;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.ciba.CIBARegisterClientMetadataService;
import io.jans.as.server.service.ScopeService;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.util.StringHelper;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static io.jans.as.model.util.StringUtils.toList;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class RegisterService {

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Logger log;

    @Inject
    private ScopeService scopeService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AttributeService attributeService;

    @Inject
    private CIBARegisterClientMetadataService cibaRegisterClientMetadataService;

    public String clientScopesToString(Client client) {
        String[] scopeDns = client.getScopes();
        if (scopeDns != null) {
            String[] scopeNames = new String[scopeDns.length];
            for (int i = 0; i < scopeDns.length; i++) {
                Scope scope = scopeService.getScopeByDn(scopeDns[i]);
                scopeNames[i] = scope.getId();
            }
            return StringUtils.join(scopeNames, " ");
        }
        return null;
    }

    public Set<ResponseType> identifyResponseTypes(Collection<ResponseType> requestResponseTypes, Collection<GrantType> requestGrantTypes) {
        Set<ResponseType> result = new HashSet<>(requestResponseTypes);

        if (result.isEmpty()) { // fallback to "code", spec: If omitted, the default is that the Client will use only the code Response Type.
            result.add(ResponseType.CODE);
        }

        if (isTrue(appConfiguration.getGrantTypesAndResponseTypesAutofixEnabled())) {
            if (isTrue(appConfiguration.getClientRegDefaultToCodeFlowWithRefresh())) {
                if (result.isEmpty()) {
                    result.add(ResponseType.CODE);
                }

                if (requestGrantTypes.contains(GrantType.AUTHORIZATION_CODE)) {
                    result.add(ResponseType.CODE);
                }
            }
            if (requestGrantTypes.contains(GrantType.IMPLICIT)) {
                result.add(ResponseType.TOKEN);
            }
        }

        result.retainAll(appConfiguration.getAllResponseTypesSupported());
        return result;
    }

    public Set<GrantType> identifyGrantTypes(Collection<ResponseType> identifiedResponseTypes, Collection<GrantType> requestGrantTypes) {
        Set<GrantType> result = new HashSet<>(requestGrantTypes);

        if (result.isEmpty()) {
            result.add(GrantType.AUTHORIZATION_CODE);
        }

        if (isTrue(appConfiguration.getGrantTypesAndResponseTypesAutofixEnabled())) {
            if (isTrue(appConfiguration.getClientRegDefaultToCodeFlowWithRefresh())) {
                if (identifiedResponseTypes.contains(ResponseType.CODE)) {
                    result.add(GrantType.AUTHORIZATION_CODE);
                    result.add(GrantType.REFRESH_TOKEN);
                }
                if (result.contains(GrantType.AUTHORIZATION_CODE)) {
                    identifiedResponseTypes.add(ResponseType.CODE);
                    result.add(GrantType.REFRESH_TOKEN);
                }
            }

            if (identifiedResponseTypes.contains(ResponseType.TOKEN) || identifiedResponseTypes.contains(ResponseType.ID_TOKEN)) {
                result.add(GrantType.IMPLICIT);
            }
            if (result.contains(GrantType.IMPLICIT)) {
                identifiedResponseTypes.add(ResponseType.TOKEN);
            }
        }

        result.retainAll(appConfiguration.getGrantTypesSupported());
        result.retainAll(appConfiguration.getGrantTypesSupportedByDynamicRegistration());

        return result;
    }

    private void assignResponseTypesAndGrantTypes(Client client, RegisterRequest requestObject, boolean update) {
        final Set<ResponseType> identifiedResponseTypes = identifyResponseTypes(requestObject.getResponseTypes(), requestObject.getGrantTypes());
        final Set<GrantType> identifiedGrantTypes = identifyGrantTypes(identifiedResponseTypes, requestObject.getGrantTypes());

        final boolean isNewClient = !update;

        if (isNewClient || !requestObject.getResponseTypes().isEmpty()) {
            client.setResponseTypes(identifiedResponseTypes.toArray(new ResponseType[0]));
        }

        if (isNewClient || (isTrue(appConfiguration.getEnableClientGrantTypeUpdate()) && !requestObject.getGrantTypes().isEmpty())) {
            client.setGrantTypes(identifiedGrantTypes.toArray(new GrantType[0]));
        }

        log.trace("Updating client with responseTypes: {}, grantTypes: {}, isNewClient: {}", identifiedResponseTypes, identifiedGrantTypes, isNewClient);
    }

    @SuppressWarnings("java:S1168")
    public static String[] listAsArrayWithoutDuplicates(List<String> list) {
        List<String> result = new ArrayList<>(new HashSet<>(list)); // Remove repeated elements
        return result.toArray(new String[0]);
    }

    // yuriyz - ATTENTION : this method is used for both registration and update client metadata cases, therefore any logic here
    // will be applied for both cases.
    @SuppressWarnings("java:S3776")
    public void updateClientFromRequestObject(Client client, RegisterRequest requestObject, boolean update) throws JSONException {

        JsonApplier.getInstance().transfer(requestObject, client);
        JsonApplier.getInstance().transfer(requestObject, client.getAttributes());

        List<String> redirectUris = requestObject.getRedirectUris();
        if (redirectUris != null && !redirectUris.isEmpty()) {
            client.setRedirectUris(listAsArrayWithoutDuplicates(redirectUris));
        }

        List<String> claimsRedirectUris = requestObject.getClaimsRedirectUris();
        if (claimsRedirectUris != null && !claimsRedirectUris.isEmpty()) {
            client.setClaimRedirectUris(listAsArrayWithoutDuplicates(claimsRedirectUris));
        }

        client.setApplicationType(requestObject.getApplicationType() != null ? requestObject.getApplicationType() : ApplicationType.WEB);

        if (StringUtils.isNotBlank(requestObject.getSectorIdentifierUri())) {
            client.setSectorIdentifierUri(requestObject.getSectorIdentifierUri());
        }

        assignResponseTypesAndGrantTypes(client, requestObject, update);

        List<String> contacts = requestObject.getContacts();
        if (contacts != null && !contacts.isEmpty()) {
            client.setContacts(listAsArrayWithoutDuplicates(contacts));
        }

        List<String> authorizationDetailsTypes = requestObject.getAuthorizationDetailsTypes();
        if (authorizationDetailsTypes != null && !authorizationDetailsTypes.isEmpty()) {
            client.getAttributes().setAuthorizationDetailsTypes(authorizationDetailsTypes);
        }

        for (String key : requestObject.getClientNameLanguageTags()) {
            client.setClientNameLocalized(requestObject.getClientName(key), Locale.forLanguageTag(key));
        }
        for (String key : requestObject.getLogoUriLanguageTags()) {
            client.setLogoUriLocalized(requestObject.getLogoUri(key), Locale.forLanguageTag(key));
        }
        for (String key : requestObject.getClientUriLanguageTags()) {
            client.setClientUriLocalized(requestObject.getClientUri(key), Locale.forLanguageTag(key));
        }
        for (String key : requestObject.getPolicyUriLanguageTags()) {
            client.setPolicyUriLocalized(requestObject.getPolicyUri(key), Locale.forLanguageTag(key));
        }
        for (String key : requestObject.getTosUriLanguageTags()) {
            client.setTosUriLocalized(requestObject.getTosUri(key), Locale.forLanguageTag(key));
        }

        if (StringUtils.isNotBlank(requestObject.getJwksUri())) {
            client.setJwksUri(requestObject.getJwksUri());
        }
        if (StringUtils.isNotBlank(requestObject.getJwks())) {
            client.setJwks(requestObject.getJwks());
        }
        if (requestObject.getSubjectType() != null) {
            client.setSubjectType(requestObject.getSubjectType());
        }
        if (requestObject.getRptAsJwt() != null) {
            client.setRptAsJwt(requestObject.getRptAsJwt());
        }
        if (requestObject.getAccessTokenAsJwt() != null) {
            client.setAccessTokenAsJwt(requestObject.getAccessTokenAsJwt());
        }
        if (requestObject.getRequirePkce() != null) {
            client.getAttributes().setRequirePkce(requestObject.getRequirePkce());
        }
        if (requestObject.getTlsClientAuthSubjectDn() != null) {
            client.getAttributes().setTlsClientAuthSubjectDn(requestObject.getTlsClientAuthSubjectDn());
        }
        if (requestObject.getRedirectUrisRegex() != null) {
            client.getAttributes().setRedirectUrisRegex(requestObject.getRedirectUrisRegex());
        }
        if (requestObject.getAllowSpontaneousScopes() != null) {
            client.getAttributes().setAllowSpontaneousScopes(requestObject.getAllowSpontaneousScopes());
        }
        if (requestObject.getSpontaneousScopes() != null) {
            client.getAttributes().setSpontaneousScopes(requestObject.getSpontaneousScopes());
        }
        if (requestObject.getAdditionalAudience() != null) {
            client.getAttributes().setAdditionalAudience(requestObject.getAdditionalAudience());
        }
        if (requestObject.getSpontaneousScopeScriptDns() != null) {
            client.getAttributes().setSpontaneousScopeScriptDns(requestObject.getSpontaneousScopeScriptDns());
        }
        if (requestObject.getUpdateTokenScriptDns() != null) {
            client.getAttributes().setUpdateTokenScriptDns(requestObject.getUpdateTokenScriptDns());
        }
        if (requestObject.getPostAuthnScriptDns() != null) {
            client.getAttributes().setPostAuthnScripts(requestObject.getPostAuthnScriptDns());
        }
        if (requestObject.getTokenExchangeScriptDns() != null) {
            client.getAttributes().setTokenExchangeScripts(requestObject.getTokenExchangeScriptDns());
        }
        if (requestObject.getConsentGatheringScriptDns() != null) {
            client.getAttributes().setConsentGatheringScripts(requestObject.getConsentGatheringScriptDns());
        }
        if (requestObject.getIntrospectionScriptDns() != null) {
            client.getAttributes().setIntrospectionScripts(requestObject.getIntrospectionScriptDns());
        }
        if (requestObject.getRptClaimsScriptDns() != null) {
            client.getAttributes().setRptClaimsScripts(requestObject.getRptClaimsScriptDns());
        }
        if (requestObject.getRopcScriptDns() != null) {
            client.getAttributes().setRopcScripts(requestObject.getRopcScriptDns());
        }
        if (requestObject.getRunIntrospectionScriptBeforeJwtCreation() != null) {
            client.getAttributes().setRunIntrospectionScriptBeforeJwtCreation(requestObject.getRunIntrospectionScriptBeforeJwtCreation());
        }
        if (requestObject.getKeepClientAuthorizationAfterExpiration() != null) {
            client.getAttributes().setKeepClientAuthorizationAfterExpiration(requestObject.getKeepClientAuthorizationAfterExpiration());
        }
        if (requestObject.getAccessTokenSigningAlg() != null) {
            client.setAccessTokenSigningAlg(requestObject.getAccessTokenSigningAlg().toString());
        }
        if (requestObject.getAuthorizationSignedResponseAlg() != null) {
            client.getAttributes().setAuthorizationSignedResponseAlg(requestObject.getAuthorizationSignedResponseAlg().toString());
        }
        if (requestObject.getAuthorizationEncryptedResponseAlg() != null) {
            client.getAttributes().setAuthorizationEncryptedResponseAlg(requestObject.getAuthorizationEncryptedResponseAlg().toString());
        }
        if (requestObject.getAuthorizationEncryptedResponseEnc() != null) {
            client.getAttributes().setAuthorizationEncryptedResponseEnc(requestObject.getAuthorizationEncryptedResponseEnc().toString());
        }
        if (requestObject.getIdTokenSignedResponseAlg() != null) {
            client.setIdTokenSignedResponseAlg(requestObject.getIdTokenSignedResponseAlg().toString());
        }
        if (requestObject.getIdTokenEncryptedResponseAlg() != null) {
            client.setIdTokenEncryptedResponseAlg(requestObject.getIdTokenEncryptedResponseAlg().toString());
        }
        if (requestObject.getIdTokenEncryptedResponseEnc() != null) {
            client.setIdTokenEncryptedResponseEnc(requestObject.getIdTokenEncryptedResponseEnc().toString());
        }
        if (requestObject.getUserInfoSignedResponseAlg() != null) {
            client.setUserInfoSignedResponseAlg(requestObject.getUserInfoSignedResponseAlg().toString());
        }
        if (requestObject.getUserInfoEncryptedResponseAlg() != null) {
            client.setUserInfoEncryptedResponseAlg(requestObject.getUserInfoEncryptedResponseAlg().toString());
        }
        if (requestObject.getUserInfoEncryptedResponseEnc() != null) {
            client.setUserInfoEncryptedResponseEnc(requestObject.getUserInfoEncryptedResponseEnc().toString());
        }
        if (requestObject.getIntrospectionSignedResponseAlg() != null) {
            client.getAttributes().setIntrospectionSignedResponseAlg(requestObject.getIntrospectionSignedResponseAlg().toString());
        }
        if (requestObject.getIntrospectionEncryptedResponseAlg() != null) {
            client.getAttributes().setIntrospectionEncryptedResponseAlg(requestObject.getIntrospectionEncryptedResponseAlg().toString());
        }
        if (requestObject.getIntrospectionEncryptedResponseEnc() != null) {
            client.getAttributes().setIntrospectionEncryptedResponseEnc(requestObject.getIntrospectionEncryptedResponseEnc().toString());
        }
        if (requestObject.getTxTokenSignedResponseAlg() != null) {
            client.getAttributes().setTxTokenSignedResponseAlg(requestObject.getTxTokenSignedResponseAlg().toString());
        }
        if (requestObject.getTxTokenEncryptedResponseAlg() != null) {
            client.getAttributes().setTxTokenEncryptedResponseAlg(requestObject.getTxTokenEncryptedResponseAlg().toString());
        }
        if (requestObject.getTxTokenEncryptedResponseEnc() != null) {
            client.getAttributes().setTxTokenEncryptedResponseEnc(requestObject.getTxTokenEncryptedResponseEnc().toString());
        }
        if (requestObject.getRequestObjectSigningAlg() != null) {
            client.setRequestObjectSigningAlg(requestObject.getRequestObjectSigningAlg().toString());
        }
        if (requestObject.getRequestObjectEncryptionAlg() != null) {
            client.setRequestObjectEncryptionAlg(requestObject.getRequestObjectEncryptionAlg().toString());
        }
        if (requestObject.getRequestObjectEncryptionEnc() != null) {
            client.setRequestObjectEncryptionEnc(requestObject.getRequestObjectEncryptionEnc().toString());
        }
        if (requestObject.getTokenEndpointAuthMethod() != null) {
            client.setTokenEndpointAuthMethod(requestObject.getTokenEndpointAuthMethod().toString());
        } else if (requestObject.getAdditionalTokenEndpointAuthMethods() != null && !requestObject.getAdditionalTokenEndpointAuthMethods().isEmpty()) {
            client.setTokenEndpointAuthMethod(requestObject.getAdditionalTokenEndpointAuthMethods().iterator().next().toString());
        } else { // If omitted, the default is client_secret_basic
            client.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        }

        if (requestObject.getAdditionalTokenEndpointAuthMethods() != null) {
            client.getAttributes().setAdditionalTokenEndpointAuthMethods(requestObject.getAdditionalTokenEndpointAuthMethods().stream().map(AuthenticationMethod::toString).collect(Collectors.toList()));
        }

        if (requestObject.getTokenEndpointAuthSigningAlg() != null) {
            client.setTokenEndpointAuthSigningAlg(requestObject.getTokenEndpointAuthSigningAlg().toString());
        }
        if (requestObject.getDefaultMaxAge() != null) {
            client.setDefaultMaxAge(requestObject.getDefaultMaxAge());
        }
        if (!update) { // do not allow update it. It can be set only during creation
            client.getAttributes().setRequestedLifetime(requestObject.getLifetime());
        }
        List<String> defaultAcrValues = requestObject.getDefaultAcrValues();
        if (defaultAcrValues != null && !defaultAcrValues.isEmpty()) {
            client.setDefaultAcrValues(listAsArrayWithoutDuplicates(defaultAcrValues));
        }
        if (StringUtils.isNotBlank(requestObject.getInitiateLoginUri())) {
            client.setInitiateLoginUri(requestObject.getInitiateLoginUri());
        }

        final Integer minimumAcrLevel = requestObject.getMinimumAcrLevel();
        if (minimumAcrLevel != null) {
            client.getAttributes().setMinimumAcrLevel(minimumAcrLevel);
        }
        final Boolean minimumAcrLevelAutoresolve = requestObject.getMinimumAcrLevelAutoresolve();
        if (minimumAcrLevelAutoresolve != null) {
            client.getAttributes().setMinimumAcrLevelAutoresolve(minimumAcrLevelAutoresolve);
        }
        final List<String> minimumAcrPriorityList = requestObject.getMinimumAcrPriorityList();
        if (minimumAcrPriorityList != null) {
            client.getAttributes().setMinimumAcrPriorityList(new ArrayList<>(new HashSet<>(minimumAcrPriorityList)));
        }

        final List<String> groups = requestObject.getGroups();
        if (groups != null && !groups.isEmpty()) {
            client.setGroups(listAsArrayWithoutDuplicates(groups));
        }

        List<String> postLogoutRedirectUris = requestObject.getPostLogoutRedirectUris();
        if (postLogoutRedirectUris != null && !postLogoutRedirectUris.isEmpty()) {
            client.setPostLogoutRedirectUris(listAsArrayWithoutDuplicates(postLogoutRedirectUris));
        }

        if (StringUtils.isNotBlank(requestObject.getFrontChannelLogoutUri())) {
            client.setFrontChannelLogoutUri(requestObject.getFrontChannelLogoutUri());
        }
        client.setFrontChannelLogoutSessionRequired(requestObject.getFrontChannelLogoutSessionRequired());

        if (requestObject.getBackchannelLogoutUri() != null && !requestObject.getBackchannelLogoutUri().isEmpty()) {
            client.getAttributes().setBackchannelLogoutUri(Lists.newArrayList(requestObject.getBackchannelLogoutUri()));
        }
        client.getAttributes().setBackchannelLogoutSessionRequired(requestObject.getBackchannelLogoutSessionRequired());

        List<String> requestUris = requestObject.getRequestUris();
        if (requestUris != null && !requestUris.isEmpty()) {
            client.setRequestUris(listAsArrayWithoutDuplicates(requestUris));
        }

        List<String> authorizedOrigins = requestObject.getAuthorizedOrigins();
        if (authorizedOrigins != null && !authorizedOrigins.isEmpty()) {
            client.setAuthorizedOrigins(listAsArrayWithoutDuplicates(authorizedOrigins));
        }

        assignScopes(client, requestObject);

        List<String> claims = requestObject.getClaims();
        if (claims != null && !claims.isEmpty()) {
            List<String> claimsDn = attributeService.getAttributesDn(claims);
            client.setClaims(claimsDn.toArray(new String[claimsDn.size()]));
        }

        if (requestObject.getJsonObject() != null) {
            final String orgId = requestObject.getJsonObject().optString(RegisterRequestParam.ORG_ID.getName());
            if (StringUtils.isNotBlank(orgId)) {
                client.setOrganization(orgId);
            }

            // Custom params
            putCustomStuffIntoObject(client, requestObject.getJsonObject());
        }

        if (requestObject.getAccessTokenLifetime() != null) {
            client.setAccessTokenLifetime(requestObject.getAccessTokenLifetime());
        }
        if (requestObject.getParLifetime() != null) {
            client.getAttributes().setParLifetime(requestObject.getParLifetime());
        }
        if (requestObject.getRequirePar() != null) {
            client.getAttributes().setRequirePar(requestObject.getRequirePar());
        }
        if (requestObject.getDpopBoundAccessToken() != null) {
            client.getAttributes().setDpopBoundAccessToken(requestObject.getDpopBoundAccessToken());
        }

        if (StringUtils.isNotBlank(requestObject.getSoftwareId())) {
            client.setSoftwareId(requestObject.getSoftwareId());
        }
        if (StringUtils.isNotBlank(requestObject.getSoftwareVersion())) {
            client.setSoftwareVersion(requestObject.getSoftwareVersion());
        }
        if (StringUtils.isNotBlank(requestObject.getSoftwareStatement())) {
            client.setSoftwareStatement(requestObject.getSoftwareStatement());
        }
        if (StringUtils.isNotBlank(requestObject.getEvidence())) {
            client.getAttributes().setEvidence(requestObject.getEvidence());
        }

        if (StringUtils.isNotBlank(requestObject.getSubjectIdentifierAttribute())) {
            client.getAttributes().setPublicSubjectIdentifierAttribute(requestObject.getSubjectIdentifierAttribute());
        }

        if (requestObject.getDefaultPromptLogin() != null) {
            client.getAttributes().setDefaultPromptLogin(requestObject.getDefaultPromptLogin());
        }

        List<String> authorizedAcrValues = requestObject.getAuthorizedAcrValues();
        if (authorizedAcrValues != null && !authorizedAcrValues.isEmpty()) {
            authorizedAcrValues = new ArrayList<>(new HashSet<>(authorizedAcrValues)); // Remove repeated elements
            client.getAttributes().setAuthorizedAcrValues(authorizedAcrValues);
        }

        cibaRegisterClientMetadataService.updateClient(client, requestObject.getBackchannelTokenDeliveryMode(),
                requestObject.getBackchannelClientNotificationEndpoint(), requestObject.getBackchannelAuthenticationRequestSigningAlg(),
                requestObject.getBackchannelUserCodeParameter());
    }

    public void assignScopes(Client client, RegisterRequest requestObject) {
        if (isFalse(appConfiguration.getDynamicRegistrationScopesParamEnabled())) {
            log.debug("Skip scopes update. Reason - configuration dynamicRegistrationScopesParamEnabled=false");
            return;
        }

        List<String> requestScopes = requestObject.getScope();
        if (requestScopes == null || requestScopes.isEmpty()) {
            log.trace("No scopes in request");
            return;
        }

        // apply ROPC restriction
        if (Arrays.asList(client.getGrantTypes()).contains(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS) && !appConfiguration.getDynamicRegistrationAllowedPasswordGrantScopes().isEmpty()) {
            requestScopes = Lists.newArrayList(requestScopes);
            requestScopes.retainAll(appConfiguration.getDynamicRegistrationAllowedPasswordGrantScopes());
        }

        List<String> defaultScopes = scopeService.getDefaultScopesDn();
        List<String> requestedScopes = scopeService.getScopesDn(requestScopes);
        Set<String> allowedScopes = new HashSet<>();

        for (String requestedScope : requestedScopes) {
            if (defaultScopes.contains(requestedScope)) {
                allowedScopes.add(requestedScope);
            }
        }

        log.trace("Allowed scopes: {}, requested scopes: {}, default scopes: {}", allowedScopes, requestedScopes, defaultScopes);
        client.setScopes(allowedScopes.toArray(new String[0]));
    }

    /**
     * Puts custom object class and custom attributes in client object for persistence.
     *
     * @param client        client object
     * @param requestObject request object
     */
    private void putCustomStuffIntoObject(Client client, JSONObject requestObject) throws JSONException {
        // custom object class
        final String customOC = appConfiguration.getDynamicRegistrationCustomObjectClass();
        if (StringUtils.isNotBlank(customOC)) {
            client.setCustomObjectClasses(new String[]{customOC});
        }

        // custom attributes (custom attributes must be in custom object class)
        final List<String> attrList = appConfiguration.getDynamicRegistrationCustomAttributes();
        if (attrList == null || attrList.isEmpty()) {
            return;
        }

        addDefaultCustomAttributes(requestObject);

        for (String attr : attrList) {
            if (requestObject.has(attr)) {
                addCustomAttribute(client, requestObject, attr);
            }
        }
    }

    public void addDefaultCustomAttributes(JSONObject requestObject) {
        final JsonNode node = appConfiguration.getDynamicRegistrationDefaultCustomAttributes();
        final List<String> allowed = appConfiguration.getDynamicRegistrationCustomAttributes();
        if (allowed == null || allowed.isEmpty() || node == null || node.isEmpty()) {
            return;
        }

        final Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            if (!allowed.contains(key)) {
                continue;
            }
            final JsonNode value = node.get(key);
            if (value.isBoolean()) {
                requestObject.put(key, value.booleanValue());
            } else if (value.isTextual()) {
                requestObject.put(key, value.textValue());
            } else if (value.isNumber()) {
                requestObject.put(key, value.numberValue());
            } else if (value.isDouble()) {
                requestObject.put(key, value.asDouble());
            }
        }
    }

    private void addCustomAttribute(Client client, JSONObject requestObject, String attr) {
        final JSONArray parameterValuesJsonArray = requestObject.optJSONArray(attr);
        final List<String> parameterValues = parameterValuesJsonArray != null ?
                toList(parameterValuesJsonArray) :
                Lists.newArrayList(requestObject.getString(attr));
        if (!parameterValues.isEmpty()) {
            try {
                boolean processed = processApplicationAttributes(client, attr, parameterValues);
                if (!processed) {
                    final CustomObjectAttribute customAttribute = new CustomObjectAttribute();
                    customAttribute.setName(attr);
                    customAttribute.setValues(new ArrayList<>(parameterValues));
                    client.getCustomAttributes().add(customAttribute);
                }
            } catch (Exception e) {
                log.debug(e.getMessage(), e);
            }
        }
    }

    private boolean processApplicationAttributes(Client client, String attr, final List<String> parameterValues) {
        if (StringHelper.equalsIgnoreCase("jansTrustedClnt", attr)) {
            boolean trustedClient = StringHelper.toBoolean(parameterValues.get(0), false);
            client.setTrustedClient(trustedClient);

            return true;
        } else if (StringHelper.equalsIgnoreCase("jansInclClaimsInIdTkn", attr)) {
            boolean includeClaimsInIdToken = StringHelper.toBoolean(parameterValues.get(0), false);
            client.setIncludeClaimsInIdToken(includeClaimsInIdToken);

            return true;
        }

        return false;
    }

    public Response.ResponseBuilder createInternalErrorResponse(String reason) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(errorResponseFactory.errorAsJson(RegisterErrorResponseType.INVALID_CLIENT_METADATA, reason));
    }

    @NotNull
    public JSONObject parseRequestObjectWithoutValidation(String requestParams) throws JSONException {
        try {
            if (isTrue(appConfiguration.getDcrSignatureValidationEnabled())) {
                return Jwt.parseOrThrow(requestParams).getClaims().toJsonObject();
            }
            return new JSONObject(requestParams);
        } catch (InvalidJwtException e) {
            if (log.isTraceEnabled())
                log.trace("Invalid JWT, trying to parse it as plain unencoded json", e);
            return new JSONObject(requestParams); // #241 plain unencoded request object
        } catch (Exception e) {
            final String msg = "Unable to parse request object.";
            log.error(msg, e);
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, msg);
        }
    }
}
