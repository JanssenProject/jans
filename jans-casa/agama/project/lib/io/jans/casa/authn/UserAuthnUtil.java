package io.jans.casa.authn;

import io.jans.agama.model.*;
import io.jans.as.common.model.common.User;
import io.jans.as.server.service.*;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.cdi.util.CdiUtil;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserAuthnUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(UserAuthnUtil.class);
    private static final MFAInfoHelper mfaInfo = new MFAInfoHelper();
    private static PersistenceEntryManager entryManager = CdiUtil.bean(PersistenceEntryManager.class);
    private static AuthenticationService authenticationService = CdiUtil.bean(AuthenticationService.class);

    private User user;
    private String uid;
    private String name;
    private String inum;
    private String preferredMethod;
    private boolean validCredentials;
    
    private String jsonLocation;
    private String jsonDevice;
    private List<String> policies = Collections.emptyList();

    public UserAuthnUtil() { }

    public UserAuthnUtil(List<String> policies) {
        this.policies = policies;
    }
    
    public void validate(String userName, String password) {
        
        logger.info("Validating password for {}", userName);
        if (authenticationService.authenticate(userName, password)) {
            user = authenticationService.getAuthenticatedUser();
            validCredentials = true;
            
            uid = user.getUserId();
            inum = user.getAttribute("inum");
            name = Optional.ofNullable(user.getAttribute("displayName")).orElse(user.getAttribute("givenName"));
            preferredMethod = translate(user.getAttribute("jansPreferredMethod"));
            updatePolicies();
        }
        
    }
    
    public String getUid() {
        return uid;
    }
    
    public String getName() {
        return name;
    }
    
    public String getInum() {
        return inum;
    }
    
    public boolean isValidCredentials() {
        return validCredentials;
    }    
    
    public boolean isUser2FAOn() {
         return preferredMethod != null;
    }
    
    public void setJsonLocation(String jsonLocation) {
        this.jsonLocation = jsonLocation;
    }
    
    public void setJsonDevice(String jsonDevice) {
        this.jsonDevice = jsonDevice;
    }    
    
    public boolean prompt2FA() {

        boolean prompt = policies.isEmpty() || policies.contains("EVERY_LOGIN");
        if (prompt) return true;
        
        // Validate device and location data before proceeding
        if (!isValidDeviceData()) {
            logger.warn("Invalid or missing device data, forcing 2FA prompt");
            return true;
        }
        
        TrustedDevicesManager tdm = new TrustedDevicesManager(user, jsonDevice, jsonLocation);
        
        if (/*!prompt &&*/ policies.contains("LOCATION_UNKNOWN")) {
            prompt = !tdm.knownLocation();
        }
        if (!prompt && policies.contains("DEVICE_UNKNOWN")) {
            prompt = !tdm.knownDevice();
        }
        if (!prompt) {
            logger.info("2FA will be skipped according to policy evaluation for this user");
        }
        return prompt;
        
    }

    /**
     * Validates that device data is complete and valid before using it for trusted device evaluation
     * This prevents issues with stale or incomplete device fingerprints
     */
    private boolean isValidDeviceData() {
        // Use centralized device validation service
        io.jans.casa.core.SessionValidationService validationService = 
            io.jans.service.cdi.util.CdiUtil.bean(io.jans.casa.core.SessionValidationService.class);
        
        return validationService.isDeviceDataValid(jsonDevice);
    }

    /**
     * Validates the current session state to prevent stale session issues
     * Returns true if session is valid, false if stale session detected
     */
    public boolean validateSessionState() {
        try {
            // Check if we have a valid session context
            jakarta.servlet.http.HttpServletRequest request = CdiUtil.bean(jakarta.servlet.http.HttpServletRequest.class);
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            
            if (session == null) {
                logger.debug("No active session found");
                return false;
            }
            
            // Check session age to detect stale sessions
            long sessionAge = System.currentTimeMillis() - session.getCreationTime();
            long maxSessionAge = 30 * 60 * 1000; // 30 minutes
            
            if (sessionAge > maxSessionAge) {
                logger.debug("Session is too old ({} ms), considering stale", sessionAge);
                return false;
            }
            
            // Check for authentication-related session attributes
            Object authUser = session.getAttribute("user");
            if (authUser == null) {
                logger.debug("No authenticated user in session");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.debug("Error validating session state: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Clears any stale session data to force fresh authentication
     */
    public void clearStaleSessionData() {
        try {
            jakarta.servlet.http.HttpServletRequest request = CdiUtil.bean(jakarta.servlet.http.HttpServletRequest.class);
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            
            if (session != null) {
                // Clear authentication-related session attributes
                session.removeAttribute("user");
                session.removeAttribute("authFlowContext");
                session.removeAttribute("sessionContext");
                
                // Invalidate the session to force fresh authentication
                session.invalidate();
                logger.debug("Cleared stale session data");
            }
        } catch (Exception e) {
            logger.debug("Error clearing stale session data: {}", e.getMessage());
        }
    }

    public List<String> computeUserMethods(LinkedHashSet<String> supportedMethods) {

        logger.trace("Supported methods: {}", supportedMethods);
        Set<String> methods = new LinkedHashSet<>();    //preserve insertion order
        List<String> empty = Collections.emptyList();

        //Reduce the supported methods to those actually installed
        supportedMethods.forEach(meh -> {
            try {
                String dn = String.format("%s=%s,ou=flows,ou=agama,o=jans", Flow.ATTR_NAMES.QNAME, meh);
        
                if (entryManager.contains(dn, ProtoFlow.class)) {
                    methods.add(meh);
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        });
        logger.trace("List of methods: {}", methods);

        if (methods.isEmpty()) {
            logger.info("None of {} exist as agama flows", supportedMethods);
            return empty;
        }
        
        //Filter only the methods that match registered credentials for the user
        List<String> enrolled = mfaInfo.methodsEnrolled(inum);
        logger.info("User has enrollments for {}", enrolled);
        methods.retainAll(enrolled);
        logger.trace("Updated list of methods: {}", methods);  

        List<String> result = new ArrayList<>();
        if (methods.remove(preferredMethod)) {
            //Put the preferred on top
            result.add(preferredMethod);
        }
        result.addAll(methods);
        
        logger.info("Authentication methods list has been distilled to {}", result); 
        return result;

    }
    
    public void updateTrustedDevices() {

        try {
            if (policies.isEmpty() || !isUser2FAOn()) return;
            
            // Only update trusted devices if we have valid device data
            if (!isValidDeviceData()) {
                logger.warn("Skipping trusted device update due to invalid device data");
                return;
            }
            
            TrustedDevicesManager tdm = new TrustedDevicesManager(user, jsonDevice, jsonLocation);
            tdm.updateDevices();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }
    
    private void updatePolicies() {

        if (policies.contains("CUSTOM")) {
            String usrPolicy = user.getAttribute("jansStrongAuthPolicy");

            String[] policiesArr = usrPolicy == null ? new String[]{ "EVERY_LOGIN" } : usrPolicy.split(",\\s*");
            policies = List.of(policiesArr);
        }

    }

    private String translate(String oldAcr) {
        
        if (oldAcr == null) return null;
        
        if (!List.of("fido2", "super_gluu", "otp", "twilio_sms").contains(oldAcr)) return oldAcr;
        
        return "io.jans.casa.authn." + oldAcr;
        
    }
    
}
