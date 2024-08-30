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
