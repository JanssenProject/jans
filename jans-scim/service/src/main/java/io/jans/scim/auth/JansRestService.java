package io.jans.scim.auth;

public interface JansRestService {
    
    String getName();
    boolean isEnabled();
    IProtectionService getProtectionService();
    
}
