package org.xdi.oxd.server;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * oxD configuration.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/07/2013
 */
public class Configuration {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    /**
     * oxD configuration property name
     */
    private static final String CONF_SYS_PROPERTY_NAME = "oxd.server.config";

    /**
     * Configuration file name.
     */
    public static final String FILE_NAME = Utils.isTestMode() ? "configuration-test.json" : "configuration.json";

    /**
     * Lazy initialization via static holder
     */
    private static class Holder {

        private static volatile Configuration CONF = load();

        private static Configuration load() {
            final Configuration fromSysProperty = tryToLoadFromSysProperty();
            if (fromSysProperty != null) {
                LOG.trace("Configuration loaded successfully from system property: {}.", CONF_SYS_PROPERTY_NAME);
                LOG.trace("Configuration: {}", fromSysProperty);
                return fromSysProperty;
            }

            final InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(FILE_NAME);
            final Configuration c = createConfiguration(stream);
            if (c != null) {
                LOG.trace("Configuration loaded successfully.");
                LOG.trace("Configuration: {}", c);
            } else {
                LOG.error("Failed to load configuration.");
            }
            return c;
        }

        private static Configuration tryToLoadFromSysProperty() {
            final String confProperty = System.getProperty(CONF_SYS_PROPERTY_NAME);
            if (StringUtils.isNotBlank(confProperty)) {
                LOG.trace("Try to load configuration from system property: {}, value: {}", CONF_SYS_PROPERTY_NAME, confProperty);
                FileInputStream fis = null;
                try {
                    final File f = new File(confProperty);
                    if (f.exists()) {
                        fis = new FileInputStream(f);
                        return createConfiguration(fis);
                    } else {
                        LOG.info("Failed to load configuration from system property because such file does not exist. Value: {}", confProperty);
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            }

            return null;
        }
    }

    public static Configuration getInstance() {
        return Holder.CONF;
    }

    public static synchronized Configuration createConfigurationAndSet(InputStream p_stream) {
        Holder.CONF = createConfiguration(p_stream);
        return getInstance();
    }

    public static Configuration createConfiguration(InputStream p_stream) {
        try {
            try {
                return CoreUtils.createJsonMapper().readValue(p_stream, Configuration.class);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            return null;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    @JsonProperty(value = "port")
    private int port;
    @JsonProperty(value = "time_out_in_seconds")
    private int timeOutInSeconds;
    @JsonProperty(value = "register_client_app_type")
    private String registerClientAppType;
    @JsonProperty(value = "register_client_response_types")
    private String registerClientResponesType;
    @JsonProperty(value = "localhost_only")
    private Boolean localhostOnly;
    @JsonProperty(value = "use_client_authentication_for_pat")
    private Boolean useClientAuthenticationForPat;
    @JsonProperty(value = "trust_all_certs")
    private Boolean trustAllCerts;
    @JsonProperty(value = "trust_store_path")
    private String keyStorePath;
    @JsonProperty(value = "license_server_endpoint")
    private String licenseServerEndpoint;
    @JsonProperty(value = "license_id")
    private String licenseId;
    @JsonProperty(value = "public_key")
    private String publicKey;
    @JsonProperty(value = "public_password")
    private String publicPassword;
    @JsonProperty(value = "license_password")
    private String licensePassword;
    @JsonProperty(value = "license_check_period_in_hours")
    private Integer licenseCheckPeriodInHours = 24;

    public Integer getLicenseCheckPeriodInHours() {
        return licenseCheckPeriodInHours;
    }

    public void setLicenseCheckPeriodInHours(Integer licenseCheckPeriodInHours) {
        this.licenseCheckPeriodInHours = licenseCheckPeriodInHours;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public String getLicensePassword() {
        return licensePassword;
    }

    public void setLicensePassword(String licensePassword) {
        this.licensePassword = licensePassword;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicPassword() {
        return publicPassword;
    }

    public void setPublicPassword(String publicPassword) {
        this.publicPassword = publicPassword;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getLicenseServerEndpoint() {
        return licenseServerEndpoint;
    }

    public void setLicenseServerEndpoint(String licenseServerEndpoint) {
        this.licenseServerEndpoint = licenseServerEndpoint;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public Boolean getTrustAllCerts() {
        return trustAllCerts;
    }

    public void setTrustAllCerts(Boolean trustAllCerts) {
        this.trustAllCerts = trustAllCerts;
    }

    public Boolean getUseClientAuthenticationForPat() {
        return useClientAuthenticationForPat;
    }

    public void setUseClientAuthenticationForPat(Boolean p_useClientAuthenticationForPat) {
        useClientAuthenticationForPat = p_useClientAuthenticationForPat;
    }

    public Boolean getLocalhostOnly() {
        return localhostOnly;
    }

    public void setLocalhostOnly(Boolean p_localhostOnly) {
        localhostOnly = p_localhostOnly;
    }

    public String getRegisterClientResponesType() {
        return registerClientResponesType;
    }

    public void setRegisterClientResponesType(String p_registerClientResponesType) {
        registerClientResponesType = p_registerClientResponesType;
    }

    public String getRegisterClientAppType() {
        return registerClientAppType;
    }

    public void setRegisterClientAppType(String p_registerClientAppType) {
        registerClientAppType = p_registerClientAppType;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeOutInSeconds() {
        return timeOutInSeconds;
    }

    public void setTimeOutInSeconds(int timeOutInSeconds) {
        this.timeOutInSeconds = timeOutInSeconds;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Configuration");
        sb.append("{port=").append(port);
        sb.append(", timeOutInSeconds=").append(timeOutInSeconds);
        sb.append(", registerClientAppType='").append(registerClientAppType).append('\'');
        sb.append(", registerClientResponesType='").append(registerClientResponesType).append('\'');
        sb.append(", localhostOnly=").append(localhostOnly);
        sb.append(", licenseServerEndpoint=").append(licenseServerEndpoint);
        sb.append(", licenseId=").append(licenseId);
        sb.append('}');
        return sb.toString();
    }
}
