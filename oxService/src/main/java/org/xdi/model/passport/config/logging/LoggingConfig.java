package org.xdi.model.passport.config.logging;

/**
 * Created by jgomer on 2019-02-21.
 */
public class LoggingConfig {

    private String level;
    private boolean consoleLogOnly;
    private MQConfig activeMQConf;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public boolean isConsoleLogOnly() {
        return consoleLogOnly;
    }

    public void setConsoleLogOnly(boolean consoleLogOnly) {
        this.consoleLogOnly = consoleLogOnly;
    }

    public MQConfig getActiveMQConf() {
        return activeMQConf;
    }

    public void setActiveMQConf(MQConfig activeMQConf) {
        this.activeMQConf = activeMQConf;
    }

}
