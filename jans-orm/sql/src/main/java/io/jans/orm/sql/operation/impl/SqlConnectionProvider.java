/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.operation.impl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.SQLTemplatesRegistry;

import io.jans.orm.exception.KeyConversionException;
import io.jans.orm.exception.operation.ConfigurationException;
import io.jans.orm.exception.operation.ConnectionException;
import io.jans.orm.model.AttributeType;
import io.jans.orm.operation.auth.PasswordEncryptionMethod;
import io.jans.orm.sql.dsl.template.MySQLJsonTemplates;
import io.jans.orm.sql.dsl.template.PostgreSQLJsonTemplates;
import io.jans.orm.sql.model.ResultCode;
import io.jans.orm.sql.model.TableMapping;
import io.jans.orm.sql.operation.SqlOperationService;
import io.jans.orm.sql.operation.SupportedDbType;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.PropertiesHelper;
import io.jans.orm.util.StringHelper;

/**
 * Perform connection pool initialization
 *
 * @author Yuriy Movchan Date: 12/18/2020
 */
public class SqlConnectionProvider {

	private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private static final String MYSQL_QUERY_ENGINE_TYPE =
    		"SELECT TABLE_NAME, ENGINE FROM information_schema.tables WHERE table_schema = ?";

    private static final String DRIVER_PROPERTIES_PREFIX = "connection.driver-property";

    private Properties props;

    private String connectionUri;
    private Properties connectionProperties;

    private GenericObjectPoolConfig<PoolableConnection> objectPoolConfig;
    private PoolingDataSource<PoolableConnection> poolingDataSource;

    private int creationResultCode;

    private ArrayList<String> binaryAttributes, certificateAttributes;

    private PasswordEncryptionMethod passwordEncryptionMethod;

	private SupportedDbType dbType;
	private String dbVersion;

	private boolean mariaDb = false;

	private String schemaName;

	private SQLTemplates sqlTemplates;

	private SQLQueryFactory sqlQueryFactory;
	
	private Map<String, Map<String, AttributeType>> tableColumnsMap;
	private Map<String, String> tableEnginesMap = new HashMap<>();


    protected SqlConnectionProvider() {
    }

    public SqlConnectionProvider(Properties props) {
        this.props = props;
        this.tableColumnsMap = new HashMap<>();
    }

    public void create() {
        try {
            init();
            initDsl();
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
        this.schemaName = props.getProperty("db.schema.name");

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

        Integer cpMaxWaitTimeMillis = StringHelper.toInteger(props.getProperty("connection.pool.max-wait-time-millis"), null);
        if (cpMaxWaitTimeMillis != null) {
        	objectPoolConfig.setMaxWaitMillis(cpMaxWaitTimeMillis);
        }

        Integer cpMinEvictableIdleTimeMillis = StringHelper.toInteger(props.getProperty("connection.pool.min-evictable-idle-time-millis"), null);
        if (cpMaxWaitTimeMillis != null) {
        	objectPoolConfig.setMinEvictableIdleTimeMillis(cpMinEvictableIdleTimeMillis);
        }

        openWithWaitImpl();
        LOG.info("Created connection pool");

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

        try (Connection con = this.poolingDataSource.getConnection()) {
        	DatabaseMetaData databaseMetaData = con.getMetaData();
        	String dbTypeString = databaseMetaData.getDatabaseProductName();
        	this.dbType = SupportedDbType.resolveDbType(dbTypeString.toLowerCase());
        	
        	if (this.dbType == null) {
                throw new ConnectionException(String.format("Database type '%s' is not supported", dbTypeString));
        	}
        	
        	this.dbVersion = databaseMetaData.getDatabaseProductVersion().toLowerCase();
        	if ((this.dbVersion != null) && this.dbVersion.toLowerCase().contains("mariadb")) {
        		this.mariaDb = true;
        	}
            LOG.debug("Database product name: '{}'", dbType);
            loadTableMetaData(databaseMetaData, con);
        } catch (Exception ex) {
            throw new ConnectionException("Failed to detect database product name and load metadata", ex);
        }

        this.creationResultCode = ResultCode.SUCCESS_INT_VALUE;
    }

    private void loadTableMetaData(DatabaseMetaData databaseMetaData, Connection con) throws SQLException {
        long takes = System.currentTimeMillis();

        if (SupportedDbType.MYSQL == dbType) {
	        LOG.info("Detecting engine types...");
	    	PreparedStatement preparedStatement = con.prepareStatement(MYSQL_QUERY_ENGINE_TYPE);
	    	preparedStatement.setString(1, schemaName);
	
	    	try (ResultSet tableEnginesResultSet = preparedStatement.executeQuery()) {
		    	while (tableEnginesResultSet.next()) {
		    		String tableName = tableEnginesResultSet.getString("TABLE_NAME");
		    		String engineName = tableEnginesResultSet.getString("ENGINE");
		
		        	tableEnginesMap.put(tableName, engineName);
		    	}
	    	}
        }
	
        LOG.info("Scanning DB metadata...");
        try (ResultSet tableResultSet = databaseMetaData.getTables(null, schemaName, null, new String[]{"TABLE"})) {
	    	while (tableResultSet.next()) {
	    		String tableName = tableResultSet.getString("TABLE_NAME");
	    		Map<String, AttributeType> tableColumns = new HashMap<>();
	    		
	//    		String engineType = tableEnginesMap.get(tableName);
	    		
	            LOG.debug("Found table: '{}'.", tableName);
	            try (ResultSet columnResultSet = databaseMetaData.getColumns(null, schemaName, tableName, null)) {
		        	while (columnResultSet.next()) {
		        		String columnName = columnResultSet.getString("COLUMN_NAME").toLowerCase();
						String columnTypeName = columnResultSet.getString("TYPE_NAME").toLowerCase();
		
						String remark = columnResultSet.getString("REMARKS");
		        		if (mariaDb && SqlOperationService.LONGTEXT_TYPE_NAME.equalsIgnoreCase(columnTypeName) && SqlOperationService.JSON_TYPE_NAME.equalsIgnoreCase(remark)) {
		        			columnTypeName = SqlOperationService.JSON_TYPE_NAME;
		        		}

		        		if (SqlOperationService.JSONB_TYPE_NAME.equalsIgnoreCase(columnTypeName)) {
		        			columnTypeName = SqlOperationService.JSONB_TYPE_NAME;
		        		}
		
		        		boolean multiValued = SqlOperationService.JSON_TYPE_NAME.equals(columnTypeName) || SqlOperationService.JSONB_TYPE_NAME.equals(columnTypeName);
		
		        		AttributeType attributeType = new AttributeType(columnName, columnTypeName, multiValued);
		        		tableColumns.put(columnName, attributeType);
		        	}
	            }
	
	        	tableColumnsMap.put(StringHelper.toLowerCase(tableName), tableColumns);
	    	}
        }

    	takes = System.currentTimeMillis() - takes;
        LOG.info("Metadata scan finisehd in {} milliseconds", takes);
   	}

	private void initDsl() throws SQLException {
		SQLTemplatesRegistry templatesRegistry = new SQLTemplatesRegistry();
		try (Connection con = poolingDataSource.getConnection()) {
			DatabaseMetaData databaseMetaData = con.getMetaData();
			SQLTemplates.Builder sqlBuilder = templatesRegistry.getBuilder(databaseMetaData);
			if (SupportedDbType.MYSQL == dbType) {
				sqlBuilder = MySQLJsonTemplates.builder();
			} else if (SupportedDbType.POSTGRESQL == dbType) {
				sqlBuilder = PostgreSQLJsonTemplates.builder().quote();
			}
			this.sqlTemplates = sqlBuilder.printSchema().build();
			Configuration configuration = new Configuration(sqlTemplates);

			this.sqlQueryFactory = new SQLQueryFactory(configuration, poolingDataSource);
		}
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

    private void open() {
		ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectionUri, connectionProperties);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
		ObjectPool<PoolableConnection> objectPool = new GenericObjectPool<>(poolableConnectionFactory, objectPoolConfig);

		this.poolingDataSource = new PoolingDataSource<>(objectPool);
		poolableConnectionFactory.setPool(objectPool);
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

    public int getCreationResultCode() {
        return creationResultCode;
    }

    public boolean isCreated() {
        return ResultCode.SUCCESS_INT_VALUE == creationResultCode;
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

	public String getSchemaName() {
		return schemaName;
	}

	public SQLQueryFactory getSqlQueryFactory() {
		return sqlQueryFactory;
	}

	public TableMapping getTableMappingByKey(String key, String objectClass) {
		String tableName = objectClass;
		Map<String, AttributeType> columTypes = tableColumnsMap.get(StringHelper.toLowerCase(tableName));
		if ("_".equals(key)) {
			return new TableMapping("", tableName, objectClass, columTypes);
		}

		String[] baseNameParts = key.split("_");
		if (ArrayHelper.isEmpty(baseNameParts)) {
			throw new KeyConversionException("Failed to determine base key part!");
		}

		TableMapping tableMapping = new TableMapping(baseNameParts[0], tableName, objectClass, columTypes);
		
		return tableMapping;
	}

	public String getEngineType(String objectClass) {
		String tableName = objectClass;

		return tableEnginesMap.get(tableName);
	}

	public Connection getConnection() {
        try {
			return this.poolingDataSource.getConnection();
		} catch (SQLException ex) {
            throw new ConnectionException("Failed to get connection from pool", ex);
		}
	}

	public DatabaseMetaData getDatabaseMetaData() {
        try (Connection con = this.poolingDataSource.getConnection()) {
        	DatabaseMetaData databaseMetaData = con.getMetaData();
        	return databaseMetaData;
        } catch (SQLException ex) {
        	throw new ConnectionException("Failed to get database metadata", ex);
        }
	}

	public boolean isMariaDb() {
		return mariaDb;
	}

	public SupportedDbType getDbType() {
		return dbType;
	}

}
