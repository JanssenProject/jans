package io.jans.ca.plugin.adminui.service.config;

import com.google.api.client.util.Strings;
import com.google.common.collect.Maps;
import io.jans.as.client.TokenRequest;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.LicenseConfig;
import io.jans.as.model.config.adminui.OIDCClientSettings;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.ca.plugin.adminui.model.auth.DCRResponse;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.config.LicenseConfiguration;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.rest.license.LicenseResource;
import io.jans.ca.plugin.adminui.service.BaseService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.EncryptionService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Map;

@Singleton
public class AUIConfigurationService extends BaseService {

    private Map<String, AUIConfiguration> appConfigurationMap;

    @Inject
    Logger logger;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    EncryptionService encryptionService;

    @Inject
    ConfigurationService configurationService;

    public AUIConfiguration getAUIConfiguration() throws Exception {
        return getAUIConfiguration(null);
    }

    /**
     * It reads the configuration from the LDAP server and stores it in a map
     *
     * @param appType The application type. This is either "adminUI" or "ads".
     * @throws Exception
     * @return The AUIConfiguration object
     */
    public AUIConfiguration getAUIConfiguration(String appType) throws Exception {
        logger.info("Inside method to read the configuration from the persistence and stores it in a map.");
        try {
            if (Strings.isNullOrEmpty(appType)) {
                appType = AppConstants.APPLICATION_KEY_ADMIN_UI;
            }

            if (appConfigurationMap == null) {
                appConfigurationMap = Maps.newHashMap();
            }
            AUIConfiguration auiConfiguration = null;

            if (appConfigurationMap.get(appType) == null) {
                AdminConf appConf = null;
                logger.debug("Admin UI configuration is not stored in cache map.");
                if (appType.equals(AppConstants.APPLICATION_KEY_ADMIN_UI)) {
                    logger.debug("Reading Admin UI configuration from persistence.");
                    appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
                } else if (appType.equals(AppConstants.APPLICATION_KEY_ADS)) {
                    appConf = entryManager.find(AdminConf.class, AppConstants.ADS_CONFIG_DN);
                }
                auiConfiguration = addPropertiesToAUIConfiguration(appType, appConf);
                if (!appType.equals(AppConstants.APPLICATION_KEY_ADS)) {
                    auiConfiguration.setLicenseConfiguration(addPropertiesToLicenseConfiguration(appConf));
                    appConfigurationMap.put(appType, auiConfiguration);
                }
            }
            return appConfigurationMap.get(appType);
        } catch (Exception e) {
            logger.error(ErrorResponse.ERROR_READING_CONFIG.getDescription());
            throw e;
        }
    }

    public void setAuiConfiguration(AUIConfiguration auiConfiguration) {
        if (!Strings.isNullOrEmpty(auiConfiguration.getAppType())) {
            this.appConfigurationMap.put(auiConfiguration.getAppType(), auiConfiguration);
        }
    }

    private AUIConfiguration addPropertiesToAUIConfiguration(String appType, AdminConf appConf) {
        AUIConfiguration auiConfig = new AUIConfiguration();
        AppConfiguration appConfiguration = configurationService.find();
        auiConfig.setAppType(appType);
        auiConfig.setAuiWebServerHost(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getOpHost());
        auiConfig.setAuiWebServerClientId(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getClientId());
        auiConfig.setAuiWebServerClientSecret(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getClientSecret());
        auiConfig.setAuiWebServerScope(StringUtils.join(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getScopes(), "+"));
        auiConfig.setAuiWebServerRedirectUrl(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getRedirectUri());
        auiConfig.setAuiWebServerFrontChannelLogoutUrl(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getFrontchannelLogoutUri());
        auiConfig.setAuiWebServerPostLogoutRedirectUri(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getPostLogoutUri());
        auiConfig.setAuiWebServerAuthzBaseUrl(appConfiguration.getAuthorizationEndpoint());
        auiConfig.setAuiWebServerTokenEndpoint(appConfiguration.getTokenEndpoint());
        auiConfig.setAuiWebServerIntrospectionEndpoint(appConfiguration.getIntrospectionEndpoint());
        auiConfig.setAuiWebServerUserInfoEndpoint(appConfiguration.getUserInfoEndpoint());
        auiConfig.setAuiWebServerEndSessionEndpoint(appConfiguration.getEndSessionEndpoint());
        auiConfig.setAuiWebServerAcrValues(StringUtils.join(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getAcrValues(), "+"));

        auiConfig.setAuiBackendApiServerClientId(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getClientId());
        auiConfig.setAuiBackendApiServerClientSecret(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getClientSecret());
        auiConfig.setAuiBackendApiServerScope(StringUtils.join(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getScopes(), "+"));
        auiConfig.setAuiBackendApiServerTokenEndpoint(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getTokenEndpoint());
        auiConfig.setAuiBackendApiServerIntrospectionEndpoint(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getIntrospectionEndpoint());

        auiConfig.setSessionTimeoutInMins(appConf.getMainSettings().getUiConfig().getSessionTimeoutInMins());
        auiConfig.setAllowSmtpKeystoreEdit(appConf.getMainSettings().getUiConfig().getAllowSmtpKeystoreEdit());
        auiConfig.setAdditionalParameters(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getAdditionalParameters());
        return auiConfig;
    }

    private LicenseConfiguration addPropertiesToLicenseConfiguration(AdminConf appConf) {
        logger.debug("Inside method to add Properties to license configuration.");
        LicenseConfiguration licenseConfiguration = new LicenseConfiguration();
        try {
            LicenseConfig licenseConfig = appConf.getMainSettings().getLicenseConfig();

            if (licenseConfig != null) {
                //validateLicenseClientOnAuthServer(licenseConfig);
                licenseConfiguration.setHardwareId(licenseConfig.getLicenseHardwareKey());
                licenseConfiguration.setLicenseKey(licenseConfig.getLicenseKey());
                licenseConfiguration.setScanApiHostname(licenseConfig.getScanLicenseApiHostname());
                licenseConfiguration.setScanAuthServerHostname(licenseConfig.getOidcClient().getOpHost());
                licenseConfiguration.setScanApiClientId(licenseConfig.getOidcClient().getClientId());
                licenseConfiguration.setScanApiClientSecret(licenseConfig.getOidcClient().getClientSecret());
                licenseConfiguration.setLicenseValidUpto(licenseConfig.getLicenseValidUpto());
                licenseConfiguration.setLicenseDetailsLastUpdatedOn(licenseConfig.getLicenseDetailsLastUpdatedOn());
                licenseConfiguration.setIntervalForSyncLicenseDetailsInDays(licenseConfig.getIntervalForSyncLicenseDetailsInDays());
                licenseConfiguration.setProductCode(licenseConfig.getProductCode());
                licenseConfiguration.setProductName(licenseConfig.getProductName());
                licenseConfiguration.setLicenseType(licenseConfig.getLicenseType());
                licenseConfiguration.setCustomerFirstName(licenseConfig.getCustomerFirstName());
                licenseConfiguration.setCustomerLastName(licenseConfig.getCustomerLastName());
                licenseConfiguration.setCustomerEmail(licenseConfig.getCustomerEmail());
                licenseConfiguration.setCompanyName(licenseConfig.getCompanyName());
                licenseConfiguration.setLicenseActive(licenseConfig.getLicenseActive());
                licenseConfiguration.setLicenseExpired(licenseConfig.getLicenseExpired());
                licenseConfiguration.setIntervalForSyncLicenseDetailsInDays(licenseConfig.getIntervalForSyncLicenseDetailsInDays());
            }
            return licenseConfiguration;
        } catch (Exception e) {
            logger.error(ErrorResponse.ERROR_IN_LICENSE_CONFIGURATION_VALIDATION.getDescription());
        }
        return null;
    }

    private void validateLicenseClientOnAuthServer(LicenseConfig licenseConfig) throws ApplicationException {
        try {
            logger.info("Inside method to request license credentials from SCAN api.");
            io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfig.getOidcClient().getOpHost(), licenseConfig.getOidcClient().getClientId(), licenseConfig.getOidcClient().getClientSecret());
            if (tokenResponse == null) {
                //try to re-generate clients using old SSA
                DCRResponse dcrResponse = executeDCR(licenseConfig.getSsa());
                if (dcrResponse == null) {
                    throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_IN_DCR.getDescription());
                }
                tokenResponse = generateToken(licenseConfig.getOidcClient().getOpHost(), licenseConfig.getOidcClient().getClientId(), licenseConfig.getOidcClient().getClientSecret());

                if (tokenResponse == null) {
                    throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
                }
                AdminConf appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
                LicenseConfig lc = appConf.getMainSettings().getLicenseConfig();
                lc.setScanLicenseApiHostname(dcrResponse.getScanHostname());
                OIDCClientSettings oidcClient = new OIDCClientSettings(dcrResponse.getOpHost(), dcrResponse.getClientId(), dcrResponse.getClientSecret());
                lc.setOidcClient(oidcClient);
                appConf.getMainSettings().setLicenseConfig(lc);
                entryManager.merge(appConf);
            }
        } catch (Exception e) {
            logger.error(ErrorResponse.ERROR_IN_LICENSE_CONFIGURATION_VALIDATION.getDescription());
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ERROR_IN_LICENSE_CONFIGURATION_VALIDATION.getDescription());
        }
    }

    private io.jans.as.client.TokenResponse generateToken(String opHost, String clientId, String clientSecret) {
        try {
            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setScope(LicenseResource.SCOPE_LICENSE_READ);

            logger.info("Trying to get access token from auth server: {}", opHost);
            String scanLicenseApiHostname = (new StringBuffer()).append(StringUtils.removeEnd(opHost, "/"))
                    .append("/jans-auth/restv1/token").toString();
            io.jans.as.client.TokenResponse tokenResponse = null;
            tokenResponse = getToken(tokenRequest, scanLicenseApiHostname);
            return tokenResponse;
        } catch (Exception e) {
            logger.error(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            return null;
        }
    }


}