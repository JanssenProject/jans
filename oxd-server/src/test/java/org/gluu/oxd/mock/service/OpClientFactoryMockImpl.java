package org.gluu.oxd.mock.service;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.gluu.oxauth.client.*;
import org.gluu.oxauth.client.uma.UmaClientFactory;
import org.gluu.oxauth.client.uma.UmaMetadataService;
import org.gluu.oxauth.model.common.TokenType;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jws.RSASigner;
import org.gluu.oxauth.model.uma.PermissionTicket;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.common.introspection.CorrectRptIntrospectionResponse;
import org.gluu.oxd.rs.protect.Condition;
import org.gluu.oxd.rs.protect.RsResource;
import org.gluu.oxd.rs.protect.RsResourceList;
import org.gluu.oxd.rs.protect.resteasy.*;
import org.gluu.oxd.server.introspection.ClientFactory;
import org.gluu.oxd.server.introspection.CorrectRptIntrospectionService;
import org.gluu.oxd.server.op.OpClientFactory;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;

import javax.ws.rs.core.Response;
import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpClientFactoryMockImpl implements OpClientFactory {

    @Override
    public TokenClient createTokenClient(String url) {
        TokenClient client = mock(TokenClient.class);

        TokenResponse response = new TokenResponse();
        response.setAccessToken("DUMMY_ACCESS_TOKEN_"+ System.currentTimeMillis());
        response.setTokenType(TokenType.BEARER);
        response.setExpiresIn(50000);
        response.setRefreshToken(null);
        response.setScope("openid");
        response.setIdToken("eyJraWQiOiJjZmFiMzRlYy0xNjhkLTQ4OTUtODRiOC0xZjAyNzgwNDkxYzciLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiMnI1clZ2STdpMWxfcnNXZUV4bGRuUSIsImF1ZCI6IjZiNTc4YTliLTc1MTMtNDc3YS05YTdmLTEzNDNiNDg3Y2FmOCIsInN1YiI6InMtX1ppclZ0N05PRGRuV0RBVUdyalQycVVad0s2Y1hUaGI5cVY5OXYtdGciLCJhdXRoX3RpbWUiOjE1NjgxODUzMjcsImlzcyI6Imh0dHBzOi8vZHVtbXktaXNzdWVyLm9yZyIsImV4cCI6MTk2ODE4ODkzMCwiaWF0IjoxNTY4MTg1MzMwLCJub25jZSI6IjdyNDZ1dDZlbXU5Z2kxMWduODA0NHVtNjQwIiwib3hPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIn0.Pp_rWPjTs0JWpomQIfHRrzE47cJcOQMO6otYyocgWgOUbzE0ttoS8dYvthU1LtkDdA8sBSX5rhB1CGugeSqvKdij6vLeJmE-A4G0OwfwrE7ROHLsbPpuGULJuIEwXgAZXdtoBwsNmK01Nu6ATEMgREl8dYPCRQ9divjQGLKAGLA");
        response.setStatus(200);

        when(client.exec()).thenReturn(response);
        when(client.execClientCredentialsGrant(any(), any(), any())).thenReturn(response);

        return client;
    }

    @Override
    public TokenClient createTokenClientWithUmaProtectionScope(String url) {
        TokenClient client = mock(TokenClient.class);

        TokenResponse response = new TokenResponse();
        response.setAccessToken("DUMMY_ACCESS_TOKEN_"+ System.currentTimeMillis());
        response.setTokenType(TokenType.BEARER);
        response.setExpiresIn(50000);
        response.setRefreshToken(null);
        response.setScope("uma_protection");
        response.setIdToken("eyJraWQiOiJjZmFiMzRlYy0xNjhkLTQ4OTUtODRiOC0xZjAyNzgwNDkxYzciLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiaVFPZDJ2aEtWVWFzRVRCRDZEbjV0ZyIsImF1ZCI6IjIwNDE5ZDRkLTRhMGItNGIyOC05MjgwLTkzNmNlZDBkNjVmZSIsInN1YiI6InMtX1ppclZ0N05PRGRuV0RBVUdyalQycVVad0s2Y1hUaGI5cVY5OXYtdGciLCJhdXRoX3RpbWUiOjE1Njc1MDY4MTUsImlzcyI6Imh0dHBzOi8vY2UtZGV2Ni5nbHV1Lm9yZyIsImV4cCI6MTU2NzUxMDQxOCwiaWF0IjoxNTY3NTA2ODE4LCJub25jZSI6IjVqOTlyMW9tb2Q1azQ3MTFmMnB1ZDMzZTBhIiwib3hPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIn0.Wt_pUXW1BJyjcq2WJUMIYZwzEUeAmrDe8SaWM-RC7T86TmnQOnz0JMgEEN1J9ONsJNMdf8WJZDaqWXu2tVqHh1IrWmZ-U8_36HxcgPXy65yLho0hzCdjPp_KVTdttQhOmLvqn9x_NO8p06wBjm3d5T6xOgtxOjR0c4lqMOBDh3_jb9UH5ZLHRosx9pFCluylPjok8BREmOI_YnUJKHWz2Js9juWBnE94s50EOb7JuyVHvIDvVkrfh0YRZw61idaRQYzfEzwQQYJz6MF2xd4eHT3f-iB5ZBYdrtOPk0691ogLL3HbO_pCfjvsf4QVD0Q-4rlcSJ004ORyR77cgrBSAA");
        //response.setIdToken("eyJraWQiOiJjZmFiMzRlYy0xNjhkLTQ4OTUtODRiOC0xZjAyNzgwNDkxYzciLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdF9oYXNoIjoiaVFPZDJ2aEtWVWFzRVRCRDZEbjV0ZyIsImF1ZCI6IjIwNDE5ZDRkLTRhMGItNGIyOC05MjgwLTkzNmNlZDBkNjVmZSIsInN1YiI6InMtX1ppclZ0N05PRGRuV0RBVUdyalQycVVad0s2Y1hUaGI5cVY5OXYtdGciLCJhdXRoX3RpbWUiOjE1Njc1MDY4MTUsImlzcyI6Imh0dHBzOi8vY2UtZGV2Ni5nbHV1Lm9yZyIsImV4cCI6MTk2NzUxMDQxOCwiaWF0IjoxNTY3NTA2ODE4LCJub25jZSI6IjVqOTlyMW9tb2Q1azQ3MTFmMnB1ZDMzZTBhIiwib3hPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIn0.rxyKrMvfdScDkzyGQG9Mhc-qxjzOimqrsBTpKfTRpUM");
        response.setStatus(200);

        when(client.execClientCredentialsGrant(any(), any(), any())).thenReturn(response);

        return client;
    }

    @Override
    public JwkClient createJwkClient(String url) {
        JwkClient client = mock(JwkClient.class);

        JwkResponse jwkResponse = mock(JwkResponse.class);
        when(jwkResponse.getStatus()).thenReturn(200);

        RSAPublicKey rsaPublicKey = mock(RSAPublicKey.class);
        when(jwkResponse.getPublicKey(any())).thenReturn(rsaPublicKey);

        when(client.exec()).thenReturn(jwkResponse);

        return client;
    }

    @Override
    public RSASigner createRSASigner(SignatureAlgorithm signatureAlgorithm, RSAPublicKey rsaPublicKey) {
        RSASigner client = mock(RSASigner.class);
        when(client.validate(any())).thenReturn(true);
        when(client.validateAccessToken(any(), any())).thenReturn(true);

        return client;
    }

    @Override
    public UserInfoClient createUserInfoClient(String url) {
        UserInfoClient client = mock(UserInfoClient.class);

        UserInfoResponse response = new UserInfoResponse(0);
        response.setEntity("{ \"name\":\"John\", \"age\":30, \"sub\":\"present\" }");

        when(client.exec()).thenReturn(response);
        return client;
    }

    @Override
    public RegisterClient createRegisterClient(String url) {
        RegisterClient client = mock(RegisterClient.class);

        RegisterResponse response = new RegisterResponse();

        response.setClientId("6b578a9b-7513-477a-9a7f-1343b487caf8");
        response.setClientSecret("DUMMY_CLIENT_SECRET_"+ System.currentTimeMillis());
        response.setRegistrationAccessToken("DUMMY_REGISTRATION_ACCESS_TOKEN");
        response.setRegistrationClientUri("https://www.dummy-op-server.xyz/oxauth/restv1/register?client_id=@!8DBF.24EB.FA0E.1BFF!0001!32B7.932A!0008!AB90.6BF3.8E32.7A13");

        Calendar calendar = Calendar.getInstance();
        // get a date to represent "today"
        Date today = calendar.getTime();
        // add one day to the date/calendar
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date tomorrow = calendar.getTime();

        response.setClientIdIssuedAt(today);
        response.setClientSecretExpiresAt(tomorrow);
        when(client.exec()).thenReturn(response);
        return client;
    }

    @Override
    public OpenIdConfigurationClient createOpenIdConfigurationClient(String url) throws Exception {

        OpenIdConfigurationClient client = mock(OpenIdConfigurationClient.class);

        OpenIdConfigurationResponse response = new OpenIdConfigurationResponse(200);
        response.setEntity("DUMMY_ENTITY");
        response.setRegistrationEndpoint("DUMMY_REGISTRATION_ENDPOINT");
        response.setEndSessionEndpoint("DUMMY_ENDSESSION_ENDPOINT");
        response.setIssuer("https://dummy-issuer.org");
        when(client.execOpenIdConfiguration()).thenReturn(response);
        return client;
    }

    @Override
    public AuthorizeClient createAuthorizeClient(String url) {

        AuthorizeClient client = mock(AuthorizeClient.class);

        AuthorizationResponse response = new AuthorizationResponse("");
        response.setCode("DUMMY_CODE_"+ System.currentTimeMillis());
        response.setScope("DUMMY_SCOPE_"+ System.currentTimeMillis());

        when(client.exec()).thenReturn(response);

        return client;
    }

    public ResourceRegistrar createResourceRegistrar(PatProvider patProvider, ServiceProvider serviceProvider) {
        ResourceRegistrar client = mock(ResourceRegistrar.class);

        Map<Key, RsResource> resourceMapCopy = getResourceMap("{\"resources\":[{\"path\":\"/ws/phone\",\"conditions\":[{\"httpMethods\":[\"GET\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/view\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/view\"]},{\"httpMethods\":[\"PUT\", \"POST\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/add\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/add\"]},{\"httpMethods\":[\"DELETE\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/remove\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/remove\"]}]}]}");
        Map<Key, String> resourceIdMap = getIdMap("{\"resources\":[{\"path\":\"/ws/phone\",\"conditions\":[{\"httpMethods\":[\"GET\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/view\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/view\"]},{\"httpMethods\":[\"PUT\", \"POST\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/add\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/add\"]},{\"httpMethods\":[\"DELETE\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/remove\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/remove\"]}]}]}");

        when(client.getResourceMapCopy()).thenReturn(resourceMapCopy);
        when(client.getIdMapCopy()).thenReturn(resourceIdMap);

        return client;
    }

    public RptPreProcessInterceptor createRptPreProcessInterceptor(ResourceRegistrar resourceRegistrar) {
        RptPreProcessInterceptor client = mock(RptPreProcessInterceptor.class);

        OutboundMessageContext outboundMessageContext = new OutboundMessageContext();
        PermissionTicket permissionTicket = new PermissionTicket("d457e3de-30dd-400a-8698-2b98472b7a40");
        outboundMessageContext.setEntity(permissionTicket);

        OutboundJaxrsResponse response = new OutboundJaxrsResponse(Response.Status.FORBIDDEN, outboundMessageContext);

        when(client.registerTicketResponse(any(List.class), any())).thenReturn(response);
        return client;
    }

    public ClientFactory createClientFactory() {
        ClientFactory clientFactory = mock(ClientFactory.class);

        CorrectRptIntrospectionService correctRptIntrospectionService = mock(CorrectRptIntrospectionService.class);

        when(clientFactory.createCorrectRptStatusService(any(), any())).thenReturn(correctRptIntrospectionService);

        CorrectRptIntrospectionResponse correctRptIntrospectionResponse = new CorrectRptIntrospectionResponse();
        correctRptIntrospectionResponse.setActive(true);
        correctRptIntrospectionResponse.setClientId("d457e3de-30dd-400a-8698-2b98472b7a40");
        correctRptIntrospectionResponse.setIssuedAt((int)(System.currentTimeMillis()/1000));
        correctRptIntrospectionResponse.setExpiresAt((int)(System.currentTimeMillis()/1000));
        when(correctRptIntrospectionService.requestRptStatus(any(), any(), any())).thenReturn(correctRptIntrospectionResponse);

        return clientFactory;
    }

    public UmaClientFactory createUmaClientFactory() {
        UmaClientFactory umaClientFactory = mock(UmaClientFactory.class);
        UmaMetadataService umaMetadataService = mock(UmaMetadataService.class);

        UmaMetadata umaMetadata = new UmaMetadata();

        when(umaClientFactory.createMetadataService(any(), any())).thenReturn(umaMetadataService);
        when(umaMetadataService.getMetadata()).thenReturn(umaMetadata);

        return umaClientFactory;
    }

    public ClientRequest createClientRequest(String uriTemplate, ClientExecutor executor) throws Exception {
        ClientRequest client = mock(ClientRequest.class);

        ClientResponse<String> response = mock(ClientResponse.class);

        when(response.getEntity()).thenReturn("{ \"access_token\":\"d457e3de-30dd-400a-8698-2b98472b7a40\"," +
                "\"token_type\":\"Bearer\"," +
                "\"pct\":\"30dd\"" +
                "}");
        when(client.header(any(), any())).thenReturn(client);
        when(client.formParameter(any(), any())).thenReturn(client);
        when(client.queryParameter(any(), any())).thenReturn(client);
        when(client.post(String.class)).thenReturn(response);
        return client;
    }

    private static Map<Key, RsResource> getResourceMap(String rsProtect) {
        Map<Key, RsResource> rsResourceMap = new HashMap<>();
        try {
            rsProtect = StringUtils.replace(rsProtect, "'", "\"");

            RsResourceList rsResourceList = Jackson2.createJsonMapper().readValue(rsProtect, RsResourceList.class);

            for (RsResource rsResource : rsResourceList.getResources()) {
                for (Condition condition : rsResource.getConditions()) {
                    Key key = new Key();
                    key.setHttpMethods(condition.getHttpMethods());
                    key.setPath(rsResource.getPath());
                    rsResourceMap.put(key, rsResource);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rsResourceMap;
    }

    private static Map<Key, String> getIdMap(String rsProtect) {
        Map<Key, String> rsIdMap = new HashMap<>();
        try {
            rsProtect = StringUtils.replace(rsProtect, "'", "\"");

            RsResourceList rsResourceList = Jackson2.createJsonMapper().readValue(rsProtect, RsResourceList.class);

            for (RsResource rsResource : rsResourceList.getResources()) {
                for (Condition condition : rsResource.getConditions()) {
                    Key key = new Key();
                    key.setHttpMethods(condition.getHttpMethods());
                    key.setPath(rsResource.getPath());
                    rsIdMap.put(key, UUID.randomUUID().toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rsIdMap;
    }
}
