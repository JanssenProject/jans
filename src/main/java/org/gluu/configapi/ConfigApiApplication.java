package org.gluu.configapi;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.gluu.configapi.configuration.ConfigurationFactory;
import org.gluu.exception.OxIntializationException;
import org.gluu.oxauth.service.common.ApplicationFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.service.PersistanceFactoryService;
import org.gluu.service.cdi.event.LdapConfigurationReload;
import org.gluu.service.cdi.util.CdiUtil;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
public class ConfigApiApplication {

    @Inject
    Logger logger;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
    BeanManager beanManager;
    @Inject
    ConfigurationFactory configurationFactory;
    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    void onStart(@Observes StartupEvent ev) {
        logger.info("=================================================================");
        logger.info("=============  STARTING API APPLICATION  ========================");
        logger.info("=================================================================");
        System.setProperty(ResteasyContextParameters.RESTEASY_PATCH_FILTER_DISABLED, "true");
        this.configurationFactory.create();
        persistenceEntryManagerInstance.get();
        logger.info("=================================================================");
        logger.info("==============  APPLICATION IS UP AND RUNNING ===================");
        logger.info("=================================================================");
    }

    void onStop(@Observes ShutdownEvent ev) {
        logger.info("================================================================");
        logger.info("===========  API APPLICATION STOPPED  ==========================");
        logger.info("================================================================");
    }

    @Produces
    @ApplicationScoped
    public ConfigurationFactory getConfigurationFactory() {
        return configurationFactory;
    }

    @Produces
    @ApplicationScoped
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    public PersistenceEntryManager createPersistenceEntryManager() throws OxIntializationException {
        PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistanceFactoryService.getPersistenceEntryManagerFactory(configurationFactory.getPersistenceConfiguration());
        PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerFactory.createEntryManager(configurationFactory.getDecryptedConnectionProperties());
        logger.debug("Created {}: {} with operation service: {}", ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME,
                persistenceEntryManager, persistenceEntryManager.getOperationService());
        return persistenceEntryManager;
    }

    public void recreatePersistanceEntryManager(@Observes @LdapConfigurationReload String event) {
        closePersistenceEntryManager();
        PersistenceEntryManager ldapEntryManager = persistenceEntryManagerInstance.get();
        persistenceEntryManagerInstance.destroy(ldapEntryManager);
        logger.debug("Recreated instance {}: {} with operation service: {}", ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME,
                ldapEntryManager, ldapEntryManager.getOperationService());
    }

    private void closePersistenceEntryManager() {
        PersistenceEntryManager oldInstance = CdiUtil.getContextBean(beanManager, PersistenceEntryManager.class, ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME);
        if (oldInstance == null || oldInstance.getOperationService() == null)
            return;


        logger.debug("Attempting to destroy {}:{} with operation service: {}", ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME,
                oldInstance, oldInstance.getOperationService());
        oldInstance.destroy();
        logger.debug("Destroyed {}:{} with operation service: {}", ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME,
                oldInstance, oldInstance.getOperationService());
    }
}
