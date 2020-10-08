package io.jans.configapi.service;

import com.github.fge.jackson.JacksonUtils;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import io.jans.oxauth.service.common.EncryptionService;
import io.jans.orm.couchbase.model.CouchbaseConnectionConfiguration;
import io.jans.util.security.StringEncrypter;
import org.oxauth.persistence.model.configuration.GluuConfiguration;
import org.oxauth.persistence.model.configuration.oxIDPAuthConf;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class CouchbaseConfService {

    private static final String AUTH = "auth";

    @Inject
    ConfigurationService configurationService;

    @Inject
    private EncryptionService encryptionService;

    public List<CouchbaseConnectionConfiguration> findAll() {
        return getOxIDPAuthConf().stream().filter(c -> c.asCouchbaseConfiguration() != null).map(oxIDPAuthConf::asCouchbaseConfiguration).collect(Collectors.toList());
    }

    public void save(CouchbaseConnectionConfiguration conf) {
        save(Lists.newArrayList(conf));
    }

    public void save(List<CouchbaseConnectionConfiguration> confs) {
        GluuConfiguration configuration = configurationService.findGluuConfiguration();

        configuration.setOxIDPAuthentication(getOrCreateOxIDPAuthConfs(configuration.getOxIDPAuthentication(), confs));
        configurationService.merge(configuration);
    }

    public void remove(String name) {
        final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();
        final List<oxIDPAuthConf> existing = gluuConfiguration.getOxIDPAuthentication();
        Optional<oxIDPAuthConf> existingConf = existing.stream().filter(o -> o.getName() != null && o.getName().equals(name)).findFirst();
        if (existingConf.isEmpty())
            return; // does not exist, nothing to remove

        existing.remove(existingConf.get());
        gluuConfiguration.setOxIDPAuthentication(existing);

        configurationService.merge(gluuConfiguration);
    }

    public Optional<CouchbaseConnectionConfiguration> findByName(String name) {
        final List<CouchbaseConnectionConfiguration> all = findAll();
        return all.stream().filter(d -> d != null && d.getConfigId() != null && d.getConfigId().equals(name)).findFirst();
    }

    private List<oxIDPAuthConf> getOxIDPAuthConf() {
        List<oxIDPAuthConf> idpConfList = configurationService.findGluuConfiguration().getOxIDPAuthentication();
        if (idpConfList == null) {
            return Lists.newArrayList();
        }
        return idpConfList;
    }

    private List<oxIDPAuthConf> getOrCreateOxIDPAuthConfs(List<oxIDPAuthConf> existing, List<CouchbaseConnectionConfiguration> confs) {
        if (existing == null) {
            existing = Lists.newArrayList();
        }

        for (CouchbaseConnectionConfiguration conf : confs) {
            Optional<oxIDPAuthConf> existingConf = existing.stream().filter(o -> o.getName() != null && o.getName().equals(conf.getConfigId())).findFirst();

            final oxIDPAuthConf idpConf;
            if (existingConf.isEmpty()) {
                idpConf = new oxIDPAuthConf();
                existing.add(idpConf);
            } else {
                idpConf = existingConf.get();
            }

            if (shouldEncryptPassword(conf)) {
                try {
                    conf.setUserPassword(encryptionService.encrypt(conf.getUserPassword()));
                } catch (StringEncrypter.EncryptionException e) {
                    throw new RuntimeException("Unable to decrypt password.", e);
                }
            }

            idpConf.setType(AUTH);
            idpConf.setVersion(idpConf.getVersion() + 1);
            idpConf.setName(conf.getConfigId());
            idpConf.setEnabled(true);
            idpConf.setConfig(JacksonUtils.newMapper().valueToTree(conf));
        }
        return existing;
    }

    private boolean shouldEncryptPassword(CouchbaseConnectionConfiguration conf) {
        Optional<CouchbaseConnectionConfiguration> oldConfiguration = findByName(conf.getConfigId());
        if (oldConfiguration.isEmpty()) {
            return false;
        }

        String encryptedOldPassword = oldConfiguration.get().getUserPassword();
        return !StringUtils.equals(encryptedOldPassword, conf.getUserPassword());
    }
}
