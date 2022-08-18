/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.orm.couchbase.impl;

import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;

import io.jans.orm.util.PropertiesHelper;
import io.jans.orm.util.StringHelper;
import io.jans.orm.util.init.Initializable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.exception.operation.ConfigurationException;
import io.jans.orm.service.BaseFactoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.core.env.SecurityConfig;
import com.couchbase.client.java.env.ClusterEnvironment;

import io.jans.orm.couchbase.operation.impl.CouchbaseConnectionProvider;
import io.jans.orm.couchbase.operation.impl.CouchbaseOperationServiceImpl;

/**
 * Couchbase Entry Manager Factory
 *
 * @author Yuriy Movchan Date: 05/31/2018
 */
@ApplicationScoped
public class CouchbaseEntryManagerFactory extends Initializable implements PersistenceEntryManagerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseEntryManagerFactory.class);

    public static final String PERSISTENCE_TYPE = PersistenceEntryManager.PERSITENCE_TYPES.couchbase.name();
    public static final String PROPERTIES_FILE = "jans-couchbase%s.properties";

    private ClusterEnvironment clusterEnvironment;

	private Properties couchbaseConnectionProperties;

    @PostConstruct
    public void create() {}

    @PreDestroy
    public void destroy() {
    	if (clusterEnvironment != null) {
    		clusterEnvironment.shutdown();
    		resetInitialized();
    		LOG.info("Couchbase environment was destroyed successfully");
    	}
    }

	@Override
	protected void initInternal() {
	    ClusterEnvironment.Builder clusterEnvironmentBuilder = ClusterEnvironment.builder();

	    // SSL settings
        boolean useSSL = Boolean.valueOf(couchbaseConnectionProperties.getProperty("ssl.trustStore.enable")).booleanValue();
        if (useSSL) {
            String sslTrustStoreFile = couchbaseConnectionProperties.getProperty("ssl.trustStore.file");
            String sslTrustStorePin = couchbaseConnectionProperties.getProperty("ssl.trustStore.pin");
            Optional<String> sslTrustStoreType = Optional.ofNullable(couchbaseConnectionProperties.getProperty("ssl.trustStore.type"));

            SecurityConfig.Builder securityConfigBuilder = clusterEnvironmentBuilder.securityConfig();

            boolean enableTLS = Boolean.valueOf(couchbaseConnectionProperties.getProperty("tls.enable")).booleanValue();
            if (enableTLS) {
            	securityConfigBuilder.enableTls(enableTLS);
            }

            securityConfigBuilder.trustStore(FileSystems.getDefault().getPath(sslTrustStoreFile), sslTrustStorePin, sslTrustStoreType);
        	LOG.info("Configuring builder to enable SSL support");
        } else {
        	clusterEnvironmentBuilder.securityConfig().enableTls(false);
        	LOG.info("Configuring builder to disable SSL support");
        }

        String connectTimeoutString = couchbaseConnectionProperties.getProperty("connection.connect-timeout");
        if (StringHelper.isNotEmpty(connectTimeoutString)) {
        	int connectTimeout = Integer.valueOf(connectTimeoutString);
           	clusterEnvironmentBuilder.timeoutConfig().connectTimeout(Duration.ofMillis(connectTimeout));
        	LOG.info("Configuring builder to override connectTimeout from properties");
        }

        String connectDnsUseLookupString = couchbaseConnectionProperties.getProperty("connection.dns.use-lookup");
        if (StringHelper.isNotEmpty(connectDnsUseLookupString)) {
        	boolean connectDnsUseLookup = Boolean.valueOf(connectDnsUseLookupString);
           	clusterEnvironmentBuilder.ioConfig().enableDnsSrv(connectDnsUseLookup);
        	LOG.info("Configuring builder to override enableDnsSrv from properties");
        }

        String kvTimeoutString = couchbaseConnectionProperties.getProperty("connection.kv-timeout");
        if (StringHelper.isNotEmpty(kvTimeoutString)) {
        	int kvTimeout = Integer.valueOf(kvTimeoutString);
           	clusterEnvironmentBuilder.timeoutConfig().kvTimeout(Duration.ofMillis(kvTimeout));
        	LOG.info("Configuring builder to override kvTimeout from properties");
        }

        String queryTimeoutString = couchbaseConnectionProperties.getProperty("connection.query-timeout");
        if (StringHelper.isNotEmpty(queryTimeoutString)) {
        	int queryTimeout = Integer.valueOf(queryTimeoutString);
           	clusterEnvironmentBuilder.timeoutConfig().queryTimeout(Duration.ofMillis(queryTimeout));
        	LOG.info("Configuring builder to override queryTimeout from properties");
        }

        String mutationTokensEnabledString = couchbaseConnectionProperties.getProperty("connection.mutation-tokens-enabled");
        if (StringHelper.isNotEmpty(mutationTokensEnabledString)) {
        	boolean mutationTokensEnabled = Boolean.valueOf(mutationTokensEnabledString);
        	clusterEnvironmentBuilder.ioConfig().enableMutationTokens(mutationTokensEnabled);
        	LOG.info("Configuring builder to override mutationTokensEnabled from properties");
        }

        this.clusterEnvironment = clusterEnvironmentBuilder.build();
	}

    @Override
    public String getPersistenceType() {
        return PERSISTENCE_TYPE;
    }

    @Override
    public HashMap<String, String> getConfigurationFileNames(String alias) {
    	String usedAlias = StringHelper.isEmpty(alias) ? "" : "." + alias; 

    	HashMap<String, String> confs = new HashMap<String, String>();
    	String confFileName = String.format(PROPERTIES_FILE, usedAlias);
    	confs.put(PERSISTENCE_TYPE + usedAlias, confFileName);

    	return confs;
    }
    
    public ClusterEnvironment getClusterEnvironment() {
    	return clusterEnvironment;
    }

    @Override
    public CouchbaseEntryManager createEntryManager(Properties conf) {
		Properties entryManagerConf = PropertiesHelper.filterProperties(conf, "#");

		// Allow proper initialization
		if (this.couchbaseConnectionProperties == null) {
			this.couchbaseConnectionProperties = entryManagerConf;
		}

    	init();
    	
    	if (!isInitialized()) {
            throw new ConfigurationException("Failed to create Couchbase environment!");
    	}

    	CouchbaseConnectionProvider connectionProvider = new CouchbaseConnectionProvider(entryManagerConf, clusterEnvironment);
        connectionProvider.create();
        if (!connectionProvider.isCreated()) {
            throw new ConfigurationException(
                    String.format("Failed to create Couchbase connection pool! Result code: '%s'", connectionProvider.getCreationResultCode()));
        }
        LOG.debug("Created connectionProvider '{}' with code '{}'", connectionProvider, connectionProvider.getCreationResultCode());

        CouchbaseEntryManager couchbaseEntryManager = new CouchbaseEntryManager(new CouchbaseOperationServiceImpl(entryManagerConf, connectionProvider));
        LOG.info("Created CouchbaseEntryManager: {}", couchbaseEntryManager.getOperationService());

        return couchbaseEntryManager;
    }

	@Override
	public void initStandalone(BaseFactoryService persistanceFactoryService) {
	    ClusterEnvironment.Builder clusterEnvironmentBuilder = ClusterEnvironment.builder();
	    clusterEnvironmentBuilder.ioConfig().enableMutationTokens(true).build();

	    this.clusterEnvironment = clusterEnvironmentBuilder.build();
	}

}
