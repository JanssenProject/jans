/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap.operation.impl;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.FailoverServerSet;
import com.unboundid.ldap.sdk.GetEntryLDAPConnectionPoolHealthCheck;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;

import io.jans.orm.exception.operation.ConfigurationException;
import io.jans.orm.operation.auth.PasswordEncryptionMethod;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.CertUtils;
import io.jans.orm.util.StringHelper;

/**
 * @author Yuriy Movchan
 */
public class LdapConnectionProvider {

	private static final Logger LOG = LoggerFactory.getLogger(LdapConnectionProvider.class);

    private static final int DEFAULT_SUPPORTED_LDAP_VERSION = 2;
    private static final String DEFAULT_SUBSCHEMA_SUBENTRY = "cn=schema";

    private static final String[] SSL_PROTOCOLS = {"TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3"};

    private LDAPConnectionPool connectionPool;
    private ResultCode creationResultCode;

    private int supportedLDAPVersion = DEFAULT_SUPPORTED_LDAP_VERSION;
    private String subschemaSubentry = DEFAULT_SUBSCHEMA_SUBENTRY;

    private String[] servers;
    private String[] addresses;
    private int[] ports;

    private String bindDn;
    private String bindPassword;
    private boolean useSSL;

    private ArrayList<PasswordEncryptionMethod> additionalPasswordMethods;
    private ArrayList<String> binaryAttributes, certificateAttributes;

    private boolean supportsSubtreeDeleteRequestControl;

	private Properties props;

    protected LdapConnectionProvider() {
    }

    public LdapConnectionProvider(Properties props) {
        this.props = props;
    }

    public void create(Properties props) {
    	this.props = props;
    	create();
    }

    public void create() {
        try {
            init(props);
        } catch (Exception ex) {
        	if (ex instanceof LDAPException) {
        		creationResultCode = ((LDAPException) ex).getResultCode();
        	}

            Properties clonedProperties = (Properties) props.clone();
            if (clonedProperties.getProperty("bindPassword") != null) {
                clonedProperties.setProperty("bindPassword", "REDACTED");
            }
            if (clonedProperties.getProperty("ssl.trustStorePin") != null) {
                clonedProperties.setProperty("ssl.trustStorePin", "REDACTED");
            }
            LOG.error("Failed to create connection pool with properties: " + clonedProperties, ex);
        }
    }

    /**
     * This method is used to create LDAPConnectionPool
     *
     * @throws NumberFormatException
     * @throws LDAPException
     * @throws GeneralSecurityException
     * @throws EncryptionException
     * @throws EncryptionException
     */
    protected void init(Properties props) throws NumberFormatException, LDAPException, GeneralSecurityException {
        String serverProp = props.getProperty("servers");
        this.servers = serverProp.split(",");
        this.addresses = new String[this.servers.length];
        this.ports = new int[this.servers.length];
        for (int i = 0; i < this.servers.length; i++) {
            String str = this.servers[i];
            int idx = str.indexOf(":");
            if (idx == -1) {
                throw new ConfigurationException("Ldap server settings should be in format server:port");
            }
            this.addresses[i] = str.substring(0, idx).trim();
            this.ports[i] = Integer.parseInt(str.substring(str.indexOf(":") + 1, str.length()));
        }

        BindRequest bindRequest = null;
        if (StringHelper.isEmpty(props.getProperty("bindDN"))) {
            this.bindDn = null;
            this.bindPassword = null;
            bindRequest = new SimpleBindRequest();
        } else {
            this.bindDn = props.getProperty("bindDN");
            this.bindPassword = props.getProperty("bindPassword");
            bindRequest = new SimpleBindRequest(this.bindDn, this.bindPassword);
        }

        LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();
        connectionOptions.setConnectTimeoutMillis(100 * 1000);
        //connectionOptions.setAutoReconnect(true);

        this.useSSL = Boolean.valueOf(props.getProperty("useSSL")).booleanValue();

        SSLUtil sslUtil = null;
        FailoverServerSet failoverSet;
        if (this.useSSL) {
            String trustStoreFile = props.getProperty("ssl.trustStoreFile");
            String trustStorePin = props.getProperty("ssl.trustStorePin");
            String trustStoreType = props.getProperty("ssl.trustStoreFormat");

            if (CertUtils.isFips()) {
                sslUtil = new SSLUtil(CertUtils.getTrustManagers(trustStoreFile, trustStorePin, trustStoreType));
            } else {
	            if (StringHelper.isEmpty(trustStoreFile) && StringHelper.isEmpty(trustStorePin)) {
                sslUtil = new SSLUtil(new TrustAllTrustManager());
            } else {
	                TrustStoreTrustManager trustStoreTrustManager = new TrustStoreTrustManager(trustStoreFile, trustStorePin.toCharArray(),
	                        trustStoreType, true);
                sslUtil = new SSLUtil(trustStoreTrustManager);
            }
            }

            failoverSet = new FailoverServerSet(this.addresses, this.ports, sslUtil.createSSLSocketFactory(SSL_PROTOCOLS[0]), connectionOptions);
        } else {
            failoverSet = new FailoverServerSet(this.addresses, this.ports, connectionOptions);
        }

        int maxConnections = StringHelper.toInt(props.getProperty("maxconnections"), 10);
        this.connectionPool = createConnectionPoolWithWaitImpl(props, failoverSet, bindRequest, connectionOptions, maxConnections, sslUtil);
        if (this.connectionPool != null) {
            this.connectionPool.setCreateIfNecessary(true);
            String connectionMaxWaitTime = props.getProperty("connection.max-wait-time-millis");
            if (StringHelper.isNotEmpty(connectionMaxWaitTime)) {
                this.connectionPool.setMaxWaitTimeMillis(Long.parseLong(connectionMaxWaitTime));
            }
            String maxConnectionAge = props.getProperty("connection.max-age-time-millis");
            if (StringHelper.isNotEmpty(connectionMaxWaitTime)) {
                this.connectionPool.setMaxConnectionAgeMillis(Long.parseLong(maxConnectionAge));
            }
            boolean onCheckoutHealthCheckEnabled = StringHelper.toBoolean(props.getProperty("connection-pool.health-check.on-checkout.enabled"), false);
            long healthCheckIntervalMillis = StringHelper.toLong(props.getProperty("connection-pool.health-check.interval-millis"), 0);
            long healthCheckMaxResponsetimeMillis = StringHelper.toLong(props.getProperty("connection-pool.health-check.max-response-time-millis"), 0);
            boolean backgroundHealthCheckEnabled = !onCheckoutHealthCheckEnabled && (healthCheckIntervalMillis > 0);
            // Because otherwise it has no effect anyway
            if (backgroundHealthCheckEnabled) {
                this.connectionPool.setHealthCheckIntervalMillis(healthCheckIntervalMillis);
            }
            if (onCheckoutHealthCheckEnabled || backgroundHealthCheckEnabled) {
                GetEntryLDAPConnectionPoolHealthCheck healthChecker = new GetEntryLDAPConnectionPoolHealthCheck(// entryDN (null means root DSE)
                        null, // maxResponseTime
                        healthCheckMaxResponsetimeMillis, // invokeOnCreate
                        false, // invokeOnCheckout
                        onCheckoutHealthCheckEnabled, // invokeOnRelease
                        false, // invokeForBackgroundChecks
                        backgroundHealthCheckEnabled, // invokeOnException
                        false);
                
                this.connectionPool.setHealthCheck(healthChecker);
            }
        }

        this.additionalPasswordMethods = new ArrayList<PasswordEncryptionMethod>();
        if (props.containsKey("additionalPasswordMethods")) {
            String[] additionalPasswordMethodsArray = StringHelper.split(props.get("additionalPasswordMethods").toString(), ",");
            for (String additionalPasswordMethod : additionalPasswordMethodsArray) {
                PasswordEncryptionMethod passwordEncryptionMethod =  PasswordEncryptionMethod.getMethod(additionalPasswordMethod);
                if (passwordEncryptionMethod != null) {
                    this.additionalPasswordMethods.add(passwordEncryptionMethod);
                }
            }
        }
        LOG.debug("Adding support for password methods: " + this.additionalPasswordMethods);

        this.binaryAttributes = new ArrayList<String>();
        if (props.containsKey("binaryAttributes")) {
            String[] binaryAttrs = StringHelper.split(props.get("binaryAttributes").toString().toLowerCase(), ",");
            this.binaryAttributes.addAll(Arrays.asList(binaryAttrs));
        }
        LOG.debug("Using next binary attributes: " + this.binaryAttributes);

        this.certificateAttributes = new ArrayList<String>();
        if (props.containsKey("certificateAttributes")) {
            String[] binaryAttrs = StringHelper.split(props.get("certificateAttributes").toString().toLowerCase(), ",");
            this.certificateAttributes.addAll(Arrays.asList(binaryAttrs));
        }
        LOG.debug("Using next binary certificateAttributes: " + this.certificateAttributes);

        this.supportedLDAPVersion = determineSupportedLdapVersion();
        this.subschemaSubentry = determineSubschemaSubentry();
        this.supportsSubtreeDeleteRequestControl = supportsSubtreeDeleteRequestControl();
        this.creationResultCode = ResultCode.SUCCESS;
    }

    private LDAPConnectionPool createConnectionPoolWithWaitImpl(Properties props, FailoverServerSet failoverSet, BindRequest bindRequest,
            LDAPConnectionOptions connectionOptions, int maxConnections, SSLUtil sslUtil) throws LDAPException {
        int connectionPoolMaxWaitTimeSeconds = StringHelper.toInt(props.getProperty("connection-pool-max-wait-time"), 30);
        LOG.debug("Using LDAP connection pool timeout: '" + connectionPoolMaxWaitTimeSeconds + "'");

        LDAPConnectionPool createdConnectionPool = null;
        LDAPException lastException = null;

        int attempt = 0;
        long currentTime = System.currentTimeMillis();
        long maxWaitTime = currentTime + connectionPoolMaxWaitTimeSeconds * 1000;
        do {
            attempt++;
            if (attempt > 0) {
                LOG.info("Attempting to create connection pool: " + attempt);
            }

            try {
                createdConnectionPool = createConnectionPoolImpl(failoverSet, bindRequest, connectionOptions, maxConnections, sslUtil);
                break;
            } catch (LDAPException ex) {
                if (ex.getResultCode().intValue() != ResultCode.CONNECT_ERROR_INT_VALUE) {
                    throw ex;
                }
                lastException = ex;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                LOG.error("Exception happened in sleep", ex);
                return null;
            }
            currentTime = System.currentTimeMillis();
        } while (maxWaitTime > currentTime);

        if ((createdConnectionPool == null) && (lastException != null)) {
            throw lastException;
        }

        return createdConnectionPool;
    }

    private LDAPConnectionPool createConnectionPoolImpl(FailoverServerSet failoverSet, BindRequest bindRequest,
            LDAPConnectionOptions connectionOptions, int maxConnections, SSLUtil sslUtil) throws LDAPException {

        LDAPConnectionPool createdConnectionPool;
        try {
            createdConnectionPool = new LDAPConnectionPool(failoverSet, bindRequest, maxConnections);
        } catch (LDAPException ex) {
            if (!this.useSSL) {
                throw ex;
            }

            // Error when LDAP server not supports specified encryption
            if (ex.getResultCode() != ResultCode.SERVER_DOWN) {
                throw ex;
            }

            LOG.info("Attempting to use older SSL protocols", ex);
            createdConnectionPool = createSSLConnectionPoolWithPreviousProtocols(sslUtil, bindRequest, connectionOptions, maxConnections);
            if (createdConnectionPool == null) {
                throw ex;
            }
        }

        return createdConnectionPool;
    }

    private LDAPConnectionPool createSSLConnectionPoolWithPreviousProtocols(SSLUtil sslUtil, BindRequest bindRequest,
            LDAPConnectionOptions connectionOptions, int maxConnections) throws LDAPException {
        for (int i = 1; i < SSL_PROTOCOLS.length; i++) {
            String protocol = SSL_PROTOCOLS[i];
            try {
                FailoverServerSet failoverSet = new FailoverServerSet(this.addresses, this.ports, sslUtil.createSSLSocketFactory(protocol),
                        connectionOptions);
                LDAPConnectionPool connectionPool = new LDAPConnectionPool(failoverSet, bindRequest, maxConnections);

                LOG.info("Server supports: '" + protocol + "'");
                return connectionPool;
            } catch (GeneralSecurityException ex) {
                LOG.debug("Server not supports: '" + protocol + "'", ex);
            } catch (LDAPException ex) {
                // Error when LDAP server not supports specified encryption
                if (ex.getResultCode() != ResultCode.SERVER_DOWN) {
                    throw ex;
                }
                LOG.debug("Server not supports: '" + protocol + "'", ex);
            }
        }

        return null;
    }

    private int determineSupportedLdapVersion() {
        int resultSupportedLDAPVersion = LdapConnectionProvider.DEFAULT_SUPPORTED_LDAP_VERSION;

        boolean validConnection = isValidConnection();
        if (!validConnection) {
            return resultSupportedLDAPVersion;
        }

        try {
            String[] supportedLDAPVersions = connectionPool.getRootDSE().getAttributeValues("supportedLDAPVersion");
            if (ArrayHelper.isEmpty(supportedLDAPVersions)) {
                return resultSupportedLDAPVersion;
            }

            for (String supportedLDAPVersion : supportedLDAPVersions) {
                resultSupportedLDAPVersion = Math.max(resultSupportedLDAPVersion, Integer.parseInt(supportedLDAPVersion));
            }
        } catch (Exception ex) {
            LOG.error("Failed to determine supportedLDAPVersion", ex);
        }

        return resultSupportedLDAPVersion;
    }

    private String determineSubschemaSubentry() {
        String resultSubschemaSubentry = LdapConnectionProvider.DEFAULT_SUBSCHEMA_SUBENTRY;

        boolean validConnection = isValidConnection();
        if (!validConnection) {
            return resultSubschemaSubentry;
        }

        try {
            String subschemaSubentry = connectionPool.getRootDSE().getAttributeValue("subschemaSubentry");
            if (StringHelper.isEmpty(subschemaSubentry)) {
                return resultSubschemaSubentry;
            }
            resultSubschemaSubentry = subschemaSubentry;
        } catch (Exception ex) {
            LOG.error("Failed to determine subschemaSubentry", ex);
        }

        return resultSubschemaSubentry;
    }

    private boolean supportsSubtreeDeleteRequestControl() {
        boolean supportsSubtreeDeleteRequestControl = false;

        boolean validConnection = isValidConnection();
        if (!validConnection) {
            return supportsSubtreeDeleteRequestControl;
        }

        try {
            supportsSubtreeDeleteRequestControl = connectionPool.getRootDSE()
                    .supportsControl(com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl.SUBTREE_DELETE_REQUEST_OID);
        } catch (Exception ex) {
            LOG.error("Failed to determine if LDAP server supports Subtree Delete Request Control", ex);
        }

        return supportsSubtreeDeleteRequestControl;
    }

    private boolean isValidConnection() {
        if (StringHelper.isEmptyString(bindDn) || StringHelper.isEmptyString(bindPassword)) {
            return false;
        }

        if (connectionPool == null) {
            return false;
        }

        return true;
    }

    public int getSupportedLDAPVersion() {
        return supportedLDAPVersion;
    }

    public String getSubschemaSubentry() {
        return subschemaSubentry;
    }

    public boolean isSupportsSubtreeDeleteRequestControl() {
        return supportsSubtreeDeleteRequestControl;
    }

    /**
     * This method is used to get LDAP connection from connectionPool if the
     * connection is not available it will return new connection
     *
     * @return LDAPConnection from the connectionPool
     * @throws LDAPException
     */
    public LDAPConnection getConnection() throws LDAPException {
        return connectionPool.getConnection();
    }

    /**
     * Use this method to get connection pool instance;
     *
     * @return LDAPConnectionPool
     * @throws LDAPException
     * @throws NumberFormatException
     */

    /**
     * Use this static method to release LDAPconnection to LDAPConnectionpool
     *
     * @param connection
     */
    public void releaseConnection(LDAPConnection connection) {
        connectionPool.releaseConnection(connection);
    }

    /**
     * This method to release back the connection after a exception occured
     *
     * @param connection
     * @param ex
     *            (LDAPException)
     */
    public void releaseConnection(LDAPConnection connection, LDAPException ex) {
        connectionPool.releaseConnectionAfterException(connection, ex);
    }

    /**
     * This method is used to release back a connection that is no longer been used
     * or fit to be used
     *
     * @param connection
     */
    public void closeDefunctConnection(LDAPConnection connection) {
        connectionPool.releaseDefunctConnection(connection);
    }

    public LDAPConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public void closeConnectionPool() {
        connectionPool.close();
    }

    public boolean isConnected() {
        if (connectionPool == null) {
            return false;
        }

        boolean isConnected = false;
        try {
            LDAPConnection connection = getConnection();
            try {
                isConnected = connection.isConnected();
            } finally {
                releaseConnection(connection);
            }
        } catch (LDAPException ex) {
        }

        return isConnected;
    }

    public ResultCode getCreationResultCode() {
        return creationResultCode;
    }

    public void setCreationResultCode(ResultCode creationResultCode) {
        this.creationResultCode = creationResultCode;
    }

    public boolean isCreated() {
        return ResultCode.SUCCESS == this.creationResultCode;
    }

    public String[] getServers() {
        return servers;
    }

    public String[] getAddresses() {
        return addresses;
    }

    public int[] getPorts() {
        return ports;
    }

    public String getBindDn() {
        return bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public final ArrayList<PasswordEncryptionMethod> getAdditionalPasswordMethods() {
        return additionalPasswordMethods;
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
        String realAttributeName = getCertificateAttributeName(attributeName);

        return certificateAttributes.contains(realAttributeName.toLowerCase());
    }

    public String getCertificateAttributeName(String attributeName) {
        if (StringHelper.isEmpty(attributeName)) {
            return attributeName;
        }

        if (attributeName.endsWith(";binary")) {
            return attributeName.substring(0, attributeName.length() - 7);
        }

        return attributeName;
    }

}
