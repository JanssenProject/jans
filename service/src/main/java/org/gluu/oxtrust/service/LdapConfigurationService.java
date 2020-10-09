package org.gluu.oxtrust.service;

import static org.gluu.oxtrust.util.CollectionsUtil.trimToEmpty;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.model.ldap.GluuLdapConfiguration;
import org.gluu.oxtrust.model.OxIDPAuthConf;
import org.gluu.oxtrust.util.LdapConfigurationException;
import org.gluu.oxtrust.util.LdapConfigurationLookup;
import org.gluu.oxtrust.util.LdapConfigurationNamePredicate;
import org.gluu.oxtrust.util.LdapConfigurationNotFoundException;
import org.gluu.util.security.StringEncrypter;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

@ApplicationScoped
public class LdapConfigurationService {

	private static final String AUTH = "auth";

	@Inject
	private ConfigurationService configurationService;

	@Inject
	private EncryptionService encryptionService;

	public List<GluuLdapConfiguration> findLdapConfigurations() {
		return FluentIterable.from(iDPAuthConfs()).transform(extractLdapConfiguration()).toList();
	}

	private Function<OxIDPAuthConf, GluuLdapConfiguration> extractLdapConfiguration() {
		return new Function<OxIDPAuthConf, GluuLdapConfiguration>() {
			@Override
			public GluuLdapConfiguration apply(OxIDPAuthConf oxIDPAuthConf) {
				return oxIDPAuthConf.getConfig();
			}
		};
	}

	private List<OxIDPAuthConf> iDPAuthConfs() {
		List<OxIDPAuthConf> authIdpConfs = new ArrayList<OxIDPAuthConf>();
		List<OxIDPAuthConf> idpConfs = trimToEmpty(configurationService.getConfiguration().getOxIDPAuthentication());
		for (OxIDPAuthConf idpConf : idpConfs) {
			if (idpConf.getType().equalsIgnoreCase(AUTH)) {
				authIdpConfs.add(idpConf);
			}
		}
		return authIdpConfs;
	}

	public GluuLdapConfiguration findActiveLdapConfiguration() {
		GluuLdapConfiguration result = Iterables.getFirst(findLdapConfigurations(), null);
		if (result == null) {
			throw new LdapConfigurationNotFoundException();
		}
		return result;
	}

	public GluuLdapConfiguration findLdapConfigurationByName(final String name) {
		return new LdapConfigurationLookup(findLdapConfigurations()).findByName(name);
	}

	public void save(List<GluuLdapConfiguration> ldapConfigurations) {
		org.gluu.oxtrust.model.GluuConfiguration configuration = configurationService.getConfiguration();
		configuration.setOxIDPAuthentication(oxIDPAuthConfs(ldapConfigurations));
		configurationService.updateConfiguration(configuration);
	}

	public void update(GluuLdapConfiguration ldapConfiguration) {
		List<GluuLdapConfiguration> ldapConfigurations = excludeFromConfigurations(
				new ArrayList<GluuLdapConfiguration>(findLdapConfigurations()), ldapConfiguration);
		ldapConfigurations.add(ldapConfiguration);

		save(ldapConfigurations);
	}

	public void save(GluuLdapConfiguration ldapConfiguration) {
		List<GluuLdapConfiguration> ldapConfigurations = new ArrayList<GluuLdapConfiguration>(findLdapConfigurations());
		ldapConfigurations.add(ldapConfiguration);

		save(ldapConfigurations);
	}

	private List<GluuLdapConfiguration> excludeFromConfigurations(List<GluuLdapConfiguration> ldapConfigurations,
			GluuLdapConfiguration ldapConfiguration) {
		boolean hadConfiguration = Iterables.removeIf(ldapConfigurations,
				new LdapConfigurationNamePredicate(ldapConfiguration));
		if (!hadConfiguration) {
			throw new LdapConfigurationNotFoundException(ldapConfiguration.getConfigId());
		}
		return ldapConfigurations;
	}

	private List<OxIDPAuthConf> oxIDPAuthConfs(List<GluuLdapConfiguration> ldapConfigurations) {
		final LdapConfigurationLookup ldapConfigurationLookup = new LdapConfigurationLookup(findLdapConfigurations());

		List<OxIDPAuthConf> idpConf = new ArrayList<OxIDPAuthConf>();
		for (GluuLdapConfiguration ldapConfig : ldapConfigurations) {

			if (ldapConfigurationLookup.shouldEncryptPassword(ldapConfig)) {
				ldapConfig.setBindPassword(encrypt(ldapConfig.getBindPassword()));
			}

			if (ldapConfig.isUseAnonymousBind()) {
				ldapConfig.setBindDN(null);
			}

			OxIDPAuthConf ldapConfigIdpAuthConf = new OxIDPAuthConf();
			ldapConfig.updateStringsLists();
			ldapConfigIdpAuthConf.setType(AUTH);
			ldapConfigIdpAuthConf.setVersion(ldapConfigIdpAuthConf.getVersion() + 1);
			ldapConfigIdpAuthConf.setName(ldapConfig.getConfigId());
			ldapConfigIdpAuthConf.setEnabled(ldapConfig.isEnabled());
			ldapConfigIdpAuthConf.setConfig(ldapConfig);

			idpConf.add(ldapConfigIdpAuthConf);
		}
		return idpConf;
	}

	private String encrypt(String data) {
		try {
			return encryptionService.encrypt(data);
		} catch (StringEncrypter.EncryptionException e) {
			throw new LdapConfigurationException(e);
		}
	}

	public void remove(String name) {
		GluuLdapConfiguration toRemove = findLdapConfigurationByName(name);
		List<GluuLdapConfiguration> allConfiguration = new ArrayList<GluuLdapConfiguration>(findLdapConfigurations());
		List<GluuLdapConfiguration> newConfigurations = excludeFromConfigurations(allConfiguration, toRemove);
		save(newConfigurations);
	}
}