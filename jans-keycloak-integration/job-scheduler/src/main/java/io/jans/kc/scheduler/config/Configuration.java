package io.jans.kc.scheduler.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Configuration {
    
    private static final String APP_VERSION_UNKNOWN = "N/A";
    private static final String SYS_PROP_APP_VERSION = "app.version";

    private static final String CFG_PROP_APP_VERSION = SYS_PROP_APP_VERSION;

    private final Properties configProperties;

    private Configuration(Properties configProperties) {

        this.configProperties = configProperties;
    }

    public final String getAppVersion() {

        return configProperties.getProperty(CFG_PROP_APP_VERSION);
    }

    @Override
    public String toString() {

        final String header = "+=======================================+";
        final String footer = header;
        final String newline = "\r\n";

        StringBuilder sb = new StringBuilder();
        sb.append(newline);
        sb.append(header+newline);
        sb.append("+ Application version: " + getAppVersion()+" "+newline);
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
