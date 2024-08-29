package io.jans.casa.plugins.sampleauthn.service;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.sampleauthn.model.PersonColor;
import io.jans.casa.service.IPersistenceService;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the CRUD (management) of credentials
 */
public class SampleCredentialService {
    
    public static String AGAMA_FLOW = "com.acme.authn.color";
    
	private IPersistenceService persistenceService;
    private Logger logger = LoggerFactory.getLogger(getClass());    
    
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
	    //Retrieve configuration properties of the script, not necessary in this case 
		//persistenceService.getAgamaFlowConfigProperties(AGAMA_FLOW);
        //Put other initialization stuff here
	}
	
	//Likely, other methods for credential manipulation will go here.
	//These would be called from class SampleCredentialVM which handles UI interaction 

	public String getUserColor(String id) {
	    //retrieve the user from DB using PersonColor as object representation
	    PersonColor person = persistenceService.get(PersonColor.class, persistenceService.getPersonDn(id));
	    logger.debug("User's color is {}", person.getColor());
	    return person.getColor();
	}
	
	public boolean storeUserColor(String id, String newColor) {
	    PersonColor person = persistenceService.get(PersonColor.class, persistenceService.getPersonDn(id));
	    person.setColor(newColor);
	    return persistenceService.modify(person);
	}
	
}
