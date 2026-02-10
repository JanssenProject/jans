package io.jans.casa.plugins.injiwallet.vm;

import io.jans.casa.plugins.injiwallet.service.InjiWalletLinkingService;
import io.jans.casa.core.pojo.User;
import io.jans.casa.misc.Utils;
import io.jans.casa.misc.WebUtils;
import io.jans.casa.ui.UIUtils;
import io.jans.casa.service.ISessionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Messagebox;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ViewModel for Inji Wallet user credential management page
 * Handles credential enrollment, removal, and status checking
 */
public class InjiWalletUserVM {

    public static final String LINK_QUEUE = "inji-wallet-link";
    public static final String EVENT_NAME = "link-started";
    
    public static final long ENROLL_TIME_MS = TimeUnit.MINUTES.toMillis(1);  // 1 min

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private ISessionContext sessionContext;

    private User user;
    private InjiWalletLinkingService linkingService;
    
    private boolean hasInjiCredentials;
    private String credentialStatus;
    private Map<String, String> credentialDetails;
    
    // Individual credential type status
    private boolean hasNidCredential;
    private boolean hasTaxCredential;
    
    // Pending linking tracking
    private String pendingCredentialType;
    private long pendingLinkingExpiresAt;
    
    @Init
    public void init() {
        // Get user from session context
        user = sessionContext.getLoggedUser();
        
        if (user == null) {
            logger.error("User is null in session context");
            return;
        }
        
        logger.info("Initializing Inji Wallet User VM for user: {}", user.getUserName());
        linkingService = InjiWalletLinkingService.getInstance();
        
        // Check if user has Inji credentials linked
        checkCredentialStatus();
        
        // Subscribe to linking events
        EventQueues.lookup(LINK_QUEUE, EventQueues.SESSION, true)
                .subscribe(event -> {
                    if (EVENT_NAME.equals(event.getName())) {
                        String data = Optional.ofNullable(event.getData()).map(Object::toString).orElse(null);
                        if (data != null) {
                            // Linking started for a credential type
                            logger.info("Received link start event for {}", data);
                            pendingLinkingExpiresAt = System.currentTimeMillis() + ENROLL_TIME_MS;
                            pendingCredentialType = data;
                        } else {
                            // Linking completed
                            logger.info("Received link completed event");
                            cancelPending();
                            checkCredentialStatus();
                        }
                        BindUtils.postNotifyChange(InjiWalletUserVM.this, "hasNidCredential", "hasTaxCredential", "pendingCredentialType");
                    }
                });
    }
    
    public boolean getHasNidCredential() {
        return hasNidCredential;
    }
    
    public boolean getHasTaxCredential() {
        return hasTaxCredential;
    }
    
    public String getPendingCredentialType() {
        return pendingCredentialType;
    }
    
    @NotifyChange("pendingCredentialType")
    public void cancelPending() {
        pendingCredentialType = null;
    }
    
    /**
     * Poll for credential status updates and check for pending linking timeout
     * Called by timer in ZUL
     */
    @Command
    @NotifyChange({"hasInjiCredentials", "credentialStatus", "hasNidCredential", "hasTaxCredential", "pendingCredentialType"})
    public void poll() {
        // Check for pending linking timeout
        if (pendingCredentialType != null && pendingLinkingExpiresAt < System.currentTimeMillis()) {
            logger.info("Too much time elapsed for linking to finish");
            cancelPending();
        }
        // Check credential status
        checkCredentialStatus();
    }
    
    /**
     * Check if user has Inji credentials linked
     */
    private void checkCredentialStatus() {
        try {
            if (user == null) {
                logger.warn("User is null, cannot check credential status");
                hasInjiCredentials = false;
                credentialStatus = null;
                credentialDetails = null;
                hasNidCredential = false;
                hasTaxCredential = false;
                return;
            }
            
            // OLD APPROACH: Check based on MOSIP provider in jansExtUid (acct-linking)
            // hasInjiCredentials = linkingService.hasInjiCredential(user.getId());
            
            // NEW APPROACH: Check for specific credential types from verifiableCredentials attribute
            hasNidCredential = linkingService.hasCredentialType(user.getId(), "NID");
            hasTaxCredential = linkingService.hasCredentialType(user.getId(), "TAX");
            
            // User has credentials if any credential type is present
            hasInjiCredentials = hasNidCredential || hasTaxCredential;
            
            logger.debug("User {} credential status - NID: {}, TAX: {}", 
                user.getUserName(), hasNidCredential, hasTaxCredential);
            
            if (hasInjiCredentials) {
                // OLD APPROACH: Get status from MOSIP provider
                // credentialStatus = linkingService.getInjiCredentialStatus(user.getId());
                
                // NEW APPROACH: Build status from credential types
                StringBuilder statusBuilder = new StringBuilder();
                if (hasNidCredential) statusBuilder.append("NID");
                if (hasTaxCredential) {
                    if (statusBuilder.length() > 0) statusBuilder.append(", ");
                    statusBuilder.append("TAX");
                }
                credentialStatus = statusBuilder.toString();
                
                logger.debug("User {} has Inji credentials: {}", user.getUserName(), credentialStatus);
                
                // Load credential details from user attributes
                loadCredentialDetails();
            } else {
                credentialStatus = null;
                credentialDetails = null;
                logger.debug("User {} does not have Inji credentials", user.getUserName());
            }
        } catch (Exception e) {
            logger.error("Error checking credential status", e);
            hasInjiCredentials = false;
            credentialStatus = null;
            credentialDetails = null;
            hasNidCredential = false;
            hasTaxCredential = false;
        }
    }
    
    /**
     * Load credential details from user attributes
     */
    private void loadCredentialDetails() {
        try {
            credentialDetails = new HashMap<>();
            
            // Get basic user information that's available from Casa User object
            if (user.getUserName() != null) {
                credentialDetails.put("Username", user.getUserName());
            }
            
            if (user.getId() != null) {
                credentialDetails.put("User ID", user.getId());
            }
            
            // Get additional attributes from the user's claims/attributes map
            // The User object in Casa stores additional attributes
            if (user.getClaims() != null && !user.getClaims().isEmpty()) {
                Map<String, Object> claims = user.getClaims();
                
                if (claims.containsKey("displayName")) {
                    credentialDetails.put("Full Name", String.valueOf(claims.get("displayName")));
                }
                if (claims.containsKey("mail")) {
                    credentialDetails.put("Email", String.valueOf(claims.get("mail")));
                }
                if (claims.containsKey("mobile")) {
                    credentialDetails.put("Phone", String.valueOf(claims.get("mobile")));
                }
                if (claims.containsKey("gender")) {
                    credentialDetails.put("Gender", String.valueOf(claims.get("gender")));
                }
                if (claims.containsKey("givenName")) {
                    credentialDetails.put("Given Name", String.valueOf(claims.get("givenName")));
                }
                if (claims.containsKey("birthdate")) {
                    credentialDetails.put("Date of Birth", String.valueOf(claims.get("birthdate")));
                }
            }
            
            logger.debug("Loaded {} credential details for user", credentialDetails.size());
            
        } catch (Exception e) {
            logger.error("Error loading credential details", e);
            credentialDetails = new HashMap<>();
        }
    }
    
    /**
     * View credential details
     */
    @Command
    public void viewCredentials() {
        viewNidCredentials(); // Default to NID for backward compatibility
    }
    
    /**
     * View National ID credential details
     */
    @Command
    public void viewNidCredentials() {
        try {
            if (!hasNidCredential) {
                UIUtils.showMessageUI(false, "No National ID credential available");
                return;
            }
            
            // Get verifiable credentials from service
            Map<String, Object> credentials = linkingService.getVerifiableCredentials(user.getId());
            
            if (!credentials.containsKey("NID")) {
                UIUtils.showMessageUI(false, "National ID credential not found");
                return;
            }
            
            // Extract NID credential data
            Map<String, Object> nidCredential = (Map<String, Object>) credentials.get("NID");
            
            // Show in custom popup with image support
            showCredentialDetailsPopup("National ID Credential", nidCredential, "NID");
            
        } catch (Exception e) {
            logger.error("Error viewing National ID credentials", e);
            UIUtils.showMessageUI(false, "Error viewing credentials: " + e.getMessage());
        }
    }
    
    /**
     * View TAX ID credential details
     */
    @Command
    public void viewTaxCredentials() {
        try {
            if (!hasTaxCredential) {
                UIUtils.showMessageUI(false, "No TAX ID credential available");
                return;
            }
            
            // Get verifiable credentials from service
            Map<String, Object> credentials = linkingService.getVerifiableCredentials(user.getId());
            
            if (!credentials.containsKey("TAX")) {
                UIUtils.showMessageUI(false, "TAX ID credential not found");
                return;
            }
            
            // Extract TAX credential data
            Map<String, Object> taxCredential = (Map<String, Object>) credentials.get("TAX");
            
            // Show in custom popup with image support
            showCredentialDetailsPopup("TAX ID Credential", taxCredential, "TAX");
            
        } catch (Exception e) {
            logger.error("Error viewing TAX ID credentials", e);
            UIUtils.showMessageUI(false, "Error viewing credentials: " + e.getMessage());
        }
    }
    
    /**
     * Show credential details in a custom popup window with image support
     */
    private void showCredentialDetailsPopup(String title, Map<String, Object> credential, String credentialType) {
        try {
            // Extract credentialSubject which contains the actual user data
            Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
            
            if (credentialSubject == null) {
                UIUtils.showMessageUI(false, "Invalid credential format");
                return;
            }
            
            // Prepare data for popup
            Map<String, Object> args = new HashMap<>();
            args.put("title", title);
            
            // Handle photo/face - convert base64 to data URI if available
            Object face = credentialSubject.get("face");
            if (face != null && !face.toString().isEmpty()) {
                String faceData = extractValue(face);
                // Check if it's already a data URI or just base64
                if (!faceData.startsWith("data:")) {
                    // Assume it's JPEG if not specified
                    faceData = "data:image/jpeg;base64," + faceData;
                }
                args.put("photoData", faceData);
            }
            
            // Extract other fields
            Object fullName = credentialSubject.get("fullName");
            if (fullName != null) {
                args.put("fullName", extractValue(fullName));
            }
            
            Object gender = credentialSubject.get("gender");
            if (gender != null) {
                args.put("gender", extractValue(gender));
            }
            
            Object email = credentialSubject.get("email");
            if (email != null) {
                args.put("email", extractValue(email));
            }
            
            // UIN (for NID) or Tax ID (for TAX)
            if ("NID".equals(credentialType)) {
                Object uin = credentialSubject.get("UIN");
                if (uin != null) {
                    args.put("uin", extractValue(uin));
                }
            } else if ("TAX".equals(credentialType)) {
                Object taxId = credentialSubject.get("taxId");
                if (taxId == null) {
                    taxId = credentialSubject.get("taxNumber");
                }
                if (taxId != null) {
                    args.put("taxId", extractValue(taxId));
                }
            }
            
            Object dob = credentialSubject.get("dateOfBirth");
            if (dob == null) {
                dob = credentialSubject.get("birthdate");
            }
            if (dob != null) {
                args.put("dateOfBirth", extractValue(dob));
            }
            
            Object phone = credentialSubject.get("phone");
            if (phone == null) {
                phone = credentialSubject.get("mobile");
            }
            if (phone != null) {
                args.put("phone", extractValue(phone));
            }
            
            Object address = credentialSubject.get("address");
            if (address != null) {
                args.put("address", extractValue(address));
            }
            
            // Create and show the popup
            Executions.createComponents("/pl/inji-wallet/user/credential-details.zul", null, args);
            
        } catch (Exception e) {
            logger.error("Error showing credential details popup", e);
            UIUtils.showMessageUI(false, "Error displaying credentials: " + e.getMessage());
        }
    }
    
    /**
     * Extract value from either string or array format
     * Handles MOSIP format where values can be arrays with language/value pairs
     */
    private String extractValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // If it's a string, return as is
        if (value instanceof String) {
            return (String) value;
        }
        
        // If it's a list, extract the first value
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                Object firstItem = list.get(0);
                // Check if it's a map with "value" key (MOSIP format)
                if (firstItem instanceof Map) {
                    Map<String, Object> itemMap = (Map<String, Object>) firstItem;
                    if (itemMap.containsKey("value")) {
                        return String.valueOf(itemMap.get("value"));
                    }
                }
                // Otherwise return the first item as string
                return String.valueOf(firstItem);
            }
        }
        
        // Fallback: convert to string
        return String.valueOf(value);
    }
    
    /**
     * Enroll National ID credential
     */
    @Command
    public void enrollNationalId() {
        try {
            if (user == null) {
                logger.error("User is null, cannot enroll");
                UIUtils.showMessageUI(false, "Error: User session not found");
                return;
            }
            
            logger.info("Starting National ID enrollment for user: {}", user.getUserName());
            
            // Redirect to the redirect page which will handle the Agama flow
            // Use relative URL from current page location
            String redirectUrl = "inji-redirect.zul?credentialType=NID";
            logger.info("Redirecting to: {}", redirectUrl);
            
            WebUtils.execRedirect(redirectUrl, false);
            
        } catch (Exception e) {
            logger.error("Error enrolling National ID", e);
            UIUtils.showMessageUI(false, "Error enrolling credential: " + e.getMessage());
        }
    }
    
    /**
     * Enroll TAX ID credential
     */
    @Command
    public void enrollTaxId() {
        try {
            if (user == null) {
                logger.error("User is null, cannot enroll");
                UIUtils.showMessageUI(false, "Error: User session not found");
                return;
            }
            
            logger.info("Starting TAX ID enrollment for user: {}", user.getUserName());
            
            // Redirect to the redirect page which will handle the Agama flow
            // Use relative URL from current page location
            String redirectUrl = "inji-redirect.zul?credentialType=TAX";
            logger.info("Redirecting to: {}", redirectUrl);
            
            WebUtils.execRedirect(redirectUrl, false);
            
        } catch (Exception e) {
            logger.error("Error enrolling TAX ID", e);
            UIUtils.showMessageUI(false, "Error enrolling credential: " + e.getMessage());
        }
    }
    
    /**
     * Remove TAX ID credential
     */
    @Command
    @NotifyChange({"hasInjiCredentials", "credentialStatus", "hasNidCredential", "hasTaxCredential"})
    public void removeTaxId() {
        try {
            if (user == null) {
                logger.error("User is null, cannot remove credential");
                UIUtils.showMessageUI(false, "Error: User session not found");
                return;
            }
            
            logger.info("Removing TAX ID credential for user: {}", user.getUserName());
            
            // Remove TAX credential from verifiableCredentials attribute
            boolean success = linkingService.removeCredentialType(user.getId(), "TAX");
            
            if (success) {
                logger.info("Successfully removed TAX ID credential");
                UIUtils.showMessageUI(true, "Credential removed successfully");
                checkCredentialStatus();
            } else {
                logger.error("Failed to remove TAX ID credential");
                UIUtils.showMessageUI(false, "Failed to remove credential");
            }
            
        } catch (Exception e) {
            logger.error("Error removing TAX ID credential", e);
            UIUtils.showMessageUI(false, "Error removing credential: " + e.getMessage());
        }
    }
    
    /**
     * Remove National ID credential
     */
    @Command
    @NotifyChange({"hasInjiCredentials", "credentialStatus", "hasNidCredential", "hasTaxCredential"})
    public void removeNationalId() {
        try {
            if (user == null) {
                logger.error("User is null, cannot remove credential");
                UIUtils.showMessageUI(false, "Error: User session not found");
                return;
            }
            
            logger.info("Removing National ID credential for user: {}", user.getUserName());
            
            // OLD APPROACH: Delink based on MOSIP provider
            // if (credentialStatus != null) {
            //     boolean success = linkingService.delink(user.getId(), "mosip", credentialStatus);
            //     if (success) {
            //         logger.info("Successfully removed National ID credential");
            //         UIUtils.showMessageUI(true, "Credential removed successfully");
            //         checkCredentialStatus();
            //     } else {
            //         logger.error("Failed to remove National ID credential");
            //         UIUtils.showMessageUI(false, "Failed to remove credential");
            //     }
            // }
            
            // NEW APPROACH: Remove NID credential from verifiableCredentials attribute
            boolean success = linkingService.removeCredentialType(user.getId(), "NID");
            
            if (success) {
                logger.info("Successfully removed National ID credential");
                UIUtils.showMessageUI(true, "Credential removed successfully");
                checkCredentialStatus();
            } else {
                logger.error("Failed to remove National ID credential");
                UIUtils.showMessageUI(false, "Failed to remove credential");
            }
            
        } catch (Exception e) {
            logger.error("Error removing National ID credential", e);
            UIUtils.showMessageUI(false, "Error removing credential: " + e.getMessage());
        }
    }
    
    // Getters
    public boolean isHasInjiCredentials() {
        return hasInjiCredentials;
    }
    
    public String getCredentialStatus() {
        return credentialStatus;
    }
    
    public Map<String, String> getCredentialDetails() {
        return credentialDetails;
    }
    
    public boolean isHasNidCredential() {
        return hasNidCredential;
    }
    
    public boolean isHasTaxCredential() {
        return hasTaxCredential;
    }
}
