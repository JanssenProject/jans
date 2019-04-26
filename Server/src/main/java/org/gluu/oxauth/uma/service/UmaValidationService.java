/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.service;

import com.google.common.base.Joiner;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.AuthorizationGrant;
import org.gluu.oxauth.model.common.AuthorizationGrantList;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.config.WebKeysConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.jwk.JSONWebKey;
import org.gluu.oxauth.model.jws.RSASigner;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.jwt.JwtHeaderName;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.uma.ClaimTokenFormatType;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.gluu.oxauth.model.uma.UmaPermissionList;
import org.gluu.oxauth.model.uma.UmaScopeType;
import org.gluu.oxauth.model.uma.persistence.UmaPermission;
import org.gluu.oxauth.model.uma.persistence.UmaResource;
import org.gluu.oxauth.service.ClientService;
import org.gluu.oxauth.service.RedirectionUriService;
import org.gluu.oxauth.service.token.TokenService;
import org.gluu.oxauth.uma.authorization.UmaPCT;
import org.gluu.oxauth.uma.authorization.UmaRPT;
import org.gluu.oxauth.uma.authorization.UmaWebException;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.persist.exception.EntryPersistenceException;
import org.gluu.util.StringHelper;
import org.oxauth.persistence.model.Scope;
import org.python.google.common.base.Function;
import org.python.google.common.collect.Iterables;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.util.*;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.gluu.oxauth.model.uma.UmaErrorResponseType.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 04/02/2013
 */
@Named
@Stateless
public class UmaValidationService {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private TokenService tokenService;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private UmaResourceService resourceService;

    @Inject
    private UmaScopeService umaScopeService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private UmaPermissionService permissionService;

    @Inject
    private UmaPctService pctService;

    @Inject
    private UmaRptService rptService;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private ClientService clientService;

    @Inject
    private UmaExpressionService expressionService;

    public AuthorizationGrant assertHasProtectionScope(String authorization) {
        return validateAuthorization(authorization, UmaScopeType.PROTECTION);
    }

    private AuthorizationGrant validateAuthorization(String authorization, UmaScopeType umaScopeType) {
        log.trace("Validate authorization: {}", authorization);
        if (StringHelper.isEmpty(authorization)) {
            errorResponseFactory.throwUmaWebApplicationException(UNAUTHORIZED, UNAUTHORIZED_CLIENT);
        }

        String token = tokenService.getTokenFromAuthorizationParameter(authorization);
        if (StringHelper.isEmpty(token)) {
            log.debug("Token is invalid");
            errorResponseFactory.throwUmaWebApplicationException(UNAUTHORIZED, UNAUTHORIZED_CLIENT);
        }

        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(token);
        if (authorizationGrant == null) {
            errorResponseFactory.throwUmaWebApplicationException(UNAUTHORIZED, ACCESS_DENIED);
        }

        if (!authorizationGrant.isValid()) {
            errorResponseFactory.throwUmaWebApplicationException(UNAUTHORIZED, INVALID_TOKEN);
        }

        Set<String> scopes = authorizationGrant.getScopes();
        if (!scopes.contains(umaScopeType.getValue())) {
            errorResponseFactory.throwUmaWebApplicationException(Response.Status.NOT_ACCEPTABLE, INVALID_CLIENT_SCOPE);
        }
        return authorizationGrant;
    }

    public UmaRPT validateRPT(String rptCode) {
        if (StringUtils.isNotBlank(rptCode)) {
            UmaRPT rpt = rptService.getRPTByCode(rptCode);
            if (rpt != null) {
                rpt.checkExpired();
                if (rpt.isValid()) {
                    return rpt;
                } else {
                    log.error("RPT is not valid. Revoked: " + rpt.isRevoked() + ", Expired: " + rpt.isExpired() + ", rptCode: " + rptCode);
                }
            } else {
                log.error("RPT is null, rptCode: " + rptCode);
            }

            errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_RPT);
        }
        return null;
    }

    public void validatePermissions(List<UmaPermission> permissions) {
        for (UmaPermission permission : permissions) {
            validatePermission(permission);
        }
    }

    public void validatePermission(UmaPermission permission) {
        if (permission == null || "invalidated".equalsIgnoreCase(permission.getStatus())) {
            log.error("Permission is null or otherwise invalidated. Status: " + (permission != null ? permission.getStatus() : ""));
            errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_TICKET);
        }

        permission.checkExpired();
        if (!permission.isValid()) {
            log.error("Permission is not valid.");
            errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, EXPIRED_TICKET);
        }
    }

    public void validatePermissions(UmaPermissionList permissions) {
        for (org.gluu.oxauth.model.uma.UmaPermission permission : permissions) {
            validatePermission(permission);
        }
    }

    public void validatePermission(org.gluu.oxauth.model.uma.UmaPermission permission) {
        String resourceId = permission.getResourceId();
        if (StringHelper.isEmpty(resourceId)) {
            log.error("Resource id is empty");
            errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_ID);
        }

        try {
            UmaResource resource = resourceService.getResourceById(resourceId);
            if (resource == null) {
                log.error("Resource isn't registered or there are two resources with same Id");
                errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_ID);
                return;
            }

            final List<String> scopeUrls = umaScopeService.getScopeIdsByDns(resource.getScopes());
            if (!scopeUrls.containsAll(permission.getScopes())) {
                log.error("At least one of the scope isn't registered");
                errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_SCOPE);
            } else {
                return;
            }
        } catch (EntryPersistenceException ex) {
            log.error(ex.getMessage(), ex);
        }

        log.error("Resource isn't registered");
        errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_ID);
    }

    public void validateGrantType(String grantType) {
        log.trace("Validate grantType: {}", grantType);

        if (!GrantType.OXAUTH_UMA_TICKET.getValue().equals(grantType)) {
            errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_ID);
        }
    }

    public List<UmaPermission> validateTicket(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            log.error("Ticket is null or blank.");
            errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_TICKET);
        }

        List<UmaPermission> permissions = permissionService.getPermissionsByTicket(ticket);
        if (permissions == null || permissions.isEmpty()) {
            log.error("Unable to find permissions registered for given ticket:" + ticket);
            errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_TICKET);
        }
        return permissions;
    }

    public List<UmaPermission> validateTicketWithRedirect(String ticket, String claimsRedirectUri, String state) {
        if (StringUtils.isBlank(ticket)) {
            log.error("Ticket is null or blank.");
            throw new UmaWebException(claimsRedirectUri, errorResponseFactory, INVALID_TICKET, state);
        }

        List<UmaPermission> permissions = permissionService.getPermissionsByTicket(ticket);
        if (permissions == null || permissions.isEmpty()) {
            log.error("Unable to find permissions registered for given ticket:" + ticket);
            throw new UmaWebException(claimsRedirectUri, errorResponseFactory, INVALID_TICKET, state);
        }
        return permissions;
    }

    public Jwt validateClaimToken(String claimToken, String claimTokenFormat) {
        if (StringUtils.isNotBlank(claimToken)) {
            if (!ClaimTokenFormatType.isValueValid(claimTokenFormat)) {
                log.error("claim_token_format is unsupported. Supported format is http://openid.net/specs/openid-connect-core-1_0.html#IDToken");
                errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_CLAIM_TOKEN_FORMAT);
            }

            try {
                final Jwt idToken = Jwt.parse(claimToken);
                if (idToken != null) {
                    if (ServerUtil.isTrue(appConfiguration.getUmaValidateClaimToken()) && !isIdTokenValid(idToken)) {
                        log.error("claim_token validation failed.");
                        errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_CLAIM_TOKEN);
                    }
                    return idToken;
                }
            } catch (Exception e) {
                log.error("Failed to parse claim_token as valid id_token.", e);
            }

            errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_CLAIM_TOKEN);
        } else if (StringUtils.isNotBlank(claimTokenFormat)) {
            log.error("claim_token is blank but claim_token_format is not blank. Both must be blank or both must be not blank");
            errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_CLAIM_TOKEN);
        }
        return null;
    }

    public boolean isIdTokenValid(Jwt idToken) {
        try {
            final String issuer = idToken.getClaims().getClaimAsString(JwtClaimName.ISSUER);
            //final String nonceFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.NONCE);
            //final String audienceFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.AUDIENCE);

            final Date expiresAt = idToken.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
            final Date now = new Date();
            if (now.after(expiresAt)) {
                log.error("ID Token is expired. (It is after " + now + ").");
                return false;
            }

            // 1. validate issuer
            if (!issuer.equals(appConfiguration.getIssuer())) {
                log.error("ID Token issuer is invalid. Token issuer: " + issuer + ", server issuer: " + appConfiguration.getIssuer());
                return false;
            }

            // 2. validate signature
            final String kid = idToken.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
            final String algorithm = idToken.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM);
            RSAPublicKey publicKey = getPublicKey(kid);
            if (publicKey != null) {
                RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.fromString(algorithm), publicKey);
                boolean signature = rsaSigner.validate(idToken);
                if (signature) {
                    log.debug("ID Token is successfully validated.");
                    return true;
                }
                log.error("ID Token signature is invalid.");
            } else {
                log.error("Failed to get RSA public key.");
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to validate id_token. Message: " + e.getMessage(), e);
            return false;
        }
    }

    private RSAPublicKey getPublicKey(String kid) {
        JSONWebKey key = webKeysConfiguration.getKey(kid);
        if (key != null) {
            switch (key.getKty()) {
                case RSA:
                    return new RSAPublicKey(
                            key.getN(),
                            key.getE());
            }
        }
        return null;
    }

    public UmaPCT validatePct(String pctCode) {
        if (StringUtils.isNotBlank(pctCode)) {
            UmaPCT pct = pctService.getByCode(pctCode);

            if (pct != null) {
                pct.checkExpired();
                if (pct.isValid()) {
                    log.trace("PCT is validated successfully, pct: " + pctCode);
                    return pct;
                } else {
                    log.error("PCT is not valid. Revoked: " + pct.isRevoked() + ", Expired: " + pct.isExpired() + ", pctCode: " + pctCode);
                }
            } else {
                log.error("Failed to find PCT with pctCode: " + pctCode);
            }

            errorResponseFactory.throwUmaWebApplicationException(UNAUTHORIZED, INVALID_PCT);
        }
        return null;
    }

    /**
     * @param scope scope string from token request
     * @param permissions permissions
     * @return map of loaded scope and boolean, true - if client requested scope and false if it is permission ticket scope
     */
    public Map<Scope, Boolean> validateScopes(String scope, List<UmaPermission> permissions) {
        scope = ServerUtil.urlDecode(scope);
        final String[] scopesRequested = StringUtils.isNotBlank(scope) ? scope.split(" ") : new String[0];

        final Map<Scope, Boolean> result = new HashMap<Scope, Boolean>();

        if (ArrayUtils.isNotEmpty(scopesRequested)) {
            for (Scope s : umaScopeService.getScopesByIds(Arrays.asList(scopesRequested))) {
                result.put(s, true);
            }
        }
        for (UmaPermission permission : permissions) {
            for (Scope s : umaScopeService.getScopesByDns(permission.getScopeDns())) {
                result.put(s, false);
            }
        }
        if (result.isEmpty()) {
            log.error("There are no any scopes requested in give request.");
            throw new UmaWebException(BAD_REQUEST, errorResponseFactory, UmaErrorResponseType.INVALID_RESOURCE_SCOPE);
        }
        log.trace("CandidateGrantedScopes: " + Joiner.on(", ").join(Iterables.transform(result.keySet(), new Function<Scope, String>() {
            @Override
            public String apply(Scope scope) {
                return scope.getId();
            }
        })));
        return result;
    }

    public void validateScopeExpression(String scopeExpression) {
        if (StringUtils.isNotBlank(scopeExpression) && !expressionService.isExpressionValid(scopeExpression)) {
            log.error("Scope expression is invalid. Expression: " + scopeExpression);
            throw new UmaWebException(BAD_REQUEST, errorResponseFactory, UmaErrorResponseType.INVALID_RESOURCE_SCOPE);
        }
    }

    public Client validateClientAndClaimsRedirectUri(String clientId, String claimsRedirectUri, String state) {
        if (StringUtils.isBlank(clientId)) {
            log.error("Invalid clientId: {}", clientId);
            throw new UmaWebException(BAD_REQUEST, errorResponseFactory, UmaErrorResponseType.INVALID_CLIENT_ID);
        }
        Client client = clientService.getClient(clientId);
        if (client == null) {
            log.error("Failed to find client with client_id: {}", clientId);
            throw new UmaWebException(BAD_REQUEST, errorResponseFactory, UmaErrorResponseType.INVALID_CLIENT_ID);
        }

        if (StringUtils.isNotBlank(claimsRedirectUri)) {
            if (ArrayUtils.isEmpty(client.getClaimRedirectUris())) {
                log.error("Client does not have claims_redirect_uri specified, clientId: " + clientId);
                throw new UmaWebException(BAD_REQUEST, errorResponseFactory, UmaErrorResponseType.INVALID_CLAIMS_REDIRECT_URI);
            }

            String equalRedirectUri = getEqualRedirectUri(claimsRedirectUri, client.getClaimRedirectUris());
            if (equalRedirectUri != null) {
                log.trace("Found match for claims_redirect_uri : " + equalRedirectUri);
                return client;
            } else {
                log.trace("Failed to find match for claims_redirect_uri : " + claimsRedirectUri + ", client claimRedirectUris: " + Arrays.toString(client.getClaimRedirectUris()));
            }
        } else {
            log.trace("claims_redirect_uri is blank");
            if (client.getClaimRedirectUris() != null && client.getClaimRedirectUris().length == 1) {
                log.trace("claims_redirect_uri is blank and only one claims_redirect_uri is registered.");
                return client;
            }
        }

        if (StringUtils.isBlank(claimsRedirectUri)) {
            log.error("claims_redirect_uri is blank and there is none or more then one registered claims_redirect_uri for clientId: " + clientId);
            throw new UmaWebException(BAD_REQUEST, errorResponseFactory, UmaErrorResponseType.INVALID_CLAIMS_REDIRECT_URI);
        }

        throw new UmaWebException(claimsRedirectUri, errorResponseFactory, INVALID_CLAIMS_REDIRECT_URI, state);
    }

    private String getEqualRedirectUri(String redirectUri, String[] clientRedirectUris) {
        final String redirectUriWithoutParams = RedirectionUriService.uriWithoutParams(redirectUri);

        for (String uri : clientRedirectUris) {
            log.debug("Comparing {} == {}", uri, redirectUri);
            if (uri.equals(redirectUri)) { // compare complete uri
                return redirectUri;
            }

            String uriWithoutParams = RedirectionUriService.uriWithoutParams(uri);
            final Map<String, String> params = RedirectionUriService.getParams(uri);

            if ((uriWithoutParams.equals(redirectUriWithoutParams) && params.size() == 0 && RedirectionUriService.getParams(redirectUri).size() == 0) ||
                    uriWithoutParams.equals(redirectUriWithoutParams) && params.size() > 0 && RedirectionUriService.compareParams(redirectUri, uri)) {
                return redirectUri;
            }
        }
        return null;
    }

    public String[] validatesGatheringScriptNames(String scriptNamesAsString, String claimsRedirectUri, String state) {
        if (StringUtils.isNotBlank(scriptNamesAsString)) {
            final String[] scriptNames = scriptNamesAsString.split(" ");
            if (ArrayUtils.isNotEmpty(scriptNames)) {
                return scriptNames;
            }
        }
        throw new UmaWebException(claimsRedirectUri, errorResponseFactory, INVALID_CLAIMS_GATHERING_SCRIPT_NAME, state);
    }

    public void validateRestrictedByClient(String patClientDn, String rsId) {
        if (ServerUtil.isTrue(appConfiguration.getUmaRestrictResourceToAssociatedClient())) {
            final List<String> clients = resourceService.getResourceById(rsId).getClients();
            if (!clients.contains(patClientDn)) {
                log.error("Access to resource is denied because resource associated client does not match PAT client (it can be switched off if set umaRestrictResourceToAssociatedClient oxauth configuration property to false). Associated clients: " + clients + ", PAT client: " + patClientDn);
                throw new UmaWebException(Response.Status.FORBIDDEN, errorResponseFactory, ACCESS_DENIED);
            }
        }
    }

    public void validateResource(org.gluu.oxauth.model.uma.UmaResource resource) {
        validateScopeExpression(resource.getScopeExpression());

        List<String> scopeDNs = umaScopeService.getScopeDNsByIdsAndAddToLdapIfNeeded(resource.getScopes());
        if (scopeDNs.isEmpty() && StringUtils.isBlank(resource.getScopeExpression()) ) {
            log.error("Invalid resource. Both `scope` and `scope_expression` are blank.");
            throw new UmaWebException(BAD_REQUEST, errorResponseFactory, UmaErrorResponseType.INVALID_RESOURCE_SCOPE);
        }
    }
}
