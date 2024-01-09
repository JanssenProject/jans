package io.jans.kc.scheduler.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.PatternSyntaxException;

public class Configuration {
    
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
    private static final String CFG_PROP_CFGAPI_AUTH_SCOPES = "app.config-api.client.auth.scopes";
    private static final String CFG_PROP_CFGAPI_AUTH_METHOD = "app.config-api.auth.method";

    private final Properties configProperties;

    private Configuration(Properties configProperties) {

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
            throw new ConfigurationException("Could not get config api scopes",e);
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
            throw new ConfigurationException("Unable to get specified configuration entryQue Dor",e);
        }
    }


    @Override
    public String toString() {

        final String header = "+=======================================+";
        final String footer = header;
        final String newline = "\r\n";

        StringBuilder sb = new StringBuilder();
        sb.append(newline);
        sb.append(header+newline);
        sb.append("+ Application version: " + appVersion()+" "+newline);
        sb.append(footer);
        
        return sb.toString();
    }

    public static final Configuration fromFile(String path) {

        Properties props = new Properties();
        try {
            FileInputStream cfs = new FileInputStream(path);
            props.load(cfs);
            props = mergeWithSystemProperties(props);
            return new Configuration(props);
        }catch(FileNotFoundException e) {
            throw new ConfigurationException("Specified configuration file not found",e);
        }catch(IOException e) {
            throw new ConfigurationException("Error when loading configuration file",e);
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
