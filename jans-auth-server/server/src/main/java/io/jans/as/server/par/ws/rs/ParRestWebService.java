package io.jans.as.server.par.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponse;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.util.QueryStringDecoder;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Par;
import io.jans.as.server.authorize.ws.rs.AuthorizeRestWebServiceValidator;
import io.jans.as.server.service.RedirectUriResponse;
import io.jans.as.server.service.RequestParameterService;
import io.jans.as.server.util.ServerUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Implementation based on https://datatracker.ietf.org/doc/html/draft-ietf-oauth-par-08
 *
 * @author Yuriy Zabrovarnyy
 */
@Path("/par")
public class ParRestWebService {

    @Inject
    private Logger log;

    @Inject
    private ParService parService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ParValidator parValidator;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private AuthorizeRestWebServiceValidator authorizeRestWebServiceValidator;

    @Inject
    private RequestParameterService requestParameterService;

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public Response requestPushedAuthorizationRequest(
            @FormParam("scope") String scope,
            @FormParam("response_type") String responseType,
            @FormParam("client_id") String clientId,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("state") String state,
            @FormParam("response_mode") String responseMode,
            @FormParam("nonce") String nonce,
            @FormParam("display") String display,
            @FormParam("prompt") String prompt,
            @FormParam("max_age") Integer maxAge,
            @FormParam("ui_locales") String uiLocales,
            @FormParam("id_token_hint") String idTokenHint,
            @FormParam("login_hint") String loginHint,
            @FormParam("acr_values") String acrValuesStr,
            @FormParam("amr_values") String amrValuesStr,
            @FormParam("request") String request,
            @FormParam("request_uri") String requestUri,
            @FormParam("session_id") String sessionId,
            @FormParam("origin_headers") String originHeaders,
            @FormParam("code_challenge") String codeChallenge,
            @FormParam("code_challenge_method") String codeChallengeMethod,
            @FormParam("nbf") String nbf,
            @FormParam(AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS) String customResponseHeaders,
            @FormParam("claims") String claims,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext securityContext) {
        try {
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.PAR);

            scope = ServerUtil.urlDecode(scope); // it may be encoded
            String tokenBindingHeader = httpRequest.getHeader("Sec-Token-Binding");

            // ATTENTION : please do not add more parameter in this debug method because it will not work with framework
            // there is limit of 10 parameters (hardcoded), see: org.jboss.seam.core.Interpolator#interpolate
            log.debug("Attempting to request PAR: "
                            + "responseType = {}, clientId = {}, scope = {}, redirectUri = {}, nonce = {}, "
                            + "state = {}, request = {}, isSecure = {}, sessionId = {}",
                    responseType, clientId, scope, redirectUri, nonce,
                    state, request, securityContext.isSecure(), sessionId);

            log.debug("Attempting to request PAR: "
                            + "acrValues = {}, amrValues = {}, originHeaders = {}, codeChallenge = {}, codeChallengeMethod = {}, "
                            + "customRespHeaders = {}, claims = {}, tokenBindingHeader = {}",
                    acrValuesStr, amrValuesStr, originHeaders, codeChallenge, codeChallengeMethod, customResponseHeaders, claims, tokenBindingHeader);

            List<ResponseType> responseTypes = ResponseType.fromString(responseType, " ");
            ResponseMode responseModeObj = ResponseMode.getByValue(responseMode);

            Jwt requestObject = Jwt.parseSilently(request);
            clientId = getClientId(clientId, requestObject);

            Client client = authorizeRestWebServiceValidator.validateClient(clientId, state, true);

            redirectUri = getRedirectUri(redirectUri, requestObject);
            redirectUri = authorizeRestWebServiceValidator.validateRedirectUri(client, redirectUri, state, null, httpRequest, AuthorizeErrorResponseType.INVALID_REQUEST);

            RedirectUriResponse redirectUriResponse = new RedirectUriResponse(new RedirectUri(redirectUri, responseTypes, responseModeObj), state, httpRequest, errorResponseFactory);
            redirectUriResponse.setFapiCompatible(appConfiguration.isFapi());

            parValidator.validateRequestUriIsAbsent(requestUri);

            final Integer parLifetime = client.getAttributes().getParLifetime();

            final Par par = new Par();
            par.setDeletable(true);
            par.setTtl(parLifetime);
            par.setExpirationDate(Util.createExpirationDate(parLifetime));
            par.getAttributes().setScope(scope);
            par.getAttributes().setNbf(Util.parseIntegerSilently(nbf));
            par.getAttributes().setResponseType(responseType);
            par.getAttributes().setClientId(clientId);
            par.getAttributes().setRedirectUri(redirectUri);
            par.getAttributes().setState(state);
            par.getAttributes().setResponseMode(responseMode);
            par.getAttributes().setNonce(nonce);
            par.getAttributes().setDisplay(display);
            par.getAttributes().setPrompt(prompt);
            par.getAttributes().setMaxAge(maxAge);
            par.getAttributes().setUiLocales(uiLocales);
            par.getAttributes().setIdTokenHint(idTokenHint);
            par.getAttributes().setLoginHint(loginHint);
            par.getAttributes().setAcrValuesStr(acrValuesStr);
            par.getAttributes().setAmrValuesStr(amrValuesStr);
            par.getAttributes().setRequest(request);
            par.getAttributes().setRequestUri(requestUri);
            par.getAttributes().setSessionId(sessionId);
            par.getAttributes().setOriginHeaders(originHeaders);
            par.getAttributes().setCodeChallenge(codeChallenge);
            par.getAttributes().setCodeChallengeMethod(codeChallengeMethod);
            par.getAttributes().setCustomResponseHeaders(customResponseHeaders);
            par.getAttributes().setClaims(claims);
            par.getAttributes().setCustomParameters(requestParameterService.getCustomParameters(QueryStringDecoder.decode(httpRequest.getQueryString())));

            parValidator.validateRequestObject(redirectUriResponse, par, client);

            parValidator.validatePkce(par.getAttributes().getCodeChallenge(), par.getAttributes().getCodeChallengeMethod(), state);
            authorizeRestWebServiceValidator.validatePkce(par.getAttributes().getCodeChallenge(), redirectUriResponse);

            parService.persist(par);

            ParResponse parResponse = new ParResponse();
            parResponse.setRequestUri(ParService.toOutsideId(par.getId()));
            parResponse.setExpiresIn(par.getTtl()); // set it to TTL instead of lifetime because TTL can be updated during request object validation

            final String responseAsString = ServerUtil.asJson(parResponse);

            log.debug("Created PAR {}", responseAsString);

            return Response.status(Response.Status.CREATED).entity(responseAsString).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.FOUND.getStatusCode()) {
                throw errorResponseFactory.createBadRequestException(createErrorResponseFromRedirectErrorUri(e.getResponse().getLocation()));
            }

            if (log.isErrorEnabled())
                log.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    @NotNull
    private ErrorResponse createErrorResponseFromRedirectErrorUri(@NotNull URI location) {
        final RedirectUri locationRedirect = new RedirectUri(location.toString());
        locationRedirect.parseQueryString(location.getQuery());

        final ErrorResponse response = new ErrorResponse();

        String error = locationRedirect.getResponseParameter("error");
        String errorDescription = locationRedirect.getResponseParameter("error_description");
        errorDescription = Optional.ofNullable(errorDescription)
                .map(description -> Optional.ofNullable(ThreadContext.get(Constants.CORRELATION_ID_HEADER))
                        .map(id -> description.concat(" CorrelationId: " + id))
                        .orElse(description))
                .orElse(null);

        response.setErrorCode(error);
        response.setErrorDescription(errorDescription);
        return response;
    }

    private String getRedirectUri(String redirectUri, Jwt requestJwt) {
        if (StringUtils.isNotBlank(redirectUri) || requestJwt == null)
            return redirectUri;

        final String valueFromJwt = requestJwt.getClaims().getClaimAsString("redirect_uri");
        log.trace("redirectUriFromJwt: {}", valueFromJwt);
        return valueFromJwt;
    }

    private String getClientId(String clientId, Jwt requestJwt) {
        if (StringUtils.isNotBlank(clientId) || requestJwt == null)
            return clientId;

        final String valueFromJwt = requestJwt.getClaims().getClaimAsString("client_id");
        log.trace("clientIdFromJwt: {}", valueFromJwt);
        return valueFromJwt;
    }

    @PUT
    public Response unsupportedPutMethod() {
        log.error("PUT method is not allowed");
        throw new WebApplicationException(Response.status(Response.Status.METHOD_NOT_ALLOWED).entity("GET Method Not Allowed").build());
    }

    @GET
    public Response unsupportedGetMethod() {
        log.error("GET method is not allowed");
        throw new WebApplicationException(Response.status(Response.Status.METHOD_NOT_ALLOWED).entity("GET Method Not Allowed").build());
    }

    @HEAD
    public Response unsupportedHeadMethod() {
        log.error("HEAD method is not allowed");
        throw new WebApplicationException(Response.status(Response.Status.METHOD_NOT_ALLOWED).entity("HEAD Method Not Allowed").build());
    }

    @OPTIONS
    public Response unsupportedOptionsMethod() {
        log.error("OPTIONS method is not allowed");
        throw new WebApplicationException(Response.status(Response.Status.METHOD_NOT_ALLOWED).entity("OPTIONS Method Not Allowed").build());
    }
}
