package io.jans.ca.plugin.adminui.rest.auth;

import io.jans.as.model.jwt.Jwt;
import io.jans.ca.plugin.adminui.model.auth.ApiTokenRequest;
import io.jans.ca.plugin.adminui.model.auth.TokenResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.auth.OAuth2Service;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.rest.ProtectedApi;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import io.jans.as.model.crypto.AuthCryptoProvider;
import java.util.UUID;

@Hidden
@Path("/app")
public class OAuth2Resource {
    //appType: admin-ui, ads
    static final String SESSION = "/{appType}/oauth2/session";
    static final String OAUTH2_CONFIG = "/{appType}/oauth2/config";
    static final String OAUTH2_API_PROTECTION_TOKEN = "/{appType}/oauth2/api-protection-token";
    static final String CONFIG_API_SESSION_ID = "config_api_session_id";
    public static final String SCOPE_OPENID = "openid";
    public static final String ADMINUI_CONF_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/properties.write";
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

            return Response.ok(true)
                    .cookie(cookie)
                    .build();
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

    @DELETE
    @Path(SESSION)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {ADMINUI_SESSION_DELETE}, superScopes = {ADMINUI_SESSION_DELETE})
    public Response deleteSession(@CookieParam(CONFIG_API_SESSION_ID) Cookie sessionCookie) {
        log.error("sessionCookie name: "+ sessionCookie.getName() + "value: "+ sessionCookie.getValue());
        //remove session from database
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
        return Response.ok("Session revoked successfully", MediaType.TEXT_PLAIN)
                .cookie(sessionCookieInvalidated)
                .build();
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
