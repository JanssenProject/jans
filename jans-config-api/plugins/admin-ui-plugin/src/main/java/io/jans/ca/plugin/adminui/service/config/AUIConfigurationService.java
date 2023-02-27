package io.jans.ca.plugin.adminui.service.config;

import com.google.api.client.util.Strings;
import com.google.common.collect.Maps;
import io.jans.as.client.TokenRequest;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.LicenseConfig;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.config.LicenseConfiguration;
import io.jans.ca.plugin.adminui.model.config.LicenseSpringCredentials;
import io.jans.ca.plugin.adminui.rest.license.LicenseResource;
import io.jans.ca.plugin.adminui.service.BaseService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ClientFactory;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.orm.PersistenceEntryManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class AUIConfigurationService extends BaseService {

    private Map<String, AUIConfiguration> appConfigurationMap;

    @Inject
    Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    EncryptionService encryptionService;

    @Inject
    ConfigurationService configurationService;

    public AUIConfiguration getAUIConfiguration() {
        return getAUIConfiguration(null);
    }

    /**
     * It reads the configuration from the LDAP server and stores it in a map
     *
     * @param appType The application type. This is either "adminUI" or "ads".
     * @return The AUIConfiguration object
     */
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
                AUIConfiguration auiConfiguration = addPropertiesToAUIConfiguration(appType, appConf);
                if (!appType.equals(AppConstants.APPLICATION_KEY_ADS)) {
                    auiConfiguration.setLicenseConfiguration(addPropertiesToLicenseConfiguration(appConf));
                    appConfigurationMap.put(appType, auiConfiguration);
                }
            }

            return appConfigurationMap.get(appType);
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_READING_CONFIG.getDescription(), e);
            return null;
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

        return auiConfig;
    }

    private LicenseConfiguration addPropertiesToLicenseConfiguration(AdminConf appConf) {
        LicenseConfiguration licenseConfiguration = new LicenseConfiguration();
        LicenseConfig licenseConfig = appConf.getMainSettings().getLicenseConfig();

        if (licenseConfig != null) {

            LicenseSpringCredentials licenseSpringCredentials = requestLicenseCredentialsFromScan(licenseConfig);
            licenseConfiguration.setApiKey(licenseSpringCredentials.getApiKey());
            licenseConfiguration.setProductCode(licenseSpringCredentials.getProductCode());
            licenseConfiguration.setSharedKey(licenseSpringCredentials.getSharedKey());
            licenseConfiguration.setHardwareId(licenseConfig.getLicenseHardwareKey());
            licenseConfiguration.setLicenseKey(licenseConfig.getLicenseKey());
        }
        return licenseConfiguration;
    }

    /**
     * It's a function that makes a call to a REST API endpoint to get a token, then uses that token to make another call
     * to a different REST API endpoint to get some license credentials
     *
     * @param licenseConfig This is the object that contains the configuration parameters for the license.
     */
    private LicenseSpringCredentials requestLicenseCredentialsFromScan(LicenseConfig licenseConfig) {
        try {
            log.info("Inside method to request license credentials from SCAN api.");
            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setAuthUsername(licenseConfig.getOidcClient().getClientId());
            tokenRequest.setAuthPassword(licenseConfig.getOidcClient().getClientSecret());
            tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setScope(LicenseResource.SCOPE_LICENSE_READ);

            log.info("Truing to get access token from auth server.");
            String scanLicenseApiHostname = (new StringBuffer()).append(licenseConfig.getScanLicenseAuthServerHostname()).append("/jans-auth/restv1/token").toString();
            io.jans.as.client.TokenResponse tokenResponse = getToken(tokenRequest, scanLicenseApiHostname);
            // create request header
            MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putSingle("Content-Type", "application/json");
            headers.putSingle("Authorization", "Bearer " + tokenResponse.getAccessToken());

            log.info("Trying to get license credentials from SCAN api.");
            String licenseCredentailsUrl = (new StringBuffer()).append(licenseConfig.getScanLicenseApiHostname())
                    .append("/scan/license/credentials").toString();

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(licenseCredentailsUrl);
            request.headers(headers);

            Map<String, String> body = new HashMap<>();
            body.put("pubKey", licenseConfig.getCredentialsEncryptionKey().getPublicKey());

            Response response = request.post(Entity.entity(body, MediaType.APPLICATION_JSON));
            log.info(" license credentials from scan request status code: {}", response.getStatus());
            if (response.getStatus() == 200) {
                JsonObject entity = response.readEntity(JsonObject.class);
                if (!Strings.isNullOrEmpty(entity.getString("apiKey"))) {
                    //get license spring credentials
                    LicenseSpringCredentials licenseSpringCredentials = new LicenseSpringCredentials();
                    licenseSpringCredentials.setHardwareId(licenseConfig.getLicenseHardwareKey());

                    String privateKey = (new String(Base64.getDecoder().decode(licenseConfig.getCredentialsEncryptionKey().getPrivateKey())))
                            .replace("-----BEGIN PRIVATE KEY-----", "")
                            .replaceAll(System.lineSeparator(), "")
                            .replace("-----END PRIVATE KEY-----", "");
                    ;
                    licenseSpringCredentials.setApiKey(CommonUtils.decode(entity.getString("apiKey"), privateKey));
                    licenseSpringCredentials.setProductCode(CommonUtils.decode(entity.getString("productCode"), privateKey));
                    licenseSpringCredentials.setSharedKey(CommonUtils.decode(entity.getString("sharedKey"), privateKey));

                    log.info(" licenseSpringCredentials.toString(): {}", licenseSpringCredentials.toString());
                    return licenseSpringCredentials;
                }
            }
            log.error("license Activation error response: {}", response.readEntity(String.class));
            return null;
        } catch (Exception e) {
            log.error(ErrorResponse.LICENSE_SPRING_CREDENTIALS_ERROR.getDescription(), e);
            return null;
        }
    }
}