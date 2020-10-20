package io.jans.ca.server.persistence.providers;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.PersistenceConfigKeys;
import io.jans.ca.server.OxdServerConfiguration;
import io.jans.ca.server.Utils;
import io.jans.ca.server.persistence.configuration.GluuConfiguration;
import io.jans.util.exception.ConfigurationException;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Properties;

public class GluuPersistenceConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCPersistenceProvider.class);

    private OxdServerConfiguration configuration;
    private Properties connectionProperties;

    public GluuPersistenceConfiguration(OxdServerConfiguration configuration) {
        this.configuration = configuration;
    }

    public Properties getPersistenceProps() {
        try {
            Optional<GluuConfiguration> gluuConfiguration = asGluuConfiguration(this.configuration);
            validate(gluuConfiguration);

            //read persistence `base` file
            Properties props = Utils.loadPropertiesFromFile(gluuConfiguration.get().getType(), null);
            //read persistence `connection` file
            props = Utils.loadPropertiesFromFile(gluuConfiguration.get().getConnection(), props);
            // set baseDn in props
            props.setProperty(PersistenceConfigKeys.BaseDn.getKeyName(), gluuConfiguration.get().getBaseDn());

            this.connectionProperties = props;
            //read salt file
            Properties saltProps = Utils.loadPropertiesFromFile(gluuConfiguration.get().getSalt(), null);
            return preparePersistanceProperties(saltProps.getProperty(PersistenceConfigKeys.EncodeSalt.getKeyName()));

        } catch (Exception e) {
            throw new IllegalStateException("Error starting GluuPersistenceService", e);
        }
    }

    protected Properties preparePersistanceProperties(String cryptoConfigurationSalt) {

        Properties decryptedConnectionProperties;
        try {
            decryptedConnectionProperties = PropertiesDecrypter.decryptAllProperties(StringEncrypter.defaultInstance(), this.connectionProperties, cryptoConfigurationSalt);
        } catch (StringEncrypter.EncryptionException ex) {
            throw new ConfigurationException("Failed to decript configuration properties", ex);
        }

        return decryptedConnectionProperties;
    }

    public static Optional<GluuConfiguration> asGluuConfiguration(OxdServerConfiguration configuration) {
        try {
            JsonNode node = configuration.getStorageConfiguration();
            if (node != null) {
                return Optional.ofNullable(Jackson2.createJsonMapper().treeToValue(node, GluuConfiguration.class));
            }
        } catch (Exception e) {
            LOG.error("Failed to parse GluuConfiguration.", e);
        }
        return Optional.empty();
    }

    private void validate(Optional<GluuConfiguration> gluuConfiguration) {

        if (!gluuConfiguration.isPresent()) {
            LOG.error("The `storage_configuration` has been not provided in `client-api-server.yml`");
            throw new RuntimeException("The `storage_configuration` has been not provided in `client-api-server.yml`");
        }

        GluuConfiguration configuration = gluuConfiguration.get();

        if (StringUtils.isBlank(configuration.getBaseDn())) {
            LOG.error("The `baseDn` field under storage_configuration is blank. Please provide value of this field (in `client-api-server.yml`)");
            throw new RuntimeException("The `baseDn` field under storage_configuration is blank. Please provide value of this field (in `client-api-server.yml`)");
        }

        if (StringUtils.isBlank(configuration.getType())) {
            LOG.error("The `type` field under storage_configuration is blank. Please provide the path of base persistence configuration file in this field (in `client-api-server.yml`)");
            throw new RuntimeException("The `type` field under storage_configuration is blank. Please provide the path of base persistence configuration file in this field (in `client-api-server.yml`)");
        }

        if (StringUtils.isBlank(configuration.getConnection())) {
            LOG.error("The `connection` field under storage_configuration is blank. Please provide the path of connection persistence configuration file in this field (in `client-api-server.yml`)");
            throw new RuntimeException("The `connection` field under storage_configuration is blank. Please provide the path of connection persistence configuration file in this field (in `client-api-server.yml`)");
        }

        if (StringUtils.isBlank(configuration.getSalt())) {
            LOG.error("The `salt` field under storage_configuration is blank. Please provide the path of salt file in this field (in `client-api-server.yml`)");
            throw new RuntimeException("The `salt` field under storage_configuration is blank. Please provide the path of salt file in this field (in `client-api-server.yml`)");
        }
    }
}

