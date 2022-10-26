/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.operation.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.Code;
import com.google.cloud.spanner.Type.StructField;

import io.jans.orm.cloud.spanner.model.ResultCode;
import io.jans.orm.cloud.spanner.model.TableMapping;
import io.jans.orm.exception.KeyConversionException;
import io.jans.orm.exception.MappingException;
import io.jans.orm.exception.operation.ConfigurationException;
import io.jans.orm.exception.operation.ConnectionException;
import io.jans.orm.exception.operation.PersistenceException;
import io.jans.orm.operation.auth.PasswordEncryptionMethod;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.PropertiesHelper;
import io.jans.orm.util.StringHelper;

/**
 * Perform connection pool initialization
 *
 * @author Yuriy Movchan Date: 04/14/2021
 */
public class SpannerConnectionProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SpannerConnectionProvider.class);
    
    private static final String QUERY_HEALTH_CHECK = "SELECT 1";
    private static final String QUERY_PARENT_TABLE =
    		"SELECT TABLE_NAME, PARENT_TABLE_NAME FROM information_schema.tables WHERE table_catalog = '' and table_schema = '' and parent_table_name is NOT NULL";
    private static final String QUERY_TABLE_SCHEMA =
    		"SELECT TABLE_NAME, COLUMN_NAME, SPANNER_TYPE, IS_NULLABLE FROM information_schema.columns WHERE table_catalog = '' and table_schema = ''";

    private static final String CLIENT_PROPERTIES_PREFIX = "connection.client-property";

    private Properties props;

    private Properties clientConnectionProperties;

    private int creationResultCode;

    private ArrayList<String> binaryAttributes, certificateAttributes;

    private PasswordEncryptionMethod passwordEncryptionMethod;

	private String connectionProject;
	private String connectionInstance;
	private String connectionDatabase;

	private String connectionEmulatorHost;
	
	private String connectionCredentialsFile;

	private long defaultMaximumResultSize;
	private long maximumResultDeleteSize;
	
	private Map<String, Map<String, StructField>> tableColumnsMap;
	private Map<String, Set<String>> tableNullableColumnsSet;
	private Map<String, Set<String>> tableChildAttributesMap;

	private DatabaseClient dbClient;
	private Spanner spanner;

    protected SpannerConnectionProvider() {
    }

    public SpannerConnectionProvider(Properties props) {
        this.props = props;
        this.tableColumnsMap = new HashMap<>();
        this.tableNullableColumnsSet = new HashMap<>();
        this.tableChildAttributesMap = new HashMap<>();
    }

    public void create() {
        try {
            init();
        } catch (Exception ex) {
            this.creationResultCode = ResultCode.OPERATIONS_ERROR_INT_VALUE;

            Properties clonedProperties = (Properties) props.clone();

            LOG.error("Failed to create connection with properties: '{}'. Exception: {}", clonedProperties, ex);
        }
    }

	protected void init() throws Exception {
        if (!props.containsKey("connection.project")) {
        	throw new ConfigurationException("Property 'connection.project' is mandatory!");
        }
        this.connectionProject = props.getProperty("connection.project");

        if (!props.containsKey("connection.instance")) {
        	throw new ConfigurationException("Property 'connection.instance' is mandatory!");
        }
        this.connectionInstance = props.getProperty("connection.instance");

        if (!props.containsKey("connection.database")) {
        	throw new ConfigurationException("Property 'connection.database' is mandatory!");
        }
        this.connectionDatabase = props.getProperty("connection.database");

        if (props.containsKey("connection.emulator-host")) {
            this.connectionEmulatorHost = props.getProperty("connection.emulator-host");
        }

		Properties filteredDriverProperties = PropertiesHelper.findProperties(props, CLIENT_PROPERTIES_PREFIX, ".");
        this.clientConnectionProperties = new Properties();
		for (Entry<Object, Object> driverPropertyEntry : filteredDriverProperties.entrySet()) {
			String key = StringHelper.toString(driverPropertyEntry.getKey()).substring(CLIENT_PROPERTIES_PREFIX.length() + 1);
			String value = StringHelper.toString(driverPropertyEntry.getValue());

			clientConnectionProperties.put(key, value);
		}

		if (props.containsKey("statement.limit.default-maximum-result-size")) {
            this.defaultMaximumResultSize = StringHelper.toLong(props.getProperty("statement.limit.default-maximum-result-size"), 1000);
        }

		if (props.containsKey("statement.limit.maximum-result-delete-size")) {
            this.maximumResultDeleteSize = StringHelper.toLong(props.getProperty("statement.limit.maximum-result-delete-size"), 10000);
        }

		this.connectionCredentialsFile = null;
        if (props.containsKey("connection.credentials-file")) {
        	this.connectionCredentialsFile = props.getProperty("connection.credentials-file");
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

        loadTableMetaData();

        this.creationResultCode = ResultCode.SUCCESS_INT_VALUE;
    }

    private void loadTableMetaData() {
        LOG.info("Scanning DB metadata...");

        long takes = System.currentTimeMillis();
        try (ResultSet resultSet = executeQuery(QUERY_PARENT_TABLE)) {
        	if (resultSet.next()) {
            	int tableNameIdx = resultSet.getColumnIndex("TABLE_NAME");
            	int parentTableNameIdx = resultSet.getColumnIndex("PARENT_TABLE_NAME");
	        	do {
        			String parentTableName = resultSet.getString(parentTableNameIdx);
        			String tableName = resultSet.getString(tableNameIdx);

        			Set<String> childAttributes;
	        		if (tableChildAttributesMap.containsKey(parentTableName)) {
	        			childAttributes = tableChildAttributesMap.get(parentTableName);
	        		} else {
	        			childAttributes = new HashSet<>();
	        			tableChildAttributesMap.put(parentTableName, childAttributes);
	        		}
	        		
	        		if (tableName.startsWith(parentTableName + "_")) {
	        			tableName = tableName.substring(parentTableName.length() + 1);
	        		}
	        		childAttributes.add(tableName);
	        	} while (resultSet.next());
        	}
        } catch (SpannerException ex) {
        	throw new ConnectionException("Failed to get database metadata", ex);
        }
        LOG.debug("Build child attributes map: '{}'.", tableChildAttributesMap);

        HashMap<String, Type> typeMap = buildSpannerTypesMap();

        try (ResultSet resultSet = executeQuery(QUERY_TABLE_SCHEMA)) {
        	if (resultSet.next()) {
            	int tableNameIdx = resultSet.getColumnIndex("TABLE_NAME");
            	int columnNameIdx = resultSet.getColumnIndex("COLUMN_NAME");
            	int spannerTypeIdx = resultSet.getColumnIndex("SPANNER_TYPE");
            	int isNullableIdx = resultSet.getColumnIndex("IS_NULLABLE");
	        	do {
        			String tableName = resultSet.getString(tableNameIdx);
        			String columnName = resultSet.getString(columnNameIdx);
        			String spannerType = resultSet.getString(spannerTypeIdx);
        			String isNullable = resultSet.getString(isNullableIdx);

	        		// Load table schema
        			Map<String, StructField> tableColumns;
	        		if (tableColumnsMap.containsKey(tableName)) {
	        			tableColumns = tableColumnsMap.get(tableName);
	        		} else {
	            		tableColumns = new HashMap<>();
	                	tableColumnsMap.put(tableName, tableColumns);
	        		}

	        		String comparebleType = toComparableType(spannerType);
	        		Type type = typeMap.get(comparebleType);
	        		if (type == null) {
	                	throw new ConnectionException(String.format("Failed to parse SPANNER_TYPE: '%s'", spannerType));
	        		}
	        		tableColumns.put(columnName.toLowerCase(), StructField.of(columnName, type));

	        		// Check if column nullable
	        		Set<String> nullableColumns;
	        		if (tableNullableColumnsSet.containsKey(tableName)) {
	        			nullableColumns = tableNullableColumnsSet.get(tableName);
	        		} else {
	        			nullableColumns = new HashSet<>();
	        			tableNullableColumnsSet.put(tableName, nullableColumns);
	        		}
	        		
	        		boolean nullable = "yes".equalsIgnoreCase(isNullable);
	        		if (nullable) {
	        			nullableColumns.add(columnName.toLowerCase());
	        		}
	        	} while (resultSet.next());
        	}
        } catch (SpannerException ex) {
        	throw new ConnectionException("Failed to get database metadata", ex);
        }
        LOG.debug("Build table columns map: '{}'.", tableColumnsMap);

        takes = System.currentTimeMillis() - takes;
        LOG.info("Metadata scan finisehd in {} milliseconds", takes);
   	}

	private HashMap<String, Type> buildSpannerTypesMap() {
    	HashMap<String, Type> typeMap = new HashMap<>();
    	
    	// We have to add all types manually because Type is not enum and there is no method to get them all
    	addSpannerType(typeMap, Type.bool());
    	addSpannerType(typeMap, Type.int64());
    	addSpannerType(typeMap, Type.numeric());
    	addSpannerType(typeMap, Type.float64());
    	addSpannerType(typeMap, Type.string());
    	addSpannerType(typeMap, Type.bytes());
    	addSpannerType(typeMap, Type.timestamp());
    	addSpannerType(typeMap, Type.date());

    	return typeMap;
	}

    private static String toComparableType(String spannerType) {
    	int idx = spannerType.indexOf("(");
    	if (idx != -1) {
    		spannerType = spannerType.substring(0, idx);
    	}

    	idx = spannerType.indexOf(">");
    	if (idx == -1) {
    		return spannerType.toLowerCase();
    	}
    	
    	return spannerType.substring(0, idx).toLowerCase();
	}

	private void addSpannerType(HashMap<String, Type> typeMap, Type type) {
		typeMap.put(type.toString().toLowerCase(), type);
		typeMap.put(Code.ARRAY.name().toLowerCase()  + "<" + type.toString().toLowerCase(), Type.array(type));
	}

	private void openWithWaitImpl() throws Exception {
    	long connectionMaxWaitTimeMillis = StringHelper.toLong(props.getProperty("connection.client.create-max-wait-time-millis"), 30 * 1000L);
        LOG.debug("Using connection timeout: '{}'", connectionMaxWaitTimeMillis);

        Exception lastException = null;

        int attempt = 0;
        long currentTime = System.currentTimeMillis();
        long maxWaitTime = currentTime + connectionMaxWaitTimeMillis;
        do {
            attempt++;
            if (attempt > 0) {
                LOG.info("Attempting to create client connection: '{}'", attempt);
            }

            try {
                open();
                if (isConnected()) {
                	break;
                } else {
                    LOG.info("Failed to connect to Spanner");
                    destroy();
                    throw new ConnectionException("Failed to create client connection");
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

    private void open() throws FileNotFoundException, IOException {
        SpannerOptions.Builder optionsBuilder = SpannerOptions.newBuilder();
        if (StringHelper.isNotEmpty(connectionEmulatorHost)) {
        	optionsBuilder.setEmulatorHost(connectionEmulatorHost);
        }

        if (StringHelper.isNotEmpty(connectionCredentialsFile)) {
        	optionsBuilder.setCredentials(GoogleCredentials.fromStream(new FileInputStream(connectionCredentialsFile)));
        } else {
        	optionsBuilder.setCredentials(NoCredentials.getInstance());
        }

        optionsBuilder.setProjectId(connectionProject);

        DatabaseId databaseId = DatabaseId.of(connectionProject, connectionInstance, connectionDatabase);

        this.spanner = optionsBuilder.build().getService();
        this.dbClient = spanner.getDatabaseClient(databaseId);
    }

	public boolean destroy() {
		boolean result = true;
		if (this.spanner != null) {
			try {
				this.spanner.close();
			} catch (RuntimeException ex) {
				LOG.error("Failed to close spanner instance", ex);
				result = false;
			}
		}

		return result;
	}

    public boolean isConnected() {
        if (this.dbClient == null) {
            return false;
        }

        boolean isConnected = true;
        try (ResultSet resultSet = executeQuery(QUERY_HEALTH_CHECK)) {
        	return resultSet.next();
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

	public TableMapping getTableMappingByKey(String key, String objectClass, String tableName) {
		if (!tableColumnsMap.containsKey(tableName)) {
			throw new MappingException(String.format("Table '%s' is not exists in metadata'", tableName));
		}

		Map<String, StructField> columTypes = tableColumnsMap.get(tableName);

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

	public TableMapping getTableMappingByKey(String key, String objectClass) {
		return getTableMappingByKey(key, objectClass, objectClass);
	}

	 public TableMapping getChildTableMappingByKey(String key, TableMapping tableMapping, String columnName) {
		String childTableName = tableMapping.getTableName() + "_" + columnName;

		if (!tableColumnsMap.containsKey(childTableName)) {
			return null;
		}

		TableMapping childTableMapping = getTableMappingByKey(key, tableMapping.getObjectClass(), childTableName);
			
		return childTableMapping;
	}

	public Set<String> getTableChildAttributes(String objectClass) {
		return tableChildAttributesMap.get(objectClass);
	}

	public Map<String, TableMapping> getChildTablesMapping(String key, TableMapping tableMapping) {
		Set<String> childAttributes = tableChildAttributesMap.get(tableMapping.getObjectClass());
		if (childAttributes == null) {
			return null;
		}
		
		Map<String, TableMapping> childTableMapping = new HashMap<>();
		for (String childAttribute : childAttributes) {
			TableMapping childColumTypes = getChildTableMappingByKey(key, tableMapping, childAttribute);
			if (childColumTypes == null) {
				String childTableName = tableMapping.getTableName() + "_" + childAttribute;
				throw new MappingException(String.format("Table '%s' is not exists in metadata'", childTableName));
			}
			childTableMapping.put(childAttribute.toLowerCase(), childColumTypes);
		}
		
		return childTableMapping;
	}

	public Set<String> getTableNullableColumns(String objectClass) {
		return tableNullableColumnsSet.get(objectClass);
	}

	public DatabaseClient getClient() {
		return dbClient;
	}

	private ResultSet executeQuery(String sql) {
		return this.dbClient.singleUse().executeQuery(Statement.of(sql));
	}

	public Map<String, Map<String, StructField>> getDatabaseMetaData() {
		return tableColumnsMap;
	}

	public long getDefaultMaximumResultSize() {
		return defaultMaximumResultSize;
	}

	public long getMaximumResultDeleteSize() {
		return maximumResultDeleteSize;
	}

}
