/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;

import static io.jans.as.model.configuration.ConfigurationResponseClaim.*;

/**
 * Encapsulates functionality to make OpenId Configuration request calls to an authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public class OpenIdConfigurationClient extends BaseClient<OpenIdConfigurationRequest, OpenIdConfigurationResponse> {

    private static final Logger LOG = Logger.getLogger(OpenIdConfigurationClient.class);

    private static final String MEDIA_TYPES = String.join(",", MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON);

    /**
     * Constructs an OpenID Configuration Client by providing an url where the REST service is located.
     *
     * @param url The REST service location.
     */
    public OpenIdConfigurationClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.GET;
    }

    public OpenIdConfigurationResponse execOpenIdConfiguration() {
        initClient();

        return exec();
    }

    /**
     * Executes the call to the REST Service requesting the OpenID Configuration and processes the response.
     *
     * @return The service response.
     */
    private OpenIdConfigurationResponse exec() {
        setRequest(new OpenIdConfigurationRequest());

        // Call REST Service and handle response
        String entity = null;
        try {
            Builder clientRequest = webTarget.request();
            applyCookies(clientRequest);

            // Prepare request parameters
            clientRequest.accept(MEDIA_TYPES);

            // Support AWS LB
            // Implement follow redirect manually because we have to set engine.setFollowRedirects(true); on engine layer 
            // clientRequest.followRedirects(true);

            clientResponse = clientRequest.buildGet().invoke();
            int status = clientResponse.getStatus();

            setResponse(new OpenIdConfigurationResponse(status));

            entity = clientResponse.readEntity(String.class);
            getResponse().setEntity(entity);
            getResponse().setHeaders(clientResponse.getMetadata());
            parse(entity, getResponse());
        } catch (JSONException e) {
            LOG.error("There is an error in the JSON response. Check if there is a syntax error in the JSON response or there is a wrong key", e);
            if (entity != null) {
                LOG.error("Invalid JSON: " + entity);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            LOG.error(e.getMessage(), e); // Unexpected exception.
        } finally {
            closeConnection();
        }

        return getResponse();
    }

    public static void parse(String json, OpenIdConfigurationResponse response) {
        if (StringUtils.isBlank(json)) {
            return;
        }

        JSONObject jsonObj = new JSONObject(json);

        response.setIssuer(jsonObj.optString(ISSUER, null));
        response.setAuthorizationEndpoint(jsonObj.optString(AUTHORIZATION_ENDPOINT, null));
        response.setTokenEndpoint(jsonObj.optString(TOKEN_ENDPOINT, null));
        response.setRevocationEndpoint(jsonObj.optString(REVOCATION_ENDPOINT, null));
        response.setSessionRevocationEndpoint(jsonObj.optString(SESSION_REVOCATION_ENDPOINT, null));
        response.setUserInfoEndpoint(jsonObj.optString(USER_INFO_ENDPOINT, null));
        response.setClientInfoEndpoint(jsonObj.optString(CLIENT_INFO_ENDPOINT, null));
        response.setCheckSessionIFrame(jsonObj.optString(CHECK_SESSION_IFRAME, null));
        response.setEndSessionEndpoint(jsonObj.optString(END_SESSION_ENDPOINT, null));
        response.setJwksUri(jsonObj.optString(JWKS_URI, null));
        response.setRegistrationEndpoint(jsonObj.optString(REGISTRATION_ENDPOINT, null));
        response.setIntrospectionEndpoint(jsonObj.optString(INTROSPECTION_ENDPOINT, null));
        response.setParEndpoint(jsonObj.optString(PAR_ENDPOINT, null));
        if (jsonObj.has(REQUIRE_PAR)) {
            response.setRequirePar(jsonObj.getBoolean(REQUIRE_PAR));
        }
        response.setDeviceAuthzEndpoint(jsonObj.optString(DEVICE_AUTHZ_ENDPOINT, null));
        if (jsonObj.has(SCOPE_TO_CLAIMS_MAPPING)) {
            response.setScopeToClaimsMapping(OpenIdConfigurationResponse.parseScopeToClaimsMapping(jsonObj.getJSONArray(SCOPE_TO_CLAIMS_MAPPING)));
        }
        if (jsonObj.has(MTLS_ENDPOINT_ALIASES)) {
            final JSONObject mtlsAliases = jsonObj.optJSONObject(MTLS_ENDPOINT_ALIASES);
            if (mtlsAliases != null) {
                response.setMltsAliases(Util.toSerializableMap(mtlsAliases.toMap()));
            }
        }
        Util.addToListIfHas(response.getScopesSupported(), jsonObj, SCOPES_SUPPORTED);
        Util.addToListIfHas(response.getResponseTypesSupported(), jsonObj, RESPONSE_TYPES_SUPPORTED);
        Util.addToListIfHas(response.getResponseModesSupported(), jsonObj, RESPONSE_MODES_SUPPORTED);
        Util.addToListIfHas(response.getGrantTypesSupported(), jsonObj, GRANT_TYPES_SUPPORTED);
        Util.addToListIfHas(response.getAcrValuesSupported(), jsonObj, ACR_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getSubjectTypesSupported(), jsonObj, SUBJECT_TYPES_SUPPORTED);
        Util.addToListIfHas(response.getAuthorizationSigningAlgValuesSupported(), jsonObj, AUTHORIZATION_SIGNING_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getAuthorizationEncryptionAlgValuesSupported(), jsonObj, AUTHORIZATION_ENCRYPTION_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getAuthorizationEncryptionEncValuesSupported(), jsonObj, AUTHORIZATION_ENCRYPTION_ENC_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getUserInfoSigningAlgValuesSupported(), jsonObj, USER_INFO_SIGNING_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getUserInfoEncryptionAlgValuesSupported(), jsonObj, USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getUserInfoEncryptionEncValuesSupported(), jsonObj, USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getIdTokenSigningAlgValuesSupported(), jsonObj, ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getIdTokenEncryptionAlgValuesSupported(), jsonObj, ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getIdTokenEncryptionEncValuesSupported(), jsonObj, ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getAccessTokenSigningAlgValuesSupported(), jsonObj, ACCESS_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getRequestObjectSigningAlgValuesSupported(), jsonObj, REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getRequestObjectEncryptionAlgValuesSupported(), jsonObj, REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getRequestObjectEncryptionEncValuesSupported(), jsonObj, REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getTokenEndpointAuthMethodsSupported(), jsonObj, TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED);
        Util.addToListIfHas(response.getTokenEndpointAuthSigningAlgValuesSupported(), jsonObj, TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getDpopSigningAlgValuesSupported(), jsonObj, DPOP_SIGNING_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getDisplayValuesSupported(), jsonObj, DISPLAY_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getClaimTypesSupported(), jsonObj, CLAIM_TYPES_SUPPORTED);
        Util.addToListIfHas(response.getClaimsSupported(), jsonObj, CLAIMS_SUPPORTED);
        response.setServiceDocumentation(jsonObj.optString(SERVICE_DOCUMENTATION, null));
        Util.addToListIfHas(response.getClaimsLocalesSupported(), jsonObj, CLAIMS_LOCALES_SUPPORTED);
        Util.addToListIfHas(response.getUiLocalesSupported(), jsonObj, UI_LOCALES_SUPPORTED);
        if (jsonObj.has(CLAIMS_PARAMETER_SUPPORTED)) {
            response.setClaimsParameterSupported(jsonObj.getBoolean(CLAIMS_PARAMETER_SUPPORTED));
        }
        if (jsonObj.has(REQUEST_PARAMETER_SUPPORTED)) {
            response.setRequestParameterSupported(jsonObj.getBoolean(REQUEST_PARAMETER_SUPPORTED));
        }
        if (jsonObj.has(REQUEST_URI_PARAMETER_SUPPORTED)) {
            response.setRequestUriParameterSupported(jsonObj.getBoolean(REQUEST_URI_PARAMETER_SUPPORTED));
        }
        if (jsonObj.has(TLS_CLIENT_CERTIFICATE_BOUND_ACCESS_TOKENS)) {
            response.setTlsClientCertificateBoundAccessTokens(jsonObj.optBoolean(TLS_CLIENT_CERTIFICATE_BOUND_ACCESS_TOKENS));
        }
        if (jsonObj.has(FRONTCHANNEL_LOGOUT_SUPPORTED)) {
            response.setFrontChannelLogoutSupported(jsonObj.getBoolean(FRONTCHANNEL_LOGOUT_SUPPORTED));
        }
        if (jsonObj.has(FRONTCHANNEL_LOGOUT_SESSION_SUPPORTED)) {
            response.setFrontChannelLogoutSessionSupported(jsonObj.getBoolean(FRONTCHANNEL_LOGOUT_SESSION_SUPPORTED));
        }
        response.setBackchannelLogoutSupported(jsonObj.optBoolean(BACKCHANNEL_LOGOUT_SUPPORTED));
        if (jsonObj.has(BACKCHANNEL_LOGOUT_SESSION_SUPPORTED)) {
            response.setBackchannelLogoutSessionSupported(jsonObj.optBoolean(BACKCHANNEL_LOGOUT_SESSION_SUPPORTED));
        }
        if (jsonObj.has(REQUIRE_REQUEST_URI_REGISTRATION)) {
            response.setRequireRequestUriRegistration(jsonObj.getBoolean(REQUIRE_REQUEST_URI_REGISTRATION));
        }
        response.setOpPolicyUri(jsonObj.optString(OP_POLICY_URI, null));
        response.setOpTosUri(jsonObj.optString(OP_TOS_URI, null));

        // CIBA
        response.setBackchannelAuthenticationEndpoint(jsonObj.optString(BACKCHANNEL_AUTHENTICATION_ENDPOINT, null));
        Util.addToListIfHas(response.getBackchannelTokenDeliveryModesSupported(), jsonObj, BACKCHANNEL_TOKEN_DELIVERY_MODES_SUPPORTED);
        Util.addToListIfHas(response.getBackchannelAuthenticationRequestSigningAlgValuesSupported(), jsonObj, BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG_VALUES_SUPPORTED);
        if (jsonObj.has(BACKCHANNEL_USER_CODE_PAREMETER_SUPPORTED)) {
            response.setBackchannelUserCodeParameterSupported(jsonObj.getBoolean(BACKCHANNEL_USER_CODE_PAREMETER_SUPPORTED));
        }

        // SSA
        if (jsonObj.has(SSA_ENDPOINT)) {
            response.setSsaEndpoint(jsonObj.optString(SSA_ENDPOINT));
        }
    }

    public static OpenIdConfigurationResponse parse(String json) {
        OpenIdConfigurationResponse response = new OpenIdConfigurationResponse();
        parse(json, response);
        return response;
    }
}
