package io.jans.ca.server.arquillian;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.as.model.uma.UmaConstants;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.client.GetTokensByCodeResponse2;
import io.jans.ca.client.RsProtectParams2;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.common.params.*;
import io.jans.ca.common.response.*;
import io.jans.ca.server.tests.PathTestEndPoint;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class ClientIterfaceImpl implements ClientInterface {

    protected String targeHostUrl = "";

    public static ClientIterfaceImpl getInstanceClient(String targeHostUrl) {
        ClientIterfaceImpl result = new ClientIterfaceImpl();
        result.targeHostUrl = targeHostUrl;
        return result;
    }

    private WebTarget webTarget(String pathEndPoint) {
        return ResteasyClientBuilder.newClient().target(targeHostUrl + pathEndPoint);
    }

    private Invocation.Builder requestBuilder(String pathEndPoint) {
        return webTarget(pathEndPoint).request();
    }

    private Entity<?> toPostParam(Object param) {
        String json = null;
        try {
            json = Jackson2.asJson(param);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return Entity.json(json);
    }

    private String readResponse(String endPoint, Response response) {
        String entity = response.readEntity(String.class);
        showResponse(endPoint, response, entity);
        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        return entity;
    }

    private String readResponseNoVerifyOk(String endPoint, Response response) {
        String entity = response.readEntity(String.class);
        showResponse(endPoint, response, entity);
        return entity;
    }

    @Override
    public String healthCheck() {
        Invocation.Builder builder = requestBuilder(PathTestEndPoint.HEALT_CHECK);
        Response response = builder.get();
        String entity = response.readEntity(String.class);

        showResponse("healthCheck", response, entity);
        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        return entity;
    }

    @Override
    public JsonNode getRpJwks() {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_RP_JWKS);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        Response response = builder.get();
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, JsonNode.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public String getRequestObject(String value) {
        Invocation.Builder builder = requestBuilder(PathTestEndPoint.GET_REQUEST_OBJECT + value);
        Response response = builder.get();
        String entity = response.readEntity(String.class);

        showResponse("getRequestObject", response, entity);
        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        return entity;
    }

    @Override
    public GetClientTokenResponse getClientToken(GetClientTokenParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_CLIENT_TOKEN);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, GetClientTokenResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public IntrospectAccessTokenResponse introspectAccessToken(String authorization, String authorizationRpId, IntrospectAccessTokenParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.INSTROSPECT_ACCESS_TOKEN);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, IntrospectAccessTokenResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public CorrectRptIntrospectionResponse introspectRpt(String authorization, String authorizationRpId, IntrospectRptParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.INSTROSPECT_RPT);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, CorrectRptIntrospectionResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public RegisterSiteResponse registerSite(RegisterSiteParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.REGISTER_SITE);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, RegisterSiteResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public UpdateSiteResponse updateSite(String authorization, String authorizationRpId, UpdateSiteParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.UPDATE_SITE);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, UpdateSiteResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public RemoveSiteResponse removeSite(String authorization, String authorizationRpId, RemoveSiteParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.REMOVE_SITE);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, RemoveSiteResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public GetAuthorizationUrlResponse getAuthorizationUrl(String authorization, String authorizationRpId, GetAuthorizationUrlParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_AUTHORIZATION_URL);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, GetAuthorizationUrlResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public GetAuthorizationCodeResponse getAuthorizationCode(String authorization, String authorizationRpId, GetAuthorizationCodeParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_AUTHORIZATION_CODE);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, GetAuthorizationCodeResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public GetTokensByCodeResponse2 getTokenByCode(String authorization, String authorizationRpId, GetTokensByCodeParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_TOKENS_BY_CODE);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponseNoVerifyOk(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, GetTokensByCodeResponse2.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public JsonNode getUserInfo(String authorization, String authorizationRpId, GetUserInfoParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_USER_INFO);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, JsonNode.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public GetLogoutUriResponse getLogoutUri(String authorization, String authorizationRpId, GetLogoutUrlParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_LOGOUT_URI);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, GetLogoutUriResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public GetClientTokenResponse getAccessTokenByRefreshToken(String authorization, String authorizationRpId, GetAccessTokenByRefreshTokenParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_ACCESS_TOKEN_BY_REFRESH);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, GetClientTokenResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public RsProtectResponse umaRsProtect(String authorization, String authorizationRpId, RsProtectParams2 params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.UMA_RS_PROTECT);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponseNoVerifyOk(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, RsProtectResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public RsModifyResponse umaRsModify(String authorization, String authorizationRpId, RsModifyParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.UMA_RS_MODIFY);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, RsModifyResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public RsCheckAccessResponse umaRsCheckAccess(String authorization, String authorizationRpId, RsCheckAccessParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.UMA_RS_CHECK_ACCESS);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponseNoVerifyOk(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, RsCheckAccessResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public RpGetRptResponse umaRpGetRpt(String authorization, String authorizationRpId, RpGetRptParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.UMA_RP_GET_RPT);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponseNoVerifyOk(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, RpGetRptResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public RpGetClaimsGatheringUrlResponse umaRpGetClaimsGatheringUrl(String authorization, String authorizationRpId, RpGetClaimsGatheringUrlParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.UMA_RP_GET_CLAIMS_GATHERING_URL);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, RpGetClaimsGatheringUrlResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public AuthorizationCodeFlowResponse authorizationCodeFlow(String authorization, String authorizationRpId, AuthorizationCodeFlowParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.AUTHORIZATION_CODE_FLOW);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, AuthorizationCodeFlowResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public CheckAccessTokenResponse checkAccessToken(String authorization, String authorizationRpId, CheckAccessTokenParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.CHECK_ACCESS_TOKEN);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, CheckAccessTokenResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public CheckIdTokenResponse checkIdToken(String authorization, String authorizationRpId, CheckIdTokenParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.CHECK_ID_TOKEN);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, CheckIdTokenResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public String getRp(String authorization, String authorizationRpId, GetRpParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_RP);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        return json;
    }

    @Override
    public GetJwksResponse getJwks(String authorization, String authorizationRpId, GetJwksParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_JSON_WEB_KEY_SET);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, GetJwksResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public GetDiscoveryResponse getDiscovery(GetDiscoveryParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_DISCOVERY);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, GetDiscoveryResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public GetIssuerResponse getIssuer(GetIssuerParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_ISSUER);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, GetIssuerResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public GetRequestObjectUriResponse getRequestObjectUri(String authorization, String authorizationRpId, GetRequestObjectUriParams params) {
        WebTarget webTarget = webTarget(PathTestEndPoint.GET_REQUEST_OBJECT_URI);
        Invocation.Builder builder = webTarget.request();
        builder.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Content-Type", UmaConstants.JSON_MEDIA_TYPE);
        builder.header("Authorization", authorization);
        builder.header("AuthorizationRpId", authorizationRpId);
        Response response = builder.post(toPostParam(params));
        String json = readResponse(webTarget.getUri().toString(), response);
        try {
            return Jackson2.createJsonMapper().readValue(json, GetRequestObjectUriResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @Override
    public String getApitargetURL() {
        return targeHostUrl;
    }

    public static void showResponse(String title, Response response, Object entity) {
        System.out.println(" ");
        System.out.println("RESPONSE FOR: " + title);
        System.out.println(response.getStatus());
        for (Map.Entry<String, List<Object>> headers : response.getHeaders().entrySet()) {
            String headerName = headers.getKey();
            System.out.println(headerName + ": " + headers.getValue());
        }

        if (entity != null) {
            System.out.println(entity.toString().replace("\\n", "\n"));
        }
        System.out.println(" ");
        System.out.println("Status message: " + response.getStatus());
    }

}
