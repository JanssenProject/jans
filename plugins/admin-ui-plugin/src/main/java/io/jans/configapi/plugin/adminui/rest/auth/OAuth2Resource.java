package io.jans.configapi.plugin.adminui.rest.auth;

import io.jans.configapi.plugin.adminui.model.auth.OAuth2ConfigResponse;
import io.jans.configapi.plugin.adminui.model.auth.TokenResponse;
import io.jans.configapi.plugin.adminui.model.auth.UserInfoRequest;
import io.jans.configapi.plugin.adminui.model.auth.UserInfoResponse;
import io.jans.configapi.plugin.adminui.model.config.AUIConfiguration;
import io.jans.configapi.plugin.adminui.service.auth.OAuth2Service;
import io.jans.configapi.plugin.adminui.service.config.AUIConfigurationService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/admin-ui/oauth2")
public class OAuth2Resource {

    static final String OAUTH2_CONFIG = "/config";
    static final String OAUTH2_ACCESS_TOKEN = "/access-token";
    static final String OAUTH2_API_PROTECTION_TOKEN = "/api-protection-token";
    static final String OAUTH2_API_USER_INFO = "/user-info";

    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    OAuth2Service oAuth2Service;

    @GET
    @Path(OAUTH2_CONFIG)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOAuth2Config() throws Exception {

        AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

        OAuth2ConfigResponse oauth2Config = new OAuth2ConfigResponse();
        oauth2Config.setAuthzBaseUrl(auiConfiguration.getAuthServerAuthzBaseUrl());
        oauth2Config.setClientId(auiConfiguration.getAuthServerClientId());
        oauth2Config.setResponseType("code");
        oauth2Config.setScope(auiConfiguration.getAuthServerScope());
        oauth2Config.setRedirectUrl(auiConfiguration.getAuthServerRedirectUrl());
        oauth2Config.setAcrValues("simple_password_auth");
        oauth2Config.setFrontChannelLogoutUrl(auiConfiguration.getAuthServerFrontChannelLogoutUrl());
        oauth2Config.setPostLogoutRedirectUri(auiConfiguration.getAuthServerPostLogoutRedirectUri());
        oauth2Config.setEndSessionEndpoint(auiConfiguration.getAuthServerEndSessionEndpoint());

        return Response.ok(oauth2Config).build();
    }

    @GET
    @Path(OAUTH2_ACCESS_TOKEN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccessToken(@QueryParam("code") String code) throws Exception {

        try {
            log.info("Access token request to Auth Server.");
            TokenResponse tokenResponse = oAuth2Service.getAccessToken(code);
            log.info("Access token gotten from Auth Server: {}", tokenResponse.toString());
            return Response.ok(tokenResponse).build();
        } catch (Exception e) {
            log.error("Problems getting access token", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path(OAUTH2_API_PROTECTION_TOKEN)
    public Response getApiProtectionToken(@QueryParam("ujwt") String ujwt) throws Exception {
        try {
            log.info("Api protection token request to Auth Server.");
            TokenResponse tokenResponse = oAuth2Service.getApiProtectionToken(ujwt);
            log.info("Api protection token gotten from Auth Server: {}", tokenResponse.toString());
            return Response.ok(tokenResponse).build();
        } catch (Exception e) {
            log.error("Problems getting access token", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path(OAUTH2_API_USER_INFO)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserInfo(@Valid @NotNull UserInfoRequest userInfoRequest) throws Exception {
        try {
            log.info("Get User-Info request to IdP: {}", userInfoRequest.toString());
            UserInfoResponse userInfoResponse = oAuth2Service.getUserInfo(userInfoRequest);
            log.info("Get User-Info gotten from IdP: {}", userInfoRequest.toString());
            return Response.ok(userInfoResponse).build();
        } catch (Exception e) {
            log.error("Problems getting access token", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
