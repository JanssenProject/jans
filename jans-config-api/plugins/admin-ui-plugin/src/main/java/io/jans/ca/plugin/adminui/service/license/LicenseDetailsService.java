package io.jans.ca.plugin.adminui.service.license;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import io.jans.as.client.TokenRequest;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.LicenseConfig;
import io.jans.as.model.config.adminui.OIDCClientSettings;
import io.jans.ca.plugin.adminui.model.auth.*;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.config.LicenseConfiguration;
import io.jans.ca.plugin.adminui.rest.license.LicenseResource;
import io.jans.ca.plugin.adminui.service.BaseService;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ClientFactory;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.orm.PersistenceEntryManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class LicenseDetailsService extends BaseService {

    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    private PersistenceEntryManager entryManager;

    //constants
    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String LICENSE_KEY = "licenseKey";
    public static final String HARDWARE_ID = "hardwareId";
    public static final String BEARER = "Bearer ";
    public static final String MESSAGE = "message";

    /**
     * The function checks the license key and the api key and returns a response object
     *
     * @return A LicenseApiResponse object is being returned.
     */
    public LicenseApiResponse validateLicenseConfiguration() {

        AdminConf appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
        LicenseConfig licenseConfig = appConf.getMainSettings().getLicenseConfig();

        io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfig.getOidcClient().getOpHost(), licenseConfig.getOidcClient().getClientId(), licenseConfig.getOidcClient().getClientSecret());

        if (tokenResponse == null || Strings.isNullOrEmpty(tokenResponse.getAccessToken())) {
            //try to re-generate clients using old SSA
            DCRResponse dcrResponse = executeDCR(licenseConfig.getSsa());
            if (dcrResponse == null) {
                return createLicenseResponse(false, 500, ErrorResponse.ERROR_IN_DCR.getDescription());
            }
            tokenResponse = generateToken(licenseConfig.getOidcClient().getOpHost(), licenseConfig.getOidcClient().getClientId(), licenseConfig.getOidcClient().getClientSecret());

            if (tokenResponse == null) {
                return createLicenseResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }
        }
        return createLicenseResponse(true, 200, "No error in license configuration.");
    }

    public LicenseApiResponse checkLicense() {
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();
            if (licenseConfiguration == null || Strings.isNullOrEmpty(licenseConfiguration.getHardwareId())) {
                log.info("License configuration is not present.");
                return createLicenseResponse(false, 500, "License configuration is not present.");
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getScanApiHostname())) {
                log.info("SCAN api hostname is missing in configuration.");
                return createLicenseResponse(false, 500, "SCAN api hostname is missing in configuration.");
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseKey())) {
                log.info(ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
                return createLicenseResponse(false, 500, ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
            }

            //check license-key
            String checkLicenseUrl = (new StringBuffer()).append(StringUtils.removeEnd(licenseConfiguration.getScanApiHostname(), "/"))
                    .append("/scan/license/isActive")
                    .toString();

            io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfiguration.getScanAuthServerHostname(), licenseConfiguration.getScanApiClientId(), licenseConfiguration.getScanApiClientSecret());
            if (tokenResponse == null) {
                log.info(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
                return createLicenseResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }

            Map<String, String> body = new HashMap<>();
            body.put(LICENSE_KEY, licenseConfiguration.getLicenseKey());
            body.put(HARDWARE_ID, licenseConfiguration.getHardwareId());

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(checkLicenseUrl);
            request.header(AUTHORIZATION, BEARER + tokenResponse.getAccessToken());
            request.header(CONTENT_TYPE, APPLICATION_JSON);
            Response response = request.post(Entity.entity(body, MediaType.APPLICATION_JSON));

            log.info("license request status code: {}", response.getStatus());
            if (response.getStatus() == 200) {
                JsonObject entity = response.readEntity(JsonObject.class);
                if (entity.getBoolean("license_active") && !entity.getBoolean("is_expired")) {
                    return createLicenseResponse(true, 200, "Valid license present.");
                }
            }
            //getting error
            String jsonData = response.readEntity(String.class);
            ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode jsonNode = mapper.readValue(jsonData, com.fasterxml.jackson.databind.JsonNode.class);
            if (!Strings.isNullOrEmpty(jsonNode.get(MESSAGE).textValue())) {
                log.error("license isActive error response: {}", jsonData);
                return createLicenseResponse(false, jsonNode.get("status").intValue(), jsonNode.get(MESSAGE).textValue());
            }
            log.error("license isActive error response: {}", jsonData);
            return createLicenseResponse(false, 500, ErrorResponse.LICENSE_NOT_PRESENT.getDescription());

        } catch (Exception e) {
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return createLicenseResponse(false, 500, ErrorResponse.CHECK_LICENSE_ERROR.getDescription());
        }
    }

    /**
     * The function checks if the license is already active, if not, it creates a header map, creates a body map, and sends
     * a POST request to the license server
     *
     * @param licenseRequest The license key that you received from the license server.
     * @return A LicenseApiResponse object.
     */
    public LicenseApiResponse activateLicense(LicenseRequest licenseRequest) {
        //check is license is already active
        LicenseApiResponse licenseApiResponse = checkLicense();
        if (licenseApiResponse.isApiResult()) {
            return createLicenseResponse(true, 200, "The license has been already activated.");
        }
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();

            log.debug("Trying to activate License.");
            String activateLicenseUrl = (new StringBuffer()).append(StringUtils.removeEnd(licenseConfiguration.getScanApiHostname(), "/"))
                    .append("/scan/license/activate")
                    .toString();

            io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfiguration.getScanAuthServerHostname(), licenseConfiguration.getScanApiClientId(), licenseConfiguration.getScanApiClientSecret());
            if (tokenResponse == null) {
                log.info(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
                return createLicenseResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }

            Map<String, String> body = new HashMap<>();
            body.put(LICENSE_KEY, licenseRequest.getLicenseKey());
            body.put(HARDWARE_ID, licenseConfiguration.getHardwareId());

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(activateLicenseUrl);
            request.header(AUTHORIZATION, BEARER + tokenResponse.getAccessToken());
            request.header(CONTENT_TYPE, APPLICATION_JSON);
            Response response = request.post(Entity.entity(body, MediaType.APPLICATION_JSON));

            log.info("license Activation request status code: {}", response.getStatus());
            if (response.getStatus() == 200) {
                JsonObject entity = response.readEntity(JsonObject.class);
                if (entity.getString("license_key").equals(licenseRequest.getLicenseKey())) {
                    //save license spring credentials
                    AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
                    adminConf.getMainSettings().getLicenseConfig().setLicenseKey(licenseRequest.getLicenseKey());
                    entryManager.merge(adminConf);
                    //save in license configuration
                    licenseConfiguration.setLicenseKey(licenseRequest.getLicenseKey());
                    auiConfiguration.setLicenseConfiguration(licenseConfiguration);
                    auiConfigurationService.setAuiConfiguration(auiConfiguration);

                    return createLicenseResponse(true, 200, "License have been activated.");
                }
            }
            //getting error
            String jsonData = response.readEntity(String.class);
            ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode jsonNode = mapper.readValue(jsonData, com.fasterxml.jackson.databind.JsonNode.class);
            if (!Strings.isNullOrEmpty(jsonNode.get(MESSAGE).textValue())) {
                log.error("license Activation error response: {}", jsonData);
                return createLicenseResponse(false, jsonNode.get("status").intValue(), jsonNode.get(MESSAGE).textValue());
            }
            log.error("license Activation error response: {}", jsonData);
            return createLicenseResponse(false, response.getStatus(), "License is not activated.");
        } catch (Exception e) {
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return createLicenseResponse(false, 500, ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription());
        }
    }

    /**
     * This function generates a trial license by sending a request to a specified URL and saving the license key in the
     * configuration.
     *
     * @return The method is returning a LicenseApiResponse object.
     */
    public LicenseApiResponse generateTrialLicense() {

        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();

            log.debug("Trying to generate trial License.");
            String trialLicenseUrl = (new StringBuffer()).append(StringUtils.removeEnd(licenseConfiguration.getScanApiHostname(), "/"))
                    .append("/scan/license/trial")
                    .toString();

            io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfiguration.getScanAuthServerHostname(), licenseConfiguration.getScanApiClientId(), licenseConfiguration.getScanApiClientSecret());
            if (tokenResponse == null) {
                log.info(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
                return createLicenseResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }

            Map<String, String> body = new HashMap<>();
            body.put(HARDWARE_ID, licenseConfiguration.getHardwareId());

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(trialLicenseUrl);
            request.header(AUTHORIZATION, BEARER + tokenResponse.getAccessToken());
            request.header(CONTENT_TYPE, APPLICATION_JSON);
            Response response = request.post(Entity.entity(body, MediaType.APPLICATION_JSON));

            log.info("Generate trial license request status code: {}", response.getStatus());
            if (response.getStatus() == 200) {
                JsonObject entity = response.readEntity(JsonObject.class);
                if (!Strings.isNullOrEmpty(entity.getString("license"))) {
                    //save license spring credentials
                    AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
                    adminConf.getMainSettings().getLicenseConfig().setLicenseKey(entity.getString("license"));
                    entryManager.merge(adminConf);
                    //save in license configuration
                    licenseConfiguration.setLicenseKey(entity.getString("license"));
                    auiConfiguration.setLicenseConfiguration(licenseConfiguration);
                    auiConfigurationService.setAuiConfiguration(auiConfiguration);

                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.createObjectNode();
                    ((ObjectNode) jsonNode).put("license-key", entity.getString("license"));

                    return createLicenseResponse(true, 200, "Trial license generated.", jsonNode);
                }
            }
            //getting error
            String jsonData = response.readEntity(String.class);
            ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode jsonNode = mapper.readValue(jsonData, com.fasterxml.jackson.databind.JsonNode.class);
            if (!Strings.isNullOrEmpty(jsonNode.get(MESSAGE).textValue())) {
                log.error("Generate trial license error response: {}", jsonData);
                return createLicenseResponse(false, jsonNode.get("status").intValue(), jsonNode.get(MESSAGE).textValue());
            }
            log.error("Generate trial license error response: {}", jsonData);
            return createLicenseResponse(false, response.getStatus(), "Error in generating trial license.");
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_TRIAL_LICENSE.getDescription(), e);
            return createLicenseResponse(false, 500, ErrorResponse.ERROR_IN_TRIAL_LICENSE.getDescription());
        }
    }

    /**
     * This function is used to get the license details of the admin-ui
     *
     * @return A LicenseResponse object
     */
    public LicenseResponse getLicenseDetails() {
        LicenseResponse licenseResponse = new LicenseResponse();
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();

            //check license-key
            String checkLicenseUrl = (new StringBuffer()).append(StringUtils.removeEnd(licenseConfiguration.getScanApiHostname(), "/"))
                    .append("/scan/license/isActive")
                    .toString();

            io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfiguration.getScanAuthServerHostname(), licenseConfiguration.getScanApiClientId(), licenseConfiguration.getScanApiClientSecret());
            if (tokenResponse == null) {
                log.info(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
                return licenseResponse;
            }

            Map<String, String> body = new HashMap<>();
            body.put(LICENSE_KEY, licenseConfiguration.getLicenseKey());
            body.put(HARDWARE_ID, licenseConfiguration.getHardwareId());

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(checkLicenseUrl);
            request.header(AUTHORIZATION, BEARER + tokenResponse.getAccessToken());
            request.header(CONTENT_TYPE, APPLICATION_JSON);
            Response response = request.post(Entity.entity(body, MediaType.APPLICATION_JSON));

            log.info("license details request status code: {}", response.getStatus());
            if (response.getStatus() == 200) {
                JsonObject entity = response.readEntity(JsonObject.class);
                if (entity.getBoolean("license_active") && !entity.getBoolean("is_expired")) {
                    log.debug("Active license for admin-ui found : {}", entity.getJsonObject("product_details").getString("product_name"));
                    licenseResponse.setLicenseEnabled(true);
                    licenseResponse.setProductName(entity.getJsonObject("product_details").getString("product_name"));
                    licenseResponse.setProductCode(entity.getJsonObject("product_details").getString("short_code"));
                    licenseResponse.setLicenseType(entity.getString("license_type"));
                    licenseResponse.setMaxActivations(entity.getInt("max_activations"));
                    licenseResponse.setLicenseKey(entity.getString("license_key"));
                    licenseResponse.setValidityPeriod(entity.getString("validity_period"));
                    licenseResponse.setCompanyName(entity.getJsonObject("customer").getString("company_name"));
                    licenseResponse.setCustomerEmail(entity.getJsonObject("customer").getString("email"));
                    licenseResponse.setCustomerFirstName(entity.getJsonObject("customer").getString("first_name"));
                    licenseResponse.setCustomerLastName(entity.getJsonObject("customer").getString("last_name"));
                    licenseResponse.setLicenseActive(entity.getBoolean("license_active"));
                    return licenseResponse;
                }
            }
            log.error("license details error response: {}", response.readEntity(String.class));
            log.info("Active license for admin-ui not present ");
            licenseResponse.setLicenseEnabled(false);
            return licenseResponse;
        } catch (Exception e) {
            log.error(ErrorResponse.GET_LICENSE_DETAILS_ERROR.getDescription(), e);
            licenseResponse.setLicenseEnabled(false);
            return licenseResponse;
        }

    }

    /**
     * The function takes an SSA string as input, calls the DCR API to get the scan hostname and OIDC client settings, and
     * saves the SSA string and the scan hostname and OIDC client settings in the Admin UI configuration
     *
     * @param ssaRequest The SSA request object.
     * @return A LicenseApiResponse object.
     */
    public LicenseApiResponse postSSA(SSARequest ssaRequest) {
        try {
            DCRResponse dcrResponse = executeDCR(ssaRequest.getSsa());

            if (dcrResponse == null) {
                return createLicenseResponse(false, 500, ErrorResponse.ERROR_IN_DCR.getDescription());
            }
            AdminConf appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            LicenseConfig licenseConfig = appConf.getMainSettings().getLicenseConfig();
            licenseConfig.setSsa(ssaRequest.getSsa());
            licenseConfig.setScanLicenseApiHostname(dcrResponse.getScanHostname());
            OIDCClientSettings oidcClient = new OIDCClientSettings(dcrResponse.getOpHost(), dcrResponse.getClientId(), dcrResponse.getClientSecret());
            licenseConfig.setOidcClient(oidcClient);
            appConf.getMainSettings().setLicenseConfig(licenseConfig);
            entryManager.merge(appConf);
            return createLicenseResponse(true, 201, "SSA saved successfully.");

        } catch (Exception e) {
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return createLicenseResponse(false, 500, ErrorResponse.ERROR_IN_DCR.getDescription());
        }
    }

    private io.jans.as.client.TokenResponse generateToken(String opHost, String clientId, String clientSecret) {
        try {
            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setScope(LicenseResource.SCOPE_LICENSE_READ);

            log.info("Trying to get access token from auth server.");
            String scanLicenseApiHostname = (new StringBuffer()).append(StringUtils.removeEnd(opHost, "/"))
                    .append("/jans-auth/restv1/token").toString();
            io.jans.as.client.TokenResponse tokenResponse = null;
            tokenResponse = getToken(tokenRequest, scanLicenseApiHostname);
            return tokenResponse;
        } catch (Exception e) {
            log.error(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            return null;
        }
    }

    private LicenseApiResponse createLicenseResponse(boolean result, int responseCode, String responseMessage) {
        return createLicenseResponse(result, responseCode, responseMessage, null);
    }

    private LicenseApiResponse createLicenseResponse(boolean result, int responseCode, String responseMessage, JsonNode node) {
        LicenseApiResponse licenseResponse = new LicenseApiResponse();
        licenseResponse.setResponseCode(responseCode);
        licenseResponse.setResponseMessage(responseMessage);
        licenseResponse.setApiResult(result);
        licenseResponse.setResponseObject(node);
        return licenseResponse;
    }
}