package io.jans.as.server.service;

import com.google.common.collect.Lists;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.*;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Scope;
import io.jans.as.persistence.model.ScopeAttributes;
import io.jans.as.server.ciba.CIBAConfigurationService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.ExternalAuthzDetailTypeService;
import io.jans.as.server.service.external.ExternalDynamicScopeService;
import io.jans.model.JansAttribute;
import io.jans.util.OxConstants;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static io.jans.as.model.configuration.ConfigurationResponseClaim.*;
import static io.jans.as.model.util.StringUtils.implode;
import static io.jans.as.server.servlet.OpenIdConfiguration.filterOutKeys;

/**
 * @author Yuriy Z
 */

@Stateless
@Named
public class DiscoveryService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ExternalAuthzDetailTypeService externalAuthzDetailTypeService;

    @Inject
    private CIBAConfigurationService cibaConfigurationService;

    @Inject
    private LocalResponseCache localResponseCache;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private ExternalDynamicScopeService externalDynamicScopeService;

    @Inject
    private ScopeService scopeService;

    @Inject
    private AttributeService attributeService;

    public JSONObject process() {
        JSONObject jsonObj = new JSONObject();

        jsonObj.put(ISSUER, appConfiguration.getIssuer());
        jsonObj.put(AUTHORIZATION_ENDPOINT, appConfiguration.getAuthorizationEndpoint());
        jsonObj.put(AUTHORIZATION_CHALLENGE_ENDPOINT, appConfiguration.getAuthorizationChallengeEndpoint());
        jsonObj.put(TOKEN_ENDPOINT, appConfiguration.getTokenEndpoint());
        jsonObj.put(JWKS_URI, appConfiguration.getJwksUri());
        jsonObj.put(ARCHIVED_JWKS_URI, appConfiguration.getArchivedJwksUri());
        jsonObj.put(CHECK_SESSION_IFRAME, appConfiguration.getCheckSessionIFrame());

        if (appConfiguration.isFeatureEnabled(FeatureFlagType.STATUS_LIST))
            jsonObj.put(STATUS_LIST_ENDPOINT, getTokenStatusListEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION))
            jsonObj.put(ACCESS_EVALUATION_V1_ENDPOINT, getAccessEvaluationV1Endpoint(appConfiguration));
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.REVOKE_TOKEN))
            jsonObj.put(REVOCATION_ENDPOINT, appConfiguration.getTokenRevocationEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.REVOKE_SESSION))
            jsonObj.put(SESSION_REVOCATION_ENDPOINT, endpointUrl("/revoke_session"));
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.GLOBAL_TOKEN_REVOCATION))
            jsonObj.put(GLOBAL_TOKEN_REVOCATION_ENDPOINT, endpointUrl("/global-token-revocation"));
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.USERINFO))
            jsonObj.put(USER_INFO_ENDPOINT, appConfiguration.getUserInfoEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENTINFO))
            jsonObj.put(CLIENT_INFO_ENDPOINT, appConfiguration.getClientInfoEndpoint());

        final boolean isEndSessionEnabled = appConfiguration.isFeatureEnabled(FeatureFlagType.END_SESSION);
        if (isEndSessionEnabled)
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

        final JSONArray promptValuesSupported = new JSONArray();
        promptValuesSupported.putAll(Arrays.stream(Prompt.values()).map(Prompt::getParamName).collect(Collectors.toList()));

        jsonObj.put(PROMPT_VALUES_SUPPORTED, promptValuesSupported);

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

        Util.putArray(jsonObj, getAcrValuesList(), ACR_VALUES_SUPPORTED);
        Util.putArray(jsonObj, Lists.newArrayList(externalAuthzDetailTypeService.getSupportedAuthzDetailsTypes()), AUTHORIZATION_DETAILS_TYPES_SUPPORTED);

        Util.putArray(jsonObj, appConfiguration.getSubjectTypesSupported(), SUBJECT_TYPES_SUPPORTED);

        Util.putArray(jsonObj, appConfiguration.getAuthorizationSigningAlgValuesSupported(), AUTHORIZATION_SIGNING_ALG_VALUES_SUPPORTED);
        Util.putArray(jsonObj, appConfiguration.getAuthorizationEncryptionAlgValuesSupported(), AUTHORIZATION_ENCRYPTION_ALG_VALUES_SUPPORTED);
        Util.putArray(jsonObj, appConfiguration.getAuthorizationEncryptionEncValuesSupported(), AUTHORIZATION_ENCRYPTION_ENC_VALUES_SUPPORTED);

        Util.putArray(jsonObj, appConfiguration.getUserInfoSigningAlgValuesSupported(), USER_INFO_SIGNING_ALG_VALUES_SUPPORTED);
        Util.putArray(jsonObj, appConfiguration.getUserInfoEncryptionAlgValuesSupported(), USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED);
        Util.putArray(jsonObj, appConfiguration.getUserInfoEncryptionEncValuesSupported(), USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED);

        Util.putArray(jsonObj, appConfiguration.getIntrospectionSigningAlgValuesSupported(), INTROSPECTION_SIGNING_ALG_VALUES_SUPPORTED);
        Util.putArray(jsonObj, appConfiguration.getIntrospectionEncryptionAlgValuesSupported(), INTROSPECTION_ENCRYPTION_ALG_VALUES_SUPPORTED);
        Util.putArray(jsonObj, appConfiguration.getIntrospectionEncryptionEncValuesSupported(), INTROSPECTION_ENCRYPTION_ENC_VALUES_SUPPORTED);

        Util.putArray(jsonObj, appConfiguration.getTxTokenSigningAlgValuesSupported(), TX_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
        Util.putArray(jsonObj, appConfiguration.getTxTokenEncryptionAlgValuesSupported(), TX_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED);
        Util.putArray(jsonObj, appConfiguration.getTxTokenEncryptionEncValuesSupported(), TX_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED);

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
        jsonObj.put(BACKCHANNEL_LOGOUT_SUPPORTED, isEndSessionEnabled);
        jsonObj.put(BACKCHANNEL_LOGOUT_SESSION_SUPPORTED, isEndSessionEnabled);
        jsonObj.put(FRONTCHANNEL_LOGOUT_SUPPORTED, isEndSessionEnabled);
        jsonObj.put(FRONTCHANNEL_LOGOUT_SESSION_SUPPORTED, isEndSessionEnabled);
        jsonObj.put(FRONT_CHANNEL_LOGOUT_SESSION_SUPPORTED,
                appConfiguration.getFrontChannelLogoutSessionSupported() && isEndSessionEnabled);

        addMtlsAliases(jsonObj);

        // CIBA Configuration
        cibaConfigurationService.processConfiguration(jsonObj);

        // SSA
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.SSA) && appConfiguration.getSsaConfiguration() != null) {
            jsonObj.put(SSA_ENDPOINT, appConfiguration.getSsaConfiguration().getSsaEndpoint());
        }

        final Map<String, String> acrMappings = appConfiguration.getAcrMappings();
        if (acrMappings != null && !acrMappings.isEmpty())
            jsonObj.put(ACR_MAPPINGS, acrMappings);

        filterOutKeys(jsonObj, appConfiguration);
        localResponseCache.putDiscoveryResponse(jsonObj);
        return jsonObj;
    }

    public String endpointUrl(String path) {
        return endpointUrl(appConfiguration.getEndSessionEndpoint(), path);
    }

    public static String endpointUrl(String endSessionEndpoint, String path) {
        return StringUtils.replace(endSessionEndpoint, "/end_session", path);
    }

    public String getTokenStatusListEndpoint() {
        return endpointUrl("/status_list");
    }

    public static String getAccessEvaluationV1Endpoint(AppConfiguration appConfiguration) {
        return endpointUrl(appConfiguration.getEndSessionEndpoint(), "/access/v1/evaluation");
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
     * /.well-known/openid-configuration
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

    public List<String> getAcrValuesList() {
        return getAcrValuesList(externalAuthenticationService.getAcrValuesList());
    }

    public static List<String> getAcrValuesList(final List<String> scriptAliases) {
        if (!scriptAliases.contains(OxConstants.SCRIPT_TYPE_INTERNAL_RESERVED_NAME)) {
            scriptAliases.add(OxConstants.SCRIPT_TYPE_INTERNAL_RESERVED_NAME);
        }
        return scriptAliases;
    }


    private boolean canShowInConfigEndpoint(ScopeAttributes scopeAttributes) {
        return scopeAttributes.isShowInConfigurationEndpoint();
    }

    @SuppressWarnings("java:S3776")
    private void addMtlsAliases(JSONObject jsonObj) {
        JSONObject aliases = new JSONObject();

        if (StringUtils.isNotBlank(appConfiguration.getMtlsAuthorizationEndpoint()))
            aliases.put(AUTHORIZATION_ENDPOINT, appConfiguration.getMtlsAuthorizationEndpoint());
        if (StringUtils.isNotBlank(appConfiguration.getMtlsAuthorizationChallengeEndpoint()))
            aliases.put(AUTHORIZATION_CHALLENGE_ENDPOINT, appConfiguration.getMtlsAuthorizationChallengeEndpoint());
        if (StringUtils.isNotBlank(appConfiguration.getMtlsTokenEndpoint()))
            aliases.put(TOKEN_ENDPOINT, appConfiguration.getMtlsTokenEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.STATUS_LIST) && StringUtils.isNotBlank(appConfiguration.getMtlsEndSessionEndpoint()))
            aliases.put(STATUS_LIST_ENDPOINT, endpointUrl(appConfiguration.getMtlsEndSessionEndpoint(), "/status_list"));
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION) && StringUtils.isNotBlank(appConfiguration.getMtlsEndSessionEndpoint()))
            aliases.put(ACCESS_EVALUATION_V1_ENDPOINT, endpointUrl(appConfiguration.getMtlsEndSessionEndpoint(), "/access/v1/evaluation"));
        if (StringUtils.isNotBlank(appConfiguration.getMtlsJwksUri()))
            aliases.put(JWKS_URI, appConfiguration.getMtlsJwksUri());
        if (StringUtils.isNotBlank(appConfiguration.getMtlsCheckSessionIFrame()))
            aliases.put(CHECK_SESSION_IFRAME, appConfiguration.getMtlsCheckSessionIFrame());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.REVOKE_TOKEN) && StringUtils.isNotBlank(appConfiguration.getMtlsTokenRevocationEndpoint()))
            aliases.put(REVOCATION_ENDPOINT, appConfiguration.getMtlsTokenRevocationEndpoint());
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.REVOKE_SESSION) && StringUtils.isNotBlank(appConfiguration.getMtlsEndSessionEndpoint()))
            aliases.put(SESSION_REVOCATION_ENDPOINT, endpointUrl(appConfiguration.getMtlsEndSessionEndpoint(), "/revoke_session"));
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.GLOBAL_TOKEN_REVOCATION) && StringUtils.isNotBlank(appConfiguration.getMtlsEndSessionEndpoint()))
            aliases.put(GLOBAL_TOKEN_REVOCATION_ENDPOINT, endpointUrl(appConfiguration.getMtlsEndSessionEndpoint(), "/global-token-revocation"));
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

    /**
     * @deprecated theses params:
     * <ul>
     * <li>id_generation_endpoint</li>
     * <li>introspection_endpoint</li>
     * <li>auth_level_mapping</li>
     * <li>scope_to_claims_mapping</li>
     * </ul>
     * will be moved from /.well-known/openid-configuration to
     * /.well-known/openid-configuration
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
                            final JansAttribute attribute = attributeService.getAttributeByDn(claimDn);
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

}
