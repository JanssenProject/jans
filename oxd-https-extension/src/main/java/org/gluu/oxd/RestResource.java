package org.gluu.oxd;

import org.codehaus.jackson.node.POJONode;
import org.codehaus.jettison.json.JSONException;
import org.hibernate.annotations.common.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ErrorResponse;
import org.xdi.oxd.common.params.*;
import org.xdi.oxd.common.response.IOpResponse;
import org.xdi.oxd.rs.protect.Jackson;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/")
public class RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(Oxd.class);

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
        return okResponse(Oxd.setupClient(read(params, SetupClientParams.class)));
    }

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getClientToken(String params) throws IOException {
        return okResponse(Oxd.getClientToken(read(params, GetClientTokenParams.class)));
    }

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String registerSite(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        String accessToken= validateAccessToken(p_authorization);
        if(accessToken != null) {
            return okResponse(Oxd.registerSite(read(params, RegisterSiteParams.class),accessToken));
        }
        else {
            return errorResponse();
        }
    }

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String updateSite(@HeaderParam("Authorization") String p_authorization ,String params) throws IOException , JSONException{
        String accessToken= validateAccessToken(p_authorization);
        if(accessToken != null) {
            return okResponse(Oxd.updateSite(read(params, UpdateSiteParams.class),accessToken));
        }
        else {
            return errorResponse();
        }
    }

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationUrl(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        String accessToken= validateAccessToken(p_authorization);
        if(accessToken!= null) {
            return okResponse(Oxd.getAuthorizationUrl(read(params, GetAuthorizationUrlParams.class),accessToken));
        }
        else {
            return errorResponse();
        }
    }

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getTokenByCode(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        String accessToken= validateAccessToken(p_authorization);
        if(accessToken != null) {
            return okResponse(Oxd.getTokenByCode(read(params, GetTokensByCodeParams.class),accessToken));
        }
        else {
            return errorResponse();
        }
    }

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getUserInfo(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        String accessToken= validateAccessToken(p_authorization);
        if(accessToken != null) {
            return okResponse(Oxd.getUserInfo(read(params, GetUserInfoParams.class),accessToken));
        }
        else {
            return errorResponse();
        }
    }

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getLogoutUri(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        String accessToken= validateAccessToken(p_authorization);
        if(accessToken != null) {
            return okResponse(Oxd.getLogoutUri(read(params, GetLogoutUrlParams.class),accessToken));
        }
        else {
            return errorResponse();
        }
    }

    @POST
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAccessTokenByRefreshToken(String params) throws IOException, JSONException {
        return okResponse(Oxd.getAccessTokenByRefreshToken(read(params, GetAccessTokenByRefreshTokenParams.class)));
    }


    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsProtect(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        String accessToken= validateAccessToken(p_authorization);
        if(accessToken != null) {
            return okResponse(Oxd.umaRsProtect(read(params, RsProtectParams.class),accessToken));
        }
        else {
            return errorResponse();
        }
    }

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsCheckAccess(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        String accessToken= validateAccessToken(p_authorization);
        if(accessToken != null) {
            return okResponse(Oxd.umaRsCheckAccess(read(params, RsCheckAccessParams.class),accessToken));
        }
        else {
            return errorResponse();
        }
    }

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetRpt(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        String accessToken= validateAccessToken(p_authorization);
        if(accessToken != null) {
            return okResponse(Oxd.umaRpGetRpt(read(params, RpGetRptParams.class),accessToken));
        }
        else {
            return errorResponse();
        }
    }

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String p_authorization, String params) throws IOException, JSONException {
        String accessToken= validateAccessToken(p_authorization);
        if(accessToken != null) {
            return okResponse(Oxd.umaRpGetClaimsGatheringUrl(read(params, RpGetClaimsGatheringUrlParams.class),accessToken));
        }
        else {
            return errorResponse();
        }
    }

    //region Common Methods
    public static <T> T read(String params, Class<T> clazz) throws IOException {
        return Jackson.createJsonMapper().readValue(params, clazz);
    }

    public static String okResponse(IOpResponse p_data) throws IOException {
        String nullResponse = "There is no response produced by Processor. Please check oxd server logs";
        if (p_data == null) {
            LOG.error(nullResponse);
            return nullResponse;
        }

        CommandResponse commandResponse = CommandResponse.ok().setData(new POJONode(p_data));
        if (commandResponse != null) {
            final String json = CoreUtils.asJson(commandResponse);
            LOG.trace("Send back response: {}", json);
            return json;
        } else {
            LOG.error("There is no response produced by Processor.Please check oxd server logs");
            return nullResponse;
        }
    }

    public String validateAccessToken(String authorizationParameter) {
        final String prefix = "Bearer ";
        if (StringHelper.isNotEmpty(authorizationParameter) && authorizationParameter.startsWith(prefix)) {
            return authorizationParameter.substring(prefix.length());
        }
        return null;
    }

    public static String errorResponse() throws IOException, JSONException {
        final ErrorResponse error = new ErrorResponse("403");
        error.setErrorDescription("Forbidden Access");

        CommandResponse commandResponse = CommandResponse.error().setData(new POJONode(error));
        if (commandResponse != null) {
            final String json = CoreUtils.asJson(commandResponse);
            return json;
        }
        else{
            return null;
        }
    }
    //endregion
}
