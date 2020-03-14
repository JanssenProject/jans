package org.gluu.oxd.server.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.apache.commons.dbcp.BasicDataSource;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class JDBCPersistenceProvider implements SqlPersistenceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCPersistenceProvider.class);

    private ConfigurationService configurationService;
    private static BasicDataSource dataSource = null;

    public JDBCPersistenceProvider(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void onCreate() {
        JDBCConfiguration jdbcConfiguration = asJDBCConfiguration(configurationService.getConfiguration());
        validate(jdbcConfiguration);
        if (dataSource == null) {
            dataSource = new BasicDataSource();
            dataSource.setDriverClassName(jdbcConfiguration.getDriver());
            dataSource.setUrl(jdbcConfiguration.getJdbcUrl());
            dataSource.setUsername(jdbcConfiguration.getUsername());
            dataSource.setPassword(jdbcConfiguration.getPassword());

            dataSource.setMinIdle(5);
            dataSource.setMaxIdle(10);
            dataSource.setMaxOpenPreparedStatements(100);
        }
    }

    @Override
    public void onDestroy() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
            } catch (SQLException e) {
                LOG.error("Failed to cloase JDBC dataSource.", e);
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static JDBCConfiguration asJDBCConfiguration(OxdServerConfiguration configuration) {
        try {
            JsonNode node = configuration.getStorageConfiguration();
            if (node != null) {
                return Jackson2.createJsonMapper().treeToValue(node, JDBCConfiguration.class);
            }
        } catch (Exception e) {
            LOG.error("Failed to parse JDBCConfiguration.", e);
        }
        return new JDBCConfiguration();
    }

    private void validate(JDBCConfiguration jdbcConfiguration) {
        if (Strings.isNullOrEmpty(jdbcConfiguration.getDriver())) {
            throw new HttpException(ErrorResponseCode.NO_JDBC_CONNECTION_DRIVER);
        }
        if (Strings.isNullOrEmpty(jdbcConfiguration.getJdbcUrl())) {
            throw new HttpException(ErrorResponseCode.NO_JDBC_CONNECTION_URL);
        }
        if (Strings.isNullOrEmpty(jdbcConfiguration.getUsername())) {
            throw new HttpException(ErrorResponseCode.NO_JDBC_CONNECTION_USERNAME);
        }
    }
}
