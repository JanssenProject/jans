package io.jans.as.server.auth;

import com.nimbusds.jose.jwk.JWKException;
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
import io.jans.as.server.model.common.DPoPJti;
import io.jans.as.server.util.ServerUtil;
import io.jans.service.CacheService;
import jakarta.ejb.DependsOn;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Z
 */
@DependsOn("appInitializer")
@Named
public class DPoPService {

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

    public String getDpopJwkThumbprint(String dpopStr) throws InvalidJwtException, NoSuchAlgorithmException, JWKException, NoSuchProviderException {
        final Jwt dpop = Jwt.parseOrThrow(dpopStr);
        JSONWebKey jwk = JSONWebKey.fromJSONObject(dpop.getHeader().getJwk());
        return jwk.getJwkThumbprint();
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
                        .header("Pragma", "no-cache")
                        .header("DPoP-Nonce", generateNonce()).build());
            }

            final Object nonceValue = cacheService.get(nonce);
            if (nonceValue == null) {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(errorResponseFactory.errorAsJson(TokenErrorResponseType.USE_NEW_DPOP_NONCE, "New nonce value is required"))
                        .cacheControl(ServerUtil.cacheControl(true, false))
                        .header("Pragma", "no-cache")
                        .header("DPoP-Nonce", generateNonce()).build());
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
}
