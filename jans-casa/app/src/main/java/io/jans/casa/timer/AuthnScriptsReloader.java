package io.jans.casa.timer;

import io.jans.model.ScriptLocationType;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.casa.core.ConfigurationHandler;
import io.jans.casa.core.ExtensionsManager;
import io.jans.casa.core.PersistenceService;
import io.jans.casa.core.TimerService;
import io.jans.casa.core.model.CustomScript;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.*;
import org.quartz.JobExecutionContext;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;

/**
 * @author jgomer
 */
@ApplicationScoped
public class AuthnScriptsReloader extends JobListenerSupport {

    private static final int SCAN_INTERVAL = 60;    //check the cust scripts every 60sec
    private static final String LOCATION_TYPE_PROPERTY = "location_type";
    private static final String LOCATION_PATH_PROPERTY = "location_path";
    private static final String FILENAME_TEMPLATE = "casa-external_{0}.py";
    private static final List<String> NOT_REPLACEABLE_SCRIPTS = Arrays.asList(SecurityKey2Extension.ACR,
            OTPExtension.ACR, SuperGluuExtension.ACR, OTPTwilioExtension.ACR);

    @Inject
    private Logger logger;

    @Inject
    private TimerService timerService;

    @Inject
    private ConfigurationHandler confHandler;

    @Inject
    private PersistenceService persistenceService;

    @Inject
    private ExtensionsManager extManager;

    private String scriptsJobName;
    private Map<String, Long> scriptFingerPrints;

    public void init(int gap) {
        try {
            timerService.addListener(this, scriptsJobName);
            //Start in 2 seconds and repeat indefinitely
            timerService.schedule(scriptsJobName, gap, -1, SCAN_INTERVAL);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return scriptsJobName;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {

        Long previous, current;
        CustomScript script;

        //A flag used to force a reload of main cust script
        boolean reload = false;
        Map<String, String> mapping = confHandler.getSettings().getAcrPluginMap();
        Set<String> acrs = mapping.keySet();
        List<String> toBeRemoved = new ArrayList<>();

        logger.debug("AuthnScriptsReloader. Running timer job for acrs: {}", acrs.toString());
        //In Gluu, every time a single script is changed via oxTrust, all custom scripts are reloaded. This is
        //not the case when for instance, the jansRevision attribute of a cust script is altered manually (as when developing)
        //In the future, oxTrust should only reload the script that has changed for performance reasons.
        for (String acr : acrs) {
            script = persistenceService.getScript(acr);

            if (script != null) {
                if (Optional.ofNullable(script.getEnabled()).orElse(false)) {
                    previous = scriptFingerPrints.get(acr);
                    current = scriptFingerPrint(script);
                    scriptFingerPrints.put(acr, current);

                    if (!current.equals(previous)) {
                        //When previous is null, it means it's the first run of this timer or the method was checked enabled in methods.zul
                        //When non null, it means this script is known from a previous run of the timer and its contents changed
                        //Under any of these circumstances we force a reload of main cust script in the end
                        reload = true;
                        if (previous != null) {
                            logger.info("Changes detected in {} script", acr);
                            //Force extension reloading (normally to re-read configuration properties)
                            extManager.getExtensionForAcr(acr).ifPresent(AuthnMethod::reloadConfiguration);
                        }
                        //Out-of-the-box script methods must not be copied. This avoids transfering the default scripts
                        //bundled with CE which are not suitable for Casa
                        if (!NOT_REPLACEABLE_SCRIPTS.contains(acr)) {
                            copyToLibsDir(script);
                        }
                    }
                } else {
                    //This accounts for the case in which the method is part of the mapping, but admin has externally disabled
                    //the cust script. This helps keeping in sync the methods supported in casa wrt to enabled methods in oxTrust
                    toBeRemoved.add(acr);
                }
            }
        }

        if (toBeRemoved.size() > 0) {
            logger.info("The following scripts were externally disabled: {}", toBeRemoved.toString());
            logger.warn("Corresponding acrs will be removed from Gluu Casa configuration file");

            //Remove from the mapping
            acrs.removeAll(toBeRemoved);
            //Here mapping was altered since acrs is its associated keyset
            confHandler.getSettings().setAcrPluginMap(mapping);

            try {
                //Save mapping changes to disk
                confHandler.saveSettings();
                reload = true;
            } catch (Exception e) {
                logger.error("Failure to update configuration settings!");
            }
        }

        if (!reload) {
            //It may happen that a method was disabled in methods.zul, this should trigger a reload too
            //An easy way to detect this is when scriptFingerPrints contains more elements than acrs set
            reload = !acrs.containsAll(scriptFingerPrints.keySet());
        }
        scriptFingerPrints.keySet().retainAll(acrs);

        if (reload) {
            //"touch" main script so that it gets reloaded. This helps the script to keep the list of supported methods up-to-date
            try {
                logger.info("Touching main interception script to trigger reload by jans-auth-server...");
                script = persistenceService.getScript(ConfigurationHandler.DEFAULT_ACR);
                Map<String, String> moduleProperties = Utils.scriptModulePropertiesAsMap(script);
                ScriptLocationType locType = ScriptLocationType.getByValue(moduleProperties.get(LOCATION_TYPE_PROPERTY));

                switch (locType) {
                    case FILE:
                        File f = Paths.get(moduleProperties.get(LOCATION_PATH_PROPERTY)).toFile();
                        if (f.setLastModified(System.currentTimeMillis())) {
                            logger.debug("Last modified timestamp of '{}' has been updated", f.getPath());
                        } else {
                            logger.warn("Timestamp of '{}' could not be altered. Ensure jetty user has enough permissions", f.getPath());
                            logger.error("Latest changes to custom scripts were not picked");
                        }
                        break;
                    case DB:
                    case LDAP:
                        long rev = Optional.ofNullable(script.getRevision()).map(r -> r == Long.MAX_VALUE ? 0 : r).orElse(0L);
                        script.setRevision(rev + 1);
                        if (persistenceService.modify(script)) {
                            logger.debug("jansRevision updated for script '{}'", script.getDisplayName());
                        }
                        break;
                    default:
                        //Added to pass checkstyle
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                logger.warn("Main custom script could not be touched!");
                logger.info("Recent changes in dependant scripts won't take effect until a new successful reload of script succeeds");
            }
        }

    }

    @PostConstruct
    private void inited() {
        scriptsJobName = getClass().getSimpleName() + "_scripts";
        scriptFingerPrints = new HashMap<>();
    }

    private void copyToLibsDir(CustomScript script) {

        try {
            String acr = script.getDisplayName();
            byte[] contents = getScriptContents(script);

            if (contents != null) {
                Path destPath = Paths.get(persistenceService.getPythonLibLocation(), MessageFormat.format(FILENAME_TEMPLATE, acr));
                Files.write(destPath, contents);
                logger.trace("The script for acr '{}' has been copied to {}", acr, destPath.toString());
            } else {
                logger.warn("The script for acr '{}' was not copied to jython's lib path", acr);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private byte[] getScriptContents(CustomScript script) {

        byte[] bytes = null;
        String acr = script.getDisplayName();
        try {
            Map<String, String> moduleProperties = Utils.scriptModulePropertiesAsMap(script);
            ScriptLocationType locType = ScriptLocationType.getByValue(moduleProperties.get(LOCATION_TYPE_PROPERTY));

            switch (locType) {
                case FILE:
                    bytes = Files.readAllBytes(Paths.get(moduleProperties.get(LOCATION_PATH_PROPERTY)));
                    break;
                case DB:
                case LDAP:
                    bytes = script.getScript().getBytes(StandardCharsets.UTF_8);
                    break;
                default:
                    throw new Exception("Unknown 'location_type' value for script " + acr);
            }
        } catch (Exception e) {
            logger.error("getScriptContents. There was an error reading the bytes of script '{}': {}", acr, e.getMessage());
        }
        return bytes;

    }

    private Long scriptFingerPrint(CustomScript script) {

        Long fingerPrint = null;
        String acr = script.getDisplayName();

        try {
            Map<String, String> moduleProperties = Utils.scriptModulePropertiesAsMap(script);
            ScriptLocationType locType = ScriptLocationType.getByValue(moduleProperties.get(LOCATION_TYPE_PROPERTY));

            switch (locType) {
                case FILE:
                    fingerPrint = Paths.get(moduleProperties.get(LOCATION_PATH_PROPERTY)).toFile().lastModified();
                    break;
                case DB:
                case LDAP:
                    fingerPrint = script.getRevision();
                    break;
                default:
                    throw new Exception("Unknown 'location_type' value for script " + acr);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return fingerPrint;

    }

}
