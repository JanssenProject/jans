/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.impl;

import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import io.jans.orm.couchbase.operation.impl.CouchbaseConnectionProvider;
import io.jans.orm.couchbase.operation.impl.CouchbaseOperationServiceImpl;
import io.jans.orm.service.BaseFactoryService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.exception.operation.ConfigurationException;
import io.jans.orm.util.PropertiesHelper;
import io.jans.orm.util.StringHelper;
import io.jans.orm.util.init.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Properties;

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

    private DefaultCouchbaseEnvironment.Builder builder;
    private CouchbaseEnvironment couchbaseEnvironment;

	private Properties couchbaseConnectionProperties;

    @PostConstruct
    public void create() {
    	this.builder = DefaultCouchbaseEnvironment.builder().operationTracingEnabled(false);
    }

    @PreDestroy
    public void destroy() {
    	if (couchbaseEnvironment != null) {
    		boolean result = couchbaseEnvironment.shutdown();
    		resetInitialized();
    		LOG.info("Couchbase environment are destroyed with result: {}", result);
    	}
    }

	@Override
	protected void initInternal() {
        // SSL settings
        boolean useSSL = Boolean.valueOf(couchbaseConnectionProperties.getProperty("ssl.trustStore.enable")).booleanValue();
        if (useSSL) {
            String sslTrustStoreFile = couchbaseConnectionProperties.getProperty("ssl.trustStore.file");
            String sslTrustStorePin = couchbaseConnectionProperties.getProperty("ssl.trustStore.pin");

            builder.sslEnabled(true).sslTruststoreFile(sslTrustStoreFile).sslTruststorePassword(sslTrustStorePin);
        	LOG.info("Configuring builder to enable SSL support");
        } else {
        	builder.sslEnabled(false);
        	LOG.info("Configuring builder to disable SSL support");
        }
        
        String connectTimeoutString = couchbaseConnectionProperties.getProperty("connection.connect-timeout");
        if (StringHelper.isNotEmpty(connectTimeoutString)) {
        	int connectTimeout = Integer.valueOf(connectTimeoutString);
        	builder.connectTimeout(connectTimeout);
        	LOG.info("Configuring builder to override connectTimeout from properties");
        }

        String operationTracingEnabledString = couchbaseConnectionProperties.getProperty("connection.operation-tracing-enabled");
        if (StringHelper.isNotEmpty(operationTracingEnabledString)) {
        	boolean operationTracingEnabled = Boolean.valueOf(operationTracingEnabledString);
        	builder.operationTracingEnabled(operationTracingEnabled);
        	LOG.info("Configuring builder to override operationTracingEnabled from properties");
        }

        String mutationTokensEnabledString = couchbaseConnectionProperties.getProperty("connection.mutation-tokens-enabled");
        if (StringHelper.isNotEmpty(mutationTokensEnabledString)) {
        	boolean mutationTokensEnabled = Boolean.valueOf(mutationTokensEnabledString);
        	builder.mutationTokensEnabled(mutationTokensEnabled);
        	LOG.info("Configuring builder to override mutationTokensEnabled from properties");
        }

        String computationPoolSizeString = couchbaseConnectionProperties.getProperty("connection.computation-pool-size");
        if (StringHelper.isNotEmpty(computationPoolSizeString)) {
        	int computationPoolSize = Integer.valueOf(computationPoolSizeString);
        	builder.computationPoolSize(computationPoolSize);
        	LOG.info("Configuring builder to override computationPoolSize from properties");
        }

        String keepAliveTimeoutString = couchbaseConnectionProperties.getProperty("connection.keep-alive-timeout");
        if (StringHelper.isNotEmpty(keepAliveTimeoutString)) {
        	long keepAliveTimeout = Integer.valueOf(keepAliveTimeoutString);
        	builder.keepAliveTimeout(keepAliveTimeout);
        	LOG.info("Configuring builder to override keepAliveTimeout from properties");
        }

        String keepAliveIntervalString = couchbaseConnectionProperties.getProperty("connection.keep-alive-interval");
        if (StringHelper.isNotEmpty(keepAliveIntervalString)) {
        	long keepAliveInterval = Integer.valueOf(keepAliveIntervalString);
        	builder.keepAliveInterval(keepAliveInterval);
        	LOG.info("Configuring builder to override keepAliveInterval from properties");
        }

        this.couchbaseEnvironment = builder.build();

        this.builder = null;
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
    
    public CouchbaseEnvironment getCouchbaseEnvironment() {
    	return couchbaseEnvironment;
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

    	CouchbaseConnectionProvider connectionProvider = new CouchbaseConnectionProvider(entryManagerConf, couchbaseEnvironment);
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
		this.builder = DefaultCouchbaseEnvironment.builder().mutationTokensEnabled(true).computationPoolSize(5);
	}


/*
    public static void main(String[] args) throws FileNotFoundException, IOException {
    	Properties prop = new Properties();
    	prop.load(new FileInputStream(new File("D:/Temp/jans-couchbase.properties")));
    	
    	CouchbaseEntryManagerFactory cemf = new CouchbaseEntryManagerFactory();
    	cemf.create();
    	
    	CouchbaseEntryManager cem = cemf.createEntryManager(prop);
        
        System.out.println(cem.getOperationService().getConnectionProvider().isCreated());
        
	}
*/
}
