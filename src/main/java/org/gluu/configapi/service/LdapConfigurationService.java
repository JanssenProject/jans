package org.gluu.configapi.service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gluu.model.ldap.GluuLdapConfiguration;
import org.gluu.oxauth.service.common.EncryptionService;
import org.gluu.util.security.StringEncrypter;
import org.oxauth.persistence.model.configuration.GluuConfiguration;
import org.oxauth.persistence.model.configuration.oxIDPAuthConf;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class LdapConfigurationService {

    private static final String AUTH = "auth";

    @Inject
    ConfigurationService configurationService;

    @Inject
    private EncryptionService encryptionService;

  public List<GluuLdapConfiguration> findLdapConfigurations() {
      return getOxIDPAuthConf().stream().map(oxIDPAuthConf::getConfig).collect(Collectors.toList());
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
        new ArrayList<>(findLdapConfigurations()), ldapConfiguration);
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
      return matchingLdapConfiguration.get();
  }

    private List<oxIDPAuthConf> getOxIDPAuthConf() {
        List<oxIDPAuthConf> idpConfList = configurationService.findGluuConfiguration().getOxIDPAuthentication();
        if (idpConfList == null) {
            return Lists.newArrayList();
        }
        return idpConfList.stream().filter(c -> c.getType().equalsIgnoreCase(AUTH)).collect(Collectors.toCollection(ArrayList::new));
    }

  private List<oxIDPAuthConf> getOxIDPAuthConfs(List<GluuLdapConfiguration> ldapConfigurations) {
    List<oxIDPAuthConf> idpConf = new ArrayList<oxIDPAuthConf>();
    for (GluuLdapConfiguration ldapConfig : ldapConfigurations) {
      if (shouldEncryptPassword(ldapConfig)) {
          try {
              ldapConfig.setBindPassword(encryptionService.encrypt(ldapConfig.getBindPassword()));
          } catch (StringEncrypter.EncryptionException e) {
              throw new RuntimeException("Unable to decrypt password.", e);
          }
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
