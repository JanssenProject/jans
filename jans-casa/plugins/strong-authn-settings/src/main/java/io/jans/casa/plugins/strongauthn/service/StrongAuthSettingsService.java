package io.jans.casa.plugins.strongauthn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.casa.core.model.PersonPreferences;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.strongauthn.conf.EnforcementPolicy;
import io.jans.casa.plugins.strongauthn.conf.Configuration;
import io.jans.casa.plugins.strongauthn.model.TrustedDevice;
import io.jans.casa.plugins.strongauthn.model.TrustedDeviceComparator;
import io.jans.casa.service.IPersistenceService;
import io.jans.casa.service.settings.IPluginSettingsHandler;
import io.jans.casa.service.settings.IPluginSettingsHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.Pair;

import java.util.*;
import java.util.stream.Stream;

public class StrongAuthSettingsService {

    private Logger logger;
    private IPersistenceService persistenceService;
    private ObjectMapper mapper;
    private IPluginSettingsHandler<Configuration> settingsHandler;

    private static StrongAuthSettingsService instance;

    private StrongAuthSettingsService(String pluginId) throws Exception {

        persistenceService = Utils.managedBean(IPersistenceService.class);
        mapper = new ObjectMapper();
        logger = LoggerFactory.getLogger(getClass());
        settingsHandler = Utils.managedBean(IPluginSettingsHandlerFactory.class).getHandler(pluginId, Configuration.class);

    }

    public static StrongAuthSettingsService instance() {
        return instance;
    }

    public IPluginSettingsHandler<Configuration> getSettingsHandler() {
        return settingsHandler;
    }

    public static StrongAuthSettingsService instance(String pluginId) throws Exception {
        if (instance == null && pluginId != null) {
            instance = new StrongAuthSettingsService(pluginId);
        }
        return instance;
    }

    public Pair<Set<String>, List<TrustedDevice>> get2FAPolicyData(String userId) {

        Set<String> list = new HashSet<>();
        List<TrustedDevice> trustedDevices = new ArrayList<>();
        try {
            PersonPreferences person = personPreferencesInstance(userId);
            String policy = person.getStrongAuthPolicy();

            if (Utils.isNotEmpty(policy)) {
                Stream.of(policy.split(",\\s*")).forEach(str -> {
                    try {
                        EnforcementPolicy.valueOf(str);
                        list.add(str);
                    } catch (Exception e) {
                        logger.error("The policy '{}' is not recognized", str);
                    }
                });
            }

            if (Utils.isNotEmpty(person.getTrustedDevices())) {
                String trustedDevicesInfo = Utils.stringEncrypter().decrypt(person.getTrustedDevices());
                
                if (Utils.isNotEmpty(trustedDevicesInfo)) {
                    trustedDevices = mapper.readValue(trustedDevicesInfo, new TypeReference<List<TrustedDevice>>() { });
                    trustedDevices.forEach(TrustedDevice::sortOriginsDescending);
    
                    TrustedDeviceComparator comparator = new TrustedDeviceComparator(true);
                    trustedDevices.sort((first, second) -> comparator.compare(second, first));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return new Pair<>(list, trustedDevices);

    }

    public boolean update2FAPolicies(String userId, Set<String> policies) {

        boolean updated = false;
        String str = policies.stream().reduce("", (partial, next) -> partial + ", " + next);
        try {
            PersonPreferences person = personPreferencesInstance(userId);
            person.setStrongAuthPolicy(str.substring(2));
            updated = persistenceService.modify(person);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return updated;

    }

    public boolean deleteTrustedDevice(String userId, List<TrustedDevice> devices, int index) {

        boolean updated = false;
        List<TrustedDevice> copyOfDevices = new ArrayList<>(devices);
        try {
            copyOfDevices.remove(index);
            String updatedJson = Utils.stringEncrypter().encrypt(mapper.writeValueAsString(copyOfDevices));

            PersonPreferences person = personPreferencesInstance(userId);
            person.setTrustedDevices(updatedJson);
            if (persistenceService.modify(person)) {
                devices.remove(index);
                updated = true;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return updated;

    }

    private PersonPreferences personPreferencesInstance(String id) {
        return persistenceService.get(PersonPreferences.class, persistenceService.getPersonDn(id));
    }

}
