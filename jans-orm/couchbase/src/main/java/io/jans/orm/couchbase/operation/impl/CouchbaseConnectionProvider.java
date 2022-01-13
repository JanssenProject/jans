/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.operation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.jans.orm.couchbase.model.BucketMapping;
import io.jans.orm.couchbase.model.ResultCode;
import io.jans.orm.exception.KeyConversionException;
import io.jans.orm.exception.operation.ConfigurationException;
import io.jans.orm.operation.auth.PasswordEncryptionMethod;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.message.internal.PingReport;
import com.couchbase.client.core.message.internal.PingServiceHealth;
import com.couchbase.client.core.message.internal.PingServiceHealth.PingState;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketInfo;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * Perform cluster initialization and open required buckets
 *
 * @author Yuriy Movchan Date: 05/10/2018
 */
public class CouchbaseConnectionProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private Properties props;

    private String[] servers;
    private String[] buckets;
    private String defaultBucket;

    private String userName;
    private String userPassword;

    private CouchbaseEnvironment couchbaseEnvironment;
    private CouchbaseCluster cluster;
    private int creationResultCode;

    private HashMap<String, BucketMapping> bucketToBaseNameMapping;
    private HashMap<String, BucketMapping> baseNameToBucketMapping;
    private BucketMapping defaultBucketMapping;

    private ArrayList<String> binaryAttributes, certificateAttributes;

    private PasswordEncryptionMethod passwordEncryptionMethod;

    protected CouchbaseConnectionProvider() {
    }

    public CouchbaseConnectionProvider(Properties props, CouchbaseEnvironment couchbaseEnvironment) {
        this.props = props;
        this.couchbaseEnvironment = couchbaseEnvironment;
    }

    public void create() {
        try {
            init();
        } catch (Exception ex) {
            this.creationResultCode = ResultCode.OPERATIONS_ERROR_INT_VALUE;

            Properties clonedProperties = (Properties) props.clone();
            if (clonedProperties.getProperty("auth.userName") != null) {
                clonedProperties.setProperty("auth.userPassword", "REDACTED");
            }

            LOG.error("Failed to create connection with properties: '{}'. Exception: {}", clonedProperties, ex);
            ex.printStackTrace();
        }
    }

    protected void init() {
        this.servers = StringHelper.split(props.getProperty("servers"), ",");

        this.userName = props.getProperty("auth.userName");
        this.userPassword = props.getProperty("auth.userPassword");

        this.defaultBucket = props.getProperty("bucket.default", null);
        if (StringHelper.isEmpty(defaultBucket)) {
            throw new ConfigurationException("Default bucket is not defined!");
        }

        this.buckets = StringHelper.split(props.getProperty("buckets"), ",");
        if (!Arrays.asList(buckets).contains(defaultBucket)) {
            this.buckets = ArrayHelper.addItemToStringArray(buckets, defaultBucket);
        }

        openWithWaitImpl();
        LOG.info("Opended: '{}' buket with base names: '{}'", bucketToBaseNameMapping.keySet(), baseNameToBucketMapping.keySet());

        if (props.containsKey("password.encryption.method")) {
            this.passwordEncryptionMethod = PasswordEncryptionMethod.getMethod(props.getProperty("password.encryption.method"));
        } else {
            this.passwordEncryptionMethod = PasswordEncryptionMethod.HASH_METHOD_SHA256;
        }

        this.binaryAttributes = new ArrayList<String>();
        if (props.containsKey("binaryAttributes")) {
            String[] binaryAttrs = StringHelper.split(props.get("binaryAttributes").toString().toLowerCase(), ",");
            this.binaryAttributes.addAll(Arrays.asList(binaryAttrs));
        }
        LOG.debug("Using next binary attributes: '{}'", binaryAttributes);

        this.certificateAttributes = new ArrayList<String>();
        if (props.containsKey("certificateAttributes")) {
            String[] binaryAttrs = StringHelper.split(props.get("certificateAttributes").toString().toLowerCase(), ",");
            this.certificateAttributes.addAll(Arrays.asList(binaryAttrs));
        }
        LOG.debug("Using next binary certificateAttributes: '{}'", certificateAttributes);

        this.creationResultCode = ResultCode.SUCCESS_INT_VALUE;
    }

    private void openWithWaitImpl() {
        String connectionMaxWaitTime = props.getProperty("connection.connection-max-wait-time");
        int connectionMaxWaitTimeSeconds = 30;
        if (StringHelper.isNotEmpty(connectionMaxWaitTime)) {
            connectionMaxWaitTimeSeconds = Integer.parseInt(connectionMaxWaitTime);
        }
        LOG.debug("Using Couchbase connection timeout: '{}'", connectionMaxWaitTimeSeconds);

        CouchbaseException lastException = null;

        int attempt = 0;
        long currentTime = System.currentTimeMillis();
        long maxWaitTime = currentTime + connectionMaxWaitTimeSeconds * 1000;
        do {
            attempt++;
            if (attempt > 0) {
                LOG.info("Attempting to create connection: '{}'", attempt);
            }

            try {
                open();
                if (isConnected()) {
                	break;
                } else {
                    LOG.info("Failed to connect to Couchbase");
                    destroy();
                    throw new CouchbaseException("Failed to create connection");
                }
            } catch (CouchbaseException ex) {
                lastException = ex;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                LOG.error("Exception happened in sleep", ex);
                return;
            }
            currentTime = System.currentTimeMillis();
        } while (maxWaitTime > currentTime);

        if (lastException != null) {
            throw lastException;
        }
    }

    private void open() {
        this.bucketToBaseNameMapping = new HashMap<String, BucketMapping>();
        this.baseNameToBucketMapping = new HashMap<String, BucketMapping>();

        this.cluster = CouchbaseCluster.create(couchbaseEnvironment, servers);
        cluster.authenticate(userName, userPassword);

        // Open required buckets
        for (String bucketName : buckets) {
            String baseNamesProp = props.getProperty(String.format("bucket.%s.mapping", bucketName), "");
            String[] baseNames = StringHelper.split(baseNamesProp, ",");

            Bucket bucket = cluster.openBucket(bucketName);

            BucketMapping bucketMapping = new BucketMapping(bucketName, bucket);

            // Store in separate map to speed up search by base name
            bucketToBaseNameMapping.put(bucketName, bucketMapping);
            for (String baseName : baseNames) {
                baseNameToBucketMapping.put(baseName, bucketMapping);
            }

            if (StringHelper.equalsIgnoreCase(bucketName, defaultBucket)) {
                this.defaultBucketMapping = bucketMapping;
            }
        }
    }

    public boolean destroy() {
    	boolean result = true;
    	if (bucketToBaseNameMapping != null) {
	        for (BucketMapping bucketMapping : bucketToBaseNameMapping.values()) {
	            try {
	                bucketMapping.getBucket().close();
	            } catch (CouchbaseException ex) {
	                LOG.error("Failed to close bucket '{}'", bucketMapping.getBucketName(), ex);
	                result = false;
	            }
	        }
    	}
    	
    	if (cluster != null) {
    		result &= cluster.disconnect();
    	}
    	
    	return result;
    }

    public boolean isConnected() {
        if (cluster == null) {
            return false;
        }

        boolean isConnected = true;
        try {
	        for (BucketMapping bucketMapping : bucketToBaseNameMapping.values()) {
                Bucket bucket = bucketMapping.getBucket();
                if (bucket.isClosed() || !isConnected(bucketMapping)) {
                    if (bucket.isClosed()) {
                        LOG.debug("Bucket '{}' is closed", bucketMapping.getBucketName());
                    }

                    LOG.error("Bucket '{}' is in invalid state", bucketMapping.getBucketName());
                    isConnected = false;
                    break;
                }
	        }
        } catch (RuntimeException ex) {
            LOG.error("Failed to check bucket", ex);
            isConnected = false;
        }

        return isConnected;
    }

    private boolean isConnected(BucketMapping bucketMapping) {
        Bucket bucket = bucketMapping.getBucket();

        BucketManager bucketManager = bucket.bucketManager();
        BucketInfo bucketInfo = bucketManager.info(30, TimeUnit.SECONDS);

        boolean result = true;
        if (com.couchbase.client.java.bucket.BucketType.COUCHBASE == bucketInfo.type()) {
        	// Check indexes state
	        Statement query = Select.select("state").from("system:indexes").where(Expression.path("state").eq(Expression.s("online")).not());
	        N1qlQueryResult queryResult = bucket.query(query); 
	        result = queryResult.finalSuccess();
            
            if (result) {
            	result = queryResult.info().resultCount() == 0;
            	if (LOG.isDebugEnabled()) {
            		LOG.debug("There are indexes which not online");
            	}
            } else {
            	if (LOG.isDebugEnabled()) {
            		LOG.debug("Faield to check indexes status");
            	}
            }
        }

        if (result) {
	    	PingReport pingReport = bucket.ping();
	    	for (PingServiceHealth pingServiceHealth : pingReport.services()) {
	    		if (PingState.OK != pingServiceHealth.state()) {
	        		LOG.debug("Ping returns that service typ {} is not online", pingServiceHealth.type());
	    			result = false;
	    			break;
	    		}
	    	}
        }
 
    	return result;
	}

	public BucketMapping getBucketMapping(String baseName) {
        BucketMapping bucketMapping = baseNameToBucketMapping.get(baseName);
        if (bucketMapping == null) {
            return null;
        }

        return bucketMapping;
    }

    public BucketMapping getBucketMappingByKey(String key) {
        if ("_".equals(key)) {
            return defaultBucketMapping;
        }

        String[] baseNameParts = key.split("_");
        if (ArrayHelper.isEmpty(baseNameParts)) {
            throw new KeyConversionException("Failed to determine base key part!");
        }

        BucketMapping bucketMapping = baseNameToBucketMapping.get(baseNameParts[0]);
        if (bucketMapping != null) {
            return bucketMapping;
        }

        return defaultBucketMapping;
    }

    public int getCreationResultCode() {
        return creationResultCode;
    }

    public boolean isCreated() {
        return ResultCode.SUCCESS_INT_VALUE == creationResultCode;
    }

    public String[] getServers() {
        return servers;
    }

    public ArrayList<String> getBinaryAttributes() {
        return binaryAttributes;
    }

    public ArrayList<String> getCertificateAttributes() {
        return certificateAttributes;
    }

    public boolean isBinaryAttribute(String attributeName) {
        if (StringHelper.isEmpty(attributeName)) {
            return false;
        }

        return binaryAttributes.contains(attributeName.toLowerCase());
    }

    public boolean isCertificateAttribute(String attributeName) {
        if (StringHelper.isEmpty(attributeName)) {
            return false;
        }

        return certificateAttributes.contains(attributeName.toLowerCase());
    }

    public PasswordEncryptionMethod getPasswordEncryptionMethod() {
        return passwordEncryptionMethod;
    }

}

