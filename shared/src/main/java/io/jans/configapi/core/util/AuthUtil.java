package io.jans.configapi.core.util;

import io.jans.as.common.model.registration.Client;
import io.jans.configapi.core.service.ConfService;
import io.jans.configapi.core.service.ClientService;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class AuthUtil {

    @Inject
    Logger log;

    @Inject
    ConfService confService;

    @Inject
    ClientService clientService;

    public String getOpenIdConfigurationEndpoint(String dn) {
        return this.confService.find(dn).getOpenIdConfigurationEndpoint();
    }

    public String getIssuer(String dn) {
        return this.confService.find(dn).getIssuer();
    }

    public Client getClient(String clientId) {
        return clientService.getClientByInum(clientId);
    }

    public List<String> findMissingElements(List<String> list1, List<String> list2) {
        List<String> unavailable = list1.stream().filter(e -> !list2.contains(e)).collect(Collectors.toList());
        return unavailable;
    }

    public boolean isEqualCollection(List<String> list1, List<String> list2) {
        return CollectionUtils.isEqualCollection(list1, list2);
    }

}
