package org.gluu.configapi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

import org.gluu.model.ldap.GluuLdapConfiguration;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.util.security.StringEncrypter;
import org.gluu.oxauth.service.common.EncryptionService;
import org.oxauth.persistence.model.configuration.GluuConfiguration;
import org.oxauth.persistence.model.configuration.oxIDPAuthConf;
import org.gluu.configapi.exception.GlobalRuntimeException;

@ApplicationScoped
public class LdapConfigurationService {

  private static final String AUTH = "auth";

  @Inject
  Logger looger;

  @Inject
  ConfigurationService configurationService;

  @Inject
  PersistenceEntryManager persistenceEntryManager;

  @Inject
  private EncryptionService encryptionService;


  public List<GluuLdapConfiguration> findLdapConfigurations() {
    return FluentIterable.from(getOxIDPAuthConf()).transform(extractLdapConfiguration()).toList();
  }

  public GluuLdapConfiguration findLdapConfigurationByName(final String name) {
    return findByName(name);
  }

  public void save(GluuLdapConfiguration ldapConfiguration) {
    List<GluuLdapConfiguration> ldapConfigurations = new ArrayList<GluuLdapConfiguration>(findLdapConfigurations());
    ldapConfigurations.add(ldapConfiguration);
    save(ldapConfigurations);
  }

  public void save(List<GluuLdapConfiguration> ldapConfigurations) {
    GluuConfiguration configuration = configurationService.findGluuConfiguration();
    configuration.setOxIDPAuthentication(getOxIDPAuthConfs(ldapConfigurations));
    configurationService.merge(configuration);
  }

  public void update(GluuLdapConfiguration ldapConfiguration) {
    List<GluuLdapConfiguration> ldapConfigurations = excludeFromConfigurations(
        new ArrayList<GluuLdapConfiguration>(findLdapConfigurations()), ldapConfiguration);
    ldapConfigurations.add(ldapConfiguration);

    save(ldapConfigurations);
  }

  public void remove(String name) {
    GluuLdapConfiguration toRemove = findLdapConfigurationByName(name);
    List<GluuLdapConfiguration> allConfiguration = new ArrayList<GluuLdapConfiguration>(findLdapConfigurations());
    List<GluuLdapConfiguration> newConfigurations = excludeFromConfigurations(allConfiguration, toRemove);
    save(newConfigurations);
  }

  private GluuLdapConfiguration findByName(String name) {
    List<GluuLdapConfiguration> ldapConfigurations = findLdapConfigurations();
    Optional<GluuLdapConfiguration> matchingLdapConfiguration = ldapConfigurations.stream()
        .filter(d -> d.getConfigId().equals(name)).findFirst();
    GluuLdapConfiguration ldapConfiguration = matchingLdapConfiguration.get();
    return ldapConfiguration;
  }

  private Function<oxIDPAuthConf, GluuLdapConfiguration> extractLdapConfiguration() {
    return new Function<oxIDPAuthConf, GluuLdapConfiguration>() {
      @Override
      public GluuLdapConfiguration apply(oxIDPAuthConf authConf) {
        return authConf.getConfig();
      }
    };
  }

  private List<oxIDPAuthConf> getOxIDPAuthConf() {
    List<oxIDPAuthConf> idpConfList = trimToEmpty(
        configurationService.findGluuConfiguration().getOxIDPAuthentication());
    List<oxIDPAuthConf> authConfs = idpConfList.stream().filter(c -> c.getType().equalsIgnoreCase(AUTH))
        .collect(Collectors.toCollection(() -> new ArrayList<oxIDPAuthConf>()));
    return authConfs;
  }

  private List<oxIDPAuthConf> getOxIDPAuthConfs(List<GluuLdapConfiguration> ldapConfigurations) {
    List<oxIDPAuthConf> idpConf = new ArrayList<oxIDPAuthConf>();
    for (GluuLdapConfiguration ldapConfig : ldapConfigurations) {
      if (shouldEncryptPassword(ldapConfig)) {
        ldapConfig.setBindPassword(encrypt(ldapConfig.getBindPassword()));
      }
      if (ldapConfig.isUseAnonymousBind()) {
        ldapConfig.setBindDN(null);
      }

      oxIDPAuthConf ldapConfigIdpAuthConf = new oxIDPAuthConf();
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

  private List<GluuLdapConfiguration> excludeFromConfigurations(List<GluuLdapConfiguration> ldapConfigurations,
      GluuLdapConfiguration ldapConfiguration) {
    boolean hadConfiguration = Iterables.removeIf(ldapConfigurations,
        c -> c.getConfigId().equals(ldapConfiguration.getConfigId()));
    if (!hadConfiguration) {
      throw new NoSuchElementException(ldapConfiguration.getConfigId());
    }
    return ldapConfigurations;
  }

  public static <E> List<E> trimToEmpty(List<E> list) {
    return list == null ? Collections.<E>emptyList() : list;
  }

  private String encrypt(String data) {
    try {
      return encryptionService.encrypt(data);
    } catch (StringEncrypter.EncryptionException ex) {
      throw new GlobalRuntimeException(ex);
    }
  }

  public boolean shouldEncryptPassword(GluuLdapConfiguration ldapConfiguration) {
    try {
      GluuLdapConfiguration oldConfiguration = findByName(ldapConfiguration.getConfigId());
      String encryptedOldPassword = oldConfiguration.getBindPassword();
      return !StringUtils.equals(encryptedOldPassword, ldapConfiguration.getBindPassword());
    } catch (NoSuchElementException ex) {
      return true;
    }
  }

}
