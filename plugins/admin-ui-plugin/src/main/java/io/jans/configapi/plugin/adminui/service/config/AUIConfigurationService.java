package io.jans.configapi.plugin.adminui.service.config;

import io.jans.configapi.plugin.adminui.model.config.AUIConfiguration;
import io.jans.configapi.plugin.adminui.model.config.LicenseConfiguration;
import io.jans.configapi.plugin.adminui.rest.auth.OAuth2Resource;
import io.jans.configapi.plugin.adminui.utils.ErrorResponse;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
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

    private AUIConfiguration addPropertiesToAUIConfiguration(Properties props) {
        AUIConfiguration auiConfiguration = new AUIConfiguration();
        auiConfiguration.setAuthServerHost(props.getProperty("authserver.host"));
        auiConfiguration.setAuthServerClientId(props.getProperty("authserver.clientId"));
        auiConfiguration.setAuthServerClientSecret(props.getProperty("authserver.clientSecret"));
        auiConfiguration.setAuthServerScope(props.getProperty("authserver.scope"));
        auiConfiguration.setAuthServerRedirectUrl(props.getProperty("authserver.redirectUrl"));
        auiConfiguration.setAuthServerFrontChannelLogoutUrl(props.getProperty("authserver.frontChannelLogoutUrl"));
        auiConfiguration.setAuthServerPostLogoutRedirectUri(props.getProperty("authserver.postLogoutRedirectUri"));
        auiConfiguration.setAuthServerAuthzBaseUrl(props.getProperty("authserver.authzBaseUrl"));
        auiConfiguration.setAuthServerTokenEndpoint(props.getProperty("authserver.tokenEndpoint"));
        auiConfiguration.setAuthServerIntrospectionEndpoint(props.getProperty("authserver.introspectionEndpoint"));
        auiConfiguration.setAuthServerUserInfoEndpoint(props.getProperty("authserver.userInfoEndpoint"));
        auiConfiguration.setAuthServerEndSessionEndpoint(props.getProperty("authserver.endSessionEndpoint"));

        auiConfiguration.setTokenServerClientId(props.getProperty("tokenServer.clientId"));
        auiConfiguration.setTokenServerClientSecret(props.getProperty("tokenServer.clientSecret"));
        auiConfiguration.setTokenServerScope(props.getProperty("tokenServer.scope"));
        auiConfiguration.setTokenServerRedirectUrl(props.getProperty("tokenServer.redirectUrl"));
        auiConfiguration.setTokenServerFrontChannelLogoutUrl(props.getProperty("tokenServer.frontChannelLogoutUrl"));
        auiConfiguration.setTokenServerPostLogoutRedirectUri(props.getProperty("tokenServer.postLogoutRedirectUri"));
        auiConfiguration.setTokenServerAuthzBaseUrl(props.getProperty("tokenServer.authzBaseUrl"));
        auiConfiguration.setTokenServerTokenEndpoint(props.getProperty("tokenServer.tokenEndpoint"));
        auiConfiguration.setTokenServerIntrospectionEndpoint(props.getProperty("tokenServer.introspectionEndpoint"));
        auiConfiguration.setTokenServerUserInfoEndpoint(props.getProperty("tokenServer.userInfoEndpoint"));
        auiConfiguration.setTokenServerEndSessionEndpoint(props.getProperty("tokenServer.endSessionEndpoint"));

        LicenseConfiguration licenseConfiguration = new LicenseConfiguration();
        licenseConfiguration.setApiKey(props.getProperty("licenseSpring.apiKey"));
        licenseConfiguration.setProductCode(props.getProperty("licenseSpring.productCode"));
        licenseConfiguration.setSharedKey(props.getProperty("licenseSpring.sharedKey"));
        licenseConfiguration.setEnabled(Boolean.valueOf(props.getProperty("licenseSpring.enabled")));
        licenseConfiguration.setManagementKey(props.getProperty("licenseSpring.managementKey"));
        licenseConfiguration.initializeLicenseManager();

        auiConfiguration.setLicenseConfiguration(licenseConfiguration);

        return auiConfiguration;
    }

    private Properties loadPropertiesFromFile() throws IOException {

        Properties props = new Properties();
        File jarPath = new File(OAuth2Resource.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String propertiesPath = jarPath.getParentFile().getAbsolutePath() + "/../config";
        try (InputStream in = new FileInputStream(propertiesPath + "/auiConfiguration.properties")) {
            props.load(in);
            return props;
        } catch (IOException e) {
            log.error(ErrorResponse.ERROR_READING_CONFIG.getDescription(), e);
            throw e;
        }
    }

}