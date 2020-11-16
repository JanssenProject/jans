package io.jans.configapi.auth.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/token")
@RegisterRestClient(configKey="uma-token")
public interface UMATokenService {
    @POST
    @Path("/token")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestAccessToken(
            @FormParam("grant_type")
            String grantType,
            @FormParam("code")
            String code,
            @FormParam("redirect_uri")
            String redirectUri,
            @FormParam("username")
            String username,
            @FormParam("password")
            String password,
            @FormParam("scope")
            String scope,
            @FormParam("assertion")
            String assertion,
            @FormParam("refresh_token")
            String refreshToken,
            @FormParam("client_id")
            String clientId,
            @FormParam("client_secret")
            String clientSecret,
            @FormParam("code_verifier")
            String codeVerifier,
            @FormParam("ticket")
            String ticket,
            @FormParam("claim_token")
            String claimToken,
            @FormParam("claim_token_format")
            String claimTokenFormat,
            @FormParam("pct")
            String pctCode,
            @FormParam("rpt")
            String rptCode,
            @FormParam("auth_req_id")
            String authReqId,
            @FormParam("device_code")
            String deviceCode,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Context SecurityContext sec);
}

