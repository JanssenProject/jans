/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.register.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.json.JsonApplier;
import io.jans.as.model.register.RegisterResponseParam;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.ciba.CIBARegisterClientResponseService;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.ScopeService;
import io.jans.model.GluuAttribute;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.util.security.StringEncrypter;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static io.jans.as.model.register.RegisterRequestParam.*;
import static io.jans.as.model.register.RegisterResponseParam.*;
import static io.jans.as.model.util.StringUtils.implode;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class RegisterJsonService {

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ClientService clientService;

    @Inject
    private ScopeService scopeService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private CIBARegisterClientResponseService cibaRegisterClientResponseService;

    public String jsonObjectToString(JSONObject jsonObject) throws JSONException {
        return jsonObject.toString(4).replace("\\/", "/");
    }

    public JSONObject getJSONObject(Client client) throws JSONException, StringEncrypter.EncryptionException {
        JSONObject responseJsonObject = new JSONObject();

        JsonApplier.getInstance().apply(client, responseJsonObject);
        JsonApplier.getInstance().apply(client.getAttributes(), responseJsonObject);

        Util.addToJSONObjectIfNotNull(responseJsonObject, RegisterResponseParam.CLIENT_ID.toString(), client.getClientId());
        if (isTrue(appConfiguration.getReturnClientSecretOnRead())) {
            Util.addToJSONObjectIfNotNull(responseJsonObject, CLIENT_SECRET.toString(), clientService.decryptSecret(client.getClientSecret()));
        }
        Util.addToJSONObjectIfNotNull(responseJsonObject, RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString(), client.getRegistrationAccessToken());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REGISTRATION_CLIENT_URI.toString(),
                appConfiguration.getRegistrationEndpoint() + "?" +
                        RegisterResponseParam.CLIENT_ID.toString() + "=" + client.getClientId());
        responseJsonObject.put(CLIENT_ID_ISSUED_AT.toString(), client.getClientIdIssuedAt().getTime() / 1000);
        responseJsonObject.put(CLIENT_SECRET_EXPIRES_AT.toString(), client.getClientSecretExpiresAt() != null && client.getClientSecretExpiresAt().getTime() > 0 ?
                client.getClientSecretExpiresAt().getTime() / 1000 : 0);

        Util.addToJSONObjectIfNotNull(responseJsonObject, CLIENT_NAME.toString(), client.getClientName());
        Util.addToJSONObjectIfNotNull(responseJsonObject, LOGO_URI.toString(), client.getLogoUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CLIENT_URI.toString(), client.getClientUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, POLICY_URI.toString(), client.getPolicyUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, TOS_URI.toString(), client.getTosUri());

        Util.addToJSONObjectIfNotNull(responseJsonObject, CLIENT_NAME.toString(), client.getClientNameLocalized());
        Util.addToJSONObjectIfNotNull(responseJsonObject, LOGO_URI.toString(), client.getLogoUriLocalized());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CLIENT_URI.toString(), client.getClientUriLocalized());
        Util.addToJSONObjectIfNotNull(responseJsonObject, POLICY_URI.toString(), client.getPolicyUriLocalized());
        Util.addToJSONObjectIfNotNull(responseJsonObject, TOS_URI.toString(), client.getTosUriLocalized());

        Util.addToJSONObjectIfNotNull(responseJsonObject, REDIRECT_URIS.toString(), client.getRedirectUris());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CLAIMS_REDIRECT_URIS.toString(), client.getClaimRedirectUris());
        Util.addToJSONObjectIfNotNull(responseJsonObject, RESPONSE_TYPES.toString(), ResponseType.toStringArray(client.getResponseTypes()));
        Util.addToJSONObjectIfNotNull(responseJsonObject, GRANT_TYPES.toString(), GrantType.toStringArray(client.getGrantTypes()));
        Util.addToJSONObjectIfNotNull(responseJsonObject, APPLICATION_TYPE.toString(), client.getApplicationType());
        Util.addToJSONObjectIfNotNull(responseJsonObject, CONTACTS.toString(), client.getContacts());
        Util.addToJSONObjectIfNotNull(responseJsonObject, JWKS_URI.toString(), client.getJwksUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SECTOR_IDENTIFIER_URI.toString(), client.getSectorIdentifierUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SUBJECT_TYPE.toString(), client.getSubjectType());
        Util.addToJSONObjectIfNotNull(responseJsonObject, AUTHORIZATION_SIGNED_RESPONSE_ALG.toString(), client.getAttributes().getAuthorizationSignedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, AUTHORIZATION_ENCRYPTED_RESPONSE_ALG.toString(), client.getAttributes().getAuthorizationEncryptedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, AUTHORIZATION_ENCRYPTED_RESPONSE_ENC.toString(), client.getAttributes().getAuthorizationEncryptedResponseEnc());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ID_TOKEN_SIGNED_RESPONSE_ALG.toString(), client.getIdTokenSignedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString(), client.getIdTokenEncryptedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString(), client.getIdTokenEncryptedResponseEnc());
        Util.addToJSONObjectIfNotNull(responseJsonObject, USERINFO_SIGNED_RESPONSE_ALG.toString(), client.getUserInfoSignedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, USERINFO_ENCRYPTED_RESPONSE_ALG.toString(), client.getUserInfoEncryptedResponseAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, USERINFO_ENCRYPTED_RESPONSE_ENC.toString(), client.getUserInfoEncryptedResponseEnc());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUEST_OBJECT_SIGNING_ALG.toString(), client.getRequestObjectSigningAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUEST_OBJECT_ENCRYPTION_ALG.toString(), client.getRequestObjectEncryptionAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUEST_OBJECT_ENCRYPTION_ENC.toString(), client.getRequestObjectEncryptionEnc());
        Util.addToJSONObjectIfNotNull(responseJsonObject, TOKEN_ENDPOINT_AUTH_METHOD.toString(), client.getTokenEndpointAuthMethod());
        Util.addToJSONObjectIfNotNull(responseJsonObject, TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString(), client.getTokenEndpointAuthSigningAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, DEFAULT_MAX_AGE.toString(), client.getDefaultMaxAge());
        Util.addToJSONObjectIfNotNull(responseJsonObject, DEFAULT_ACR_VALUES.toString(), client.getDefaultAcrValues());
        Util.addToJSONObjectIfNotNull(responseJsonObject, INITIATE_LOGIN_URI.toString(), client.getInitiateLoginUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, POST_LOGOUT_REDIRECT_URIS.toString(), client.getPostLogoutRedirectUris());
        Util.addToJSONObjectIfNotNull(responseJsonObject, GROUPS.toString(), client.getGroups());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUEST_URIS.toString(), client.getRequestUris());
        Util.addToJSONObjectIfNotNull(responseJsonObject, AUTHORIZED_ORIGINS.toString(), client.getAuthorizedOrigins());
        Util.addToJSONObjectIfNotNull(responseJsonObject, RPT_AS_JWT.toString(), client.isRptAsJwt());
        Util.addToJSONObjectIfNotNull(responseJsonObject, TLS_CLIENT_AUTH_SUBJECT_DN.toString(), client.getAttributes().getTlsClientAuthSubjectDn());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ALLOW_SPONTANEOUS_SCOPES.toString(), client.getAttributes().getAllowSpontaneousScopes());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SPONTANEOUS_SCOPES.toString(), client.getAttributes().getSpontaneousScopes());
        Util.addToJSONObjectIfNotNull(responseJsonObject, RUN_INTROSPECTION_SCRIPT_BEFORE_JWT_CREATION.toString(), client.getAttributes().getRunIntrospectionScriptBeforeJwtCreation());
        Util.addToJSONObjectIfNotNull(responseJsonObject, KEEP_CLIENT_AUTHORIZATION_AFTER_EXPIRATION.toString(), client.getAttributes().getKeepClientAuthorizationAfterExpiration());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ACCESS_TOKEN_AS_JWT.toString(), client.isAccessTokenAsJwt());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ACCESS_TOKEN_SIGNING_ALG.toString(), client.getAccessTokenSigningAlg());
        Util.addToJSONObjectIfNotNull(responseJsonObject, ACCESS_TOKEN_LIFETIME.toString(), client.getAccessTokenLifetime());
        Util.addToJSONObjectIfNotNull(responseJsonObject, PAR_LIFETIME.toString(), client.getAttributes().getParLifetime());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REQUIRE_PAR.toString(), client.getAttributes().getRequirePar());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SOFTWARE_ID.toString(), client.getSoftwareId());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SOFTWARE_VERSION.toString(), client.getSoftwareVersion());
        Util.addToJSONObjectIfNotNull(responseJsonObject, SOFTWARE_STATEMENT.toString(), client.getSoftwareStatement());
        Util.addToJSONObjectIfNotNull(responseJsonObject, PUBLIC_SUBJECT_IDENTIFIER_ATTRIBUTE.getName(), client.getAttributes().getPublicSubjectIdentifierAttribute());

        if (!Util.isNullOrEmpty(client.getJwks())) {
            Util.addToJSONObjectIfNotNull(responseJsonObject, JWKS.toString(), new JSONObject(client.getJwks()));
        }

        // Logout params
        Util.addToJSONObjectIfNotNull(responseJsonObject, FRONT_CHANNEL_LOGOUT_URI.toString(), client.getFrontChannelLogoutUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString(), client.getFrontChannelLogoutSessionRequired());
        Util.addToJSONObjectIfNotNull(responseJsonObject, BACKCHANNEL_LOGOUT_URI.toString(), client.getAttributes().getBackchannelLogoutUri());
        Util.addToJSONObjectIfNotNull(responseJsonObject, BACKCHANNEL_LOGOUT_SESSION_REQUIRED.toString(), client.getAttributes().getBackchannelLogoutSessionRequired());
        Util.addToJSONObjectIfNotNull(responseJsonObject, REDIRECT_URIS_REGEX.toString(), client.getAttributes().getRedirectUrisRegex());

        // Custom Params
        String[] scopeNames = null;
        String[] scopeDns = client.getScopes();
        if (scopeDns != null) {
            scopeNames = new String[scopeDns.length];
            for (int i = 0; i < scopeDns.length; i++) {
                Scope scope = scopeService.getScopeByDn(scopeDns[i]);
                scopeNames[i] = scope.getId();
            }
        }

        Util.addToJSONObjectIfNotNull(responseJsonObject, SCOPE.toString(), implode(scopeNames, " "));

        String[] claimNames = null;
        String[] claimDns = client.getClaims();
        if (claimDns != null) {
            claimNames = new String[claimDns.length];
            for (int i = 0; i < claimDns.length; i++) {
                GluuAttribute gluuAttribute = attributeService.getAttributeByDn(claimDns[i]);
                claimNames[i] = gluuAttribute.getClaimName();
            }
        }

        putCustomAttributesInResponse(client, responseJsonObject);

        if (claimNames != null && claimNames.length > 0) {
            Util.addToJSONObjectIfNotNull(responseJsonObject, CLAIMS.toString(), implode(claimNames, " "));
        }

        cibaRegisterClientResponseService.updateResponse(responseJsonObject, client);

        return responseJsonObject;
    }

    private void putCustomAttributesInResponse(Client client, JSONObject responseJsonObject) {
        final List<String> allowedCustomAttributeNames = appConfiguration.getDynamicRegistrationCustomAttributes();
        final List<CustomObjectAttribute> customAttributes = client.getCustomAttributes();
        if (allowedCustomAttributeNames == null || allowedCustomAttributeNames.isEmpty() || customAttributes == null) {
            return;
        }

        for (CustomObjectAttribute attribute : customAttributes) {
            if (!allowedCustomAttributeNames.contains(attribute.getName()))
                continue;

            if (attribute.isMultiValued()) {
                Util.addToJSONObjectIfNotNull(responseJsonObject, attribute.getName(), attribute.getValues());
            } else {
                Util.addToJSONObjectIfNotNull(responseJsonObject, attribute.getName(), attribute.getValue());
            }
        }
    }
}
