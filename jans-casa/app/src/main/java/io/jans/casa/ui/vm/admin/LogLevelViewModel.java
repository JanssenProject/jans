package io.jans.casa.ui.vm.admin;

import io.jans.casa.core.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import java.util.*;

/**
 * @author jgomer
 */
public class LogLevelViewModel extends MainViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private List<String> logLevels;
    private String selectedLogLevel;

    @WireVariable
    private LogService logService;

    public List<String> getLogLevels() {
        return logLevels;
    }

    public String getSelectedLogLevel() {
        return selectedLogLevel;
    }

    @Init(superclass = true)
    public void childInit() {
        //it seems ZK doesn't like ummodifiable lists
        logLevels = new ArrayList(LogService.SLF4J_LEVELS);
        selectedLogLevel = getSettings().getLogLevel();
    }

    @NotifyChange({"selectedLogLevel"})
    public void change(String newLevel) {

        //here it is assumed that changing log level is always a successful operation
        logService.updateLoggingLevel(newLevel);
        selectedLogLevel = newLevel;
        getSettings().setLogLevel(newLevel);

        if (updateMainSettings(Labels.getLabel("adm.logging_action"))) {
            logger.info("Log level changed to {}", newLevel);
        }

    }

}
