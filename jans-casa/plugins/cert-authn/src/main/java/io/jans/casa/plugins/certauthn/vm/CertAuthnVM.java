package io.jans.casa.plugins.certauthn.vm;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.as.model.util.CertUtils;
import io.jans.casa.core.pojo.User;
import io.jans.casa.misc.Utils;
import io.jans.casa.misc.WebUtils;
import io.jans.casa.plugins.certauthn.service.*;
import io.jans.casa.service.*;
import io.jans.casa.service.SndFactorAuthenticationUtils;
import io.jans.service.cache.CacheProvider;
import io.jans.util.security.StringEncrypter;

import java.util.*;
import java.security.cert.X509Certificate;

import org.slf4j.*;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.select.annotation.WireVariable;

public class CertAuthnVM {

    private static final String RND_KEY = "ref";
    private static final String CERT_HEADER = "X-ClientCert";
    private static final int STATUS_EXP = 10;    //max. number of seconds to go from index.zul back to agama flow 

    private Logger logger = LoggerFactory.getLogger(getClass());
    private CertService certService;
    private StringEncrypter stringEncrypter;

    @WireVariable
    private ISessionContext sessionContext;

    @WireVariable
    private CacheProvider cacheProvider;

    private User user;
    private boolean hasConfigErrors;
    private boolean present;
    private boolean parsed;
    private boolean inCasaSession;
    private boolean valid;
        
    private String errorMessage;
    private UserCertificateMatch userCertMatch;
    private String userId;    

    public String errorMessage() {
        return errorMessage;
    }

    public boolean isHasConfigErrors() {
        return hasConfigErrors;
    }

    public String getUserId() {
        return userId;
    }

    public UserCertificateMatch getUserCertMatch() {
        return userCertMatch;
    }

    //See LocationMatch directive in Apache's https_gluu.conf
    @Init
    public void init() throws Exception {

        logger.info("Loading certificate validation page...");
        user = sessionContext.getLoggedUser();

        //Truthy value means usage of this page is in the context of enrollment only (not authentication)
        inCasaSession = user != null;
        logger.info("There is{} user in session", inCasaSession ? "": " no");

        String encKey = WebUtils.getQueryParam(RND_KEY);
        String key = null;
        
        if (inCasaSession) {
            userId = user.getId();
        } else {
            if (Utils.isEmpty(encKey)) {
                logger.warn("Expected parameter '{}' not specified in URL.", RND_KEY);
            } else {
                key = Utils.stringEncrypter().decrypt(encKey);                
                userId = Optional.ofNullable(cacheProvider.get(key)).map(Object::toString).orElse(null);                
            }

            if (userId != null) {
                logger.debug("User id is {}", userId);
            } else {
                logger.error("No user ID could be obtained. Aborting...");
                return;
            }
        }

        certService = CertService.getInstance();
        hasConfigErrors = !certService.isHasValidProperties();

        if (hasConfigErrors) {
            logger.info("Configuration errors were detected. Please check the log file and plugin documentation");
            return;
        }

        X509Certificate userCert = processCert();
        //If parsed is true, present is too
        //If valid is true, parsed is too
        userCertMatch = valid ? certService.processMatch(userCert, userId, inCasaSession) : null;
        String status = makeOutputStatus();

        if (inCasaSession) {
        	if (userCertMatch.equals(UserCertificateMatch.SUCCESS)) {
        		Utils.managedBean(SndFactorAuthenticationUtils.class).notifyEnrollment(user, CertService.AGAMA_FLOW);
        	}
        } else {
            cacheProvider.put(STATUS_EXP, key, status);

            logger.info("Preparing redirect for completion of authentication flow");
            String url = Utils.managedBean(IPersistenceService.class).getIssuerUrl();
            WebUtils.execRedirect(url + "/jans-auth/fl/callback", true);
        }

    }

    private X509Certificate processCert() {

        X509Certificate clientCert = null;
        String clientCertString = WebUtils.getRequestHeader(CERT_HEADER);

        try {
            if (Utils.isEmpty(clientCertString)) {
                String attribute = "javax.servlet.request.X509Certificate";
                Optional<?> optAttr = Optional.ofNullable(WebUtils.getServletRequest().getAttribute(attribute));

                if (optAttr.isPresent()) {
                    logger.info("Got a certificate in request attribute '{}'", attribute);
                    present = true;
                    clientCert = optAttr.map(X509Certificate[].class::cast).map(certs -> certs[0]).orElse(null);
                }

            } else {
                logger.info("Got a certificate in request header '{}'", CERT_HEADER);
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

    private String makeOutputStatus() {
        
        if (!present) {
            errorMessage = Labels.getLabel("usrcert.not_selected");
            return "NOT_SELECTED";
        }
        if (!parsed) {
            errorMessage = Labels.getLabel("usrcert.unparsable");
            return "UNPARSABLE";
        }
        if (!valid) {
            errorMessage = Labels.getLabel("usrcert.not_valid");
            return "NOT_VALID";
        }
        //match is not null here
        return userCertMatch.name();
        
    }

}
