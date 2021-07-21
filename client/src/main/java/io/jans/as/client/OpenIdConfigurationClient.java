/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static io.jans.as.model.configuration.ConfigurationResponseClaim.ACR_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.AUTHORIZATION_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.BACKCHANNEL_AUTHENTICATION_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.BACKCHANNEL_LOGOUT_SESSION_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.BACKCHANNEL_LOGOUT_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.BACKCHANNEL_TOKEN_DELIVERY_MODES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.BACKCHANNEL_USER_CODE_PAREMETER_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.CHECK_SESSION_IFRAME;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.CLAIMS_LOCALES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.CLAIMS_PARAMETER_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.CLAIMS_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.CLAIM_TYPES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.CLIENT_INFO_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.DEVICE_AUTHZ_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.DISPLAY_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.END_SESSION_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.FRONTCHANNEL_LOGOUT_SESSION_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.FRONTCHANNEL_LOGOUT_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.GRANT_TYPES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.ID_GENERATION_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.INTROSPECTION_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.ISSUER;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.JWKS_URI;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.OP_POLICY_URI;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.OP_TOS_URI;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.REGISTRATION_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.REQUEST_PARAMETER_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.REQUEST_URI_PARAMETER_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.REQUIRE_PAR;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.REQUIRE_REQUEST_URI_REGISTRATION;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.RESPONSE_MODES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.RESPONSE_TYPES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.REVOCATION_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.SCOPES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.SCOPE_TO_CLAIMS_MAPPING;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.SERVICE_DOCUMENTATION;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.SESSION_REVOCATION_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.SUBJECT_TYPES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.TLS_CLIENT_CERTIFICATE_BOUND_ACCESS_TOKENS;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.TOKEN_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.UI_LOCALES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.USER_INFO_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.USER_INFO_SIGNING_ALG_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.PAR_ENDPOINT;

/**
 * Encapsulates functionality to make OpenId Configuration request calls to an authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version August 22, 2019
 */
public class OpenIdConfigurationClient extends BaseClient<OpenIdConfigurationRequest, OpenIdConfigurationResponse> {

    private static final Logger LOG = Logger.getLogger(OpenIdConfigurationClient.class);

    private static final String mediaTypes = String.join(",", MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON);

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

    public OpenIdConfigurationResponse execOpenIdConfiguration() throws IOException {
        initClientRequest();

        return _execOpenIdConfiguration();
    }

    @Deprecated
    public OpenIdConfigurationResponse execOpenIdConfiguration(ClientExecutor executor) throws IOException {
        this.clientRequest = new ClientRequest(getUrl(), executor);

        return _execOpenIdConfiguration();
    }

    /**
     * Executes the call to the REST Service requesting the OpenID Configuration and processes the response.
     *
     * @return The service response.
     */
    private OpenIdConfigurationResponse _execOpenIdConfiguration() throws IOException {
        setRequest(new OpenIdConfigurationRequest());

        // Prepare request parameters
        clientRequest.accept(mediaTypes);
        clientRequest.setHttpMethod(getHttpMethod());

        // Support AWS LB
        clientRequest.followRedirects(true);

        // Call REST Service and handle response
        String entity = null;
        try {
            clientResponse = clientRequest.get(String.class);
            int status = clientResponse.getStatus();

            setResponse(new OpenIdConfigurationResponse(status));

            entity = clientResponse.getEntity(String.class);
            getResponse().setEntity(entity);
            getResponse().setHeaders(clientResponse.getMetadata());
            parse(entity, getResponse());
        } catch (JSONException e) {
            LOG.error("There is an error in the JSON response. Check if there is a syntax error in the JSON response or there is a wrong key", e);
            if (entity != null) {
            	LOG.error("Invalid JSON: " + entity);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw e;
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

        if (jsonObj.has(ISSUER)) {
            response.setIssuer(jsonObj.getString(ISSUER));
        }
        if (jsonObj.has(AUTHORIZATION_ENDPOINT)) {
            response.setAuthorizationEndpoint(jsonObj.getString(AUTHORIZATION_ENDPOINT));
        }
        if (jsonObj.has(TOKEN_ENDPOINT)) {
            response.setTokenEndpoint(jsonObj.getString(TOKEN_ENDPOINT));
        }
        if (jsonObj.has(REVOCATION_ENDPOINT)) {
            response.setRevocationEndpoint(jsonObj.getString(REVOCATION_ENDPOINT));
        }
        if (jsonObj.has(SESSION_REVOCATION_ENDPOINT)) {
            response.setSessionRevocationEndpoint(jsonObj.getString(SESSION_REVOCATION_ENDPOINT));
        }
        if (jsonObj.has(USER_INFO_ENDPOINT)) {
            response.setUserInfoEndpoint(jsonObj.getString(USER_INFO_ENDPOINT));
        }
        if (jsonObj.has(CLIENT_INFO_ENDPOINT)) {
            response.setClientInfoEndpoint(jsonObj.getString(CLIENT_INFO_ENDPOINT));
        }
        if (jsonObj.has(CHECK_SESSION_IFRAME)) {
            response.setCheckSessionIFrame(jsonObj.getString(CHECK_SESSION_IFRAME));
        }
        if (jsonObj.has(END_SESSION_ENDPOINT)) {
            response.setEndSessionEndpoint(jsonObj.getString(END_SESSION_ENDPOINT));
        }
        if (jsonObj.has(JWKS_URI)) {
            response.setJwksUri(jsonObj.getString(JWKS_URI));
        }
        if (jsonObj.has(REGISTRATION_ENDPOINT)) {
            response.setRegistrationEndpoint(jsonObj.getString(REGISTRATION_ENDPOINT));
        }
        if (jsonObj.has(ID_GENERATION_ENDPOINT)) {
            response.setIdGenerationEndpoint(jsonObj.getString(ID_GENERATION_ENDPOINT));
        }
        if (jsonObj.has(INTROSPECTION_ENDPOINT)) {
            response.setIntrospectionEndpoint(jsonObj.getString(INTROSPECTION_ENDPOINT));
        }
        if (jsonObj.has(PAR_ENDPOINT)) {
            response.setParEndpoint(jsonObj.getString(PAR_ENDPOINT));
        }
        if (jsonObj.has(REQUIRE_PAR)) {
            response.setRequirePar(jsonObj.getBoolean(REQUIRE_PAR));
        }
        if (jsonObj.has(DEVICE_AUTHZ_ENDPOINT)) {
            response.setDeviceAuthzEndpoint(jsonObj.getString(DEVICE_AUTHZ_ENDPOINT));
        }
        if (jsonObj.has(SCOPE_TO_CLAIMS_MAPPING)) {
            response.setScopeToClaimsMapping(OpenIdConfigurationResponse.parseScopeToClaimsMapping(jsonObj.getJSONArray(SCOPE_TO_CLAIMS_MAPPING)));
        }
        Util.addToListIfHas(response.getScopesSupported(), jsonObj, SCOPES_SUPPORTED);
        Util.addToListIfHas(response.getResponseTypesSupported(), jsonObj, RESPONSE_TYPES_SUPPORTED);
        Util.addToListIfHas(response.getResponseModesSupported(), jsonObj, RESPONSE_MODES_SUPPORTED);
        Util.addToListIfHas(response.getGrantTypesSupported(), jsonObj, GRANT_TYPES_SUPPORTED);
        Util.addToListIfHas(response.getAcrValuesSupported(), jsonObj, ACR_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getSubjectTypesSupported(), jsonObj, SUBJECT_TYPES_SUPPORTED);
        Util.addToListIfHas(response.getUserInfoSigningAlgValuesSupported(), jsonObj, USER_INFO_SIGNING_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getUserInfoEncryptionAlgValuesSupported(), jsonObj, USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getUserInfoEncryptionEncValuesSupported(), jsonObj, USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getIdTokenSigningAlgValuesSupported(), jsonObj, ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getIdTokenEncryptionAlgValuesSupported(), jsonObj, ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getIdTokenEncryptionEncValuesSupported(), jsonObj, ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getRequestObjectSigningAlgValuesSupported(), jsonObj, REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getRequestObjectEncryptionAlgValuesSupported(), jsonObj, REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getRequestObjectEncryptionEncValuesSupported(), jsonObj, REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getTokenEndpointAuthMethodsSupported(), jsonObj, TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED);
        Util.addToListIfHas(response.getTokenEndpointAuthSigningAlgValuesSupported(), jsonObj, TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getDisplayValuesSupported(), jsonObj, DISPLAY_VALUES_SUPPORTED);
        Util.addToListIfHas(response.getClaimTypesSupported(), jsonObj, CLAIM_TYPES_SUPPORTED);
        Util.addToListIfHas(response.getClaimsSupported(), jsonObj, CLAIMS_SUPPORTED);
        if (jsonObj.has(SERVICE_DOCUMENTATION)) {
            response.setServiceDocumentation(jsonObj.getString(SERVICE_DOCUMENTATION));
        }
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
        if (jsonObj.has(BACKCHANNEL_LOGOUT_SUPPORTED)) {
            response.setBackchannelLogoutSupported(jsonObj.optBoolean(BACKCHANNEL_LOGOUT_SUPPORTED));
        }
        if (jsonObj.has(BACKCHANNEL_LOGOUT_SESSION_SUPPORTED)) {
            response.setBackchannelLogoutSessionSupported(jsonObj.optBoolean(BACKCHANNEL_LOGOUT_SESSION_SUPPORTED));
        }
        if (jsonObj.has(REQUIRE_REQUEST_URI_REGISTRATION)) {
            response.setRequireRequestUriRegistration(jsonObj.getBoolean(REQUIRE_REQUEST_URI_REGISTRATION));
        }
        if (jsonObj.has(OP_POLICY_URI)) {
            response.setOpPolicyUri(jsonObj.getString(OP_POLICY_URI));
        }
        if (jsonObj.has(OP_TOS_URI)) {
            response.setOpTosUri(jsonObj.getString(OP_TOS_URI));
        }

        // CIBA
        if (jsonObj.has(BACKCHANNEL_AUTHENTICATION_ENDPOINT)) {
            response.setBackchannelAuthenticationEndpoint(jsonObj.getString(BACKCHANNEL_AUTHENTICATION_ENDPOINT));
        }
        Util.addToListIfHas(response.getBackchannelTokenDeliveryModesSupported(), jsonObj, BACKCHANNEL_TOKEN_DELIVERY_MODES_SUPPORTED);
        Util.addToListIfHas(response.getBackchannelAuthenticationRequestSigningAlgValuesSupported(), jsonObj, BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG_VALUES_SUPPORTED);
        if (jsonObj.has(BACKCHANNEL_USER_CODE_PAREMETER_SUPPORTED)) {
            response.setBackchannelUserCodeParameterSupported(jsonObj.getBoolean(BACKCHANNEL_USER_CODE_PAREMETER_SUPPORTED));
        }
    }

    public static OpenIdConfigurationResponse parse(String json) {
        OpenIdConfigurationResponse response = new OpenIdConfigurationResponse();
        parse(json, response);
        return response;
    }
}
