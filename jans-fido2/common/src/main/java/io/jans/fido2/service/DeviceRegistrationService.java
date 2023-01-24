/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import io.jans.orm.model.fido2.Fido2RegistrationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.entry.DeviceRegistration;
import io.jans.entry.DeviceRegistrationStatus;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.ctap.CoseEC2Algorithm;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.persist.RegistrationPersistenceService;
import io.jans.as.model.config.StaticConfiguration;

/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

import io.jans.as.common.service.common.UserService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.service.net.NetworkService;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Provides search operations with user U2F devices
 *
 * @author Yuriy Movchan Date: 05/27/2020
 */
@ApplicationScoped
public class DeviceRegistrationService {

    @Inject
    private Logger log;

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
	
	@Inject
	private CoseService coseService;

    @Inject
    private Base64Service base64Service;
    
    @Inject
    private DataMapperService dataMapperService;

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

		Filter resultFilter = Filter.createEqualityFilter("jansStatus", DeviceRegistrationStatus.ACTIVE.getValue());

		List<DeviceRegistration> fidoRegistrations = persistenceEntryManager.findEntries(baseDn, DeviceRegistration.class, resultFilter,
				returnAttributes);

		fidoRegistrations = fidoRegistrations.parallelStream()
				.filter(f -> StringHelper.equals(domain, networkService.getHost(f.getApplication())))
				.filter(f -> (f.getDeviceData() == null)) /* Ignore Super Gluu */
				.collect(Collectors.toList());

		return fidoRegistrations;
	}

	public void migrateToFido2(List<DeviceRegistration> fidoRegistrations, String documentDomain, String username) {
		for (DeviceRegistration fidoRegistration: fidoRegistrations) {

			Fido2RegistrationData fido2RegistrationData;
			try {
				fido2RegistrationData = convertToFido2RegistrationData(documentDomain, username, fidoRegistration);
			} catch (IOException ex) {
				log.error("Faield to migrate Fido to Fido2 device: {}" , fidoRegistration.getId());
				continue;
			}

			// Save converted Fido2 entry
			Date enrollmentDate = fidoRegistration.getCreationDate();
			Fido2RegistrationEntry fido2RegistrationEntry = registrationPersistenceService.buildFido2RegistrationEntry(fido2RegistrationData);
			
			// Restore dates modified by buildFido2RegistrationEntry
			fido2RegistrationEntry.getRegistrationData().setCreatedDate(enrollmentDate);
			fido2RegistrationEntry.setCreationDate(enrollmentDate);
			
			fido2RegistrationEntry.setDisplayName(fidoRegistration.getDisplayName());
			fido2RegistrationEntry.setPublicKeyId(fido2RegistrationData.getPublicKeyId());
			persistenceEntryManager.persist(fido2RegistrationEntry);

//			Testing code
//			JsonNode uncompressedECPointNode;
//			try {
//				uncompressedECPointNode = dataMapperService.cborReadTree(base64Service.urlDecode(fido2RegistrationData.getUncompressedECPoint()));
//	            PublicKey publicKey = coseService.createUncompressedPointFromCOSEPublicKey(uncompressedECPointNode);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
	        
	        // Mark Fido registration entry as migrated
	        fidoRegistration.setStatus(DeviceRegistrationStatus.MIGRATED);
	        fidoRegistration.setDeletable(false);
	        
	        persistenceEntryManager.merge(fidoRegistration);
		}
	}

	protected Fido2RegistrationData convertToFido2RegistrationData(String documentDomain, String username,
			DeviceRegistration fidoRegistration) throws IOException {
		Fido2RegistrationData registrationData = new Fido2RegistrationData();
		
		registrationData.setCreatedDate(fidoRegistration.getCreationDate());
		registrationData.setUpdatedDate(new Date());
		registrationData.setCreatedBy(username);
		registrationData.setUpdatedBy(username);

		registrationData.setUsername(username);
		registrationData.setDomain(documentDomain);

		JsonNode uncompressedECPoint = coseService.convertECKeyToUncompressedPoint(
				base64Service.urlDecode(fidoRegistration.getDeviceRegistrationConfiguration().getPublicKey()));
		registrationData.setUncompressedECPoint(base64Service.urlEncodeToString(dataMapperService.cborWriteAsBytes(uncompressedECPoint)));

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
	public String getDnForU2fDevice(String userInum, String jsId) {
		String baseDnForU2fDevices = getBaseDnForU2fUserDevices(userInum);
		if (StringHelper.isEmpty(jsId)) {
			return baseDnForU2fDevices;
		}
		return String.format("jansId=%s,%s", jsId, baseDnForU2fDevices);
	}

	public String getBaseDnForU2fUserDevices(String userInum) {
		final String peopleDn = staticConfiguration.getBaseDn().getPeople();
		return String.format("ou=fido,inum=%s,%s", userInum, peopleDn);
	}

}
