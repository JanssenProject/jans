package org.gluu.oxtrust.util;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.gluu.model.SimpleProperty;
import org.gluu.model.ldap.GluuLdapConfiguration;

import java.util.List;

public class LdapConnectionData {

	private String bindDN;
	private String bindPassword;
	private List<String> servers;
	private boolean useSSL;

	public LdapConnectionData() {
	}

	public LdapConnectionData(String bindDN, String bindPassword, List<String> servers, boolean useSSL) {
		this.bindDN = bindDN;
		this.bindPassword = bindPassword;
		this.servers = servers;
		this.useSSL = useSSL;
	}

	public static LdapConnectionData from(GluuLdapConfiguration ldapConfig) {
		return new LdapConnectionData(ldapConfig.getBindDN(), ldapConfig.getBindPassword(),
				FluentIterable.from(ldapConfig.getServers()).transform(new Function<SimpleProperty, String>() {
					@Override
					public String apply(SimpleProperty property) {
						return property.getValue();
					}
				}).toList(), ldapConfig.isUseSSL());
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

	public boolean isUseSSL() {
		return useSSL;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}
}