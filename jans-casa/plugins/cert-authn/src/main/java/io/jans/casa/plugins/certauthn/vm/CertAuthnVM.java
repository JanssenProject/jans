package io.jans.casa.plugins.certauthn.vm;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.as.model.util.CertUtils;
import io.jans.casa.core.pojo.User;
import io.jans.casa.misc.*;
import io.jans.casa.plugins.certauthn.model.*;
import io.jans.casa.plugins.certauthn.service.*;
import io.jans.casa.service.*;
import io.jans.casa.service.SndFactorAuthenticationUtils;
import io.jans.service.cache.CacheProvider;

import java.util.*;
import java.net.URLDecoder;
import java.security.cert.X509Certificate;

import org.slf4j.*;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import static java.nio.charset.StandardCharsets.UTF_8;

//This page may be hosted in a domain other than Casa. Typical ZK interactions (Javascript/Ajax) may
//not work fine due to CORS. It does not pay to get into CORS stuff for this case, so this page should
//just do the processing in the init method and show a static page
public class CertAuthnVM {

    public static final String RND_KEY = "ref";
    
    private static final String CERT_HEADER = "X-ClientCert";
    private static final String AGAMA_CB = "/jans-auth/fl/callback";
    
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private String key;
    private String messageKey;
    private String flowCallbackUrl;
    private boolean present;
    private boolean parsed;
    private boolean valid;
    private boolean enroll;
    
    private UserCertificateMatch userCertMatch;
    private CertService certService;
    private CacheProvider cacheProvider;
    
    public String getMessageKey() {
        return messageKey;
    }
    
    public boolean isEnroll() {
        return enroll;
    }
    
    public String getFlowCallbackUrl() {
        return flowCallbackUrl;
    }
    
    @Init
    public void init() {
        
        try {
            logger.info("Loading certificate validation page...");
            messageKey = "general.error.general";            
            flowCallbackUrl = Utils.managedBean(IPersistenceService.class).getIssuerUrl() + AGAMA_CB;
         
            cacheProvider = Utils.managedBean(CacheProvider.class);
            certService = CertService.getInstance();    
            if (!certService.isHasValidProperties()) {
                logger.warn("Configuration errors detected. Please check the log file and plugin documentation");
                messageKey = "usrcert.config_problems";
                return;
            }
            
            String encKey = WebUtils.getQueryParam(RND_KEY);        
            if (Utils.isEmpty(encKey)) {
                logger.warn("Expected parameter '{}' not specified in URL.", RND_KEY);
                return;
            }
    
            key = URLDecoder.decode(Utils.stringEncrypter().decrypt(encKey), UTF_8);            
            Reference ref = getReference(key);
            if (ref == null) {
                logger.warn("Expired or missing cache entry. Check if roundTripMaxTime property is too low");
                return;
            }
            
            String userId = ref.getUserId();
            enroll = ref.isEnroll();
            if (userId == null) {
                logger.warn("UserId missing in cache entry");
                return;
            }
            
            X509Certificate userCert = processCert(WebUtils.getRequestHeader(CERT_HEADER));
            //If parsed is true, present is too
            //If valid is true, parsed is too
            userCertMatch = valid ? certService.processMatch(userCert, userId, enroll) : null;
            boolean success = UserCertificateMatch.SUCCESS.equals(userCertMatch);
            
            computeMessageKey();
            String outcome = "";
            
            if (!success) {
                outcome = Labels.getLabel(messageKey);
                logger.error("Cert processing failure. Reason: {}", outcome);
            }
            
            if (!enroll) {                
                //number of seconds available for returning to agama flow in a safe manner
                Long timeLeft = (ref.getExpiresAt() - System.currentTimeMillis()) / 1000;
                
                if (timeLeft <= 0) {                    
                    logger.warn("Expired cache entry");
                    return;
                }

                //Rewrite the cache entry with the outcome of the operation
                cacheProvider.put(timeLeft.intValue(), key, outcome);
                
                if (success) {      //trigger inmmediate redirect. No need to display a success message
                    logger.info("Redirecting for completion of authentication flow");
                    WebUtils.execRedirect(flowCallbackUrl, false);
                }
                return;
            }
            
            if (success) {
                IPersistenceService ps = Utils.managedBean(IPersistenceService.class);
                Minion p = ps.get(Minion.class, ps.getPersonDn(userId));
                
                User user = new User();
                user.setUserName(p.getUid());
                user.setId(p.getInum());
                user.setPreferredMethod(p.getPreferredMethod());
                
                Utils.managedBean(SndFactorAuthenticationUtils.class).notifyEnrollment(user, CertService.AGAMA_FLOW);
            }
            
        } catch (Exception e) {
            messageKey = "general.error.general";
            logger.error(e.getMessage(), e);
        }
        
    }
   
    private X509Certificate processCert(String clientCertString) {

        X509Certificate clientCert = null;
        try {            
            if (Utils.isEmpty(clientCertString) || clientCertString.equals("(null)")) {
                logger.warn("No data in header '{}'", CERT_HEADER); 
            } else {
                present = true;
                clientCert = CertUtils.x509CertificateFromPem(clientCertString);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        if (clientCert == null) {
            logger.warn("No client certificate was found. Probably the user hit the Cancel button in the browser prompt");
        } else {
            //parsing was successful
            parsed = true;
            //apply applicable validations
            valid = certService.validate(clientCert);
        }
        return clientCert;
        
    }
    
    private Reference getReference(String k) throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        String str = Optional.ofNullable(cacheProvider.get(k)).map(Object::toString).orElse(null);
        
        if (str == null) return null;
        return mapper.readValue(str, Reference.class);

    }   
    
    private void computeMessageKey() {
        
        if (!present) {
            messageKey = "usrcert.not_selected";
        } else if (!parsed) {
            messageKey = "usrcert.unparsable";
        } else if (!valid) {
            messageKey = "usrcert.not_valid";
        } else {
            //match is not null here
            messageKey = "usrcert.match_" + userCertMatch.name();
        }
        
    }
    
}
