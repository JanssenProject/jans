package io.jans.configapi.service;

import io.jans.as.client.uma.UmaMetadataService;
import io.jans.as.client.uma.UmaPermissionService;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.configapi.auth.AuthClientFactory;
import io.jans.exception.ConfigurationException;
import io.jans.exception.OxIntializationException;
import io.jans.util.init.Initializable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import org.slf4j.Logger;

@ApplicationScoped
public class UmaService extends Initializable implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String WELL_KNOWN_UMA_PATH = "/.well-known/uma2-configuration";

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    private UmaMetadata umaMetadata;
    private UmaPermissionService umaPermissionService;
    private UmaRptIntrospectionService umaRptIntrospectionService;

    @Override
    protected void initInternal() {
        try {
            loadUmaConfigurationService();
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to load oxAuth UMA configuration", ex);
        }
    }

    public UmaMetadata getUmaMetadata() throws Exception {
        init();
        return this.umaMetadata;
    }

    public void loadUmaConfigurationService() throws Exception {
        this.umaMetadata = getUmaMetadataConfiguration();
        this.umaPermissionService = AuthClientFactory.getUmaPermissionService(this.umaMetadata, false);
        this.umaRptIntrospectionService = AuthClientFactory.getUmaRptIntrospectionService(this.umaMetadata, false);
    }

    @Produces
    @ApplicationScoped
    @Named("umaMetadataConfiguration")
    public UmaMetadata getUmaMetadataConfiguration() throws OxIntializationException {

        logger.info("##### Getting UMA Metadata Service ...");
        logger.debug(
                "\n\n UmaService::initUmaMetadataConfiguration() - configurationService.find().getUmaConfigurationEndpoint() = "
                        + configurationService.find().getUmaConfigurationEndpoint());
        UmaMetadataService umaMetadataService = AuthClientFactory
                .getUmaMetadataService(configurationService.find().getUmaConfigurationEndpoint(), false);
        logger.debug("\n\n UmaService::initUmaMetadataConfiguration() - umaMetadataService = " + umaMetadataService);

        logger.info("##### Getting UMA Metadata ...");
        UmaMetadata umaMetadata = umaMetadataService.getMetadata();
        logger.debug("\n\n UmaService::initUmaMetadataConfiguration() - umaMetadata = " + umaMetadata);
        logger.info("##### Getting UMA metadata ... DONE");

        if (umaMetadata == null) {
            throw new OxIntializationException("UMA meta data configuration is invalid!");
        }

        return umaMetadata;
    }

}
