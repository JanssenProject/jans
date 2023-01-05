/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */
package io.jans.as.server.register.ws.rs;

import com.google.common.base.Strings;
import io.jans.as.client.RegisterRequest;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.SoftwareStatementValidationType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.register.RegisterErrorResponseType;
import io.jans.as.model.ssa.SsaValidationType;
import io.jans.as.model.util.Pair;
import io.jans.as.server.ciba.CIBARegisterParamsValidatorService;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.registration.RegisterParamsValidator;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import io.jans.as.server.service.net.UriService;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;

import static io.jans.as.model.register.RegisterRequestParam.SOFTWARE_STATEMENT;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class RegisterValidator {

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private CIBARegisterParamsValidatorService cibaRegisterParamsValidatorService;

    @Inject
    private RegisterParamsValidator registerParamsValidator;

    @Inject
    private SsaValidationConfigService ssaValidationConfigService;

    @Inject
    private UriService uriService;

    public void validateNotBlank(String input, String errorReason) {
        if (StringUtils.isBlank(input)) {
            log.trace("Failed to perform client action, reason: {}", errorReason);
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, "");
        }
    }

    public void validateRequestObject(String requestParams, JSONObject softwareStatement, HttpServletRequest httpRequest) {
        try {
            if (isFalse(appConfiguration.getDcrSignatureValidationEnabled())) {
                return;
            }

            final Jwt jwt = Jwt.parseOrThrow(requestParams);
            final SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getSignatureAlgorithm();
            final SsaValidationConfigContext ssaContext = new SsaValidationConfigContext(jwt, SsaValidationType.DCR);

            final boolean isHmac = AlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily());
            if (isHmac) {
                validateRequestObjectHmac(httpRequest, ssaContext);
                return;
            }

            String jwksUri = null;
            if (StringUtils.isNotBlank(appConfiguration.getDcrSignatureValidationSoftwareStatementJwksURIClaim())) {
                jwksUri = softwareStatement.optString(appConfiguration.getDcrSignatureValidationSoftwareStatementJwksURIClaim());
            }
            if (StringUtils.isBlank(jwksUri) && StringUtils.isNotBlank(appConfiguration.getDcrSignatureValidationJwksUri())) {
                jwksUri = appConfiguration.getDcrSignatureValidationJwksUri();
            }

            String jwksStr = getJwksString(softwareStatement);
            JSONObject jwks = getJwks(httpRequest, jwt, jwksUri, jwksStr);

            log.trace("Validating request object with jwks: {} ...", jwks);

            boolean validSignature = cryptoProvider.verifySignature(jwt.getSigningInput(),
                    jwt.getEncodedSignature(), jwt.getHeader().getKeyId(), jwks, null, signatureAlgorithm);

            log.trace("Request object validation result: {}", validSignature);
            if (validSignature) {
                return;
            }

            if (validateRequestObjectSignatureWithSsaValidationConfigs(requestParams)) {
                return;
            }

            throw new InvalidJwtException("Invalid request object.");
        } catch (Exception e) {
            if (validateRequestObjectSignatureWithSsaValidationConfigs(requestParams)) {
                return;
            }

            final String msg = "Unable to validate request object JWT.";
            log.error(msg, e);
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, msg);
        }
    }

    private boolean validateRequestObjectSignatureWithSsaValidationConfigs(String requestParams) {
        try {
            final SsaValidationConfigContext ssaContext = new SsaValidationConfigContext(Jwt.parseOrThrow(requestParams), SsaValidationType.DCR);
            final boolean valid = ssaValidationConfigService.hasValidSignature(ssaContext);
            log.trace("Request object validation result for ssaValidationConfigs: {}", valid);
            if (valid) {
                log.trace("Request object successfully validated by ssaValidationConfig: {}", ssaContext.getSuccessfulConfig());
                return true;
            }
        } catch (InvalidJwtException e) {
            log.error("Unable to validate request object JWT.", e);
        }
        return false;
    }

    private void validateRequestObjectHmac(HttpServletRequest httpRequest, SsaValidationConfigContext ssaContext) throws CryptoProviderException, InvalidJwtException {
        final Jwt jwt = ssaContext.getJwt();

        if (ssaValidationConfigService.isHmacValid(ssaContext)) {
            log.trace("Request object successfully validated by ssaValidationConfig: {}", ssaContext.getSuccessfulConfig());
            return;
        }
        String hmacSecret = appConfiguration.getDcrSignatureValidationSharedSecret();
        if (StringUtils.isBlank(hmacSecret)) {
            hmacSecret = externalDynamicClientRegistrationService.getDcrHmacSecret(httpRequest, jwt);
        }
        if (StringUtils.isBlank(hmacSecret)) {
            log.error("No hmacSecret provided in Dynamic Client Registration script (method getDcrHmacSecret didn't return actual secret). ");
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_SOFTWARE_STATEMENT, "");
        }

        boolean validSignature = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null, null, hmacSecret, jwt.getHeader().getSignatureAlgorithm());
        log.trace("Request object validation result: {}", validSignature);
        if (validSignature) {
            return;
        }
        throw new InvalidJwtException("Invalid cryptographic segment in the request object.");
    }

    @Nullable
    private String getJwksString(JSONObject softwareStatement) {
        if (StringUtils.isNotBlank(appConfiguration.getDcrSignatureValidationSoftwareStatementJwksClaim())) {
            return softwareStatement.optString(appConfiguration.getDcrSignatureValidationSoftwareStatementJwksClaim());
        }
        if (StringUtils.isNotBlank(appConfiguration.getDcrSignatureValidationJwks())) {
            return appConfiguration.getDcrSignatureValidationJwks();
        }
        return null;
    }

    @Nullable
    private JSONObject getJwks(HttpServletRequest httpRequest, Jwt jwt, String jwksUri, String jwksStr) {
        if (StringUtils.isNotBlank(jwksUri)) {
            return uriService.loadJson(jwksUri);
        }

        if (StringUtils.isNotBlank(jwksStr)) {
            return new JSONObject(jwksStr);
        }

        if (externalDynamicClientRegistrationService.isEnabled()) {
            log.trace("No values are set for dcrSignatureValidationJwksUri and dcrSignatureValidationJwks, invoking script ...");

            JSONObject jwks = externalDynamicClientRegistrationService.getDcrJwks(httpRequest, jwt);
            if (jwks == null) {
                log.error("No jwks provided in Dynamic Client Registration script (method getDcrJwks didn't return actual jwks). ");
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_SOFTWARE_STATEMENT, "");
            }
        }
        return null;
    }

    public JSONObject validateSoftwareStatement(HttpServletRequest httpServletRequest, JSONObject requestObject) {
        if (!requestObject.has(SOFTWARE_STATEMENT.toString())) {
            return null;
        }

        try {
            Jwt softwareStatement = Jwt.parseOrThrow(requestObject.getString(SOFTWARE_STATEMENT.toString()));
            final SignatureAlgorithm signatureAlgorithm = softwareStatement.getHeader().getSignatureAlgorithm();

            final SoftwareStatementValidationType validationType = SoftwareStatementValidationType.fromString(appConfiguration.getSoftwareStatementValidationType());
            printWarningIfNeeded(validationType);

            if (validationType == SoftwareStatementValidationType.NONE) {
                log.trace("software_statement validation was skipped due to `softwareStatementValidationType` configuration property set to none. (Not recommended.)");
                return softwareStatement.getClaims().toJsonObject();
            }

            if (validationType == SoftwareStatementValidationType.SCRIPT) {
                return validateSoftwareStatementForScript(httpServletRequest, requestObject, softwareStatement, signatureAlgorithm);
            }

            if (validationType == SoftwareStatementValidationType.BUILTIN) {
                return ssaValidationConfigService.validateSsaForBuiltIn(softwareStatement);
            }

            if ((validationType == SoftwareStatementValidationType.JWKS_URI ||
                    validationType == SoftwareStatementValidationType.JWKS) &&
                    StringUtils.isBlank(appConfiguration.getSoftwareStatementValidationClaimName())) {
                log.error("softwareStatementValidationClaimName configuration property is not specified. Please specify claim name from software_statement which points to jwks (or jwks_uri).");
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_SOFTWARE_STATEMENT, "Failed to validate software statement");
            }

            String jwksUriClaim = null;
            if (validationType == SoftwareStatementValidationType.JWKS_URI) {
                jwksUriClaim = softwareStatement.getClaims().getClaimAsString(appConfiguration.getSoftwareStatementValidationClaimName());
            }

            String jwksClaim = null;
            if (validationType == SoftwareStatementValidationType.JWKS) {
                jwksClaim = softwareStatement.getClaims().getClaimAsString(appConfiguration.getSoftwareStatementValidationClaimName());
            }

            if (StringUtils.isBlank(jwksUriClaim) && StringUtils.isBlank(jwksClaim)) {
                final String msg = String.format("software_statement does not contain `%s` claim and thus is considered as invalid.", appConfiguration.getSoftwareStatementValidationClaimName());
                log.error(msg);
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_SOFTWARE_STATEMENT, msg);
            }

            JSONObject jwks = Strings.isNullOrEmpty(jwksUriClaim) ?
                    null :
                    uriService.loadJson(jwksUriClaim);
            if (jwks == null && StringUtils.isNotBlank(jwksClaim)) {
                jwks = new JSONObject(jwksClaim);
            }

            boolean validSignature = cryptoProvider.verifySignature(softwareStatement.getSigningInput(),
                    softwareStatement.getEncodedSignature(),
                    softwareStatement.getHeader().getKeyId(), jwks, null, signatureAlgorithm);

            if (!validSignature) {
                throw new InvalidJwtException("Invalid cryptographic segment in the software statement");
            }

            return softwareStatement.getClaims().toJsonObject();
        } catch (Exception e) {
            final String msg = "Invalid software_statement.";
            log.error(msg, e);
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_SOFTWARE_STATEMENT, msg);
        }
    }

    private void printWarningIfNeeded(SoftwareStatementValidationType validationType) {
        if (validationType == SoftwareStatementValidationType.SCRIPT || validationType == SoftwareStatementValidationType.BUILTIN) {
            return;
        }
        log.warn("It is strongly recommended to use SCRIPT or BUILTIN value for softwareStatementValidationType configuration property.");
    }

    @Nullable
    private JSONObject validateSoftwareStatementForScript(HttpServletRequest httpServletRequest, JSONObject requestObject, Jwt softwareStatement, SignatureAlgorithm signatureAlgorithm) throws CryptoProviderException, InvalidJwtException {
        if (!externalDynamicClientRegistrationService.isEnabled()) {
            log.error("Server is mis-configured. softwareStatementValidationType=script but there is no any Dynamic Client Registration script enabled.");
            return null;
        }

        if (AlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily())) {

            final String hmacSecret = externalDynamicClientRegistrationService.getSoftwareStatementHmacSecret(httpServletRequest, requestObject, softwareStatement);
            if (StringUtils.isBlank(hmacSecret)) {
                log.error("No hmacSecret provided in Dynamic Client Registration script (method getSoftwareStatementHmacSecret didn't return actual secret). ");
                throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_SOFTWARE_STATEMENT, "");
            }

            if (!cryptoProvider.verifySignature(softwareStatement.getSigningInput(), softwareStatement.getEncodedSignature(), null, null, hmacSecret, signatureAlgorithm)) {
                throw new InvalidJwtException("Invalid signature in the software statement");
            }

            return softwareStatement.getClaims().toJsonObject();
        }

        final JSONObject softwareStatementJwks = externalDynamicClientRegistrationService.getSoftwareStatementJwks(httpServletRequest, requestObject, softwareStatement);
        if (softwareStatementJwks == null) {
            log.error("No jwks provided in Dynamic Client Registration script (method getSoftwareStatementJwks didn't return actual jwks). ");
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_SOFTWARE_STATEMENT, "");
        }

        if (!cryptoProvider.verifySignature(softwareStatement.getSigningInput(), softwareStatement.getEncodedSignature(), softwareStatement.getHeader().getKeyId(), softwareStatementJwks, null, signatureAlgorithm)) {
            throw new InvalidJwtException("Invalid signature in the software statement");
        }

        return softwareStatement.getClaims().toJsonObject();
    }

    public void validateSubjectIdentifierAttribute(RegisterRequest registerRequest) {
        if (StringUtils.isNotBlank(registerRequest.getSubjectIdentifierAttribute())) {
            if (Boolean.FALSE.equals(appConfiguration.getPublicSubjectIdentifierPerClientEnabled())) {
                throw errorResponseFactory.createWebApplicationException(
                        Response.Status.BAD_REQUEST,
                        RegisterErrorResponseType.INVALID_PUBLIC_SUBJECT_IDENTIFIER_ATTRIBUTE,
                        "The public subject identifier per client is disabled."
                );
            }

            if (registerRequest.getSubjectType() != SubjectType.PUBLIC) {
                throw errorResponseFactory.createWebApplicationException(
                        Response.Status.BAD_REQUEST,
                        RegisterErrorResponseType.INVALID_PUBLIC_SUBJECT_IDENTIFIER_ATTRIBUTE,
                        "The custom subject identifier requires public subject type."
                );
            }

            if (!appConfiguration.getSubjectIdentifiersPerClientSupported().contains(registerRequest.getSubjectIdentifierAttribute())) {
                throw errorResponseFactory.createWebApplicationException(
                        Response.Status.BAD_REQUEST,
                        RegisterErrorResponseType.INVALID_PUBLIC_SUBJECT_IDENTIFIER_ATTRIBUTE,
                        "Invalid subject identifier attribute."
                );
            }
        }

        if (StringUtils.isNotBlank(registerRequest.getRedirectUrisRegex()) && Boolean.FALSE.equals(appConfiguration.getRedirectUrisRegexEnabled())) {
            throw errorResponseFactory.createBadRequestException(
                    RegisterErrorResponseType.INVALID_REDIRECT_URIS_REGEX,
                    "The redirect URI's Regex is disabled."
            );
        }
    }

    public void validateAuthorizationAccessToken(String accessToken, String clientId) {
        if (isFalse(appConfiguration.getDcrAuthorizationWithClientCredentials())) {
            return;
        }
        if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(clientId)) {
            log.trace("Access Token or clientId is blank.");
            throw new WebApplicationException(Response.
                    status(Response.Status.BAD_REQUEST).
                    type(MediaType.APPLICATION_JSON_TYPE).
                    entity(errorResponseFactory.errorAsJson(RegisterErrorResponseType.INVALID_TOKEN, "The Access Token is not valid for the Client ID."))
                    .build());
        }

        final AuthorizationGrant grant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);
        if (grant == null) {
            log.trace("Unable to find grant by access token: {}", accessToken);
            throw new WebApplicationException(Response.
                    status(Response.Status.UNAUTHORIZED).
                    type(MediaType.APPLICATION_JSON_TYPE).
                    entity(errorResponseFactory.errorAsJson(RegisterErrorResponseType.INVALID_TOKEN, "The Access Token grant is not found."))
                    .build());
        }

        final AbstractToken accessTokenObj = grant.getAccessToken(accessToken);
        if (accessTokenObj == null || !accessTokenObj.isValid()) {
            log.trace("Unable to find access token object or otherwise it's expired.");
            throw new WebApplicationException(Response.
                    status(Response.Status.UNAUTHORIZED).
                    type(MediaType.APPLICATION_JSON_TYPE).
                    entity(errorResponseFactory.errorAsJson(RegisterErrorResponseType.INVALID_TOKEN, "The Access Token object is not found or otherwise expired."))
                    .build());
        }

        if (!clientId.equals(grant.getClientId())) {
            log.trace("ClientId from request does not match to access token's client id.");
            throw new WebApplicationException(Response.
                    status(Response.Status.BAD_REQUEST).
                    type(MediaType.APPLICATION_JSON_TYPE).
                    entity(errorResponseFactory.errorAsJson(RegisterErrorResponseType.INVALID_TOKEN, "The Access Token object is not found or otherwise expired."))
                    .build());
        }
    }

    public void validateCiba(RegisterRequest r) {
        if (!cibaRegisterParamsValidatorService.validateParams(
                r.getBackchannelTokenDeliveryMode(),
                r.getBackchannelClientNotificationEndpoint(),
                r.getBackchannelAuthenticationRequestSigningAlg(),
                r.getGrantTypes(),
                r.getSubjectType(),
                r.getSectorIdentifierUri(),
                r.getJwks(),
                r.getJwksUri()
        )) { // CIBA
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA,
                    "Invalid Client Metadata registering to use CIBA (Client Initiated Backchannel Authentication).");
        }
    }

    public void validateRedirectUris(RegisterRequest r) {
        if (!registerParamsValidator.validateRedirectUris(
                r.getGrantTypes(), r.getResponseTypes(),
                r.getApplicationType(), r.getSubjectType(),
                r.getRedirectUris(), r.getSectorIdentifierUri())) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_REDIRECT_URI, "Failed to validate redirect uris.");
        }
    }

    public void validateParamsClientRegister(RegisterRequest r) {
        final Pair<Boolean, String> validateResult = registerParamsValidator.validateParamsClientRegister(
                r.getApplicationType(), r.getSubjectType(),
                r.getGrantTypes(), r.getResponseTypes(),
                r.getRedirectUris());
        if (isFalse(validateResult.getFirst())) {
            log.trace("Client parameters are invalid, returns invalid_request error. Reason: {}", validateResult.getSecond());
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLIENT_METADATA, validateResult.getSecond());
        }
    }

    public void validateInitiateLoginUri(RegisterRequest r) {
        if (!Strings.isNullOrEmpty(r.getInitiateLoginUri()) && !registerParamsValidator.validateInitiateLoginUri(r.getInitiateLoginUri())) {
            log.debug("The Initiate Login Uri is invalid. The initiate_login_uri must use the https schema: {}", r.getInitiateLoginUri());
            throw errorResponseFactory.createWebApplicationException(
                    Response.Status.BAD_REQUEST,
                    RegisterErrorResponseType.INVALID_CLIENT_METADATA,
                    "The Initiate Login Uri is invalid. The initiate_login_uri must use the https schema.");
        }
    }

    public void validateClaimsRedirectUris(RegisterRequest r) {
        if (r.getClaimsRedirectUris() != null &&
                !r.getClaimsRedirectUris().isEmpty() &&
                !registerParamsValidator.validateRedirectUris(r.getGrantTypes(), r.getResponseTypes(), r.getApplicationType(), r.getSubjectType(), r.getClaimsRedirectUris(), r.getSectorIdentifierUri())) {
            log.debug("Value of one or more claims_redirect_uris is invalid, claims_redirect_uris: {}", r.getClaimsRedirectUris());
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.INVALID_CLAIMS_REDIRECT_URI, "Value of one or more claims_redirect_uris is invalid");
        }
    }

    public void validatePasswordGrantType(RegisterRequest r) {
        if (isFalse(appConfiguration.getDynamicRegistrationPasswordGrantTypeEnabled())
                && registerParamsValidator.checkIfThereIsPasswordGrantType(r.getGrantTypes())) {
            log.info("Password Grant Type is not allowed for Dynamic Client Registration.");
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.ACCESS_DENIED, "Password Grant Type is not allowed for Dynamic Client Registration.");
        }
    }

    public void validateDcrAuthorizationWithClientCredentials(RegisterRequest r) {
        if (isTrue(appConfiguration.getDcrAuthorizationWithClientCredentials()) && !r.getGrantTypes().contains(GrantType.CLIENT_CREDENTIALS)) {
            log.info("Register request does not contain grant_type=client_credentials, however dcrAuthorizationWithClientCredentials=true which is forbidden.");
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.ACCESS_DENIED, "Client Credentials Grant Type is not present in Dynamic Client Registration request.");
        }
    }
}
