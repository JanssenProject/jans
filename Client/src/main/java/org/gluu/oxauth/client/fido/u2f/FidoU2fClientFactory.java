/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.fido.u2f;

import javax.ws.rs.core.UriBuilder;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.gluu.oxauth.model.fido.u2f.U2fConfiguration;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

/**
 * Helper class which creates proxy FIDO U2F services
 *
 * @author Yuriy Movchan Date: 05/27/2015
 */
public class FidoU2fClientFactory {

    private final static FidoU2fClientFactory instance = new FidoU2fClientFactory();

    private ApacheHttpClient4Engine engine;

    private FidoU2fClientFactory() {
        this.engine = createEngine();
    }

    public static FidoU2fClientFactory instance() {
        return instance;
    }

    public U2fConfigurationService createMetaDataConfigurationService(String u2fMetaDataUri) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(u2fMetaDataUri));
        U2fConfigurationService proxy = target.proxy(U2fConfigurationService.class);

        return proxy;
    }

    public AuthenticationRequestService createAuthenticationRequestService(U2fConfiguration metadataConfiguration) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadataConfiguration.getAuthenticationEndpoint()));
        AuthenticationRequestService proxy = target.proxy(AuthenticationRequestService.class);

        return proxy;
    }

    public RegistrationRequestService createRegistrationRequestService(U2fConfiguration metadataConfiguration) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(metadataConfiguration.getRegistrationEndpoint()));
        RegistrationRequestService proxy = target.proxy(RegistrationRequestService.class);

        return proxy;
    }

	private ApacheHttpClient4Engine createEngine() {
	    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
	    CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
	    		.setConnectionManager(cm).build();
	    cm.setMaxTotal(200); // Increase max total connection to 200
	    cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20
	    ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
	    
	    return engine;
	}

}
