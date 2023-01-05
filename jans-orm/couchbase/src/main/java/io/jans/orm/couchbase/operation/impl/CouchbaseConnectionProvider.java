/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.operation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;
import io.jans.orm.exception.KeyConversionException;
import io.jans.orm.exception.operation.ConfigurationException;
import io.jans.orm.operation.auth.PasswordEncryptionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.core.diagnostics.EndpointPingReport;
import com.couchbase.client.core.diagnostics.PingResult;
import com.couchbase.client.core.diagnostics.PingState;
import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.manager.bucket.BucketManager;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryStatus;

import io.jans.orm.couchbase.model.BucketMapping;
import io.jans.orm.couchbase.model.ResultCode;

/**
 * Perform cluster initialization and open required buckets
 *
 * @author Yuriy Movchan Date: 05/10/2018
 */
public class CouchbaseConnectionProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private Properties props;

    private String connectionString;
    private String[] buckets;
    private String defaultBucket;

    private String userName;
    private String userPassword;

    private ClusterEnvironment clusterEnvironment;
    private Cluster cluster;
    private int creationResultCode;

    private HashMap<String, BucketMapping> bucketToBaseNameMapping;
    private HashMap<String, BucketMapping> baseNameToBucketMapping;
    private BucketMapping defaultBucketMapping;

    private ArrayList<String> binaryAttributes, certificateAttributes;

    private PasswordEncryptionMethod passwordEncryptionMethod;

    protected CouchbaseConnectionProvider() {
    }

    public CouchbaseConnectionProvider(Properties props, ClusterEnvironment clusterEnvironment) {
        this.props = props;
        this.clusterEnvironment = clusterEnvironment;
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
        this.connectionString = props.getProperty("servers");

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

        ClusterOptions clusterOptions = ClusterOptions.clusterOptions(userName, userPassword);
        if (clusterEnvironment != null) {
        	clusterOptions.environment(clusterEnvironment);
        }

        this.cluster = Cluster.connect(connectionString, clusterOptions);

        // Open required buckets
        for (String bucketName : buckets) {
            String baseNamesProp = props.getProperty(String.format("bucket.%s.mapping", bucketName), "");
            String[] baseNames = StringHelper.split(baseNamesProp, ",");

            Bucket bucket = this.cluster.bucket(bucketName);

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
    	if (this.cluster != null) {
    		this.cluster.disconnect();
    	}
    	
    	return true;
    }

    public boolean isConnected() {
        if (cluster == null) {
            return false;
        }

        boolean isConnected = true;
        try {
	        for (BucketMapping bucketMapping : bucketToBaseNameMapping.values()) {
                if (!isConnected(bucketMapping)) {
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

        BucketManager bucketManager = this.cluster.buckets();
        BucketSettings bucketSettings = bucketManager.getBucket(bucket.name());

        boolean result = true;
        if (com.couchbase.client.java.manager.bucket.BucketType.COUCHBASE == bucketSettings.bucketType()) {
        	// Check indexes state
        	QueryResult queryResult = cluster.query("SELECT state FROM system:indexes WHERE state != $1 AND keyspace_id = $2", QueryOptions.queryOptions().parameters(JsonArray.from("online", bucket.name())));
            
            if (QueryStatus.SUCCESS == queryResult.metaData().status()) {
            	result = queryResult.rowsAsObject().size() == 0;
            	if (LOG.isDebugEnabled()) {
            		LOG.debug("There are indexes which not online");
            	}
            } else {
            	result = false;
            	if (LOG.isDebugEnabled()) {
            		LOG.debug("Faield to check indexes status");
            	}
            }
        }

        if (result) {
        	PingResult pingResult = bucket.ping();
	    	for (Entry<ServiceType, List<EndpointPingReport>> pingResultEntry : pingResult.endpoints().entrySet()) {
	    		for (EndpointPingReport endpointPingReport : pingResultEntry.getValue()) {
		    		if (PingState.OK != endpointPingReport.state()) {
		        		LOG.debug("Ping returns that service type {} is not online", endpointPingReport.type());
		    			result = false;
		    			break;
		    		}
	    		}
	    	}
        }
 
    	return result;
	}

	public Cluster getCluster() {
		return cluster;
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

    public String getServers() {
        return connectionString;
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

