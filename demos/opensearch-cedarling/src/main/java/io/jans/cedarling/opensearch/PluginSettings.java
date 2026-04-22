package io.jans.cedarling.opensearch;

import java.util.Map;

import org.apache.logging.log4j.*;
import org.json.*;

public class PluginSettings {
    
    private static Logger logger = LogManager.getLogger(PluginSettings.class);

    private long lastUpdated;
    private boolean enabled;
    private boolean skipHits;
    private boolean logCedarlingLogs;
    private JSONObject bootstrapProperties;
    private String searchActionName;
    private String schemaPrefix;
    
    public static PluginSettings from(JSONObject job, long lastUdpated) {
        
        PluginSettings ps = new PluginSettings();
        ps.enabled = job.optBoolean("enabled", true);
        ps.skipHits = job.optBoolean("skipHits", false);
        ps.logCedarlingLogs = job.optBoolean("logCedarlingLogs", true);
        
        ps.bootstrapProperties = job.optJSONObject("bootstrapProperties");        
        if (ps.bootstrapProperties == null) {
            logger.warn("Undefined 'bootstrapProperties'");
            return null;
        }

        ps.searchActionName = job.optString("searchActionName");        
        if (ps.searchActionName == null) {
            logger.warn("Undefined 'searchActionName'");
            return null;
        }

        ps.schemaPrefix = job.optString("schemaPrefix");        
        if (ps.schemaPrefix == null) {
            logger.warn("Undefined 'schemaPrefix'");
            return null;
        }
        
        ps.lastUpdated = lastUdpated;
        return ps;

    }

    @JSONPropertyIgnore
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isSkipHits() {
        return skipHits;
    }
    
    public boolean isLogCedarlingLogs() {
        return logCedarlingLogs;
    }
    
    public JSONObject getBootstrapProperties() {
        return bootstrapProperties;
    }
    
    public String getSearchActionName() {
        return searchActionName;
    }
    
    public String getSchemaPrefix() {
        return schemaPrefix;
    }
    
/*
    public void setLastUdpated(long lastUdpated) {
        this.lastUdpated = lastUdpated;
    }
  */  
    public Map<String, Object> asMap() {
        return new JSONObject(this).toMap();
    }
    
}
