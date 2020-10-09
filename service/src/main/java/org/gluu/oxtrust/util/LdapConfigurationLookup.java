package org.gluu.oxtrust.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.gluu.model.ldap.GluuLdapConfiguration;

import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;

public class LdapConfigurationLookup {

	private final List<GluuLdapConfiguration> ldapConfigurations;

	public LdapConfigurationLookup(List<GluuLdapConfiguration> ldapConfigurations) {
		this.ldapConfigurations = ldapConfigurations;
	}

	public boolean shouldEncryptPassword(GluuLdapConfiguration ldapConfiguration) {
		try {
			GluuLdapConfiguration oldConfiguration = findByName(ldapConfiguration.getConfigId());
			String encryptedOldPassword = oldConfiguration.getBindPassword();
			return !StringUtils.equals(encryptedOldPassword, ldapConfiguration.getBindPassword());
		} catch (LdapConfigurationNotFoundException e) {
			return true;
		}
	}

	public GluuLdapConfiguration findByName(final String name) {
		return FluentIterable.from(ldapConfigurations).filter(new LdapConfigurationNamePredicate(name)).first()
				.or(notFound(name));
	}

	private Supplier<GluuLdapConfiguration> notFound(final String name) {
		return new Supplier<GluuLdapConfiguration>() {
			@Override
			public GluuLdapConfiguration get() {
				throw new LdapConfigurationNotFoundException(name);
			}
		};
	}

}
