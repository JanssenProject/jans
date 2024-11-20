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
import io.jans.ca.plugin.adminui.utils.CommonUtils;
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
import java.util.Optional;

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
    public static final String CODE = "code";
    public static final String LICENSE_ISACTIVE_ERROR_RESPONSE = "License isActive error response";
    public static final String LICENSE_RETRIEVE_ERROR_RESPONSE = "License retrieve error response";
    public static final String LICENSE_ACTIVATE_ERROR_RESPONSE = "License activate error response";
    public static final String LICENSE_APIS_404 = "The requested license apis not found. Response Code: 404";
    public static final String LICENSE_APIS_503 = "The requested license apis not available. Response Code: 503";
    public static final String TRIAL_GENERATE_ERROR_RESPONSE = "Generate Trial license error response";

    /**
     * The function checks the license key and the api key and returns a response object
     *
     * @return A LicenseApiResponse object is being returned.
     */
    public GenericResponse validateLicenseConfiguration() {

        AdminConf appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
        LicenseConfig licenseConfig = appConf.getMainSettings().getLicenseConfig();

        io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfig.getOidcClient().getOpHost(), licenseConfig.getOidcClient().getClientId(), licenseConfig.getOidcClient().getClientSecret());

        if (tokenResponse == null || Strings.isNullOrEmpty(tokenResponse.getAccessToken())) {
            //try to re-generate clients using old SSA
            DCRResponse dcrResponse = executeDCR(licenseConfig.getSsa());
            if (dcrResponse == null) {
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.ERROR_IN_DCR.getDescription());
            }
            try {
                saveCreateClientInPersistence(licenseConfig.getSsa(), dcrResponse);
            } catch (Exception e) {
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.ERROR_IN_SAVING_LICENSE_CLIENT.getDescription());
            }
            tokenResponse = generateToken(licenseConfig.getOidcClient().getOpHost(), licenseConfig.getOidcClient().getClientId(), licenseConfig.getOidcClient().getClientSecret());

            if (tokenResponse == null) {
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }
        }
        return CommonUtils.createGenericResponse(true, 200, "No error in license configuration.");
    }

    public GenericResponse checkLicense() {
        Response response = null;
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();
            if (licenseConfiguration == null || Strings.isNullOrEmpty(licenseConfiguration.getHardwareId())) {
                log.error(ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getScanApiHostname())) {
                log.error(ErrorResponse.SCAN_HOSTNAME_MISSING.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.SCAN_HOSTNAME_MISSING.getDescription());
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseKey())) {
                log.info(ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
                return CommonUtils.createGenericResponse(false, 404, ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
            }

            //check license-key
            String checkLicenseUrl = (new StringBuffer()).append(StringUtils.removeEnd(licenseConfiguration.getScanApiHostname(), "/"))
                    .append("/scan/license/isActive")
                    .toString();

            io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfiguration.getScanAuthServerHostname(), licenseConfiguration.getScanApiClientId(), licenseConfiguration.getScanApiClientSecret());
            if (tokenResponse == null) {
                log.info(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }

            Map<String, String> body = new HashMap<>();
            body.put(LICENSE_KEY, licenseConfiguration.getLicenseKey());
            body.put(HARDWARE_ID, licenseConfiguration.getHardwareId());

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(checkLicenseUrl);
            request.header(AUTHORIZATION, BEARER + tokenResponse.getAccessToken());
            request.header(CONTENT_TYPE, APPLICATION_JSON);
            response = request.post(Entity.entity(body, MediaType.APPLICATION_JSON));

            log.info("license request status code: {}", response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            if (response.getStatus() == 200) {
                JsonObject entity = response.readEntity(JsonObject.class);
                Optional<GenericResponse> genericResOptional = handleMissingFieldsInResponse(entity, ErrorResponse.LICENSE_DATA_MISSING.getDescription(), "license_active", "is_expired");
                if (genericResOptional.isPresent()) {
                    return genericResOptional.get();
                }
                if (!entity.getBoolean("license_active")) {
                    return CommonUtils.createGenericResponse(false, 404, ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
                }
                if (entity.getBoolean("is_expired")) {
                    return CommonUtils.createGenericResponse(false, 500, ErrorResponse.LICENSE_IS_EXPIRED.getDescription());
                }
                return CommonUtils.createGenericResponse(true, 200, "Valid license present.",
                        mapper.readTree(entity.getJsonArray("custom_fields").toString()));

            }
            //getting error

            String jsonData = response.readEntity(String.class);
            JsonNode jsonNode = mapper.readValue(jsonData, JsonNode.class);

            if (!Strings.isNullOrEmpty(jsonNode.get(MESSAGE).textValue())) {
                log.error("{}: {}", LICENSE_ISACTIVE_ERROR_RESPONSE, jsonData);
                return CommonUtils.createGenericResponse(false, jsonNode.get(CODE).intValue(), jsonNode.get(MESSAGE).textValue());
            }
            log.error("{}: {}", LICENSE_ISACTIVE_ERROR_RESPONSE, jsonData);
            return CommonUtils.createGenericResponse(false, 404, ErrorResponse.LICENSE_NOT_PRESENT.getDescription());

        } catch (Exception e) {
            Optional<GenericResponse> genericResOptional = handleLicenseApiNotAccessible(response);
            if (genericResOptional.isPresent()) {
                return genericResOptional.get();
            }
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.CHECK_LICENSE_ERROR.getDescription());
        }
    }

    /**
     * The function `retrieveLicense()` retrieves a license using the provided configuration and returns a generic
     * response.
     *
     * @return The method `retrieveLicense()` returns a `GenericResponse` object.
     */
    public GenericResponse retrieveLicense() {
        Response response = null;
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();
            if (licenseConfiguration == null || Strings.isNullOrEmpty(licenseConfiguration.getHardwareId())) {
                log.error(ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getScanApiHostname())) {
                log.error(ErrorResponse.SCAN_HOSTNAME_MISSING.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.SCAN_HOSTNAME_MISSING.getDescription());
            }

            io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfiguration.getScanAuthServerHostname(), licenseConfiguration.getScanApiClientId(), licenseConfiguration.getScanApiClientSecret());
            if (tokenResponse == null) {
                log.info(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }

            String retriveLicenseUrl = (new StringBuffer()).append(StringUtils.removeEnd(licenseConfiguration.getScanApiHostname(), "/"))
                    .append("/scan/license/retrieve?org_id=" + licenseConfiguration.getHardwareId())
                    .toString();


            Invocation.Builder request = ClientFactory.instance().getClientBuilder(retriveLicenseUrl);
            request.header(AUTHORIZATION, BEARER + tokenResponse.getAccessToken());
            response = request.get();

            log.info("license request status code: {}", response.getStatus());

            ObjectMapper mapper = new ObjectMapper();
            if (response.getStatus() == 200) {
                JsonObject entity = response.readEntity(JsonObject.class);
                JsonNode jsonNode = mapper.createObjectNode();
                Optional<GenericResponse> genericResOptional = handleMissingFieldsInResponse(entity, ErrorResponse.LICENSE_DATA_MISSING.getDescription(), "licenseKey", "mauThreshold");
                if (genericResOptional.isPresent()) {
                    return genericResOptional.get();
                }
                ((ObjectNode) jsonNode).put("licenseKey", entity.getString("licenseKey"));
                ((ObjectNode) jsonNode).put("mauThreshold", entity.getInt("mauThreshold"));

                return CommonUtils.createGenericResponse(true, 200, "Valid license present.", jsonNode);
            }
            //getting error

            String jsonData = response.readEntity(String.class);
            JsonNode jsonNode = mapper.readValue(jsonData, com.fasterxml.jackson.databind.JsonNode.class);

            if (response.getStatus() == 402) {
                log.error("Payment Required: 402");
                return CommonUtils.createGenericResponse(false, 402, "Payment Required. Subscribe Admin UI license on Agama Lab.");
            }
            if (!Strings.isNullOrEmpty(jsonNode.get(MESSAGE).textValue())) {
                log.error("{}: {}", LICENSE_RETRIEVE_ERROR_RESPONSE, jsonData);
                return CommonUtils.createGenericResponse(false, jsonNode.get(CODE).intValue(), jsonNode.get(MESSAGE).textValue());
            }
            log.error("{}: {}", LICENSE_RETRIEVE_ERROR_RESPONSE, jsonData);
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.RETRIEVE_LICENSE_ERROR.getDescription());

        } catch (Exception e) {
            Optional<GenericResponse> genericResOptional = handleLicenseApiNotAccessible(response);
            if (genericResOptional.isPresent()) {
                return genericResOptional.get();
            }
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.RETRIEVE_LICENSE_ERROR.getDescription());
        }
    }

    /**
     * The function checks if the license is already active, if not, it creates a header map, creates a body map, and sends
     * a POST request to the license server
     *
     * @param licenseRequest The license key that you received from the license server.
     * @return A LicenseApiResponse object.
     */
    public GenericResponse activateLicense(LicenseRequest licenseRequest) {
        //check is license is already active
        GenericResponse licenseApiResponse = checkLicense();
        if (licenseApiResponse.isSuccess()) {
            return CommonUtils.createGenericResponse(true, 200, ErrorResponse.LICENSE_ALREADY_ACTIVE.getDescription());
        }
        Response response = null;
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
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }

            Map<String, String> body = new HashMap<>();
            body.put(LICENSE_KEY, licenseRequest.getLicenseKey());
            body.put(HARDWARE_ID, licenseConfiguration.getHardwareId());

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(activateLicenseUrl);
            request.header(AUTHORIZATION, BEARER + tokenResponse.getAccessToken());
            request.header(CONTENT_TYPE, APPLICATION_JSON);
            response = request.post(Entity.entity(body, MediaType.APPLICATION_JSON));

            log.info("license Activation request status code: {}", response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            if (response.getStatus() == 200) {
                JsonObject entity = response.readEntity(JsonObject.class);
                Optional<GenericResponse> genericResOptional = handleMissingFieldsInResponse(entity, ErrorResponse.LICENSE_DATA_MISSING.getDescription(), "license_key");
                if (genericResOptional.isPresent()) {
                    return genericResOptional.get();
                }
                //save license spring credentials
                AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
                adminConf.getMainSettings().getLicenseConfig().setLicenseKey(licenseRequest.getLicenseKey());
                entryManager.merge(adminConf);
                //save in license configuration
                licenseConfiguration.setLicenseKey(licenseRequest.getLicenseKey());
                auiConfiguration.setLicenseConfiguration(licenseConfiguration);
                auiConfigurationService.setAuiConfiguration(auiConfiguration);

                return CommonUtils.createGenericResponse(true, 200, "License have been activated."
                        , mapper.readTree(entity.getJsonArray("custom_fields").toString()));

            }
            //getting error
            String jsonData = response.readEntity(String.class);
            JsonNode jsonNode = mapper.readValue(jsonData, JsonNode.class);
            if (!Strings.isNullOrEmpty(jsonNode.get(MESSAGE).textValue())) {
                log.error("license Activation error response: {}", jsonData);
                log.error("{}: {}", LICENSE_ACTIVATE_ERROR_RESPONSE, jsonData);
                return CommonUtils.createGenericResponse(false, jsonNode.get(CODE).intValue(), jsonNode.get(MESSAGE).textValue());
            }
            log.error("license Activation error response: {}", jsonData);
            log.error("{}: {}", LICENSE_ACTIVATE_ERROR_RESPONSE, jsonData);
            return CommonUtils.createGenericResponse(false, response.getStatus(), "License is not activated.");
        } catch (Exception e) {
            Optional<GenericResponse> genericResOptional = handleLicenseApiNotAccessible(response);
            if (genericResOptional.isPresent()) {
                return genericResOptional.get();
            }
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription());
        }
    }

    /**
     * This function generates a trial license by sending a request to a specified URL and saving the license key in the
     * configuration.
     *
     * @return The method is returning a LicenseApiResponse object.
     */
    public GenericResponse generateTrialLicense() {
        Response response = null;
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
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }

            Map<String, String> body = new HashMap<>();
            body.put(HARDWARE_ID, licenseConfiguration.getHardwareId());

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(trialLicenseUrl);
            request.header(AUTHORIZATION, BEARER + tokenResponse.getAccessToken());
            request.header(CONTENT_TYPE, APPLICATION_JSON);
            response = request.post(Entity.entity(body, MediaType.APPLICATION_JSON));

            log.info("Generate trial license request status code: {}", response.getStatus());

            ObjectMapper objectMapper = new ObjectMapper();
            if (response.getStatus() == 200) {
                JsonObject entity = response.readEntity(JsonObject.class);
                Optional<GenericResponse> genericResOptional = handleMissingFieldsInResponse(entity, ErrorResponse.LICENSE_DATA_MISSING.getDescription(), "license");
                if (genericResOptional.isPresent()) {
                    return genericResOptional.get();
                }
                //save license spring credentials
                AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
                adminConf.getMainSettings().getLicenseConfig().setLicenseKey(entity.getString("license"));
                entryManager.merge(adminConf);
                //save in license configuration
                licenseConfiguration.setLicenseKey(entity.getString("license"));
                auiConfiguration.setLicenseConfiguration(licenseConfiguration);
                auiConfigurationService.setAuiConfiguration(auiConfiguration);

                JsonNode jsonNode = objectMapper.createObjectNode();
                ((ObjectNode) jsonNode).put("license-key", entity.getString("license"));

                return CommonUtils.createGenericResponse(true, 200, "Trial license generated.", jsonNode);
            }
            //getting error

            String jsonData = response.readEntity(String.class);
            JsonNode jsonNode = objectMapper.readValue(jsonData, com.fasterxml.jackson.databind.JsonNode.class);
            if (!Strings.isNullOrEmpty(jsonNode.get(MESSAGE).textValue())) {
                log.error("{}: {}", TRIAL_GENERATE_ERROR_RESPONSE, jsonData);
                return CommonUtils.createGenericResponse(false, jsonNode.get(CODE).intValue(), jsonNode.get(MESSAGE).textValue());
            }
            log.error("{}: {}", TRIAL_GENERATE_ERROR_RESPONSE, jsonData);
            return CommonUtils.createGenericResponse(false, response.getStatus(), "Error in generating trial license.");
        } catch (Exception e) {
            Optional<GenericResponse> genericResOptional = handleLicenseApiNotAccessible(response);
            if (genericResOptional.isPresent()) {
                return genericResOptional.get();
            }
            log.error(ErrorResponse.ERROR_IN_TRIAL_LICENSE.getDescription(), e);
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.ERROR_IN_TRIAL_LICENSE.getDescription());
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
                if (entity.getBoolean("license_active")) {
                    log.debug("Active license for admin-ui found : {}", entity.getJsonObject("product_details").getString("product_name"));
                    licenseResponse.setLicenseEnabled(true);
                    licenseResponse.setProductName(entity.getJsonObject("product_details").getString("product_name"));
                    licenseResponse.setProductCode(entity.getJsonObject("product_details").getString("short_code"));
                    licenseResponse.setLicenseType(entity.getString("license_type"));
                    licenseResponse.setMaxActivations(entity.getInt("max_activations"));
                    licenseResponse.setLicenseKey(entity.getString("license_key"));
                    licenseResponse.setValidityPeriod(entity.getString("validity_period"));
                    licenseResponse.setLicenseActive(entity.getBoolean("license_active"));
                    licenseResponse.setLicenseExpired(entity.getBoolean("is_expired"));

                    JsonObject customer = entity.getJsonObject("customer");
                    if (customer != null) {
                        if (customer.get("company_name") != null) {
                            licenseResponse.setCompanyName(customer.get("company_name").toString());
                        }
                        if (customer.get("email") != null) {
                            licenseResponse.setCustomerEmail(customer.get("email").toString());
                        }
                        if (customer.get("first_name") != null) {
                            licenseResponse.setCustomerFirstName(customer.get("first_name").toString());
                        }
                        if (customer.get("last_name") != null) {
                            licenseResponse.setCustomerLastName(customer.get("last_name").toString());
                        }
                    }

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
    public GenericResponse postSSA(SSARequest ssaRequest) {
        try {
            DCRResponse dcrResponse = executeDCR(ssaRequest.getSsa());

            if (dcrResponse == null) {
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.ERROR_IN_DCR.getDescription());
            }
            saveCreateClientInPersistence(ssaRequest.getSsa(), dcrResponse);
            return CommonUtils.createGenericResponse(true, 201, "SSA saved successfully.");

        } catch (Exception e) {
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.ERROR_IN_DCR.getDescription());
        }
    }

    /**
     * The function saves the client information and license configuration in the persistence layer.
     *
     * @param ssa         The parameter "ssa" is a string that represents the SSA value. It is
     *                    used to set the SSA value in the license configuration.
     * @param dcrResponse DCRResponse is an object that contains the response data from a Dynamic Client Registration (DCR)
     *                    request. It has the following properties:
     */
    private void saveCreateClientInPersistence(String ssa, DCRResponse dcrResponse) throws Exception {
        AdminConf appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
        LicenseConfig licenseConfig = appConf.getMainSettings().getLicenseConfig();
        licenseConfig.setSsa(ssa);
        licenseConfig.setScanLicenseApiHostname(dcrResponse.getScanHostname());
        licenseConfig.setLicenseHardwareKey(dcrResponse.getHardwareId());
        OIDCClientSettings oidcClient = new OIDCClientSettings(dcrResponse.getOpHost(), dcrResponse.getClientId(), dcrResponse.getClientSecret());
        licenseConfig.setOidcClient(oidcClient);
        appConf.getMainSettings().setLicenseConfig(licenseConfig);
        entryManager.merge(appConf);

        AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
        LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();

        if (licenseConfiguration == null) {
            licenseConfiguration = new LicenseConfiguration();
        }

        licenseConfiguration.setScanAuthServerHostname(dcrResponse.getOpHost());
        licenseConfiguration.setScanApiClientId(dcrResponse.getClientId());
        licenseConfiguration.setScanApiClientSecret(dcrResponse.getClientSecret());
        licenseConfiguration.setHardwareId(dcrResponse.getHardwareId());
        licenseConfiguration.setScanApiHostname(dcrResponse.getScanHostname());
        auiConfiguration.setLicenseConfiguration(licenseConfiguration);
        auiConfigurationService.setAuiConfiguration(auiConfiguration);
    }

    /**
     * The function generates a token using client credentials and returns the token response.
     *
     * @param opHost       The `opHost` parameter represents the hostname or URL of the authorization server. It is used to
     *                     construct the URL for the token endpoint.
     * @param clientId     The `clientId` parameter is the unique identifier assigned to the client application by the
     *                     authorization server. It is used to identify the client when making requests to the server.
     * @param clientSecret The `clientSecret` parameter is a secret key that is used to authenticate the client application
     *                     when requesting an access token from the authorization server. It is typically provided by the authorization server
     *                     when registering the client application.
     * @return The method is returning a `io.jans.as.client.TokenResponse` object.
     */
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

    private Optional<GenericResponse> handleLicenseApiNotAccessible(Response response) {
        if (response.getStatus() == 404) {
            log.error("{}", LICENSE_APIS_404);
            return Optional.of(CommonUtils.createGenericResponse(false, response.getStatus(), LICENSE_APIS_404));
        }
        if (response.getStatus() == 503) {
            log.error("{}", LICENSE_APIS_503);
            return Optional.of(CommonUtils.createGenericResponse(false, response.getStatus(), LICENSE_APIS_503));
        }
        return Optional.empty();
    }

    private Optional<GenericResponse> handleMissingFieldsInResponse(JsonObject entity, String format, String... args) {
        StringBuffer missingFields = new StringBuffer("");
        for (String arg : args) {
            if (!entity.containsKey(arg)) {
                missingFields.append(missingFields.length() > 0 ? ", " : "");
                missingFields.append(arg);
            }
        }
        return missingFields.length() > 0 ?
                Optional.of(CommonUtils.createGenericResponse(false, 500, String.format(format, missingFields.toString()))) :
                Optional.empty();
    }
}