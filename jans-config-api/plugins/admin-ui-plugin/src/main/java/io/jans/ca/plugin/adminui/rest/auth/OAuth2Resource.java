package io.jans.ca.plugin.adminui.rest.auth;

import io.jans.ca.plugin.adminui.model.auth.OAuth2ConfigResponse;
import io.jans.ca.plugin.adminui.model.auth.TokenResponse;
import io.jans.ca.plugin.adminui.model.auth.UserInfoRequest;
import io.jans.ca.plugin.adminui.model.auth.UserInfoResponse;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.auth.OAuth2Service;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.rest.ProtectedApi;

import io.swagger.v3.oas.annotations.Hidden;

import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Hidden
@Path("/app")
public class OAuth2Resource {

    static final String OAUTH2_CONFIG = "/{appType}/oauth2/config";
    static final String OAUTH2_ACCESS_TOKEN = "/{appType}/oauth2/access-token";
    static final String OAUTH2_API_PROTECTION_TOKEN = "/{appType}/oauth2/api-protection-token";
    static final String OAUTH2_API_USER_INFO = "/{appType}/oauth2/user-info";

    public static final String SCOPE_OPENID = "openid";

    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    OAuth2Service oAuth2Service;

    @GET
    @Path(OAUTH2_CONFIG)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {SCOPE_OPENID})
    public Response getOAuth2Config(@PathParam("appType") String appType) {

        AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration(appType);

        OAuth2ConfigResponse oauth2Config = new OAuth2ConfigResponse();
        oauth2Config.setAuthzBaseUrl(auiConfiguration.getAuthServerAuthzBaseUrl());
        oauth2Config.setClientId(auiConfiguration.getAuthServerClientId());
        oauth2Config.setResponseType("code");
        oauth2Config.setScope(auiConfiguration.getAuthServerScope());
        oauth2Config.setRedirectUrl(auiConfiguration.getAuthServerRedirectUrl());
        oauth2Config.setAcrValues(auiConfiguration.getAuthServerAcrValues());
        oauth2Config.setFrontChannelLogoutUrl(auiConfiguration.getAuthServerFrontChannelLogoutUrl());
        oauth2Config.setPostLogoutRedirectUri(auiConfiguration.getAuthServerPostLogoutRedirectUri());
        oauth2Config.setEndSessionEndpoint(auiConfiguration.getAuthServerEndSessionEndpoint());

        return Response.ok(oauth2Config).build();
    }

    @GET
    @Path(OAUTH2_ACCESS_TOKEN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccessToken(@QueryParam("code") String code, @PathParam("appType") String appType) {

        try {
            log.info("Access token request to Auth Server.");
            TokenResponse tokenResponse = oAuth2Service.getAccessToken(code, appType);
            log.info("Access token received from Auth Server.");
            return Response.ok(tokenResponse).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_ACCESS_TOKEN_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_ACCESS_TOKEN_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path(OAUTH2_API_PROTECTION_TOKEN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiProtectionToken(@QueryParam("ujwt") String ujwt, @PathParam("appType") String appType) {
        try {
            log.info("Api protection token request to Auth Server.");
            TokenResponse tokenResponse = oAuth2Service.getApiProtectionToken(ujwt, appType);
            log.info("Api protection token received from Auth Server.");
            return Response.ok(tokenResponse).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path(OAUTH2_API_USER_INFO)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserInfo(@Valid @NotNull UserInfoRequest userInfoRequest, @PathParam("appType") String appType) {
        try {
            log.info("Get User-Info request to Auth Server.");
            UserInfoResponse userInfoResponse = oAuth2Service.getUserInfo(userInfoRequest, appType);
            log.info("Get User-Info received from Auth Server.");
            return Response.ok(userInfoResponse).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.GET_USER_INFO_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_USER_INFO_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
