package io.jans.casa.certauthn;

import io.jans.service.CacheService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.security.StringEncrypter;

import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Optional;

import org.slf4j.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CertAuthnHelper {
    
    private static final String RND_KEY = "ref";
    private static final int DEFAULT_ROUND_TRIP_MAX_TIME = 45;
    
    private static SecureRandom RND = new SecureRandom();
    private static Logger logger = LoggerFactory.getLogger(CertAuthnHelper.class); 
    private static CacheService cs = CdiUtil.bean(CacheService.class);
    private static StringEncrypter encrypter = CdiUtil.bean(StringEncrypter.class);

    private String certPickupUrl;
    private String key;
    
    //Max. time allowed to go from authn flow to cert pickup url and back again
    //(measured in seconds)
    private int roundTripMaxTime;
    
    public CertAuthnHelper() {}

    public CertAuthnHelper(String certPickupUrl, Integer rtmt) {

        if (rtmt == null || rtmt < 10) {
            logger.warn("roundTripMaxTime provided is of no practical use. Setting a default value...");
            this.roundTripMaxTime =  DEFAULT_ROUND_TRIP_MAX_TIME;
        } else {
            this.roundTripMaxTime =  rtmt;
        }
        
        this.certPickupUrl = certPickupUrl;
        this.key = ("" + RND.nextDouble()).substring(2);

    }
    
    public String buildRedirectUrl() throws StringEncrypter.EncryptionException {

        String encKey = encrypter.encrypt(key);
        encKey = URLEncoder.encode(encKey, UTF_8);

        cs.put(roundTripMaxTime, key, System.currentTimeMillis() + 1000L*roundTripMaxTime);
        return certPickupUrl + "?" + RND_KEY + "=" + encKey;
        
    }

    public String getCertPEM() {

        //See class io.jans.casa.plugins.certauthn.vm.CertAuthnVM
        String cert = Optional.ofNullable(cs.get(key)).map(Object::toString).orElse(null);        
        if (cert != null) {
            cs.remove(key);
            
            if (cert.equals("(null)")) {    //Apache server may send '(null)'
                cert = null;
            }
        }
        return cert;
        
    }

    public static String decryptValue(String val) throws StringEncrypter.EncryptionException {
        return encrypter.decrypt(val);
    }
    
}
