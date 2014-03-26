package org.gluu.site.ldap;

import java.security.GeneralSecurityException;
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
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

/**
 * @author Yuriy Movchan
 */
public class LDAPConnectionProvider {

	private static final Logger log = Logger.getLogger(LDAPConnectionProvider.class);

	public static final String bindPassword = "bindPassword";
	private static final int DEFAULT_SUPPORTED_LDAP_VERSION = 2;

	private LDAPConnectionPool connectionPool;
	
	private int supportedLDAPVersion = DEFAULT_SUPPORTED_LDAP_VERSION;

	@SuppressWarnings("unused")
	private LDAPConnectionProvider() {}

	public LDAPConnectionProvider(Properties props) {
		try {
			init(props);
		} catch (Exception ex) {
			log.error("Failed to create connection pool", ex);
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
		String[] servers = serverProp.split(",");
		String[] addresses = new String[servers.length];
		int[] ports = new int[servers.length];
		for (int i = 0; i < servers.length; i++) {
			String str = servers[i];
			addresses[i] = str.substring(0, str.indexOf(":")).trim();
			ports[i] = Integer.parseInt(str.substring(str.indexOf(":") + 1, str.length()));
		}

		BindRequest bindRequest = null;
		if (StringHelper.isEmpty(props.getProperty("bindDN"))) {
			bindRequest = new SimpleBindRequest();
		} else {
			bindRequest = new SimpleBindRequest(props.getProperty("bindDN"), props.getProperty(bindPassword));
		}

		LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();
		connectionOptions.setConnectTimeoutMillis(100 * 1000);
		connectionOptions.setAutoReconnect(true);

		boolean useSSL = Boolean.valueOf(props.getProperty("useSSL"));

		FailoverServerSet failoverSet;
		if (useSSL) {
			SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
			failoverSet = new FailoverServerSet(addresses, ports, sslUtil.createSSLSocketFactory(), connectionOptions);
		} else {
			failoverSet = new FailoverServerSet(addresses, ports, connectionOptions);
		}

		connectionPool = new LDAPConnectionPool(failoverSet, bindRequest, Integer.parseInt(props.getProperty("maxconnections")));
		if (connectionPool != null) {
			connectionPool.setCreateIfNecessary(true);
			String connectionMaxWaitTime = props.getProperty("connection-max-wait-time");
			if (StringHelper.isNotEmpty(connectionMaxWaitTime)) {
				connectionPool.setMaxWaitTimeMillis(Long.parseLong(connectionMaxWaitTime));
			}
		}
		
		this.supportedLDAPVersion = determineSupportedLdapVersion();
	}

	private int determineSupportedLdapVersion() {
		int resultSupportedLDAPVersion = LDAPConnectionProvider.DEFAULT_SUPPORTED_LDAP_VERSION;

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

}
