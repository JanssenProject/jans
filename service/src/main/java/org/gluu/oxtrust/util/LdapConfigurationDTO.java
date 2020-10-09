package org.gluu.oxtrust.util;

import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import javax.validation.constraints.Size;
import java.util.List;

public class LdapConfigurationDTO {

    @Required
    @Size(min = 1)
    private String configId;

    @Required
    private String bindDN;

    @Required
    @Size(min = 1)
    private String bindPassword;

    @Required
    private List<String> servers;

    private int maxConnections;
    private boolean useSSL;

    @Required
    private List<String> baseDNs;

    @Required
    private String primaryKey;

    @Required
    private String localPrimaryKey;

    private boolean useAnonymousBind;

    private boolean enabled;

    private int level;

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getBindDN() {
        return bindDN;
    }

    public void setBindDN(String bindDN) {
        this.bindDN = bindDN;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public List<String> getBaseDNs() {
        return baseDNs;
    }

    public void setBaseDNs(List<String> baseDNs) {
        this.baseDNs = baseDNs;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getLocalPrimaryKey() {
        return localPrimaryKey;
    }

    public void setLocalPrimaryKey(String localPrimaryKey) {
        this.localPrimaryKey = localPrimaryKey;
    }

    public boolean isUseAnonymousBind() {
        return useAnonymousBind;
    }

    public void setUseAnonymousBind(boolean useAnonymousBind) {
        this.useAnonymousBind = useAnonymousBind;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}

