package io.jans.casa.plugins.strongauthn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.casa.core.model.PersonPreferences;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.strongauthn.conf.Configuration;
import io.jans.casa.plugins.strongauthn.model.TrustedDevice;
import io.jans.casa.plugins.strongauthn.conf.TrustedDevicesSettings;
import io.jans.casa.plugins.strongauthn.model.TrustedOrigin;
import io.jans.casa.service.IPersistenceService;
import io.jans.orm.search.filter.Filter;
import io.jans.service.cache.CacheInterface;
import io.jans.util.security.StringEncrypter;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.jans.casa.plugins.strongauthn.StrongAuthnSettingsPlugin.*;

public class TrustedDevicesSweeper implements Job {

    private static final int QUASI_DAY = (int) (TimeUnit.DAYS.toSeconds(1) - TimeUnit.HOURS.toSeconds(1));
    private final String ACTIVE_INSTANCE_PRESENCE = getClass().getName() + "_activeInstanceSet";
    
    private Logger logger = LoggerFactory.getLogger(getClass());

    private IPersistenceService persistenceService;
    private StringEncrypter stringEncrypter;
    private CacheInterface storeService;

    private long locationExpiration;
    private long deviceExpiration;
    private ObjectMapper mapper;

    public TrustedDevicesSweeper() {

        Configuration settings = StrongAuthSettingsService.instance().getSettingsHandler().getSettings();
        Optional<TrustedDevicesSettings> tsettings = Optional.ofNullable(settings.getTrustedDevicesSettings());
        locationExpiration = TimeUnit.DAYS.toMillis(
                tsettings.map(TrustedDevicesSettings::getLocationExpirationDays).orElse(TRUSTED_LOCATION_EXPIRATION_DAYS));
        deviceExpiration = TimeUnit.DAYS.toMillis(
                tsettings.map(TrustedDevicesSettings::getDeviceExpirationDays).orElse(TRUSTED_DEVICE_EXPIRATION_DAYS));

        try {
            mapper = new ObjectMapper();
            persistenceService = Utils.managedBean(IPersistenceService.class);

            storeService = Utils.managedBean(CacheInterface.class);
            stringEncrypter = Utils.stringEncrypter();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.warn("Device sweeping will not be available");
        }

    }

    public void execute(JobExecutionContext context) throws JobExecutionException {

        if (stringEncrypter == null)
            return;

        //Optimistically, the following two statements avoid the sweeping logic to be executed by
        //several nodes (in a multi node environment) at the same time. This process is expensive
        if (storeService.get(ACTIVE_INSTANCE_PRESENCE) != null) return;

        //temporarily take the ownership for sweeping data
        storeService.put(QUASI_DAY, ACTIVE_INSTANCE_PRESENCE, true);

        logger.info("TrustedDevicesSweeper. Running timer job");
        long now = System.currentTimeMillis();
        List<PersonPreferences> people = getPeopleTrustedDevices();

        for (PersonPreferences person : people) {
            String jsonStr = null;
            try {
                String trustedDevicesInfo = stringEncrypter.decrypt(person.getTrustedDevices());
                List<TrustedDevice> list = mapper.readValue(trustedDevicesInfo, new TypeReference<List<TrustedDevice>>(){});

                if (removeExpiredData(list, now)) {
                    if (list.size() > 0) {
                        //update list
                        jsonStr = mapper.writeValueAsString(list);
                        updateTrustedDevices(person, stringEncrypter.encrypt(jsonStr));
                    } else {
                        updateTrustedDevices(person, null);
                    }
                }
            } catch (Exception e) {
                if (jsonStr == null) {
                    //This may happen when data in jansTrustedDevices attribute could not be parsed (e.g. migration
                    //of gluu version brought change in encryption salt?)
                    updateTrustedDevices(person, null);
                }
                logger.error(e.getMessage(), e);
            }
        }

    }

    private boolean removeExpiredData(List<TrustedDevice> list, long time) {

        boolean changed = false;
        List<Integer> deviceIndexes = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            TrustedDevice device = list.get(i);
            List<TrustedOrigin> origins = device.getOrigins();

            if (origins != null) {
                List<Integer> origIndexes = new ArrayList<>();
                for (int j = 0; j < origins.size(); j++) {
                    long timeStamp = origins.get(j).getTimestamp();

                    if (time - timeStamp > locationExpiration) {
                        origIndexes.add(0, j);
                    }
                }
                //Remove expired ones from the origins. This is a right-to-left removal
                origIndexes.forEach(ind -> origins.remove(ind.intValue()));    //intValue() is important here!
                changed = origIndexes.size() > 0;
            }
            if (device.getAddedOn() > 0 && time - device.getAddedOn() > deviceExpiration) {
                deviceIndexes.add(0, i);
                changed = true;
            }
        }
        //Right-to-left removal of expired devices
        deviceIndexes.forEach(ind -> list.remove(ind.intValue()));
        return changed;

    }

    private List<PersonPreferences> getPeopleTrustedDevices() {

        List<PersonPreferences> list = new ArrayList<>();
        try {
            list = persistenceService.find(PersonPreferences.class, persistenceService.getPeopleDn(),
                    Filter.createPresenceFilter("jansTrustedDevices"));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return list;

    }

    private void updateTrustedDevices(PersonPreferences person, String value) {

        String uid = person.getUid();
        logger.trace("TrustedDevicesSweeper. Cleaning expired trusted devices for user '{}'", uid);
        person.setTrustedDevices(value);
        persistenceService.modify(person);

    }

}
