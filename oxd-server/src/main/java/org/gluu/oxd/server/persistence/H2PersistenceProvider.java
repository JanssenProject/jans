package org.gluu.oxd.server.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxd.server.Jackson2;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.service.ConfigurationService;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 16/04/2017
 */

public class H2PersistenceProvider implements SqlPersistenceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(H2PersistenceProvider.class);

    private ConfigurationService configurationService;
    private JdbcConnectionPool pool = null;

    @Inject
    public H2PersistenceProvider(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void onCreate() {
        H2Configuration h2Configuration = asH2Configuration(configurationService.getConfiguration());

        pool = JdbcConnectionPool.create("jdbc:h2:file:" + h2Configuration.getDbFileLocation(), "oxd", "oxd");
    }

    @Override
    public void onDestroy() {
        pool.dispose();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return pool.getConnection();
    }

    public static H2Configuration asH2Configuration(OxdServerConfiguration configuration) {
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
}
