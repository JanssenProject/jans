/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.xdi.util.ArrayHelper;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter.EncryptionException;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.FailoverServerSet;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

/**
 * @author Yuriy Movchan
 */
public class LDAPConnectionProvider {

	private static final Logger log = Logger.getLogger(LDAPConnectionProvider.class);

	private static final int DEFAULT_SUPPORTED_LDAP_VERSION = 2;

	private static final String[] SSL_PROTOCOLS = { "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3" };

	private LDAPConnectionPool connectionPool;
	private ResultCode creationResultCode;
	
	private int supportedLDAPVersion = DEFAULT_SUPPORTED_LDAP_VERSION;

	private String[] servers;
	private String[] addresses;
	private int[] ports;

	private String bindDn;
	private String bindPassword;
	private boolean useSSL;

	private ArrayList<String> binaryAttributes;

	@SuppressWarnings("unused")
	private LDAPConnectionProvider() {}

	public LDAPConnectionProvider(Properties props) {
		try {
			init(props);
		} catch (LDAPException ex) {
			creationResultCode = ex.getResultCode();
			log.error("Failed to create connection pool with properties: " + props, ex);
		} catch (Exception ex) {
			log.error("Failed to create connection pool with properties: " + props, ex);
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
	public void init(Properties props) throws NumberFormatException, LDAPException, GeneralSecurityException {
		String serverProp = props.getProperty("servers");
		this.servers = serverProp.split(",");
		this.addresses = new String[this.servers.length];
		this.ports = new int[this.servers.length];
		for (int i = 0; i < this.servers.length; i++) {
			String str = this.servers[i];
			this.addresses[i] = str.substring(0, str.indexOf(":")).trim();
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
		connectionOptions.setAutoReconnect(true);

		this.useSSL = Boolean.valueOf(props.getProperty("useSSL")).booleanValue();
		SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());

		FailoverServerSet failoverSet;
		if (this.useSSL) {
			failoverSet = new FailoverServerSet(this.addresses, this.ports, sslUtil.createSSLSocketFactory(SSL_PROTOCOLS[0]), connectionOptions);
		} else {
			failoverSet = new FailoverServerSet(this.addresses, this.ports, connectionOptions);
		}

		int maxConnections = Integer.parseInt(props.getProperty("maxconnections"));
		this.connectionPool = createConnectionPoolWithWaitImpl(props, failoverSet, bindRequest, connectionOptions, maxConnections, sslUtil);
		if (this.connectionPool != null) {
			this.connectionPool.setCreateIfNecessary(true);
			String connectionMaxWaitTime = props.getProperty("connection-max-wait-time");
			if (StringHelper.isNotEmpty(connectionMaxWaitTime)) {
				this.connectionPool.setMaxWaitTimeMillis(Long.parseLong(connectionMaxWaitTime));
			}
		}
		
		this.binaryAttributes = new ArrayList<String>();
		if (props.containsKey("binaryAttributes")) {
			String[] binaryAttrs = StringHelper.split(props.get("binaryAttributes").toString().toLowerCase(), ",");
			this.binaryAttributes.addAll(Arrays.asList(binaryAttrs));
		}
		log.debug("Using next binary attributes: " + this.binaryAttributes);
		
		
		this.supportedLDAPVersion = determineSupportedLdapVersion();
		this.creationResultCode = ResultCode.SUCCESS;
	}
	private LDAPConnectionPool createConnectionPoolWithWaitImpl(Properties props, FailoverServerSet failoverSet, BindRequest bindRequest, LDAPConnectionOptions connectionOptions,
			int maxConnections, SSLUtil sslUtil) throws LDAPException {		
		String connectionPoolMaxWaitTime = props.getProperty("connection-pool-max-wait-time");
		int connectionPoolMaxWaitTimeSeconds = 30;
		if (StringHelper.isNotEmpty(connectionPoolMaxWaitTime)) {
			connectionPoolMaxWaitTimeSeconds = Integer.parseInt(connectionPoolMaxWaitTime);
		}
		log.debug("Using LDAP connection pool timeout: '" + connectionPoolMaxWaitTimeSeconds + "'");

		LDAPConnectionPool createdConnectionPool = null;

		int attempt = 0;
		long currentTime = System.currentTimeMillis();
		long maxWaitTime = currentTime + connectionPoolMaxWaitTimeSeconds * 1000;
		do {
			attempt++;
			if (attempt > 0) {
				log.info("Attempting to create connection pool: " + attempt);
			}

			try {
				createdConnectionPool = createConnectionPoolImpl(failoverSet, bindRequest, connectionOptions, maxConnections, sslUtil);
				break;
			} catch (LDAPException ex) {
				if (ex.getResultCode().intValue() != ResultCode.CONNECT_ERROR_INT_VALUE) {
					throw ex;
				}
			}
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ex) {
				log.error("Exception happened in sleep", ex);
				return null;
			}
			currentTime = System.currentTimeMillis();
		} while (maxWaitTime > currentTime);
		
		return createdConnectionPool;
	}

	private LDAPConnectionPool createConnectionPoolImpl(FailoverServerSet failoverSet, BindRequest bindRequest, LDAPConnectionOptions connectionOptions,
			int maxConnections, SSLUtil sslUtil) throws LDAPException {

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
			
			log.info("Attempting to use older SSL protocols", ex);
			createdConnectionPool = createSSLConnectionPoolWithPreviousProtocols(sslUtil, bindRequest, connectionOptions, maxConnections);
			if (createdConnectionPool == null) {
				throw ex;
			}
		}
		
		return createdConnectionPool;
	}

	private LDAPConnectionPool createSSLConnectionPoolWithPreviousProtocols(SSLUtil sslUtil, BindRequest bindRequest, LDAPConnectionOptions connectionOptions, int maxConnections) throws LDAPException {
		for (int i = 1; i < SSL_PROTOCOLS.length; i++) {
			String protocol = SSL_PROTOCOLS[i];
			try {
				FailoverServerSet failoverSet = new FailoverServerSet(this.addresses, this.ports, sslUtil.createSSLSocketFactory(protocol), connectionOptions);
				LDAPConnectionPool connectionPool = new LDAPConnectionPool(failoverSet, bindRequest, maxConnections);
				
				log.info("Server supports: '" + protocol + "'");
				return connectionPool;
			} catch (GeneralSecurityException ex) {
				log.debug("Server not supports: '" + protocol + "'", ex);
			} catch (LDAPException ex) {
				// Error when LDAP server not supports specified encryption
				if (ex.getResultCode() != ResultCode.SERVER_DOWN) {
					throw ex;
				}
				log.debug("Server not supports: '" + protocol + "'", ex);
			}
		}

		return null;
	}

	private int determineSupportedLdapVersion() {
		int resultSupportedLDAPVersion = LDAPConnectionProvider.DEFAULT_SUPPORTED_LDAP_VERSION;
		
		if (StringHelper.isEmptyString(bindDn) || StringHelper.isEmptyString(bindPassword)) {
			return resultSupportedLDAPVersion;
		}

		if (connectionPool == null) {
			return resultSupportedLDAPVersion;
		}
		try {
			String supportedLDAPVersions[] = connectionPool.getRootDSE().getAttributeValues("supportedLDAPVersion");
			if (ArrayHelper.isEmpty(supportedLDAPVersions)) {
				return resultSupportedLDAPVersion;
			}

			for (String supportedLDAPVersion : supportedLDAPVersions) {
				resultSupportedLDAPVersion = Math.max(resultSupportedLDAPVersion, Integer.parseInt(supportedLDAPVersion));
			}
		} catch (Exception ex) {
			log.error("Failed to determine supportedLDAPVersion", ex);
		}

		return resultSupportedLDAPVersion;
	}

	public int getSupportedLDAPVersion() {
		return supportedLDAPVersion;
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
	 * This method is used to release back a connection that is no longer been
	 * used or fit to be used
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

	public ArrayList<String> getBinaryAttributes() {
		return binaryAttributes;
	}

	public boolean isBinaryAttribute(String attributeName) {
		if (StringHelper.isEmpty(attributeName)) {
			return false;
		}

		return binaryAttributes.contains(attributeName.toLowerCase());
	}

}
