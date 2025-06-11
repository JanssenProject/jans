package io.jans.casa.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.agama.model.*;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;
import io.jans.orm.ldap.operation.LdapOperationService;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.service.cache.CacheConfiguration;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;

import java.io.FileReader;
import java.io.Reader;
import java.util.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.jans.casa.model.ApplicationConfiguration;
import io.jans.casa.core.model.ASConfiguration;
import io.jans.casa.core.model.CustomScript;
import io.jans.casa.core.model.JansOrganization;
import io.jans.casa.core.model.GluuConfiguration;
import io.jans.casa.core.model.Person;
import io.jans.casa.misc.Utils;
import io.jans.casa.service.IPersistenceService;

import org.jboss.weld.inject.WeldInstance;
import org.json.JSONObject;
import org.slf4j.Logger;

@ApplicationScoped
public class PersistenceService implements IPersistenceService {

    private static final int RETRIES = 15;
    private static final int RETRY_INTERVAL = 15;
    private static final String DEFAULT_CONF_BASE = "/etc/jans/conf";
    private static final String ADMIN_ROLE = "CasaAdmin";

    @Inject
    private Logger logger;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private WeldInstance<PersistenceEntryManagerFactory> pFactoryInstance;

    private PersistenceEntryManager entryManager;

    private LdapOperationService ldapOperationService;

    private String rootDn;
    private String pythonLibLocation;

    private JsonNode dynamicConfig;

    private JsonNode staticConfig;

    private Set<String> personCustomObjectClasses;

    private ObjectMapper mapper;

    private StringEncrypter stringEncrypter;

    private CacheConfiguration cacheConfiguration;
    private DocumentStoreConfiguration documentStoreConfiguration;

    public boolean initialize() {

        boolean success = false;
        try {
            mapper = new ObjectMapper();
            success = setup(RETRIES, RETRY_INTERVAL);
            logger.info("PersistenceService was{} initialized successfully", success ? "" : " not");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return success;

    }

    public <T> List<T> find(Class<T> clazz, String baseDn, Filter filter, int start, int count) {

        try {
            return entryManager.findEntries(baseDn, clazz, filter, SearchScope.SUB, null, null, start, count, 0);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public <T> List<T> find(Class<T> clazz, String baseDn, Filter filter) {

        try {
            return entryManager.findEntries(baseDn, clazz, filter);
        } catch (Exception e) {
            //logger.error(e.getMessage(), e);
            //TODO: uncomment the above once https://github.com/GluuFederation/oxCore/issues/160 is solved
            logger.error(e.getMessage());
            return Collections.emptyList();
        }

    }

    public <T> List<T> find(Class<T> clazz, String baseDn, Filter filter, int start, int count, SearchScope searchScope) {

        try {
            return entryManager.findEntries(baseDn, clazz, filter, searchScope, null, null, start, count, 0);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }

    }

    public <T> List<T> find(T object) {

        try {
            return entryManager.findEntries(object);
        } catch (Exception e) {
            //logger.error(e.getMessage(), e);
            //TODO: uncomment the above once https://github.com/GluuFederation/oxCore/issues/160 is solved
            logger.error(e.getMessage());
            return Collections.emptyList();
        }
    }

    public <T> int count(T object) {

        try {
            return entryManager.countEntries(object);
        } catch (Exception e) {
            //logger.error(e.getMessage(), e);
            //TODO: uncomment the above once https://github.com/GluuFederation/oxCore/issues/160 is solved
            logger.warn(e.getMessage());
            return -1;
        }

    }

    public <T> boolean add(T object) {

        try {
            entryManager.persist(object);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

    }

    public <T> T get(Class<T> clazz, String dn) {

        try {
            return entryManager.find(clazz, dn);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }

    }

    public <T> boolean modify(T object) {

        try {
            entryManager.merge(object);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

    }

    public <T> boolean delete(T object) {

        try {
            entryManager.remove(object);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

    }

    public JSONObject getAgamaFlowConfigProperties(String qname) {
        return Optional.ofNullable(getAgamaFlow(qname, Flow.ATTR_NAMES.META)).map(Flow::getMetadata)
                .map(FlowMetadata::getProperties).map(JSONObject::new).orElse(null);
    }

    public Map<String, String> getCustScriptConfigProperties(String acr) {
        return Optional.ofNullable(getScript(acr)).map(Utils::scriptConfigPropertiesAsMap).orElse(null);
    }

    public JansOrganization getOrganization() {
        return get(JansOrganization.class, rootDn);
    }

    public Set<String> getPersonOCs() {
        return personCustomObjectClasses;
    }

    public String getPersonDn(String id) {
        return String.format("inum=%s,%s", id, getPeopleDn());
    }

    public String getPeopleDn() {
        return jsonProperty(staticConfig, "baseDn", "people");
    }

    public String getGroupsDn() {
        return jsonProperty(staticConfig, "baseDn", "groups");
    }

    public String getClientsDn() {
        return jsonProperty(staticConfig, "baseDn", "clients");
    }

    public String getScopesDn() {
        return jsonProperty(staticConfig, "baseDn", "scopes");
    }

    public String getCustomScriptsDn() {
        return jsonProperty(staticConfig, "baseDn", "scripts");
    }

    public String getIssuerUrl() {
        return jsonProperty(dynamicConfig, "issuer");
    }

    public PersistenceEntryManager getEntryManager() {
        return entryManager;
    }

    public boolean isAdmin(String userId) {

        Person petardo = get(Person.class, getPersonDn(userId));
        return petardo != null
                && petardo.getRoles().stream().anyMatch(ADMIN_ROLE::equals);

    }

    public String getIntrospectionEndpoint() {
        return jsonProperty(dynamicConfig, "introspectionEndpoint");
    }
    
    public String getAuthorizationEndpoint() {
        return jsonProperty(dynamicConfig, "authorizationEndpoint");
    }
    
    public String getTokenEndpoint() {
        return jsonProperty(dynamicConfig, "tokenEndpoint");
    }
    
    public String getUserInfoEndpoint() {
        return jsonProperty(dynamicConfig, "userInfoEndpoint");
    }
    
    public String getEndSessionEndpoint() {
        return jsonProperty(dynamicConfig, "endSessionEndpoint");
    }
    
    public String getJwksUri() {
        return jsonProperty(dynamicConfig, "jwksUri");
    }

    @Produces
    @ApplicationScoped
    public StringEncrypter getStringEncrypter() {
        return stringEncrypter;
    }

    public String getPythonLibLocation() {
        return pythonLibLocation;
    }

    public CacheConfiguration getCacheConfiguration() {
        return cacheConfiguration;
    }

    public DocumentStoreConfiguration getDocumentStoreConfiguration() {
        return null;
        //return documentStoreConfiguration;
    }

    public boolean authenticate(String uid, String pass) throws Exception {
        return entryManager.authenticate(rootDn, Person.class, uid, pass);
    }

    public void prepareFidoBranch(String userInum) {
        prepareBranch(userInum, "fido");
    }

    public void prepareFido2Branch(String userInum) {
        prepareBranch(userInum, "fido2_register");
    }

    public ApplicationConfiguration getAppConfiguration() {
        String baseDn = String.format("ou=casa,ou=configuration,%s", getRootDn());
        return get(ApplicationConfiguration.class, baseDn);
    }

    private void prepareBranch(String userInum, String ou) {

        String dn = String.format("ou=%s,%s", ou, getPersonDn(userInum));
        SimpleBranch entry = get(SimpleBranch.class, dn);
        if (entry == null) {
            logger.info("Non existing {} branch for {}, creating...", ou, userInum);
            entry = new SimpleBranch(dn, ou);

            if (!add(entry)) {
                logger.error("Could not create {} branch", ou);
            }
        }

    }

    public CustomScript getScript(String acr) {

        CustomScript script = new CustomScript();
        script.setDisplayName(acr);
        script.setBaseDn(getCustomScriptsDn());

        List<CustomScript> scripts = find(script);
        if (scripts.isEmpty()) {
            logger.warn("Script '{}' not found", acr);
            script = null;
        } else {
            script = scripts.get(0);
        }
        return script;

    }
    
    private Flow getAgamaFlow(String qname, String... attributes) {
        
        try {
            String dn = String.format("%s=%s,ou=flows,ou=agama,o=jans", Flow.ATTR_NAMES.QNAME, qname);
            return entryManager.find(dn, Flow.class, attributes);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
        
    }
    
    private String jsonProperty(JsonNode node, String ...path) {
        
        for (String prop : path) {
            node = node.get(prop);
        }
        return node.textValue();
        
    }

    private boolean loadApplianceSettings(Properties properties) {

        boolean success = false;
        try {
            loadASSettings(properties.getProperty("jansAuth_ConfigurationEntryDN"), properties.getProperty("persistence.type"));
            pythonLibLocation = properties.getProperty("pythonModulesDir");
            rootDn = "o=jans";
            success = true;
            
            cacheConfiguration = get(GluuConfiguration.class, 
                jsonProperty(staticConfig, "baseDn", "configuration")).getCacheConfiguration();
            //documentStoreConfiguration = gluuConf.getDocumentStoreConfiguration();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return success;

    }

    private void loadASSettings(String dn, String persistenceType) throws Exception {

        ASConfiguration conf = get(ASConfiguration.class, dn);
        dynamicConfig = mapper.readTree(conf.getJansConfDyn());
        staticConfig = mapper.readTree(conf.getJansConfStatic());

        if (persistenceType.equals(LdapEntryManagerFactory.PERSISTENCE_TYPE)) {

            personCustomObjectClasses = Optional.ofNullable(dynamicConfig.get("personCustomObjectClassList"))
                    .map(node -> {
                        try {
                            List<String> ocs = new ArrayList<>();
                            node.elements().forEachRemaining(e -> ocs.add(e.asText()));
                            
                            return Set.copyOf(ocs);
                        } catch (Exception e) {
                            logger.error(e.getMessage());
                            return null;
                        }
                    })
                    .orElse(Collections.singleton("jansCustomPerson"));
        }
        if (personCustomObjectClasses == null) {
            personCustomObjectClasses = Collections.emptySet();
        }

    }

    LdapOperationService getOperationService() {
        return ldapOperationService;
    }

    public String getRootDn() {
        return rootDn;
    }

    private boolean setup(int retries, int retry_interval) throws Exception {

        boolean ret = false;
        entryManager = null;
        stringEncrypter = Utils.stringEncrypter();

        //load the configuration using the jans-core-persistence-cdi API
        logger.debug("Obtaining PersistenceEntryManagerFactory from persistence API");
        PersistenceConfiguration persistenceConf = persistanceFactoryService.loadPersistenceConfiguration();
        FileConfiguration persistenceConfig = persistenceConf.getConfiguration();
        Properties backendProperties = persistenceConfig.getProperties();
        PersistenceEntryManagerFactory factory = persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceConf);

        String type = factory.getPersistenceType();
        logger.info("Underlying database of type '{}' detected", type);
        String file = String.format("%s/%s", DEFAULT_CONF_BASE, persistenceConf.getFileName());
        logger.info("Using config file: {}", file);

        logger.debug("Decrypting backend properties");
        backendProperties = PropertiesDecrypter.decryptAllProperties(stringEncrypter, backendProperties);

        logger.info("Obtaining a Persistence EntryManager");
        int i = 0;

        do {
            try {
                i++;
                entryManager = factory.createEntryManager(backendProperties);
            } catch (Exception e) {
                logger.warn("Unable to create persistence entry manager, retrying in {} seconds", retry_interval);
                Thread.sleep(retry_interval * 1000);
            }
        } while (entryManager == null && i < retries);

        if (entryManager == null) {
            logger.error("No EntryManager could be obtained");
        } else {

            try (Reader f = new FileReader(String.format("%s/jans.properties", DEFAULT_CONF_BASE))) {

                Properties generalProps = new Properties();
                generalProps.load(f);
                //Initialize important class members
                ret = loadApplianceSettings(generalProps);
            } catch (Exception e) {
                logger.error("Fatal: jans.properties not readable", e);
            }
        }

        return ret;

    }

}
