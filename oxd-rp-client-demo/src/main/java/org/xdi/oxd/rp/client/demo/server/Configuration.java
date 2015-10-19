package org.xdi.oxd.rp.client.demo.server;

import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonProperty;
import org.xdi.util.Util;

import java.io.InputStream;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2015
 */

public class Configuration {

    private static final Logger LOG = Logger.getLogger(Configuration.class);

    public static final String FILE_NAME = isTestMode() ? "oxd-rp-demo-test.json" : "oxd-rp-demo.json";

    private static class Holder {

        private static final Configuration CONF = createConfiguration();

        private static Configuration createConfiguration() {
            try {
                try {
                    final InputStream stream = Configuration.class.getResourceAsStream("/" + FILE_NAME);
                    final Configuration c = Util.createJsonMapper().readValue(stream, Configuration.class);
                    if (c != null) {
                        LOG.info("Demo site configuration loaded successfully.");
                    } else {
                        LOG.error("Failed to load Demo site configuration.");
                    }
                    return c;
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
                return null;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        }
    }

    public static Configuration getInstance() {
        return Holder.CONF;
    }

    private static final String APP_MODE = System.getProperty("app.mode");

    public static boolean isTestMode() {
        return "test".equals(APP_MODE);
    }

    @JsonProperty(value = "oxd_host")
    private String oxdHost;
    @JsonProperty(value = "oxd_port")
    private int oxdPort;
    @JsonProperty(value = "site_public_url")
    private String sitePublicUrl;

    public Configuration() {
    }

    public String getSitePublicUrl() {
        return sitePublicUrl;
    }

    public void setSitePublicUrl(String sitePublicUrl) {
        this.sitePublicUrl = sitePublicUrl;
    }

    public String getOxdHost() {
        return oxdHost;
    }

    public void setOxdHost(String oxdHost) {
        this.oxdHost = oxdHost;
    }

    public int getOxdPort() {
        return oxdPort;
    }

    public void setOxdPort(int oxdPort) {
        this.oxdPort = oxdPort;
    }
}
