package org.gluu.oxtrust.util;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.gluu.model.SimpleProperty;
import org.gluu.model.ldap.GluuLdapConfiguration;

import java.util.List;

public class LdapConfigurationDtoAssembly {

	private static final String EMPTY_PASSWORD = "";

	private static final Function<SimpleProperty, String> AS_TEXT = new Function<SimpleProperty, String>() {
		@Override
		public String apply(SimpleProperty property) {
			return property.getValue();
		}
	};

	public Function<GluuLdapConfiguration, LdapConfigurationDTO> toDtoAsFunction() {
		return new Function<GluuLdapConfiguration, LdapConfigurationDTO>() {
			@Override
			public LdapConfigurationDTO apply(GluuLdapConfiguration ldapConfiguration) {
				return toDto(ldapConfiguration);
			}
		};
	}

	public LdapConfigurationDTO toDto(GluuLdapConfiguration ldapConfiguration) {
		LdapConfigurationDTO ldapConfigurationDTO = new LdapConfigurationDTO();
		ldapConfigurationDTO.setConfigId(ldapConfiguration.getConfigId());
		ldapConfigurationDTO.setBindDN(ldapConfiguration.getBindDN());
		ldapConfigurationDTO.setServers(toTextList(ldapConfiguration.getServers()));
		ldapConfigurationDTO.setMaxConnections(ldapConfiguration.getMaxConnections());
		ldapConfigurationDTO.setUseSSL(ldapConfiguration.isUseSSL());
		ldapConfigurationDTO.setBaseDNs(toTextList(ldapConfiguration.getBaseDNs()));
		ldapConfigurationDTO.setPrimaryKey(ldapConfiguration.getPrimaryKey());
		ldapConfigurationDTO.setLocalPrimaryKey(ldapConfiguration.getLocalPrimaryKey());
		ldapConfigurationDTO.setUseAnonymousBind(ldapConfiguration.isUseAnonymousBind());
		ldapConfigurationDTO.setEnabled(ldapConfiguration.isEnabled());
		ldapConfigurationDTO.setLevel(ldapConfiguration.getLevel());

		ldapConfigurationDTO.setBindPassword(EMPTY_PASSWORD);
		return ldapConfigurationDTO;
	}

	private List<String> toTextList(List<SimpleProperty> properties) {
		return FluentIterable.from(properties).transform(AS_TEXT).toList();
	}

	public GluuLdapConfiguration fromDto(LdapConfigurationDTO ldapConfiguration) {
		GluuLdapConfiguration gluuLdapConfiguration = new GluuLdapConfiguration();
		gluuLdapConfiguration.setLevel(ldapConfiguration.getLevel());
		gluuLdapConfiguration.setConfigId(ldapConfiguration.getConfigId());
		gluuLdapConfiguration.setBindDN(ldapConfiguration.getBindDN());
		gluuLdapConfiguration.setBindPassword(ldapConfiguration.getBindPassword());
		gluuLdapConfiguration.setServersStringsList(ldapConfiguration.getServers());
		gluuLdapConfiguration.setMaxConnections(ldapConfiguration.getMaxConnections());
		gluuLdapConfiguration.setUseSSL(ldapConfiguration.isUseSSL());
		gluuLdapConfiguration.setBaseDNsStringsList(ldapConfiguration.getBaseDNs());
		gluuLdapConfiguration.setPrimaryKey(ldapConfiguration.getPrimaryKey());
		gluuLdapConfiguration.setLocalPrimaryKey(ldapConfiguration.getLocalPrimaryKey());
		gluuLdapConfiguration.setUseAnonymousBind(ldapConfiguration.isUseAnonymousBind());
		gluuLdapConfiguration.setEnabled(ldapConfiguration.isEnabled());
		return gluuLdapConfiguration;
	}
}
