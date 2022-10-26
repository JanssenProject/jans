package io.jans.configapi.plugin.fido2.service;

import com.google.api.client.util.Lists;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.List;

@ApplicationScoped
@Named("userFido2Srv")
public class UserFido2Service extends io.jans.as.common.service.common.UserService {
    @Override
    public List<String> getPersonCustomObjectClassList() {
        return Lists.newArrayList();
    }

    @Override
    public String getPeopleBaseDn() {
        return null;
    }
}
