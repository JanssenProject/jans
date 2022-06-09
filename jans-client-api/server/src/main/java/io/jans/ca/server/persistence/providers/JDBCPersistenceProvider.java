package io.jans.ca.server.persistence.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import static io.jans.ca.server.configuration.ConfigurationFactory.CONFIGURATION_ENTRY_DN;

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
            logger.error("Error creating jdbc connection." + e.getMessage(), e);
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

    public JDBCConfiguration asJDBCConfiguration(ApiAppConfiguration configuration) throws IllegalArgumentException, JsonProcessingException {
        JsonNode node = configuration.getStorageConfiguration();
        if (node != null) {
            return Jackson2.createJsonMapper().treeToValue(node, JDBCConfiguration.class);
        }
        logger.error("JDBC Configuration not provided, check configuration: {}", CONFIGURATION_ENTRY_DN);
        return null;
    }

    private boolean validate(JDBCConfiguration jdbcConfiguration) {
        if (jdbcConfiguration == null) {
            logger.error("JDBC connection driver null or not provided.");
            return false;
        }
        if (Strings.isNullOrEmpty(jdbcConfiguration.getDriver())) {
            logger.error("JDBC connection driver not provided.");
            return false;
        }
        if (Strings.isNullOrEmpty(jdbcConfiguration.getJdbcUrl())) {
            logger.error("JDBC connection url not provided.");
            return false;
        }
        if (Strings.isNullOrEmpty(jdbcConfiguration.getUsername())) {
            logger.error("JDBC connection username not provided.");
            return false;
        }
        return true;
    }
}
