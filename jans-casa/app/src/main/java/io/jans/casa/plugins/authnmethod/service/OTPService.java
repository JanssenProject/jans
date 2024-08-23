package io.jans.casa.plugins.authnmethod.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lochbridge.oath.otp.keyprovisioning.OTPKey;
import io.jans.casa.core.model.PersonOTP;
import io.jans.casa.core.pojo.OTPDevice;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.OTPExtension;
import io.jans.casa.plugins.authnmethod.conf.OTPConfig;
import io.jans.casa.plugins.authnmethod.conf.otp.HOTPConfig;
import io.jans.casa.plugins.authnmethod.conf.otp.TOTPConfig;
import io.jans.casa.plugins.authnmethod.service.otp.*;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;

@Named
@ApplicationScoped
public class OTPService extends BaseService {

    private static final String TOTP_PREFIX="totp:";
    private static final String HOTP_PREFIX="hotp:";

    @Inject
    private Logger logger;

    @Inject
    private TOTPAlgorithmService tAS;

    @Inject
    private HOTPAlgorithmService hAS;

    private OTPConfig conf;

    @PostConstruct
    private void inited() {
        reloadConfiguration();
    }

    public OTPConfig getConf() {
        return conf;
    }

    public void reloadConfiguration() {

        String acr = OTPExtension.ACR;
        props = persistenceService.getAgamaFlowConfigProperties(acr);
        if (props == null) {
            logger.warn("Config. properties for flow '{}' could not be read", acr);
        } else {
            conf = OTPConfig.get(props);
            hAS.init((HOTPConfig) Utils.cloneObject(conf.getHotp()), conf.getIssuer());            
            tAS.init((TOTPConfig) Utils.cloneObject(conf.getTotp()), conf.getIssuer());
        }

    }

    public int getDevicesTotal(String userId) {

        int total = 0;
        try {
            PersonOTP person = persistenceService.get(PersonOTP.class, persistenceService.getPersonDn(userId));
            total = (int) person.getExternalUids().stream()
                    .filter(uid -> uid.startsWith(TOTP_PREFIX) || uid.startsWith(HOTP_PREFIX)).count();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return total;

    }

    public List<OTPDevice> getDevices(String userId) {

        List<OTPDevice> devices = new ArrayList<>();
        try {
            PersonOTP person = persistenceService.get(PersonOTP.class, persistenceService.getPersonDn(userId));
            String json = person.getOTPDevices();
            json = Utils.isEmpty(json) ? "[]" : mapper.readTree(json).get("devices").toString();

            List<OTPDevice> devs = mapper.readValue(json, new TypeReference<List<OTPDevice>>() { });
            devices = person.getExternalUids().stream().filter(uid -> uid.startsWith(TOTP_PREFIX) || uid.startsWith(HOTP_PREFIX))
                    .map(uid -> getExtraOTPInfo(uid, devs)).sorted().collect(Collectors.toList());
            logger.trace("getDevices. User '{}' has {}", userId, devices.stream().map(OTPDevice::getId).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return devices;

    }

    public boolean updateDevicesAdd(String userId, List<OTPDevice> devices, OTPDevice newDevice) {

        boolean success = false;
        try {
            List<OTPDevice> vdevices = new ArrayList<>(devices);
            if (newDevice != null) {
                vdevices.add(newDevice);
            }
            List<String> uids = vdevices.stream().map(OTPDevice::getUid).collect(Collectors.toList());
            String json = uids.size() > 0 ? mapper.writeValueAsString(Collections.singletonMap("devices", vdevices)) : null;

            logger.debug("Updating otp devices for user '{}'", userId);
            PersonOTP person = persistenceService.get(PersonOTP.class, persistenceService.getPersonDn(userId));
            person.setOTPDevices(json);

            //This helps prevent "losing" data (e.g. entries prefixed with passport)
            List<String> alluids = new ArrayList<>(
                    person.getExternalUids().stream().filter(uid -> !uid.startsWith(TOTP_PREFIX) && !uid.startsWith(HOTP_PREFIX))
                            .collect(Collectors.toList()));
            alluids.addAll(uids);
            person.setExternalUids(alluids);

            success = persistenceService.modify(person);
            if (success && newDevice != null) {
                devices.add(newDevice);
                logger.debug("Added {}", newDevice.getNickName());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return success;

    }

    public boolean addDevice(String userId, OTPDevice newDevice) {
        return updateDevicesAdd(userId, getDevices(userId), newDevice);
    }
    
    public void updateMovingFactor(String userId, OTPDevice device, int movingFactor) {
    
        int cmf = device.currentMovingFactor();
        if (cmf == -1) return;
        
        logger.debug("Updating hotp device for user '{}'", userId);
        String uid = device.getUid();
        PersonOTP person = persistenceService.get(PersonOTP.class, persistenceService.getPersonDn(userId));
        
        List<String> uids = person.getExternalUids();//new ArrayList<>(); 
        int i = uids.indexOf(uid);
        if (i == -1) {
            logger.warn("Device {} not found", uid);
        } else {
            int j = uid.lastIndexOf("" + cmf);

            if (j != -1) {
                uids.set(i, uid.substring(0, j) + movingFactor);
                //person.setExternalUids(uids);

                if (!persistenceService.modify(person)) {                
                    logger.error("Unable to update moving factor for device {}", uid);
                }
            }
        }
            
    }

    /**
     * Creates an instance of OTPDevice by looking up in the list of OTPDevices passed. If the item is not found in the
     * in the list, it means the device was previously enrolled by using a different application. In this case the resulting
     * object will not have properties like nickname, etc. Just a basic ID
     * @param uid Identifier of an OTP device (LDAP attribute "oxExternalUid" inside a user entry)
     * @param list List of existing OTP devices enrolled. Ideally, there is an item here corresponding to the uid passed
     * @return OTPDevice object
     */
    private OTPDevice getExtraOTPInfo(String uid, List<OTPDevice> list) {
        //Complements current otp device with extra info in the list if any

        OTPDevice device = new OTPDevice(uid);
        int hash = device.getId();

        Optional<OTPDevice> extraInfoOTP = list.stream().filter(dev -> dev.getId() == hash).findFirst();
        if (extraInfoOTP.isPresent()) {
            OTPDevice extraInfo = extraInfoOTP.get();
            device.setAddedOn(extraInfo.getAddedOn());
            device.setNickName(extraInfo.getNickName());
            device.setSoft(extraInfo.getSoft());
        }
        device.setTimeBased(uid.startsWith(TOTP_PREFIX));
        return device;

    }

    public IOTPAlgorithm getAlgorithmService(OTPKey.OTPType type) {

        switch (type) {
            case HOTP:                
                return hAS;
            case TOTP:
                return tAS;
            default:
                return null;
        }

    }

}
