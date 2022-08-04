/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.service;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.KeyType;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.uma.ClaimTokenFormatType;
import io.jans.as.model.uma.UmaErrorResponseType;
import io.jans.as.model.uma.UmaPermissionList;
import io.jans.as.model.uma.UmaScopeType;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.RedirectionUriService;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.uma.authorization.UmaPCT;
import io.jans.as.server.uma.authorization.UmaRPT;
import io.jans.as.server.uma.authorization.UmaWebException;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.util.StringHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.python.google.common.collect.Iterables;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.jans.as.model.uma.UmaErrorResponseType.ACCESS_DENIED;
import static io.jans.as.model.uma.UmaErrorResponseType.EXPIRED_TICKET;
import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_CLAIMS_GATHERING_SCRIPT_NAME;
import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_CLAIMS_REDIRECT_URI;
import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_CLAIM_TOKEN;
import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_CLAIM_TOKEN_FORMAT;
import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_CLIENT_SCOPE;
import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_PCT;
import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_RESOURCE_ID;
import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_RPT;
import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_SCOPE;
import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_TICKET;
import static io.jans.as.model.uma.UmaErrorResponseType.UNAUTHORIZED_CLIENT;
import static io.jans.as.model.util.Util.escapeLog;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

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
            throw errorResponseFactory.createWebApplicationException(UNAUTHORIZED, UNAUTHORIZED_CLIENT, "Authorization header is blank.");
        }

        String token = tokenService.getToken(authorization);
        if (StringHelper.isEmpty(token)) {
            log.debug("Token is invalid.");
            throw errorResponseFactory.createWebApplicationException(UNAUTHORIZED, UNAUTHORIZED_CLIENT, "Token is invalid.");
        }

        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(token);
        if (authorizationGrant == null) {
            throw errorResponseFactory.createWebApplicationException(UNAUTHORIZED, ACCESS_DENIED, "Unable to find authorization grant by token.");
        }

        Set<String> scopes = authorizationGrant.getScopes();
        if (!scopes.contains(umaScopeType.getValue())) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.NOT_ACCEPTABLE, INVALID_CLIENT_SCOPE, "Client does not have scope: " + umaScopeType.getValue());
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
                    log.error("RPT is not valid. Revoked: {}, Expired: {}, rptCode: {}", rpt.isRevoked(), rpt.isExpired(), rptCode);
                    throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_RPT, String.format("RPT is not valid. Revoked: %s, Expired: %s, rptCode: %s", rpt.isRevoked(), rpt.isExpired(), rptCode));
                }
            } else {
                log.error("RPT is null, rptCode: {}", rptCode);
                throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_RPT, "RPT is null, rptCode: " + rptCode);
            }
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
            log.error("Permission is null or otherwise invalidated. Status: {}", (permission != null ? permission.getStatus() : "No permissions."));
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_TICKET, "Permission is null or otherwise invalidated. Status: " + (permission != null ? permission.getStatus() : "No permissions."));
        }

        permission.checkExpired();
        if (!permission.isValid()) {
            log.error("Permission is not valid.");
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, EXPIRED_TICKET, "Permission is not valid.");
        }
    }

    public void validatePermissions(UmaPermissionList permissions, Client client) {
        for (io.jans.as.model.uma.UmaPermission permission : permissions) {
            validatePermission(permission, client);
        }
    }

    public void validatePermission(io.jans.as.model.uma.UmaPermission permission, Client client) {
        String resourceId = permission.getResourceId();
        if (StringHelper.isEmpty(resourceId)) {
            log.error("Resource id is empty");
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_ID, "Resource id is empty");
        }

        try {
            UmaResource resource = resourceService.getResourceById(resourceId);
            if (resource == null) {
                log.error("Resource isn't registered or there are two resources with same Id");
                throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_ID, "Resource is not registered.");
            }

            for (String s : permission.getScopes()) {
                if (resource.getScopes().contains(s)) {
                    continue;
                }

                final Scope spontaneousScope = umaScopeService.getOrCreate(client, s, Sets.newHashSet(umaScopeService.getScopeIdsByDns(resource.getScopes())));
                if (spontaneousScope == null) {
                    log.error("Scope isn't registered and is not allowed by spontaneous scopes. Scope: {}", s);
                    throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_SCOPE, "At least one of the scopes isn't registered");
                }
            }
            return;
        } catch (EntryPersistenceException ex) {
            log.error(ex.getMessage(), ex);
        }

        log.error("Resource isn't registered");
        throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_ID, "Resource isn't registered");
    }

    public void validateGrantType(String grantType) {
        log.trace("Validate grantType: {}", grantType);

        if (!GrantType.OXAUTH_UMA_TICKET.getValue().equals(grantType)) {
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_ID, "No required grant_type: " + GrantType.OXAUTH_UMA_TICKET.getValue());
        }
    }

    public List<UmaPermission> validateTicket(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            log.error("Ticket is blank.");
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_TICKET, "Ticket is null or blank.");
        }

        List<UmaPermission> permissions = permissionService.getPermissionsByTicket(ticket);
        if (permissions == null || permissions.isEmpty()) {
            if (log.isErrorEnabled()) {
                log.error("Unable to find permissions registered for given ticket: {}", escapeLog(ticket));
            }
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_TICKET, "Unable to find permissions registered for given ticket:" + ticket);
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
            if (log.isErrorEnabled()) {
                log.error("Unable to find permissions registered for given ticket:{}", escapeLog(ticket));
            }
            throw new UmaWebException(claimsRedirectUri, errorResponseFactory, INVALID_TICKET, state);
        }
        return permissions;
    }

    public Jwt validateClaimToken(String claimToken, String claimTokenFormat) {
        if (StringUtils.isNotBlank(claimToken)) {
            if (!ClaimTokenFormatType.isValueValid(claimTokenFormat)) {
                log.error("claim_token_format is unsupported. Supported format is http://openid.net/specs/openid-connect-core-1_0.html#IDToken");
                throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_CLAIM_TOKEN_FORMAT, "claim_token_format is unsupported. Supported format is http://openid.net/specs/openid-connect-core-1_0.html#IDToken");
            }

            try {
                final Jwt idToken = Jwt.parse(claimToken);
                if (isTrue(appConfiguration.getUmaValidateClaimToken()) && !isIdTokenValid(idToken)) {
                    log.error("claim_token validation failed.");
                    throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_CLAIM_TOKEN, "claim_token validation failed.");
                }
                return idToken;
            } catch (Exception e) {
                log.error("Failed to parse claim_token as valid id_token.", e);
                throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_CLAIM_TOKEN, "Failed to parse claim_token as valid id_token.");
            }
        } else if (StringUtils.isNotBlank(claimTokenFormat)) {
            log.error("claim_token is blank but claim_token_format is not blank. Both must be blank or both must be not blank");
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_CLAIM_TOKEN, "claim_token is blank but claim_token_format is not blank. Both must be blank or both must be not blank");
        }
        return null;
    }

    public boolean isIdTokenValid(Jwt idToken) {
        try {
            final String issuer = idToken.getClaims().getClaimAsString(JwtClaimName.ISSUER);

            final Date expiresAt = idToken.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
            final Date now = new Date();
            if (now.after(expiresAt)) {
                log.error("ID Token is expired. (It is after {}).", now);
                return false;
            }

            // 1. validate issuer
            if (!issuer.equals(appConfiguration.getIssuer())) {
                log.error("ID Token issuer is invalid. Token issuer: {}, server issuer: {}", issuer, appConfiguration.getIssuer());
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
        if (key != null && key.getKty() == KeyType.RSA) {
            return new RSAPublicKey(key.getN(), key.getE());
        }
        return null;
    }

    public UmaPCT validatePct(String pctCode) {
        if (StringUtils.isNotBlank(pctCode)) {
            UmaPCT pct = pctService.getByCode(pctCode);

            if (pct != null) {
                pct.checkExpired();
                if (pct.isValid()) {
                    log.trace("PCT is validated successfully, pct: {}", pctCode);
                    return pct;
                } else {
                    log.error("PCT is not valid. Revoked: {}, Expired: {}, pctCode: {}", pct.isRevoked(), pct.isExpired(), pctCode);
                    throw errorResponseFactory.createWebApplicationException(UNAUTHORIZED, INVALID_PCT, "PCT is not valid. Revoked: " + pct.isRevoked() + ", Expired: " + pct.isExpired() + ", pctCode: " + pctCode);
                }
            } else {
                log.error("Failed to find PCT with pctCode: {}", pctCode);
                throw errorResponseFactory.createWebApplicationException(UNAUTHORIZED, INVALID_PCT, "Failed to find PCT with pctCode: " + pctCode);
            }
        }
        return null;
    }

    /**
     * @param scope       scope string from token request
     * @param permissions permissions
     * @return map of loaded scope and boolean, true - if client requested scope and false if it is permission ticket scope
     */
    public Map<Scope, Boolean> validateScopes(String scope, List<UmaPermission> permissions, Client client) {
        scope = ServerUtil.urlDecode(scope);
        final String[] scopesRequested = StringUtils.isNotBlank(scope) ? scope.split(" ") : new String[0];

        final Map<Scope, Boolean> result = new HashMap<>();

        if (ArrayUtils.isNotEmpty(scopesRequested)) {
            final Set<String> resourceScopes = resourceService.getResourceScopes(permissions.stream().map(UmaPermission::getResourceId).collect(Collectors.toSet()));
            for (String scopeId : scopesRequested) {
                final Scope ldapScope = umaScopeService.getOrCreate(client, scopeId, resourceScopes);
                if (ldapScope != null) {
                    result.put(ldapScope, true);
                } else {
                    log.trace("Skip requested scope because it's not allowed, scope: {}", scopeId);
                }
            }
        }
        for (UmaPermission permission : permissions) {
            for (Scope s : umaScopeService.getScopesByDns(permission.getScopeDns())) {
                result.put(s, false);
            }
        }
        if (result.isEmpty()) {
            log.error("There are no any scopes requested in the request.");
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, UmaErrorResponseType.INVALID_SCOPE, "There are no any scopes requested in give request.");
        }
        if (log.isTraceEnabled()) {
            log.trace("CandidateGrantedScopes: {}", Joiner.on(", ").join(Iterables.transform(result.keySet(), Scope::getId)));
        }
        return result;
    }

    public void validateScopeExpression(String scopeExpression) {
        if (StringUtils.isNotBlank(scopeExpression) && !expressionService.isExpressionValid(scopeExpression)) {
            if (log.isErrorEnabled()) {
                log.error("Scope expression is invalid. Expression: {}", escapeLog(scopeExpression));
            }
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, UmaErrorResponseType.INVALID_SCOPE, "Scope expression is invalid. Expression: " + scopeExpression);
        }
    }

    public Client validateClientAndClaimsRedirectUri(String clientId, String claimsRedirectUri, String state) {
        if (StringUtils.isBlank(clientId)) {
            if (log.isErrorEnabled()) {
                log.error("Invalid clientId: {}", escapeLog(clientId));
            }
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, UmaErrorResponseType.INVALID_CLIENT_ID, "Invalid clientId: " + clientId);
        }
        Client client = clientService.getClient(clientId);
        if (client == null) {
            if (log.isErrorEnabled()) {
                log.error("Failed to find client with client_id: {}", escapeLog(clientId));
            }
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, UmaErrorResponseType.INVALID_CLIENT_ID, "Failed to find client with client_id:" + clientId);
        }

        if (StringUtils.isNotBlank(claimsRedirectUri)) {
            if (ArrayUtils.isEmpty(client.getClaimRedirectUris())) {
                if (log.isErrorEnabled()) {
                    log.error("Client does not have claims_redirect_uri specified, clientId: {}", escapeLog(clientId));
                }
                throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, UmaErrorResponseType.INVALID_CLAIMS_REDIRECT_URI, "Client does not have claims_redirect_uri specified, clientId: " + clientId);
            }

            String equalRedirectUri = getEqualRedirectUri(claimsRedirectUri, client.getClaimRedirectUris());
            if (equalRedirectUri != null) {
                log.trace("Found match for claims_redirect_uri : {}", equalRedirectUri);
                return client;
            } else if (log.isTraceEnabled()) {
                log.trace("Failed to find match for claims_redirect_uri : {}, client claimRedirectUris: {}", escapeLog(claimsRedirectUri), Arrays.toString(client.getClaimRedirectUris()));
            }

        } else {
            log.trace("claims_redirect_uri is blank");
            if (client.getClaimRedirectUris() != null && client.getClaimRedirectUris().length == 1) {
                log.trace("claims_redirect_uri is blank and only one claims_redirect_uri is registered.");
                return client;
            }
        }

        if (StringUtils.isBlank(claimsRedirectUri)) {
            if (log.isErrorEnabled()) {
                log.error("claims_redirect_uri is blank and there is none or more then one registered claims_redirect_uri for clientId: {}", escapeLog(clientId));
            }
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, UmaErrorResponseType.INVALID_CLAIMS_REDIRECT_URI, "claims_redirect_uri is blank and there is none or more then one registered claims_redirect_uri for clientId: " + clientId);
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
        if (isTrue(appConfiguration.getUmaRestrictResourceToAssociatedClient())) {
            final List<String> clients = resourceService.getResourceById(rsId).getClients();
            if (!clients.contains(patClientDn)) {
                log.error("Access to resource is denied because resource associated client does not match PAT client (it can be switched off if set umaRestrictResourceToAssociatedClient oxauth configuration property to false). Associated clients: {}, PAT client: {}", clients, patClientDn);
                throw errorResponseFactory.createWebApplicationException(Response.Status.FORBIDDEN, ACCESS_DENIED, "Access to resource is denied because resource associated client does not match PAT client (it can be switched off if set umaRestrictResourceToAssociatedClient oxauth configuration property to false).");
            }
        }
    }

    public void validateResource(io.jans.as.model.uma.UmaResource resource) {
        validateScopeExpression(resource.getScopeExpression());

        List<String> scopeDNs = umaScopeService.getScopeDNsByIdsAndAddToPersistenceIfNeeded(resource.getScopes());
        if (scopeDNs.isEmpty() && StringUtils.isBlank(resource.getScopeExpression())) {
            log.error("Invalid resource. Both `scope` and `scope_expression` are blank.");
            throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, UmaErrorResponseType.INVALID_SCOPE, "Invalid resource. Both `scope` and `scope_expression` are blank.");
        }
    }

    public Client validate(Client client) {
        if (client == null || client.isDisabled()) {
            log.debug("Client is not found or otherwise disabled.");
            throw errorResponseFactory.createWebApplicationException(Response.Status.FORBIDDEN, UmaErrorResponseType.DISABLED_CLIENT, "Client is disabled.");
        }
        return client;
    }
}
