package io.jans.as.server.token.ws.rs;

import com.google.common.base.Strings;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ScopeConstants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.server.model.common.AbstractAuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.common.RefreshToken;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;

import static org.apache.commons.lang.BooleanUtils.isTrue;

/**
 * @author Yuriy Z
 */
@Stateless
@Named
public class TokenCreatorService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AttributeService attributeService;

    public boolean isRefreshTokenAllowed(Client client, String requestedScope, AbstractAuthorizationGrant grant) {
        if (isTrue(appConfiguration.getForceOfflineAccessScopeToEnableRefreshToken()) && !grant.getScopes().contains(ScopeConstants.OFFLINE_ACCESS) && !Strings.nullToEmpty(requestedScope).contains(ScopeConstants.OFFLINE_ACCESS)) {
            return false;
        }
        return Arrays.asList(client.getGrantTypes()).contains(GrantType.REFRESH_TOKEN);
    }

    @Nullable
    public RefreshToken createRefreshToken(@NotNull ExecutionContext executionContext, @NotNull String scope) {
        final AuthorizationGrant grant = executionContext.getGrant();
        if (!isRefreshTokenAllowed(executionContext.getClient(), scope, grant)) {
            return null;
        }

        checkUser(grant);

        final ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(executionContext.getHttpRequest(), grant, executionContext.getClient(), appConfiguration, attributeService);
        context.setExecutionContext(executionContext);

        final int refreshTokenLifetimeInSeconds = externalUpdateTokenService.getRefreshTokenLifetimeInSeconds(context);
        if (refreshTokenLifetimeInSeconds > 0) {
            return grant.createRefreshToken(executionContext, refreshTokenLifetimeInSeconds);
        }
        return grant.createRefreshToken(executionContext);
    }

    private void checkUser(AuthorizationGrant authorizationGrant) {
        if (BooleanUtils.isFalse(appConfiguration.getCheckUserPresenceOnRefreshToken())) {
            return;
        }

        final User user = authorizationGrant.getUser();
        if (user == null || "inactive".equalsIgnoreCase(user.getStatus())) {
            log.trace("The user associated with this grant is not found or otherwise with status=inactive.");
            throw new WebApplicationException(error(400, TokenErrorResponseType.INVALID_GRANT, "The user associated with this grant is not found or otherwise with status=inactive.").build());
        }
    }

    public Response.ResponseBuilder error(int status, TokenErrorResponseType type, String reason) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(type, reason));
    }
}
