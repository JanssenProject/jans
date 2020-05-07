package org.gluu.oxd.server.persistence.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.apache.commons.dbcp.BasicDataSource;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.persistence.configuration.JDBCConfiguration;
import org.gluu.oxd.server.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class JDBCPersistenceProvider implements SqlPersistenceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCPersistenceProvider.class);

    private ConfigurationService configurationService;
    private BasicDataSource dataSource = null;

    public JDBCPersistenceProvider(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void onCreate() {
        try {
            JDBCConfiguration jdbcConfiguration = asJDBCConfiguration(configurationService.getConfiguration());
            validate(jdbcConfiguration);

            dataSource = new BasicDataSource();
            dataSource.setDriverClassName(jdbcConfiguration.getDriver());
            dataSource.setUrl(jdbcConfiguration.getJdbcUrl());
            dataSource.setUsername(jdbcConfiguration.getUsername());
            dataSource.setPassword(jdbcConfiguration.getPassword());

            dataSource.setMinIdle(5);
            dataSource.setMaxIdle(10);
            dataSource.setMaxOpenPreparedStatements(100);
        } catch (Exception e) {
            LOG.error("Error in creating jdbc connection.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDestroy() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
            } catch (SQLException e) {
                LOG.error("Failed to close JDBC dataSource.", e);
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static JDBCConfiguration asJDBCConfiguration(OxdServerConfiguration configuration) throws Exception {
        try {
            JsonNode node = configuration.getStorageConfiguration();
            if (node != null) {
                return Jackson2.createJsonMapper().treeToValue(node, JDBCConfiguration.class);
            }
            LOG.error("JDBC Configuration not provided.");
            throw new Exception("JDBC configuration not provided in `oxd-server.yml`.");
        } catch (Exception e) {
            LOG.error("Failed to parse JDBCConfiguration.", e);
            throw e;
        }
    }

    private void validate(JDBCConfiguration jdbcConfiguration) throws Exception {
        if (Strings.isNullOrEmpty(jdbcConfiguration.getDriver())) {
            throw new Exception("JDBC connection driver not provided.");
        }
        if (Strings.isNullOrEmpty(jdbcConfiguration.getJdbcUrl())) {
            throw new Exception("JDBC connection url not provided.");
        }
        if (Strings.isNullOrEmpty(jdbcConfiguration.getUsername())) {
            throw new Exception("JDBC connection username not provided.");
        }
    }
}
