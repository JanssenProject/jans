package io.jans.casa.plugins.certauthn.extension;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.plugins.certauthn.service.CertService;

import java.util.*;

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
		return "usercert.method_label";
	}

	public String getPanelTitleKey() {
		return "usercert.method_title";
	}

	public String getPanelTextKey() {
		return "usercert.method_text";
	}

	public String getPanelButtonKey() {
		return "usercert.method_button_label";
	}

	public String getPageUrl() {
		return "cert-detail.zul";
	}

	public String getAcr() {
	    return certService.AGAMA_FLOW;
	}

	public void reloadConfiguration() {		
	    certService.reloadConfiguration();
	}

	public List<BasicCredential> getEnrolledCreds(String id) {
	    //Code the logic required to build a list of the credentials already enrolled
	    //by the user whose unique identifier is id

	    //if (certService.getUserColor(id) == null) {
	        return Collections.emptyList();
	    //}
        //In practice you should create instances of BasicCredential and fill them with data appropriately 
	    //return Collections.singletonList(new BasicCredential("My color", 0L));

	}

	public int getTotalUserCreds(String id) {
	    //Code the logic required to compute the number of the credentials already enrolled
	    //by the user whose unique identifier is id. Calling size over the returned value of
	    //method getEnrolledCreds is an option
	    return getEnrolledCreds(id).size();
	}

}
