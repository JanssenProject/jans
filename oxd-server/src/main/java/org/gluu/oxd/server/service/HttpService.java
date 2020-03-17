/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.common.proxy.ProxyConfiguration;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

/**
 * @author Yuriy Zabrovarnyy
 */

public class HttpService {

    private static final Logger LOG = LoggerFactory.getLogger(HttpService.class);

    private OxdServerConfiguration configuration;

    @Inject
    public HttpService(OxdServerConfiguration configuration) {
        this.configuration = configuration;
    }

    public HttpClient getHttpClient() {
        final Optional<ProxyConfiguration> proxyConfig = asProxyConfiguration(configuration);
        try {
            validate(proxyConfig);
            final Boolean trustAllCerts = configuration.getTrustAllCerts();
            if (trustAllCerts != null && trustAllCerts) {
                LOG.trace("Created TRUST_ALL client.");
                return CoreUtils.createHttpClientTrustAll(proxyConfig);
            }
            final String keyStorePath = configuration.getKeyStorePath();
            if (StringUtils.isNotBlank(keyStorePath)) {
                final File keyStoreFile = new File(keyStorePath);
                if (!keyStoreFile.exists()) {
                    LOG.error("ERROR in configuration. Key store path is invalid! Please fix key_store_path in oxd configuration");
                } else {
                    return CoreUtils.createHttpClientWithKeyStore(keyStoreFile, configuration.getKeyStorePassword(), proxyConfig);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            LOG.error("Failed to create http client based on oxd configuration. Created default client.");
        }
        return CoreUtils.createClient(null, proxyConfig);
    }

    private static Optional<ProxyConfiguration> asProxyConfiguration(OxdServerConfiguration configuration) {
        try {
            JsonNode node = configuration.getProxyConfiguration();
            if (node != null) {
                return Optional.ofNullable(Jackson2.createJsonMapper().treeToValue(node, ProxyConfiguration.class));
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
            throw new RuntimeException("Invalid proxy server `hostname` provided (empty or null). oxd will connect to OP_HOST without proxy configuration.");
        }
    }

    public ClientExecutor getClientExecutor() {
        return new ApacheHttpClient4Executor(getHttpClient());
    }

    public ClientHttpEngine getClientEngine() {
        return new ApacheHttpClient4Engine(getHttpClient());
    }
}
