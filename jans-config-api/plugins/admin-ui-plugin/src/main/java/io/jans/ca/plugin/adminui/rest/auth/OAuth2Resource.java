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

import java.security.KeyStoreException;
import java.util.UUID;

@Hidden
@Path("/app")
public class OAuth2Resource {
    //appType: admin-ui, ads
    static final String SESSION = "/{appType}/oauth2/session";
    static final String USER_DN_VARIABLE = "/{userDn}";
    static final String USER_DN_CONST = "userDn";
    static final String OAUTH2_API_PROTECTION_TOKEN = "/{appType}/oauth2/api-protection-token";
    static final String ADMIN_UI_SESSION_ID = "admin_ui_session_id";
    public static final String SCOPE_OPENID = "openid";
    static final String ADMINUI_SESSION_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/session.write";
    static final String ADMINUI_SESSION_DELETE = "https://jans.io/oauth/jans-auth-server/config/adminui/user/session.delete";

    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    OAuth2Service oAuth2Service;

    @Inject
    ConfigurationFactory configurationFactory;

    private volatile AuthCryptoProvider authCryptoProvider;
    private final Object cryptoProviderLock = new Object();

    private AuthCryptoProvider getAuthCryptoProvider() throws KeyStoreException {
        if (authCryptoProvider == null) {
            synchronized (cryptoProviderLock) {
                if (authCryptoProvider == null) {
                    authCryptoProvider = new AuthCryptoProvider();
                }
            }
        }
        return authCryptoProvider;
    }

    /**
     * Create an Admin UI session and set an HttpOnly, Secure session cookie.
     * <p>
     * Validates that the provided API token request contains a user JWT (UJWT) and that the JWT's signature is valid
     * against the configured JWKS; on success a new session is persisted and a Set-Cookie header with the
     * `admin_ui_session_id` is returned.
     *
     * @param apiTokenRequest request containing the UJWT used to authenticate and create the session; must include a signed UJWT
     * @return a Response with a success entity and a Set-Cookie header containing the `admin_ui_session_id` on success;
     * on failure the Response contains an error entity and an appropriate HTTP status code
     */
    @POST
    @Path(SESSION)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {ADMINUI_SESSION_WRITE}, superScopes = {ADMINUI_SESSION_WRITE})
    public Response createSession(@Valid @NotNull ApiTokenRequest apiTokenRequest) {
        try {
            //Bad Request: if UJWT is null
            if (apiTokenRequest.getUjwt() == null) {
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.UJWT_NOT_FOUND.getDescription());
            }

            String sessionId = UUID.randomUUID().toString();

            authCryptoProvider = getAuthCryptoProvider();


            Jwt userInfoJwt = Jwt.parse(apiTokenRequest.getUjwt());
            boolean isValidJWTSignature = authCryptoProvider.verifySignature(userInfoJwt.getSigningInput(), userInfoJwt.getEncodedSignature(), userInfoJwt.getHeader().getKeyId(), new JSONObject(configurationFactory.getJwks()), null, userInfoJwt.getHeader().getSignatureAlgorithm());
            if (!isValidJWTSignature) {
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.SIGNATURE_NOT_VALID.getDescription());
            }

            String cookie =
                    ADMIN_UI_SESSION_ID + "=" + sessionId +
                            "; Path=/" +
                            "; HttpOnly" +
                            "; Secure" +
                            "; SameSite=None";
            oAuth2Service.setAdminUISession(sessionId, apiTokenRequest.getUjwt());

            return Response.ok(CommonUtils.createGenericResponse(true, 200, "Admin UI Session created successfully."))
                    .header(HttpHeaders.SET_COOKIE, cookie)
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
                    .entity(CommonUtils.createGenericResponse(false, 500, ErrorResponse.ADMINUI_SESSION_CREATE_ERROR.getDescription()))
                    .build();
        }
    }

    /**
     * Revokes the Admin UI session identified by the provided session cookie and invalidates that cookie in the client.
     *
     * @param sessionCookie the request cookie named "admin_ui_session_id" identifying the session to remove
     * @return a Response containing a generic success payload and a Set-Cookie header that clears the session cookie; on failure a Response with the error status and message
     */
    @DELETE
    @Path(SESSION)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {ADMINUI_SESSION_DELETE}, superScopes = {ADMINUI_SESSION_DELETE})
    public Response deleteSessionBySessionCookie(@CookieParam(ADMIN_UI_SESSION_ID) Cookie sessionCookie) {
        if (sessionCookie == null || sessionCookie.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(CommonUtils.createGenericResponse(false, 400, "Session cookie not found"))
                    .build();
        }
        log.debug("Inside deleteSessionBySessionCookie method. Session Cookie name: {}", sessionCookie.getName());
        //remove session from database
        try {
            oAuth2Service.removeSession(sessionCookie.getValue());
            //remove session from browser
            String cookie =
                    ADMIN_UI_SESSION_ID + "=" + sessionCookie.getValue() +
                            "; Path=/" +
                            "; Max-Age=0" +
                            "; HttpOnly" +
                            "; Secure" +
                            "; SameSite=None";

            // Return a response with the new, invalidated cookie
            return Response.ok(CommonUtils.createGenericResponse(true, 200, "Admin UI Session revoked successfully."))
                    .header(HttpHeaders.SET_COOKIE, cookie)
                    .build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ADMINUI_SESSION_REMOVE_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        }
    }

    /**
     * Revoke all Admin UI sessions associated with the specified user DN.
     *
     * @param userDn the user's distinguished name whose Admin UI sessions will be revoked
     * @return an HTTP response: 200 with a success payload when sessions are revoked; otherwise a response
     * with the error status code and an error payload describing the failure
     */
    @DELETE
    @Path(SESSION + USER_DN_VARIABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = {ADMINUI_SESSION_DELETE}, superScopes = {ADMINUI_SESSION_DELETE})
    public Response deleteSessionsByUserDn(@Parameter(description = "User DN") @PathParam(USER_DN_CONST) @NotNull String userDn) {
        log.debug("Inside deleteSessionsByUserDn method...");
        // Validate DN format to prevent malicious injection
        if (!userDn.matches("^inum=[a-zA-Z0-9\\-]+,ou=people,o=jans$")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(CommonUtils.createGenericResponse(false, 400, "Invalid user DN format"))
                    .build();
        }
        //remove sessions from database by userDn
        try {
            oAuth2Service.removeAdminUIUserSessionByDn(userDn);
            return Response.ok(CommonUtils.createGenericResponse(true, 200, "Admin UI Sessions revoked successfully."))
                    .build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.ADMINUI_SESSION_REMOVE_ERROR.getDescription(), e);
            return Response
                    .status(e.getErrorCode())
                    .entity(CommonUtils.createGenericResponse(false, e.getErrorCode(), e.getMessage()))
                    .build();
        }
    }

    /**
     * Requests an API protection token from the OAuth2 service for the specified app type.
     *
     * @param apiTokenRequest the credentials and parameters required to request the token
     * @param appType         the application type for which the token is requested
     * @return a Response whose entity is a TokenResponse on success, or a generic error payload with an HTTP error status on failure
     */
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