package io.jans.casa.plugins.emailotp.service;

import io.jans.casa.credential.BasicCredential;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.emailotp.model.EmailPerson;
import io.jans.casa.service.IPersistenceService;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the CRUD (management) of credentials
 */
public class EmailOtpService {
    
    public static String AGAMA_FLOW = "io.jans.casa.authn.emailotp";
    
	private IPersistenceService persistenceService;
    private Logger logger = LoggerFactory.getLogger(getClass());    
    
	private static EmailOtpService SINGLE_INSTANCE = null;

    private EmailOtpService() {
		persistenceService = Utils.managedBean(IPersistenceService.class);
    }

	public static EmailOtpService getInstance() {
	    
		if (SINGLE_INSTANCE == null) {
            SINGLE_INSTANCE = new EmailOtpService();
		}
		return SINGLE_INSTANCE;
		
	}
	
	public List<String> emailsOf(String inum) {	    
	    EmailPerson p = persistenceService.get(EmailPerson.class, persistenceService.getPersonDn(inum));
	    return Optional.ofNullable(p).map(EmailPerson::getEmails).orElse(Collections.emptyList());
	}
	
}
