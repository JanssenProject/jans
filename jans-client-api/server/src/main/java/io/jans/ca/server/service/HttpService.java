/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.service;

import com.google.common.base.Strings;
import io.jans.ca.common.CoreUtils;
import io.jans.ca.common.proxy.ProxyConfiguration;
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.ca.server.configuration.ConfigurationFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.http.client.HttpClient;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class HttpService {

    private static final Logger LOG = LoggerFactory.getLogger(HttpService.class);
    @Inject
    ConfigurationFactory configurationFactory;

    public HttpClient getHttpClient() {
        ApiAppConfiguration configuration = configurationFactory.getAppConfiguration();
        final Optional<ProxyConfiguration> proxyConfig = asProxyConfiguration(configuration);
        final String[] tlsVersions = listToArray(configuration.getTlsVersion());
        final String[] tlsSecureCiphers = listToArray(configuration.getTlsSecureCipher());
        try {
            validate(proxyConfig);
            final Boolean trustAllCerts = configuration.getTrustAllCerts();
            if (trustAllCerts != null && trustAllCerts) {
                LOG.trace("Created TRUST_ALL client.");
                return CoreUtils.createHttpClientTrustAll(proxyConfig, tlsVersions, tlsSecureCiphers);
            }
            final String trustStorePath = configuration.getKeyStorePath();

            if (Strings.isNullOrEmpty(trustStorePath)) {
                return CoreUtils.createClientFallback(proxyConfig);
            }
            final File trustStoreFile = new File(trustStorePath);

            if (!trustStoreFile.exists()) {
                LOG.error("ERROR in configuration. Trust store path is invalid! Please fix key_store_path in jans_client_api configuration");
                return CoreUtils.createClientFallback(proxyConfig);
            }
            //Perform mutual authentication over SSL if allowed
            if (configuration.getMtlsEnabled()) {
                final String mtlsClientKeyStorePath = configuration.getMtlsClientKeyStorePath();

                if (Strings.isNullOrEmpty(mtlsClientKeyStorePath)) {
                    LOG.error("Mtls Client key store path is empty! Please fix mtls_client_key_store_path in jans_client_api configuration");
                    return CoreUtils.createHttpClientWithKeyStore(trustStoreFile, configuration.getKeyStorePassword(), tlsVersions, tlsSecureCiphers, proxyConfig);
                }
                final File mtlsClientKeyStoreFile = new File(mtlsClientKeyStorePath);
                if (!mtlsClientKeyStoreFile.exists()) {
                    LOG.error("ERROR in configuration. Mtls Client key stroe path is invalid! Please fix mtls_client_key_store_path in jans_client_api configuration");
                    return CoreUtils.createHttpClientWithKeyStore(trustStoreFile, configuration.getKeyStorePassword(), tlsVersions, tlsSecureCiphers, proxyConfig);
                }
                return CoreUtils.createHttpClientForMutualAuthentication(trustStoreFile, configuration.getKeyStorePassword(), mtlsClientKeyStoreFile, configuration.getMtlsClientKeyStorePassword(), tlsVersions, tlsSecureCiphers, proxyConfig);
            }
            return CoreUtils.createHttpClientWithKeyStore(trustStoreFile, configuration.getKeyStorePassword(), tlsVersions, tlsSecureCiphers, proxyConfig);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            LOG.error("Failed to create http client based on jans_client_api configuration. Creating default client...");
        }
        return CoreUtils.createClientFallback(proxyConfig);
    }

    private static Optional<ProxyConfiguration> asProxyConfiguration(ApiAppConfiguration configuration) {
        try {
            ProxyConfiguration node = configuration.getProxyConfiguration();
            if (node != null) {
                return Optional.ofNullable(node);
            }
        } catch (Exception e) {
            LOG.error("Failed to parse ProxyConfiguration.", e);
        }
        return Optional.empty();
    }

    private void validate(Optional<ProxyConfiguration> proxyConfiguration) {

        if (!proxyConfiguration.isPresent()) {
            return;
        }

        if (Strings.isNullOrEmpty(proxyConfiguration.get().getHost())) {
            LOG.warn("Invalid proxy server `hostname` provided (empty or null). jans_client_api will connect to OP_HOST without proxy configuration.");
        }
    }

    public ClientHttpEngine getClientEngine() {
        return new ApacheHttpClient43Engine(getHttpClient());
    }

    private static String[] listToArray(List<String> input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return input.stream().toArray(String[]::new);

    }
}