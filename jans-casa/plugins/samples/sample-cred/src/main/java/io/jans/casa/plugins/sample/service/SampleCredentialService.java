package io.jans.casa.plugins.sample.service;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.misc.Utils;
import io.jans.casa.service.IPersistenceService;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the CRUD (management) of credentials
 */
public class SampleCredentialService {
    
    public static String ACR = "sample_cred_acr";
    
	private IPersistenceService persistenceService;
    private Logger logger = LoggerFactory.getLogger(getClass());    
    
    private Map<String, String> properties;
	private static SampleCredentialService SINGLE_INSTANCE = null;

    private SampleCredentialService() {
		persistenceService = Utils.managedBean(IPersistenceService.class);
		reloadConfiguration();
    }

	public static SampleCredentialService getInstance() {
	    
		if (SINGLE_INSTANCE == null) {
            SINGLE_INSTANCE = new SampleCredentialService();
		}
		return SINGLE_INSTANCE;
		
	}
	
	public void reloadConfiguration() {
	    //Retrieve configuration properties of the script, if any
		properties = persistenceService.getCustScriptConfigProperties(ACR);
        //Put other initialization stuff here
	}
        
	public List<BasicCredential> getEnrolledCreds(String id) {
	    //Code the logic required to build a list of the credentials already enrolled
	    //by the user whose unique identifier is id
	    
	    return Collections.emptyList();
	}
	
	public int getTotalUserCreds(String id) {
	    //Code the logic required to compute the number of the credentials already enrolled
	    //by the user whose unique identifier is id. Calling size over the returned value of
	    //method getEnrolledCreds is an option
	    return 0;
	}
	
	//Likely, other methods for credential manipulation will go here.
	//These would be called from class SampleCredentialVM which handles UI interaction 

}
