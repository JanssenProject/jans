package io.jans.configapi.service.auth;

import com.github.fge.jackson.JacksonUtils;
import com.google.common.collect.Lists;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.as.persistence.model.configuration.IDPAuthConf;
import io.jans.orm.sql.model.SqlConnectionConfiguration;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang3.StringUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class SqlConfService {

    private static final String AUTH = "auth";

    @Inject
    ConfigurationService configurationService;

    @Inject
    private EncryptionService encryptionService;

    public List<SqlConnectionConfiguration> findAll() {
        return getIDPAuthConf().stream().filter(c -> c.asSqlConfiguration() != null)
                .map(IDPAuthConf::asSqlConfiguration).collect(Collectors.toList());
    }

    public void save(SqlConnectionConfiguration conf) {
        save(Lists.newArrayList(conf));
    }

    public void save(List<SqlConnectionConfiguration> confs) {
        GluuConfiguration configuration = configurationService.findGluuConfiguration();

        configuration.setIdpAuthn(getOrCreateIDPAuthConfs(configuration.getIdpAuthn(), confs));
        configurationService.merge(configuration);
    }

    public void remove(String name) {
        final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();
        final List<IDPAuthConf> existing = gluuConfiguration.getIdpAuthn();
        Optional<IDPAuthConf> existingConf = existing.stream()
                .filter(o -> o.getName() != null && o.getName().equals(name)).findFirst();
        if (!existingConf.isPresent())
            return; // does not exist, nothing to remove

        existing.remove(existingConf.get());
        gluuConfiguration.setIdpAuthn(existing);

        configurationService.merge(gluuConfiguration);
    }

    public Optional<SqlConnectionConfiguration> findByName(String name) {
        final List<SqlConnectionConfiguration> all = findAll();
        return all.stream().filter(d -> d != null && d.getConfigId() != null && d.getConfigId().equals(name))
                .findFirst();
    }

    private List<IDPAuthConf> getIDPAuthConf() {
        List<IDPAuthConf> idpConfList = configurationService.findGluuConfiguration().getIdpAuthn();
        if (idpConfList == null) {
            return Lists.newArrayList();
        }
        return idpConfList;
    }

    private List<IDPAuthConf> getOrCreateIDPAuthConfs(List<IDPAuthConf> existing,
            List<SqlConnectionConfiguration> confs) {
        if (existing == null) {
            existing = Lists.newArrayList();
        }

        for (SqlConnectionConfiguration conf : confs) {
            Optional<IDPAuthConf> existingConf = existing.stream()
                    .filter(o -> o.getName() != null && o.getName().equals(conf.getConfigId())).findFirst();

            final IDPAuthConf idpConf;
            if (!existingConf.isPresent()) {
                idpConf = new IDPAuthConf();
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

    private boolean shouldEncryptPassword(SqlConnectionConfiguration conf) {
        Optional<SqlConnectionConfiguration> oldConfiguration = findByName(conf.getConfigId());
        if (!oldConfiguration.isPresent()) {
            return false;
        }

        String encryptedOldPassword = oldConfiguration.get().getUserPassword();
        return !StringUtils.equals(encryptedOldPassword, conf.getUserPassword());
    }
}
