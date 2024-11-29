package io.jans.as.server.auth;

import com.nimbusds.jose.jwk.JWKException;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwt.DPoPJwtPayloadParam;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.model.token.TokenRequestParam;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.DPoPJti;
import io.jans.as.server.util.ServerUtil;
import io.jans.service.CacheService;
import jakarta.ejb.DependsOn;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;
import java.util.UUID;

import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Z
 */
@DependsOn("appInitializer")
@Named
public class DpopService {

    public static final String NO_CACHE = "no-cache";
    public static final String PRAGMA = "Pragma";
    public static final String DPOP_NONCE = "DPoP-Nonce";
    public static final String DPOP = "DPoP";

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CacheService cacheService;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    public void validateDpopValuesCount(HttpServletRequest servletRequest) {
        validateDpopValuesCount(servletRequest.getParameterValues(TokenRequestParam.DPOP));
    }

    public void validateDpopValuesCount(final String[] values) {
        if (values != null && values.length > 1) {
            log.trace("Multiple DPoP header values are not allowed. Count: {}", values.length);
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(errorResponseFactory.errorAsJson(TokenErrorResponseType.INVALID_DPOP_PROOF, "Multiple DPoP header values"))
                    .cacheControl(ServerUtil.cacheControl(true, false))
                    .header(PRAGMA, NO_CACHE)
                    .build());
        }
    }

    public boolean validateDpop(String dpop) {
        try {
            return validateDpop(Jwt.parseOrThrow(dpop));
        } catch (InvalidJwtException e) {
            log.error("Failed to validate dpop: " + dpop, e);
            return false;
        }
    }

    public boolean validateDpop(Jwt dpop) {
        try {
            validateDpopHeader(dpop);

            validateDpopPayload(dpop);

            JSONWebKey jwk = JSONWebKey.fromJSONObject(dpop.getHeader().getJwk());
            String dpopJwkThumbprint = jwk.getJwkThumbprint();
            return validateDpopSignature(dpop, jwk, dpopJwkThumbprint);
        } catch (WebApplicationException e) {
            throw e;
        } catch (InvalidJwtException e) {
            log.error("Failed to validate dpop: " + dpop, e);
        } catch (Exception e) {
            log.error("Invalid dpop: " + dpop, e);
        }
        return false;
    }

    private boolean validateDpopSignature(Jwt dpop, JSONWebKey jwk, String dpopJwkThumbprint) throws InvalidJwtException, CryptoProviderException {
        if (dpopJwkThumbprint == null) {
            throw new InvalidJwtException("Invalid DPoP Proof Header. The jwk header is not valid.");
        }

        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.getKeys().add(jwk);

        return cryptoProvider.verifySignature(
                dpop.getSigningInput(),
                dpop.getEncodedSignature(),
                null,
                jwks.toJSONObject(),
                null,
                dpop.getHeader().getSignatureAlgorithm());
    }

    private void validateDpopPayload(Jwt dpop) throws InvalidJwtException {
        if (StringUtils.isBlank(dpop.getClaims().getClaimAsString(DPoPJwtPayloadParam.HTM))) {
            throw new InvalidJwtException("Invalid DPoP Proof Payload. The htm param is required.");
        }
        if (StringUtils.isBlank(dpop.getClaims().getClaimAsString(DPoPJwtPayloadParam.HTU))) {
            throw new InvalidJwtException("Invalid DPoP Proof Payload. The htu param is required");
        }
        if (dpop.getClaims().getClaimAsLong(DPoPJwtPayloadParam.IAT) == null) {
            throw new InvalidJwtException("Invalid DPoP Proof Payload. The iat param is required.");
        }
        if (StringUtils.isBlank(dpop.getClaims().getClaimAsString(DPoPJwtPayloadParam.JTI))) {
            throw new InvalidJwtException("Invalid DPoP Proof Payload. The jti param is required");
        }

        String jti = dpop.getClaims().getClaimAsString(DPoPJwtPayloadParam.JTI);
        Long iat = dpop.getClaims().getClaimAsLong(DPoPJwtPayloadParam.IAT);
        String htu = dpop.getClaims().getClaimAsString(DPoPJwtPayloadParam.HTU);
        String nonce = dpop.getClaims().getClaimAsString(DPoPJwtPayloadParam.NONCE);
        String cacheKey = "dpop_jti_" + jti;
        DPoPJti dPoPJti = (DPoPJti) cacheService.get(cacheKey);

        // Validate the token was issued within an acceptable timeframe.
        int seconds = appConfiguration.getDpopTimeframe();
        long diff = (new Date().getTime() - iat) / 1000;
        if (diff > seconds) {
            throw new InvalidJwtException("The DPoP token has expired.");
        }

        if (dPoPJti == null) {
            dPoPJti = new DPoPJti(jti, iat, htu);
            cacheService.put(appConfiguration.getDpopJtiCacheTime(), cacheKey, dPoPJti);
        } else {
            throw new InvalidJwtException("Invalid DPoP Proof. The jti param has been used before.");
        }

        if (BooleanUtils.isTrue(appConfiguration.getDpopUseNonce())) {
            if (StringUtils.isBlank(nonce)) {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(errorResponseFactory.errorAsJson(TokenErrorResponseType.USE_DPOP_NONCE, "Nonce is not set"))
                        .cacheControl(ServerUtil.cacheControl(true, false))
                        .header(PRAGMA, NO_CACHE)
                        .header(DPOP_NONCE, generateNonce()).build());
            }

            final Object nonceValue = cacheService.get(nonce);
            if (nonceValue == null) {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(errorResponseFactory.errorAsJson(TokenErrorResponseType.USE_NEW_DPOP_NONCE, "New nonce value is required"))
                        .cacheControl(ServerUtil.cacheControl(true, false))
                        .header(PRAGMA, NO_CACHE)
                        .header(DPOP_NONCE, generateNonce()).build());
            }
        }
    }

    private String generateNonce() {
        final String nonce = UUID.randomUUID().toString();
        cacheService.put(appConfiguration.getDpopNonceCacheTime(), nonce, nonce);
        return nonce;
    }

    private void validateDpopHeader(Jwt dpop) throws InvalidJwtException {
        if (dpop.getHeader().getType() != JwtType.DPOP_PLUS_JWT) {
            throw new InvalidJwtException("Invalid DPoP Proof Header. The typ header must be dpop+jwt.");
        }
        if (dpop.getHeader().getSignatureAlgorithm() == null) {
            throw new InvalidJwtException("Invalid DPoP Proof Header. The typ header must be dpop+jwt.");
        }
        if (dpop.getHeader().getJwk() == null) {
            throw new InvalidJwtException("Invalid DPoP Proof Header. The jwk header is required.");
        }
    }

    private Response response(Response.ResponseBuilder builder, OAuth2AuditLog oAuth2AuditLog) {
        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header(PRAGMA, NO_CACHE);

        applicationAuditLogger.sendMessage(oAuth2AuditLog);

        return builder.build();
    }

    private Response.ResponseBuilder error(int status, TokenErrorResponseType type, String reason) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(type, reason));
    }

    public String getDpopJwkThumbprint(String dpopStr) throws InvalidJwtException, NoSuchAlgorithmException, JWKException, NoSuchProviderException {
        final Jwt dpop = Jwt.parseOrThrow(dpopStr);
        JSONWebKey jwk = JSONWebKey.fromJSONObject(dpop.getHeader().getJwk());
        return jwk.getJwkThumbprint();
    }

    public String getDPoPJwkThumbprint(HttpServletRequest httpRequest, Client client, OAuth2AuditLog oAuth2AuditLog) {
        try {
            String dpopStr = httpRequest.getHeader(DPOP);
            final boolean isDpopBlank = StringUtils.isBlank(dpopStr);

            if (isTrue(client.getAttributes().getDpopBoundAccessToken()) && isDpopBlank) {
                log.debug("Client requires DPoP bound access token. Invalid request - DPoP header is not set.");
                throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_DPOP_PROOF, "Invalid request - DPoP header is not set."), oAuth2AuditLog));
            }

            if (isDpopBlank) return null;

            String dpopJwkThumbprint = getDpopJwkThumbprint(dpopStr);

            if (dpopJwkThumbprint == null)
                throw new InvalidJwtException("Invalid DPoP Proof Header. The jwk header is not valid.");

            return dpopJwkThumbprint;
        } catch (InvalidJwtException | JWKException | NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_DPOP_PROOF, e.getMessage()), oAuth2AuditLog));
        }
    }

    public void validateDpopThumprintIsPresent(String dpopJkt, String state) {
        if (BooleanUtils.isTrue(appConfiguration.getDpopJktForceForAuthorizationCode()) && StringUtils.isBlank(dpopJkt)) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST.getStatusCode())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, "dpop_jkt is absent"))
                    .build());
        }
    }

    public void validateDpopThumprint(String existingThumprint, String requestThumprint) {
        if (StringUtils.isBlank(existingThumprint) && isFalse(appConfiguration.getDpopJktForceForAuthorizationCode())) {
            return; // nothing to check
        }

        if (!StringUtils.equals(existingThumprint, requestThumprint)) {
            log.debug("DPoP Thumprint between saved one '{}' and send in request '{}' does NOT match. Reject request.", existingThumprint, requestThumprint);
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(errorResponseFactory.errorAsJson(TokenErrorResponseType.INVALID_DPOP_PROOF, "Thumprint does not match"))
                    .cacheControl(ServerUtil.cacheControl(true, false))
                    .header(PRAGMA, NO_CACHE)
                    .build());
        }
    }
}
