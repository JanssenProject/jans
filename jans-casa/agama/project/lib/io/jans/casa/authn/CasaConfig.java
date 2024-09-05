package io.jans.casa.authn;

import io.jans.casa.conf.MainSettings;
import io.jans.casa.model.ApplicationConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.CacheService;
import io.jans.service.cdi.util.CdiUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CasaConfig {
    
    //static fields usage avoids serialization issues
    private static final String ASSETS_CACHE_KEY = "casa_assets";
    private static final Logger logger = LoggerFactory.getLogger(CasaConfig.class);
    private static CacheService cacheService = CdiUtil.bean(CacheService.class);
    private static PersistenceEntryManager entryManager = CdiUtil.bean(PersistenceEntryManager.class);
    
    private MainSettings settings;
    private Map<String, String> uiParams;
    private List<String> policies;
    private Set<String> methods;
    
    public CasaConfig() {        
        settings = entryManager.find(ApplicationConfiguration.class, "ou=casa,ou=configuration,o=jans")
                .getSettings();
                
        readUIParams();

        methods = new LinkedHashSet(settings.getAcrPluginMap().keySet());
        //This is actually an instance of io.jans.casa.plugins.strongauthn.conf.Configuration
        Map<String, Object> strongAuthnSettings = (Map<String, Object>) settings.getPluginSettings()
                .get("strong-authn-settings");
        
        if (strongAuthnSettings == null) {
            policies = Collections.emptyList();
        } else {
            policies = (List<String>) strongAuthnSettings.get("policy_2fa");
        }
        logger.info("Detected 2FA policies: {}", policies);

    }

    public Map<String, String> getUiParams() {
        return uiParams;
    }
    
    public List<String> getPolicies() {
        return policies;
    }
    
    public Set<String> getMethods() {
        return methods;
    }

    public boolean isNeedsCaptureLocation() {
        return !policies.isEmpty();
    }
    
    private void readUIParams() {

        Map<String, Object> cs = (Map<String, Object>) cacheService.get(ASSETS_CACHE_KEY);
        
        if (cs == null) {
            //This may happen when cache type is IN_MEMORY (unlikey to occur), where actual cache is merely 
            //a local variable (expiring map) living inside Casa webapp, not jans-auth webapp
            //Here we try to make an approximate guess of what the map might contain based on casa settings
            
            String custPrefix = "/custom";
            String logoUrl = "/images/logo.png";
            String faviconUrl = "/images/favicon.ico";
            String extraCssSnippet = settings.getExtraCssSnippet();
            
            if (extraCssSnippet != null || settings.isUseExternalBranding()) {
                logoUrl = custPrefix + logoUrl;
                faviconUrl = custPrefix + faviconUrl
            }

            cs = Map.of(
                "contextPath", "/jans-casa",
                "prefix", settings.isUseExternalBranding() ? custPrefix : "",
                "faviconUrl", faviconUrl,
                "extraCss", Optional.ofNullable(extraCssSnippet).orElse(""),
                "logoUrl", logoUrl);
            
            try {
                cacheService.put((int) TimeUnit.HOURS.toSeconds(1), ASSETS_CACHE_KEY, cs);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
 
    }

}
