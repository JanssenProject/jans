package org.gluu.oxd.client;

import org.glassfish.jersey.client.proxy.WebResourceFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * @author yuriyz
 */
public class OxdClient {

    private OxdClient() {
    }

    public static ClientInterface newClient(String target) {
        WebTarget webTarget = ClientBuilder.newClient().target(target);
        return WebResourceFactory.newResource(ClientInterface.class, webTarget);
    }

    public static ClientInterface newTrustAllClient(String target) throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());

        WebTarget webTarget = ClientBuilder.newBuilder().sslContext(sc).build().target(target);
        return WebResourceFactory.newResource(ClientInterface.class, webTarget);
    }
}
