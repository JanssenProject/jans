package org.gluu.u2f.service.persist;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.fido.model.entry.DeviceRegistration;
import org.gluu.fido.model.entry.DeviceRegistrationStatus;
import org.gluu.fido2.ctap.AttestationFormat;
import org.gluu.fido2.ctap.CoseEC2Algorithm;
import org.gluu.fido2.model.entry.Fido2RegistrationData;
import org.gluu.fido2.model.entry.Fido2RegistrationEntry;
import org.gluu.fido2.model.entry.Fido2RegistrationStatus;
import org.gluu.fido2.service.persist.RegistrationPersistenceService;
import org.gluu.oxauth.model.config.StaticConfiguration;

/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

import org.gluu.oxauth.service.common.UserService;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.service.net.NetworkService;
import org.gluu.util.StringHelper;

/**
 * Provides search operations with user U2F devices
 *
 * @author Yuriy Movchan Date: 05/27/2020
 */
@ApplicationScoped
public class DeviceRegistrationService {

	@Inject
	private PersistenceEntryManager persistenceEntryManager;
	
	@Inject
	private RegistrationPersistenceService registrationPersistenceService;

	@Inject
	private UserService userService;

	@Inject
	private StaticConfiguration staticConfiguration;

	@Inject
	private NetworkService networkService;

	public boolean containsBranch(final String baseDn) {
		return persistenceEntryManager.contains(baseDn, SimpleBranch.class);
	}

	public List<DeviceRegistration> findAllRegisteredByUsername(String username, String domain, String... returnAttributes) {
		String userInum = userService.getUserInum(username);
		if (userInum == null) {
			return Collections.emptyList();
		}

		String baseDn = getBaseDnForU2fUserDevices(userInum);

		if (persistenceEntryManager.hasBranchesSupport(baseDn)) {
			if (!containsBranch(baseDn)) {
				return Collections.emptyList();
			}
		}

		Filter resultFilter = Filter.createEqualityFilter("oxStatus", DeviceRegistrationStatus.ACTIVE.getValue());

		List<DeviceRegistration> fidoRegistrations = persistenceEntryManager.findEntries(baseDn, DeviceRegistration.class, resultFilter,
				returnAttributes);

		fidoRegistrations = fidoRegistrations.parallelStream()
				.filter(f -> StringHelper.equals(domain, networkService.getHost(f.getApplication()))).collect(Collectors.toList());

		return fidoRegistrations;
	}

	public void migrateToFido2(List<DeviceRegistration> fidoRegistrations, String documentDomain, String username) {
		for (DeviceRegistration fidoRegistration: fidoRegistrations) {

			Fido2RegistrationData fido2RegistrationData = convertToFido2RegistrationData(documentDomain, username, fidoRegistration);

			// Save converted Fido2 entry
			Fido2RegistrationEntry fido2RegistrationEntry = registrationPersistenceService.buildFido2RegistrationEntry(fido2RegistrationData);
			fido2RegistrationEntry.setPublicKeyId(fido2RegistrationData.getPublicKeyId());
			persistenceEntryManager.persist(fido2RegistrationEntry);
	        
	        // Mark Fido registration entry as migrated
	        fidoRegistration.setStatus(DeviceRegistrationStatus.MIGRATED);
	        fidoRegistration.setDeletable(false);
	        
	        persistenceEntryManager.merge(fidoRegistration);
		}
	}

	protected Fido2RegistrationData convertToFido2RegistrationData(String documentDomain, String username,
			DeviceRegistration fidoRegistration) {
		Fido2RegistrationData registrationData = new Fido2RegistrationData();
		
		registrationData.setCreatedDate(fidoRegistration.getCreationDate());
		registrationData.setUpdatedDate(new Date());
		registrationData.setCreatedBy(username);
		registrationData.setUpdatedBy(username);

		registrationData.setUsername(username);
		registrationData.setDomain(documentDomain);

		// TODO: Fix key conversion
		registrationData.setUncompressedECPoint(fidoRegistration.getDeviceRegistrationConfiguration().getPublicKey());
		registrationData.setPublicKeyId(fidoRegistration.getKeyHandle());

		registrationData.setCounter((int) fidoRegistration.getCounter());
		if (registrationData.getCounter() == -1) {
			registrationData.setCounter(0);
		}
		
		registrationData.setType("public-key");
		registrationData.setAttestationType(AttestationFormat.fido_u2f.getFmt());
		registrationData.setSignatureAlgorithm(CoseEC2Algorithm.ES256.getNumericValue());

		registrationData.setStatus(Fido2RegistrationStatus.registered);
		
		registrationData.setApplicationId(fidoRegistration.getApplication());

		return registrationData;
	}

	/**
	 * Build DN string for U2F user device
	 */
	public String getDnForU2fDevice(String userInum, String oxId) {
		String baseDnForU2fDevices = getBaseDnForU2fUserDevices(userInum);
		if (StringHelper.isEmpty(oxId)) {
			return baseDnForU2fDevices;
		}
		return String.format("oxId=%s,%s", oxId, baseDnForU2fDevices);
	}

	public String getBaseDnForU2fUserDevices(String userInum) {
		final String peopleDn = staticConfiguration.getBaseDn().getPeople();
		return String.format("ou=fido,inum=%s,%s", userInum, peopleDn);
	}

}
