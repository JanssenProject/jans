/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.servlet;

import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.*;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Scope;
import io.jans.as.persistence.model.ScopeAttributes;
import io.jans.as.server.ciba.CIBAConfigurationService;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.LocalResponseCache;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.ExternalDiscoveryService;
import io.jans.as.server.service.external.ExternalDynamicScopeService;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.GluuAttribute;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static io.jans.as.model.configuration.ConfigurationResponseClaim.*;
import static io.jans.as.model.util.StringUtils.implode;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version September 30, 2021
 */
@WebServlet(urlPatterns = "/.well-known/openid-configuration", loadOnStartup = 10)
public class OpenIdConfiguration extends HttpServlet {

    private static final long serialVersionUID = -8224898157373678904L;

    @Inject
    private transient Logger log;

    @Inject
    private transient AppConfiguration appConfiguration;

    @Inject
    private transient AttributeService attributeService;

    @Inject
    private transient ScopeService scopeService;

    @Inject
    private transient ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private transient ExternalDynamicScopeService externalDynamicScopeService;

    @Inject
    private transient ExternalDiscoveryService externalDiscoveryService;

    @Inject
    private transient CIBAConfigurationService cibaConfigurationService;

    @Inject
    private transient LocalResponseCache localResponseCache;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param servletRequest servlet request
     * @param httpResponse   servlet response
     * @throws IOException
     */
    @SuppressWarnings({"deprecation", "java:S3776"})
    protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse httpResponse) throws IOException {
        if (!(externalAuthenticationService.isLoaded() && externalDynamicScopeService.isLoaded())) {
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            log.error("Jans Auth still starting up!");
            return;
        }

        httpResponse.setContentType("application/json");
        try (PrintWriter out = httpResponse.getWriter()) {
            final JSONObject cachedResponse = localResponseCache.getDiscoveryResponse();
            if (cachedResponse != null) {
                log.trace("Cached discovery response returned.");
                out.println(ServerUtil.toPrettyJson(cachedResponse).replace("\\/", "/"));
                return;
            }

            JSONObject jsonObj = new JSONObject();

            jsonObj.put(ISSUER, appConfiguration.getIssuer());
            jsonObj.put(AUTHORIZATION_ENDPOINT, appConfiguration.getAuthorizationEndpoint());
            jsonObj.put(TOKEN_ENDPOINT, appConfiguration.getTokenEndpoint());
            jsonObj.put(JWKS_URI, appConfiguration.getJwksUri());
            jsonObj.put(CHECK_SESSION_IFRAME, appConfiguration.getCheckSessionIFrame());

            if (appConfiguration.isFeatureEnabled(FeatureFlagType.REVOKE_TOKEN))
                jsonObj.put(REVOCATION_ENDPOINT, appConfiguration.getTokenRevocationEndpoint());
            if (appConfiguration.isFeatureEnabled(FeatureFlagType.REVOKE_SESSION))
                jsonObj.put(SESSION_REVOCATION_ENDPOINT, endpointUrl("/revoke_session"));
            if (appConfiguration.isFeatureEnabled(FeatureFlagType.USERINFO))
                jsonObj.put(USER_INFO_ENDPOINT, appConfiguration.getUserInfoEndpoint());
            if (appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENTINFO))
                jsonObj.put(CLIENT_INFO_ENDPOINT, appConfiguration.getClientInfoEndpoint());
            if (appConfiguration.isFeatureEnabled(FeatureFlagType.END_SESSION))
                jsonObj.put(END_SESSION_ENDPOINT, appConfiguration.getEndSessionEndpoint());
            if (appConfiguration.isFeatureEnabled(FeatureFlagType.REGISTRATION))
                jsonObj.put(REGISTRATION_ENDPOINT, appConfiguration.getRegistrationEndpoint());
            if (appConfiguration.isFeatureEnabled(FeatureFlagType.ID_GENERATION))
                jsonObj.put(ID_GENERATION_ENDPOINT, appConfiguration.getIdGenerationEndpoint());
            if (appConfiguration.isFeatureEnabled(FeatureFlagType.INTROSPECTION))
                jsonObj.put(INTROSPECTION_ENDPOINT, appConfiguration.getIntrospectionEndpoint());
            if (appConfiguration.isFeatureEnabled(FeatureFlagType.DEVICE_AUTHZ))
                jsonObj.put(DEVICE_AUTHZ_ENDPOINT, appConfiguration.getDeviceAuthzEndpoint());
            if (appConfiguration.isFeatureEnabled(FeatureFlagType.PAR)) {
                jsonObj.put(PAR_ENDPOINT, appConfiguration.getParEndpoint());
                jsonObj.put(REQUIRE_PAR, appConfiguration.getRequirePar());
            }

            JSONArray responseTypesSupported = new JSONArray();
            for (Set<ResponseType> responseTypes : appConfiguration.getResponseTypesSupported()) {
                responseTypesSupported.put(implode(responseTypes, " "));
            }
            if (responseTypesSupported.length() > 0) {
                jsonObj.put(RESPONSE_TYPES_SUPPORTED, responseTypesSupported);
            }

            List<String> listResponseModesSupported = new ArrayList<>();
            if (appConfiguration.getResponseModesSupported() != null) {
                for (ResponseMode responseMode : appConfiguration.getResponseModesSupported()) {
                    listResponseModesSupported.add(responseMode.getValue());
                }
            }
            if (!listResponseModesSupported.isEmpty()) {
                Util.putArray(jsonObj, listResponseModesSupported, RESPONSE_MODES_SUPPORTED);
            }

            List<String> listGrantTypesSupported = new ArrayList<>();
            for (GrantType grantType : appConfiguration.getGrantTypesSupported()) {
                listGrantTypesSupported.add(grantType.getValue());
            }
            if (!listGrantTypesSupported.isEmpty()) {
                Util.putArray(jsonObj, listGrantTypesSupported, GRANT_TYPES_SUPPORTED);
            }

            jsonObj.put(AUTH_LEVEL_MAPPING, createAuthLevelMapping());

            Util.putArray(jsonObj, externalAuthenticationService.getAcrValuesList(), ACR_VALUES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getSubjectTypesSupported(), SUBJECT_TYPES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getAuthorizationSigningAlgValuesSupported(), AUTHORIZATION_SIGNING_ALG_VALUES_SUPPORTED);
            Util.putArray(jsonObj, appConfiguration.getAuthorizationEncryptionAlgValuesSupported(), AUTHORIZATION_ENCRYPTION_ALG_VALUES_SUPPORTED);
            Util.putArray(jsonObj, appConfiguration.getAuthorizationEncryptionEncValuesSupported(), AUTHORIZATION_ENCRYPTION_ENC_VALUES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getUserInfoSigningAlgValuesSupported(), USER_INFO_SIGNING_ALG_VALUES_SUPPORTED);
            Util.putArray(jsonObj, appConfiguration.getUserInfoEncryptionAlgValuesSupported(), USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED);
            Util.putArray(jsonObj, appConfiguration.getUserInfoEncryptionEncValuesSupported(), USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getIdTokenSigningAlgValuesSupported(), ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
            Util.putArray(jsonObj, appConfiguration.getIdTokenEncryptionAlgValuesSupported(), ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED);
            Util.putArray(jsonObj, appConfiguration.getIdTokenEncryptionEncValuesSupported(), ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getAccessTokenSigningAlgValuesSupported(), ACCESS_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getRequestObjectSigningAlgValuesSupported(), REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED);
            Util.putArray(jsonObj, appConfiguration.getRequestObjectEncryptionAlgValuesSupported(), REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED);
            Util.putArray(jsonObj, appConfiguration.getRequestObjectEncryptionEncValuesSupported(), REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getTokenEndpointAuthMethodsSupported(), TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED);
            Util.putArray(jsonObj, appConfiguration.getTokenEndpointAuthSigningAlgValuesSupported(), TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getDpopSigningAlgValuesSupported(), DPOP_SIGNING_ALG_VALUES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getDisplayValuesSupported(), DISPLAY_VALUES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getClaimTypesSupported(), CLAIM_TYPES_SUPPORTED);

            jsonObj.put(SERVICE_DOCUMENTATION, appConfiguration.getServiceDocumentation());

            Util.putArray(jsonObj, appConfiguration.getIdTokenTokenBindingCnfValuesSupported(), ID_TOKEN_TOKEN_BINDING_CNF_VALUES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getClaimsLocalesSupported(), CLAIMS_LOCALES_SUPPORTED);

            Util.putArray(jsonObj, appConfiguration.getUiLocalesSupported(), UI_LOCALES_SUPPORTED);

            JSONArray scopesSupported = new JSONArray();
            JSONArray claimsSupported = new JSONArray();
            JSONArray scopeToClaimsMapping = createScopeToClaimsMapping(scopesSupported, claimsSupported);
            if (scopesSupported.length() > 0) {
                jsonObj.put(SCOPES_SUPPORTED, scopesSupported);
            }
            if (claimsSupported.length() > 0) {
                jsonObj.put(CLAIMS_SUPPORTED, claimsSupported);
            }
            jsonObj.put(SCOPE_TO_CLAIMS_MAPPING, scopeToClaimsMapping);

            jsonObj.put(CLAIMS_PARAMETER_SUPPORTED, appConfiguration.getClaimsParameterSupported());
            jsonObj.put(REQUEST_PARAMETER_SUPPORTED, appConfiguration.getRequestParameterSupported());
            jsonObj.put(REQUEST_URI_PARAMETER_SUPPORTED, appConfiguration.getRequestUriParameterSupported());
            jsonObj.put(REQUIRE_REQUEST_URI_REGISTRATION, appConfiguration.getRequireRequestUriRegistration());
            jsonObj.put(OP_POLICY_URI, appConfiguration.getOpPolicyUri());
            jsonObj.put(OP_TOS_URI, appConfiguration.getOpTosUri());
            jsonObj.put(TLS_CLIENT_CERTIFICATE_BOUND_ACCESS_TOKENS, Boolean.TRUE);
            jsonObj.put(BACKCHANNEL_LOGOUT_SUPPORTED, Boolean.TRUE);
            jsonObj.put(BACKCHANNEL_LOGOUT_SESSION_SUPPORTED, Boolean.TRUE);
            jsonObj.put(FRONTCHANNEL_LOGOUT_SUPPORTED, Boolean.TRUE);
            jsonObj.put(FRONTCHANNEL_LOGOUT_SESSION_SUPPORTED, Boolean.TRUE);
            jsonObj.put(FRONT_CHANNEL_LOGOUT_SESSION_SUPPORTED,
                    appConfiguration.getFrontChannelLogoutSessionSupported());

            addMtlsAliases(jsonObj);

            // CIBA Configuration
            cibaConfigurationService.processConfiguration(jsonObj);

            // SSA
            if (appConfiguration.isFeatureEnabled(FeatureFlagType.SSA) && appConfiguration.getSsaConfiguration() != null) {
                jsonObj.put(SSA_ENDPOINT, appConfiguration.getSsaConfiguration().getSsaEndpoint());
            }

            filterOutKeys(jsonObj, appConfiguration);
            localResponseCache.putDiscoveryResponse(jsonObj);

            JSONObject clone = new JSONObject(jsonObj.toString());

            ExecutionContext context = new ExecutionContext(servletRequest, httpResponse);
            if (!externalDiscoveryService.modifyDiscovery(jsonObj, context)) {
                jsonObj = clone; // revert to original state if object was modified in script
            }

            out.println(ServerUtil.toPrettyJson(jsonObj).replace("\\/", "/"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("java:S3776")
    private void addMtlsAliases(JSONObject jsonObj) {
        JSONObject aliases = new JSONObject();

        if (StringUtils.isNotBlank(appConfiguration.getMtlsAuthorizationEndpoint()))
            aliases.put(AUTHORIZATION_ENDPOINT, appConfiguration.getMtlsAuthorizationEndpoint());
        if (StringUtils.isNotBlank(appConfiguration.getMtlsTokenEndpoint()))
            aliases.put(TOKEN_ENDPOINT, appConfiguration.getMtlsTokenEndpoint());
        if (StringUtils.isNotBlank(appConfiguration.getMtlsJwksUri()))
            aliases.put(JWKS_URI, appConfiguration.getMtlsJwksUri());
        if (StringUtils.isNotBlank(appConfiguration.getMtlsCheckSessionIFrame()))
            aliases.put(CHECK_SESSION_IFRAME, appConfiguration.getMtlsCheckSessionIFrame());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.REVOKE_TOKEN) && StringUtils.isNotBlank(appConfiguration.getMtlsTokenRevocationEndpoint()))
            aliases.put(REVOCATION_ENDPOINT, appConfiguration.getMtlsTokenRevocationEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.REVOKE_SESSION) && StringUtils.isNotBlank(appConfiguration.getMtlsEndSessionEndpoint()))
            aliases.put(SESSION_REVOCATION_ENDPOINT, StringUtils.replace(appConfiguration.getMtlsEndSessionEndpoint(), "/end_session", "/revoke_session"));
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.USERINFO) && StringUtils.isNotBlank(appConfiguration.getMtlsUserInfoEndpoint()))
            aliases.put(USER_INFO_ENDPOINT, appConfiguration.getMtlsUserInfoEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENTINFO) && StringUtils.isNotBlank(appConfiguration.getMtlsClientInfoEndpoint()))
            aliases.put(CLIENT_INFO_ENDPOINT, appConfiguration.getMtlsClientInfoEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.END_SESSION) && StringUtils.isNotBlank(appConfiguration.getMtlsEndSessionEndpoint()))
            aliases.put(END_SESSION_ENDPOINT, appConfiguration.getMtlsEndSessionEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.REGISTRATION) && StringUtils.isNotBlank(appConfiguration.getMtlsRegistrationEndpoint()))
            aliases.put(REGISTRATION_ENDPOINT, appConfiguration.getMtlsRegistrationEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.ID_GENERATION) && StringUtils.isNotBlank(appConfiguration.getMtlsIdGenerationEndpoint()))
            aliases.put(ID_GENERATION_ENDPOINT, appConfiguration.getMtlsIdGenerationEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.INTROSPECTION) && StringUtils.isNotBlank(appConfiguration.getMtlsIntrospectionEndpoint()))
            aliases.put(INTROSPECTION_ENDPOINT, appConfiguration.getMtlsIntrospectionEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.DEVICE_AUTHZ) && StringUtils.isNotBlank(appConfiguration.getMtlsDeviceAuthzEndpoint()))
            aliases.put(DEVICE_AUTHZ_ENDPOINT, appConfiguration.getMtlsDeviceAuthzEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.PAR) && StringUtils.isNotBlank(appConfiguration.getMtlsParEndpoint())) {
            aliases.put(PAR_ENDPOINT, appConfiguration.getMtlsParEndpoint());
        }

        if (log.isTraceEnabled()) {
            log.trace("MTLS aliases: {}", aliases);
        }
        if (!aliases.isEmpty()) {
            jsonObj.put(MTLS_ENDPOINT_ALIASES, aliases);
        }
    }

    public static void filterOutKeys(JSONObject jsonObj, AppConfiguration appConfiguration) {
        final List<String> denyKeys = appConfiguration.getDiscoveryDenyKeys();
        if (!denyKeys.isEmpty()) {
            for (String key : new HashSet<>(jsonObj.keySet())) {
                if (denyKeys.contains(key)) {
                    jsonObj.remove(key);
                }
            }
        }

        final List<String> allowedKeys = appConfiguration.getDiscoveryAllowedKeys();
        if (!allowedKeys.isEmpty()) {
            for (String key : new HashSet<>(jsonObj.keySet())) {
                if (!allowedKeys.contains(key)) {
                    jsonObj.remove(key);
                }
            }
        }
    }

    private String endpointUrl(String path) {
        return StringUtils.replace(appConfiguration.getEndSessionEndpoint(), "/end_session", path);
    }

    /**
     * @deprecated theses params:
     * <ul>
     * <li>id_generation_endpoint</li>
     * <li>introspection_endpoint</li>
     * <li>auth_level_mapping</li>
     * <li>scope_to_claims_mapping</li>
     * </ul>
     * will be moved from /.well-known/openid-configuration to
     * /.well-known/gluu-configuration
     */
    @Deprecated
    @SuppressWarnings("java:S3776")
    private JSONArray createScopeToClaimsMapping(JSONArray scopesSupported, JSONArray claimsSupported) {
        final JSONArray scopeToClaimMapping = new JSONArray();
        Set<String> scopes = new HashSet<>();
        Set<String> claims = new HashSet<>();

        try {
            for (Scope scope : scopeService.getAllScopesList()) {
                if ((scope.getScopeType() == ScopeType.SPONTANEOUS && scope.isDeletable())
                        || !(canShowInConfigEndpoint(scope.getAttributes()))) {
                    continue;
                }

                final JSONArray claimsList = new JSONArray();
                final JSONObject mapping = new JSONObject();
                mapping.put(scope.getId(), claimsList);
                scopes.add(scope.getId());

                scopeToClaimMapping.put(mapping);

                if (ScopeType.DYNAMIC.equals(scope.getScopeType())) {
                    List<String> claimNames = externalDynamicScopeService
                            .executeExternalGetSupportedClaimsMethods(Arrays.asList(scope));
                    for (String claimName : claimNames) {
                        if (StringUtils.isNotBlank(claimName)) {
                            claimsList.put(claimName);
                            claims.add(claimName);
                        }
                    }
                } else {
                    final List<String> claimIdList = scope.getClaims();
                    if (claimIdList != null && !claimIdList.isEmpty()) {
                        for (String claimDn : claimIdList) {
                            final GluuAttribute attribute = attributeService.getAttributeByDn(claimDn);
                            final String claimName = attribute.getClaimName();
                            if (StringUtils.isNotBlank(claimName) && !Boolean.TRUE.equals(attribute.getJansHideOnDiscovery())) {
                                claimsList.put(claimName);
                                claims.add(claimName);
                            }
                        }
                    }
                }
            }

            for (String scope : scopes) {
                scopesSupported.put(scope);
            }
            for (String claim : claims) {
                claimsSupported.put(claim);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return scopeToClaimMapping;
    }

    private boolean canShowInConfigEndpoint(ScopeAttributes scopeAttributes) {
        return scopeAttributes.isShowInConfigurationEndpoint();
    }

    /**
     * @deprecated theses params:
     * <ul>
     * <li>id_generation_endpoint</li>
     * <li>introspection_endpoint</li>
     * <li>auth_level_mapping</li>
     * <li>scope_to_claims_mapping</li>
     * </ul>
     * will be moved from /.well-known/openid-configuration to
     * /.well-known/gluu-configuration
     */
    @Deprecated
    private JSONObject createAuthLevelMapping() {
        final JSONObject mappings = new JSONObject();
        try {
            Map<Integer, Set<String>> map = externalAuthenticationService.levelToAcrMapping();
            for (Map.Entry<Integer, Set<String>> entry : map.entrySet())
                mappings.put(entry.getKey().toString(), entry.getValue());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return mappings;
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "OpenID Provider Configuration Information";
    }

}