package io.jans.casa.plugins.certauthn.vm;

import io.jans.casa.misc.*;
import io.jans.casa.service.*;
import io.jans.service.cache.CacheProvider;

import java.net.URLDecoder;
import java.util.*;

import org.slf4j.*;
import org.zkoss.bind.annotation.Init;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CertAuthnVM {

    public static final String RND_KEY = "ref";
    
    private static final String CERT_HEADER = "X-ClientCert";
    private static final String AGAMA_CB = "/jans-auth/fl/callback";
    
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Init
    public void init() {
        
        try {
            logger.info("Loading certificate picking page..."); 
            String encKey = WebUtils.getQueryParam(RND_KEY);
            
            if (Utils.isEmpty(encKey)) {
                logger.error("Expected parameter '{}' not specified in URL.", RND_KEY);
                return;
            }
            
            String key = URLDecoder.decode(Utils.stringEncrypter().decrypt(encKey), UTF_8);
            String strCert = WebUtils.getRequestHeader(CERT_HEADER);

            CacheProvider cacheProvider = Utils.managedBean(CacheProvider.class);
            Long expiresAt = Optional.ofNullable(cacheProvider.get(key)).map(Long.class::cast).orElse(null);
            
            Long timeLeftSec = expiresAt == null ? -1 : (expiresAt - System.currentTimeMillis()) / 1000;            
            if (timeLeftSec <= 0) {                    
                logger.error("Expired cache entry");
            } else {
                //Rewrite the cache entry with the cert provided in HTTP header
                cacheProvider.put(timeLeftSec.intValue(), key, strCert);
            }
                                   
            String flowCallbackUrl = Utils.managedBean(IPersistenceService.class).getIssuerUrl() + AGAMA_CB;
            //Resume Agama flow
            WebUtils.execRedirect(flowCallbackUrl, false);
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        
    }
    
}
