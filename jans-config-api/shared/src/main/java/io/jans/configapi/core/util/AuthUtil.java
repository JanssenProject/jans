package io.jans.configapi.core.util;

import io.jans.as.common.model.registration.Client;
import io.jans.configapi.core.service.ConfService;
import io.jans.configapi.core.service.ClientService;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class AuthUtil {

    @Inject
    Logger log;

    @Inject
    ConfService confService;

    @Inject
    ClientService cltSrv;

    public String getOpenIdConfigurationEndpoint() {
        return this.confService.find().getOpenIdConfigurationEndpoint();
    }

    public String getIssuer() {
        return this.confService.find().getIssuer();
    }

    public Client getClient(String clientId) {
        return cltSrv.getClientByInum(clientId);
    }

    public List<String> findMissingElements(List<String> list1, List<String> list2) {
        return list1.stream().filter(e -> !list2.contains(e)).collect(Collectors.toList());
    }

    public boolean isEqualCollection(List<String> list1, List<String> list2) {
        return CollectionUtils.isEqualCollection(list1, list2);
    }

}
