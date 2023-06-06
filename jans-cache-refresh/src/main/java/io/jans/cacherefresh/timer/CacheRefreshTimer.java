/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.cacherefresh.timer;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

//import javax.inject.Inject;
//import javax.inject.Named;

import io.jans.cacherefresh.CacheRefreshEvent;
import io.jans.cacherefresh.constants.JansConstants;
import io.jans.cacherefresh.external.ExternalCacheRefreshService;
import io.jans.cacherefresh.model.*;
import io.jans.cacherefresh.model.config.AppConfiguration;
import io.jans.cacherefresh.model.config.CacheRefreshConfiguration;
import io.jans.cacherefresh.model.config.CacheRefreshAttributeMapping;
import io.jans.cacherefresh.service.*;
import io.jans.cacherefresh.service.config.ApplicationFactory;
import io.jans.cacherefresh.service.config.ConfigurationFactory;
import io.jans.cacherefresh.util.PropertyUtil;
import io.jans.model.GluuStatus;
import io.jans.model.SchemaEntry;
import io.jans.model.custom.script.model.bind.BindCredentials;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;
import io.jans.orm.ldap.operation.LdapOperationService;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.DummyEntry;
import io.jans.orm.operation.PersistenceOperationService;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;
import io.jans.service.AttributeService;
import io.jans.service.ObjectSerializationService;
import io.jans.service.SchemaService;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.OxConstants;
import io.jans.util.Pair;
import io.jans.util.security.PropertiesDecrypter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

/**
 * Check periodically if source servers contains updates and trigger target
 * server entry update if needed
 * 
 * @author Yuriy Movchan Date: 05.05.2011
 */
@ApplicationScoped
public class CacheRefreshTimer {

	private static final String LETTERS_FOR_SEARCH = "abcdefghijklmnopqrstuvwxyz1234567890.";
	private static final String[] TARGET_PERSON_RETURN_ATTRIBUTES = { JansConstants.inum };

	private static final int DEFAULT_INTERVAL = 60;

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	protected ApplicationFactory applicationFactory;

	@Inject
	protected AttributeService attributeService;

	@Inject
	private ConfigurationFactory configurationFactory;

	@Inject
	private CacheRefreshService cacheRefreshService;

	@Inject
	private PersonService personService;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	@Inject
	private CacheRefreshSnapshotFileService cacheRefreshSnapshotFileService;

	@Inject
	private ExternalCacheRefreshService externalCacheRefreshService;

	@Inject
	private SchemaService schemaService;

	@Inject
	private InumService inumService;

	@Inject
	private AppConfiguration appConfiguration;
	
	@Inject
	private CacheRefrshConfigurationService CacheRefrshConfigurationService;

	@Inject
	private EncryptionService encryptionService;
	
	@Inject
	private PairwiseIdService pairwiseIdService;
	
	@Inject
	private FidoDeviceService fidoDeviceService;
	
	@Inject
	private Fido2DeviceService fido2DeviceService;

	@Inject
	private ObjectSerializationService objectSerializationService;

	private AtomicBoolean isActive;
	private long lastFinishedTime;

	public void initTimer() {
		log.info("Initializing Cache Refresh Timer");
		this.isActive = new AtomicBoolean(false);

		// Clean up previous Inum cache
		CacheRefreshConfiguration cacheRefreshConfiguration = getConfigurationFactory().getAppConfiguration();
		if (cacheRefreshConfiguration != null) {
			String snapshotFolder = cacheRefreshConfiguration.getSnapshotFolder();
			if (StringHelper.isNotEmpty(snapshotFolder)) {
				String inumCachePath = getInumCachePath(cacheRefreshConfiguration);
				objectSerializationService.cleanup(inumCachePath);
			}
		}

		// Schedule to start cache refresh every 1 minute
		timerEvent.fire(new TimerEvent(new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL), new CacheRefreshEvent(),
				Scheduled.Literal.INSTANCE));

		this.lastFinishedTime = System.currentTimeMillis();
	}

	@Asynchronous
	public void process(@Observes @Scheduled CacheRefreshEvent cacheRefreshEvent) {
		if (this.isActive.get()) {
			System.out.println("Another process is active");
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			System.out.println("Failed to start process exclusively");
			return;
		}

		try {
			processInt();
		} finally {
			System.out.println("Allowing to run new process exclusively");
			this.isActive.set(false);
		}
	}

	public void processInt() {
		AppConfiguration currentConfiguration = getConfigurationFactory().getAppConfiguration();
		try {
			//GluuConfiguration currentConfiguration = getConfigurationService().getConfiguration();
			//GluuConfiguration currentConfiguration = new GluuConfiguration();
			currentConfiguration.setVdsCacheRefreshEnabled(true);
			//currentConfiguration.setVdsCacheRefreshPollingInterval();
			currentConfiguration.setCacheRefreshServerIpAddress("255.255.255.255");
			if (!isStartCacheRefresh(currentConfiguration)) {
				System.out.println("Starting conditions aren't reached");
				return;
			}

			processImpl(currentConfiguration);
			updateStatus(currentConfiguration, System.currentTimeMillis());

			this.lastFinishedTime = System.currentTimeMillis();
		} catch (Throwable ex) {
			ex.printStackTrace();
			System.out.println("Exception happened while executing cache refresh synchronization"+ ex);
		}
	}

	private boolean isStartCacheRefresh(AppConfiguration currentConfiguration) {
		if (!currentConfiguration.isVdsCacheRefreshEnabled()) {
			return false;
		}

		long poolingInterval = StringHelper.toInteger(currentConfiguration.getVdsCacheRefreshPollingInterval()) * 60 * 1000;
		if (poolingInterval < 0) {
			return false;
		}

		String cacheRefreshServerIpAddress = currentConfiguration.getCacheRefreshServerIpAddress();
		// if (StringHelper.isEmpty(cacheRefreshServerIpAddress)) {
		// log.debug("There is no master Cache Refresh server");
		// return false;
		// }

		// Compare server IP address with cacheRefreshServerIp
		boolean cacheRefreshServer = false;
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface networkInterface : Collections.list(nets)) {
				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				for (InetAddress inetAddress : Collections.list(inetAddresses)) {
					if (StringHelper.equals(cacheRefreshServerIpAddress, inetAddress.getHostAddress())) {
						cacheRefreshServer = true;
						break;
					}
				}

				if (cacheRefreshServer) {
					break;
				}
			}
		} catch (SocketException ex) {
			log.error("Failed to enumerate server IP addresses"+ ex);
		}

		if (!cacheRefreshServer) {
			//cacheRefreshServer = externalCacheRefreshService.executeExternalIsStartProcessMethods();
			cacheRefreshServer = true;
		}

		if (!cacheRefreshServer) {
			System.out.println("This server isn't master Cache Refresh server");
			return false;
		}

		// Check if cache refresh specific configuration was loaded
		if (currentConfiguration == null) {
			log.info("Failed to start cache refresh. Can't loading configuration from oxTrustCacheRefresh.properties");
			return false;
		}

		long timeDiffrence = System.currentTimeMillis() - this.lastFinishedTime;

		return timeDiffrence >= poolingInterval;
	}

	private void processImpl(AppConfiguration currentConfiguration)
			throws SearchException {
		CacheRefreshUpdateMethod updateMethod = getUpdateMethod(currentConfiguration);

		// Prepare and check connections to LDAP servers
		LdapServerConnection[] sourceServerConnections = prepareLdapServerConnections(currentConfiguration,
				currentConfiguration.getSourceConfigs());

		LdapServerConnection inumDbServerConnection;
		if (currentConfiguration.isDefaultInumServer()) {
			GluuLdapConfiguration ldapInumConfiguration = new GluuLdapConfiguration();
			ldapInumConfiguration.setConfigId("local_inum");
			ldapInumConfiguration.setBaseDNsStringsList(
					Arrays.asList(new String[] { JansConstants.CACHE_REFRESH_DEFAULT_BASE_DN }));

			inumDbServerConnection = prepareLdapServerConnection(currentConfiguration, ldapInumConfiguration,
					true);
		} else {
			inumDbServerConnection = prepareLdapServerConnection(currentConfiguration,
					currentConfiguration.getInumConfig());
		}

		boolean isVdsUpdate = CacheRefreshUpdateMethod.VDS.equals(updateMethod);
		LdapServerConnection targetServerConnection = null;
		if (isVdsUpdate) {
			targetServerConnection = prepareLdapServerConnection(currentConfiguration,
					currentConfiguration.getTargetConfig());
		}

		try {
			if ((sourceServerConnections == null) || (inumDbServerConnection == null)
					|| (isVdsUpdate && (targetServerConnection == null))) {
				log.error("Skipping cache refresh due to invalid server configuration");
			} else {
				detectChangedEntries(currentConfiguration, sourceServerConnections,
						inumDbServerConnection, targetServerConnection, updateMethod);
			}
		} finally {
			// Close connections to LDAP servers
			try {
				closeLdapServerConnection(sourceServerConnections);
			} catch (Exception e) {
				// Nothing can be done
			}

			if (!currentConfiguration.isDefaultInumServer()) {
				try {
					closeLdapServerConnection(inumDbServerConnection);
				} catch (Exception e) {
					// Nothing can be done
				}
			}
			try {
				if (isVdsUpdate) {
					closeLdapServerConnection(targetServerConnection);
				}
			} catch (Exception e) {
				// Nothing can be done
			}
		}

		return;
	}

	@SuppressWarnings("unchecked")
	private boolean detectChangedEntries( AppConfiguration currentConfiguration, LdapServerConnection[] sourceServerConnections,
										 LdapServerConnection inumDbServerConnection, LdapServerConnection targetServerConnection,
										 CacheRefreshUpdateMethod updateMethod) throws SearchException {
		boolean isVDSMode = CacheRefreshUpdateMethod.VDS.equals(updateMethod);

		// Load all entries from Source servers
		System.out.println("Attempting to load entries from source server");
		List<GluuSimplePerson> sourcePersons;

		if (currentConfiguration.isUseSearchLimit()) {
			sourcePersons = loadSourceServerEntries(currentConfiguration, sourceServerConnections);
		} else {
			sourcePersons = loadSourceServerEntriesWithoutLimits(currentConfiguration, sourceServerConnections);
		}

		System.out.println("Found '{}' entries in source server"+ sourcePersons.size());

		Map<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyMap = getSourcePersonCompoundKeyMap(
				currentConfiguration, sourcePersons);
		System.out.println("Found '{}' unique entries in source server"+ sourcePersonCacheCompoundKeyMap.size());

		// Load all inum entries
		List<JansInumMap> inumMaps = null;

		// Load all inum entries from local disk cache
		String inumCachePath = getInumCachePath(currentConfiguration);
		Object loadedObject = objectSerializationService.loadObject(inumCachePath);
		if (loadedObject != null) {
			try {
				inumMaps = (List<JansInumMap>) loadedObject;
				System.out.println("Found '{}' entries in inum objects disk cache"+ inumMaps.size());
			} catch (Exception ex) {
				log.error("Failed to convert to GluuInumMap list"+ ex);
				objectSerializationService.cleanup(inumCachePath);
			}
		}

		if (inumMaps == null) {
			// Load all inum entries from LDAP
			inumMaps = loadInumServerEntries(currentConfiguration, inumDbServerConnection);
			System.out.println("Found '{}' entries in inum server"+ inumMaps.size());
		}

		HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap = getPrimaryKeyAttrValueInumMap(inumMaps);

		// Go through Source entries and create new InumMap entries if needed
		HashMap<CacheCompoundKey, JansInumMap> addedPrimaryKeyAttrValueInumMap = addNewInumServerEntries(
				currentConfiguration, inumDbServerConnection, sourcePersonCacheCompoundKeyMap,
				primaryKeyAttrValueInumMap);

		HashMap<CacheCompoundKey, JansInumMap> allPrimaryKeyAttrValueInumMap = getAllInumServerEntries(
				primaryKeyAttrValueInumMap, addedPrimaryKeyAttrValueInumMap);
		System.out.println("Count actual inum entries '{}' after updating inum server"+ allPrimaryKeyAttrValueInumMap.size());

		HashMap<String, Integer> currInumWithEntryHashCodeMap = getSourcePersonsHashCodesMap(inumDbServerConnection,
				sourcePersonCacheCompoundKeyMap, allPrimaryKeyAttrValueInumMap);
		System.out.println("Count actual source entries '{}' after calculating hash code"+ currInumWithEntryHashCodeMap.size());

		// Create snapshots cache folder if needed
		boolean result = cacheRefreshSnapshotFileService.prepareSnapshotsFolder(currentConfiguration);
		if (!result) {
			return false;
		}

		// Load last snapshot into memory
		Map<String, Integer> prevInumWithEntryHashCodeMap = cacheRefreshSnapshotFileService
				.readLastSnapshot(currentConfiguration);

		// Compare 2 snapshot and invoke update if needed
		Set<String> changedInums = getChangedInums(currInumWithEntryHashCodeMap, prevInumWithEntryHashCodeMap,
				isVDSMode);
		System.out.println("Found '{}' changed entries"+ changedInums.size());

		// Load problem list from disk and add to changedInums
		List<String> problemInums = cacheRefreshSnapshotFileService.readProblemList(currentConfiguration);
		if (problemInums != null) {
			System.out.println("Loaded '{}' problem entries from problem file"+ problemInums.size());
			// Process inums from problem list too
			changedInums.addAll(problemInums);
		}

		List<String> updatedInums = null;
		if (isVDSMode) {
			// Update request to VDS to update entries on target server
			updatedInums = updateTargetEntriesViaVDS(currentConfiguration, targetServerConnection, changedInums);
		} else {
			updatedInums = updateTargetEntriesViaCopy(currentConfiguration, sourcePersonCacheCompoundKeyMap,
					allPrimaryKeyAttrValueInumMap, changedInums);
		}

		System.out.println("Updated '{}' entries"+ updatedInums.size());
		changedInums.removeAll(updatedInums);
		System.out.println("Failed to update '{}' entries"+ changedInums.size());

		// Persist snapshot to cache folder
		result = cacheRefreshSnapshotFileService.createSnapshot(currentConfiguration,
				currInumWithEntryHashCodeMap);
		if (!result) {
			return false;
		}

		// Retain only specified number of snapshots
		cacheRefreshSnapshotFileService.retainSnapshots(currentConfiguration,
				currentConfiguration.getSnapshotMaxCount());

		// Save changedInums as problem list to disk
		currentConfiguration.setVdsCacheRefreshProblemCount(String.valueOf(changedInums.size()));
		cacheRefreshSnapshotFileService.writeProblemList(currentConfiguration, changedInums);

		// Prepare list of persons for removal
		List<GluuSimplePerson> personsForRemoval = null;

		boolean keepExternalPerson = currentConfiguration.isKeepExternalPerson();
		System.out.println("Keep external persons: '{}'"+ keepExternalPerson);
		if (keepExternalPerson) {
			// Determine entries which need to remove
			personsForRemoval = getRemovedPersons(currInumWithEntryHashCodeMap, prevInumWithEntryHashCodeMap);
		} else {
			// Process entries which don't exist in source server

			// Load all entries from Target server
			List<TypedGluuSimplePerson> targetPersons = loadTargetServerEntries(currentConfiguration, getLdapEntryManager());
			System.out.println("Found '{}' entries in target server"+ targetPersons.size());

			// Detect entries which need to remove
			personsForRemoval = processTargetPersons(targetPersons, currInumWithEntryHashCodeMap);
		}
		System.out.println("Count entries '{}' for removal from target server"+ personsForRemoval.size());

		// Remove entries from target server
		HashMap<String, JansInumMap> inumInumMap = getInumInumMap(inumMaps);
		Pair<List<String>, List<String>> removeTargetEntriesResult = removeTargetEntries(inumDbServerConnection,
				getLdapEntryManager(), personsForRemoval, inumInumMap);
		List<String> removedPersonInums = removeTargetEntriesResult.getFirst();
		List<String> removedGluuInumMaps = removeTargetEntriesResult.getSecond();
		System.out.println("Removed '{}' persons from target server"+ removedPersonInums.size());

		// Prepare list of inum for serialization
		ArrayList<JansInumMap> currentInumMaps = applyChangesToInumMap(inumInumMap, addedPrimaryKeyAttrValueInumMap,
				removedGluuInumMaps);

		// Strore all inum entries into local disk cache
		objectSerializationService.saveObject(inumCachePath, currentInumMaps);

		currentConfiguration
				.setVdsCacheRefreshLastUpdateCount(String.valueOf(updatedInums.size() + removedPersonInums.size()));

		return true;
	}

	private ArrayList<JansInumMap> applyChangesToInumMap(HashMap<String, JansInumMap> inumInumMap,
			HashMap<CacheCompoundKey, JansInumMap> addedPrimaryKeyAttrValueInumMap, List<String> removedGluuInumMaps) {
		System.out.println("There are '{}' entries before updating inum list"+ inumInumMap.size());
		for (String removedGluuInumMap : removedGluuInumMaps) {
			inumInumMap.remove(removedGluuInumMap);
		}
		System.out.println("There are '{}' entries after removal '{}' entries" + inumInumMap.size() +" : " +removedGluuInumMaps.size());

		ArrayList<JansInumMap> currentInumMaps = new ArrayList<JansInumMap>(inumInumMap.values());
		currentInumMaps.addAll(addedPrimaryKeyAttrValueInumMap.values());
		System.out.println("There are '{}' entries after adding '{}' entries"+ currentInumMaps.size()+" : " +
				addedPrimaryKeyAttrValueInumMap.size());

		return currentInumMaps;
	}

	private Set<String> getChangedInums(HashMap<String, Integer> currInumWithEntryHashCodeMap,
			Map<String, Integer> prevInumWithEntryHashCodeMap, boolean includeDeleted) {
		// Find chaged inums
		Set<String> changedInums = null;
		// First time run
		if (prevInumWithEntryHashCodeMap == null) {
			changedInums = new HashSet<String>(currInumWithEntryHashCodeMap.keySet());
		} else {
			changedInums = new HashSet<String>();

			// Add all inums which not exist in new snapshot
			if (includeDeleted) {
				for (String prevInumKey : prevInumWithEntryHashCodeMap.keySet()) {
					if (!currInumWithEntryHashCodeMap.containsKey(prevInumKey)) {
						changedInums.add(prevInumKey);
					}
				}
			}

			// Add all new inums and changed inums
			for (Entry<String, Integer> currEntry : currInumWithEntryHashCodeMap.entrySet()) {
				String currInumKey = currEntry.getKey();
				Integer prevHashCode = prevInumWithEntryHashCodeMap.get(currInumKey);
				if ((prevHashCode == null)
						|| ((prevHashCode != null) && !(prevHashCode.equals(currEntry.getValue())))) {
					changedInums.add(currInumKey);
				}
			}
		}
		return changedInums;
	}

	private List<GluuSimplePerson> getRemovedPersons(HashMap<String, Integer> currInumWithEntryHashCodeMap,
			Map<String, Integer> prevInumWithEntryHashCodeMap) {
		// First time run
		if (prevInumWithEntryHashCodeMap == null) {
			return new ArrayList<GluuSimplePerson>(0);
		}

		// Add all inums which not exist in new snapshot
		Set<String> deletedInums = new HashSet<String>();
		for (String prevInumKey : prevInumWithEntryHashCodeMap.keySet()) {
			if (!currInumWithEntryHashCodeMap.containsKey(prevInumKey)) {
				deletedInums.add(prevInumKey);
			}
		}

		List<GluuSimplePerson> deletedPersons = new ArrayList<GluuSimplePerson>(deletedInums.size());
		for (String deletedInum : deletedInums) {
			GluuSimplePerson person = new GluuSimplePerson();
			String personDn = personService.getDnForPerson(deletedInum);
			person.setDn(personDn);

			List<JansCustomAttribute> customAttributes = new ArrayList<JansCustomAttribute>();
			customAttributes.add(new JansCustomAttribute(JansConstants.inum, deletedInum));
			person.setCustomAttributes(customAttributes);

			deletedPersons.add(person);
		}

		return deletedPersons;
	}

	private List<String> updateTargetEntriesViaVDS(CacheRefreshConfiguration cacheRefreshConfiguration,
			LdapServerConnection targetServerConnection, Set<String> changedInums) {
		List<String> result = new ArrayList<String>();

		PersistenceEntryManager targetPersistenceEntryManager = targetServerConnection.getPersistenceEntryManager();
		Filter filter = cacheRefreshService.createObjectClassPresenceFilter();
		for (String changedInum : changedInums) {
			String baseDn = "action=synchronizecache," + personService.getDnForPerson(changedInum);
			try {
				targetPersistenceEntryManager.findEntries(baseDn, DummyEntry.class, filter, SearchScope.SUB, null,
						null, 0, 0, cacheRefreshConfiguration.getLdapSearchSizeLimit());
				result.add(changedInum);
				System.out.println("Updated entry with inum {}"+ changedInum);
			} catch (BasePersistenceException ex) {
				log.error("Failed to update entry with inum '{}' using baseDN {}"+ changedInum, baseDn, ex);
			}
		}

		return result;
	}

	private List<String> updateTargetEntriesViaCopy(CacheRefreshConfiguration cacheRefreshConfiguration,
			Map<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyMap,
			HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap, Set<String> changedInums) {
		HashMap<String, CacheCompoundKey> inumCacheCompoundKeyMap = getInumCacheCompoundKeyMap(
				primaryKeyAttrValueInumMap);
		Map<String, String> targetServerAttributesMapping = getTargetServerAttributesMapping(cacheRefreshConfiguration);
		String[] customObjectClasses = appConfiguration.getPersonObjectClassTypes();

		List<String> result = new ArrayList<String>();

		if (!validateTargetServerSchema(cacheRefreshConfiguration, targetServerAttributesMapping,
				customObjectClasses)) {
			return result;
		}

		for (String targetInum : changedInums) {
			CacheCompoundKey compoundKey = inumCacheCompoundKeyMap.get(targetInum);
			if (compoundKey == null) {
				continue;
			}

			GluuSimplePerson sourcePerson = sourcePersonCacheCompoundKeyMap.get(compoundKey);
			if (sourcePerson == null) {
				continue;
			}

			if (updateTargetEntryViaCopy(sourcePerson, targetInum, customObjectClasses,
					targetServerAttributesMapping)) {
				result.add(targetInum);
			}
		}

		return result;
	}

	private boolean validateTargetServerSchema(CacheRefreshConfiguration cacheRefreshConfiguration,
											   Map<String, String> targetServerAttributesMapping, String[] customObjectClasses) {
		// Get list of return attributes
		String[] keyAttributesWithoutValues = getCompoundKeyAttributesWithoutValues(cacheRefreshConfiguration);
		String[] sourceAttributes = getSourceAttributes(cacheRefreshConfiguration);
		String[] returnAttributes = ArrayHelper.arrayMerge(keyAttributesWithoutValues, sourceAttributes);

		GluuSimplePerson sourcePerson = new GluuSimplePerson();
		for (String returnAttribute : returnAttributes) {
			sourcePerson.setAttribute(returnAttribute, "Test");
		}

		String targetInum = inumService.generateInums(JansConstants.INUM_TYPE_PEOPLE_SLUG, false);
		String targetPersonDn = personService.getDnForPerson(targetInum);

		GluuCustomPerson targetPerson = new GluuCustomPerson();
		targetPerson.setDn(targetPersonDn);
		targetPerson.setInum(targetInum);
		targetPerson.setStatus(appConfiguration.getSupportedUserStatus().get(0));
		targetPerson.setCustomObjectClasses(customObjectClasses);

		// Update list of return attributes according mapping
		cacheRefreshService.setTargetEntryAttributes(sourcePerson, targetServerAttributesMapping, targetPerson);

		// Execute interceptor script
		externalCacheRefreshService.executeExternalUpdateUserMethods(targetPerson);
		boolean executionResult = externalCacheRefreshService.executeExternalUpdateUserMethods(targetPerson);
		if (!executionResult) {
			log.error("Failed to execute Cache Refresh scripts for person '{}'"+ targetInum);
			return false;
		}

		// Validate target server attributes
		List<JansCustomAttribute> customAttributes = targetPerson.getCustomAttributes();

		List<String> targetAttributes = new ArrayList<String>(customAttributes.size());
		for (JansCustomAttribute customAttribute : customAttributes) {
			targetAttributes.add(customAttribute.getName());
		}

		List<String> targetObjectClasses = Arrays
				.asList(getLdapEntryManager().getObjectClasses(targetPerson, GluuCustomPerson.class));

		return validateTargetServerSchema(targetObjectClasses, targetAttributes);
	}

	private boolean validateTargetServerSchema(List<String> targetObjectClasses, List<String> targetAttributes) {
		SchemaEntry schemaEntry = schemaService.getSchema();
		if (schemaEntry == null) {
			// Destination server not requires schema validation
			return true;
		}

		Set<String> objectClassesAttributesSet = schemaService.getObjectClassesAttributes(schemaEntry,
				targetObjectClasses.toArray(new String[0]));

		Set<String> targetAttributesSet = new LinkedHashSet<String>();
		for (String attrbute : targetAttributes) {
			targetAttributesSet.add(StringHelper.toLowerCase(attrbute));
		}

		targetAttributesSet.removeAll(objectClassesAttributesSet);

		if (targetAttributesSet.size() == 0) {
			return true;
		}

		log.error("Skipping target entries update. Destination server schema doesn't has next attributes: '{}', target OC: '{}', target OC attributes: '{}'"+
				targetAttributesSet, targetObjectClasses.toArray(new String[0]), objectClassesAttributesSet);

		return false;
	}

	private boolean updateTargetEntryViaCopy(GluuSimplePerson sourcePerson, String targetInum,
			String[] targetCustomObjectClasses, Map<String, String> targetServerAttributesMapping) {
		String targetPersonDn = personService.getDnForPerson(targetInum);
		GluuCustomPerson targetPerson = null;
		boolean updatePerson;
		if (personService.contains(targetPersonDn)) {
			try {
				targetPerson = personService.findPersonByDn(targetPersonDn);
				System.out.println("Found person by inum '{}'"+ targetInum);
			} catch (EntryPersistenceException ex) {
				log.error("Failed to find person '{}'"+ targetInum, ex);
				return false;
			}
			updatePerson = true;
		} else {
			targetPerson = new GluuCustomPerson();
			targetPerson.setDn(targetPersonDn);
			targetPerson.setInum(targetInum);
			targetPerson.setStatus(appConfiguration.getSupportedUserStatus().get(0));
			updatePerson = false;
		}

		if (PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(getLdapEntryManager().getPersistenceType())) {
			targetPerson.setCustomObjectClasses(targetCustomObjectClasses);
		}

		targetPerson.setSourceServerName(sourcePerson.getSourceServerName());
		targetPerson.setSourceServerUserDn(sourcePerson.getDn());

		cacheRefreshService.setTargetEntryAttributes(sourcePerson, targetServerAttributesMapping, targetPerson);

		// Execute interceptor script
		boolean executionResult = externalCacheRefreshService.executeExternalUpdateUserMethods(targetPerson);
		if (!executionResult) {
			log.error("Failed to execute Cache Refresh scripts for person '{}'"+ targetInum);
			return false;
		}

		try {
			if (updatePerson) {
				personService.updatePersonWithoutCheck(targetPerson);
				System.out.println("Updated person '{}'"+ targetInum);
			} else {
				personService.addPersonWithoutCheck(targetPerson);
				System.out.println("Added new person '{}'"+ targetInum);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			String test = updatePerson ? "update" : "add";
			log.error("Failed to '{}' person '{}'" + test + targetInum + ex);
			return false;
		}

		return true;
	}

	private HashMap<String, CacheCompoundKey> getInumCacheCompoundKeyMap(
			HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap) {
		HashMap<String, CacheCompoundKey> result = new HashMap<String, CacheCompoundKey>();

		for (Entry<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMapEntry : primaryKeyAttrValueInumMap
				.entrySet()) {
			result.put(primaryKeyAttrValueInumMapEntry.getValue().getInum(), primaryKeyAttrValueInumMapEntry.getKey());
		}

		return result;
	}

	private Pair<List<String>, List<String>> removeTargetEntries(LdapServerConnection inumDbServerConnection,
			PersistenceEntryManager targetPersistenceEntryManager, List<GluuSimplePerson> removedPersons,
			HashMap<String, JansInumMap> inumInumMap) {

		Date runDate = new Date(this.lastFinishedTime);

		PersistenceEntryManager inumDbPersistenceEntryManager = inumDbServerConnection.getPersistenceEntryManager();
		List<String> result1 = new ArrayList<String>();
		List<String> result2 = new ArrayList<String>();

		for (GluuSimplePerson removedPerson : removedPersons) {
			String inum = removedPerson.getStringAttribute(JansConstants.inum);

			// Update GluuInumMap if it exist
			JansInumMap currentInumMap = inumInumMap.get(inum);
			if (currentInumMap == null) {
				log.warn("Can't find inum entry of person with DN: {}"+ removedPerson.getDn());
			} else {
				JansInumMap removedInumMap = getMarkInumMapEntryAsRemoved(currentInumMap,
						getLdapEntryManager().encodeTime(removedPerson.getDn(), runDate));
				try {
					inumDbPersistenceEntryManager.merge(removedInumMap);
					result2.add(removedInumMap.getInum());
				} catch (BasePersistenceException ex) {
					log.error("Failed to update entry with inum '{}' and DN: {}"+ currentInumMap.getInum(),
							currentInumMap.getDn(), ex);
					continue;
				}
			}

			// Remove person from target server
			try {				
				//ldap ORM
				if(targetPersistenceEntryManager.hasBranchesSupport(removedPerson.getDn())){					
					targetPersistenceEntryManager.removeRecursively(removedPerson.getDn(), GluuCustomPerson.class);
					
				}else {
					//other ORM
					targetPersistenceEntryManager.remove(removedPerson.getDn(), GluuCustomPerson.class);
					 
					Filter pairwiseIdentifiersFilter = Filter.createEqualityFilter(JansConstants.oxAuthUserId, removedPerson.getDn());
					targetPersistenceEntryManager.remove(pairwiseIdService.getDnForPairWiseIdentifier(null, removedPerson.getDn()), GluuUserPairwiseIdentifier.class, pairwiseIdentifiersFilter,0);
				
					Filter equalityFilter = Filter.createEqualityFilter("personInum", removedPerson.getDn());
					targetPersistenceEntryManager.remove(fidoDeviceService.getDnForFidoDevice(removedPerson.getDn(),null), GluuCustomFidoDevice.class, equalityFilter,0);
					
					Filter equalityFido2DeviceFilter = Filter.createEqualityFilter("personInum", removedPerson.getDn());
					targetPersistenceEntryManager.remove(fido2DeviceService.getDnForFido2Device(null, removedPerson.getDn()), GluuFido2Device.class, equalityFido2DeviceFilter,0);
				}
				result1.add(inum);
			} catch (BasePersistenceException ex) {
				log.error("Failed to remove person entry with inum '{}' and DN: {}"+ inum, removedPerson.getDn(), ex);
				continue;
			}

			System.out.println("Person with DN: '{}' removed from target server"+ removedPerson.getDn());
		}

		return new Pair<List<String>, List<String>>(result1, result2);
	}

	private JansInumMap getMarkInumMapEntryAsRemoved(JansInumMap currentInumMap, String date) {
		JansInumMap clonedInumMap;
		try {
			clonedInumMap = (JansInumMap) BeanUtilsBean2.getInstance().cloneBean(currentInumMap);
		} catch (Exception ex) {
			log.error("Failed to prepare GluuInumMap for removal"+ ex);
			return null;
		}

		String suffix = "-" + date;

		String[] primaryKeyValues = ArrayHelper.arrayClone(clonedInumMap.getPrimaryKeyValues());
		String[] secondaryKeyValues = ArrayHelper.arrayClone(clonedInumMap.getSecondaryKeyValues());
		String[] tertiaryKeyValues = ArrayHelper.arrayClone(clonedInumMap.getTertiaryKeyValues());

		if (ArrayHelper.isNotEmpty(primaryKeyValues)) {
			markInumMapEntryKeyValuesAsRemoved(primaryKeyValues, suffix);
		}

		if (ArrayHelper.isNotEmpty(secondaryKeyValues)) {
			markInumMapEntryKeyValuesAsRemoved(secondaryKeyValues, suffix);
		}

		if (ArrayHelper.isNotEmpty(tertiaryKeyValues)) {
			markInumMapEntryKeyValuesAsRemoved(tertiaryKeyValues, suffix);
		}

		clonedInumMap.setPrimaryKeyValues(primaryKeyValues);
		clonedInumMap.setSecondaryKeyValues(secondaryKeyValues);
		clonedInumMap.setTertiaryKeyValues(tertiaryKeyValues);

		clonedInumMap.setStatus(GluuStatus.INACTIVE);

		return clonedInumMap;
	}

	private void markInumMapEntryKeyValuesAsRemoved(String[] keyValues, String suffix) {
		for (int i = 0; i < keyValues.length; i++) {
			keyValues[i] = keyValues[i] + suffix;
		}
	}

	private List<JansInumMap> loadInumServerEntries(CacheRefreshConfiguration cacheRefreshConfiguration,
			LdapServerConnection inumDbServerConnection) {
		PersistenceEntryManager inumDbPersistenceEntryManager = inumDbServerConnection.getPersistenceEntryManager();
		String inumbaseDn = inumDbServerConnection.getBaseDns()[0];

		Filter filterObjectClass = Filter.createEqualityFilter(OxConstants.OBJECT_CLASS,
				JansConstants.objectClassInumMap);
		Filter filterStatus = Filter.createNOTFilter(
				Filter.createEqualityFilter(JansConstants.jansStatus, GluuStatus.INACTIVE.getValue()));
		Filter filter = Filter.createANDFilter(filterObjectClass, filterStatus);

		return inumDbPersistenceEntryManager.findEntries(inumbaseDn, JansInumMap.class, filter, SearchScope.SUB, null,
				null, 0, 0, cacheRefreshConfiguration.getLdapSearchSizeLimit());
	}

	private List<GluuSimplePerson> loadSourceServerEntriesWithoutLimits(
			CacheRefreshConfiguration cacheRefreshConfiguration, LdapServerConnection[] sourceServerConnections)
			throws SearchException {
		Filter customFilter = cacheRefreshService.createFilter(cacheRefreshConfiguration.getCustomLdapFilter());
		String[] keyAttributes = getCompoundKeyAttributes(cacheRefreshConfiguration);
		String[] keyAttributesWithoutValues = getCompoundKeyAttributesWithoutValues(cacheRefreshConfiguration);
		String[] keyObjectClasses = getCompoundKeyObjectClasses(cacheRefreshConfiguration);
		String[] sourceAttributes = getSourceAttributes(cacheRefreshConfiguration);

		String[] returnAttributes = ArrayHelper.arrayMerge(keyAttributesWithoutValues, sourceAttributes);

		Set<String> addedDns = new HashSet<String>();

		List<GluuSimplePerson> sourcePersons = new ArrayList<GluuSimplePerson>();
		for (LdapServerConnection sourceServerConnection : sourceServerConnections) {
			String sourceServerName = sourceServerConnection.getSourceServerName();

			PersistenceEntryManager sourcePersistenceEntryManager = sourceServerConnection.getPersistenceEntryManager();
			String[] baseDns = sourceServerConnection.getBaseDns();
			Filter filter = cacheRefreshService.createFilter(keyAttributes, keyObjectClasses, "", customFilter);
			if (log.isTraceEnabled()) {
				log.trace("Using next filter to load entris from source server: {}"+ filter);
			}

			for (String baseDn : baseDns) {
				List<GluuSimplePerson> currentSourcePersons = sourcePersistenceEntryManager.findEntries(baseDn,
						GluuSimplePerson.class, filter, SearchScope.SUB, returnAttributes, null, 0, 0,
						cacheRefreshConfiguration.getLdapSearchSizeLimit());

				// Add to result and ignore root entry if needed
				for (GluuSimplePerson currentSourcePerson : currentSourcePersons) {
					currentSourcePerson.setSourceServerName(sourceServerName);
					// if (!StringHelper.equalsIgnoreCase(baseDn,
					// currentSourcePerson.getDn())) {
					String currentSourcePersonDn = currentSourcePerson.getDn().toLowerCase();
					if (!addedDns.contains(currentSourcePersonDn)) {
						sourcePersons.add(currentSourcePerson);
						addedDns.add(currentSourcePersonDn);
					}
					// }
				}
			}
		}

		return sourcePersons;
	}

	private List<GluuSimplePerson> loadSourceServerEntries(CacheRefreshConfiguration cacheRefreshConfiguration,
			LdapServerConnection[] sourceServerConnections) throws SearchException {
		Filter customFilter = cacheRefreshService.createFilter(cacheRefreshConfiguration.getCustomLdapFilter());
		String[] keyAttributes = getCompoundKeyAttributes(cacheRefreshConfiguration);
		String[] keyAttributesWithoutValues = getCompoundKeyAttributesWithoutValues(cacheRefreshConfiguration);
		String[] keyObjectClasses = getCompoundKeyObjectClasses(cacheRefreshConfiguration);
		String[] sourceAttributes = getSourceAttributes(cacheRefreshConfiguration);

		String[] twoLettersArray = createTwoLettersArray();
		String[] returnAttributes = ArrayHelper.arrayMerge(keyAttributesWithoutValues, sourceAttributes);

		Set<String> addedDns = new HashSet<String>();

		List<GluuSimplePerson> sourcePersons = new ArrayList<GluuSimplePerson>();
		for (LdapServerConnection sourceServerConnection : sourceServerConnections) {
			String sourceServerName = sourceServerConnection.getSourceServerName();

			PersistenceEntryManager sourcePersistenceEntryManager = sourceServerConnection.getPersistenceEntryManager();
			String[] baseDns = sourceServerConnection.getBaseDns();
			for (String keyAttributeStart : twoLettersArray) {
				Filter filter = cacheRefreshService.createFilter(keyAttributes, keyObjectClasses, keyAttributeStart,
						customFilter);
				if (log.isDebugEnabled()) {
					log.trace("Using next filter to load entris from source server: {}"+ filter);
				}

				for (String baseDn : baseDns) {
					List<GluuSimplePerson> currentSourcePersons = sourcePersistenceEntryManager.findEntries(baseDn,
							GluuSimplePerson.class, filter, SearchScope.SUB, returnAttributes, null, 0, 0,
							cacheRefreshConfiguration.getLdapSearchSizeLimit());

					// Add to result and ignore root entry if needed
					for (GluuSimplePerson currentSourcePerson : currentSourcePersons) {
						currentSourcePerson.setSourceServerName(sourceServerName);
						// if (!StringHelper.equalsIgnoreCase(baseDn,
						// currentSourcePerson.getDn())) {
						String currentSourcePersonDn = currentSourcePerson.getDn().toLowerCase();
						if (!addedDns.contains(currentSourcePersonDn)) {
							sourcePersons.add(currentSourcePerson);
							addedDns.add(currentSourcePersonDn);
						}
						// }
					}
				}
			}
		}

		return sourcePersons;
	}

	private List<TypedGluuSimplePerson> loadTargetServerEntries(CacheRefreshConfiguration cacheRefreshConfiguration,
			PersistenceEntryManager targetPersistenceEntryManager) {
		Filter filter = Filter.createEqualityFilter(OxConstants.OBJECT_CLASS, JansConstants.objectClassPerson);

		return targetPersistenceEntryManager.findEntries(personService.getDnForPerson(null), TypedGluuSimplePerson.class,
				filter, SearchScope.SUB, TARGET_PERSON_RETURN_ATTRIBUTES, null, 0, 0,
				cacheRefreshConfiguration.getLdapSearchSizeLimit());
	}

	private JansInumMap addGluuInumMap(String inumbBaseDn, PersistenceEntryManager inumDbPersistenceEntryManager,
			String[] primaryKeyAttrName, String[][] primaryKeyValues) {
		String inum = cacheRefreshService.generateInumForNewInumMap(inumbBaseDn, inumDbPersistenceEntryManager);
		String inumDn = cacheRefreshService.getDnForInum(inumbBaseDn, inum);

		JansInumMap inumMap = new JansInumMap();
		inumMap.setDn(inumDn);
		inumMap.setInum(inum);
		inumMap.setPrimaryKeyAttrName(primaryKeyAttrName[0]);
		inumMap.setPrimaryKeyValues(primaryKeyValues[0]);
		if (primaryKeyAttrName.length > 1) {
			inumMap.setSecondaryKeyAttrName(primaryKeyAttrName[1]);
			inumMap.setSecondaryKeyValues(primaryKeyValues[1]);
		}
		if (primaryKeyAttrName.length > 2) {
			inumMap.setTertiaryKeyAttrName(primaryKeyAttrName[2]);
			inumMap.setTertiaryKeyValues(primaryKeyValues[2]);
		}
		inumMap.setStatus(GluuStatus.ACTIVE);
		cacheRefreshService.addInumMap(inumDbPersistenceEntryManager, inumMap);

		return inumMap;
	}

	private HashMap<CacheCompoundKey, JansInumMap> addNewInumServerEntries(
			CacheRefreshConfiguration cacheRefreshConfiguration, LdapServerConnection inumDbServerConnection,
			Map<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyMap,
			HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap) {
		PersistenceEntryManager inumDbPersistenceEntryManager = inumDbServerConnection.getPersistenceEntryManager();
		String inumbaseDn = inumDbServerConnection.getBaseDns()[0];

		HashMap<CacheCompoundKey, JansInumMap> result = new HashMap<CacheCompoundKey, JansInumMap>();

		String[] keyAttributesWithoutValues = getCompoundKeyAttributesWithoutValues(cacheRefreshConfiguration);
		for (Entry<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyEntry : sourcePersonCacheCompoundKeyMap
				.entrySet()) {
			CacheCompoundKey cacheCompoundKey = sourcePersonCacheCompoundKeyEntry.getKey();
			GluuSimplePerson sourcePerson = sourcePersonCacheCompoundKeyEntry.getValue();

			if (log.isTraceEnabled()) {
				log.trace("Checking source entry with key: '{}', and DN: {}"+ cacheCompoundKey, sourcePerson.getDn());
			}

			JansInumMap currentInumMap = primaryKeyAttrValueInumMap.get(cacheCompoundKey);
			if (currentInumMap == null) {
				String[][] keyAttributesValues = getKeyAttributesValues(keyAttributesWithoutValues, sourcePerson);
				currentInumMap = addGluuInumMap(inumbaseDn, inumDbPersistenceEntryManager, keyAttributesWithoutValues,
						keyAttributesValues);
				result.put(cacheCompoundKey, currentInumMap);
				System.out.println("Added new inum entry for DN: {}"+ sourcePerson.getDn());
			} else {
				log.trace("Inum entry for DN: '{}' exist"+ sourcePerson.getDn());
			}
		}

		return result;
	}

	private HashMap<CacheCompoundKey, JansInumMap> getAllInumServerEntries(
			HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap,
			HashMap<CacheCompoundKey, JansInumMap> addedPrimaryKeyAttrValueInumMap) {
		HashMap<CacheCompoundKey, JansInumMap> result = new HashMap<CacheCompoundKey, JansInumMap>();

		result.putAll(primaryKeyAttrValueInumMap);
		result.putAll(addedPrimaryKeyAttrValueInumMap);

		return result;
	}

	private HashMap<String, Integer> getSourcePersonsHashCodesMap(LdapServerConnection inumDbServerConnection,
			Map<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyMap,
			HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap) {
		PersistenceEntryManager inumDbPersistenceEntryManager = inumDbServerConnection.getPersistenceEntryManager();

		HashMap<String, Integer> result = new HashMap<String, Integer>();

		for (Entry<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyEntry : sourcePersonCacheCompoundKeyMap
				.entrySet()) {
			CacheCompoundKey cacheCompoundKey = sourcePersonCacheCompoundKeyEntry.getKey();
			GluuSimplePerson sourcePerson = sourcePersonCacheCompoundKeyEntry.getValue();

			JansInumMap currentInumMap = primaryKeyAttrValueInumMap.get(cacheCompoundKey);

			result.put(currentInumMap.getInum(), inumDbPersistenceEntryManager.getHashCode(sourcePerson));
		}

		return result;
	}

	private List<GluuSimplePerson> processTargetPersons(List<TypedGluuSimplePerson> targetPersons,
			HashMap<String, Integer> currInumWithEntryHashCodeMap) {
		List<GluuSimplePerson> result = new ArrayList<GluuSimplePerson>();

		for (GluuSimplePerson targetPerson : targetPersons) {
			String personInum = targetPerson.getStringAttribute(JansConstants.inum);
			if (!currInumWithEntryHashCodeMap.containsKey(personInum)) {
				System.out.println("Person with such DN: '{}' isn't present on source server"+ targetPerson.getDn());
				result.add(targetPerson);
			}
		}

		return result;
	}

	private HashMap<CacheCompoundKey, JansInumMap> getPrimaryKeyAttrValueInumMap(List<JansInumMap> inumMaps) {
		HashMap<CacheCompoundKey, JansInumMap> result = new HashMap<CacheCompoundKey, JansInumMap>();

		for (JansInumMap inumMap : inumMaps) {
			result.put(new CacheCompoundKey(inumMap.getPrimaryKeyValues(), inumMap.getSecondaryKeyValues(),
					inumMap.getTertiaryKeyValues()), inumMap);
		}

		return result;
	}

	private HashMap<String, JansInumMap> getInumInumMap(List<JansInumMap> inumMaps) {
		HashMap<String, JansInumMap> result = new HashMap<String, JansInumMap>();

		for (JansInumMap inumMap : inumMaps) {
			result.put(inumMap.getInum(), inumMap);
		}

		return result;
	}

	private Map<CacheCompoundKey, GluuSimplePerson> getSourcePersonCompoundKeyMap(
			CacheRefreshConfiguration cacheRefreshConfiguration, List<GluuSimplePerson> sourcePersons) {
		Map<CacheCompoundKey, GluuSimplePerson> result = new HashMap<CacheCompoundKey, GluuSimplePerson>();
		Set<CacheCompoundKey> duplicateKeys = new HashSet<CacheCompoundKey>();

		String[] keyAttributesWithoutValues = getCompoundKeyAttributesWithoutValues(cacheRefreshConfiguration);
		for (GluuSimplePerson sourcePerson : sourcePersons) {
			String[][] keyAttributesValues = getKeyAttributesValues(keyAttributesWithoutValues, sourcePerson);
			CacheCompoundKey cacheCompoundKey = new CacheCompoundKey(keyAttributesValues);

			if (result.containsKey(cacheCompoundKey)) {
				duplicateKeys.add(cacheCompoundKey);
			}

			result.put(cacheCompoundKey, sourcePerson);
		}

		for (CacheCompoundKey duplicateKey : duplicateKeys) {
			log.error("Non-deterministic primary key. Skipping user with key: {}"+ duplicateKey);
			result.remove(duplicateKey);
		}

		return result;
	}

	private LdapServerConnection[] prepareLdapServerConnections(CacheRefreshConfiguration cacheRefreshConfiguration,
			List<GluuLdapConfiguration> ldapConfigurations) {
		LdapServerConnection[] ldapServerConnections = new LdapServerConnection[ldapConfigurations.size()];
		for (int i = 0; i < ldapConfigurations.size(); i++) {
			ldapServerConnections[i] = prepareLdapServerConnection(cacheRefreshConfiguration,
					ldapConfigurations.get(i));
			if (ldapServerConnections[i] == null) {
				return null;
			}
		}

		return ldapServerConnections;
	}

	private LdapServerConnection prepareLdapServerConnection(CacheRefreshConfiguration cacheRefreshConfiguration,
			GluuLdapConfiguration ldapConfiguration) {
		return prepareLdapServerConnection(cacheRefreshConfiguration, ldapConfiguration, false);
	}

	private LdapServerConnection prepareLdapServerConnection(CacheRefreshConfiguration cacheRefreshConfiguration,
			GluuLdapConfiguration ldapConfiguration, boolean useLocalConnection) {
		String ldapConfig = ldapConfiguration.getConfigId();

		if (useLocalConnection) {
			return new LdapServerConnection(ldapConfig, getLdapEntryManager(), getBaseDNs(ldapConfiguration));
		}
		PersistenceEntryManagerFactory entryManagerFactory = applicationFactory
				.getPersistenceEntryManagerFactory(LdapEntryManagerFactory.class);
		String persistenceType = entryManagerFactory.getPersistenceType();

		Properties ldapProperties = toLdapProperties(entryManagerFactory, ldapConfiguration);
		Properties ldapDecryptedProperties = encryptionService.decryptAllProperties(ldapProperties);

		// Try to get updated password via script
		BindCredentials bindCredentials = externalCacheRefreshService
				.executeExternalGetBindCredentialsMethods(ldapConfig);
        String bindPasswordPropertyKey = persistenceType + "#" + PropertiesDecrypter.BIND_PASSWORD;
		if (bindCredentials != null) {
			log.error("Using updated password which got from getBindCredentials method");
			ldapDecryptedProperties.setProperty(persistenceType + ".bindDN", bindCredentials.getBindDn());
			ldapDecryptedProperties.setProperty(bindPasswordPropertyKey,
					bindCredentials.getBindPassword());
		}

		if (log.isTraceEnabled()) {
	        Properties clonedLdapDecryptedProperties = (Properties) ldapDecryptedProperties.clone();
	        if (clonedLdapDecryptedProperties.getProperty(bindPasswordPropertyKey) != null) {
	        	clonedLdapDecryptedProperties.setProperty(bindPasswordPropertyKey, "REDACTED");
	        }
			log.trace("Attempting to create PersistenceEntryManager with properties: {}"+ clonedLdapDecryptedProperties);
		}
		PersistenceEntryManager customPersistenceEntryManager = entryManagerFactory
				.createEntryManager(ldapDecryptedProperties);
		System.out.println("Created Cache Refresh PersistenceEntryManager: {}"+ customPersistenceEntryManager);

		if (!customPersistenceEntryManager.getOperationService().isConnected()) {
			log.error("Failed to connect to LDAP server using configuration {}"+ ldapConfig);
			return null;
		}

		return new LdapServerConnection(ldapConfig, customPersistenceEntryManager, getBaseDNs(ldapConfiguration));
	}

	private void closeLdapServerConnection(LdapServerConnection... ldapServerConnections) {
		for (LdapServerConnection ldapServerConnection : ldapServerConnections) {
			if ((ldapServerConnection != null) && (ldapServerConnection.getPersistenceEntryManager() != null)) {
				ldapServerConnection.getPersistenceEntryManager().destroy();
			}
		}
	}

	private String[] createTwoLettersArray() {
		char[] characters = LETTERS_FOR_SEARCH.toCharArray();
		int lettersCount = characters.length;

		String[] result = new String[lettersCount * lettersCount];
		for (int i = 0; i < lettersCount; i++) {
			for (int j = 0; j < lettersCount; j++) {
				result[i * lettersCount + j] = "" + characters[i] + characters[j];
			}
		}

		return result;
	}

	private String[][] getKeyAttributesValues(String[] attrs, GluuSimplePerson person) {
		String[][] result = new String[attrs.length][];
		for (int i = 0; i < attrs.length; i++) {
			result[i] = person.getStringAttributes(attrs[i]);
		}

		return result;
	}

	private void updateStatus(AppConfiguration currentConfiguration, long lastRun) {
		Date currentDateTime = new Date();
		currentConfiguration.setVdsCacheRefreshLastUpdate(currentDateTime);
		currentConfiguration.setVdsCacheRefreshLastUpdateCount(currentConfiguration.getVdsCacheRefreshLastUpdateCount());
		currentConfiguration.setVdsCacheRefreshProblemCount(currentConfiguration.getVdsCacheRefreshProblemCount());
		CacheRefrshConfigurationService.updateConfiguration(currentConfiguration);
	}

	private String getInumCachePath(CacheRefreshConfiguration cacheRefreshConfiguration) {
		return FilenameUtils.concat(cacheRefreshConfiguration.getSnapshotFolder(), "inum_cache.dat");
	}

	public PersistenceEntryManager getLdapEntryManager() {
		return ldapEntryManager;
	}

	public void setLdapEntryManager(PersistenceEntryManager ldapEntryManager) {
		this.ldapEntryManager = ldapEntryManager;
	}

	public ConfigurationFactory getConfigurationFactory() {
		return configurationFactory;
	}

	public void setConfigurationFactory(ConfigurationFactory configurationFactory) {
		this.configurationFactory = configurationFactory;
	}

	private class LdapServerConnection {
		private String sourceServerName;
		private PersistenceEntryManager ldapEntryManager;
		private String[] baseDns;

		protected LdapServerConnection(String sourceServerName, PersistenceEntryManager ldapEntryManager,
				String[] baseDns) {
			this.sourceServerName = sourceServerName;
			this.ldapEntryManager = ldapEntryManager;
			this.baseDns = baseDns;
		}

		public final String getSourceServerName() {
			return sourceServerName;
		}

		public final PersistenceEntryManager getPersistenceEntryManager() {
			return ldapEntryManager;
		}

		public final String[] getBaseDns() {
			return baseDns;
		}
	}

	private CacheRefreshUpdateMethod getUpdateMethod(CacheRefreshConfiguration cacheRefreshConfiguration) {
		String updateMethod = cacheRefreshConfiguration.getUpdateMethod();
		if (StringHelper.isEmpty(updateMethod)) {
			return CacheRefreshUpdateMethod.COPY;
		}

		return CacheRefreshUpdateMethod.getByValue(cacheRefreshConfiguration.getUpdateMethod());
	}

	private String[] getSourceAttributes(CacheRefreshConfiguration cacheRefreshConfiguration) {
		return cacheRefreshConfiguration.getSourceAttributes().toArray(new String[0]);
	}

	private String[] getCompoundKeyObjectClasses(CacheRefreshConfiguration cacheRefreshConfiguration) {
		return cacheRefreshConfiguration.getKeyObjectClasses().toArray(new String[0]);
	}

	private String[] getCompoundKeyAttributes(CacheRefreshConfiguration cacheRefreshConfiguration) {
		return cacheRefreshConfiguration.getKeyAttributes().toArray(new String[0]);
	}

	private String[] getCompoundKeyAttributesWithoutValues(CacheRefreshConfiguration cacheRefreshConfiguration) {
		String[] result = cacheRefreshConfiguration.getKeyAttributes().toArray(new String[0]);
		for (int i = 0; i < result.length; i++) {
			int index = result[i].indexOf('=');
			if (index != -1) {
				result[i] = result[i].substring(0, index);
			}
		}

		return result;
	}

	private Map<String, String> getTargetServerAttributesMapping(CacheRefreshConfiguration cacheRefreshConfiguration) {
		Map<String, String> result = new HashMap<String, String>();
		for (CacheRefreshAttributeMapping attributeMapping : cacheRefreshConfiguration.getAttributeMapping()) {
			result.put(attributeMapping.getDestination(), attributeMapping.getSource());
		}

		return result;
	}

	private Properties toLdapProperties(PersistenceEntryManagerFactory ldapEntryManagerFactory,
			GluuLdapConfiguration ldapConfiguration) {
		String persistenceType = ldapEntryManagerFactory.getPersistenceType();
		Properties ldapProperties = new Properties();
		ldapProperties.put(persistenceType + "#servers",
				PropertyUtil.simplePropertiesToCommaSeparatedList(ldapConfiguration.getServers()));
		ldapProperties.put(persistenceType + "#maxconnections",
				Integer.toString(ldapConfiguration.getMaxConnections()));
		ldapProperties.put(persistenceType + "#useSSL", Boolean.toString(ldapConfiguration.isUseSSL()));
		ldapProperties.put(persistenceType + "#bindDN", ldapConfiguration.getBindDN());
		ldapProperties.put(persistenceType + "#bindPassword", ldapConfiguration.getBindPassword());

		// Copy binary attributes list from main LDAP connection
		PersistenceOperationService persistenceOperationService = getLdapEntryManager().getOperationService();
		if (persistenceOperationService instanceof LdapOperationService) {
			ldapProperties.put(persistenceType + "#binaryAttributes",
					PropertyUtil.stringsToCommaSeparatedList(((LdapOperationService) persistenceOperationService)
							.getConnectionProvider().getBinaryAttributes()));
		}

		return ldapProperties;
	}

	private String[] getBaseDNs(GluuLdapConfiguration ldapConfiguration) {
		return ldapConfiguration.getBaseDNsStringsList().toArray(new String[0]);
	}

	@ObjectClass(value = "gluuPerson")
	class TypedGluuSimplePerson extends GluuSimplePerson {
		public TypedGluuSimplePerson() {
			super();
		}
	}

	public AtomicBoolean getIsActive() {
		return isActive;
	}

	public void setIsActive(AtomicBoolean isActive) {
		this.isActive = isActive;
	}

}
