package io.jans.kc.scheduler.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.PatternSyntaxException;

public class AppConfiguration {
    
    private static final String APP_VERSION_UNKNOWN = "N/A";
    private static final String SYS_PROP_APP_VERSION = "app.version";

    private static final String CFG_PROP_APP_VERSION = SYS_PROP_APP_VERSION;
    private static final String CFG_PROP_QUARTZ_SCHEDULER_NAME = "app.scheduler.quartz.name";
    private static final String CFG_PROP_QUARTZ_SCHEDULER_INSTANCEID = "app.scheduler.quartz.instanceid";
    private static final String CFG_PROP_QUARTZ_SCHEDULER_THREAD_POOL_SIZE = "app.scheduler.quartz.threadpoolsize";

    private static final String CFG_PROP_CFGAPI_URL = "app.config-api.url";
    private static final String CFG_PROP_CFGAPI_AUTH_URL = "app.config-api.auth.url";
    private static final String CFG_PROP_CFGAPI_AUTH_CLIENT_ID = "app.config-api.auth.client.id";
    private static final String CFG_PROP_CFGAPI_AUTH_CLIENT_SECRET = "app.config-api.auth.client.secret";
    private static final String CFG_PROP_CFGAPI_AUTH_SCOPES = "app.config-api.auth.client.scopes";
    private static final String CFG_PROP_CFGAPI_AUTH_METHOD = "app.config-api.auth.method";

    private static final String CFG_PROP_KEYCLOAK_ADMIN_SERVER_URL = "app.keycloak-admin.url";
    private static final String CFG_PROP_KEYCLOAK_ADMIN_REALM = "app.keycloak-admin.realm";
    private static final String CFG_PROP_KEYCLOAK_ADMIN_USERNAME = "app.keycloak-admin.username";
    private static final String CFG_PROP_KEYCLOAK_ADMIN_PASSWORD = "app.keycloak-admin.password";
    private static final String CFG_PROP_KEYCLOAK_ADMIN_CLIENT_ID = "app.keycloak-admin.client.id";
    private static final String CFG_PROP_KEYCLOAK_ADMIN_CONN_POOL_SIZE = "app.keycloak-admin.conn.poolsize";

    private static final String CFG_PROP_JOB_TRSYNC_SCHEDULE_INTERVAL = "app.job.trustrelationship-sync.schedule-interval";

    private static final String CFG_PROP_KEYCLOAK_RESOURCES_REALM = "app.keycloak.resources.realm";
    private static final String CFG_PROP_KEYCLOAK_RESOURCES_AUTHN_BROWSER_FLOW_ALIAS = "app.keycloak.resources.authn.browser.flow-alias";
    private static final String CFG_PROP_KEYCLOAK_RESOURCES_SAML_USER_ATTRIBUTE_MAPPER = "app.keycloak.resources.saml.user-attribute-mapper";

    private final Properties configProperties;

    private AppConfiguration(Properties configProperties) {

        this.configProperties = configProperties;
    }

    public String appVersion() {

        return getStringEntry(CFG_PROP_APP_VERSION);
    }

    public String quatzSchedulerName() {

        return getStringEntry(CFG_PROP_QUARTZ_SCHEDULER_NAME);
    }

    public String quartzSchedulerInstanceId() {

        return getStringEntry(CFG_PROP_QUARTZ_SCHEDULER_INSTANCEID);
    }

    public Integer quartzSchedulerThreadPoolSize() {

        return getIntEntry(CFG_PROP_QUARTZ_SCHEDULER_THREAD_POOL_SIZE);
    }

    public String configApiUrl() {

        return getStringEntry(CFG_PROP_CFGAPI_URL);
    }

    public String configApiAuthUrl() {

        return getStringEntry(CFG_PROP_CFGAPI_AUTH_URL);
    }

    public String configApiAuthClientId() {

        return getStringEntry(CFG_PROP_CFGAPI_AUTH_CLIENT_ID);
    }

    public String configApiAuthClientSecret() {

        return getStringEntry(CFG_PROP_CFGAPI_AUTH_CLIENT_SECRET);
    }

    public String keycloakAdminUrl() {

        return getStringEntry(CFG_PROP_KEYCLOAK_ADMIN_SERVER_URL);
    }

    public String keycloakAdminRealm() {

        return getStringEntry(CFG_PROP_KEYCLOAK_ADMIN_REALM);
    }

    public String keycloakAdminUsername() {

        return getStringEntry(CFG_PROP_KEYCLOAK_ADMIN_USERNAME);
    }

    public String keycloakAdminPassword() {

        return getStringEntry(CFG_PROP_KEYCLOAK_ADMIN_PASSWORD);
    }

    public String keycloakAdminClientId() {

        return getStringEntry(CFG_PROP_KEYCLOAK_ADMIN_CLIENT_ID);
    }

    public Integer keycloakAdminConnPoolSize() {

        return getIntEntry(CFG_PROP_KEYCLOAK_ADMIN_CONN_POOL_SIZE);
    }

    public String keycloakResourcesRealm() {

        return getStringEntry(CFG_PROP_KEYCLOAK_RESOURCES_REALM);
    }

    public String keycloakResourcesBrowserFlowAlias() {

        return getStringEntry(CFG_PROP_KEYCLOAK_RESOURCES_AUTHN_BROWSER_FLOW_ALIAS);
    }

    public String keycloakResourcesSamlUserAttributeMapper() {

        return getStringEntry(CFG_PROP_KEYCLOAK_RESOURCES_SAML_USER_ATTRIBUTE_MAPPER);
    }

    public Duration trustRelationshipSyncScheduleInterval() {

        try {
            String value = getStringEntry(CFG_PROP_JOB_TRSYNC_SCHEDULE_INTERVAL);
            if(value == null || value.isEmpty()) {
               return null;
            }
            return Duration.parse(value);
        }catch(DateTimeParseException e) {
            throw new AppConfigException("Could not get the trustrelationship sync job interval",e);
        }
    }

    public List<String> configApiAuthScopes() {

        String scopes = getStringEntry(CFG_PROP_CFGAPI_AUTH_SCOPES);
        if(scopes == null) {
            return new ArrayList<String>();
        }

        try {

            String [] individualscopes = scopes.split(",");
            List<String> ret = new ArrayList<String>();
            for(String scope: individualscopes) {
                ret.add(scope.trim());
            }
            return ret;
        }catch(PatternSyntaxException e) {
            throw new AppConfigException("Could not get config api scopes",e);
        }
    }

    public ConfigApiAuthnMethod configApiAuthMethod() {

        return ConfigApiAuthnMethod.fromString(getStringEntry(CFG_PROP_CFGAPI_AUTH_METHOD));
    }

    private String getStringEntry(String entry) {

        return configProperties.getProperty(entry);
    }

    private Integer getIntEntry(String entry) {

        String strvalue = configProperties.getProperty(entry);
        if(strvalue == null || strvalue.isEmpty() ) {
            return null;
        }
        try {
            return Integer.parseInt(strvalue);
        }catch(NumberFormatException e) {
            throw new AppConfigException("Unable to get specified configuration entryQue Dor",e);
        }
    }


    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("Application version: " + appVersion()+" ");
        return sb.toString();
    }

    public static final AppConfiguration fromFile(String path) {

        Properties props = new Properties();
        try {
            FileInputStream cfs = new FileInputStream(path);
            props.load(cfs);
            props = mergeWithSystemProperties(props);
            return new AppConfiguration(props);
        }catch(FileNotFoundException e) {
            throw new AppConfigException("Specified configuration file not found",e);
        }catch(IOException e) {
            throw new AppConfigException("Error when loading configuration file",e);
        }

    }

    private static final Properties mergeWithSystemProperties(Properties props) {

        //include application version if it doesn't exist 
        String appversion = System.getProperty(SYS_PROP_APP_VERSION);
        if(appversion != null ) {
            props.setProperty(CFG_PROP_APP_VERSION, appversion);
        }else {
            props.setProperty(CFG_PROP_APP_VERSION,APP_VERSION_UNKNOWN);
        }

        return props;
    }
}
