package io.jans.link.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import io.jans.link.constants.JansConstants;
import io.jans.link.external.ExternalLinkService;
import io.jans.link.model.GluuCustomPerson;
import io.jans.link.model.GluuSimplePerson;
import io.jans.link.model.JansInumMap;
import io.jans.link.model.config.shared.LinkAttributeMapping;
import io.jans.link.model.config.shared.LinkConfiguration;
import io.jans.link.util.PropertyUtil;
import io.jans.model.GluuStatus;
import io.jans.model.JansCustomAttribute;
import io.jans.model.SchemaEntry;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.ldap.operation.LdapOperationService;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.DummyEntry;
import io.jans.orm.operation.PersistenceOperationService;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;
import io.jans.service.EncryptionService;
import io.jans.service.ObjectSerializationService;
import io.jans.service.SchemaService;
import io.jans.util.OxConstants;
import jakarta.inject.Inject;


public abstract class BaseJansLinkTimer {

    private static final String LETTERS_FOR_SEARCH = "abcdefghijklmnopqrstuvwxyz1234567890.";
    private static final String[] TARGET_PERSON_RETURN_ATTRIBUTES = { JansConstants.inum };
    @Inject
    private Logger log;
    @Inject
    private LinkService linkService;
    @Inject
    private PersonService personService;
    @Inject
    private PersistenceEntryManager ldapEntryManager;
    @Inject
    private LinkSnapshotFileService linkSnapshotFileService;
    @Inject
    private ExternalLinkService externalLinkService;
    @Inject
    private SchemaService schemaService;
    @Inject
    private InumService inumService;

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



    /*public ArrayList<JansInumMap> applyChangesToInumMap(HashMap<String, JansInumMap> inumInumMap,
                                                         HashMap<CacheCompoundKey, JansInumMap> addedPrimaryKeyAttrValueInumMap, List<String> removedGluuInumMaps) {
        log.info("There are '{}' entries before updating inum list"+ inumInumMap.size());
        for (String removedGluuInumMap : removedGluuInumMaps) {
            inumInumMap.remove(removedGluuInumMap);
        }
        log.info("There are '{}' entries after removal '{}' entries" + inumInumMap.size() +" : " +removedGluuInumMaps.size());

        ArrayList<JansInumMap> currentInumMaps = new ArrayList<JansInumMap>(inumInumMap.values());
        currentInumMaps.addAll(addedPrimaryKeyAttrValueInumMap.values());
        log.info("There are '{}' entries after adding '{}' entries"+ currentInumMaps.size()+" : " +
                addedPrimaryKeyAttrValueInumMap.size());

        return currentInumMaps;
    }*/

    public Set<String> getChangedInums(HashMap<String, Integer> currInumWithEntryHashCodeMap,
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
            for (Map.Entry<String, Integer> currEntry : currInumWithEntryHashCodeMap.entrySet()) {
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

    public List<GluuSimplePerson> getRemovedPersons(HashMap<String, Integer> currInumWithEntryHashCodeMap,
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

    public List<String> updateTargetEntriesViaVDS(LinkConfiguration linkConfiguration,
                                                   LdapServerConnection targetServerConnection, Set<String> changedInums) {
        List<String> result = new ArrayList<String>();

        PersistenceEntryManager targetPersistenceEntryManager = targetServerConnection.getPersistenceEntryManager();
        Filter filter = linkService.createObjectClassPresenceFilter();
        for (String changedInum : changedInums) {
            String baseDn = "action=synchronizecache," + personService.getDnForPerson(changedInum);
            try {
                targetPersistenceEntryManager.findEntries(baseDn, DummyEntry.class, filter, SearchScope.SUB, null,
                        null, 0, 0, linkConfiguration.getLdapSearchSizeLimit());
                result.add(changedInum);
                log.info("Updated entry with inum {}"+ changedInum);
            } catch (BasePersistenceException ex) {
                log.error("Failed to update entry with inum '{}' using baseDN {}"+ changedInum, baseDn, ex);
            }
        }

        return result;
    }

    /*public List<String> updateTargetEntriesViaCopy(LinkConfiguration linkConfiguration,
                                                    Map<CacheCompoundKey, GluuSimplePerson> sourcePersonCacheCompoundKeyMap,
                                                    HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap, Set<String> changedInums) {
        HashMap<String, CacheCompoundKey> inumCacheCompoundKeyMap = getInumCacheCompoundKeyMap(
                primaryKeyAttrValueInumMap);
        Map<String, String> targetServerAttributesMapping = getTargetServerAttributesMapping(LinkConfiguration);
        String[] customObjectClasses = appConfiguration.getPersonObjectClassTypes();

        List<String> result = new ArrayList<String>();

        if (!validateTargetServerSchema(LinkConfiguration, targetServerAttributesMapping,
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
    }*/

    public boolean validateTargetServerSchema(LinkConfiguration linkConfiguration,
                                               Map<String, String> targetServerAttributesMapping, String[] customObjectClasses) {
        // Get list of return attributes
        String[] keyAttributesWithoutValues = getCompoundKeyAttributesWithoutValues(linkConfiguration);
        String[] sourceAttributes = getSourceAttributes(linkConfiguration);
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
        targetPerson.setStatus(GluuStatus.ACTIVE.getValue());
        targetPerson.setCustomObjectClasses(customObjectClasses);

        // Update list of return attributes according mapping
        linkService.setTargetEntryAttributes(sourcePerson, targetServerAttributesMapping, targetPerson);

        // Execute interceptor script
        externalLinkService.executeExternalUpdateUserMethods(targetPerson);
        boolean executionResult = externalLinkService.executeExternalUpdateUserMethods(targetPerson);
        if (!executionResult) {
            log.error("Failed to execute Link Interception scripts for person '{}'"+ targetInum);
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

    public boolean updateTargetEntryViaCopy(GluuSimplePerson sourcePerson, String targetInum,
                                             String[] targetCustomObjectClasses, Map<String, String> targetServerAttributesMapping) {
        String targetPersonDn = personService.getDnForPerson(targetInum);
        GluuCustomPerson targetPerson = null;
        boolean updatePerson;
        if (personService.contains(targetPersonDn)) {
            try {
                targetPerson = personService.findPersonByDn(targetPersonDn);
                log.info("Found person by inum '{}'"+ targetInum);
            } catch (EntryPersistenceException ex) {
                log.error("Failed to find person '{}'"+ targetInum, ex);
                return false;
            }
            updatePerson = true;
        } else {
            targetPerson = new GluuCustomPerson();
            targetPerson.setDn(targetPersonDn);
            targetPerson.setInum(targetInum);
            targetPerson.setStatus(GluuStatus.ACTIVE.getValue());
            updatePerson = false;
        }

        if (PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(getLdapEntryManager().getPersistenceType())) {
            targetPerson.setCustomObjectClasses(targetCustomObjectClasses);
        }

        //####################################
        targetPerson.setSourceServerName(sourcePerson.getSourceServerName()); //add keycloak
        //####################################
        targetPerson.setSourceServerUserDn(sourcePerson.getDn());

        linkService.setTargetEntryAttributes(sourcePerson, targetServerAttributesMapping, targetPerson);

        // Execute interceptor script
        boolean executionResult = externalLinkService.executeExternalUpdateUserMethods(targetPerson);
        if (!executionResult) {
            log.error("Failed to execute Link Interception scripts for person '{}'"+ targetInum);
            return false;
        }

        try {
            if (updatePerson) {
                personService.updatePersonWithoutCheck(targetPerson);
                log.info("Updated person '{}'"+ targetInum);
            } else {
                personService.addPersonWithoutCheck(targetPerson);
                log.info("Added new person '{}'"+ targetInum);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            String test = updatePerson ? "update" : "add";
            log.error("Failed to '{}' person '{}'" + test + targetInum + ex);
            return false;
        }

        return true;
    }

    /*public HashMap<String, CacheCompoundKey> getInumCacheCompoundKeyMap(
            HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap) {
        HashMap<String, CacheCompoundKey> result = new HashMap<String, CacheCompoundKey>();

        for (Map.Entry<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMapEntry : primaryKeyAttrValueInumMap
                .entrySet()) {
            result.put(primaryKeyAttrValueInumMapEntry.getValue().getInum(), primaryKeyAttrValueInumMapEntry.getKey());
        }

        return result;
    }*/

    /*public List<JansInumMap> loadInumServerEntries(LinkConfiguration LinkConfiguration,
                                                    LdapServerConnection inumDbServerConnection) {
        PersistenceEntryManager inumDbPersistenceEntryManager = inumDbServerConnection.getPersistenceEntryManager();
        String inumbaseDn = inumDbServerConnection.getBaseDns()[0];

        Filter filterObjectClass = Filter.createEqualityFilter(OxConstants.OBJECT_CLASS,
                JansConstants.objectClassInumMap);
        Filter filterStatus = Filter.createNOTFilter(
                Filter.createEqualityFilter(JansConstants.jansStatus, GluuStatus.INACTIVE.getValue()));
        Filter filter = Filter.createANDFilter(filterObjectClass, filterStatus);

        return inumDbPersistenceEntryManager.findEntries(inumbaseDn, JansInumMap.class, filter, SearchScope.SUB, null,
                null, 0, 0, LinkConfiguration.getLdapSearchSizeLimit());
    }*/

    public List<GluuSimplePerson> loadSourceServerEntriesWithoutLimits(
            LinkConfiguration LinkConfiguration, LdapServerConnection[] sourceServerConnections)
            throws SearchException {
        Filter customFilter = linkService.createFilter(LinkConfiguration.getCustomLdapFilter());
        String[] keyAttributes = getCompoundKeyAttributes(LinkConfiguration);
        String[] keyAttributesWithoutValues = getCompoundKeyAttributesWithoutValues(LinkConfiguration);
        String[] keyObjectClasses = getCompoundKeyObjectClasses(LinkConfiguration);
        String[] sourceAttributes = getSourceAttributes(LinkConfiguration);

        String[] returnAttributes = ArrayHelper.arrayMerge(keyAttributesWithoutValues, sourceAttributes);

        Set<String> addedDns = new HashSet<String>();

        List<GluuSimplePerson> sourcePersons = new ArrayList<GluuSimplePerson>();
        for (LdapServerConnection sourceServerConnection : sourceServerConnections) {
            String sourceServerName = sourceServerConnection.getSourceServerName();

            PersistenceEntryManager sourcePersistenceEntryManager = sourceServerConnection.getPersistenceEntryManager();
            String[] baseDns = sourceServerConnection.getBaseDns();
            Filter filter = linkService.createFilter(keyAttributes, keyObjectClasses, "", customFilter);
            if (log.isTraceEnabled()) {
                log.trace("Using next filter to load entris from source server: {}"+ filter);
            }

            for (String baseDn : baseDns) {
                List<GluuSimplePerson> currentSourcePersons = sourcePersistenceEntryManager.findEntries(baseDn,
                        GluuSimplePerson.class, filter, SearchScope.SUB, returnAttributes, null, 0, 0,
                        LinkConfiguration.getLdapSearchSizeLimit());

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

    public List<GluuSimplePerson> loadSourceServerEntries(LinkConfiguration LinkConfiguration,
                                                           LdapServerConnection[] sourceServerConnections) throws SearchException {
        Filter customFilter = linkService.createFilter(LinkConfiguration.getCustomLdapFilter());
        String[] keyAttributes = getCompoundKeyAttributes(LinkConfiguration);
        String[] keyAttributesWithoutValues = getCompoundKeyAttributesWithoutValues(LinkConfiguration);
        String[] keyObjectClasses = getCompoundKeyObjectClasses(LinkConfiguration);
        String[] sourceAttributes = getSourceAttributes(LinkConfiguration);

        String[] twoLettersArray = createTwoLettersArray();
        String[] returnAttributes = ArrayHelper.arrayMerge(keyAttributesWithoutValues, sourceAttributes);

        Set<String> addedDns = new HashSet<String>();

        List<GluuSimplePerson> sourcePersons = new ArrayList<GluuSimplePerson>();
        for (LdapServerConnection sourceServerConnection : sourceServerConnections) {
            String sourceServerName = sourceServerConnection.getSourceServerName();

            PersistenceEntryManager sourcePersistenceEntryManager = sourceServerConnection.getPersistenceEntryManager();
            String[] baseDns = sourceServerConnection.getBaseDns();
            for (String keyAttributeStart : twoLettersArray) {
                Filter filter = linkService.createFilter(keyAttributes, keyObjectClasses, keyAttributeStart,
                        customFilter);
                if (log.isDebugEnabled()) {
                    log.trace("Using next filter to load entris from source server: {}"+ filter);
                }

                for (String baseDn : baseDns) {
                    List<GluuSimplePerson> currentSourcePersons = sourcePersistenceEntryManager.findEntries(baseDn,
                            GluuSimplePerson.class, filter, SearchScope.SUB, returnAttributes, null, 0, 0,
                            LinkConfiguration.getLdapSearchSizeLimit());

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

    public List<TypedGluuSimplePerson> loadTargetServerEntries(LinkConfiguration LinkConfiguration,
                                                                PersistenceEntryManager targetPersistenceEntryManager) {
        Filter filter = Filter.createEqualityFilter(OxConstants.OBJECT_CLASS, JansConstants.objectClassPerson);

        return targetPersistenceEntryManager.findEntries(personService.getDnForPerson(null), TypedGluuSimplePerson.class,
                filter, SearchScope.SUB, TARGET_PERSON_RETURN_ATTRIBUTES, null, 0, 0,
                LinkConfiguration.getLdapSearchSizeLimit());
    }

    public JansInumMap addGluuInumMap(String inumbBaseDn, PersistenceEntryManager inumDbPersistenceEntryManager,
                                       String[] primaryKeyAttrName, String[][] primaryKeyValues) {
        String inum = linkService.generateInumForNewInumMap(inumbBaseDn, inumDbPersistenceEntryManager);
        String inumDn = linkService.getDnForInum(inumbBaseDn, inum);

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
        linkService.addInumMap(inumDbPersistenceEntryManager, inumMap);

        return inumMap;
    }

    /*public HashMap<CacheCompoundKey, JansInumMap> getAllInumServerEntries(
            HashMap<CacheCompoundKey, JansInumMap> primaryKeyAttrValueInumMap,
            HashMap<CacheCompoundKey, JansInumMap> addedPrimaryKeyAttrValueInumMap) {
        HashMap<CacheCompoundKey, JansInumMap> result = new HashMap<CacheCompoundKey, JansInumMap>();

        result.putAll(primaryKeyAttrValueInumMap);
        result.putAll(addedPrimaryKeyAttrValueInumMap);

        return result;
    }*/

    /*public HashMap<String, Integer> getSourcePersonsHashCodesMap(LdapServerConnection inumDbServerConnection,
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
    }*/

    public List<GluuSimplePerson> processTargetPersons(List<TypedGluuSimplePerson> targetPersons,
                                                        HashMap<String, Integer> currInumWithEntryHashCodeMap) {
        List<GluuSimplePerson> result = new ArrayList<GluuSimplePerson>();

        for (GluuSimplePerson targetPerson : targetPersons) {
            String personInum = targetPerson.getStringAttribute(JansConstants.inum);
            if (!currInumWithEntryHashCodeMap.containsKey(personInum)) {
                log.info("Person with such DN: '{}' isn't present on source server"+ targetPerson.getDn());
                result.add(targetPerson);
            }
        }

        return result;
    }

    /*public HashMap<CacheCompoundKey, JansInumMap> getPrimaryKeyAttrValueInumMap(List<JansInumMap> inumMaps) {
        HashMap<CacheCompoundKey, JansInumMap> result = new HashMap<CacheCompoundKey, JansInumMap>();

        for (JansInumMap inumMap : inumMaps) {
            result.put(new CacheCompoundKey(inumMap.getPrimaryKeyValues(), inumMap.getSecondaryKeyValues(),
                    inumMap.getTertiaryKeyValues()), inumMap);
        }

        return result;
    }*/

    /*public HashMap<String, JansInumMap> getInumInumMap(List<JansInumMap> inumMaps) {
        HashMap<String, JansInumMap> result = new HashMap<String, JansInumMap>();

        for (JansInumMap inumMap : inumMaps) {
            result.put(inumMap.getInum(), inumMap);
        }

        return result;
    }*/

    public void closeLdapServerConnection(LdapServerConnection... ldapServerConnections) {
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

    public String getInumCachePath(LinkConfiguration LinkConfiguration) {
        return FilenameUtils.concat(LinkConfiguration.getSnapshotFolder(), "inum_cache.dat");
    }

    public PersistenceEntryManager getLdapEntryManager() {
        return ldapEntryManager;
    }

    public void setLdapEntryManager(PersistenceEntryManager ldapEntryManager) {
        this.ldapEntryManager = ldapEntryManager;
    }

    public class LdapServerConnection {
        private String sourceServerName;
        private PersistenceEntryManager ldapEntryManager;
        private String[] baseDns;

        public LdapServerConnection(String sourceServerName, PersistenceEntryManager ldapEntryManager,
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

    public LinkUpdateMethod getUpdateMethod(LinkConfiguration LinkConfiguration) {
        String updateMethod = LinkConfiguration.getUpdateMethod();
        if (StringHelper.isEmpty(updateMethod)) {
            return LinkUpdateMethod.COPY;
        }

        return LinkUpdateMethod.getByValue(LinkConfiguration.getUpdateMethod());
    }

    public String[] getSourceAttributes(LinkConfiguration LinkConfiguration) {
        return LinkConfiguration.getSourceAttributes().toArray(new String[0]);
    }

    public String[] getCompoundKeyObjectClasses(LinkConfiguration LinkConfiguration) {
        return LinkConfiguration.getKeyObjectClasses().toArray(new String[0]);
    }

    public String[] getCompoundKeyAttributes(LinkConfiguration LinkConfiguration) {
        return LinkConfiguration.getKeyAttributes().toArray(new String[0]);
    }

    public String[] getCompoundKeyAttributesWithoutValues(LinkConfiguration LinkConfiguration) {
        String[] result = LinkConfiguration.getKeyAttributes().toArray(new String[0]);
        for (int i = 0; i < result.length; i++) {
            int index = result[i].indexOf('=');
            if (index != -1) {
                result[i] = result[i].substring(0, index);
            }
        }

        return result;
    }

    public Map<String, String> getTargetServerAttributesMapping(LinkConfiguration LinkConfiguration) {
        Map<String, String> result = new HashMap<String, String>();
        for (LinkAttributeMapping attributeMapping : LinkConfiguration.getAttributeMapping()) {
            result.put(attributeMapping.getDestination(), attributeMapping.getSource());
        }

        return result;
    }

    public Properties toLdapProperties(PersistenceEntryManagerFactory ldapEntryManagerFactory,
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

    public String[] getBaseDNs(GluuLdapConfiguration ldapConfiguration) {
        return ldapConfiguration.getBaseDNsStringsList().toArray(new String[0]);
    }

    @ObjectClass(value = "gluuPerson")
    public class TypedGluuSimplePerson extends GluuSimplePerson {
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
