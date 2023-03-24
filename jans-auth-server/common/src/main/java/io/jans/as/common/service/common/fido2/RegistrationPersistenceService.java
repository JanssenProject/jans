/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service.common.fido2;

import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.slf4j.Logger;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.inject.Inject;

/**
 * Abstract class for registrations that are persisted under Person Entry
 * @author madhumitas
 *
 */

public abstract class RegistrationPersistenceService {

	@Inject
	protected Logger log;

	@Inject
	protected  PersistenceEntryManager persistenceEntryManager;

	@Inject
	protected  UserService userService;

	@Inject
	protected StaticConfiguration staticConfiguration;
	
    public void save(Fido2RegistrationEntry registrationEntry) {
        prepareBranch(registrationEntry.getUserInum());

        persistenceEntryManager.persist(registrationEntry);
    }

    public void update(Fido2RegistrationEntry registrationEntry) {
        prepareBranch(registrationEntry.getUserInum());

        Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();

        Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();
        registrationData.setUpdatedDate(now);
        registrationData.setUpdatedBy(registrationData.getUsername());

        registrationEntry.setRegistrationStatus(registrationData.getStatus());

        persistenceEntryManager.merge(registrationEntry);
    }

    public void addBranch(final String baseDn) {
        SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName("fido2_register");
        branch.setDn(baseDn);

        persistenceEntryManager.persist(branch);
    }

    public boolean containsBranch(final String baseDn) {
        return persistenceEntryManager.contains(baseDn, SimpleBranch.class);
    }

    public String prepareBranch(final String userInum) {
        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (!persistenceEntryManager.hasBranchesSupport(baseDn)) {
        	return baseDn;
        }

        // Create Fido2 base branch for registration entries if needed
        if (!containsBranch(baseDn)) {
            addBranch(baseDn);
        }
        
        return baseDn;
    }

    public Fido2RegistrationEntry findRegisteredUserDevice(String userInum, String deviceId, String... returnAttributes) {
        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (persistenceEntryManager.hasBranchesSupport(baseDn)) {
        	if (!containsBranch(baseDn)) {
                return null;
        	}
        }

    	String deviceDn = getDnForRegistrationEntry(userInum, deviceId);

        return persistenceEntryManager.find(deviceDn, Fido2RegistrationEntry.class, returnAttributes);
    }

    public List<Fido2RegistrationEntry> findByRpRegisteredUserDevices(String userName, String rpId, String ... returnAttributes) {
		String userInum = userService.getUserInum(userName);
		if (userInum == null) {
			return Collections.emptyList();
		}

		String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (persistenceEntryManager.hasBranchesSupport(baseDn)) {
        	if (!containsBranch(baseDn)) {
                return Collections.emptyList();
        	}
        }

        Filter userInumFilter = Filter.createEqualityFilter("personInum", userInum);
        Filter registeredFilter = Filter.createEqualityFilter("jansStatus", Fido2RegistrationStatus.registered.getValue());
        Filter filter = null;
        if (StringHelper.isNotEmpty(rpId)) {
        	Filter appIdFilter = Filter.createEqualityFilter("jansApp", rpId);
        	filter = Filter.createANDFilter(userInumFilter, registeredFilter, appIdFilter);
        }
        else
        {
        	filter = Filter.createANDFilter(userInumFilter, registeredFilter);
        }
        List<Fido2RegistrationEntry> fido2RegistrationnEntries = persistenceEntryManager.findEntries(baseDn, Fido2RegistrationEntry.class, filter, returnAttributes);

        return fido2RegistrationnEntries;
    }
    
    
    public boolean attachDeviceRegistrationToUser(String userInum, String deviceDn) {
		Fido2RegistrationEntry registrationEntry = persistenceEntryManager.find(Fido2RegistrationEntry.class, deviceDn);
		if (registrationEntry == null) {
			return false;
		}
		
		User user = userService.getUserByInum(userInum, "uid");
		if (user == null) {
			return false;
		}
		
		persistenceEntryManager.remove(deviceDn, Fido2RegistrationEntry.class);
		
        final String id = UUID.randomUUID().toString();

        String userAttestationDn = getDnForRegistrationEntry(userInum, id);
        registrationEntry.setId(id);
        registrationEntry.setDn(userAttestationDn);
        registrationEntry.setUserInum(userInum);

		Fido2RegistrationData registrationData = registrationEntry.getRegistrationData();    
		registrationData.setUsername(user.getUserId());
		registrationEntry.clearExpiration();
		
		save(registrationEntry);

		return true;
    }
    
    public Fido2RegistrationEntry findOneStepUserDeviceRegistration(String deviceDn) {
		Fido2RegistrationEntry registrationEntry = persistenceEntryManager.find(Fido2RegistrationEntry.class, deviceDn);
		
		return registrationEntry;
    }

    public String getDnForRegistrationEntry(String userInum, String jsId) {
        // Build DN string for Fido2 registration entry
        String baseDn = getBaseDnForFido2RegistrationEntries(userInum);
        if (StringHelper.isEmpty(jsId)) {
            return baseDn;
        }
        return String.format("jansId=%s,%s", jsId, baseDn);
    }

    public String getBaseDnForFido2RegistrationEntries(String userInum) {
        final String userBaseDn = getDnForUser(userInum); // "ou=fido2_register,inum=1234,ou=people,o=jans"
        if (StringHelper.isEmpty(userInum)) {
            return userBaseDn;
        }

        return String.format("ou=fido2_register,%s", userBaseDn);
    }

    public String getDnForUser(String userInum) {
        String peopleDn = getBasedPeopleDn();
        if (StringHelper.isEmpty(userInum)) {
            return peopleDn;
        }

        return String.format("inum=%s,%s", userInum, peopleDn);
    }

    public String getBasedPeopleDn() {
    	return staticConfiguration.getBaseDn().getPeople();
    }
    
	public abstract String getUserInum(String userName);

}
