package io.jans.as.server.authorize.ws.rs;

import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.util.Util;
import io.jans.as.persistence.model.Par;
import io.jans.as.server.par.ws.rs.ParService;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.util.Map;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named
public class AuthzRequestService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ParService parService;


    public boolean processPar(AuthzRequest authzRequest, Map<String, String> customParameters) {
        boolean isPar = Util.isPar(authzRequest.getRequestUri());
        if (!isPar && isTrue(appConfiguration.getRequirePar())) {
            log.debug("Server configured for PAR only (via requirePar conf property). Failed to find PAR by request_uri (id): {}", authzRequest.getRequestUri());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, authzRequest.getState(), "Failed to find par by request_uri"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }

        if (!isPar) {
            return false;
        }

        final Par par = parService.getParAndValidateForAuthorizationRequest(authzRequest.getRequestUri(), authzRequest.getState(), authzRequest.getClientId());

        authzRequest.setRequestUri(null); // set it to null, we don't want to follow request uri for PAR
        authzRequest.setRequest(null); // request is validated and parameters parsed by PAR endpoint before PAR persistence

        log.debug("Setting request parameters from PAR - {}", par);

        authzRequest.setResponseType(par.getAttributes().getResponseType());
        authzRequest.setResponseMode(par.getAttributes().getResponseMode());
        authzRequest.setScope(par.getAttributes().getScope());
        authzRequest.setPrompt(par.getAttributes().getPrompt());
        authzRequest.setRedirectUri(par.getAttributes().getRedirectUri());
        authzRequest.setAcrValues(par.getAttributes().getAcrValuesStr());
        authzRequest.setAmrValues(par.getAttributes().getAmrValuesStr());
        authzRequest.setCodeChallenge(par.getAttributes().getCodeChallenge());
        authzRequest.setCodeChallengeMethod(par.getAttributes().getCodeChallengeMethod());

        authzRequest.setState(StringUtils.isNotBlank(par.getAttributes().getState()) ? par.getAttributes().getState() : "");

        if (StringUtils.isNotBlank(par.getAttributes().getNonce()))
            authzRequest.setNonce(par.getAttributes().getNonce());
        if (StringUtils.isNotBlank(par.getAttributes().getSessionId()))
            authzRequest.setSessionId(par.getAttributes().getSessionId());
        if (StringUtils.isNotBlank(par.getAttributes().getCustomResponseHeaders()))
            authzRequest.setCustomResponseHeaders(par.getAttributes().getCustomResponseHeaders());
        if (StringUtils.isNotBlank(par.getAttributes().getClaims()))
            authzRequest.setClaims(par.getAttributes().getClaims());
        if (StringUtils.isNotBlank(par.getAttributes().getOriginHeaders()))
            authzRequest.setOriginHeaders(par.getAttributes().getOriginHeaders());
        if (StringUtils.isNotBlank(par.getAttributes().getUiLocales()))
            authzRequest.setUiLocales(par.getAttributes().getUiLocales());
        if (!par.getAttributes().getCustomParameters().isEmpty())
            customParameters.putAll(par.getAttributes().getCustomParameters());

        return isPar;
    }
}
