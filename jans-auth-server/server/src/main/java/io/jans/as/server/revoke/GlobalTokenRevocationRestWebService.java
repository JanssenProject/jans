package io.jans.as.server.revoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.revoke.GlobalTokenRevocationRequest;
import io.jans.as.model.session.EndSessionErrorResponseType;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.UserService;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.token.TokenEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Z
 */
@Path("/")
public class GlobalTokenRevocationRestWebService {

    @Inject
    private Logger log;

    @Inject
    private UserService userService;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private Identity identity;

    @Inject
    private ScopeService scopeService;

    @Inject
    private GrantService grantService;

    @POST
    @Path("/global-token-revocation")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response requestGlobalTokenRevocation(String requestAsString) {
        try {
            log.debug("Attempt for global token revocation: request = {}, ", requestAsString);

            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.GLOBAL_TOKEN_REVOCATION);

            validateAccess();

            GlobalTokenRevocationRequest parsedRequest = parseRequest(requestAsString);

            final String key = parsedRequest.getSubId().getFormat();
            final String value = parsedRequest.getSubId().getId();

            final User user = userService.getUserByAttribute(key, value);
            if (user == null) {
                log.trace("Unable to find user by {}={}", key, value);
                return Response.noContent().build(); // no error because we don't want to disclose internal AS info about users
            }

            // remove sessions
            List<SessionId> sessionIdList = sessionIdService.findByUser(user.getDn());
            sessionIdService.remove(sessionIdList);

            log.debug("Revoked {} user's sessions (user: {})", sessionIdList != null ? sessionIdList.size() : 0, user.getUserId());

            // remove tokens
            final List<TokenEntity> grants = grantService.getGrantsByUserId(user.getUserId());
            grantService.removeSilently(grants);

            return Response.noContent().build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(500).build();
        }
    }

    private void validateAccess() {
        SessionClient sessionClient = identity.getSessionClient();
        if (sessionClient == null || sessionClient.getClient() == null || ArrayUtils.isEmpty(sessionClient.getClient().getScopes())) {
            log.debug("Client failed to authenticate.");
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED.getStatusCode())
                            .entity(errorResponseFactory.getErrorAsJson(EndSessionErrorResponseType.INVALID_REQUEST))
                            .build());
        }

        List<String> scopesAllowedIds = scopeService.getScopeIdsByDns(Arrays.asList(sessionClient.getClient().getScopes()));

        if (!scopesAllowedIds.contains(Constants.GLOBAL_TOKEN_REVOCATION_SCOPE)) {
            log.debug("Client does not have required global_token_revocation scope.");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED.getStatusCode())
                    .entity(errorResponseFactory.getErrorAsJson(EndSessionErrorResponseType.INVALID_REQUEST))
                    .build());
        }
    }

    private GlobalTokenRevocationRequest parseRequest(String requestAsString) {
        final ObjectMapper mapper = ServerUtil.createJsonMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        try {
            return mapper.readValue(requestAsString, GlobalTokenRevocationRequest.class);
        } catch (IOException e) {
            log.error("Failed to parse " + requestAsString, e);
        }

        throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, AuthorizeErrorResponseType.INVALID_REQUEST, "Failed to parse GlobalTokenRevocationRequest.");
    }
}
