package io.jans.casa.notify;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.as.common.service.common.fido2.RegistrationPersistenceService;
import io.jans.as.server.service.push.sns.PushSnsService;
import io.jans.notify.client.NotifyClientFactory;
import io.jans.notify.client.NotifyClientService;
import io.jans.notify.model.NotificationResponse;
import io.jans.notify.model.NotifyMetadata;
import io.jans.notify.model.RegisterDeviceResponse;
import io.jans.orm.model.fido2.Fido2DeviceData;
import io.jans.orm.model.fido2.Fido2DeviceNotificationConf;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.StringHelper;

import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SGNotificationsUtil {
    
    private static final String JANS_KEY = "jans";
    private static final String SCAN_SCOPE = "https://api.gluu.org/auth/scopes/scan.supergluu";
    private static final Logger logger = LoggerFactory.getLogger(SGNotificationsUtil.class);
    
    private static SGNotificationsUtil instance;

    private JSONObject settings;
    private NotifyClientService jansClient;
    
    private int confHash = -1;
    private boolean androidEnabled;
    private boolean iosEnabled;
    private String androidPlatformId;
    private String iosPlatformId;
    
    private SGNotificationsUtil() { }
    
    public static SGNotificationsUtil getInstance() {
        if (instance == null) {
            instance = new SGNotificationsUtil();
        }
        return instance;
    }
    
    public void send(String inum, String appId, String request) throws Exception {

        //It's easier to re-read the configuration every time than adding logic to detect changes
        settings = Configuration.get();
        int hash = settings.toString().hashCode();
        if (hash != confHash) {
            confHash = hash;

            if (!reloadConf()) {
                logger.error("Error loading the notifications configuration, aborting");
                return;
            }
        }
                
        User user = getUser(inum);
        if (user == null) {
            logger.error("Unable to retrieve user entry " + inum);
            return;
        }

        PushSnsService pushSnsService = CdiUtil.bean(PushSnsService.class);
        String title = "Super Gluu";
        String body = "Confirm your sign in request";

        RegistrationPersistenceService rps = CdiUtil.bean(RegistrationPersistenceService.class);
        List<Fido2RegistrationEntry> devices = rps.findByRpRegisteredUserDevices(
                    user.getUserId(), appId, "jansId", "jansApp", "jansDeviceData", "jansDeviceNotificationConf")
                .stream().filter(d -> d.getDeviceData() != null)
                .collect(Collectors.toList());
        
        logger.info("User {} has {} super-gluu devices registered", inum, devices.size());
        NotificationResponse response;
        for (Fido2RegistrationEntry device : devices) {
            
            Fido2DeviceData deviceData = device.getDeviceData();
            String deviceId = device.getId();
            String platform = deviceData.getPlatform();
            String pushToken = deviceData.getPushToken();
            
            if (StringHelper.isEmpty(pushToken)) {
                logger.warn("Device {} has no push token", deviceId);
                continue;
            }

            boolean isIos = StringHelper.equalsIgnoreCase(platform, "ios"); 
            boolean isAndroid = StringHelper.equalsIgnoreCase(platform, "android");
            
            if (isIos) {
                if (!iosEnabled) {
                    logger.warn("Cannot send notification to device {}: ios not enabled in configuration", deviceId);
                    continue;
                }

            } else if (isAndroid) {
                if (!androidEnabled) {
                    logger.warn("Cannot send notification to device {}: android not enabled in configuration", deviceId);
                    continue;
                }

            } else continue;
            
            String platformId = isIos ? iosPlatformId : androidPlatformId;
            String endpoint = getTargetEndpointArn(rps, pushSnsService,  user, device, platformId);
            if (endpoint == null) {
                logger.error("Unable to create target endpoint ARN for user (platform {})", platform);
                continue;
            }
            
            Map<String, Object> map = null;

            if (isIos) {

                map = Map.of(
                        "aps", Map.of(
                            "badge", 0,
                            "alert", Map.of("body", body, "title", title),
                            "category", "ACTIONABLE",
                            "content-available", "1",
                            "sound", "default"
                        ),
                        "request", request
                );
            } else if (isAndroid) {

                map = Map.of(
                    "collapse_key", "single",
                    "content_available", true,
                    "time_to_live", 60,
                    "data", Map.of("message", request, "title", title)
                );
            }
                
            String message = new JSONObject(map).toString();
            logger.debug("Sending message \n{}\n to device {}", message, deviceId);

            response = jansClient.sendNotification(buildServiceAuth(), endpoint, message, platformId);
            logger.debug("Notification response was: {}", response);
        }
        
    }
    
    private String buildServiceAuth() throws Exception {       

        String basic = settings.getString(Configuration.CLIENT_ID_PROP) + ":" + 
            settings.getString(Configuration.CLIENT_SECRET_PROP); 
        basic = new String(Base64.getEncoder().encode(basic.getBytes(UTF_8)), UTF_8);
        
        StringJoiner joiner = new StringJoiner("&");
        Map.of("grant_type", "client_credentials", "scope", URLEncoder.encode(SCAN_SCOPE, UTF_8))
            .forEach((k, v) -> joiner.add( k + "=" + v));
            
        String asEndpoint = settings.getString(Configuration.AS_ENDPOINT_PROP) + "/jans-auth/restv1/token";
        HTTPRequest request = new HTTPRequest(HTTPRequest.Method.POST, new URL(asEndpoint));
        
        request.setAccept(APPLICATION_JSON);
        request.setConnectTimeout(3000);
        request.setReadTimeout(3000);
        request.setQuery(joiner.toString());
        request.setAuthorization("Basic " + basic);
        
        HTTPResponse r = request.send();
        r.ensureStatusCode(200);

        logger.info("Got a token from {}", asEndpoint);
        return "Bearer " + r.getContentAsJSONObject().getAsString("access_token");

    }
    
    private boolean reloadConf() {        
        
        JSONObject angroid = settings.getJSONObject("android").getJSONObject(JANS_KEY);
        JSONObject ayos = settings.getJSONObject("ios").getJSONObject(JANS_KEY);
        
        androidEnabled = angroid.getBoolean("enabled");
        iosEnabled = ayos.getBoolean("enabled");
        
        if (!androidEnabled && !iosEnabled) {
            logger.warn("Android and IOS are disabled in the configuration");
            return false;
        }
        
        androidPlatformId = angroid.getString("platform_id");
        iosPlatformId = ayos.getString("platform_id");
        
        String serverUri = settings.getJSONObject(JANS_KEY).getString("server_uri");
        logger.debug("Using server uri: {}", serverUri);
        
        NotifyClientFactory notifyClientFactory = NotifyClientFactory.instance();
        logger.debug("Retrieving configuration metadata");
        NotifyMetadata metadataConfiguration = notifyClientFactory
                .createMetaDataConfigurationService(serverUri).getMetadataConfiguration();
        
        jansClient = notifyClientFactory.createNotifyService(metadataConfiguration);
        return true;

    }
    
    private User getUser(String inum) {
        
        UserService userService = CdiUtil.bean(UserService.class);
        logger.debug("Retrieving user identified by inum {}", inum);
        return userService.getUserByInum(inum, "uid", "inum");

    }

    private String getTargetEndpointArn(RegistrationPersistenceService rps, PushSnsService pss,
            User user, Fido2RegistrationEntry device, String platformId) throws Exception {
    
        String targetEndpointArn = null;

        try {
            String userInum = user.getAttribute("inum");

            //Try to obtain endpoint from device itself
            targetEndpointArn = Optional.ofNullable(device.getDeviceNotificationConf())
                    .map(Fido2DeviceNotificationConf::getSnsEndpointArn).orElse(null);

            if (targetEndpointArn != null) {
                logger.debug("Picking already stored endpoint ARN");
                return targetEndpointArn;
            }
            
            Fido2DeviceData deviceData = device.getDeviceData();
            String pushToken = deviceData.getPushToken();

            RegisterDeviceResponse registerDeviceResponse = jansClient.registerDevice(
                        buildServiceAuth(), pushToken, pss.getCustomUserData(user), platformId);
            
            if (registerDeviceResponse != null && registerDeviceResponse.getStatusCode() == 200) {
                targetEndpointArn = registerDeviceResponse.getEndpointArn();
            }
    
            if (targetEndpointArn == null) {
                logger.error("Failed to get endpoint ARN for user: {}", userInum);
                return null;
            }
            
            //Store endpoint in device            
            Fido2RegistrationEntry updatedDevice = rps.findRegisteredUserDevice(userInum, device.getId());
            updatedDevice.setDeviceNotificationConf(new Fido2DeviceNotificationConf(targetEndpointArn, null, null));   
            rps.update(updatedDevice);
            
            logger.debug("ARN endpoint stored in user's device");
            
        } catch (Exception e) {
            logger.error("Error at getTargetEndpointArn: " + e.getMessage());
        }
        return targetEndpointArn;
        
    }

}
