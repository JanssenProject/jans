package io.jans.ca.plugin.adminui.rest.auth;

import io.jans.ca.plugin.adminui.model.auth.ApiTokenRequest;
import io.jans.ca.plugin.adminui.model.auth.TokenResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.auth.OAuth2Service;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

@Hidden
@Path("/app")
public class OAuth2Resource {
    //appType: admin-ui, ads
    static final String OAUTH2_CONFIG = "/{appType}/oauth2/config";
    static final String OAUTH2_API_PROTECTION_TOKEN = "/{appType}/oauth2/api-protection-token";

    public static final String SCOPE_OPENID = "openid";

    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    OAuth2Service oAuth2Service;

    @POST
    @Path(OAUTH2_API_PROTECTION_TOKEN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiProtectionToken(@Valid @NotNull ApiTokenRequest apiTokenRequest, @PathParam("appType") String appType) {
        try {
            log.info("Api protection token request to Auth Server.");
            TokenResponse tokenResponse = oAuth2Service.getApiProtectionToken(apiTokenRequest, appType);
            log.info("Api protection token received from Auth Server.");
            return Response.ok(tokenResponse).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription()))
                    .build();
        }
    }

}
