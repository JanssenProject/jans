/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import com.github.fge.jackson.JacksonUtils;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.as.persistence.model.configuration.IDPAuthConf;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class LdapConfigurationService {

    private static final String AUTH = "auth";

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @Inject
    private EncryptionService encryptionService;

    public List<GluuLdapConfiguration> findLdapConfigurations() {
        return getIDPAuthConf().stream().map(IDPAuthConf::asLdapConfiguration).collect(Collectors.toList());
    }

    public void save(GluuLdapConfiguration ldapConfiguration) {
        List<GluuLdapConfiguration> ldapConfigurations = new ArrayList<GluuLdapConfiguration>(findLdapConfigurations());
        ldapConfigurations.add(ldapConfiguration);
        save(ldapConfigurations);
    }

    public void save(List<GluuLdapConfiguration> ldapConfigurations) {
        GluuConfiguration configuration = configurationService.findGluuConfiguration();
        configuration.setIdpAuthn(getIDPAuthConfs(ldapConfigurations));
        configurationService.merge(configuration);
    }

    public void update(GluuLdapConfiguration ldapConfiguration) {
        List<GluuLdapConfiguration> ldapConfigurations = excludeFromConfigurations(
                new ArrayList<>(findLdapConfigurations()), ldapConfiguration);
        ldapConfigurations.add(ldapConfiguration);

        save(ldapConfigurations);
    }

    public void remove(String name) {
        GluuLdapConfiguration toRemove = findByName(name);
        List<GluuLdapConfiguration> allConfiguration = new ArrayList<GluuLdapConfiguration>(findLdapConfigurations());
        List<GluuLdapConfiguration> newConfigurations = excludeFromConfigurations(allConfiguration, toRemove);
        save(newConfigurations);
    }

    public GluuLdapConfiguration findByName(String name) {
        List<GluuLdapConfiguration> ldapConfigurations = findLdapConfigurations();
        log.debug(" findByName name = name " + name + " ldapConfigurations = " + ldapConfigurations);

        Optional<GluuLdapConfiguration> matchingLdapConfiguration = ldapConfigurations.stream()
                .filter(d -> d.getConfigId().equals(name)).findFirst();
        return matchingLdapConfiguration.get();
    }

    private List<IDPAuthConf> getIDPAuthConf() {
        List<IDPAuthConf> idpConfList = configurationService.findGluuConfiguration().getIdpAuthn();
        if (idpConfList == null) {
            return Lists.newArrayList();
        }
        return idpConfList.stream().filter(c -> c.getType().equalsIgnoreCase(AUTH))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<IDPAuthConf> getIDPAuthConfs(List<GluuLdapConfiguration> ldapConfigurations) {
        List<IDPAuthConf> idpConf = new ArrayList<IDPAuthConf>();
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

            IDPAuthConf ldapConfigIdpAuthConf = new IDPAuthConf();
            ldapConfig.updateStringsLists();
            ldapConfigIdpAuthConf.setType(AUTH);
            ldapConfigIdpAuthConf.setVersion(ldapConfigIdpAuthConf.getVersion() + 1);
            ldapConfigIdpAuthConf.setName(ldapConfig.getConfigId());
            ldapConfigIdpAuthConf.setEnabled(ldapConfig.isEnabled());
            ldapConfigIdpAuthConf.setConfig(JacksonUtils.newMapper().valueToTree(ldapConfig));

            idpConf.add(ldapConfigIdpAuthConf);
        }
        return idpConf;
    }

    private List<GluuLdapConfiguration> excludeFromConfigurations(List<GluuLdapConfiguration> ldapConfigurations,
            GluuLdapConfiguration ldapConfiguration) {
        log.debug("\n\n\n excludeFromConfigurations ldapConfigurations = " + ldapConfigurations
                + " , ldapConfiguration = " + ldapConfiguration + "\n\n\n");
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
