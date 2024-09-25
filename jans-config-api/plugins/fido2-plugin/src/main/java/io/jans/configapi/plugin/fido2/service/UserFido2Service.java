package io.jans.configapi.plugin.fido2.service;

import io.jans.as.model.config.StaticConfiguration;
import com.google.api.client.util.Lists;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;

@ApplicationScoped
@Named("userFido2Srv")
public class UserFido2Service extends io.jans.as.common.service.common.UserService {
    
    @Inject
    private StaticConfiguration staticConfiguration;
    
    @Override
    public List<String> getPersonCustomObjectClassList() {
        return Lists.newArrayList();
    }

    @Override
    public String getPeopleBaseDn() {
        return staticConfiguration.getBaseDn().getPeople();
    }
}
