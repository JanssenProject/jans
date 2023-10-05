package io.jans.casa.plugins.authnmethod.conf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Created by jgomer on 2017-09-06.
 * POJO storing values needed for Supergluu. Static method of this class parse information belonging to the corresponding
 * custom script to be able to get an instance of this class.
 * Only the basic properties required for enrolling are parsed, so there is no need to inspect super_gluu_creds.json
 */

public class SGConfig extends QRConfig {

    private static Logger LOG = LogManager.getLogger(SGConfig.class);

    private String appId;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * Creates an SGConfig object to hold all properties required for SuperGluu operation
     * @param propsMap A map of string-ed key/value pairs with the source of data for this operation
     * @return null if an error or inconsistency is found while inspecting the configuration properties of the custom script.
     * Otherwise returns a SGConfig object
     */
    public static SGConfig get(Map<String, String> propsMap) {

        SGConfig cfg = new SGConfig();
        try {
            cfg.populate(propsMap);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            cfg = null;
        }
        return cfg;

    }

}
