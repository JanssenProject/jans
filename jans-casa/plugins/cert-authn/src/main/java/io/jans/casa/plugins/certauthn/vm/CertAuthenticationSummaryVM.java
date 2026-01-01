package io.jans.casa.plugins.certauthn.vm;

import io.jans.casa.core.pojo.User;
import io.jans.casa.misc.*;
import io.jans.casa.plugins.certauthn.model.*;
import io.jans.casa.plugins.certauthn.service.CertService;
import io.jans.casa.service.ISessionContext;
import io.jans.casa.service.SndFactorAuthenticationUtils;
import io.jans.casa.ui.UIUtils;
import io.jans.service.cache.CacheProvider;
import io.jans.util.security.StringEncrypter;

import java.util.List;
import java.net.URLEncoder;
import java.security.SecureRandom;

import org.json.JSONObject;
import org.slf4j.*;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Messagebox;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This is the ViewModel of page cert-detail.zul. It controls the display of user certs
 */
public class CertAuthenticationSummaryVM {

    //Max. time it can take from the redirect to the cert pickup url until the user 
    //effectively selects the cert
    private static final int ENTRY_EXP_SECONDS = 20;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private ISessionContext sessionContext;

    private CacheProvider cacheProvider;

    private String userId;
    private User user;
    private List<Certificate> certificates;
    private CertService certService;
    private SndFactorAuthenticationUtils sndFactorUtils;
    private StringEncrypter stringEncrypter;
    private SecureRandom random;

    public List<Certificate> getCertificates() {
        return certificates;
    }

    @Init
    public void init() throws StringEncrypter.EncryptionException {

        user = sessionContext.getLoggedUser();
        userId = user.getId();
        
        random = new SecureRandom();
        certService = CertService.getInstance();
        certificates = certService.getUserCerts(userId);
        
        stringEncrypter = Utils.stringEncrypter();
        cacheProvider = Utils.managedBean(CacheProvider.class);
        sndFactorUtils = Utils.managedBean(SndFactorAuthenticationUtils.class);
        logger.debug("CertAuthenticationSummaryVM initialized");
        
    }
    
    public void redirect() throws StringEncrypter.EncryptionException {

        String key = ("" + random.nextDouble()).substring(2);
        String encKey = URLEncoder.encode(stringEncrypter.encrypt(key), UTF_8);
        
        //We cannot store a Reference object straight in the cache because the 
        //the class is not in the class used by the provider
        JSONObject job = new JSONObject(new Reference(userId, true));
        cacheProvider.put(ENTRY_EXP_SECONDS, key, job.toString());

        Executions.getCurrent().sendRedirect(
                certService.getCertPickupUrl() + "?" + CertAuthnVM.RND_KEY + "=" + encKey);
        
    }

    public void delete(Certificate certificate) {

        String resetMessages = sndFactorUtils.removalConflict(CertService.AGAMA_FLOW, certificates.size(), user).getY();
        boolean reset = resetMessages != null;
        Pair<String, String> delMessages = getDeleteMessages(resetMessages);

        Messagebox.show(delMessages.getY(), delMessages.getX(), Messagebox.YES | Messagebox.NO,
                reset ? Messagebox.EXCLAMATION : Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        try {
                            String fingerprint = certificate.getFingerPrint();
                            boolean success = certService.removeFromUser(fingerprint, userId);                           
                            
                            if (success) {
                                logger.info("Certificate {} removed from user account", fingerprint);

                                if (reset) {
                                    sndFactorUtils.turn2faOff(user);
                                }
                                certificates.remove(certificate);

                                BindUtils.postNotifyChange(CertAuthenticationSummaryVM.this, "certificates");
                            } else {
                                logger.error("Failure to remove certificate {} from user account", fingerprint);
                            }
                            
                            UIUtils.showMessageUI(success);
                        } catch (Exception e) {
                            UIUtils.showMessageUI(false);
                            logger.error(e.getMessage(), e);
                        }
                    }
                });

    }

    private Pair<String, String> getDeleteMessages(String msg){

        StringBuilder text=new StringBuilder();
        if (msg != null) {
            text.append(msg).append("\n\n");
        }
        text.append(Labels.getLabel("usercert.del_confirm"));
        if (msg != null) {
            text.append("\n");
        }

        return new Pair<>(null, text.toString());

    }

}
