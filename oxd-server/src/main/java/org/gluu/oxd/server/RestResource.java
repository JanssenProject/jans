package org.gluu.oxd.server;

import io.dropwizard.util.Strings;
import io.opentracing.Scope;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.IntrospectionResponse;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.CommandType;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.common.params.*;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.POJOResponse;
import org.gluu.oxd.server.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

@Path("/")
public class RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    @Context
    private UriInfo uriInfo;

    public RestResource() {
    }

    @GET
    @Path("/health-check")
    @Produces(MediaType.APPLICATION_JSON)
    public String healthCheck() {
        return "{\"status\":\"running\"}";
    }

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getClientToken(String params) {
        return process(CommandType.GET_CLIENT_TOKEN, params, GetClientTokenParams.class, null, null, uriInfo);
    }

    @POST
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectAccessToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.INTROSPECT_ACCESS_TOKEN, params, IntrospectAccessTokenParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectRpt(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.INTROSPECT_RPT, params, IntrospectRptParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String registerSite(String params) {
        return process(CommandType.REGISTER_SITE, params, RegisterSiteParams.class, null, null, uriInfo);
    }

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String updateSite(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.UPDATE_SITE, params, UpdateSiteParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String removeSite(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.REMOVE_SITE, params, RemoveSiteParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationUrl(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_AUTHORIZATION_URL, params, GetAuthorizationUrlParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/get-authorization-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationCode(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_AUTHORIZATION_CODE, params, GetAuthorizationCodeParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getTokenByCode(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_TOKENS_BY_CODE, params, GetTokensByCodeParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getUserInfo(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_USER_INFO, params, GetUserInfoParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getLogoutUri(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_LOGOUT_URI, params, GetLogoutUrlParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAccessTokenByRefreshToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN, params, GetAccessTokenByRefreshTokenParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsProtect(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.RS_PROTECT, params, RsProtectParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/uma-rs-modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsModify(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.RS_MODIFY, params, RsModifyParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsCheckAccess(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.RS_CHECK_ACCESS, params, RsCheckAccessParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetRpt(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.RP_GET_RPT, params, RpGetRptParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.RP_GET_CLAIMS_GATHERING_URL, params, RpGetClaimsGatheringUrlParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/authorization-code-flow")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String authorizationCodeFlow(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.AUTHORIZATION_CODE_FLOW, params, AuthorizationCodeFlowParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/check-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String checkAccessToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.CHECK_ACCESS_TOKEN, params, CheckAccessTokenParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/check-id-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String checkIdToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.CHECK_ID_TOKEN, params, CheckIdTokenParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/get-rp")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getRp(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_RP, params, GetRpParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/get-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getJwks(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_JWKS, params, GetJwksParams.class, authorization, authorizationOxdId, uriInfo);
    }

    @POST
    @Path("/get-discovery")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getDiscovery(String params) {
        return process(CommandType.GET_DISCOVERY, params, GetDiscoveryParams.class, null, null, uriInfo);
    }

    public static <T> T read(String params, Class<T> clazz) {
        try {
            return Jackson2.createJsonMapper().readValue(params, clazz);
        } catch (IOException e) {
            TracingUtil.errorLog(e);
            LOG.error("Invalid params: " + params, e);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid parameters. Message: " + e.getMessage()).build());
        }
    }

    private static <T extends IParams> String process(CommandType commandType, String paramsAsString, Class<T> paramsClass, String authorization, String authorizationOxdId, UriInfo uriInfo) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(commandType.toString(), true)) {
            TracingUtil.setTag("end-point", uriInfo.getAbsolutePath().toString());
            TracingUtil.log("Request parameters: " + paramsAsString);
            TracingUtil.log("CommandType: " + commandType);
            Object forJsonConversion = getObjectForJsonConversion(commandType, paramsAsString, paramsClass, authorization, authorizationOxdId);
            final String json = Jackson2.asJsonSilently(forJsonConversion);
            TracingUtil.log("Send back response: " + json);
            LOG.trace("Send back response: {}", json);
            return json;
        }
    }

    private static <T extends IParams> Object getObjectForJsonConversion(CommandType commandType, String paramsAsString, Class<T> paramsClass, String authorization, String authorizationOxdId) {
        LOG.trace("Command: {}", paramsAsString);
        T params = read(paramsAsString, paramsClass);

        final OxdServerConfiguration conf = ServerLauncher.getInjector().getInstance(ConfigurationService.class).get();

        if (commandType.isAuthorizationRequired()) {
            validateAuthorizationOxdId(conf, authorizationOxdId);
            validateAccessToken(authorization, safeToOxdId((HasOxdIdParams) params, authorizationOxdId));
        }

        Command command = new Command(commandType, params);
        final IOpResponse response = ServerLauncher.getInjector().getInstance(Processor.class).process(command);
        Object forJsonConversion = response;
        if (response instanceof POJOResponse) {
            forJsonConversion = ((POJOResponse) response).getNode();
        }
        return forJsonConversion;
    }

    private static void validateAuthorizationOxdId(OxdServerConfiguration conf, String authorizationOxdId) {

        if (Strings.isNullOrEmpty(authorizationOxdId)) {
            return;
        }

        final RpSyncService rpSyncService = ServerLauncher.getInjector().getInstance(RpSyncService.class);
        final Rp rp = rpSyncService.getRp(authorizationOxdId);

        if (rp == null || Strings.isNullOrEmpty(rp.getOxdId())) {
            LOG.debug("`oxd_id` in `AuthorizationOxdId` header is not registered in oxd.");
            throw new HttpException(ErrorResponseCode.AUTHORIZATION_OXD_ID_NOT_FOUND);
        }

        if (conf.getProtectCommandsWithOxdId() == null || conf.getProtectCommandsWithOxdId().isEmpty()) {
            return;
        }

        if (!conf.getProtectCommandsWithOxdId().contains(authorizationOxdId)) {
            LOG.debug("`oxd_id` in `AuthorizationOxdId` header is invalid. The `AuthorizationOxdId` header should contain `oxd_id` from `protect_commands_with_oxd_id` field in oxd-server.yml.");
            throw new HttpException(ErrorResponseCode.INVALID_AUTHORIZATION_OXD_ID);
        }
    }

    private static void validateAccessToken(String authorization, String authorizationOxdId) {
        final String prefix = "Bearer ";

        final OxdServerConfiguration conf = ServerLauncher.getInjector().getInstance(ConfigurationService.class).get();
        if (conf.getProtectCommandsWithAccessToken() != null && !conf.getProtectCommandsWithAccessToken()) {
            LOG.debug("Skip protection because protect_commands_with_access_token: false in configuration file.");
            return;
        }

        if (Strings.isNullOrEmpty(authorization)) {
            LOG.debug("No access token provided in Authorization header. Forbidden.");
            throw new HttpException(ErrorResponseCode.BLANK_ACCESS_TOKEN);
        }

        String accessToken = authorization.substring(prefix.length());
        if (Strings.isNullOrEmpty(accessToken)) {
            LOG.debug("No access token provided in Authorization header. Forbidden.");
            throw new HttpException(ErrorResponseCode.BLANK_ACCESS_TOKEN);
        }

        final ValidationService validationService = ServerLauncher.getInjector().getInstance(ValidationService.class);
        validationService.validateAccessToken(accessToken, authorizationOxdId);
    }

    private static String safeToOxdId(HasOxdIdParams params, String authorizationOxdId) {
        return Strings.isNullOrEmpty(authorizationOxdId) ? params.getOxdId() : authorizationOxdId;
    }
}
