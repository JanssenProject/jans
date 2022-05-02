package io.jans.ca.plugin.adminui.service.config;

import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.LicenseSpringCredentials;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.config.LicenseConfiguration;
import io.jans.ca.plugin.adminui.rest.auth.OAuth2Resource;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.orm.PersistenceEntryManager;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Singleton
public class AUIConfigurationService {

    private AUIConfiguration auiConfiguration;

    @Inject
    Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    public AUIConfiguration getAUIConfiguration() {

        try {
            if (this.auiConfiguration == null) {
                Properties props = loadPropertiesFromFile();
                this.auiConfiguration = addPropertiesToAUIConfiguration(props);
            }

            return auiConfiguration;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_READING_CONFIG.getDescription(), e);
            return null;
        }
    }

    public void setAuiConfiguration(AUIConfiguration auiConfiguration) {
        this.auiConfiguration = auiConfiguration;
    }

    private AUIConfiguration addPropertiesToAUIConfiguration(Properties props) {
        AUIConfiguration auiConfig = new AUIConfiguration();
        auiConfig.setAuthServerHost(props.getProperty("authserver.host"));
        auiConfig.setAuthServerClientId(props.getProperty("authserver.clientId"));
        auiConfig.setAuthServerClientSecret(props.getProperty("authserver.clientSecret"));
        auiConfig.setAuthServerScope(props.getProperty("authserver.scope"));
        auiConfig.setAuthServerRedirectUrl(props.getProperty("authserver.redirectUrl"));
        auiConfig.setAuthServerFrontChannelLogoutUrl(props.getProperty("authserver.frontChannelLogoutUrl"));
        auiConfig.setAuthServerPostLogoutRedirectUri(props.getProperty("authserver.postLogoutRedirectUri"));
        auiConfig.setAuthServerAuthzBaseUrl(props.getProperty("authserver.authzBaseUrl"));
        auiConfig.setAuthServerTokenEndpoint(props.getProperty("authserver.tokenEndpoint"));
        auiConfig.setAuthServerIntrospectionEndpoint(props.getProperty("authserver.introspectionEndpoint"));
        auiConfig.setAuthServerUserInfoEndpoint(props.getProperty("authserver.userInfoEndpoint"));
        auiConfig.setAuthServerEndSessionEndpoint(props.getProperty("authserver.endSessionEndpoint"));
        auiConfig.setAuthServerAcrValues(props.getProperty("authserver.acrValues"));

        auiConfig.setTokenServerClientId(props.getProperty("tokenServer.clientId"));
        auiConfig.setTokenServerClientSecret(props.getProperty("tokenServer.clientSecret"));
        auiConfig.setTokenServerScope(props.getProperty("tokenServer.scope"));
        auiConfig.setTokenServerRedirectUrl(props.getProperty("tokenServer.redirectUrl"));
        auiConfig.setTokenServerFrontChannelLogoutUrl(props.getProperty("tokenServer.frontChannelLogoutUrl"));
        auiConfig.setTokenServerPostLogoutRedirectUri(props.getProperty("tokenServer.postLogoutRedirectUri"));
        auiConfig.setTokenServerAuthzBaseUrl(props.getProperty("tokenServer.authzBaseUrl"));
        auiConfig.setTokenServerTokenEndpoint(props.getProperty("tokenServer.tokenEndpoint"));
        auiConfig.setTokenServerIntrospectionEndpoint(props.getProperty("tokenServer.introspectionEndpoint"));
        auiConfig.setTokenServerUserInfoEndpoint(props.getProperty("tokenServer.userInfoEndpoint"));
        auiConfig.setTokenServerEndSessionEndpoint(props.getProperty("tokenServer.endSessionEndpoint"));
        auiConfig.setTokenServerAcrValues(props.getProperty("tokenServer.acrValues"));

        LicenseConfiguration licenseConfiguration = new LicenseConfiguration();
        AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.CONFIG_DN);
        LicenseSpringCredentials licenseSpringCredentials = adminConf.getDynamic().getLicenseSpringCredentials();

        if(licenseSpringCredentials != null)
        {
            licenseConfiguration.setApiKey(licenseSpringCredentials.getApiKey());
            licenseConfiguration.setProductCode(licenseSpringCredentials.getProductCode());
            licenseConfiguration.setSharedKey(licenseSpringCredentials.getSharedKey());
            licenseConfiguration.setManagementKey(licenseSpringCredentials.getManagementKey());
            licenseConfiguration.setHardwareId(licenseSpringCredentials.getHardwareId());
            licenseConfiguration.setLicenseKey(licenseSpringCredentials.getLicenseKey());
        }
        auiConfig.setLicenseConfiguration(licenseConfiguration);
        return auiConfig;
    }

    private Properties loadPropertiesFromFile() throws IOException {

        Properties props = new Properties();
        File jarPath = new File(OAuth2Resource.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String propertiesPath = jarPath.getParentFile().getAbsolutePath() + "/../config";
        try (InputStream in = new FileInputStream(propertiesPath + "/auiConfiguration.properties")) {
            props.load(in);
            return props;
        } catch (IOException e) {
            log.error(ErrorResponse.ERROR_READING_CONFIG.getDescription());
            throw e;
        }
    }

}