/*
 /*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.impl;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.couchbase.operation.impl.CouchbaseOperationsServiceImpl;
import org.gluu.persist.exception.operation.ConfigurationException;
import org.gluu.util.init.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;

/**
 * Couchbase Entry Manager Factory
 *
 * @author Yuriy Movchan Date: 05/31/2018
 */
@ApplicationScoped
public class CouchbaseEntryManagerFactory extends Initializable implements PersistenceEntryManagerFactory{

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseEntryManagerFactory.class);

    private DefaultCouchbaseEnvironment.Builder builder;
    private CouchbaseEnvironment couchbaseEnvironment;

	private Properties couchbaseConnectionProperties;

    @PostConstruct
    public void create() {
    	this.builder = DefaultCouchbaseEnvironment.builder().mutationTokensEnabled(true).computationPoolSize(5);
    }

	@Override
	protected void initInternal() {
        // SSL settings
        boolean useSSL = Boolean.valueOf(couchbaseConnectionProperties.getProperty("ssl.trustStore.enable")).booleanValue();
        if (useSSL) {
            String sslTrustStoreFile = couchbaseConnectionProperties.getProperty("ssl.trustStore.file");
            String sslTrustStorePin = couchbaseConnectionProperties.getProperty("ssl.trustStore.pin");

            this.couchbaseEnvironment = builder.sslEnabled(true).sslTruststoreFile(sslTrustStoreFile).sslTruststorePassword(sslTrustStorePin).build();
        } else {
        	this.couchbaseEnvironment = builder.sslEnabled(false).build();
        }

        this.builder = null;
	}

    @Override
    public String getPersistenceType() {
        return "couchbase";
    }

    @Override
    public String getDefaultConfigurationFileName() {
        return "gluu-couchbase.properties";
    }
    
    public CouchbaseEnvironment getCouchbaseEnvironment() {
    	return couchbaseEnvironment;
    }

    @Override
    public CouchbaseEntryManager createEntryManager(Properties conf) {
    	this.couchbaseConnectionProperties = conf;
    	init();
    	
    	if (!isInitialized()) {
            throw new ConfigurationException("Failed to create Couchbase environment !");
    	}

    	CouchbaseConnectionProvider connectionProvider = new CouchbaseConnectionProvider(conf, couchbaseEnvironment);
        connectionProvider.create();
        if (!connectionProvider.isCreated()) {
            throw new ConfigurationException(
                    String.format("Failed to create Couchbase connection pool! Result code: '%s'", connectionProvider.getCreationResultCode()));
        }
        LOG.debug("Created connectionProvider '{}' with code '{}'", connectionProvider, connectionProvider.getCreationResultCode());

        CouchbaseEntryManager couchbaseEntryManager = new CouchbaseEntryManager(new CouchbaseOperationsServiceImpl(connectionProvider));
        LOG.info("Created CouchbaseEntryManager: {}", couchbaseEntryManager.getOperationService());

        return couchbaseEntryManager;
    }
/*
    public static void main(String[] args) throws FileNotFoundException, IOException {
    	Properties prop = new Properties();
    	prop.load(new FileInputStream(new File("D:/Temp/gluu-couchbase.properties")));
    	
    	CouchbaseEntryManagerFactory cemf = new CouchbaseEntryManagerFactory();
    	cemf.create();
    	
    	CouchbaseEntryManager cem = cemf.createEntryManager(prop);
        
        System.out.println(cem.getOperationService().getConnectionProvider().isCreated());
        
	}
*/
}
