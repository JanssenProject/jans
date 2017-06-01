/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.service;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.slf4j.Logger;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.config.WebKeysConfiguration;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jws.RSASigner;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
import org.xdi.oxauth.model.uma.ClaimTokenFormatType;
import org.xdi.oxauth.model.uma.UmaPermissionList;
import org.xdi.oxauth.model.uma.UmaScopeType;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.UmaResource;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.uma.authorization.UmaPCT;
import org.xdi.oxauth.uma.authorization.UmaRPT;
import org.xdi.util.StringHelper;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.xdi.oxauth.model.uma.UmaErrorResponseType.*;

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
        for (org.xdi.oxauth.model.uma.UmaPermission permission : permissions) {
            validatePermission(permission);
        }
    }

    public void validatePermission(org.xdi.oxauth.model.uma.UmaPermission permission) {
        String resourceId = permission.getResourceId();
        if (StringHelper.isEmpty(resourceId)) {
            log.error("Resource id is empty");
            errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_ID);
        }

        try {
            UmaResource exampleResource = new UmaResource();
            exampleResource.setDn(resourceService.getBaseDnForResource());
            exampleResource.setId(resourceId);
            List<UmaResource> resources = resourceService.findResources(exampleResource);
            if (resources.size() != 1) {
                log.error("Resource isn't registered or there are two resources with same Id");
                errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_ID);
            }
            UmaResource resource = resources.get(0);

            final List<String> scopeUrls = umaScopeService.getScopeUrlsByDns(resource.getScopes());
            if (!scopeUrls.containsAll(permission.getScopes())) {
                log.error("At least one of the scope isn't registered");
                errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_RESOURCE_SCOPE);
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
            log.error("Unable to find permissions registered for given ticket.");
            errorResponseFactory.throwUmaWebApplicationException(BAD_REQUEST, INVALID_TICKET);
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
                if (idToken != null && isIdTokenValid(idToken)) {
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
            final String nonceFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.NONCE);
            final String audienceFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.AUDIENCE);

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
                    return true;
                }
                log.error("ID Token signature is invalid.");
            } else {
                log.error("Failed to get RSA public key.");
            }
            return false;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
}
