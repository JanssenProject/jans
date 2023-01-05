package io.jans.ca.plugin.adminui.service.config;

import com.google.api.client.util.Strings;
import com.google.common.collect.Maps;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.LicenseSpringCredentials;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.config.LicenseConfiguration;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.ca.plugin.adminui.rest.auth.OAuth2Resource;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.orm.PersistenceEntryManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

@Singleton
public class AUIConfigurationService {

    private Map<String, AUIConfiguration> appConfigurationMap;

    @Inject
    Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    ConfigurationService configurationService;

    public AUIConfiguration getAUIConfiguration() {
        return getAUIConfiguration(null);
    }

    public AUIConfiguration getAUIConfiguration(String appType) {

        try {
            if (Strings.isNullOrEmpty(appType)) {
                appType = AppConstants.APPLICATION_KEY_ADMIN_UI;
            }

            if (appConfigurationMap == null) {
                appConfigurationMap = Maps.newHashMap();
            }

            if (appConfigurationMap.get(appType) == null) {
                AdminConf appConf = null;
                if (appType.equals(AppConstants.APPLICATION_KEY_ADMIN_UI)) {
                    appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
                } else if (appType.equals(AppConstants.APPLICATION_KEY_ADS)) {
                    appConf = entryManager.find(AdminConf.class, AppConstants.ADS_CONFIG_DN);
                }
                appConfigurationMap.put(appType, addPropertiesToAUIConfiguration(appType, appConf));
            }

            return appConfigurationMap.get(appType);
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_READING_CONFIG.getDescription(), e);
            return null;
        }

    }

    public void setAuiConfiguration(AUIConfiguration auiConfiguration) {
        if(!Strings.isNullOrEmpty(auiConfiguration.getAppType())) {
            this.appConfigurationMap.put(auiConfiguration.getAppType(), auiConfiguration);
        }
    }

    private AUIConfiguration addPropertiesToAUIConfiguration(String appType, AdminConf appConf) {
        AUIConfiguration auiConfig = new AUIConfiguration();
        AppConfiguration appConfiguration = configurationService.find();
        auiConfig.setAppType(appType);
        auiConfig.setAuthServerHost(appConf.getMainSettings().getOidcConfig().getAuthServerClient().getOpHost());
        auiConfig.setAuthServerClientId(appConf.getMainSettings().getOidcConfig().getAuthServerClient().getClientId());
        auiConfig.setAuthServerClientSecret(appConf.getMainSettings().getOidcConfig().getAuthServerClient().getClientSecret());
        auiConfig.setAuthServerScope(StringUtils.join(appConf.getMainSettings().getOidcConfig().getAuthServerClient().getScopes(), "+"));
        auiConfig.setAuthServerRedirectUrl(appConf.getMainSettings().getOidcConfig().getAuthServerClient().getRedirectUri());
        auiConfig.setAuthServerFrontChannelLogoutUrl(appConf.getMainSettings().getOidcConfig().getAuthServerClient().getFrontchannelLogoutUri());
        auiConfig.setAuthServerPostLogoutRedirectUri(appConf.getMainSettings().getOidcConfig().getAuthServerClient().getPostLogoutUri());
        auiConfig.setAuthServerAuthzBaseUrl(appConfiguration.getAuthorizationEndpoint());
        auiConfig.setAuthServerTokenEndpoint(appConfiguration.getTokenEndpoint());
        auiConfig.setAuthServerIntrospectionEndpoint(appConfiguration.getIntrospectionEndpoint());
        auiConfig.setAuthServerUserInfoEndpoint(appConfiguration.getUserInfoEndpoint());
        auiConfig.setAuthServerEndSessionEndpoint(appConfiguration.getEndSessionEndpoint());
        auiConfig.setAuthServerAcrValues(StringUtils.join(appConf.getMainSettings().getOidcConfig().getAuthServerClient().getAcrValues(), "+"));

        auiConfig.setTokenServerClientId(appConf.getMainSettings().getOidcConfig().getTokenServerClient().getClientId());
        auiConfig.setTokenServerClientSecret(appConf.getMainSettings().getOidcConfig().getTokenServerClient().getClientSecret());
        auiConfig.setTokenServerScope(StringUtils.join(appConf.getMainSettings().getOidcConfig().getTokenServerClient().getScopes(), "+"));
        auiConfig.setTokenServerTokenEndpoint(appConf.getMainSettings().getOidcConfig().getTokenServerClient().getTokenEndpoint());

        if(appType.equals(AppConstants.APPLICATION_KEY_ADS)) {
            return auiConfig;
        }

        LicenseConfiguration licenseConfiguration = new LicenseConfiguration();
        LicenseSpringCredentials licenseSpringCredentials = appConf.getDynamic().getLicenseSpringCredentials();

        if (licenseSpringCredentials != null) {
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

}