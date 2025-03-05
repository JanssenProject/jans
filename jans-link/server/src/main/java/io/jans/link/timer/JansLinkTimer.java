/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.link.timer;

import io.jans.link.constants.JansConstants;
import io.jans.link.event.CacheRefreshEvent;
import io.jans.link.external.ExternalCacheRefreshService;
import io.jans.link.model.*;
import io.jans.link.model.config.AppConfiguration;
import io.jans.link.model.config.CacheRefreshConfiguration;
import io.jans.link.server.service.CacheRefrshConfigurationService;
import io.jans.link.service.*;
import io.jans.link.service.config.ApplicationFactory;
import io.jans.link.service.config.ConfigurationFactory;
import io.jans.model.GluuStatus;
import io.jans.model.custom.script.model.bind.BindCredentials;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;
import io.jans.orm.model.SearchScope;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;
import io.jans.service.EncryptionService;
import io.jans.service.ObjectSerializationService;
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
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Check periodically if source servers contains updates and trigger target
 * server entry update if needed
 * 
 * @author Yuriy Movchan Date: 05.05.2011
 */
@ApplicationScoped
public class JansLinkTimer extends BaseJansLinkTimer {

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
	private AppConfiguration appConfiguration;

	@Inject
	private ConfigurationFactory configurationFactory;

	@Inject
	private CacheRefreshSnapshotFileService cacheRefreshSnapshotFileService;

	@Inject
	private ExternalCacheRefreshService externalCacheRefreshService;

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
		log.info("Initializing jans link Cache Refresh Timer");
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
			log.info("Another process is active");
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			log.info("Failed to start process exclusively");
			return;
		}

		try {
			processInt();
		} finally {
			log.info("Allowing to run new process exclusively");
			this.isActive.set(false);
		}
	}

	public void processInt() {
		AppConfiguration currentConfiguration = getConfigurationFactory().getAppConfiguration();
		try {
			currentConfiguration.setServerIpAddress("255.255.255.255");
			if (!isStartCacheRefresh(currentConfiguration)) {
				log.info("Starting conditions aren't reached");
				return;
			}

			processImpl(currentConfiguration);
			updateStatus(currentConfiguration, System.currentTimeMillis());

			this.lastFinishedTime = System.currentTimeMillis();
		} catch (Throwable ex) {
			ex.printStackTrace();
			log.info("Exception happened while executing cache refresh synchronization (exception: {})", ex.getMessage()); //Giving Exception Details
		}
	}

	private boolean isStartCacheRefresh(AppConfiguration currentConfiguration) {
		if (!currentConfiguration.isLinkEnabled()) {
			return false;
		}

		long poolingInterval = StringHelper.toInteger(currentConfiguration.getPollingInterval());
		if (poolingInterval < 0) {
			return false;
		}

		if(null == currentConfiguration.getSourceConfigs()){
			log.info("Source Config is null, nothing to load ");
			return false;
		}

		String cacheRefreshServerIpAddress = currentConfiguration.getServerIpAddress();
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
			log.error("Failed to enumerate server IP addresses (exception: {})", ex.getMessage()); //Giving Exception Details
		}

		if (!cacheRefreshServer) {
			cacheRefreshServer = externalCacheRefreshService.executeExternalIsStartProcessMethods();
			cacheRefreshServer = true;
		}

		if (!cacheRefreshServer) {
			log.info("This server isn't master Cache Refresh server");
			return false;
		}

		// Check if cache refresh specific configuration was loaded
		if (currentConfiguration == null) {
			log.info("Failed to start cache refresh. Can't loading configuration from oxTrustCacheRefresh.properties");
			return false;
		}

		long timeDiffrence = System.currentTimeMillis() - this.lastFinishedTime;
		timeDiffrence = timeDiffrence/1000;

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
		log.info("Attempting to load entries from source server");
		List<GluuSimplePerson> sourcePersons;

		if (currentConfiguration.isUseSearchLimit()) {
			sourcePersons = loadSourceServerEntries(currentConfiguration, sourceServerConnections);
		} else {
			sourcePersons = loadSourceServerEntriesWithoutLimits(currentConfiguration, sourceServerConnections);
		}

		log.info("Found {} entries in source server", sourcePersons.size());

		Map<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyMap = getSourcePersonCompoundKeyMap(
				currentConfiguration, sourcePersons);
		log.info("Found {} unique entries in source server", sourcePersonCacheCompoundKeyMap.size());

		// Load all inum entries
		List<JansInumMap> inumMaps = null;

		// Load all inum entries from local disk cache
		String inumCachePath = getInumCachePath(currentConfiguration);
		Object loadedObject = objectSerializationService.loadObject(inumCachePath);
		if (loadedObject != null) {
			try {
				inumMaps = (List<JansInumMap>) loadedObject;
				log.info("Found {} entries in inum objects disk cache", inumMaps.size());
			} catch (Exception ex) {
				log.error("Failed to convert to GluuInumMap list (exception: {})", ex.getMessage()); //Giving Exception Details
				objectSerializationService.cleanup(inumCachePath);
			}
		}

		if (inumMaps == null) {
			// Load all inum entries from LDAP
			inumMaps = loadInumServerEntries(currentConfiguration, inumDbServerConnection);
			log.info("Found {} entries in inum server", inumMaps.size());
		}

		HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap = getPrimaryKeyAttrValueInumMap(inumMaps);

		// Go through Source entries and create new InumMap entries if needed
		HashMap<CacheCompoundKey, JansInumMap> addedPrimaryKeyAttrValueInumMap = addNewInumServerEntries(
				currentConfiguration, inumDbServerConnection, sourcePersonCacheCompoundKeyMap,
				primaryKeyAttrValueInumMap);

		HashMap<CacheCompoundKey, JansInumMap> allPrimaryKeyAttrValueInumMap = getAllInumServerEntries(
				primaryKeyAttrValueInumMap, addedPrimaryKeyAttrValueInumMap);
		log.info("Count actual inum entries {} after updating inum server", allPrimaryKeyAttrValueInumMap.size());

		HashMap<String, Integer> currInumWithEntryHashCodeMap = getSourcePersonsHashCodesMap(inumDbServerConnection,
				sourcePersonCacheCompoundKeyMap, allPrimaryKeyAttrValueInumMap);
		log.info("Count actual source entries {} after calculating hash code", currInumWithEntryHashCodeMap.size());

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
		log.info("Found {} changed entries", changedInums.size());

		// Load problem list from disk and add to changedInums
		List<String> problemInums = cacheRefreshSnapshotFileService.readProblemList(currentConfiguration);
		if (problemInums != null) {
			log.info("Loaded {} problem entries from problem file", problemInums.size());
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

		log.info("Updated {} entries", updatedInums.size());
		changedInums.removeAll(updatedInums);
		log.info("Failed to update {} entries", changedInums.size());

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
		currentConfiguration.setProblemCount(String.valueOf(changedInums.size()));
		cacheRefreshSnapshotFileService.writeProblemList(currentConfiguration, changedInums);

		// Prepare list of persons for removal
		List<GluuSimplePerson> personsForRemoval = null;

		boolean keepExternalPerson = currentConfiguration.isKeepExternalPerson();
		log.info("Keep external persons: {}", keepExternalPerson);
		if (keepExternalPerson) {
			// Determine entries which need to remove
			personsForRemoval = getRemovedPersons(currInumWithEntryHashCodeMap, prevInumWithEntryHashCodeMap);
		} else {
			// Process entries which don't exist in source server

			// Load all entries from Target server
			List<TypedGluuSimplePerson> targetPersons = loadTargetServerEntries(currentConfiguration, getLdapEntryManager());
			log.info("Found {} entries in target server", targetPersons.size());

			// Detect entries which need to remove
			personsForRemoval = processTargetPersons(targetPersons, currInumWithEntryHashCodeMap);
		}
		log.info("Count entries {} for removal from target server", personsForRemoval.size());

		// Remove entries from target server
		HashMap<String, JansInumMap> inumInumMap = getInumInumMap(inumMaps);
		Pair<List<String>, List<String>> removeTargetEntriesResult = removeTargetEntries(inumDbServerConnection,
				getLdapEntryManager(), personsForRemoval, inumInumMap);
		List<String> removedPersonInums = removeTargetEntriesResult.getFirst();
		List<String> removedGluuInumMaps = removeTargetEntriesResult.getSecond();
		log.info("Removed {} persons from target server", removedPersonInums.size());

		// Prepare list of inum for serialization
		ArrayList<JansInumMap> currentInumMaps = applyChangesToInumMap(inumInumMap, addedPrimaryKeyAttrValueInumMap,
				removedGluuInumMaps);

		// Strore all inum entries into local disk cache
		objectSerializationService.saveObject(inumCachePath, currentInumMaps);

		currentConfiguration
				.setLastUpdateCount(String.valueOf(updatedInums.size() + removedPersonInums.size()));

		return true;
	}

	public LdapServerConnection prepareLdapServerConnection(CacheRefreshConfiguration cacheRefreshConfiguration,
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
			log.trace("Attempting to create PersistenceEntryManager with properties: {}", clonedLdapDecryptedProperties);
		}
		PersistenceEntryManager customPersistenceEntryManager = entryManagerFactory
				.createEntryManager(ldapDecryptedProperties);
		log.info("Created Cache Refresh PersistenceEntryManager: {}", customPersistenceEntryManager);

		if (!customPersistenceEntryManager.getOperationService().isConnected()) {
			log.error("Failed to connect to LDAP server using configuration {}", ldapConfig);
			return null;
		}

		return new LdapServerConnection(ldapConfig, customPersistenceEntryManager, getBaseDNs(ldapConfiguration));
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
				log.warn("Can't find inum entry of person with DN: {}", removedPerson.getDn());
			} else {
				JansInumMap removedInumMap = getMarkInumMapEntryAsRemoved(currentInumMap,
						getLdapEntryManager().encodeTime(removedPerson.getDn(), runDate));
				try {
					inumDbPersistenceEntryManager.merge(removedInumMap);
					result2.add(removedInumMap.getInum());
				} catch (BasePersistenceException ex) {
					log.error("Failed to update entry with inum {} and DN: {} (exception: {})", currentInumMap.getInum(),
							currentInumMap.getDn(), ex.getMessage()); //Giving Exception Details
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
				log.error("Failed to remove person entry with inum {} and DN: {} (exception: {})", inum, removedPerson.getDn(), ex.getMessage()); //Giving Exception Details
				continue;
			}

			log.info("Person with DN: {} removed from target server", removedPerson.getDn());
		}

		return new Pair<List<String>, List<String>>(result1, result2);
	}

	private JansInumMap getMarkInumMapEntryAsRemoved(JansInumMap currentInumMap, String date) {
		JansInumMap clonedInumMap;
		try {
			clonedInumMap = (JansInumMap) BeanUtilsBean2.getInstance().cloneBean(currentInumMap);
		} catch (Exception ex) {
			log.error("Failed to prepare GluuInumMap for removal (exception: {})", ex.getMessage()); //Giving Exception Details
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
				log.trace("Checking source entry with key: {}, and DN: {}", cacheCompoundKey, sourcePerson.getDn());
			}

			JansInumMap currentInumMap = primaryKeyAttrValueInumMap.get(cacheCompoundKey);
			if (currentInumMap == null) {
				String[][] keyAttributesValues = getKeyAttributesValues(keyAttributesWithoutValues, sourcePerson);
				currentInumMap = addGluuInumMap(inumbaseDn, inumDbPersistenceEntryManager, keyAttributesWithoutValues,
						keyAttributesValues);
				result.put(cacheCompoundKey, currentInumMap);
				log.info("Added new inum entry for DN: {}", sourcePerson.getDn());
			} else {
				log.trace("Inum entry for DN: {} exist", sourcePerson.getDn());
			}
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
			log.error("Non-deterministic primary key. Skipping user with key: {}", duplicateKey);
			result.remove(duplicateKey);
		}

		return result;
	}

	private LdapServerConnection[] prepareLdapServerConnections(CacheRefreshConfiguration cacheRefreshConfiguration,
			List<GluuLdapConfiguration> ldapConfigurations) {
		if (null == ldapConfigurations) {
			return null;
		}

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

	private String[][] getKeyAttributesValues(String[] attrs, GluuSimplePerson person) {
		String[][] result = new String[attrs.length][];
		for (int i = 0; i < attrs.length; i++) {
			result[i] = person.getStringAttributes(attrs[i]);
		}

		return result;
	}

	private void updateStatus(AppConfiguration currentConfiguration, long lastRun) {
		Date currentDateTime = new Date();
		currentConfiguration.setLastUpdate(currentDateTime);
		currentConfiguration.setLastUpdateCount(currentConfiguration.getLastUpdateCount());
		currentConfiguration.setProblemCount(currentConfiguration.getProblemCount());
		CacheRefrshConfigurationService.updateConfiguration(currentConfiguration);
	}

	public ConfigurationFactory getConfigurationFactory() {
		return configurationFactory;
	}

	public void setConfigurationFactory(ConfigurationFactory configurationFactory) {
		this.configurationFactory = configurationFactory;
	}

	public ArrayList<JansInumMap> applyChangesToInumMap(HashMap<String, JansInumMap> inumInumMap,
                                                         HashMap<CacheCompoundKey, JansInumMap> addedPrimaryKeyAttrValueInumMap, List<String> removedGluuInumMaps) {
        log.info("There are {} entries before updating inum list", inumInumMap.size());
        for (String removedGluuInumMap : removedGluuInumMaps) {
            inumInumMap.remove(removedGluuInumMap);
        }
        log.info("There are {} entries after removal {} entries", inumInumMap.size(), removedGluuInumMaps.size());

        ArrayList<JansInumMap> currentInumMaps = new ArrayList<JansInumMap>(inumInumMap.values());
        currentInumMaps.addAll(addedPrimaryKeyAttrValueInumMap.values());
        log.info("There are {} entries after adding {} entries", currentInumMaps.size(),
                addedPrimaryKeyAttrValueInumMap.size());

        return currentInumMaps;
    }

	public List<String> updateTargetEntriesViaCopy(CacheRefreshConfiguration cacheRefreshConfiguration,
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

	public HashMap<String, CacheCompoundKey> getInumCacheCompoundKeyMap(
				HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap) {
			HashMap<String, CacheCompoundKey> result = new HashMap<String, CacheCompoundKey>();

			for (Map.Entry<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMapEntry : primaryKeyAttrValueInumMap
					.entrySet()) {
				result.put(primaryKeyAttrValueInumMapEntry.getValue().getInum(), primaryKeyAttrValueInumMapEntry.getKey());
			}

			return result;
		}

    public List<JansInumMap> loadInumServerEntries(CacheRefreshConfiguration cacheRefreshConfiguration,
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

	public HashMap<CacheCompoundKey, JansInumMap> getAllInumServerEntries(
				HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap,
				HashMap<CacheCompoundKey, JansInumMap> addedPrimaryKeyAttrValueInumMap) {
			HashMap<CacheCompoundKey, JansInumMap> result = new HashMap<CacheCompoundKey, JansInumMap>();

			result.putAll(primaryKeyAttrValueInumMap);
			result.putAll(addedPrimaryKeyAttrValueInumMap);

			return result;
		}

    public HashMap<String, Integer> getSourcePersonsHashCodesMap(LdapServerConnection inumDbServerConnection,
                                                                  Map<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyMap,
                                                                  HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap) {
        PersistenceEntryManager inumDbPersistenceEntryManager = inumDbServerConnection.getPersistenceEntryManager();

        HashMap<String, Integer> result = new HashMap<String, Integer>();

        for (Map.Entry<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyEntry : sourcePersonCacheCompoundKeyMap
                .entrySet()) {
            CacheCompoundKey cacheCompoundKey = sourcePersonCacheCompoundKeyEntry.getKey();
            GluuSimplePerson sourcePerson = sourcePersonCacheCompoundKeyEntry.getValue();

            JansInumMap currentInumMap = primaryKeyAttrValueInumMap.get(cacheCompoundKey);

            result.put(currentInumMap.getInum(), inumDbPersistenceEntryManager.getHashCode(sourcePerson));
        }

        return result;
    }

	public HashMap<CacheCompoundKey, JansInumMap> getPrimaryKeyAttrValueInumMap(List<JansInumMap> inumMaps) {
			HashMap<CacheCompoundKey, JansInumMap> result = new HashMap<CacheCompoundKey, JansInumMap>();

			for (JansInumMap inumMap : inumMaps) {
				result.put(new CacheCompoundKey(inumMap.getPrimaryKeyValues(), inumMap.getSecondaryKeyValues(),
						inumMap.getTertiaryKeyValues()), inumMap);
			}

			return result;
		}

    public HashMap<String, JansInumMap> getInumInumMap(List<JansInumMap> inumMaps) {
        HashMap<String, JansInumMap> result = new HashMap<String, JansInumMap>();

        for (JansInumMap inumMap : inumMaps) {
            result.put(inumMap.getInum(), inumMap);
        }

        return result;
    }



}