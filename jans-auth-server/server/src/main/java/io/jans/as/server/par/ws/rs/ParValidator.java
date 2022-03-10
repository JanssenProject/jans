package io.jans.as.server.par.ws.rs;

import com.google.common.collect.Lists;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.authorize.CodeVerifier;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.error.IErrorType;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Par;
import io.jans.as.server.authorize.ws.rs.AuthorizeRestWebServiceValidator;
import io.jans.as.server.model.authorize.Claim;
import io.jans.as.server.model.authorize.IdTokenMember;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.service.RedirectUriResponse;
import io.jans.as.server.service.RequestParameterService;
import io.jans.as.server.util.ServerUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.Set;

import static io.jans.as.model.util.StringUtils.implode;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class ParValidator {

    @Inject
    private Logger log;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private AuthorizeRestWebServiceValidator authorizeRestWebServiceValidator;

    @Inject
    private ScopeChecker scopeChecker;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private RequestParameterService requestParameterService;

    public void validateRequestUriIsAbsent(@Nullable String requestUri) {
        validateRequestUriIsAbsent(requestUri, AuthorizeErrorResponseType.INVALID_REQUEST);
    }

    public void validateRequestUriIsAbsent(@Nullable String requestUri, @NotNull IErrorType error) {
        if (StringUtils.isBlank(requestUri))
            return;

        log.trace("request_uri parameter is not allowed at PAR endpoint. Return error.");
        throw errorResponseFactory.createBadRequestException(error, "");
    }

    public void validateRequestObject(RedirectUriResponse redirectUriResponse, Par par, Client client) {
        final String request = par.getAttributes().getRequest();

        if (StringUtils.isBlank(request)) {
            return;
        }

        try {
            JwtAuthorizationRequest jwtRequest = JwtAuthorizationRequest.createJwtRequest(request, null, client, redirectUriResponse, cryptoProvider, appConfiguration);

            if (jwtRequest == null) {
                throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "Failed to parse jwt.");
            }
            validateRequestUriIsAbsent(jwtRequest.getJsonPayload().optString("request_uri"), AuthorizeErrorResponseType.INVALID_REQUEST_OBJECT);
            setStateIntoPar(redirectUriResponse, par, jwtRequest);

            authorizeRestWebServiceValidator.validateRequestObject(jwtRequest, redirectUriResponse);

            if (!jwtRequest.getResponseTypes().isEmpty()) {
                par.getAttributes().setResponseType(jwtRequest.getJsonPayload().optString("response_type"));
            }
            if (StringUtils.isNotBlank(jwtRequest.getClientId())) {
                par.getAttributes().setClientId(jwtRequest.getClientId());
            }
            if (jwtRequest.getNbf() != null) {
                par.getAttributes().setNbf(jwtRequest.getNbf());
            }
            if (jwtRequest.getExp() != null) {
                par.setTtl(jwtRequest.getExp());
                par.setExpirationDate(Util.createExpirationDate(jwtRequest.getExp()));
            }
            if (jwtRequest.getExp() != null) {
                par.setTtl(ServerUtil.calculateTtl(jwtRequest.getExp()));
                par.setExpirationDate(new Date(jwtRequest.getExp() * 1000L));
            }
            if (!jwtRequest.getScopes().isEmpty()) { // JWT wins
                Set<String> scopes = scopeChecker.checkScopesPolicy(client, Lists.newArrayList(jwtRequest.getScopes()));
                par.getAttributes().setScope(implode(scopes, " "));
            }
            if (StringUtils.isNotBlank(jwtRequest.getRedirectUri())) {
                par.getAttributes().setRedirectUri(jwtRequest.getRedirectUri());
            }
            if (StringUtils.isNotBlank(jwtRequest.getNonce())) {
                par.getAttributes().setNonce(jwtRequest.getNonce());
            }
            if (StringUtils.isNotBlank(jwtRequest.getCodeChallenge())) {
                par.getAttributes().setCodeChallenge(jwtRequest.getCodeChallenge());
            }
            if (StringUtils.isNotBlank(jwtRequest.getCodeChallengeMethod())) {
                par.getAttributes().setCodeChallengeMethod(jwtRequest.getCodeChallengeMethod());
            }
            if (jwtRequest.getDisplay() != null && StringUtils.isNotBlank(jwtRequest.getDisplay().getParamName())) {
                par.getAttributes().setDisplay(jwtRequest.getDisplay().getParamName());
            }
            if (!jwtRequest.getPrompts().isEmpty()) {
                par.getAttributes().setPrompt(jwtRequest.getJsonPayload().optString("prompt"));
            }
            if (jwtRequest.getResponseMode() != null) {
                redirectUriResponse.getRedirectUri().setResponseMode(jwtRequest.getResponseMode());
                par.getAttributes().setResponseMode(jwtRequest.getJsonPayload().optString("response_mode"));
            }

            setParAttributesFromIdTokenMember(par, jwtRequest);
            requestParameterService.getCustomParameters(jwtRequest, par.getAttributes().getCustomParameters());
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Invalid JWT authorization request. Message : " + e.getMessage(), e);
            throw authorizeRestWebServiceValidator.createInvalidJwtRequestException(redirectUriResponse, "Invalid JWT authorization request");
        }
    }

    private void setParAttributesFromIdTokenMember(@NotNull Par par, @NotNull JwtAuthorizationRequest jwtRequest) {
        final IdTokenMember idTokenMember = jwtRequest.getIdTokenMember();
        if (idTokenMember == null) {
            return;
        }

        if (idTokenMember.getMaxAge() != null) {
            par.getAttributes().setMaxAge(idTokenMember.getMaxAge());
        }
        final Claim acrClaim = idTokenMember.getClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE);
        if (acrClaim != null && acrClaim.getClaimValue() != null) {
            par.getAttributes().setAcrValuesStr(acrClaim.getClaimValue().getValueAsString());
        }
    }

    private void setStateIntoPar(@NotNull RedirectUriResponse redirectUriResponse, @NotNull Par par, @NotNull JwtAuthorizationRequest jwtRequest) {
        if (StringUtils.isNotBlank(jwtRequest.getState())) {
            par.getAttributes().setState(jwtRequest.getState());
            redirectUriResponse.setState(jwtRequest.getState());
        }
        if (appConfiguration.isFapi() && StringUtils.isBlank(jwtRequest.getState())) {
            par.getAttributes().setState(""); // #1250 - FAPI : discard state if in JWT we don't have state
            redirectUriResponse.setState("");
        }
    }

    public void validatePkce(String codeChallenge, String codeChallengeMethod, String state) {
        if (!appConfiguration.isFapi()) {
            return;
        }
        if (StringUtils.isBlank(codeChallengeMethod) ||
                CodeVerifier.CodeChallengeMethod.fromString(codeChallengeMethod) == CodeVerifier.CodeChallengeMethod.PLAIN) {
            log.error("code_challenge_method is invalid: {} (plain or blank method is not allowed)", codeChallengeMethod);
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, ""))
                    .build());
        }
        if (StringUtils.isBlank(codeChallenge)) {
            log.error("code_challenge is blank");
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, ""))
                    .build());
        }
    }
}
