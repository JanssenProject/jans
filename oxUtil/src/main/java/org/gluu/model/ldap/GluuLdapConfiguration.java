/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model.ldap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.gluu.model.SimpleProperty;

/**
 * GluuLdapConfiguration
 *
 * @author Yuriy Movchan Date: 07.29.2011
 */
@JsonPropertyOrder({ "configId", "bindDN", "bindPassword", "servers", "maxConnections", "useSSL", "baseDNs",
        "primaryKey", "localPrimaryKey", "useAnonymousBind", "enabled", "version" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class GluuLdapConfiguration implements Serializable {

    private static final long serialVersionUID = -7160480457430436511L;

    private String configId;
    private String bindDN;
    private String bindPassword;

    @JsonIgnore
    private List<SimpleProperty> servers;

    @JsonProperty("servers")
    private List<String> serversStringsList;

    private int maxConnections;
    private boolean useSSL;

    @JsonIgnore
    private List<SimpleProperty> baseDNs;

    @JsonProperty("baseDNs")
    private List<String> baseDNsStringsList;

    private String primaryKey;
    private String localPrimaryKey;
    private boolean useAnonymousBind;
    private boolean enabled;
    private int version;

    @JsonProperty("level")
    private int level;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public GluuLdapConfiguration() {
        this.servers = new ArrayList<SimpleProperty>();
        this.baseDNs = new ArrayList<SimpleProperty>();

        updateStringsLists();
    }

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

    public List<SimpleProperty> getServers() {
        return servers;
    }

    public void setServers(List<SimpleProperty> servers) {
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

    public List<SimpleProperty> getBaseDNs() {
        return baseDNs;
    }

    public void setBaseDNs(List<SimpleProperty> baseDNs) {
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<String> getServersStringsList() {
        return serversStringsList;
    }

    public void setServersStringsList(List<String> serversStringsList) {
        this.serversStringsList = serversStringsList;

        updateSimplePropertiesLists();
    }

    public List<String> getBaseDNsStringsList() {
        return baseDNsStringsList;
    }

    public void setBaseDNsStringsList(List<String> baseDNsStringsList) {
        this.baseDNsStringsList = baseDNsStringsList;

        updateSimplePropertiesLists();
    }

    public void updateStringsLists() {
        this.serversStringsList = toStringList(servers);
        this.baseDNsStringsList = toStringList(baseDNs);
    }

    public void updateSimplePropertiesLists() {
        this.servers = toSimpleProperties(serversStringsList);
        this.baseDNs = toSimpleProperties(baseDNsStringsList);
    }

    private List<String> toStringList(List<SimpleProperty> values) {
        if (values == null) {
            return null;
        }

        List<String> result = new ArrayList<String>();

        for (SimpleProperty simpleProperty : values) {
            result.add(simpleProperty.getValue());
        }

        return result;
    }

    private List<SimpleProperty> toSimpleProperties(List<String> values) {
        if (values == null) {
            return null;
        }

        List<SimpleProperty> result = new ArrayList<SimpleProperty>();

        for (String value : values) {
            result.add(new SimpleProperty(value));
        }

        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseDNs == null) ? 0 : baseDNs.hashCode());
        result = prime * result + ((bindDN == null) ? 0 : bindDN.hashCode());
        result = prime * result + ((bindPassword == null) ? 0 : bindPassword.hashCode());
        result = prime * result + ((configId == null) ? 0 : configId.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((localPrimaryKey == null) ? 0 : localPrimaryKey.hashCode());
        result = prime * result + maxConnections;
        result = prime * result + ((primaryKey == null) ? 0 : primaryKey.hashCode());
        result = prime * result + ((servers == null) ? 0 : servers.hashCode());
        result = prime * result + (useAnonymousBind ? 1231 : 1237);
        result = prime * result + (useSSL ? 1231 : 1237);
        result = prime * result + version;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof GluuLdapConfiguration)) {
            return false;
        }
        GluuLdapConfiguration other = (GluuLdapConfiguration) obj;
        if (baseDNs == null) {
            if (other.baseDNs != null) {
                return false;
            }
        } else if (!baseDNs.equals(other.baseDNs)) {
            return false;
        }
        if (bindDN == null) {
            if (other.bindDN != null) {
                return false;
            }
        } else if (!bindDN.equals(other.bindDN)) {
            return false;
        }
        if (bindPassword == null) {
            if (other.bindPassword != null) {
                return false;
            }
        } else if (!bindPassword.equals(other.bindPassword)) {
            return false;
        }
        if (configId == null) {
            if (other.configId != null) {
                return false;
            }
        } else if (!configId.equals(other.configId)) {
            return false;
        }
        if (enabled != other.enabled) {
            return false;
        }
        if (localPrimaryKey == null) {
            if (other.localPrimaryKey != null) {
                return false;
            }
        } else if (!localPrimaryKey.equals(other.localPrimaryKey)) {
            return false;
        }
        if (maxConnections != other.maxConnections) {
            return false;
        }
        if (primaryKey == null) {
            if (other.primaryKey != null) {
                return false;
            }
        } else if (!primaryKey.equals(other.primaryKey)) {
            return false;
        }
        if (servers == null) {
            if (other.servers != null) {
                return false;
            }
        } else if (!servers.equals(other.servers)) {
            return false;
        }
        if (useAnonymousBind != other.useAnonymousBind) {
            return false;
        }
        if (useSSL != other.useSSL) {
            return false;
        }
        if (version != other.version) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GluuLdapConfiguration [configId=").append(configId).append(", bindDN=").append(bindDN)
                .append(", bindPassword=").append(bindPassword).append(", servers=").append(servers)
                .append(", maxConnections=").append(maxConnections).append(", useSSL=").append(useSSL)
                .append(", baseDNs=").append(baseDNs).append(", primaryKey=").append(primaryKey)
                .append(", localPrimaryKey=").append(localPrimaryKey).append(", useAnonymousBind=")
                .append(useAnonymousBind).append(", enabled=").append(enabled).append(", version=").append(version)
                .append("]");
        return builder.toString();
    }

}
