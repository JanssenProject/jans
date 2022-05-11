package io.jans.ca.server.persistence.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.jans.ca.common.Jackson2;
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.ca.server.persistence.configuration.H2Configuration;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 16/04/2017
 */

public class H2PersistenceProvider implements SqlPersistenceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(H2PersistenceProvider.class);

    private ApiAppConfiguration configuration;
    private JdbcConnectionPool pool = null;

    public H2PersistenceProvider(ApiAppConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void onCreate() {
        H2Configuration h2Configuration = asH2Configuration(configuration);
        setDefaultUsernamePasswordIfEmpty(h2Configuration);
        pool = JdbcConnectionPool.create("jdbc:h2:file:" + h2Configuration.getDbFileLocation(), h2Configuration.getUsername(), h2Configuration.getPassword());
    }

    @Override
    public void onDestroy() {
        pool.dispose();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return pool.getConnection();
    }

    public static H2Configuration asH2Configuration(ApiAppConfiguration configuration) {
        try {
            JsonNode node = configuration.getStorageConfiguration();
            if (node != null) {
                return Jackson2.createJsonMapper().treeToValue(node, H2Configuration.class);
            }
        } catch (Exception e) {
            LOG.error("Failed to parse H2Configuration.", e);
        }
        return new H2Configuration();
    }

    public void setDefaultUsernamePasswordIfEmpty(H2Configuration h2Configuration) {
        if (Strings.isNullOrEmpty(h2Configuration.getUsername())) {
            h2Configuration.setUsername("oxd");
        }

        if (Strings.isNullOrEmpty(h2Configuration.getPassword())) {
            h2Configuration.setPassword("oxd");
        }
    }
}
