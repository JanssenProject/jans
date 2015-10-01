/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.service;

import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.server.Configuration;

import java.io.File;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/05/2014
 */

public class HttpService {

    private static final Logger LOG = LoggerFactory.getLogger(HttpService.class);

    private Configuration configuration;

    @Inject
    public HttpService(Configuration configuration) {
        this.configuration = configuration;
    }

    public HttpClient getHttpClient() {
        try {
            final Boolean trustAllCerts = configuration.getTrustAllCerts();
            if (trustAllCerts != null && trustAllCerts) {
                LOG.trace("Created TRUST_ALL client.");
                return CoreUtils.createHttpClientTrustAll();
            }
            final String keyStorePath = configuration.getKeyStorePath();
            if (StringUtils.isNotBlank(keyStorePath)) {
                final File keyStoreFile = new File(keyStorePath);
                if (!keyStoreFile.exists()) {
                    LOG.error("ERROR in configuration. Key store path is invalid! Please fix key_store_path in oxd configuration");
                } else {
                    return CoreUtils.createHttpClientWithKeyStore(keyStoreFile);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            LOG.error("Failed to create http client based on oxd configuration. Created default client.");
        }
        return new DefaultHttpClient();
    }

    public ClientExecutor getClientExecutor() {
        return new ApacheHttpClient4Executor(getHttpClient());
    }
}
