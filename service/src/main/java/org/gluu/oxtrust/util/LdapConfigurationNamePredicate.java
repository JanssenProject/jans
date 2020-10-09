package org.gluu.oxtrust.util;

import com.google.common.base.Predicate;
import org.apache.commons.codec.binary.StringUtils;
import org.gluu.model.ldap.GluuLdapConfiguration;

public class LdapConfigurationNamePredicate implements Predicate<GluuLdapConfiguration> {

	private final String name;

	public LdapConfigurationNamePredicate(GluuLdapConfiguration ldapConfiguration) {
		this(ldapConfiguration.getConfigId());
	}

	public LdapConfigurationNamePredicate(String name) {
		this.name = name;
	}

	@Override
	public boolean apply(GluuLdapConfiguration ldapConfiguration) {
		return StringUtils.equals(ldapConfiguration.getConfigId(), name);
	}
}