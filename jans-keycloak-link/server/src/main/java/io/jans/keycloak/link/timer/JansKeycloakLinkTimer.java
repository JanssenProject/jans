/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.keycloak.link.timer;

import io.jans.keycloak.link.model.CacheCompoundKey;
import io.jans.keycloak.link.model.JansInumMap;
import io.jans.keycloak.link.model.config.AppConfiguration;
import io.jans.keycloak.link.model.config.LinkConfiguration;
import io.jans.keycloak.link.server.service.LinkConfigurationService;
import io.jans.keycloak.link.server.service.KeycloakService;
import io.jans.keycloak.link.service.LinkInterceptionService;
import io.jans.keycloak.link.service.PersonService;
import io.jans.keycloak.link.service.config.ApplicationFactory;
import io.jans.keycloak.link.service.config.ConfigurationFactory;
import io.jans.link.constants.JansConstants;
import io.jans.link.event.LinkEvent;
import io.jans.link.external.ExternalLinkService;
import io.jans.link.model.*;
import io.jans.link.service.*;
import io.jans.model.GluuStatus;
import io.jans.model.JansCustomAttribute;
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
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;

import java.lang.reflect.Field;
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
public class JansKeycloakLinkTimer extends BaseJansLinkTimer {

	private static final int DEFAULT_INTERVAL = 60;

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	protected ApplicationFactory applicationFactory;

	@Inject
	private ConfigurationFactory configurationFactory;

	@Inject
	private LinkInterceptionService linkInterceptionService;

	@Inject
	private PersonService personService;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	//@Inject
	//private ConfigurationService configurationService;

	@Inject
	private LinkSnapshotFileService linkSnapshotFileService;

	@Inject
	private ExternalLinkService externalLinkService;

	@Inject
	private SchemaService schemaService;

	@Inject
	private InumService inumService;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private LinkConfigurationService linkConfigurationService;

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

	@Inject
	private KeycloakService keycloakService;

	private AtomicBoolean isActive;
	private long lastFinishedTime;

	public void initTimer() {
		log.info("Initializing Jans Keycloak Link Timer");
		this.isActive = new AtomicBoolean(false);

		// Clean up previous Inum cache
		LinkConfiguration linkConfiguration = getConfigurationFactory().getAppConfiguration();
		if (linkConfiguration != null) {
			String snapshotFolder = linkConfiguration.getSnapshotFolder();
			if (StringHelper.isNotEmpty(snapshotFolder)) {
				String inumCachePath = getInumCachePath(linkConfiguration);
				objectSerializationService.cleanup(inumCachePath);
			}
		}

		// Schedule to start link every 1 minute
		timerEvent.fire(new TimerEvent(new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL), new LinkEvent(),
				Scheduled.Literal.INSTANCE));

		this.lastFinishedTime = System.currentTimeMillis();
	}

	@Asynchronous
	public void process(@Observes @Scheduled LinkEvent linkEvent) {
		if (this.isActive.get()) {
			log.info("Another process is active");
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			log.info("Failed to start process exclusively");
			return;
		}

		try {
		} finally {
			processInt();
			log.info("Allowing to run new process exclusively");
			this.isActive.set(false);
		}
	}

	public void processInt() {
		log.info("test logger");
		AppConfiguration currentConfiguration = getConfigurationFactory().getAppConfiguration();
		try {
			//GluuConfiguration currentConfiguration = getConfigurationService().getConfiguration();
			//GluuConfiguration currentConfiguration = new GluuConfiguration();
			currentConfiguration.setKeycloakLinkEnabled(true);
			//currentConfiguration.setVdsLinkPollingInterval();
			currentConfiguration.setKeycloakLinkServerIpAddress("255.255.255.255");
			if (!isStartLink(currentConfiguration)) {
				log.info("Starting conditions aren't reached");
				return;
			}

			processImpl(currentConfiguration);
			updateStatus(currentConfiguration, System.currentTimeMillis());

			this.lastFinishedTime = System.currentTimeMillis();
		} catch (Throwable ex) {
			ex.printStackTrace();
			log.error("Exception happened while executing link synchronization"+ ex);
		}
	}

	private boolean isStartLink(AppConfiguration currentConfiguration) {
		if (!currentConfiguration.isKeycloakLinkEnabled()) {
			return false;
		}

		try {
			// check connections to keycloak servers
			log.info("keycloak server connection check ::");

			String keycloak = keycloakService.getKeycloakInstance().serverInfo().getInfo().toString();
			log.info("keycloak server connection check ::" + keycloak);
		}catch(Exception e){
			log.error("not able to connect keycloack server : " + e.getMessage());
			e.printStackTrace();
			return false;
		}

		long poolingInterval = StringHelper.toInteger(currentConfiguration.getKeycloakLinkPollingInterval()) * 60 * 1000;
		if (poolingInterval < 0) {
			return false;
		}

		String linkServerIpAddress = currentConfiguration.getKeycloakLinkServerIpAddress();
		// if (StringHelper.isEmpty(linkServerIpAddress)) {
		// log.debug("There is no master Link Interception server");
		// return false;
		// }

		// Compare server IP address with linkServerIp
		boolean linkServer = false;
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface networkInterface : Collections.list(nets)) {
				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				for (InetAddress inetAddress : Collections.list(inetAddresses)) {
					if (StringHelper.equals(linkServerIpAddress, inetAddress.getHostAddress())) {
						linkServer = true;
						break;
					}
				}

				if (linkServer) {
					break;
				}
			}
		} catch (SocketException ex) {
			log.error("Failed to enumerate server IP addresses"+ ex);
		}

		if (!linkServer) {
			linkServer = externalLinkService.executeExternalIsStartProcessMethods();
			linkServer = true;
		}

		if (!linkServer) {
			log.info("This server isn't master Link Interception server");
			return false;
		}

		// Check if link specific configuration was loaded
		if (currentConfiguration == null) {
			log.info("Failed to start link. Can't loading configuration from jans-link.properties");
			return false;
		}

		long timeDiffrence = System.currentTimeMillis() - this.lastFinishedTime;

		return timeDiffrence >= poolingInterval;
	}

	private void processImpl(AppConfiguration currentConfiguration)
			throws SearchException {
		LinkUpdateMethod updateMethod = getUpdateMethod(currentConfiguration);

		LdapServerConnection inumDbServerConnection;
		if (currentConfiguration.isDefaultInumServer()) {
			GluuLdapConfiguration ldapInumConfiguration = new GluuLdapConfiguration();
			ldapInumConfiguration.setConfigId("local_inum");
			ldapInumConfiguration.setBaseDNsStringsList(
					Arrays.asList(new String[] { JansConstants.JANS_KEYCLOAK_LINK_DEFAULT_BASE_DN }));

			inumDbServerConnection = prepareLdapServerConnection(currentConfiguration, ldapInumConfiguration,
					true);
		} else {
			inumDbServerConnection = prepareLdapServerConnection(currentConfiguration,
					currentConfiguration.getInumConfig());
		}

		boolean isVdsUpdate = LinkUpdateMethod.VDS.equals(updateMethod);
		LdapServerConnection targetServerConnection = null;
		if (isVdsUpdate) {
			targetServerConnection = prepareLdapServerConnection(currentConfiguration,
					currentConfiguration.getTargetConfig());
		}

		try {
			if ((currentConfiguration.getKeycloakConfiguration() == null) || (inumDbServerConnection == null)) {
				log.error("Skipping link due to invalid server configuration");
			} else {
				detectChangedEntries(currentConfiguration, null,
						inumDbServerConnection, targetServerConnection, updateMethod);
			}
		} finally {
			// Close connections to LDAP servers
			if (!currentConfiguration.isDefaultInumServer()) {
				try {
					closeLdapServerConnection(inumDbServerConnection);
				} catch (Exception e) {
					log.error("Failed to close ldap connection "+ e);
					// Nothing can be done
				}
			}
			try {
				if (isVdsUpdate) {
					closeLdapServerConnection(targetServerConnection);
				}
			} catch (Exception e) {
				log.error("Failed to close ldap connection "+ e);
				// Nothing can be done
			}
		}

		return;
	}

	@SuppressWarnings("unchecked")
	private boolean detectChangedEntries( AppConfiguration currentConfiguration, LdapServerConnection[] sourceServerConnections,
										  LdapServerConnection inumDbServerConnection, LdapServerConnection targetServerConnection,
										  LinkUpdateMethod updateMethod) throws SearchException {
		boolean isVDSMode = LinkUpdateMethod.VDS.equals(updateMethod);

		// Load all entries from Source servers
		log.info("Attempting to load entries from source server");
		List<UserRepresentation> sourceUserRepresentation = keycloakService.getUserList();
		log.info("Found '{}' entries in source source User Representation from keycloak : "+ sourceUserRepresentation.size());

		List<GluuSimplePerson> sourcePersons = loadSourceServerKeycloakEntriesWithoutLimits(currentConfiguration, sourceServerConnections,sourceUserRepresentation);

		log.info("Found '{}' entries in source server sourcePersons  : "+ sourcePersons.size());

		Map<CacheCompoundKey, GluuSimplePerson> sourceUserRepresentPersonCacheCompoundKeyMap = getSourcePersonCompoundKeyMap(
				currentConfiguration, sourcePersons);
		log.info("Found '{}' unique entries in source server"+ sourceUserRepresentPersonCacheCompoundKeyMap.size());

		// Load all inum entries
		List<JansInumMap> inumMaps = null;

		// Load all inum entries from local disk cache
		String inumCachePath = getInumCachePath(currentConfiguration);
		Object loadedObject = objectSerializationService.loadObject(inumCachePath);
		if (loadedObject != null) {
			try {
				inumMaps = (List<JansInumMap>) loadedObject;
				log.info("Found '{}' entries in inum objects disk cache"+ inumMaps.size());
			} catch (Exception ex) {
				log.error("Failed to convert to GluuInumMap list"+ ex);
				objectSerializationService.cleanup(inumCachePath);
			}
		}

		if (inumMaps == null) {
			// Load all inum entries from LDAP
			inumMaps = loadInumServerEntries(currentConfiguration, inumDbServerConnection);
			log.info("Found '{}' entries in inum server"+ inumMaps.size());
		}

		HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap = getPrimaryKeyAttrValueInumMap(inumMaps);

		// Go through Source entries and create new InumMap entries if needed
		HashMap<CacheCompoundKey, JansInumMap> addedPrimaryKeyAttrValueInumMap = addNewInumServerEntries(
				currentConfiguration, inumDbServerConnection, sourceUserRepresentPersonCacheCompoundKeyMap,
				primaryKeyAttrValueInumMap);

		HashMap<CacheCompoundKey, JansInumMap> allPrimaryKeyAttrValueInumMap = getAllInumServerEntries(
				primaryKeyAttrValueInumMap, addedPrimaryKeyAttrValueInumMap);
		log.info("Count actual inum entries '{}' after updating inum server"+ allPrimaryKeyAttrValueInumMap.size());

		HashMap<String, Integer> currInumWithEntryHashCodeMap = getSourcePersonsHashCodesMap(inumDbServerConnection,
				sourceUserRepresentPersonCacheCompoundKeyMap, allPrimaryKeyAttrValueInumMap);
		log.info("Count actual source entries '{}' after calculating hash code"+ currInumWithEntryHashCodeMap.size());

		// Create snapshots cache folder if needed
		boolean result = linkSnapshotFileService.prepareSnapshotsFolder(currentConfiguration);
		if (!result) {
			return false;
		}

		// Load last snapshot into memory
		Map<String, Integer> prevInumWithEntryHashCodeMap = linkSnapshotFileService
				.readLastSnapshot(currentConfiguration);

		// Compare 2 snapshot and invoke update if needed
		Set<String> changedInums = getChangedInums(currInumWithEntryHashCodeMap, prevInumWithEntryHashCodeMap,
				isVDSMode);
		log.info("Found '{}' changed entries"+ changedInums.size());

		// Load problem list from disk and add to changedInums
		List<String> problemInums = linkSnapshotFileService.readProblemList(currentConfiguration);
		if (problemInums != null) {
			log.info("Loaded '{}' problem entries from problem file"+ problemInums.size());
			// Process inums from problem list too
			changedInums.addAll(problemInums);
		}

		List<String> updatedInums = null;
		if (isVDSMode) {
			// Update request to VDS to update entries on target server
			updatedInums = updateTargetEntriesViaVDS(currentConfiguration, targetServerConnection, changedInums);
		} else {
			updatedInums = updateTargetEntriesViaCopy(currentConfiguration, sourceUserRepresentPersonCacheCompoundKeyMap,
					allPrimaryKeyAttrValueInumMap, changedInums);
		}

		log.info("Updated '{}' entries"+ updatedInums.size());
		changedInums.removeAll(updatedInums);
		log.info("Failed to update '{}' entries"+ changedInums.size());

		// Persist snapshot to cache folder
		result = linkSnapshotFileService.createSnapshot(currentConfiguration,
				currInumWithEntryHashCodeMap);
		if (!result) {
			return false;
		}

		// Retain only specified number of snapshots
		linkSnapshotFileService.retainSnapshots(currentConfiguration,
				currentConfiguration.getSnapshotMaxCount());

		// Save changedInums as problem list to disk
		currentConfiguration.setKeycloakLinkProblemCount(String.valueOf(changedInums.size()));
		linkSnapshotFileService.writeProblemList(currentConfiguration, changedInums);

		// Prepare list of persons for removal
		List<GluuSimplePerson> personsForRemoval = null;

		boolean keepExternalPerson = currentConfiguration.isKeepExternalPerson();
		log.info("Keep external persons: '{}'"+ keepExternalPerson);
		if (keepExternalPerson) {
			// Determine entries which need to remove
			personsForRemoval = getRemovedPersons(currInumWithEntryHashCodeMap, prevInumWithEntryHashCodeMap);
		} else {
			// Process entries which don't exist in source server

			// Load all entries from Target server
			List<TypedGluuSimplePerson> targetPersons = loadTargetServerEntries(currentConfiguration, getLdapEntryManager());
			log.info("Found '{}' entries in target server"+ targetPersons.size());

			// Detect entries which need to remove
			personsForRemoval = processTargetPersons(targetPersons, currInumWithEntryHashCodeMap);
		}
		log.info("Count entries '{}' for removal from target server"+ personsForRemoval.size());

		// Remove entries from target server
		HashMap<String, JansInumMap> inumInumMap = getInumInumMap(inumMaps);
		Pair<List<String>, List<String>> removeTargetEntriesResult = removeTargetEntries(inumDbServerConnection,
				getLdapEntryManager(), personsForRemoval, inumInumMap);
		List<String> removedPersonInums = removeTargetEntriesResult.getFirst();
		List<String> removedGluuInumMaps = removeTargetEntriesResult.getSecond();
		log.info("Removed '{}' persons from target server"+ removedPersonInums.size());

		// Prepare list of inum for serialization
		ArrayList<JansInumMap> currentInumMaps = applyChangesToInumMap(inumInumMap, addedPrimaryKeyAttrValueInumMap,
				removedGluuInumMaps);

		// Strore all inum entries into local disk cache
		objectSerializationService.saveObject(inumCachePath, currentInumMaps);

		currentConfiguration
				.setKeycloakLinkLastUpdateCount(String.valueOf(updatedInums.size() + removedPersonInums.size()));

		return true;
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

			log.info("Person with DN: '{}' removed from target server"+ removedPerson.getDn());
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

		String primaryKeyValues = clonedInumMap.getPrimaryKeyValues();
		String secondaryKeyValues = clonedInumMap.getSecondaryKeyValues();
		String tertiaryKeyValues = clonedInumMap.getTertiaryKeyValues();

		if (StringHelper.isNotEmpty(primaryKeyValues)) {
			markInumMapEntryKeyValuesAsRemoved(primaryKeyValues, suffix);
		}

		if (StringHelper.isNotEmpty(secondaryKeyValues)) {
			markInumMapEntryKeyValuesAsRemoved(secondaryKeyValues, suffix);
		}

		if (StringHelper.isNotEmpty(tertiaryKeyValues)) {
			markInumMapEntryKeyValuesAsRemoved(tertiaryKeyValues, suffix);
		}

		clonedInumMap.setPrimaryKeyValues(primaryKeyValues);
		clonedInumMap.setSecondaryKeyValues(secondaryKeyValues);
		clonedInumMap.setTertiaryKeyValues(tertiaryKeyValues);

		clonedInumMap.setStatus(GluuStatus.INACTIVE);

		return clonedInumMap;
	}

	private void markInumMapEntryKeyValuesAsRemoved(String keyValues, String suffix) {
		//for (int i = 0; i < keyValues.length; i++) {
		keyValues = keyValues + suffix;

	}

	private HashMap<CacheCompoundKey, JansInumMap> addNewInumServerEntries(
			LinkConfiguration linkConfiguration, LdapServerConnection inumDbServerConnection,
			Map<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyMap,
			HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap) {
		PersistenceEntryManager inumDbPersistenceEntryManager = inumDbServerConnection.getPersistenceEntryManager();
		String inumbaseDn = inumDbServerConnection.getBaseDns()[0];

		HashMap<CacheCompoundKey, JansInumMap> result = new HashMap<CacheCompoundKey, JansInumMap>();

		String[] keyAttributesWithoutValues = getCompoundKeyAttributesWithoutValues(linkConfiguration);
		for (Entry<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyEntry : sourcePersonCacheCompoundKeyMap
				.entrySet()) {
			CacheCompoundKey cacheCompoundKey = sourcePersonCacheCompoundKeyEntry.getKey();
			GluuSimplePerson sourcePerson = sourcePersonCacheCompoundKeyEntry.getValue();

			if (log.isTraceEnabled()) {
				log.trace("Checking source entry with key: '{}', and DN: {}"+ cacheCompoundKey, sourcePerson.getDn());
			}

			JansInumMap currentInumMap = primaryKeyAttrValueInumMap.get(cacheCompoundKey);
			if (currentInumMap == null) {
				String[] keyAttributesValues = getKeyAttributesValues(keyAttributesWithoutValues, sourcePerson);
				currentInumMap = addGluuInumMap(inumbaseDn, inumDbPersistenceEntryManager, keyAttributesWithoutValues,
						keyAttributesValues);
				result.put(cacheCompoundKey, currentInumMap);
				log.info("Added new inum entry for DN: {}"+ sourcePerson.getDn());
			} else {
				log.trace("Inum entry for DN: '{}' exist"+ sourcePerson.getDn());
			}
		}

		return result;
	}

	private Map<CacheCompoundKey, GluuSimplePerson> getSourcePersonCompoundKeyMap(
			LinkConfiguration linkConfiguration, List<GluuSimplePerson> sourcePersons) {
		Map<CacheCompoundKey, GluuSimplePerson> result = new HashMap<CacheCompoundKey, GluuSimplePerson>();
		Set<CacheCompoundKey> duplicateKeys = new HashSet<CacheCompoundKey>();

		String[] keyAttributesWithoutValues = getCompoundKeyAttributesWithoutValues(linkConfiguration);
		for (GluuSimplePerson sourcePerson : sourcePersons) {
			String[] keyAttributesValues = getKeyAttributesValues(keyAttributesWithoutValues, sourcePerson);
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


	private LdapServerConnection[] prepareLdapServerConnections(LinkConfiguration linkConfiguration,
																List<GluuLdapConfiguration> ldapConfigurations) {
		LdapServerConnection[] ldapServerConnections = new LdapServerConnection[ldapConfigurations.size()];
		for (int i = 0; i < ldapConfigurations.size(); i++) {
			ldapServerConnections[i] = prepareLdapServerConnection(linkConfiguration,
					ldapConfigurations.get(i));
			if (ldapServerConnections[i] == null) {
				return null;
			}
		}

		return ldapServerConnections;
	}

	private LdapServerConnection prepareLdapServerConnection(LinkConfiguration linkConfiguration,
															 GluuLdapConfiguration ldapConfiguration) {
		return prepareLdapServerConnection(linkConfiguration, ldapConfiguration, false);
	}

	private LdapServerConnection prepareLdapServerConnection(LinkConfiguration linkConfiguration,
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
		BindCredentials bindCredentials = externalLinkService
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
		log.info("Created Link Interception PersistenceEntryManager: {}"+ customPersistenceEntryManager);

		if (!customPersistenceEntryManager.getOperationService().isConnected()) {
			log.error("Failed to connect to LDAP server using configuration {}"+ ldapConfig);
			return null;
		}

		return new LdapServerConnection(ldapConfig, customPersistenceEntryManager, getBaseDNs(ldapConfiguration));
	}

	private String[] getKeyAttributesValues(String[] attrs, GluuSimplePerson person) {
		String[] result = new String[attrs.length];
		try {
			for (int i = 0; i < attrs.length; i++) {
				//result[i] = (String) PropertyUtils.getProperty(person, attrs[i]);
				result[i] = person.getStringAttributes(attrs[i])[0];
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Failed to close ldap connection "+ e.getMessage());
		}

		return result;
	}

	private void updateStatus(AppConfiguration currentConfiguration, long lastRun) {
		Date currentDateTime = new Date();
		currentConfiguration.setKeycloakLinkLastUpdate(currentDateTime);
		currentConfiguration.setKeycloakLinkLastUpdateCount(currentConfiguration.getKeycloakLinkLastUpdateCount());
		currentConfiguration.setKeycloakLinkProblemCount(currentConfiguration.getKeycloakLinkProblemCount());
		linkConfigurationService.updateConfiguration(currentConfiguration);
	}

	public ConfigurationFactory getConfigurationFactory() {
		return configurationFactory;
	}

	public void setConfigurationFactory(ConfigurationFactory configurationFactory) {
		this.configurationFactory = configurationFactory;
	}

	private List<GluuSimplePerson> loadSourceServerKeycloakEntriesWithoutLimits(
			LinkConfiguration linkConfiguration, LdapServerConnection[] sourceServerConnections,
			List<UserRepresentation> sourceUserRepresentations){
		List<GluuSimplePerson> sourceJansSimplePerson = new ArrayList<GluuSimplePerson>();
		try {
			Filter customFilter = linkInterceptionService.createFilter(linkConfiguration.getCustomLdapFilter());
			String[] keyAttributes = getCompoundKeyAttributes(linkConfiguration);
			String[] keyAttributesWithoutValues = getCompoundKeyAttributesWithoutValues(linkConfiguration);
			String[] keyObjectClasses = getCompoundKeyObjectClasses(linkConfiguration);
			String[] sourceAttributes = getSourceAttributes(linkConfiguration);

			String[] returnAttributes = ArrayHelper.arrayMerge(keyAttributesWithoutValues, sourceAttributes);

			Set<String> addedDns = new HashSet<String>();

			for (UserRepresentation userRepresentation : sourceUserRepresentations) {
				GluuSimplePerson jansSimplePerson = new GluuSimplePerson();
				for (String attr : keyAttributes) {
					JansCustomAttribute jansCustomAttribute = new JansCustomAttribute();
					jansCustomAttribute.setName(attr);
					Field f = userRepresentation.getClass().getDeclaredField(attr);
					f.setAccessible(true);
					jansCustomAttribute.setValue((String) f.get(userRepresentation));

					jansSimplePerson.getCustomAttributes().add(jansCustomAttribute);
				}
				for (String attr : sourceAttributes) {
					JansCustomAttribute jansCustomAttribute = new JansCustomAttribute();
					jansCustomAttribute.setName(attr);
					Field f = userRepresentation.getClass().getDeclaredField(attr);
					f.setAccessible(true);
					jansCustomAttribute.setValue((String) f.get(userRepresentation));

					jansSimplePerson.getCustomAttributes().add(jansCustomAttribute);
				}
				jansSimplePerson.setDn(personService.getDnForPerson(personService.generateInumForNewPersonImpl()));
				jansSimplePerson.setSourceServerName("Keycloak to jans");

				sourceJansSimplePerson.add(jansSimplePerson);

			}
		}catch(SearchException| NoSuchFieldException| IllegalAccessException e){
			log.error("Exception :  "+ e.getMessage());
			e.printStackTrace();
		}

		return sourceJansSimplePerson;
	}
	private JansInumMap addGluuInumMap(String inumbBaseDn, PersistenceEntryManager inumDbPersistenceEntryManager,
									   String[] primaryKeyAttrName, String[] primaryKeyValues) {
		String inum = linkInterceptionService.generateInumForNewInumMap(inumbBaseDn, inumDbPersistenceEntryManager);
		String inumDn = linkInterceptionService.getDnForInum(inumbBaseDn, inum);

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
		linkInterceptionService.addInumMap(inumDbPersistenceEntryManager, inumMap);

		return inumMap;
	}

	public AtomicBoolean getIsActive() {
		return isActive;
	}

	public void setIsActive(AtomicBoolean isActive) {
		this.isActive = isActive;
	}

	public List<io.jans.keycloak.link.model.JansInumMap> loadInumServerEntries(io.jans.link.model.config.shared.LinkConfiguration linkConfiguration,
																	  LdapServerConnection inumDbServerConnection) {
		PersistenceEntryManager inumDbPersistenceEntryManager = inumDbServerConnection.getPersistenceEntryManager();
		String inumbaseDn = inumDbServerConnection.getBaseDns()[0];

		Filter filterObjectClass = Filter.createEqualityFilter(OxConstants.OBJECT_CLASS,
				JansConstants.objectClassInumMap);
		Filter filterStatus = Filter.createNOTFilter(
				Filter.createEqualityFilter(JansConstants.jansStatus, GluuStatus.INACTIVE.getValue()));
		Filter filter = Filter.createANDFilter(filterObjectClass, filterStatus);

		return inumDbPersistenceEntryManager.findEntries(inumbaseDn, io.jans.keycloak.link.model.JansInumMap.class, filter, SearchScope.SUB, null,
				null, 0, 0, linkConfiguration.getLdapSearchSizeLimit());
	}

	private HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap> getAllInumServerEntries(
			HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap> primaryKeyAttrValueInumMap,
			HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap> addedPrimaryKeyAttrValueInumMap) {
		HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap> result = new HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap>();

		result.putAll(primaryKeyAttrValueInumMap);
		result.putAll(addedPrimaryKeyAttrValueInumMap);

		return result;
	}

	private HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap> getPrimaryKeyAttrValueInumMap(List<io.jans.keycloak.link.model.JansInumMap> inumMaps) {
		HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap> result = new HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap>();

		for (io.jans.keycloak.link.model.JansInumMap inumMap : inumMaps) {
			result.put(new io.jans.keycloak.link.model.CacheCompoundKey(inumMap.getPrimaryKeyValues(), inumMap.getSecondaryKeyValues(),
					inumMap.getTertiaryKeyValues()), inumMap);
		}

		return result;
	}

	public HashMap<String, Integer> getSourcePersonsHashCodesMap(LdapServerConnection inumDbServerConnection,
																 Map<io.jans.keycloak.link.model.CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyMap,
																 HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap> primaryKeyAttrValueInumMap) {
		PersistenceEntryManager inumDbPersistenceEntryManager = inumDbServerConnection.getPersistenceEntryManager();

		HashMap<String, Integer> result = new HashMap<String, Integer>();

		for (Map.Entry<io.jans.keycloak.link.model.CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyEntry : sourcePersonCacheCompoundKeyMap
				.entrySet()) {
			io.jans.keycloak.link.model.CacheCompoundKey cacheCompoundKey = sourcePersonCacheCompoundKeyEntry.getKey();
			GluuSimplePerson sourcePerson = sourcePersonCacheCompoundKeyEntry.getValue();

			io.jans.keycloak.link.model.JansInumMap currentInumMap = primaryKeyAttrValueInumMap.get(cacheCompoundKey);

			result.put(currentInumMap.getInum(), inumDbPersistenceEntryManager.getHashCode(sourcePerson));
		}

		return result;
	}

	public List<String> updateTargetEntriesViaCopy(io.jans.link.model.config.shared.LinkConfiguration linkConfiguration,
												   Map<io.jans.keycloak.link.model.CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyMap,
												   HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap> primaryKeyAttrValueInumMap, Set<String> changedInums) {
		HashMap<String, io.jans.keycloak.link.model.CacheCompoundKey> inumCacheCompoundKeyMap = getInumCacheCompoundKeyMap(
				primaryKeyAttrValueInumMap);
		Map<String, String> targetServerAttributesMapping = getTargetServerAttributesMapping(linkConfiguration);
		String[] customObjectClasses = appConfiguration.getPersonObjectClassTypes();

		List<String> result = new ArrayList<String>();

		if (!validateTargetServerSchema(linkConfiguration, targetServerAttributesMapping,
				customObjectClasses)) {
			return result;
		}

		for (String targetInum : changedInums) {
			io.jans.keycloak.link.model.CacheCompoundKey compoundKey = inumCacheCompoundKeyMap.get(targetInum);
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

	public HashMap<String, io.jans.keycloak.link.model.JansInumMap> getInumInumMap(List<io.jans.keycloak.link.model.JansInumMap> inumMaps) {
		HashMap<String, io.jans.keycloak.link.model.JansInumMap> result = new HashMap<String, io.jans.keycloak.link.model.JansInumMap>();

		for (io.jans.keycloak.link.model.JansInumMap inumMap : inumMaps) {
			result.put(inumMap.getInum(), inumMap);
		}

		return result;
	}

	public ArrayList<io.jans.keycloak.link.model.JansInumMap> applyChangesToInumMap(HashMap<String, io.jans.keycloak.link.model.JansInumMap> inumInumMap,
																		   HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap> addedPrimaryKeyAttrValueInumMap, List<String> removedGluuInumMaps) {
		log.info("There are '{}' entries before updating inum list"+ inumInumMap.size());
		for (String removedGluuInumMap : removedGluuInumMaps) {
			inumInumMap.remove(removedGluuInumMap);
		}
		log.info("There are '{}' entries after removal '{}' entries" + inumInumMap.size() +" : " +removedGluuInumMaps.size());

		ArrayList<io.jans.keycloak.link.model.JansInumMap> currentInumMaps = new ArrayList<io.jans.keycloak.link.model.JansInumMap>(inumInumMap.values());
		currentInumMaps.addAll(addedPrimaryKeyAttrValueInumMap.values());
		log.info("There are '{}' entries after adding '{}' entries"+ currentInumMaps.size()+" : " +
				addedPrimaryKeyAttrValueInumMap.size());

		return currentInumMaps;
	}

	public HashMap<String, io.jans.keycloak.link.model.CacheCompoundKey> getInumCacheCompoundKeyMap(
			HashMap<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap> primaryKeyAttrValueInumMap) {
		HashMap<String, io.jans.keycloak.link.model.CacheCompoundKey> result = new HashMap<String, io.jans.keycloak.link.model.CacheCompoundKey>();

		for (Map.Entry<io.jans.keycloak.link.model.CacheCompoundKey, io.jans.keycloak.link.model.JansInumMap> primaryKeyAttrValueInumMapEntry : primaryKeyAttrValueInumMap
				.entrySet()) {
			result.put(primaryKeyAttrValueInumMapEntry.getValue().getInum(), primaryKeyAttrValueInumMapEntry.getKey());
		}

		return result;
	}


}