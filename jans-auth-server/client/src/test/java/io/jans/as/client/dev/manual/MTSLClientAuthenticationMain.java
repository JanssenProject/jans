/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.dev.manual;

import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

import static io.jans.as.client.BaseTest.showClient;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class MTSLClientAuthenticationMain {

    public static void main(String[] args) throws Exception {

        File jdkJks = new File("u:\\tmp\\ce-ob\\clientkeystore");
        assertTrue(jdkJks.exists(), "Failed to find jks trust store");

        File certificate = new File("u:\\tmp\\ce-ob\\fullchain.p12");
        assertTrue(certificate.exists(), "Failed to find certificate");

        HttpClient httpclient = new DefaultHttpClient();
// truststore
        KeyStore ts = KeyStore.getInstance("JKS", "SUN");
        ts.load(new FileInputStream(jdkJks), "secret".toCharArray());
// if you remove me, you've got 'javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated' on missing truststore
        if (0 == ts.size()) throw new IOException("Error loading truststore");
// tmf
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
// keystore
        KeyStore ks = KeyStore.getInstance("PKCS12", "SunJSSE");
        ks.load(new FileInputStream(certificate), "".toCharArray());
// if you remove me, you've got 'javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated' on missing keystore
        if (0 == ks.size()) throw new IOException("Error loading keystore");
// kmf
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "".toCharArray());
// SSL
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
// socket
        SSLSocketFactory socketFactory = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        Scheme sch = new Scheme("https", 443, socketFactory);
        httpclient.getConnectionManager().getSchemeRegistry().register(sch);

        String clientId = "@!D445.22BF.5EF1.0D87!0001!03F2.297D!0008!F599.E2C7";
        String clientSecret = "testClientSecret";

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode("testCode");
        tokenRequest.setRedirectUri("https://ce-ob.gluu.org/cas/login");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.TLS_CLIENT_AUTH);

        TokenClient tokenClient = new TokenClient("https://ce-ob.gluu.org/jans-auth/restv1/token");
        tokenClient.setExecutor(new ApacheHttpClient43Engine(httpclient));
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        System.out.println(tokenResponse);
        showClient(tokenClient);
    }
}
