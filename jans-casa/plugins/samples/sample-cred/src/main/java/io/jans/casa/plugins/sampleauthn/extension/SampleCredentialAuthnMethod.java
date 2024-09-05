package io.jans.casa.plugins.sampleauthn.extension;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.plugins.sampleauthn.service.SampleCredentialService;

import java.util.*;

import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class SampleCredentialAuthnMethod implements AuthnMethod {

	private SampleCredentialService credService;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public SampleCredentialAuthnMethod() {
		credService = SampleCredentialService.getInstance();
	}

	public String getUINameKey() {
	    //see zk-label.properties
		return "sample.method_label";
	}

	public String getPanelTitleKey() {
	    //see zk-label.properties
		return "sample.method_title";
	}

	public String getPanelTextKey() {
	    //see zk-label.properties
		return "sample.method_text";
	}

	public String getPanelButtonKey() {
	    //see zk-label.properties
		return "sample.method_button_label";
	}

	public String getPageUrl() {
	    //Path (relative to the plugin's base url) to the page where management of these credentials will take place
		return "user/cred_details.zul";
	}

	public String getAcr() {
	    //The flow associated to this type of credential
	    return credService.AGAMA_FLOW;
	}

	public void reloadConfiguration() {
	    //See javadocs of method reloadConfiguration in
	    //https://github.com/JanssenProject/jans/blob/main/jans-casa/shared/src/main/java/io/jans/casa/extension/AuthnMethod.java
		
	    //credService.reloadConfiguration();
	}

	public List<BasicCredential> getEnrolledCreds(String id) {
	    //Code the logic required to build a list of the credentials already enrolled
	    //by the user whose unique identifier is id

	    if (credService.getUserColor(id) == null) {
	        return Collections.emptyList();
	    }
        //In practice you should create instances of BasicCredential and fill them with data appropriately 
	    return Collections.singletonList(new BasicCredential("My color", 0L));

	}

	public int getTotalUserCreds(String id) {
	    //Code the logic required to compute the number of the credentials already enrolled
	    //by the user whose unique identifier is id. Calling size over the returned value of
	    //method getEnrolledCreds is an option
	    return getEnrolledCreds(id).size();
	}

}
