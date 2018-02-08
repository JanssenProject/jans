package org.gluu.oxd;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.POJONode;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ErrorResponse;
import org.xdi.oxd.common.params.*;
import org.xdi.oxd.rs.protect.Jackson;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/")
public class RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    private final Oxd oxd;

    public RestResource(OxdHttpsConfiguration configuration) {
        this.oxd = new Oxd(configuration);
    }

    @GET
    @Path("/health-check")
    @Produces(MediaType.APPLICATION_JSON)
    public String healthCheck() {
        return "running";
    }

    @POST
    @Path("/setup-client")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String setupClient(String params) throws IOException, JSONException {
        return response(oxd.setupClient(read(params, SetupClientParams.class)));
    }

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getClientToken(String params) throws IOException {
        return response(oxd.getClientToken(read(params, GetClientTokenParams.class)));
    }

    @POST
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectAccessToken(String params) throws IOException {
        return response(oxd.introspectAccessToken(read(params, IntrospectAccessTokenParams.class)));
    }

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectRpt(String params) throws IOException {
        return response(oxd.introspectRpt(read(params, IntrospectRptParams.class)));
    }

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String registerSite(@HeaderParam("Authorization") String authorization, String params) throws IOException, JSONException {
        return response(oxd.registerSite(read(params, RegisterSiteParams.class), validateAccessToken(authorization)));
    }

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String updateSite(@HeaderParam("Authorization") String authorization, String params) throws IOException, JSONException {
        return response(oxd.updateSite(read(params, UpdateSiteParams.class), validateAccessToken(authorization)));
    }

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String removeSite(@HeaderParam("Authorization") String authorization, String params) throws IOException, JSONException {
        return response(oxd.removeSite(read(params, RemoveSiteParams.class), validateAccessToken(authorization)));
    }

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationUrl(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        return response(oxd.getAuthorizationUrl(read(params, GetAuthorizationUrlParams.class), validateAccessToken(p_authorization)));
    }

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getTokenByCode(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        return response(oxd.getTokenByCode(read(params, GetTokensByCodeParams.class), validateAccessToken(p_authorization)));
    }

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getUserInfo(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        return response(oxd.getUserInfo(read(params, GetUserInfoParams.class), validateAccessToken(p_authorization)));
    }

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getLogoutUri(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        return response(oxd.getLogoutUri(read(params, GetLogoutUrlParams.class), validateAccessToken(p_authorization)));
    }

    @POST
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAccessTokenByRefreshToken(String params) throws IOException, JSONException {
        return response(oxd.getAccessTokenByRefreshToken(read(params, GetAccessTokenByRefreshTokenParams.class)));
    }

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsProtect(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        return response(oxd.umaRsProtect(read(params, RsProtectParams.class), validateAccessToken(p_authorization)));
    }

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsCheckAccess(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        return response(oxd.umaRsCheckAccess(read(params, RsCheckAccessParams.class), validateAccessToken(p_authorization)));
    }

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetRpt(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        return response(oxd.umaRpGetRpt(read(params, RpGetRptParams.class), validateAccessToken(p_authorization)));
    }

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String authorization, String params) throws IOException, JSONException {
        return response(oxd.umaRpGetClaimsGatheringUrl(read(params, RpGetClaimsGatheringUrlParams.class), validateAccessToken(authorization)));
    }

    public static <T> T read(String params, Class<T> clazz) throws IOException {
        return Jackson.createJsonMapper().readValue(params, clazz);
    }

    public static String response(CommandResponse commandResponse) throws IOException {
        if (commandResponse == null) {
            LOG.error("Command response is null, please check oxd-server.log file of oxd-server application.");
            return "Command response is null, please check oxd-server.log file of oxd-server application.";
        }
        final String json = CoreUtils.asJson(commandResponse);
        LOG.trace("Send back response: {}", json);
        return json;
    }

    public static String validateAccessToken(String authorizationParameter) {
        final String prefix = "Bearer ";
        if (StringUtils.isNotEmpty(authorizationParameter) && authorizationParameter.startsWith(prefix)) {
            String accessToken = authorizationParameter.substring(prefix.length());
            if (StringUtils.isNotBlank(accessToken)) {
                return accessToken;
            }
        }
        LOG.debug("No access token provided in Authorization header. Forbidden.");
        throw new ServerErrorException(forbiddenErrorResponse(), Response.Status.FORBIDDEN);
    }

    public static String forbiddenErrorResponse() {
        final ErrorResponse error = new ErrorResponse("403");
        error.setErrorDescription("Forbidden Access");

        CommandResponse commandResponse = CommandResponse.error().setData(new POJONode(error));
        return CoreUtils.asJsonSilently(commandResponse);
    }
}
