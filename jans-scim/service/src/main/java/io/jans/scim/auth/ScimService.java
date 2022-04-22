package io.jans.scim.auth;

import io.jans.scim.model.conf.AppConfiguration;
import io.jans.scim.model.conf.ScimMode;
import io.jans.scim.auth.none.NoProtectionService;
import io.jans.scim.auth.oauth.DefaultOAuthProtectionService;
import io.jans.scim.service.ConfigurationService;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
@BindingUrls({"/"})
public class ScimService implements JansRestService {
    
    @Inject
    private Logger log;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private AppConfiguration appConfiguration;
    
    @Inject
    private DefaultOAuthProtectionService oauthProtectionService;
    
    @Inject
    private NoProtectionService noProtectionService;

    @Override
    public String getName() {
        return "SCIM";
    }
    
    @Override
    public boolean isEnabled() {
        boolean enabled = configurationService.getConfiguration().isScimEnabled();
        if (!enabled) {
            log.debug("SCIM API is disabled.");    
        }
        return enabled;
    }
        
    @Override    
    public IProtectionService getProtectionService() {
        
        ScimMode mode = Optional.ofNullable(appConfiguration.getProtectionMode()).orElse(ScimMode.OAUTH);
        log.debug("SCIM protection mode is: {}", mode);
        
        switch (mode) {
            case OAUTH: return oauthProtectionService;
            case BYPASS: return noProtectionService;
        }
        return null;
        
    }
    
}
