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
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
     * Validates the license configuration by checking for the presence of required parameters.
     *
     * This method retrieves the license configuration from the persistence layer and checks the following:
     * 1. The existence of the license hardware key.
     * 2. The presence and validity of OIDC client details (OP host, client ID, client secret).
     * 3. The presence of the SSA (Software Statement Assertion).
     * 4. The existence of the license key.
     * 5. The presence of the scan license API hostname.
     * 6. The last updated date of the license details. If missing, it triggers synchronization of OIDC client details.
     * 7. The time elapsed since the last license details update, comparing it with the configured synchronization interval.
     * If the interval has passed, it initiates synchronization of OIDC client details.
     *
     * @return A GenericResponse object indicating the validity of the license configuration.
     * - Returns a successful response (true, 200) if all required parameters are present and valid.
     * - Returns an error response (false, various error codes) if any required parameter is missing or invalid.
     */
    public GenericResponse validateLicenseConfiguration() {
        log.debug("Inside validateLicenseConfiguration: the method to validate license configuration.");
        AdminConf appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
        LicenseConfig licenseConfiguration = appConf.getMainSettings().getLicenseConfig();

        if (licenseConfiguration == null || Strings.isNullOrEmpty(licenseConfiguration.getLicenseHardwareKey())) {
            log.error(ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
        }
        if (Strings.isNullOrEmpty(licenseConfiguration.getOidcClient().getOpHost()) ||
                Strings.isNullOrEmpty(licenseConfiguration.getOidcClient().getClientId()) ||
                Strings.isNullOrEmpty(licenseConfiguration.getOidcClient().getClientSecret())) {
            log.error(ErrorResponse.LICENSE_OIDC_CLIENT_MISSING.getDescription());
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.LICENSE_OIDC_CLIENT_MISSING.getDescription());
        }
        if (Strings.isNullOrEmpty(licenseConfiguration.getSsa())) {
            log.error(ErrorResponse.LICENSE_SSA_MISSING.getDescription());
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.LICENSE_SSA_MISSING.getDescription());
        }
        if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseKey())) {
            log.error(ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
            return CommonUtils.createGenericResponse(false, 404, ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
        }
        if (Strings.isNullOrEmpty(licenseConfiguration.getScanLicenseApiHostname())) {
            log.error(ErrorResponse.SCAN_HOSTNAME_MISSING.getDescription());
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.SCAN_HOSTNAME_MISSING.getDescription());
        }
        if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseDetailsLastUpdatedOn())) {
            log.info(ErrorResponse.LICENSE_INFO_LAST_FETCHED_ON_ABSENT.getDescription());
            return syncLicenseOIDCClientDetails(licenseConfiguration);
        }
        long daysDiffOfLicenseDetailsLastUpdated = ChronoUnit.DAYS.between(LocalDate.now(), CommonUtils.convertStringToLocalDate(licenseConfiguration.getLicenseDetailsLastUpdatedOn()));
        long intervalForSyncLicenseDetailsInDays = licenseConfiguration.getIntervalForSyncLicenseDetailsInDays() == null ? AppConstants.LICENSE_DETAILS_SYNC_INTERVAL_IN_DAYS : licenseConfiguration.getIntervalForSyncLicenseDetailsInDays();
        log.debug("License details were last updated before {} days. The sync process will run after an interval of {} days.", daysDiffOfLicenseDetailsLastUpdated, intervalForSyncLicenseDetailsInDays);
        if (daysDiffOfLicenseDetailsLastUpdated > intervalForSyncLicenseDetailsInDays) {
            return syncLicenseOIDCClientDetails(licenseConfiguration);
        }
        return CommonUtils.createGenericResponse(true, 200, "No error in license configuration.");
    }

    /**
     * Synchronizes the OIDC client details used to access the License API on Agama Lab.
     *
     * This method attempts to synchronize the OIDC client details required to access the License API.
     * It first tries to generate an access token using the existing client credentials. If that fails,
     * it attempts to re-generate the client credentials using the provided SSA (Software Statement Assertion)
     * and persists the newly generated client details.
     *
     * @param licenseConfig The license configuration containing OIDC client details and SSA.
     * @return A GenericResponse object indicating the success or failure of the synchronization.
     * - Returns a successful response (true, 200) if the OIDC client details are successfully synchronized.
     * - Returns an error response (false, 500) if token generation fails, DCR (Dynamic Client Registration) fails,
     * or saving the client details to persistence fails.
     */
    private GenericResponse syncLicenseOIDCClientDetails(LicenseConfig licenseConfig) {
        log.debug("Inside syncLicenseOIDCClientDetails: the method to sync OIDC client details used to access License API on Agama Lab.");

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
    /**
     * Checks if the license details are valid and up-to-date.
     *
     * This method retrieves the license configuration from the persistence layer and performs several checks:
     * 1. It verifies the presence of essential license configuration parameters like hardware key, license key, and scan API hostname.
     * 2. It checks if the license validity and last update dates are available. If not, it triggers a synchronization with Agama Lab.
     * 3. It calculates the time elapsed since the last license details update and compares it with the configured synchronization interval.
     * If the interval has passed, it initiates a synchronization.
     * 4. It determines if the license has expired by comparing the current date with the license validity date.
     * If expired, it triggers a synchronization.
     * 5. If all checks pass, it constructs a JSON response containing custom attributes (specifically, the MAU threshold)
     * and returns a success response.
     *
     * @return A GenericResponse object indicating the validity of the license and containing relevant license details.
     * - Returns a successful response (true, 200) with license details if the license is valid and up-to-date.
     * - Returns an error response (false, various error codes) if any configuration is missing, the license is invalid,
     * or an exception occurs during the process.
     */
    public GenericResponse checkLicense() {
        log.debug("Inside checkLicense: the method to check if License details are valid.");
        try {
            AdminConf appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
            LicenseConfig licenseConfiguration = appConf.getMainSettings().getLicenseConfig();

            if (licenseConfiguration == null || Strings.isNullOrEmpty(licenseConfiguration.getLicenseHardwareKey())) {
                log.error(ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseKey())) {
                log.info(ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
                return CommonUtils.createGenericResponse(false, 404, ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getScanLicenseApiHostname())) {
                log.error(ErrorResponse.SCAN_HOSTNAME_MISSING.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.SCAN_HOSTNAME_MISSING.getDescription());
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseValidUpto())) {
                log.info(ErrorResponse.LICENSE_EXPIRY_DATE_NOT_PRESENT.getDescription());
                return syncLicenseDetailsFromAgamaLab();
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseDetailsLastUpdatedOn())) {
                log.info(ErrorResponse.LICENSE_INFO_LAST_FETCHED_ON_ABSENT.getDescription());
                return syncLicenseDetailsFromAgamaLab();
            }
            long daysDiffOfLicenseDetailsLastUpdated = ChronoUnit.DAYS.between(LocalDate.now(), CommonUtils.convertStringToLocalDate(licenseConfiguration.getLicenseDetailsLastUpdatedOn()));
            long intervalForSyncLicenseDetailsInDays = licenseConfiguration.getIntervalForSyncLicenseDetailsInDays() == null ? AppConstants.LICENSE_DETAILS_SYNC_INTERVAL_IN_DAYS : licenseConfiguration.getIntervalForSyncLicenseDetailsInDays();
            log.debug("License details were last updated before {} days. The sync process will run after an interval of {} days.", daysDiffOfLicenseDetailsLastUpdated, intervalForSyncLicenseDetailsInDays);
            if (daysDiffOfLicenseDetailsLastUpdated > intervalForSyncLicenseDetailsInDays) {
                return syncLicenseDetailsFromAgamaLab();
            }
            //calculate if license is expired
            long daysDiffOfLicenseValidity = ChronoUnit.DAYS.between(LocalDate.now(), CommonUtils.convertStringToLocalDate(licenseConfiguration.getLicenseValidUpto()));
            log.debug("License will expire after {} days", daysDiffOfLicenseValidity);
            if (daysDiffOfLicenseValidity < 0) {
                return syncLicenseDetailsFromAgamaLab();
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonArray customAttributes = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("name", "mau_threshold")
                            .add("value", licenseConfiguration.getLicenseMAUThreshold()))
                    .build();
            return CommonUtils.createGenericResponse(true, 200, "Valid license present.", mapper.readTree(customAttributes.toString()));
        } catch (Exception e) {
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.CHECK_LICENSE_ERROR.getDescription());
        }
    }

    /**
     * Synchronizes license details from Agama Lab by checking the license status and updating local configurations.
     *
     * This method retrieves the license configuration, checks the license status with Agama Lab's API,
     * and updates the local license configuration with the retrieved details. It performs the following steps:
     * 1. Retrieves the AUI and license configurations.
     * 2. Validates the presence of hardware ID, scan API hostname, and license key.
     * 3. Constructs the URL for checking the license status ("/isActive").
     * 4. Generates an access token for accessing the Agama Lab API.
     * 5. Sends a POST request to the Agama Lab API with the license key and hardware ID.
     * 6. Parses the response, checks for license activity and expiry, and handles errors.
     * 7. Updates the local license configuration with the details from the response.
     * 8. Persists the updated license configuration in the persistence layer.
     * 9. Returns a success response with license details or an error response with error messages.
     *
     * @return A GenericResponse object indicating the success or failure of the synchronization.
     * - Returns a successful response (true, 200) with license details if the license is valid and synchronization is successful.
     * - Returns an error response (false, various error codes) if any configuration is missing, the license is invalid,
     * API access fails, or an exception occurs during the process.
     */
    private GenericResponse syncLicenseDetailsFromAgamaLab() {
        log.info("Inside syncLicenseDetailsFromAgamaLab: the method to sync license details from Agama lab");
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
            String checkLicenseUrl = (new StringBuffer())
                    .append(formatApiUrl(licenseConfiguration.getScanApiHostname(), "/isActive"))
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

                //save in license configuration
                setToLicenseConfiguration(entity, licenseConfiguration);
                //save license spring credentials
                AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
                adminConf.getMainSettings().getLicenseConfig().setLicenseKey(licenseConfiguration.getLicenseKey());
                adminConf.getMainSettings().getLicenseConfig().setLicenseExpired(licenseConfiguration.getLicenseExpired());
                adminConf.getMainSettings().getLicenseConfig().setLicenseActive(licenseConfiguration.getLicenseActive());
                adminConf.getMainSettings().getLicenseConfig().setLicenseType(licenseConfiguration.getLicenseType());
                adminConf.getMainSettings().getLicenseConfig().setLicenseDetailsLastUpdatedOn(licenseConfiguration.getLicenseDetailsLastUpdatedOn());
                adminConf.getMainSettings().getLicenseConfig().setLicenseValidUpto(licenseConfiguration.getLicenseValidUpto());
                adminConf.getMainSettings().getLicenseConfig().setProductName(licenseConfiguration.getProductName());
                adminConf.getMainSettings().getLicenseConfig().setProductCode(licenseConfiguration.getProductCode());
                adminConf.getMainSettings().getLicenseConfig().setCompanyName(licenseConfiguration.getCompanyName());
                adminConf.getMainSettings().getLicenseConfig().setCustomerEmail(licenseConfiguration.getCustomerEmail());
                adminConf.getMainSettings().getLicenseConfig().setCustomerFirstName(licenseConfiguration.getCustomerFirstName());
                adminConf.getMainSettings().getLicenseConfig().setCustomerLastName(licenseConfiguration.getCustomerLastName());
                adminConf.getMainSettings().getLicenseConfig().setLicenseMAUThreshold(licenseConfiguration.getLicenseMAUThreshold());
                entryManager.merge(adminConf);
                auiConfiguration.setLicenseConfiguration(licenseConfiguration);
                auiConfigurationService.setAuiConfiguration(auiConfiguration);

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

            String retriveLicenseUrl = (new StringBuffer())
                    .append(formatApiUrl(licenseConfiguration.getScanApiHostname(), "/retrieve?org_id=") + licenseConfiguration.getHardwareId())
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
            JsonNode jsonNode = mapper.readValue(jsonData, JsonNode.class);

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
            String activateLicenseUrl = (new StringBuffer())
                    .append(formatApiUrl(licenseConfiguration.getScanApiHostname(), "/activate"))
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
                //save in license configuration
                setToLicenseConfiguration(entity, licenseConfiguration);
                //save license spring credentials
                AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);
                adminConf.getMainSettings().getLicenseConfig().setLicenseKey(licenseConfiguration.getLicenseKey());
                adminConf.getMainSettings().getLicenseConfig().setLicenseExpired(licenseConfiguration.getLicenseExpired());
                adminConf.getMainSettings().getLicenseConfig().setLicenseActive(licenseConfiguration.getLicenseActive());
                adminConf.getMainSettings().getLicenseConfig().setLicenseType(licenseConfiguration.getLicenseType());
                adminConf.getMainSettings().getLicenseConfig().setLicenseDetailsLastUpdatedOn(licenseConfiguration.getLicenseDetailsLastUpdatedOn());
                adminConf.getMainSettings().getLicenseConfig().setLicenseValidUpto(licenseConfiguration.getLicenseValidUpto());
                adminConf.getMainSettings().getLicenseConfig().setProductName(licenseConfiguration.getProductName());
                adminConf.getMainSettings().getLicenseConfig().setProductCode(licenseConfiguration.getProductCode());
                adminConf.getMainSettings().getLicenseConfig().setCompanyName(licenseConfiguration.getCompanyName());
                adminConf.getMainSettings().getLicenseConfig().setCustomerEmail(licenseConfiguration.getCustomerEmail());
                adminConf.getMainSettings().getLicenseConfig().setCustomerFirstName(licenseConfiguration.getCustomerFirstName());
                adminConf.getMainSettings().getLicenseConfig().setCustomerLastName(licenseConfiguration.getCustomerLastName());
                adminConf.getMainSettings().getLicenseConfig().setLicenseMAUThreshold(licenseConfiguration.getLicenseMAUThreshold());
                entryManager.merge(adminConf);
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
     * Sets the license configuration details from a JSON object entity.
     *
     * This method populates the `LicenseConfiguration` object with data extracted from the provided `JsonObject`.
     * It handles various license details, including license key, type, validity, product information, customer details,
     * MAU threshold, and license status (expired and active).
     *
     * @param entity             The `JsonObject` containing license details.
     * @param licenseConfiguration The `LicenseConfiguration` object to be populated.
     * @throws RuntimeException If an error occurs during the process of setting the license configuration.
     */
    private void setToLicenseConfiguration(JsonObject entity, LicenseConfiguration licenseConfiguration) throws RuntimeException {
        try {
            log.debug("Inside setToLicenseConfiguration: the method to set licence configuration");
            licenseConfiguration.setLicenseKey(entity.getString("license_key"));
            licenseConfiguration.setLicenseType(entity.getString("license_type"));
            licenseConfiguration.setLicenseDetailsLastUpdatedOn(CommonUtils.convertLocalDateToString(LocalDate.now()));
            licenseConfiguration.setLicenseValidUpto(CommonUtils.convertIsoToDateString(entity.getString("validity_period")));
            licenseConfiguration.setProductName(entity.getJsonObject("product_details").getString("product_name"));
            licenseConfiguration.setProductCode(entity.getJsonObject("product_details").getString("short_code"));
            JsonObject customer = entity.getJsonObject("customer");
            if (customer != null) {
                if (customer.get("company_name") != null) {
                    licenseConfiguration.setCompanyName(customer.getString("company_name"));
                }
                if (customer.get("email") != null) {
                    licenseConfiguration.setCustomerEmail(customer.getString("email"));
                }
                if (customer.get("first_name") != null) {
                    licenseConfiguration.setCustomerFirstName(customer.getString("first_name"));
                }
                if (customer.get("last_name") != null) {
                    licenseConfiguration.setCustomerLastName(customer.getString("last_name"));
                }
            }
            licenseConfiguration.setLicenseDetailsLastUpdatedOn(CommonUtils.convertLocalDateToString(LocalDate.now()));
            JsonArray customFields = entity.getJsonArray("custom_fields");
            Long mauThresholdValue = null;
            for (JsonObject obj : customFields.getValuesAs(JsonObject.class)) {
                if ("mau_threshold".equals(obj.getString("name"))) {
                    log.info(obj.getString("name"));
                    log.info(obj.getString("value"));
                    mauThresholdValue = Long.valueOf(!Strings.isNullOrEmpty(obj.getString("value")) ? obj.getString("value") : "0");
                    break; // Exit loop once found
                }
            }
            licenseConfiguration.setLicenseMAUThreshold(mauThresholdValue);
            try {
                licenseConfiguration.setLicenseExpired(entity.getBoolean("is_expired"));
                licenseConfiguration.setLicenseActive(entity.getBoolean("license_active"));
            } catch(Exception ex) {
                //when the method call from activateLicense method where is_expired and license_active fields are absent in response.
                licenseConfiguration.setLicenseExpired(false);
                licenseConfiguration.setLicenseActive(entity.getBoolean("active"));
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error in setting licence configuration from persistence", ex);
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
            String trialLicenseUrl = (new StringBuffer())
                    .append(formatApiUrl(licenseConfiguration.getScanApiHostname(), "/trial"))
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
            JsonNode jsonNode = objectMapper.readValue(jsonData, JsonNode.class);
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
            if (licenseConfiguration == null || Strings.isNullOrEmpty(licenseConfiguration.getHardwareId())) {
                log.error(ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
                licenseResponse.setLicenseEnabled(false);
                return licenseResponse;
            }

            if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseKey())) {
                log.info(ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
                licenseResponse.setLicenseEnabled(false);
                return licenseResponse;
            }

            if (!licenseConfiguration.getLicenseActive()) {
                log.info(ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
                licenseResponse.setLicenseEnabled(false);
                return licenseResponse;
            }

            log.debug("Active license for admin-ui found : {}", licenseConfiguration.getProductName());
            licenseResponse.setLicenseEnabled(licenseConfiguration.getLicenseActive());
            licenseResponse.setProductName(licenseConfiguration.getProductName());
            licenseResponse.setProductCode(licenseConfiguration.getProductCode());
            licenseResponse.setLicenseType(licenseConfiguration.getLicenseType());
            licenseResponse.setLicenseMAUThreshold(licenseConfiguration.getLicenseMAUThreshold());
            licenseResponse.setLicenseKey(licenseConfiguration.getLicenseKey());
            licenseResponse.setValidityPeriod(licenseConfiguration.getLicenseValidUpto());
            licenseResponse.setLicenseActive(licenseConfiguration.getLicenseActive());
            licenseResponse.setLicenseExpired(licenseConfiguration.getLicenseExpired());
            licenseResponse.setCompanyName(licenseConfiguration.getCompanyName());
            licenseResponse.setCustomerEmail(licenseConfiguration.getCustomerEmail());
            licenseResponse.setCustomerFirstName(licenseConfiguration.getCustomerFirstName());
            licenseResponse.setCustomerLastName(licenseConfiguration.getCustomerLastName());
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

    /**
     * Handles API URL format
     */
    private String formatApiUrl(String scanApiHostname, String endpoint) {
        return StringUtils.removeEnd(scanApiHostname, "/") + "/scan/license" + endpoint;
    }
}