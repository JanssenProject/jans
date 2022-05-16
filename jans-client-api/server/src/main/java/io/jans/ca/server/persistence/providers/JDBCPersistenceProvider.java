package io.jans.ca.server.persistence.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.jans.ca.common.Jackson2;
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.ca.server.persistence.configuration.JDBCConfiguration;
import io.jans.ca.server.persistence.service.MainPersistenceService;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
@ApplicationScoped
public class JDBCPersistenceProvider implements SqlPersistenceProvider {

    @Inject
    Logger logger;

    @Inject
    MainPersistenceService jansConfigurationService;
    private BasicDataSource dataSource = null;

    @Override
    public void onCreate() {
        try {
            JDBCConfiguration jdbcConfiguration = asJDBCConfiguration(jansConfigurationService.find());
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
            logger.error("Error in creating jdbc connection.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDestroy() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
            } catch (SQLException e) {
                logger.error("Failed to close JDBC dataSource.", e);
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public JDBCConfiguration asJDBCConfiguration(ApiAppConfiguration configuration) throws Exception {
        try {
            JsonNode node = configuration.getStorageConfiguration();
            if (node != null) {
                return Jackson2.createJsonMapper().treeToValue(node, JDBCConfiguration.class);
            }
            logger.error("JDBC Configuration not provided.");
            throw new Exception("JDBC configuration not provided in `client-api-server.yml`.");
        } catch (Exception e) {
            logger.error("Failed to parse JDBCConfiguration.", e);
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
