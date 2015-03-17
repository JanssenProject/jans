/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.load.benchmark;

import static org.testng.Assert.assertNotNull;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.AuthorizationResponse;
import org.xdi.oxauth.client.AuthorizeClient;
import org.xdi.oxauth.client.OpenIdConfigurationClient;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/07/2014
 */

@Listeners({BenchmarkTestSuiteListener.class, BenchmarkTestListener.class})
public class BenchmarkAuthorizatoinRequests extends BaseTest {


    @Test
    public void testDiscovery() throws Exception {
        OpenIdConfigurationClient client = new OpenIdConfigurationClient("https://pcy28751:8443/oxauth/.well-known/openid-configuration");
//        OpenIdConfigurationClient client = new OpenIdConfigurationClient("https://seed.gluu.org/.well-known/openid-configuration");
        client.setExecutor(new ApacheHttpClient4Executor(createHttpClientTrustAll()));
        OpenIdConfigurationResponse r = client.execOpenIdConfiguration();
        Assert.assertNotNull(r);
    }

    // Think twice before invoking this test ;). Leads to OpenDJ (Berkley DB) failure
    // Caused by: LDAPSearchException(resultCode=80 (other), numEntries=0, numReferences=0, errorMessage='Database exception: (JE 4.1.10) JAVA_ERROR: Java Error occurred, recovery may not be possible.')
    // http://ox.gluu.org/doku.php?id=oxauth:profiling#obtain_access_token_-_2000_invocations_within_200_concurrent_threads
    @Parameters({"userId", "userSecret", "redirectUri", "clientId"})
    @Test(invocationCount = 2000, threadPoolSize = 10)
//    @Test
    public void testAuthentication(final String userId, final String userSecret, String redirectUri, String clientId) throws Exception {

        // hardcode -> we don't want to loose time on discover call
        String authorizationEndpoint = "https://pcy28751:8443/oxauth/seam/resource/restv1/oxauth/authorize";
//        String authorizationEndpoint = "https://localhost:8443/seam/resource/restv1/oxauth/authorize";

        final List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.TOKEN);
        responseTypes.add(ResponseType.ID_TOKEN);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = "STATE_XYZ";
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        request.setState(state);
        request.setNonce(nonce);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setExecutor(new ApacheHttpClient4Executor(createHttpClientTrustAll()));
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getAccessToken(), "The access token is null");
        assertNotNull(response.getState(), "The state is null");
        assertNotNull(response.getTokenType(), "The token type is null");
        assertNotNull(response.getExpiresIn(), "The expires in value is null");
        assertNotNull(response.getScope(), "The scope must be null");
    }

    public static HttpClient createHttpClientTrustAll() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        SSLSocketFactory sf = new SSLSocketFactory(new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
            }
        }, new AllowAllHostnameVerifier());

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", 8080, PlainSocketFactory.getSocketFactory()));
        registry.register(new Scheme("https", 8443, sf));
        ClientConnectionManager ccm = new PoolingClientConnectionManager(registry);
        return new DefaultHttpClient(ccm);
    }
}
