package io.jans.casa.plugins.authnmethod.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.slf4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import io.jans.casa.core.model.Fido2RegistrationEntry;
import io.jans.casa.core.pojo.FidoDevice;
import io.jans.casa.misc.Utils;
import io.jans.casa.rest.RSUtils;
import io.jans.fido2.client.AttestationService;
import io.jans.fido2.model.attestation.AttestationOptions;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import io.jans.orm.search.filter.Filter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;

@Named
@ApplicationScoped
public class Fido2Service extends BaseService {

    @Inject
    private Logger logger;

    private String appId;

    private AttestationService attestationService;

    private static final String FIDO2_OU = "fido2_register";

    @PostConstruct
    private void inited() {
        reloadConfiguration();
    }

    public void reloadConfiguration() {
        props = new JSONObject();
        String issuerUrl = persistenceService.getIssuerUrl();
        String tmp = issuerUrl + "/.well-known/fido2-configuration";
        try {
            appId = new URL(issuerUrl).getHost();
            String attestationURL = mapper.readTree(new URL(tmp)).get("attestation").get("base_path").asText();
            attestationService = RSUtils.getClient().target(attestationURL).proxy(AttestationService.class);
        } catch (Exception e) {
            logger.error("Error loading FIDO2 configuration", e);
        }
    }

    public int getDevicesTotal(String userId, String appId, boolean active) {
        return getDevices(userId, appId, active).size();
    }

    public List<FidoDevice> getDevices(String userId, String appId, boolean active) {
        String state = active ? Fido2RegistrationStatus.registered.getValue() : Fido2RegistrationStatus.pending.getValue();
        Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("jansStatus", state),
                Filter.createEqualityFilter("personInum", userId),
                Filter.createEqualityFilter("jansApp", appId));
        List<FidoDevice> devices = new ArrayList<>();
        try {
            List<Fido2RegistrationEntry> list = persistenceService.find(Fido2RegistrationEntry.class,
                    String.format("ou=%s,%s", FIDO2_OU, persistenceService.getPersonDn(userId)), filter);
            for (Fido2RegistrationEntry entry : list) {
                FidoDevice device = new FidoDevice();
                Set<String> transports = new HashSet<>(Arrays.asList(entry.getRegistrationData().getTransports()));

                device.setId(entry.getId());
                device.setCreationDate(entry.getCreationDate());
                device.setNickName(entry.getDisplayName());
                device.setTransports(transports.toArray(new String[0]));
                devices.add(device);
            }
            return devices.stream().sorted().collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public boolean updateDevice(FidoDevice device) {
        boolean success = false;
        Fido2RegistrationEntry deviceRegistration = getDeviceRegistrationFor(device);
        if (deviceRegistration != null) {
            deviceRegistration.setDisplayName(device.getNickName());
            success = persistenceService.modify(deviceRegistration);
        }
        return success;
    }

    public boolean removeDevice(FidoDevice device, String userId, String appId, boolean active) {
        boolean success = false;
        String state = active ? Fido2RegistrationStatus.registered.getValue() : Fido2RegistrationStatus.pending.getValue();

        Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("jansStatus", state),
                Filter.createEqualityFilter("personInum", userId),
                Filter.createEqualityFilter("jansApp", appId));
        try {
            List<Fido2RegistrationEntry> list = persistenceService.find(Fido2RegistrationEntry.class,
                    String.format("ou=%s,%s", FIDO2_OU, persistenceService.getPersonDn(userId)), filter);
            for (Fido2RegistrationEntry entry : list) {
                if (Utils.isNotEmpty(device.getId()) && Utils.isNotEmpty(entry.getId()) && device.getId().equals(entry.getId())) {
                    success = persistenceService.delete(entry);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception while removing device: {}", e.getMessage());
        }
        return success;
    }

    public boolean removeDevice(FidoDevice device) {
        logger.info("Removing device by ID only: {}", device.getId());
        boolean success = false;
        Fido2RegistrationEntry rentry = getDeviceRegistrationFor(device);
        if (rentry != null) {
            success = persistenceService.delete(rentry);
            logger.info("Device deleted from DB: {}", success);
        }
        return success;
    }

    public String appId() {
        return appId;
    }

    private Fido2RegistrationEntry getDeviceRegistrationFor(FidoDevice device) {
        String id = device.getId();
        logger.info("Looking up registration entry for device id={}", id);
        Fido2RegistrationEntry deviceRegistration = new Fido2RegistrationEntry();
        deviceRegistration.setBaseDn(persistenceService.getPeopleDn());
        deviceRegistration.setId(id);

        List<Fido2RegistrationEntry> list = persistenceService.find(deviceRegistration);
        logger.info(" Found {} matching entries for device id={}", list.size(), id);
        if (list.size() == 1) {
            return list.get(0);
        } else {
            logger.warn("Search for fido2 device registration with jansId {} returned {} results!", id, list.size());
            return null;
        }
    }

    public String doRegister(String userName, String displayName) throws Exception {
        logger.info("Starting doRegister for user={} displayName={}", userName, displayName);
        AttestationOptions attestationOptions = new AttestationOptions();
        attestationOptions.setUsername(userName);
        attestationOptions.setDisplayName(displayName);

        try (Response response = attestationService.register(attestationOptions)) {
            String content = response.readEntity(String.class);
            int status = response.getStatus();
            if (status != Response.Status.OK.getStatusCode()) {
                String msg = "Registration failed (code: " + status + ")";
                logger.error(msg + "; response was: " + content);
                throw new Exception(msg);
            }
            return content;
        }
    }

    public boolean verifyRegistration(String tokenResponse) throws Exception {
        JsonNode jsonObj = mapper.readTree(tokenResponse);
        try (Response response = attestationService.verify(mapper.convertValue(jsonObj, io.jans.fido2.model.attestation.AttestationResult.class))) {
            int status = response.getStatus();
            boolean verified = status == Response.Status.OK.getStatusCode();

            if (!verified) {
                String content = response.readEntity(String.class);
            }
            return verified;
        }
    }

    public FidoDevice getLatestPasskey(String userId, long time) {
        FidoDevice sk = null;
        try {
            List<FidoDevice> list = getDevices(userId, appId(), true);
            sk = FidoService.getRecentlyCreatedDevice(list, time);
            if (sk != null && sk.getNickName() != null) {
                sk = null;
            } else {
                logger.info("Latest device found: {}", sk != null ? sk.getId() : "null");
            }
        } catch (Exception e) {
            logger.error("Exception in getLatestPasskey: {}", e.getMessage(), e);
        }
        return sk;
    }
}
