package org.xdi.oxauth.model.config;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.LdapMappingException;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.oxauth.model.error.ErrorMessages;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.util.FileConfiguration;
import org.xdi.oxauth.util.ServerUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ConfigurationFactory {

    private static final Log LOG = Logging.getLog(ConfigurationFactory.class);

    private static final String BASE_DIR = System.getProperty("catalina.home") != null ?
            System.getProperty("catalina.home") :
            System.getProperty("jboss.home.dir");
    private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

    private static final String CONFIG_FILE_PATH = DIR + "oxauth-config.xml";
    private static final String LDAP_FILE_PATH = DIR + "oxauth-ldap.properties";

    public static final String ERRORS_FILE_PATH = DIR + "oxauth-errors.json";
    public static final String STATIC_CONF_FILE_PATH = DIR + "oxauth-static-conf.json";
    public static final String WEB_KEYS_FILE_PATH = DIR + "oxauth-web-keys.json";
    public static final String ID_GEN_SCRIPT_FILE_PATH = DIR + "oxauth-id-gen.py";

    private static final ConfigurationFactory INSTANCE = new ConfigurationFactory();

    private volatile Configuration m_conf;
    private volatile StaticConf m_staticConf;
    private volatile JSONWebKeySet m_jwks;
    private volatile String m_idGenScript;

    private static class LdapHolder {
        private static final FileConfiguration LDAP_CONF = createLdapConfiguration();

        private static FileConfiguration createLdapConfiguration() {
            try {
                return new FileConfiguration(LDAP_FILE_PATH);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        }
    }

    /**
     * Singleton.
     */
    private ConfigurationFactory() {
    }

    public static FileConfiguration getLdapConfiguration() {
        return LdapHolder.LDAP_CONF;  // lazy init via static holder
    }

    public static String getIdGenerationScript() {
        return INSTANCE.getIdGenScript();
    }

    public static Configuration getConfiguration() {
        return INSTANCE.getConf();
    }

    public static StaticConf getStaticConfiguration() {
        return INSTANCE.getStaticConf();
    }

    public static BaseDnConfiguration getBaseDn() {
        return getStaticConfiguration().getBaseDn();
    }

    public static List<ClaimMappingConfiguration> getClaimMappings() {
        return getStaticConfiguration().getClaimMapping();
    }

    public static JSONWebKeySet getWebKeys() {
        return INSTANCE.getKeyValueList();
    }

    public static void create() {
        if (!createFromLdap(true)) {
            LOG.error("Failed to load configuration from LDAP. Please fix it!!!.");
            throw new RuntimeException("Failed to load configuration from LDAP.");
//            LOG.warn("Emergency configuration load from files.");
//            createFromFile();
        } else {
            LOG.info("Configuration loaded successfully.");
        }
    }

    private static void createFromFile() {
        final Configuration configFromFile = loadConfFromFile();
        final ErrorMessages errorsFromFile = loadErrorsFromFile();
        final StaticConf staticConfFromFile = loadStaticConfFromFile();
        final JSONWebKeySet webKeysFromFile = loadWebKeysFromFile();
        final String idGenScriptFromFile = loadIdGenPythonScriptFromFile();

        if (configFromFile != null) {
            INSTANCE.setConf(configFromFile);
        } else {
            LOG.error("Failed to load configuration from file: {0}. " + CONFIG_FILE_PATH);
        }

        if (errorsFromFile != null) {
            final ErrorResponseFactory f = ServerUtil.instance(ErrorResponseFactory.class);
            f.setMessages(errorsFromFile);
        } else {
            LOG.error("Failed to load errors from file: {0}. ", ERRORS_FILE_PATH);
        }

        if (staticConfFromFile != null) {
            INSTANCE.setStaticConf(staticConfFromFile);
        } else {
            LOG.error("Failed to load static configuration from file: {0}. ", STATIC_CONF_FILE_PATH);
        }

        if (webKeysFromFile != null) {
            INSTANCE.setKeyValueList(webKeysFromFile);
        } else {
            LOG.error("Failed to load web keys configuration from file: {0}. ", WEB_KEYS_FILE_PATH);
        }

        if (StringUtils.isNotBlank(idGenScriptFromFile)) {
            INSTANCE.setIdGenScript(idGenScriptFromFile);
        } else {
            LOG.error("Failed to load id gen script from file: {0}", ID_GEN_SCRIPT_FILE_PATH);
        }
    }

    public static boolean updateFromLdap() {
        return createFromLdap(false);
    }

    private static boolean createFromLdap(boolean p_recoverFromFiles) {
        LOG.info("Loading configuration from LDAP...");
        final LdapEntryManager ldapManager = ServerUtil.getLdapManager();
        final String dn = getLdapConfiguration().getString("configurationEntryDN");
        try {
            final Conf conf = ldapManager.find(Conf.class, dn);
            if (conf != null) {
                init(conf);
                return true;
            }
        } catch (LdapMappingException e) {
            LOG.trace(e.getMessage(), e);
            if (p_recoverFromFiles) {
                LOG.info("Unable to find configuration in LDAP, try to create configuration entry in LDAP... ");
                if (getLdapConfiguration().getBoolean("createLdapConfigurationEntryIfNotExist")) {
                    createFromFile();
                    final Conf conf = asConf();
                    if (conf != null) {
                        try {
                            ldapManager.persist(conf);
                            LOG.info("Configuration entry is created in LDAP.");
                            return true;
                        } catch (Exception ex) {
                            LOG.error(e.getMessage(), ex);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        return false;
    }

    private static Conf asConf() {
        try {
            final String dn = getLdapConfiguration().getString("configurationEntryDN");
            final ErrorResponseFactory errorFactory = ServerUtil.instance(ErrorResponseFactory.class);

            final Conf c = new Conf();
            c.setDn(dn);
            c.setDynamic(ServerUtil.createJsonMapper().writeValueAsString(INSTANCE.getConf()));
            c.setErrors(ServerUtil.createJsonMapper().writeValueAsString(errorFactory.getMessages()));
            c.setStatics(ServerUtil.createJsonMapper().writeValueAsString(INSTANCE.getStaticConf()));
            c.setWebKeys(ServerUtil.createJsonMapper().writeValueAsString(INSTANCE.getKeyValueList()));
            c.setIdGeneratorScript(INSTANCE.getIdGenScript());
            return c;
        } catch (Exception e) {
            LOG.trace(e.getMessage(), e);
        }
        return null;
    }

    private static void init(Conf p_conf) {
        initConfigurationFromJson(p_conf.getDynamic());
        initStaticConfigurationFromJson(p_conf.getStatics());
        initErrorsFromJson(p_conf.getErrors());
        initWebKeysFromJson(p_conf.getWebKeys());

        final String idGeneratorScript = p_conf.getIdGeneratorScript();
        if (StringUtils.isNotBlank(idGeneratorScript)) {
            INSTANCE.setIdGenScript(idGeneratorScript);
        }
    }

    private static void initWebKeysFromJson(String p_webKeys) {
        try {
            final JSONWebKeySet k = ServerUtil.createJsonMapper().readValue(p_webKeys, JSONWebKeySet.class);
            if (k != null) {
                INSTANCE.setKeyValueList(k);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private static void initStaticConfigurationFromJson(String p_statics) {
        try {
            final StaticConf c = ServerUtil.createJsonMapper().readValue(p_statics, StaticConf.class);
            if (c != null) {
                INSTANCE.setStaticConf(c);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private static void initConfigurationFromJson(String p_configurationJson) {
        try {
            final Configuration c = ServerUtil.createJsonMapper().readValue(p_configurationJson, Configuration.class);
            if (c != null) {
                INSTANCE.setConf(c);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private static void initErrorsFromJson(String p_errosAsJson) {
        try {
            final ErrorMessages errorMessages = ServerUtil.createJsonMapper().readValue(p_errosAsJson, ErrorMessages.class);
            if (errorMessages != null) {
                final ErrorResponseFactory f = ServerUtil.instance(ErrorResponseFactory.class);
                f.setMessages(errorMessages);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private static Configuration loadConfFromFile() {
        try {
            final JAXBContext jc = JAXBContext.newInstance(Configuration.class);
            final Unmarshaller u = jc.createUnmarshaller();
            return (Configuration) u.unmarshal(new File(CONFIG_FILE_PATH));
        } catch (JAXBException e) {
            LOG.error(e.getMessage(), e);
            return null;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private static ErrorMessages loadErrorsFromFile() {
        try {
            return ServerUtil.createJsonMapper().readValue(new File(ERRORS_FILE_PATH), ErrorMessages.class);
        } catch (Exception e) {
            LOG.trace(e.getMessage(), e);
        }
        return null;
    }

    private static StaticConf loadStaticConfFromFile() {
        try {
            return ServerUtil.createJsonMapper().readValue(new File(STATIC_CONF_FILE_PATH), StaticConf.class);
        } catch (Exception e) {
            LOG.trace(e.getMessage(), e);
        }
        return null;
    }

    private static JSONWebKeySet loadWebKeysFromFile() {
        try {
            return ServerUtil.createJsonMapper().readValue(new File(WEB_KEYS_FILE_PATH), JSONWebKeySet.class);
        } catch (Exception e) {
            LOG.trace(e.getMessage(), e);
        }
        return null;
    }

    private static String loadIdGenPythonScriptFromFile() {
        try {
            return FileUtils.readFileToString(new File(ID_GEN_SCRIPT_FILE_PATH), Util.UTF8_STRING_ENCODING);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    public Configuration getConf() {
        return m_conf;
    }

    public void setConf(Configuration p_conf) {
        m_conf = p_conf;
    }

    public StaticConf getStaticConf() {
        return m_staticConf;
    }

    public void setStaticConf(StaticConf p_staticConf) {
        m_staticConf = p_staticConf;
    }

    public JSONWebKeySet getKeyValueList() {
        return m_jwks;
    }

    public void setKeyValueList(JSONWebKeySet p_jwks) {
        m_jwks = p_jwks;
    }

    public String getIdGenScript() {
        return m_idGenScript;
    }

    public void setIdGenScript(String p_idGenScript) {
        m_idGenScript = p_idGenScript;
    }
}