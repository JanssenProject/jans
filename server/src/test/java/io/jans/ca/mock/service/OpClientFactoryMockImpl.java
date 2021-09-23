package io.jans.ca.mock.service;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.jans.as.client.*;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.client.uma.UmaMetadataService;
import io.jans.as.model.common.TokenType;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.rs.protect.Condition;
import io.jans.ca.rs.protect.RsResource;
import io.jans.ca.rs.protect.RsResourceList;
import io.jans.ca.rs.protect.resteasy.*;
import io.jans.ca.server.introspection.ClientFactory;
import io.jans.ca.server.introspection.CorrectRptIntrospectionService;
import io.jans.ca.server.op.OpClientFactory;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpClientFactoryMockImpl implements OpClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OpClientFactoryMockImpl.class);
    private static Cache<String, Object> opClientCache = CacheBuilder.newBuilder()
            .expireAfterWrite(120, TimeUnit.MINUTES)
            .build();

    @Override
    public synchronized TokenClient createTokenClient(String url) {
        Optional<TokenClient> tokenClient = Optional.ofNullable((TokenClient) opClientCache.getIfPresent("TokenClient"));
        TokenClient client = null;
        if (!tokenClient.isPresent()) {
            client = mock(TokenClient.class);

            TokenResponse response = new TokenResponse();
            response.setAccessToken("DUMMY_ACCESS_TOKEN_" + System.currentTimeMillis());
            response.setTokenType(TokenType.BEARER);
            response.setExpiresIn(50000);
            response.setRefreshToken(null);
            response.setScope("openid");
            response.setIdToken("eyJraWQiOiJjZmFiMzRlYy0xNjhkLTQ4OTUtODRiOC0xZjAyNzgwNDkxYzciLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiMnI1clZ2STdpMWxfcnNXZUV4bGRuUSIsImF1ZCI6IjZiNTc4YTliLTc1MTMtNDc3YS05YTdmLTEzNDNiNDg3Y2FmOCIsInN1YiI6InMtX1ppclZ0N05PRGRuV0RBVUdyalQycVVad0s2Y1hUaGI5cVY5OXYtdGciLCJhdXRoX3RpbWUiOjE1NjgxODUzMjcsImlzcyI6Imh0dHBzOi8vZHVtbXktaXNzdWVyLm9yZyIsImV4cCI6MTk2ODE4ODkzMCwiaWF0IjoxNTY4MTg1MzMwLCJub25jZSI6IjdyNDZ1dDZlbXU5Z2kxMWduODA0NHVtNjQwIiwib3hPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIn0.Pp_rWPjTs0JWpomQIfHRrzE47cJcOQMO6otYyocgWgOUbzE0ttoS8dYvthU1LtkDdA8sBSX5rhB1CGugeSqvKdij6vLeJmE-A4G0OwfwrE7ROHLsbPpuGULJuIEwXgAZXdtoBwsNmK01Nu6ATEMgREl8dYPCRQ9divjQGLKAGLA");
            response.setStatus(200);

            when(client.exec()).thenReturn(response);
            when(client.execClientCredentialsGrant(any(), any(), any())).thenReturn(response);
            opClientCache.put("TokenClient", client);
        } else {
            client = (TokenClient) opClientCache.getIfPresent("TokenClient");
        }

        return client;
    }

    @Override
    public synchronized TokenClient createTokenClientWithUmaProtectionScope(String url) {

        Optional<TokenClient> umaTokenClient = Optional.ofNullable((TokenClient) opClientCache.getIfPresent("umaTokenClient"));
        TokenClient client = null;

        if (!umaTokenClient.isPresent()) {
            client = mock(TokenClient.class);

            TokenResponse response = new TokenResponse();
            response.setAccessToken("DUMMY_ACCESS_TOKEN_" + System.currentTimeMillis());
            response.setTokenType(TokenType.BEARER);
            response.setExpiresIn(50000);
            response.setRefreshToken(null);
            response.setScope("uma_protection");
            response.setIdToken("eyJraWQiOiJjZmFiMzRlYy0xNjhkLTQ4OTUtODRiOC0xZjAyNzgwNDkxYzciLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiaVFPZDJ2aEtWVWFzRVRCRDZEbjV0ZyIsImF1ZCI6IjIwNDE5ZDRkLTRhMGItNGIyOC05MjgwLTkzNmNlZDBkNjVmZSIsInN1YiI6InMtX1ppclZ0N05PRGRuV0RBVUdyalQycVVad0s2Y1hUaGI5cVY5OXYtdGciLCJhdXRoX3RpbWUiOjE1Njc1MDY4MTUsImlzcyI6Imh0dHBzOi8vY2UtZGV2Ni5nbHV1Lm9yZyIsImV4cCI6MTU2NzUxMDQxOCwiaWF0IjoxNTY3NTA2ODE4LCJub25jZSI6IjVqOTlyMW9tb2Q1azQ3MTFmMnB1ZDMzZTBhIiwib3hPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIn0.Wt_pUXW1BJyjcq2WJUMIYZwzEUeAmrDe8SaWM-RC7T86TmnQOnz0JMgEEN1J9ONsJNMdf8WJZDaqWXu2tVqHh1IrWmZ-U8_36HxcgPXy65yLho0hzCdjPp_KVTdttQhOmLvqn9x_NO8p06wBjm3d5T6xOgtxOjR0c4lqMOBDh3_jb9UH5ZLHRosx9pFCluylPjok8BREmOI_YnUJKHWz2Js9juWBnE94s50EOb7JuyVHvIDvVkrfh0YRZw61idaRQYzfEzwQQYJz6MF2xd4eHT3f-iB5ZBYdrtOPk0691ogLL3HbO_pCfjvsf4QVD0Q-4rlcSJ004ORyR77cgrBSAA");
            //response.setIdToken("eyJraWQiOiJjZmFiMzRlYy0xNjhkLTQ4OTUtODRiOC0xZjAyNzgwNDkxYzciLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdF9oYXNoIjoiaVFPZDJ2aEtWVWFzRVRCRDZEbjV0ZyIsImF1ZCI6IjIwNDE5ZDRkLTRhMGItNGIyOC05MjgwLTkzNmNlZDBkNjVmZSIsInN1YiI6InMtX1ppclZ0N05PRGRuV0RBVUdyalQycVVad0s2Y1hUaGI5cVY5OXYtdGciLCJhdXRoX3RpbWUiOjE1Njc1MDY4MTUsImlzcyI6Imh0dHBzOi8vY2UtZGV2Ni5nbHV1Lm9yZyIsImV4cCI6MTk2NzUxMDQxOCwiaWF0IjoxNTY3NTA2ODE4LCJub25jZSI6IjVqOTlyMW9tb2Q1azQ3MTFmMnB1ZDMzZTBhIiwib3hPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIn0.rxyKrMvfdScDkzyGQG9Mhc-qxjzOimqrsBTpKfTRpUM");
            response.setStatus(200);

            when(client.execClientCredentialsGrant(any(), any(), any())).thenReturn(response);

            opClientCache.put("umaTokenClient", client);
        } else {
            client = (TokenClient) opClientCache.getIfPresent("umaTokenClient");
        }
        return client;
    }

    @Override
    public synchronized JwkClient createJwkClient(String url) {
        Optional<JwkClient> jwkClient = Optional.ofNullable((JwkClient) opClientCache.getIfPresent("JwkClient"));
        Optional<RSAPublicKey> rsaPublicKeyOp = Optional.ofNullable((RSAPublicKey) opClientCache.getIfPresent("RSAPublicKey"));
        JwkClient client = null;
        RSAPublicKey rsaPublicKey = null;
        if (!jwkClient.isPresent() || !rsaPublicKeyOp.isPresent()) {
            client = mock(JwkClient.class);

            JwkResponse jwkResponse = mock(JwkResponse.class);
            when(jwkResponse.getStatus()).thenReturn(200);

            rsaPublicKey = mock(RSAPublicKey.class);

            when(jwkResponse.getPublicKey(any())).thenReturn(rsaPublicKey);

            when(client.exec()).thenReturn(jwkResponse);
            opClientCache.put("JwkClient", client);
            opClientCache.put("RSAPublicKey", rsaPublicKey);
        } else {
            client = (JwkClient) opClientCache.getIfPresent("JwkClient");
        }

        return client;
    }

    @Override
    public synchronized RSASigner createRSASigner(SignatureAlgorithm signatureAlgorithm, RSAPublicKey rsaPublicKey) {
        Optional<RSASigner> rsaSigner = Optional.ofNullable((RSASigner) opClientCache.getIfPresent("RSASigner"));
        RSASigner client = null;
        if (!rsaSigner.isPresent()) {
            client = mock(RSASigner.class);
            when(client.validate(any())).thenReturn(true);
            when(client.validateAccessToken(any(), any())).thenReturn(true);
            opClientCache.put("RSASigner", client);
        } else {
            client = (RSASigner) opClientCache.getIfPresent("RSASigner");
        }

        return client;
    }

    @Override
    public synchronized UserInfoClient createUserInfoClient(String url) {
        Optional<UserInfoClient> userInfoClient = Optional.ofNullable((UserInfoClient) opClientCache.getIfPresent("UserInfoClient"));
        UserInfoClient client = null;
        if (!userInfoClient.isPresent()) {
            client = mock(UserInfoClient.class);

            UserInfoResponse response = new UserInfoResponse(null);
            response.setEntity("{ \"name\":\"John\", \"age\":30, \"sub\":\"present\" }");

            when(client.exec()).thenReturn(response);
            opClientCache.put("UserInfoClient", client);
        } else {
            client = (UserInfoClient) opClientCache.getIfPresent("UserInfoClient");
        }
        return client;
    }

    @Override
    public synchronized RegisterClient createRegisterClient(String url) {
        Optional<RegisterClient> registerClient = Optional.ofNullable((RegisterClient) opClientCache.getIfPresent("RegisterClient"));
        RegisterClient client = null;
        if (!registerClient.isPresent()) {
            client = mock(RegisterClient.class);

            RegisterResponse response = new RegisterResponse();

            response.setClientId("6b578a9b-7513-477a-9a7f-1343b487caf8");
            response.setClientSecret("DUMMY_CLIENT_SECRET_" + System.currentTimeMillis());
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
            opClientCache.put("RegisterClient", client);
        } else {
            client = (RegisterClient) opClientCache.getIfPresent("RegisterClient");
        }
        return client;
    }

    @Override
    public synchronized OpenIdConfigurationClient createOpenIdConfigurationClient(String url) throws Exception {
        Optional<OpenIdConfigurationClient> openIdConfigurationClient = Optional.ofNullable((OpenIdConfigurationClient) opClientCache.getIfPresent("OpenIdConfigurationClient"));
        OpenIdConfigurationClient client = null;
        if (!openIdConfigurationClient.isPresent()) {
            client = mock(OpenIdConfigurationClient.class);

            OpenIdConfigurationResponse response = new OpenIdConfigurationResponse(200);
            response.setEntity("DUMMY_ENTITY");
            response.setRegistrationEndpoint("DUMMY_REGISTRATION_ENDPOINT");
            response.setEndSessionEndpoint("DUMMY_ENDSESSION_ENDPOINT");
            response.setIssuer("https://dummy-issuer.org");
            when(client.execOpenIdConfiguration()).thenReturn(response);
            opClientCache.put("OpenIdConfigurationClient", client);
        } else {
            client = (OpenIdConfigurationClient) opClientCache.getIfPresent("OpenIdConfigurationClient");
        }
        return client;
    }

    @Override
    public synchronized AuthorizeClient createAuthorizeClient(String url) {
        Optional<AuthorizeClient> authorizeClient = Optional.ofNullable((AuthorizeClient) opClientCache.getIfPresent("AuthorizeClient"));
        AuthorizeClient client = null;
        if (!authorizeClient.isPresent()) {
            client = mock(AuthorizeClient.class);

            AuthorizationResponse response = new AuthorizationResponse("");
            response.setCode("DUMMY_CODE_" + System.currentTimeMillis());
            response.setScope("DUMMY_SCOPE_" + System.currentTimeMillis());

            when(client.exec()).thenReturn(response);
            opClientCache.put("AuthorizeClient", client);
        } else {
            client = (AuthorizeClient) opClientCache.getIfPresent("AuthorizeClient");
        }

        return client;
    }

    public synchronized ResourceRegistrar createResourceRegistrar(PatProvider patProvider, ServiceProvider serviceProvider) {
        Optional<ResourceRegistrar> resourceRegistrar = Optional.ofNullable((ResourceRegistrar) opClientCache.getIfPresent("ResourceRegistrar"));
        ResourceRegistrar client = null;
        if (!resourceRegistrar.isPresent()) {
            client = mock(ResourceRegistrar.class);

            Map<Key, RsResource> resourceMapCopy = getResourceMap("{\"resources\":[{\"path\":\"/ws/phone\",\"conditions\":[{\"httpMethods\":[\"GET\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/view\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/view\"]},{\"httpMethods\":[\"PUT\", \"POST\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/add\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/add\"]},{\"httpMethods\":[\"DELETE\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/remove\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/remove\"]}]}]}");
            Map<Key, String> resourceIdMap = getIdMap("{\"resources\":[{\"path\":\"/ws/phone\",\"conditions\":[{\"httpMethods\":[\"GET\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/view\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/view\"]},{\"httpMethods\":[\"PUT\", \"POST\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/add\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/add\"]},{\"httpMethods\":[\"DELETE\"],\"scopes\":[\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/remove\"],\"ticketScopes\":[\"http://photoz.example.com/dev/actions/remove\"]}]}]}");

            when(client.getResourceMapCopy()).thenReturn(resourceMapCopy);
            when(client.getIdMapCopy()).thenReturn(resourceIdMap);
            opClientCache.put("ResourceRegistrar", client);
        } else {
            client = (ResourceRegistrar) opClientCache.getIfPresent("ResourceRegistrar");
        }

        return client;
    }

    public synchronized RptPreProcessInterceptor createRptPreProcessInterceptor(ResourceRegistrar resourceRegistrar) {
        Optional<RptPreProcessInterceptor> rptPreProcessInterceptor = Optional.ofNullable((RptPreProcessInterceptor) opClientCache.getIfPresent("RptPreProcessInterceptor"));
        RptPreProcessInterceptor client = null;
        if (!rptPreProcessInterceptor.isPresent()) {
            client = mock(RptPreProcessInterceptor.class);

            OutboundMessageContext outboundMessageContext = new OutboundMessageContext();
            PermissionTicket permissionTicket = new PermissionTicket("d457e3de-30dd-400a-8698-2b98472b7a40");
            outboundMessageContext.setEntity(permissionTicket);

            OutboundJaxrsResponse response = new OutboundJaxrsResponse(Response.Status.FORBIDDEN, outboundMessageContext);

            when(client.registerTicketResponse(any(List.class), any())).thenReturn(response);
            opClientCache.put("RptPreProcessInterceptor", client);
        } else {
            client = (RptPreProcessInterceptor) opClientCache.getIfPresent("RptPreProcessInterceptor");
        }
        return client;
    }

    public synchronized ClientFactory createClientFactory() {
        Optional<ClientFactory> clientFactoryOpt = Optional.ofNullable((ClientFactory) opClientCache.getIfPresent("ClientFactory"));
        Optional<CorrectRptIntrospectionService> correctRptIntrospectionServiceOpt = Optional.ofNullable((CorrectRptIntrospectionService) opClientCache.getIfPresent("CorrectRptIntrospectionService"));
        ClientFactory clientFactory = null;
        if (!clientFactoryOpt.isPresent() || !correctRptIntrospectionServiceOpt.isPresent()) {
            clientFactory = mock(ClientFactory.class);

            CorrectRptIntrospectionService correctRptIntrospectionService = mock(CorrectRptIntrospectionService.class);

            when(clientFactory.createCorrectRptStatusService(any(), any())).thenReturn(correctRptIntrospectionService);

            CorrectRptIntrospectionResponse correctRptIntrospectionResponse = new CorrectRptIntrospectionResponse();
            correctRptIntrospectionResponse.setActive(true);
            correctRptIntrospectionResponse.setClientId("d457e3de-30dd-400a-8698-2b98472b7a40");
            correctRptIntrospectionResponse.setIssuedAt((int) (System.currentTimeMillis() / 1000));
            correctRptIntrospectionResponse.setExpiresAt((int) (System.currentTimeMillis() / 1000));
            when(correctRptIntrospectionService.requestRptStatus(any(), any(), any())).thenReturn(correctRptIntrospectionResponse);
            opClientCache.put("ClientFactory", clientFactory);
            opClientCache.put("CorrectRptIntrospectionService", correctRptIntrospectionService);
        } else {
            clientFactory = (ClientFactory) opClientCache.getIfPresent("ClientFactory");
        }
        return clientFactory;
    }

    public synchronized UmaClientFactory createUmaClientFactory() {
        Optional<UmaClientFactory> umaClientFactoryOpt = Optional.ofNullable((UmaClientFactory) opClientCache.getIfPresent("umaClientFactory"));
        Optional<UmaMetadataService> umaMetadataServiceOpt = Optional.ofNullable((UmaMetadataService) opClientCache.getIfPresent("UmaMetadataService"));
        UmaClientFactory umaClientFactory = null;
        if (!umaClientFactoryOpt.isPresent() || !umaMetadataServiceOpt.isPresent()) {
            umaClientFactory = mock(UmaClientFactory.class);
            UmaMetadataService umaMetadataService = mock(UmaMetadataService.class);

            UmaMetadata umaMetadata = new UmaMetadata();

            when(umaClientFactory.createMetadataService(any(), any())).thenReturn(umaMetadataService);
            when(umaMetadataService.getMetadata()).thenReturn(umaMetadata);
            opClientCache.put("umaClientFactory", umaClientFactory);
            opClientCache.put("UmaMetadataService", umaMetadataService);
        } else {
            umaClientFactory = (UmaClientFactory) opClientCache.getIfPresent("umaClientFactory");
        }

        return umaClientFactory;
    }

    public synchronized ClientRequest createClientRequest(String uriTemplate, ClientExecutor executor) throws Exception {
        Optional<ClientRequest> clientRequest = Optional.ofNullable((ClientRequest) opClientCache.getIfPresent("ClientRequest"));
        Optional<ClientResponse> clientResponse = Optional.ofNullable((ClientResponse) opClientCache.getIfPresent("ClientResponse"));
        ClientRequest client = null;
        if (!clientRequest.isPresent() || !clientResponse.isPresent()) {
            client = mock(ClientRequest.class);

            ClientResponse<String> response = mock(ClientResponse.class);

            when(response.getEntity()).thenReturn("{ \"access_token\":\"d457e3de-30dd-400a-8698-2b98472b7a40\"," +
                    "\"token_type\":\"Bearer\"," +
                    "\"pct\":\"30dd\"" +
                    "}");
            when(client.header(any(), any())).thenReturn(client);
            when(client.formParameter(any(), any())).thenReturn(client);
            when(client.queryParameter(any(), any())).thenReturn(client);
            when(client.post(String.class)).thenReturn(response);
            opClientCache.put("ClientRequest", client);
            opClientCache.put("ClientResponse", response);
        } else {
            client = (ClientRequest) opClientCache.getIfPresent("ClientRequest");
        }
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
            LOG.error("Failed to parse uma-rs-protect resource json .", e);
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
            LOG.error("Failed to parse uma-rs-protect resource json .", e);
        }
        return rsIdMap;
    }
}
