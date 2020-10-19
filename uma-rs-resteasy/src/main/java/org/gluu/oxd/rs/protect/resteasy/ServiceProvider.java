package org.gluu.oxd.rs.protect.resteasy;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.log4j.Logger;
import org.gluu.oxauth.client.uma.UmaClientFactory;
import org.gluu.oxauth.client.uma.UmaMetadataService;
import org.gluu.oxauth.client.uma.UmaPermissionService;
import org.gluu.oxauth.client.uma.UmaResourceService;
import org.gluu.oxauth.client.uma.UmaRptIntrospectionService;
import org.gluu.oxauth.client.uma.UmaTokenService;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

/**
 * @author Yuriy Zabrovarnyy
 */

public class ServiceProvider {

    public static final String WELL_KNOWN_UMA_PATH = "/.well-known/uma2-configuration";

    private static final Logger LOG = Logger.getLogger(ServiceProvider.class);

    private final String opHost;

    private ClientHttpEngine engine;
	private ApacheHttpClient4Executor executor;

    private UmaMetadata umaMetadata = null;
	private UmaTokenService tokenService;
    private UmaMetadataService metadataService = null;
    private UmaResourceService resourceService = null;
    private UmaPermissionService permissionService;
    private UmaRptIntrospectionService rptIntrospectionService;

    /**
     * @param opHost opHost (example: https://ophost.com)
     * @throws KeyStoreException 
     * @throws NoSuchAlgorithmException 
     * @throws KeyManagementException 
     */
    public ServiceProvider(String opHost) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        this(opHost, true);
    }

    public ServiceProvider(String opHost, boolean trustAll) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        this(opHost, trustAll ? createAcceptSelfSignedCertificateClient() : createClient());
    }

    public ServiceProvider(String opHost, HttpClient httpClient) {
        this.opHost = opHost;
        this.engine = new ApacheHttpClient4Engine(httpClient);
        this.executor = new ApacheHttpClient4Executor(httpClient);
    }

    public synchronized UmaRptIntrospectionService getRptIntrospectionService() {
        if (rptIntrospectionService == null) {
            rptIntrospectionService = UmaClientFactory.instance().createRptStatusService(umaMetadata, engine);
        }
        return rptIntrospectionService;
    }

    public synchronized UmaMetadataService getMetadataService() {
        if (metadataService == null) {
            metadataService = UmaClientFactory.instance().createMetadataService(opHost + WELL_KNOWN_UMA_PATH, engine);
        }
        return metadataService;
    }

    public synchronized UmaMetadata getUmaMetadata() {
        if (umaMetadata == null) {
            umaMetadata = getMetadataService().getMetadata();
            LOG.trace("UMA discovery:" + umaMetadata);
        }
        return umaMetadata;
    }

    public synchronized UmaResourceService getResourceService() {
        if (resourceService == null) {
            resourceService = UmaClientFactory.instance().createResourceService(getUmaMetadata(), engine);
        }

        return resourceService;
    }

    public synchronized UmaPermissionService getPermissionService() {
        if (permissionService == null) {
            permissionService = UmaClientFactory.instance().createPermissionService(getUmaMetadata(), engine);
        }

        return permissionService;
    }

    public synchronized UmaTokenService getTokenService() {
        if (tokenService == null) {
        	tokenService = UmaClientFactory.instance().createTokenService(getUmaMetadata(), engine);
        }

        return tokenService;
    }

    public String getOpHost() {
        return opHost;
    }

    public String opHostWithoutProtocol() {
        if (StringUtils.contains(opHost, "//")) {
            return StringUtils.substringAfter(opHost, "//");
        }
        return opHost;
    }

	public ApacheHttpClient4Executor getClientExecutor() {
		return executor;
	}

	private static HttpClient createClient() {
	    return createClient(null);
	}

    private static HttpClient createAcceptSelfSignedCertificateClient()
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        SSLConnectionSocketFactory connectionFactory = createAcceptSelfSignedSocketFactory();

	    return createClient(connectionFactory);
    }

	private static HttpClient createClient(SSLConnectionSocketFactory connectionFactory) {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		HttpClientBuilder httClientBuilder = HttpClients.custom();
		if (connectionFactory != null) {
			httClientBuilder = httClientBuilder.setSSLSocketFactory(connectionFactory);
		}

		HttpClient httpClient = httClientBuilder
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
	    		.setConnectionManager(cm).build();
	    cm.setMaxTotal(200); // Increase max total connection to 200
	    cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20

	    return httpClient;
	}

	private static SSLConnectionSocketFactory createAcceptSelfSignedSocketFactory()
			throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
		// Use the TrustSelfSignedStrategy to allow Self Signed Certificates
        SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustSelfSignedStrategy()).build();

        // We can optionally disable hostname verification. 
        // If you don't want to further weaken the security, you don't have to include this.
        HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

        // Create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy
        // and allow all hosts verifier.
        SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);

        return connectionFactory;
	}

}
