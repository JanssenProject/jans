package io.jans.casa.core;

import io.jans.casa.conf.MainSettings;
import io.jans.casa.misc.AppStateEnum;
import io.jans.casa.model.ApplicationConfiguration;
import io.jans.casa.timer.*;
import io.jans.orm.exception.operation.PersistenceException;
import io.jans.service.document.store.manager.DocumentStoreManager;
import io.jans.util.security.SecurityProviderUtility;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.*;

import org.slf4j.Logger;

@ApplicationScoped
public class ConfigurationHandler {

    public static final String AGAMA_FLOW_ACR = System.getProperty("acr");

    private final static String DOCUMENT_STORE_MANAGER_JANS_CASA_TYPE = "casa"; // Module name

    @Inject
    private Logger logger;

    @Inject
    private AssetsService assetsService;

    @Inject
    private PersistenceService persistenceService;

    @Inject
    private ExtensionsManager extManager;

    @Inject
    private LogService logService;

    @Inject
    private AuthnScriptsReloader scriptsReloader;

    @Inject
    private SyncSettingsTimer syncSettingsTimer;

    @Inject
    private FSPluginChecker pluginChecker;

    @Inject
    private DocumentStoreManager documentStoreManager;

    private ApplicationConfiguration appConfiguration;

    private MainSettings settings;

    private AppStateEnum appState;

    @PostConstruct
    private void inited() {
        setAppState(AppStateEnum.LOADING);
        logger.info("ConfigurationHandler inited");
        SecurityProviderUtility.installBCProvider();
    }

    private boolean initializeSettings() {
        logger.info("initializeSettings. Obtaining global settings");
        appConfiguration = persistenceService.getAppConfiguration();
        settings = appConfiguration.getSettings();
        return settings != null;
    }

    void init() {

        try {
            //Check DB access to proceed with the rest of initialization
            if (persistenceService.initialize() && initializeSettings()) {
                //Update log level ASAP
                computeLoggingLevel();
                //Force early initialization of assets service before it is used in zul templates
                assetsService.reloadUrls();

                extManager.scan();
                computeAcrPluginMapping();
                computeCorsOrigins();
                saveSettings();

                // Initialize Document Store Manager
                documentStoreManager.initTimer(Arrays.asList(DOCUMENT_STORE_MANAGER_JANS_CASA_TYPE));

                setAppState(AppStateEnum.OPERATING);
                
                try {
                    logger.info("=== WEBAPP INITIALIZED SUCCESSFULLY ===");
        
                    //Add some random seconds to gaps. This reduces the chance of timers running at the same time
                    //in a multi node environment, which IMO it's somewhat safer
                    int gap = Double.valueOf(Math.random() * 7).intValue();
                    scriptsReloader.init(1 + gap);
                    syncSettingsTimer.activate(60 + gap);
                    //plugin checker is not shared-state related
                    pluginChecker.activate(5);
                    
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
        
                
            } else {
                setAppState(AppStateEnum.FAIL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            setAppState(AppStateEnum.FAIL);
        }

    }

    @Produces @ApplicationScoped
    public MainSettings getSettings() {
        return settings;
    }

    public void saveSettings() throws PersistenceException {
        logger.info("Persisting settings to database");
        if (!persistenceService.modify(appConfiguration)) {
            throw new PersistenceException("Config changes could not be saved to database");
        }
    }

    public AppStateEnum getAppState() {
        return appState;
    }

    private void setAppState(AppStateEnum state) {

        if (state.equals(AppStateEnum.FAIL)) {
            logger.error("Application not in operable state, please fix configuration issues before proceeding.");
            logger.info("=== WEBAPP INITIALIZATION FAILED ===");
        }
        appState = state;

    }

    private void computeLoggingLevel() {
        settings.setLogLevel(logService.updateLoggingLevel(settings.getLogLevel()));
    }

    private void computeAcrPluginMapping() {
        
        Map<String, String> map = settings.getAcrPluginMap();
        if (map == null) {
            settings.setAcrPluginMap(new LinkedHashMap<>());
        } else {
            //migrate from installations older than 1.1.4
            List.of("fido2", "super_gluu", "otp", "twilio_sms").forEach(old -> {
                    if (map.remove(old, null)) {
                        //it was an OOTB method
                        map.put("io.jans.casa.authn." + old, null);
                    }
            });
        }
        
    }

    private void computeCorsOrigins() {
        if (settings.getCorsDomains() == null) {
            settings.setCorsDomains(new ArrayList<>());
        }
    }

}
