package io.jans.casa.plugins.sample.extension;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.plugins.sample.service.SampleCredentialService;

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
	    //ACR associated to this type of credential. A custom script has to exist in the server
	    //and must use this acr as display name
	    return credService.ACR;
	}

	public void reloadConfiguration() {
	    //See javadocs of method reloadConfiguration in
	    //https://github.com/JanssenProject/jans/blob/main/jans-casa/shared/src/main/java/io/jans/casa/extension/AuthnMethod.java
		
	    //credService.reloadConfiguration();
	}

	public List<BasicCredential> getEnrolledCreds(String id) {
	    //See javadocs of method getEnrolledCreds in
	    //https://github.com/JanssenProject/jans/blob/main/jans-casa/shared/src/main/java/io/jans/casa/extension/AuthnMethod.java

	    //return credService.getEnrolledCreds(id);
	    return Collections.emptyList();
	}

	public int getTotalUserCreds(String id) {
	    //See javadocs of method getTotalUserCreds in
	    //https://github.com/JanssenProject/jans/blob/main/jans-casa/shared/src/main/java/io/jans/casa/extension/AuthnMethod.java
	    
	    //return credService.getTotalUserCreds(id);
	    return 0;
	}

}
