package io.jans.casa.plugins.certauthn.vm;

import io.jans.as.model.util.Base64Util;            
import io.jans.casa.core.pojo.User;
import io.jans.casa.misc.*;
import io.jans.casa.model.ApplicationConfiguration;
import io.jans.casa.plugins.certauthn.model.Certificate;
import io.jans.casa.plugins.certauthn.service.CertService;
import io.jans.casa.service.*;
import io.jans.casa.ui.UIUtils;
import io.jans.inbound.oauth2.*;
import io.jans.util.security.StringEncrypter;

import java.util.*;
import java.net.URISyntaxException;

import org.json.JSONObject;
import org.slf4j.*;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This is the ViewModel of page cert-detail.zul. It controls the display of user certs
 */
public class CertAuthenticationSummaryVM {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private ISessionContext sessionContext;
    
    private IPersistenceService ips;
    private StringEncrypter encrypter;

    private String serverUrl;
    private String certDetailUrl;
    private String userId;
    private User user;
    private List<Certificate> certificates;
    private CertService certService;
    private SndFactorAuthenticationUtils sndFactorUtils;

    public List<Certificate> getCertificates() {
        return certificates;
    }

    @Init
    public void init() throws StringEncrypter.EncryptionException {

        user = sessionContext.getLoggedUser();
        userId = user.getId();
        
        certService = CertService.getInstance();
        certificates = certService.getUserCerts(userId);

        ips = Utils.managedBean(IPersistenceService.class);
        sndFactorUtils = Utils.managedBean(SndFactorAuthenticationUtils.class);
        encrypter = Utils.stringEncrypter();
                    
        serverUrl = ips.getIssuerUrl();
        certDetailUrl = WebUtils.getServletRequest().getRequestURL().toString();
        logger.debug("CertAuthenticationSummaryVM initialized");
        
        if (WebUtils.getQueryParam("code") != null) {   //This is the OpenID code param at the redirect uri
            logger.debug("Agama flow for enrollment finished. Redirect URI hit");
            
            //There is no need to verify the code received: If someone tries to hit this URL directly with a
            //fake code, access is allowed as long as there is an active session already. There is no request 
            //for an access token here. When there is no session, a "not authorized" error paeg will be shown
            sndFactorUtils.notifyEnrollment(user, CertService.AGAMA_FLOW);
        }
        
    }
    
    public void redirect() throws URISyntaxException, StringEncrypter.EncryptionException {

        String casaClientId = ips.get(ApplicationConfiguration.class, "ou=casa,ou=configuration,o=jans")
                .getSettings().getOidcSettings().getClient().getClientId();         
        
        Map<String, String> map = Map.of("uidRef", encrypter.encrypt(user.getUserName()));
        String inputs = new JSONObject(map).toString();                    
        inputs = Base64Util.base64urlencode(inputs.getBytes(UTF_8));
                    
        OAuthParams params = makeOAuthParams(casaClientId, certDetailUrl, inputs);
        CodeGrantUtil cgu = new CodeGrantUtil(params);         
        
        io.jans.util.Pair<String, String> pair = cgu.makeAuthzRequest();
        Executions.sendRedirect(pair.getFirst());
        
    }

    public void download(Certificate certificate) {
        
        String fileName = Optional.ofNullable(certificate.getCommonName())
                .map(s -> s.replaceAll("[^\\w ]+", "_")).orElse("");
        fileName = fileName.length() == 0 ? "cert" : fileName;
        Filedownload.save(certificate.getPemContent(), "application/x-pem-file", fileName + ".pem");
        
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
                            boolean success = certService.removeFromUser(certificate, userId);                           
                            
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

    private OAuthParams makeOAuthParams(String clientId, String redirectUri, String flowInputs) {
        
        OAuthParams p = new OAuthParams();
        p.setAuthzEndpoint(serverUrl + "/jans-auth/restv1/authorize");
        p.setTokenEndpoint(serverUrl + "/jans-auth/restv1/token");
        p.setClientId(clientId);
        p.setScopes(Collections.singletonList("openid"));
        p.setRedirectUri(redirectUri);
        
        p.setCustParamsAuthReq(
            Map.of(
                "prompt", "login",
                "acr_values", "agama_" + CertService.AGAMA_ENROLLMENT_FLOW + "-" + flowInputs
            )
        );
        
        return p;

    }

}
