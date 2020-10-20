package io.jans.ca.server;

import io.dropwizard.util.Strings;
import io.jans.ca.common.params.*;
import io.opentracing.Scope;
import io.jans.ca.common.Command;
import io.jans.ca.common.CommandType;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.service.ConfigurationService;
import io.jans.ca.server.service.Rp;
import io.jans.ca.server.service.RpSyncService;
import io.jans.ca.server.service.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

@Path("/")
public class RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    @Context
    private HttpServletRequest httpRequest;
    private static final String LOCALHOST_IP_ADDRESS = "127.0.0.1";

    public RestResource() {
    }

    @GET
    @Path("/health-check")
    @Produces(MediaType.APPLICATION_JSON)
    public String healthCheck() {
        validateIpAddressAllowed(httpRequest.getRemoteAddr());
        return "{\"status\":\"running\"}";
    }

    @GET
    @Path("/get-request-object/{request_object_id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getRequestObject(@PathParam("request_object_id") String value) {
        return process(CommandType.GET_REQUEST_OBJECT_JWT, (new StringParam(value)).toJsonString(), StringParam.class, null, null, httpRequest);
    }

    @GET
    @Path("/get-rp-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    public String getRpJwks() {
        return process(CommandType.GET_RP_JWKS, null, GetJwksParams.class, null, null, httpRequest);
    }

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getClientToken(String params) {
        return process(CommandType.GET_CLIENT_TOKEN, params, GetClientTokenParams.class, null, null, httpRequest);
    }

    @POST
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectAccessToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.INTROSPECT_ACCESS_TOKEN, params, IntrospectAccessTokenParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectRpt(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.INTROSPECT_RPT, params, IntrospectRptParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String registerSite(String params) {
        return process(CommandType.REGISTER_SITE, params, RegisterSiteParams.class, null, null, httpRequest);
    }

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String updateSite(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.UPDATE_SITE, params, UpdateSiteParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String removeSite(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.REMOVE_SITE, params, RemoveSiteParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationUrl(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_AUTHORIZATION_URL, params, GetAuthorizationUrlParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/get-authorization-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationCode(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_AUTHORIZATION_CODE, params, GetAuthorizationCodeParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getTokenByCode(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_TOKENS_BY_CODE, params, GetTokensByCodeParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getUserInfo(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_USER_INFO, params, GetUserInfoParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getLogoutUri(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_LOGOUT_URI, params, GetLogoutUrlParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAccessTokenByRefreshToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN, params, GetAccessTokenByRefreshTokenParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsProtect(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.RS_PROTECT, params, RsProtectParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/uma-rs-modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsModify(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.RS_MODIFY, params, RsModifyParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsCheckAccess(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.RS_CHECK_ACCESS, params, RsCheckAccessParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetRpt(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.RP_GET_RPT, params, RpGetRptParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.RP_GET_CLAIMS_GATHERING_URL, params, RpGetClaimsGatheringUrlParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/authorization-code-flow")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String authorizationCodeFlow(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.AUTHORIZATION_CODE_FLOW, params, AuthorizationCodeFlowParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/check-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String checkAccessToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.CHECK_ACCESS_TOKEN, params, CheckAccessTokenParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/check-id-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String checkIdToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.CHECK_ID_TOKEN, params, CheckIdTokenParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/get-rp")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getRp(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_RP, params, GetRpParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/get-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getJwks(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_JWKS, params, GetJwksParams.class, authorization, authorizationOxdId, httpRequest);
    }

    @POST
    @Path("/get-discovery")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getDiscovery(String params) {
        return process(CommandType.GET_DISCOVERY, params, GetDiscoveryParams.class, null, null, httpRequest);
    }

    @POST
    @Path("/get-issuer")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getIssuer(String params) {
        return process(CommandType.ISSUER_DISCOVERY, params, GetIssuerParams.class, null, null, httpRequest);
    }

    @POST
    @Path("/get-request-object-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getRequestObjectUri(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, String params) {
        return process(CommandType.GET_REQUEST_URI, params, GetRequestObjectUriParams.class, authorization, authorizationOxdId, httpRequest);
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

    private static <T extends IParams> String process(CommandType commandType, String paramsAsString, Class<T> paramsClass, String authorization, String authorizationOxdId, HttpServletRequest httpRequest) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(commandType.toString(), true)) {
            TracingUtil.setTag("end-point", httpRequest.getRequestURL().toString());
            TracingUtil.log("Request parameters: " + paramsAsString);
            TracingUtil.log("CommandType: " + commandType);

            validateIpAddressAllowed(httpRequest.getRemoteAddr());
            Object forJsonConversion = getObjectForJsonConversion(commandType, paramsAsString, paramsClass, authorization, authorizationOxdId);
            String response = null;

            if (commandType.getReturnType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
                response = Jackson2.asJsonSilently(forJsonConversion);
            } else if (commandType.getReturnType().equalsIgnoreCase(MediaType.TEXT_PLAIN)) {
                response = forJsonConversion.toString();
            }

            TracingUtil.log("Send back response: " + response);
            LOG.trace("Send back response: {}", response);
            return response;
        }
    }

    private static void validateIpAddressAllowed(String callerIpAddress) {
        LOG.trace("Checking if caller ipAddress : {} is allowed to make request to oxd.", callerIpAddress);
        final RpServerConfiguration conf = ServerLauncher.getInjector().getInstance(ConfigurationService.class).get();
        List<String> bindIpAddresses = conf.getBindIpAddresses();

        //localhost as default bindAddress
        if ((bindIpAddresses == null || bindIpAddresses.isEmpty()) && LOCALHOST_IP_ADDRESS.equalsIgnoreCase(callerIpAddress)) {
            return;
        }
        //show error if ip_address of a remote caller is not set in `bind_ip_addresses`
        if (bindIpAddresses == null || bindIpAddresses.isEmpty()) {
            LOG.error("The caller is not allowed to make request to oxd. To allow add ip_address of caller in `bind_ip_addresses` array of `jans-client-api.yml`.");
            throw new HttpException(ErrorResponseCode.RP_ACCESS_DENIED);
        }
        //allow all ip_address
        if (bindIpAddresses.contains("*")) {
            return;
        }

        if (bindIpAddresses.contains(callerIpAddress)) {
            return;
        }
        LOG.error("The caller is not allowed to make request to oxd. To allow add ip_address of caller in `bind_ip_addresses` array of `jans-client-api.yml`.");
        throw new HttpException(ErrorResponseCode.RP_ACCESS_DENIED);

    }

    private static <T extends IParams> Object getObjectForJsonConversion(CommandType commandType, String paramsAsString, Class<T> paramsClass, String authorization, String authorizationOxdId) {
        LOG.trace("Command: {}", paramsAsString);
        T params = read(safeToJson(paramsAsString), paramsClass);

        final RpServerConfiguration conf = ServerLauncher.getInjector().getInstance(ConfigurationService.class).get();

        if (commandType.isAuthorizationRequired()) {
            validateAuthorizationOxdId(conf, authorizationOxdId);
            validateAccessToken(authorization, safeToOxdId((HasRpIdParams) params, authorizationOxdId));
        }

        Command command = new Command(commandType, params);
        final IOpResponse response = ServerLauncher.getInjector().getInstance(Processor.class).process(command);
        Object forJsonConversion = response;
        if (response instanceof POJOResponse) {
            forJsonConversion = ((POJOResponse) response).getNode();
        }
        return forJsonConversion;
    }

    private static void validateAuthorizationOxdId(RpServerConfiguration conf, String authorizationOxdId) {

        if (Strings.isNullOrEmpty(authorizationOxdId)) {
            return;
        }

        final RpSyncService rpSyncService = ServerLauncher.getInjector().getInstance(RpSyncService.class);
        final Rp rp = rpSyncService.getRp(authorizationOxdId);

        if (rp == null || Strings.isNullOrEmpty(rp.getRpId())) {
            LOG.debug("`oxd_id` in `AuthorizationOxdId` header is not registered in oxd.");
            throw new HttpException(ErrorResponseCode.AUTHORIZATION_RP_ID_NOT_FOUND);
        }

        if (conf.getProtectCommandsWithRpId() == null || conf.getProtectCommandsWithRpId().isEmpty()) {
            return;
        }

        if (!conf.getProtectCommandsWithRpId().contains(authorizationOxdId)) {
            LOG.debug("`oxd_id` in `AuthorizationOxdId` header is invalid. The `AuthorizationOxdId` header should contain `oxd_id` from `protect_commands_with_oxd_id` field in jans-client-api.yml.");
            throw new HttpException(ErrorResponseCode.INVALID_AUTHORIZATION_RP_ID);
        }
    }

    private static void validateAccessToken(String authorization, String authorizationOxdId) {
        final String prefix = "Bearer ";

        final RpServerConfiguration conf = ServerLauncher.getInjector().getInstance(ConfigurationService.class).get();
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

    private static String safeToOxdId(HasRpIdParams params, String authorizationOxdId) {
        return Strings.isNullOrEmpty(authorizationOxdId) ? params.getRpId() : authorizationOxdId;
    }

    private static String safeToJson(String jsonString) {
        return Strings.isNullOrEmpty(jsonString) ? "{}" : jsonString;
    }
}
