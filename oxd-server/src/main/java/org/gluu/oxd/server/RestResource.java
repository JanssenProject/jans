package org.gluu.oxd.server;

import io.opentracing.Scope;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.CommandType;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.common.params.*;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.POJOResponse;
import org.gluu.oxd.server.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/")
public class RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    public RestResource() {
    }

    @GET
    @Path("/health-check")
    @Produces(MediaType.APPLICATION_JSON)
    public String healthCheck() {
        try (Scope orderSpanScope = TracingUtil.buildSpan("health-check", true)) {
            TracingUtil.setTag("end-point", "/health-check");
            return "{\"status\":\"running\"}";
        }
    }

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getClientToken(String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.GET_CLIENT_TOKEN.toString(), true)) {
            TracingUtil.setTag("end-point", "/get-client-token");
            return process(CommandType.GET_CLIENT_TOKEN, params, GetClientTokenParams.class, null);
        }
    }

    @POST
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectAccessToken(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.INTROSPECT_ACCESS_TOKEN.toString(), true)) {
            TracingUtil.setTag("end-point", "/introspect-access-token");
            return process(CommandType.INTROSPECT_ACCESS_TOKEN, params, IntrospectAccessTokenParams.class, authorization);
        }
    }

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectRpt(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.INTROSPECT_RPT.toString(), true)) {
            TracingUtil.setTag("end-point", "/introspect-rpt");
            return process(CommandType.INTROSPECT_RPT, params, IntrospectRptParams.class, authorization);
        }
    }

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String registerSite(String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.REGISTER_SITE.toString(), true)) {
            TracingUtil.setTag("end-point", "/register-site");
            return process(CommandType.REGISTER_SITE, params, RegisterSiteParams.class, null);
        }
    }

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String updateSite(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.UPDATE_SITE.toString(), true)) {
            TracingUtil.setTag("end-point", "/update-site");
            return process(CommandType.UPDATE_SITE, params, UpdateSiteParams.class, authorization);
        }
    }

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String removeSite(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.REMOVE_SITE.toString(), true)) {
            TracingUtil.setTag("end-point", "/remove-site");
            return process(CommandType.REMOVE_SITE, params, RemoveSiteParams.class, authorization);
        }
    }

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationUrl(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.GET_AUTHORIZATION_URL.toString(), true)) {
            TracingUtil.setTag("end-point", "/get-authorization-url");
            return process(CommandType.GET_AUTHORIZATION_URL, params, GetAuthorizationUrlParams.class, authorization);
        }
    }

    @POST
    @Path("/get-authorization-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationCode(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.GET_AUTHORIZATION_URL.toString(), true)) {
            TracingUtil.setTag("end-point", "/get-authorization-code");
            return process(CommandType.GET_AUTHORIZATION_CODE, params, GetAuthorizationCodeParams.class, authorization);
        }
    }

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getTokenByCode(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.GET_TOKENS_BY_CODE.toString(), true)) {
            TracingUtil.setTag("end-point", "/get-tokens-by-code");
            return process(CommandType.GET_TOKENS_BY_CODE, params, GetTokensByCodeParams.class, authorization);
        }
    }

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getUserInfo(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.GET_USER_INFO.toString(), true)) {
            TracingUtil.setTag("end-point", "/get-user-info");
            return process(CommandType.GET_USER_INFO, params, GetUserInfoParams.class, authorization);
        }
    }

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getLogoutUri(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.GET_LOGOUT_URI.toString(), true)) {
            TracingUtil.setTag("end-point", "/get-logout-uri");
            return process(CommandType.GET_LOGOUT_URI, params, GetLogoutUrlParams.class, authorization);
        }
    }

    @POST
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAccessTokenByRefreshToken(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN.toString(), true)) {
            TracingUtil.setTag("end-point", "/get-access-token-by-refresh-token");
            return process(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN, params, GetAccessTokenByRefreshTokenParams.class, authorization);
        }
    }

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsProtect(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.RS_PROTECT.toString(), true)) {
            TracingUtil.setTag("end-point", "/uma-rs-protect");
            return process(CommandType.RS_PROTECT, params, RsProtectParams.class, authorization);
        }
    }

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsCheckAccess(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.RS_CHECK_ACCESS.toString(), true)) {
            TracingUtil.setTag("end-point", "/uma-rs-check-access");
            return process(CommandType.RS_CHECK_ACCESS, params, RsCheckAccessParams.class, authorization);
        }
    }

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetRpt(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.RP_GET_RPT.toString(), true)) {
            TracingUtil.setTag("end-point", "/uma-rp-get-rpt");
            return process(CommandType.RP_GET_RPT, params, RpGetRptParams.class, authorization);
        }
    }

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.RP_GET_CLAIMS_GATHERING_URL.toString(), true)) {
            TracingUtil.setTag("end-point", "/uma-rp-get-claims-gathering-url");
            return process(CommandType.RP_GET_CLAIMS_GATHERING_URL, params, RpGetClaimsGatheringUrlParams.class, authorization);
        }
    }

    @POST
    @Path("/authorization-code-flow")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String authorizationCodeFlow(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.AUTHORIZATION_CODE_FLOW.toString(), true)) {
            TracingUtil.setTag("end-point", "/authorization-code-flow");
            return process(CommandType.AUTHORIZATION_CODE_FLOW, params, AuthorizationCodeFlowParams.class, authorization);
        }
    }

    @POST
    @Path("/check-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String checkAccessToken(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.CHECK_ACCESS_TOKEN.toString(), true)) {
            TracingUtil.setTag("end-point", "/check-access-token");
            return process(CommandType.CHECK_ACCESS_TOKEN, params, CheckAccessTokenParams.class, authorization);
        }
    }

    @POST
    @Path("/check-id-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String checkIdToken(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.CHECK_ID_TOKEN.toString(), true)) {
            TracingUtil.setTag("end-point", "/check-id-token");
            return process(CommandType.CHECK_ID_TOKEN, params, CheckIdTokenParams.class, authorization);
        }
    }

    @POST
    @Path("/get-rp")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getRp(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.GET_RP.toString(), true)) {
            TracingUtil.setTag("end-point", "/get-rp");
            return process(CommandType.GET_RP, params, GetRpParams.class, authorization);
        }
    }

    @POST
    @Path("/get-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getJwks(@HeaderParam("Authorization") String authorization, String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.GET_JWKS.toString(), true)) {
            TracingUtil.setTag("end-point", "/get-jwks");
            return process(CommandType.GET_JWKS, params, GetJwksParams.class, authorization);
        }
    }

    @POST
    @Path("/get-discovery")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getDiscovery(String params) {
        try (Scope orderSpanScope = TracingUtil.buildSpan(CommandType.GET_DISCOVERY.toString(), true)) {
            TracingUtil.setTag("end-point", "/get-discovery");
            return process(CommandType.GET_DISCOVERY, params, GetDiscoveryParams.class, null);
        }
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

    private static <T extends IParams> String process(CommandType commandType, String paramsAsString, Class<T> paramsClass, String authorization) {
        TracingUtil.log("Request parameters: "+ paramsAsString);
        TracingUtil.log("CommandType: "+ commandType);
        Object forJsonConversion = getObjectForJsonConversion(commandType, paramsAsString, paramsClass, authorization);
        final String json = Jackson2.asJsonSilently(forJsonConversion);
        TracingUtil.log("Send back response: "+ json);
        LOG.trace("Send back response: {}", json);
        return json;
    }


    private static <T extends IParams> Object getObjectForJsonConversion(CommandType commandType, String paramsAsString, Class<T> paramsClass, String authorization) {
        LOG.trace("Command: {}", paramsAsString);
        T params = read(paramsAsString, paramsClass);
        if (params instanceof HasAccessTokenParams && !(params instanceof RegisterSiteParams)) {
            ((HasAccessTokenParams) params).setToken(validateAccessToken(authorization));
        }
        Command command = new Command(commandType, params);
        final IOpResponse response = ServerLauncher.getInjector().getInstance(Processor.class).process(command);
        Object forJsonConversion = response;
        if (response instanceof POJOResponse) {
            forJsonConversion = ((POJOResponse) response).getNode();
        }
        return forJsonConversion;
    }

    private static String validateAccessToken(String authorization) {
        final String prefix = "Bearer ";
        if (StringUtils.isNotEmpty(authorization) && authorization.startsWith(prefix)) {
            String accessToken = authorization.substring(prefix.length());
            if (StringUtils.isNotBlank(accessToken)) {
                return accessToken;
            }
        }
        final OxdServerConfiguration conf = ServerLauncher.getInjector().getInstance(ConfigurationService.class).get();
        if (conf.getProtectCommandsWithAccessToken() != null && !conf.getProtectCommandsWithAccessToken()) {
            LOG.debug("Skip protection because protect_commands_with_access_token: false in configuration file.");
            return "";
        }
        LOG.debug("No access token provided in Authorization header. Forbidden.");
        throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
    }
}
