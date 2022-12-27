package io.jans.as.server.register.ws.rs;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.register.RegisterRequestParam;
import io.jans.as.model.ssa.SsaValidationConfig;
import io.jans.as.model.ssa.SsaValidationType;
import io.jans.as.server.service.net.UriService;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.jans.as.model.util.StringUtils.implode;

/**
 * @author Yuriy Z
 */
@Stateless
@Named
public class SsaValidationConfigService {

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Logger log;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private UriService uriService;

    public List<SsaValidationConfig> getByIssuer(String issuer, SsaValidationType type) {
        if (StringUtils.isBlank(issuer)) {
            return new ArrayList<>();
        }
        final List<SsaValidationConfig> all = appConfiguration.getDcrSsaValidationConfigs();
        return all.stream().filter(s -> s.getIssuers().contains(issuer) && s.getType() == type).collect(Collectors.toList());
    }

    public List<SsaValidationConfig> getByIssuer(Jwt jwt, SsaValidationType type) {
        final String issuer = jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER);
        return getByIssuer(issuer, type);
    }

    public boolean isHmacValid(SsaValidationConfigContext context) {
        final List<SsaValidationConfig> byIssuer = getByIssuer(context.getJwt(), context.getType());
        if (byIssuer.isEmpty()) {
            return false;
        }

        for (SsaValidationConfig config : byIssuer) {
            if (isHmacValid(context.getJwt(), config)) {
                context.setSuccessfulConfig(config);
                return true;
            }
        }
        return false;
    }

    private boolean isHmacValid(Jwt jwt, SsaValidationConfig config) {
        String hmacSecret = config.getSharedSecret();
        if (StringUtils.isBlank(hmacSecret)) {
            log.trace("No hmacSecret provided in SsaValidationConfig: {}", config);
            return false;
        }

        final SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getSignatureAlgorithm();

        try {
            boolean validSignature = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null, null, hmacSecret, signatureAlgorithm);
            log.trace("Request object validation result: {}, SsaValidationConfig: {}", validSignature, config);
            if (validSignature) {
                log.trace("Request object is validated successfully. SsaValidationConfig: {}", config);
                return true;
            }
        } catch (CryptoProviderException | InvalidJwtException e) {
            log.trace("Unable to validate jwt with ssaValidationConfig: " + config, e);
        }

        return false;
    }

    public boolean hasValidSignature(SsaValidationConfigContext context) {
        final List<SsaValidationConfig> byIssuer = getByIssuer(context.getJwt(), context.getType());
        if (byIssuer.isEmpty()) {
            return false;
        }

        for (SsaValidationConfig config : byIssuer) {
            if (isSignatureValid(context.getJwt(), config)) {
                context.setSuccessfulConfig(config);
                return true;
            }
        }

        return false;
    }

    private boolean isSignatureValid(Jwt jwt, SsaValidationConfig config) {
        try {
            JSONObject jwks = loadJwks(config);
            if (jwks == null || jwks.isEmpty()) {
                log.error("Unable to load jwks for ssaValidationConfig: {}", config);
                return false;
            }

            log.trace("Validating request object with jwks: {} ...", jwks);

            return cryptoProvider.verifySignature(jwt.getSigningInput(),
                    jwt.getEncodedSignature(), jwt.getHeader().getKeyId(), jwks, null, jwt.getHeader().getSignatureAlgorithm());
        } catch (CryptoProviderException | InvalidJwtException e) {
            log.trace("Unable to validate jwt with ssaValidationConfig: " + config, e);
        }
        return false;
    }

    private JSONObject loadJwks(SsaValidationConfig config) {
        JSONObject jwks = null;
        if (StringUtils.isNotBlank(config.getJwksUri())) {
            jwks = uriService.loadJson(config.getJwksUri());
        }

        if (jwks == null && StringUtils.isNotBlank(config.getJwks())) {
            jwks = new JSONObject(config.getJwks());
        }

        if (jwks == null && StringUtils.isNotBlank(config.getConfigurationEndpoint()) && StringUtils.isNotBlank(config.getConfigurationEndpointClaim())) {
            final JSONObject responseJson = uriService.loadJson(config.getConfigurationEndpoint());
            final String jwksEndpoint = responseJson.optString(config.getConfigurationEndpointClaim());
            if (StringUtils.isNotBlank(jwksEndpoint)) {
                jwks = uriService.loadJson(jwksEndpoint);
            }
        }
        return jwks;
    }

    public JSONObject validateSsaForBuiltIn(Jwt ssa) throws InvalidJwtException {
        log.debug("Validating ssa with softwareStatementValidationType=builtin validation ...");

        final List<SsaValidationConfig> byIssuer = getByIssuer(ssa, SsaValidationType.SSA);
        final SignatureAlgorithm signatureAlgorithm = ssa.getHeader().getSignatureAlgorithm();
        final boolean isHmac = AlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily());

        for (SsaValidationConfig config : byIssuer) {
            if (isHmac && isHmacValid(ssa, config)) {
                return prepareSsaJsonObject(ssa.getClaims(), config);
            }

            if (!isHmac && isSignatureValid(ssa, config)) {
                return prepareSsaJsonObject(ssa.getClaims(), config);
            }
        }

        return null;
    }

    public JSONObject prepareSsaJsonObject(JwtClaims ssa, SsaValidationConfig config) throws InvalidJwtException {
        final JSONObject result = ssa.toJsonObject();
        if (!config.getScopes().isEmpty()) {
            log.trace("Set scopes from ssaValidationConfig: {}", config);
            result.putOpt(RegisterRequestParam.SCOPE.toString(), implode(config.getScopes(), " "));
        }
        if (!config.getAllowedClaims().isEmpty()) {
            log.trace("Set claims from ssaValidationConfig: {}", config);
            result.putOpt(RegisterRequestParam.CLAIMS.toString(), implode(config.getAllowedClaims(), " "));
        }
        return result;
    }
}
