package io.jans.ca.plugin.adminui.rest.auth;

import io.jans.as.model.jwt.Jwt;
import io.jans.ca.plugin.adminui.model.auth.ApiTokenRequest;
import io.jans.ca.plugin.adminui.model.auth.TokenResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.auth.OAuth2Service;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.util.security.StringEncrypter;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import io.jans.as.model.crypto.AuthCryptoProvider;
import java.util.UUID;

@Hidden
@Path("/app")
public class OAuth2Resource {
    //appType: admin-ui, ads
    static final String SESSION = "/{appType}/oauth2/session";
    static final String USER_DN_VARIABLE = "/{userDn}";
    static final String USER_DN_CONST = "userDn";
    static final String OAUTH2_API_PROTECTION_TOKEN = "/{appType}/oauth2/api-protection-token";
    static final String CONFIG_API_SESSION_ID = "config_api_session_id";
    public static final String SCOPE_OPENID = "openid";
    static final String ADMINUI_SESSION_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/session.write";
    static final String ADMINUI_SESSION_DELETE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/session.delete";

    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    OAuth2Service oAuth2Service;

    AuthCryptoProvider authCryptoProvider;

    @Inject
    ConfigurationFactory configurationFactory;

    @POST
    @Path(SESSION)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {ADMINUI_SESSION_WRITE}, superScopes = {ADMINUI_SESSION_WRITE})
    public Response createSession(@Valid @NotNull ApiTokenRequest apiTokenRequest) {
        try {
            //Bad Request: if UJWT is null
            if(apiTokenRequest.getUjwt() == null) {
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.UJWT_NOT_FOUND.getDescription());
            }

            String sessionId = UUID.randomUUID().toString();

            if(authCryptoProvider == null) {
                authCryptoProvider = new AuthCryptoProvider();
            }

            Jwt userInfoJwt = Jwt.parse(apiTokenRequest.getUjwt());
            boolean isValidJWTSignature = authCryptoProvider.verifySignature(userInfoJwt.getSigningInput(), userInfoJwt.getEncodedSignature(), userInfoJwt.getHeader().getKeyId(), new JSONObject(configurationFactory.getJwks()), null, userInfoJwt.getHeader().getSignatureAlgorithm());
            if(!isValidJWTSignature) {
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.SIGNATURE_NOT_VALID.getDescription());
            }

            NewCookie cookie = new NewCookie.Builder(CONFIG_API_SESSION_ID)
                    .value(sessionId)
                    .path("/")
                    .secure(true) // Set to true if using HTTPS
                    .httpOnly(true) // Makes the cookie inaccessible to JavaScript (recommended for auth tokens)
                    .sameSite(NewCookie.SameSite.NONE)
                    .build();

            oAuth2Service.setAdminUISession(sessionId, apiTokenRequest.getUjwt());

            return Response.ok(CommonUtils.createGenericResponse(true, 200, "Sessions created successfully."))
                    .cookie(cookie)
                    .build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ADMINUI_SESSION_CREATE_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error(ErrorResponse.ADMINUI_SESSION_CREATE_ERROR.getDescription(), e);
            return Response
                    .serverError()
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription()))
                    .build();
        }
    }

    @DELETE
    @Path(SESSION)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {ADMINUI_SESSION_DELETE}, superScopes = {ADMINUI_SESSION_DELETE})
    public Response deleteSessionBySessionCookie(@CookieParam(CONFIG_API_SESSION_ID) Cookie sessionCookie) {
        log.debug("Inside deleteSessionBySessionId method. Session Cookie name: "+ sessionCookie.getName() + "value: "+ sessionCookie.getValue());
        //remove session from database
        try {
            oAuth2Service.removeSession(sessionCookie.getValue());
            //remove session from browser
            NewCookie sessionCookieInvalidated = new NewCookie.Builder(CONFIG_API_SESSION_ID)
                    .value(sessionCookie.getValue())
                    .maxAge(0) // Sets Max-Age to 0, instructing the browser to delete it immediately
                    .path("/")
                    .httpOnly(true)
                    .secure(true)
                    .build();

            // Return a response with the new, invalidated cookie
            return Response.ok(CommonUtils.createGenericResponse(true, 200, "Sessions revoked successfully."))
                    .cookie(sessionCookieInvalidated)
                    .build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ADMINUI_SESSION_REMOVE_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path(SESSION + USER_DN_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {ADMINUI_SESSION_DELETE}, superScopes = {ADMINUI_SESSION_DELETE})
    public Response deleteSessionsByUserDn(@Parameter(description = "User DN") @PathParam(USER_DN_CONST) @NotNull String userDn) {
        log.debug("Inside deleteSessionsByUserDn method...");
        //remove sessions from database by userDn
        try {
            oAuth2Service.removeAdminUIUserSessionByDn(userDn);
            return Response.ok(CommonUtils.createGenericResponse(true, 200, "Sessions revoked successfully."))
                    .build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ADMINUI_SESSION_REMOVE_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        }
    }

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
