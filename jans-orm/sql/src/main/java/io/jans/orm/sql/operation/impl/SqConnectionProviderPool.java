package io.jans.orm.sql.operation.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.exception.operation.ConfigurationException;
import io.jans.orm.exception.operation.ConnectionException;
import io.jans.orm.sql.model.ResultCode;
import io.jans.orm.util.PropertiesHelper;
import io.jans.orm.util.StringHelper;

public class SqConnectionProviderPool {

	private static final Logger LOG = LoggerFactory.getLogger(SqConnectionProviderPool.class);

	private static final String DRIVER_PROPERTIES_PREFIX = "connection.driver-property";

	protected Properties props;

	private String connectionUri;
	private Properties connectionProperties;

	private GenericObjectPoolConfig<PoolableConnection> objectPoolConfig;
	protected PoolingDataSource<PoolableConnection> poolingDataSource;

	protected int creationResultCode;

	protected SqConnectionProviderPool() {}

	public SqConnectionProviderPool(Properties props) {
		this.props = props;
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

			LOG.error("Failed to create connection pool with properties: '{}'. Exception: {}", clonedProperties, ex);
		}
	}

	protected void init() throws Exception {
		if (!props.containsKey("db.schema.name")) {
			throw new ConfigurationException("Property 'db.schema.name' is mandatory!");
		}

		if (!props.containsKey("connection.uri")) {
			throw new ConfigurationException("Property 'connection.uri' is mandatory!");
		}
		this.connectionUri = props.getProperty("connection.uri");

		Properties filteredDriverProperties = PropertiesHelper.findProperties(props, DRIVER_PROPERTIES_PREFIX, ".");
		this.connectionProperties = new Properties();
		for (Entry<Object, Object> driverPropertyEntry : filteredDriverProperties.entrySet()) {
			String key = StringHelper.toString(driverPropertyEntry.getKey()).substring(DRIVER_PROPERTIES_PREFIX.length() + 1);
			String value = StringHelper.toString(driverPropertyEntry.getValue());

			connectionProperties.put(key, value);
		}

		String userName = props.getProperty("auth.userName");
		String userPassword = props.getProperty("auth.userPassword");

		connectionProperties.setProperty("user", userName);
		connectionProperties.setProperty("password", userPassword);

		this.objectPoolConfig = new GenericObjectPoolConfig<>();

		Integer cpMaxTotal = StringHelper.toInteger(props.getProperty("connection.pool.max-total"), null);
		if (cpMaxTotal != null) {
			objectPoolConfig.setMaxTotal(cpMaxTotal);
		}

		Integer cpMaxIdle = StringHelper.toInteger(props.getProperty("connection.pool.max-idle"), null);
		if (cpMaxIdle != null) {
			objectPoolConfig.setMaxIdle(cpMaxIdle);
		}

		Integer cpMinIdle = StringHelper.toInteger(props.getProperty("connection.pool.min-idle"), null);
		if (cpMinIdle != null) {
			objectPoolConfig.setMinIdle(cpMinIdle);
		}

		Integer cpMaxWaitTimeMillis = StringHelper.toInteger(props.getProperty("connection.pool.max-wait-time-millis"),
				null);
		if (cpMaxWaitTimeMillis != null) {
			objectPoolConfig.setMaxWaitMillis(cpMaxWaitTimeMillis);
		}

		Integer cpMinEvictableIdleTimeMillis = StringHelper
				.toInteger(props.getProperty("connection.pool.min-evictable-idle-time-millis"), null);
		if (cpMaxWaitTimeMillis != null) {
			objectPoolConfig.setMinEvictableIdleTimeMillis(cpMinEvictableIdleTimeMillis);
		}

		Boolean testOnCreate = StringHelper.toBoolean(props.getProperty("connection.pool.test-on-create"), null);
		if (testOnCreate != null) {
			objectPoolConfig.setTestOnCreate(testOnCreate);
		}

		Boolean testOnReturn = StringHelper.toBoolean(props.getProperty("connection.pool.test-on-return"), null);
		if (testOnReturn != null) {
			objectPoolConfig.setTestOnReturn(testOnReturn);
		}

		openWithWaitImpl();

		this.creationResultCode = ResultCode.SUCCESS_INT_VALUE;

		LOG.info("Created connection pool");
	}

	private void openWithWaitImpl() throws Exception {
		long connectionMaxWaitTimeMillis = StringHelper.toLong(props.getProperty("connection.pool.create-max-wait-time-millis"), 30 * 1000L);
		LOG.debug("Using connection timeout: '{}'", connectionMaxWaitTimeMillis);

		Exception lastException = null;

		int attempt = 0;
		long currentTime = System.currentTimeMillis();
		long maxWaitTime = currentTime + connectionMaxWaitTimeMillis;
		do {
			attempt++;
			if (attempt > 0) {
				LOG.info("Attempting to create connection pool: '{}'", attempt);
			}

			try {
				open();
				if (isConnected()) {
					break;
				} else {
					LOG.info("Failed to connect to DB");
					destroy();
					throw new ConnectionException("Failed to create connection pool");
				}
			} catch (Exception ex) {
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

	private void open() throws ClassNotFoundException {
		preloadJdbcDriver();

		ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectionUri, connectionProperties);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
		ObjectPool<PoolableConnection> objectPool = new GenericObjectPool<>(poolableConnectionFactory, objectPoolConfig);
		poolableConnectionFactory.setPool(objectPool);

		this.poolingDataSource = new PoolingDataSource<>(objectPool);
	}

	public void preloadJdbcDriver() throws ClassNotFoundException {
		if (props.containsKey("jdbc.driver.class-name")) {
			Class.forName(props.getProperty("jdbc.driver.class-name"));
		}
	}

	public int getCreationResultCode() {
		return creationResultCode;
	}

	public boolean isCreated() {
		return ResultCode.SUCCESS_INT_VALUE == creationResultCode;
	}

	public boolean isConnected() {
		if (this.poolingDataSource == null) {
			return false;
		}

		boolean isConnected = true;
		try (Connection con = this.poolingDataSource.getConnection()) {
			return con.isValid(30);
		} catch (Exception ex) {
			LOG.error("Failed to check connection", ex);
			isConnected = false;
		}

		return isConnected;
	}

	public Connection getConnection() {
		try {
			return this.poolingDataSource.getConnection();
		} catch (SQLException ex) {
			throw new ConnectionException("Failed to get connection from pool", ex);
		}
	}

	public boolean destroy() {
		boolean result = true;
		if (this.poolingDataSource != null) {
			try {
				this.poolingDataSource.close();
			} catch (RuntimeException ex) {
				LOG.error("Failed to close connection pool", ex);
				result = false;
			} catch (SQLException ex) {
				LOG.error("Failed to close connection pool. Erorr code: '{}'", ex.getErrorCode(), ex);
				result = false;
			}
		}

		return result;
	}

}