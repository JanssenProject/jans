package io.jans.casa.certauthn;

import io.jans.service.CacheService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.security.StringEncrypter;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CertAuthnHelper {
    
    private static final String RND_KEY = "ref";
    //Max. time allowed to go from authn flow to cert pickup url and back again
    private static final int ENTRY_EXP_SECONDS = 25;
    
    private static CacheService cs = CdiUtil.bean(CacheService.class);
    private String userId;
    private String certPickupUrl;
    private String key;
    
    public CertAuthnHelper() {}

    public CertAuthnHelper(String inum, String certPickupUrl) {
        this.userId = inum;
        this.certPickupUrl = certPickupUrl;
        this.key = ("" + Math.random()).substring(2);
    }
    
    public String buildRedirectUrl() throws StringEncrypter.EncryptionException {
        
        String encKey = CdiUtil.bean(StringEncrypter.class).encrypt(key);
        encKey = URLEncoder.encode(encKey, UTF_8);
        
        //See plugin's class io.jans.casa.plugins.certauthn.model.Reference
        JSONObject job = new JSONObject();
        job.put("userId", userId);
        job.put("enroll", false);

        cs.put(ENTRY_EXP_SECONDS, key, job.toString());
        return certPickupUrl + "?" + RND_KEY + "=" + encKey;
        
    }
    
    public String retrieveOutcome() {
        
        String val = Optional.ofNullable(cs.get(key)).map(Object::toString).orElse(null);        
        if (val != null) {
            cs.remove(key);
        }
        return val;
        
    }

}
