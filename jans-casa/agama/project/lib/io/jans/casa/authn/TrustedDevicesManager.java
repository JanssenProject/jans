package io.jans.casa.authn;

import io.jans.as.common.model.common.User;
import io.jans.as.server.service.*;
import io.jans.service.EncryptionService;
import io.jans.service.cdi.util.CdiUtil;

import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustedDevicesManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TrustedDevicesManager.class);
    private static EncryptionService encService = CdiUtil.bean(EncryptionService.class);

    private String jsonLocation;
    private String jsonDevice;
    private User user;
    
    private JSONArray trustedDevices;
    
    private JSONObject device;
    private JSONObject location;
    
    public TrustedDevicesManager(User user, String jsonDevice, String jsonLocation) {

        this.user = user;
        this.jsonDevice = jsonDevice;
        this.jsonLocation = jsonLocation;
        trustedDevices = getTrustedDevices();
        
        try {
            device = createDevice();
            location = createLocation();  
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    public boolean knownDevice() {

        if (device == null) return false; 
        logger.debug("Checking if user's device is known");

        // Additional validation to prevent false positives
        if (!isDeviceDataValid()) {
            logger.debug("Device data validation failed, treating as unknown device");
            return false;
        }

        return findDevice(device, trustedDevices) != -1;

    }

    /**
     * Validates that the device data is complete and reliable for matching
     */
    private boolean isDeviceDataValid() {
        try {
            String browserName = device.optString("name");
            String browserVersion = device.optString("version");
            String osName = device.optString("osName");
            String osVersion = device.optString("osVersion");
            
            // Check if browser information is complete
            if (browserName == null || browserName.trim().isEmpty() || 
                browserVersion == null || browserVersion.trim().isEmpty()) {
                logger.debug("Browser information incomplete: name={}, version={}", browserName, browserVersion);
                return false;
            }
            
            // Check if OS information is complete
            if (osName == null || osName.trim().isEmpty() || 
                osVersion == null || osVersion.trim().isEmpty()) {
                logger.debug("OS information incomplete: name={}, version={}", osName, osVersion);
                return false;
            }
            
            // Check for fallback/placeholder values that indicate incomplete data
            if (isFallbackVersion(browserVersion) || isFallbackVersion(osVersion)) {
                logger.debug("Device data contains fallback values: browser={}, os={}", browserVersion, osVersion);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.debug("Error validating device data: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a version string represents a fallback/placeholder value
     */
    private boolean isFallbackVersion(String version) {
        if (version == null) return true;
        
        String v = version.trim().toLowerCase();
        return v.isEmpty() || 
               v.equals("0.0.0") || 
               v.equals("unknown") || 
               v.equals("undefined") ||
               v.equals("null") ||
               v.equals("n/a") ||
               v.equals("0") ||
               v.equals("1.0.0") ||
               v.equals("0.0");
    }

    public boolean knownLocation() {

        if (location == null) return false;
        logger.debug("Checking if user's location is known");
        
        try { 
            Iterator<Object> it = trustedDevices.iterator();
            boolean match = false;
            
            while (!match && it.hasNext()) {
                //In practice, trustedDevice has the form of an io.jans.casa.plugins.strongauthn.model.TrustedDevice
                JSONObject trustedDevice = (JSONObject) it.next();
                JSONArray origins = trustedDevice.optJSONArray("origins");
                
                if (origins != null) {            
                    match = findLocation(location, origins) != -1;
                }
            }
            return match;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

    }
    
    public void updateDevices() throws Exception {

        if (device == null) return;
        
        boolean validLocation = location != null;
        logger.debug("Current location is{} considered valid", validLocation ? "": " NOT");
        
        int i = findDevice(device, trustedDevices);
        long now = System.currentTimeMillis();
        
        if (i == -1) {
            logger.debug("New device detected - will be added to trusted list");
            List<Map<String, Object>> orgies;

            if (validLocation) {
                orgies = Collections.singletonList(Map.of("timestamp", now, 
                        "city", location.optString("city"), "country", location.optString("country")));
            } else {
                orgies = Collections.emptyList();
            }
            
            //add device to the list
            trustedDevices.put(Map.of(
                "addedOn", now,
                "browser", Map.of("name", device.optString("name"), "version", device.optString("version")),
                "os", Map.of("family", device.optString("osName"), "version", device.optString("osVersion")),
                "origins", orgies                      
            ));

        } else if (validLocation) {
            boolean add = true;
            JSONArray origins = trustedDevices.getJSONObject(i).optJSONArray("origins");
            
            if (origins == null) {
                origins = new JSONArray();
                trustedDevices.getJSONObject(i).put("origins", origins); 
            } else {
                i = findLocation(location, origins);
                
                if (i == -1) {
                    logger.debug("New location detected - will be added to trusted origins");
                } else {
                    add = false;
                    
                    Optional.ofNullable(origins.optJSONObject(i))
                            .ifPresent(origin -> origin.put("timestamp", now));
                }
            }

            if (add) {
                //add location to this device
                origins.put(Map.of(
                    "timestamp", now,
                    "city", location.optString("city"),
                    "country", location.optString("country")
                ));
            }
        }

        String trustedStr = trustedDevices.toString();
        
        logger.debug("List of trusted devices {}", trustedStr);
        String encTrustedStr = encService.encrypt(trustedStr);
        
        UserService uss = CdiUtil.bean(UserService.class);
        //boolean succ = uss.replaceUserAttribute(user.getUserId(), "jansTrustedDevices",
        //            current, encTrustedStr, false);
        user.setAttribute("jansTrustedDevices", encTrustedStr);
        boolean succ = uss.updateUser(user);
        logger.info("List of trusted devices was{} updated successfully", succ ? "": " NOT");
    
    }
    
    private JSONArray getTrustedDevices() {

        String encDevices = user.getAttribute("jansTrustedDevices");
    
        try {
            if (encDevices != null) return new JSONArray(encService.decrypt(encDevices));            
        } catch (Exception e) {
            logger.error("Error decrypting trusted devices for user {}", user.getAttribute("inum"));
            logger.error(e.getMessage());
            logger.error(encDevices);
        }
        return new JSONArray();

    }

    //location param is a map with keys: country, state, city, zone
    //origins param has the form of a List<io.jans.casa.plugins.strongauthn.model.TrustedOrigin>    
    private int findLocation(JSONObject location, JSONArray origins) {

        Iterator<Object> it = origins.iterator();
        boolean match = false;
        int idx = -1;
        
        while (!match && it.hasNext()) {
            idx++;
            JSONObject origin = (JSONObject) it.next();
            
            match = origin != null && origin.optString("country").equalsIgnoreCase(location.optString("country"))
                    && origin.optString("city").equalsIgnoreCase(location.optString("city"));
        }
        return match ? idx : -1;

    }
    
    //device is a map with keys name, version, osName, osVersion (see main.ftlh#fillPlatformData)    
    //devices param has the form of a List<io.jans.casa.plugins.strongauthn.model.TrustedDevice>
    private static int findDevice(JSONObject device, JSONArray devices) {

        Iterator<Object> it = devices.iterator();
        boolean match = false;
        int idx = -1;
        
        while (!match && it.hasNext()) {
            idx++;
            JSONObject trustedDevice = (JSONObject) it.next();
            
            if (device.optString("name").equalsIgnoreCase(browserName(trustedDevice))) {
                JSONObject trustedOS = trustedDevice.optJSONObject("os");
                
                match = trustedOS != null && device.optString("osName").equalsIgnoreCase(trustedOS.optString("family"))
                        && device.optString("osVersion").equalsIgnoreCase(trustedOS.optString("version"));
            }
        }
        return match ? idx : -1;

    }
    
    private JSONObject createDevice() throws Exception {
        
        JSONObject d = new JSONObject(jsonDevice);

        if (d.optString("name").length() > 0 && d.optString("osName").length() > 0) return d;
        
        throw new Exception("Device " + jsonDevice + " is not valid");

    }
    
    private JSONObject createLocation() throws Exception {
        
        JSONObject loca = new JSONObject(jsonLocation);
        
        if (loca.optString("city").length() > 0 && loca.optString("country").length() > 0) return loca;
        
        throw new Exception("Location " + jsonLocation + " is not valid");
    }
    
    private static String browserName(JSONObject device) {
        return Optional.ofNullable(device.optJSONObject("browser"))
                    .map(jo -> jo.optString("name")).orElse(null);
    }
    
}
