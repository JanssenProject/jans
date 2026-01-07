package io.jans.casa.plugins.certauthn.extension;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.plugins.certauthn.service.CertService;

import java.util.*;
import java.util.stream.Collectors;

import org.pf4j.Extension;
import org.slf4j.*;

@Extension
public class CertAuthnMethod implements AuthnMethod {
    
	private CertService certService;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public CertAuthnMethod() {
		certService = CertService.getInstance();
	}

	public String getUINameKey() {
        return "usrcert.cert_label";
	}

	public String getPanelTitleKey() {
        return "usrcert.cert_title";
	}

	public String getPanelTextKey() {
        return "usrcert.cert_text";
	}

	public String getPanelButtonKey() {
        return "usrcert.cert_manage";
	}

	public String getPageUrl() {
        return "cert-detail.zul";
	}

	public String getAcr() {
	    return CertService.AGAMA_FLOW;
	}

	public void reloadConfiguration() {		
	    certService.reloadConfiguration();
	}

	public List<BasicCredential> getEnrolledCreds(String id) {

        try {
            return certService.getUserCerts(id).stream()
                    .map(cert -> new BasicCredential(cert.getFormattedName(), -1)).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
        
	}

	public int getTotalUserCreds(String id) {
	    return getEnrolledCreds(id).size();
	}

}
