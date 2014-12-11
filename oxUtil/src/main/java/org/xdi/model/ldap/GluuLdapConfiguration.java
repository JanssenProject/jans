/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.model.ldap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.xdi.model.SimpleProperty;

/**
 * GluuLdapConfiguration
 * 
 * @author Yuriy Movchan Date: 07.29.2011
 */
@XmlRootElement
@JsonPropertyOrder({ "configId", "bindDN", "bindPassword", "servers", "maxConnections", "useSSL", "baseDNs", "primaryKey", "localPrimaryKey", "useAnonymousBind", "enabled", "version" })
public class GluuLdapConfiguration implements Serializable {

	private static final long serialVersionUID = -7160480457430436511L;

	private String configId;
	private String bindDN;
	private String bindPassword;
	private List<SimpleProperty> servers;
	private int maxConnections;
	private boolean useSSL;
	private List<SimpleProperty> baseDNs;
	private String primaryKey;
	private String localPrimaryKey;
	private boolean useAnonymousBind;
	private boolean enabled;
	private int version;

	public GluuLdapConfiguration() {
		this.servers = new ArrayList<SimpleProperty>();
		this.baseDNs = new ArrayList<SimpleProperty>();
	}

	public GluuLdapConfiguration(String configId, String bindDN, String bindPassword, List<SimpleProperty> servers, int maxConnections,
			boolean useSSL, List<SimpleProperty> baseDNs, String primaryKey, String localPrimaryKey, boolean useAnonymousBind) {
		this.configId = configId;
		this.bindDN = bindDN;
		this.bindPassword = bindPassword;
		this.servers = servers;
		this.maxConnections = maxConnections;
		this.useSSL = useSSL;
		this.baseDNs = baseDNs;
		this.primaryKey = primaryKey;
		this.localPrimaryKey = localPrimaryKey;
		this.useAnonymousBind = useAnonymousBind;
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GluuLdapConfiguration [configId=").append(configId).append(", bindDN=").append(bindDN).append(", bindPassword=")
				.append(bindPassword).append(", servers=").append(servers).append(", maxConnections=").append(maxConnections)
				.append(", useSSL=").append(useSSL).append(", baseDNs=").append(baseDNs).append(", primaryKey=").append(primaryKey)
				.append(", localPrimaryKey=").append(localPrimaryKey).append(", useAnonymousBind=").append(useAnonymousBind)
				.append(", enabled=").append(enabled).append(", version=").append(version).append("]");
		return builder.toString();
	}

}
