package io.jans.casa.certauthn;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.service.CacheService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.security.StringEncrypter;

import java.util.Optional;

public class CertAuhnHelper {
    
    private static final int ENTRY_EXP_SECONDS = 15;
    
    private static CacheService cs = CdiUtil.bean(CacheService.class);
    private String userId;
    private String key;
    
    public CacheHelper(String inum) {
        this.userId = inum;
        this.key = ("" + Math.random()).substring(2);
    }
    
    public String buildRedirectUrl() throws StringEncrypter.EncryptionException {
        
        cs.put(ENTRY_EXP_SECONDS, key, userId);
        String encKey = CdiUtil.bean(StringEncrypter.class).encrypt(key);
        return CdiUtil.bean(AppConfiguration.class).getIssuer() + "/jans-casa/pl/cert-authn/index.zul?ref=" + encKey;
        
    }
    
    public String retrieve() {
        
        String val = Optional.ofNullable(cs.get(key)).map(Object::toString).orElse(null);        
        if (val != null) {
            cs.remove(key);
        }
        return val;
        
    }

}
