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
import io.jans.ca.plugin.adminui.rest.license.LicenseResource;
import io.jans.ca.plugin.adminui.service.BaseService;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.model.adminui.AUIConfiguration;
import io.jans.configapi.core.model.adminui.LicenseConfiguration;
import io.jans.model.net.HttpServiceResponse;
import io.jans.orm.PersistenceEntryManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

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
     * <p>
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
        log.info("Inside validateLicenseConfiguration: the method to validate license configuration.");
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
        long daysDiffOfLicenseDetailsLastUpdated = ChronoUnit.DAYS.between(CommonUtils.convertStringToLocalDate(licenseConfiguration.getLicenseDetailsLastUpdatedOn()), LocalDate.now());
        long intervalForSyncLicenseDetailsInDays = licenseConfiguration.getIntervalForSyncLicenseDetailsInDays() == null ? AppConstants.LICENSE_DETAILS_SYNC_INTERVAL_IN_DAYS : licenseConfiguration.getIntervalForSyncLicenseDetailsInDays();
        log.info("License details were last updated before {} days. The sync process will run after an interval of {} days.", daysDiffOfLicenseDetailsLastUpdated, intervalForSyncLicenseDetailsInDays);
        if (daysDiffOfLicenseDetailsLastUpdated > intervalForSyncLicenseDetailsInDays) {
            return syncLicenseOIDCClientDetails(licenseConfiguration);
        }
        // Check if the 'licenseValidUpto' field is not null or empty
        if (!Strings.isNullOrEmpty(licenseConfiguration.getLicenseValidUpto())) {
            // Calculate the number of days between today and the license expiry date
            long daysDiffOfLicenseValidity = ChronoUnit.DAYS.between(LocalDate.now(), CommonUtils.convertStringToLocalDate(licenseConfiguration.getLicenseValidUpto()));
            log.info("License will expire after {} days", daysDiffOfLicenseValidity);
            // If the license has already expired (daysDiffOfLicenseValidity < 0),
            // sync the license OIDC client details to update or renew the license
            if (daysDiffOfLicenseValidity < 0) {
                return syncLicenseOIDCClientDetails(licenseConfiguration);
            }
        }

        return CommonUtils.createGenericResponse(true, 200, "No error in license configuration.");
    }

    /**
         * Ensures the OIDC client credentials used to call the License API are valid and persisted.
         *
         * If the current client credentials fail to produce an access token, attempts Dynamic Client Registration
         * (DCR) using the stored SSA to obtain new client credentials and saves them to persistence.
         *
         * @param licenseConfig the license configuration containing the OIDC client details and SSA
         * @return a GenericResponse: `true` with status 200 when credentials are successfully synchronized; `false` with an error status and message otherwise
         */
    private GenericResponse syncLicenseOIDCClientDetails(LicenseConfig licenseConfig) {
        log.info("Inside syncLicenseOIDCClientDetails: the method to sync OIDC client details used to access License API on Agama Lab.");

        log.info("Requesting for access token from {}", licenseConfig.getOidcClient().getOpHost());
        io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfig.getOidcClient().getOpHost(), licenseConfig.getOidcClient().getClientId(), licenseConfig.getOidcClient().getClientSecret());

        if (tokenResponse == null || Strings.isNullOrEmpty(tokenResponse.getAccessToken())) {
            log.info("Unable to get access token from {}", licenseConfig.getOidcClient().getOpHost());
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
            log.info("Requesting again for access token from {}", licenseConfig.getOidcClient().getOpHost());
            tokenResponse = generateToken(licenseConfig.getOidcClient().getOpHost(), licenseConfig.getOidcClient().getClientId(), licenseConfig.getOidcClient().getClientSecret());

            if (tokenResponse == null) {
                log.info("Unable to get access token from {}", licenseConfig.getOidcClient().getOpHost());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }
        }
        return CommonUtils.createGenericResponse(true, 200, "No error in license configuration.");
    }

    /**
     * Deletes the license-related configuration in both the persistent and in-memory Admin UI configuration.
     */
    public GenericResponse deleteLicenseConfiguration() {
        log.info("Deleting Admin UI license configuration.");

        // Fetch current persisted Admin UI configuration
        AdminConf appConf = entryManager.find(AdminConf.class, AppConstants.ADMIN_UI_CONFIG_DN);

        // Reset license config in persistent storage
        appConf.getMainSettings().setLicenseConfig(new LicenseConfig());
        entryManager.merge(appConf); // Persist changes

        // Fetch in-memory Admin UI configuration
        AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

        // Reset license config in in-memory configuration
        auiConfiguration.setLicenseConfiguration(new LicenseConfiguration());
        auiConfigurationService.setAuiConfiguration(auiConfiguration); // Apply changes

        // Return a standard response
        return CommonUtils.createGenericResponse(true, 200, "Admin UI license configuration reset successfully.");
    }

    /**
     * Verify that the stored license configuration is present, current, and not expired.
     *
     * If the configuration is missing, incomplete, expired, or due for refresh, the method initiates synchronization
     * with the license service and returns the corresponding error response. When the license is valid and up-to-date,
     * the response contains custom attributes (including the `mau_threshold`) describing license limits.
     *
     * @return a `GenericResponse` with `true` and HTTP 200 containing license custom attributes (including `mau_threshold`)
     *         when the license is valid; otherwise `false` with an appropriate HTTP error code and diagnostic message.
     */
    public GenericResponse checkLicense() {
        log.info("Inside checkLicense: the method to check if License details are valid.");
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();

            if (licenseConfiguration == null || Strings.isNullOrEmpty(licenseConfiguration.getHardwareId())) {
                log.error(ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.LICENSE_CONFIG_ABSENT.getDescription());
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseKey())) {
                log.info(ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
                return CommonUtils.createGenericResponse(false, 404, ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getScanApiHostname())) {
                log.error(ErrorResponse.SCAN_HOSTNAME_MISSING.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.SCAN_HOSTNAME_MISSING.getDescription());
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseValidUpto())) {
                log.info(ErrorResponse.LICENSE_EXPIRY_DATE_NOT_PRESENT.getDescription());
                return syncLicenseDetailsFromAgamaLab(auiConfiguration);
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseDetailsLastUpdatedOn())) {
                log.info(ErrorResponse.LICENSE_INFO_LAST_FETCHED_ON_ABSENT.getDescription());
                return syncLicenseDetailsFromAgamaLab(auiConfiguration);
            }
            long daysDiffOfLicenseDetailsLastUpdated = ChronoUnit.DAYS.between(CommonUtils.convertStringToLocalDate(licenseConfiguration.getLicenseDetailsLastUpdatedOn()), LocalDate.now());
            long intervalForSyncLicenseDetailsInDays = licenseConfiguration.getIntervalForSyncLicenseDetailsInDays() == null ? AppConstants.LICENSE_DETAILS_SYNC_INTERVAL_IN_DAYS : licenseConfiguration.getIntervalForSyncLicenseDetailsInDays();
            log.info("License details were last updated before {} days. The sync process will run after an interval of {} days.", daysDiffOfLicenseDetailsLastUpdated, intervalForSyncLicenseDetailsInDays);
            if (daysDiffOfLicenseDetailsLastUpdated > intervalForSyncLicenseDetailsInDays) {
                return syncLicenseDetailsFromAgamaLab(auiConfiguration);
            }
            //calculate if license is expired
            long daysDiffOfLicenseValidity = ChronoUnit.DAYS.between(LocalDate.now(), CommonUtils.convertStringToLocalDate(licenseConfiguration.getLicenseValidUpto()));
            log.info("License will expire after {} days", daysDiffOfLicenseValidity);
            if (daysDiffOfLicenseValidity < 0) {
                return syncLicenseDetailsFromAgamaLab(auiConfiguration);
            }
            if (licenseConfiguration.getLicenseMAUThreshold() == null) {
                log.error(ErrorResponse.MAU_IS_NULL.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.MAU_IS_NULL.getDescription());
            }
            ObjectMapper mapper = new ObjectMapper();
            JSONArray customAttributes = new JSONArray();
            JSONObject mauDetails = new JSONObject();
            mauDetails.put("name", "mau_threshold");
            mauDetails.put("value", licenseConfiguration.getLicenseMAUThreshold());

            customAttributes.put(mauDetails);

            return CommonUtils.createGenericResponse(true, 200, "Valid license present.", mapper.readTree(customAttributes.toString()));
        } catch (Exception e) {
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.CHECK_LICENSE_ERROR.getDescription());
        }
    }

    /**
     * Synchronizes license details with Agama Lab and updates the local license configuration.
     *
     * Checks the license status using the Agama Lab "/isActive" endpoint, persists updated license
     * fields to the persistence layer and in-memory AUI configuration when the license is valid,
     * and returns a result describing the outcome.
     *
     * @param auiConfiguration the current AUI configuration containing the license configuration to check and update
     * @return a GenericResponse indicating the result; on success contains license details (custom fields) and status 200,
     *         on failure contains an error code and message describing the problem (e.g., missing configuration, license not present,
     *         expired, token generation or API access errors)
     */
    private GenericResponse syncLicenseDetailsFromAgamaLab(AUIConfiguration auiConfiguration) {
        log.info("Inside syncLicenseDetailsFromAgamaLab: the method to sync license details from Agama lab");
        Integer httpStatus = null;
        try {
            LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();

            //check license-key
            String checkLicenseUrl = formatApiUrl(licenseConfiguration.getScanApiHostname(), "/isActive");

            io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfiguration.getScanAuthServerHostname(), licenseConfiguration.getScanApiClientId(), licenseConfiguration.getScanApiClientSecret());
            if (tokenResponse == null) {
                log.info(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }

            CloseableHttpClient httpClient = httpService.createHttpsClientWithTlsPolicy(TLS_ENABLED_PROTOCOLS,
                    TLS_ALLOWED_CIPHER_SUITES);

            Map<String, String> body = new HashMap<>();
            body.put(LICENSE_KEY, licenseConfiguration.getLicenseKey());
            body.put(HARDWARE_ID, licenseConfiguration.getHardwareId());

            HttpServiceResponse httpServiceResponse = httpService
                    .executePost(httpClient, checkLicenseUrl, tokenResponse.getAccessToken(), null, mapper.writeValueAsString(body), ContentType.APPLICATION_JSON,
                            BEARER);
            String jsonString = null;
            try {
                if (httpServiceResponse == null) {
                    log.error("HTTP request failed, no response received");
                    return CommonUtils.createGenericResponse(false, 500, "HTTP request failed");
                }
                if (httpServiceResponse.getHttpResponse() != null
                        && httpServiceResponse.getHttpResponse().getStatusLine() != null) {

                    logHttpResponse(checkLicenseUrl, httpServiceResponse);
                    HttpEntity httpEntity = httpServiceResponse.getHttpResponse().getEntity();
                    httpStatus = httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode();
                    if (httpStatus == 200 && httpEntity != null) {
                        jsonString = httpService.getContent(httpEntity);
                        JsonNode entityNode = mapper.readTree(jsonString);
                        JSONObject entity = new JSONObject(entityNode.toString());

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
                        if (adminConf.getMainSettings().getLicenseConfig().getIntervalForSyncLicenseDetailsInDays() == null) {
                            adminConf.getMainSettings().getLicenseConfig().setIntervalForSyncLicenseDetailsInDays((long) AppConstants.LICENSE_DETAILS_SYNC_INTERVAL_IN_DAYS);
                        }
                        entryManager.merge(adminConf);
                        auiConfiguration.setLicenseConfiguration(licenseConfiguration);
                        auiConfigurationService.setAuiConfiguration(auiConfiguration);

                        return CommonUtils.createGenericResponse(true, 200, "Valid license present.",
                                mapper.readTree(entity.getJSONArray("custom_fields").toString()));

                    }
                    //getting error
                    jsonString = httpService.getContent(httpEntity);
                    JsonNode jsonNode = mapper.readValue(jsonString, JsonNode.class);

                    if (!Strings.isNullOrEmpty(jsonNode.get(MESSAGE).textValue())) {
                        log.error("{}: {}", LICENSE_ISACTIVE_ERROR_RESPONSE, jsonString);
                        return CommonUtils.createGenericResponse(false, jsonNode.get(CODE).intValue(), jsonNode.get(MESSAGE).textValue());
                    }
                }
            } finally {
                if (httpServiceResponse != null) {
                    httpServiceResponse.closeConnection(); // Returns connection to pool
                }
            }
            log.error("{}: {}", LICENSE_ISACTIVE_ERROR_RESPONSE, jsonString);
            return CommonUtils.createGenericResponse(false, 404, ErrorResponse.LICENSE_NOT_PRESENT.getDescription());
        } catch (Exception e) {
            Optional<GenericResponse> genericResOptional = handleLicenseApiNotAccessible(httpStatus);
            if (genericResOptional.isPresent()) {
                return genericResOptional.get();
            }
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.CHECK_LICENSE_ERROR.getDescription());
        }
    }

    /**
     * Retrieve license details from the configured License API.
     *
     * <p>Validates that the local license configuration contains a hardware ID and scan API hostname, obtains
     * an access token, and queries the remote retrieve endpoint. On success returns a response containing
     * `licenseKey` and `mauThreshold` in the response payload.</p>
     *
     * @return GenericResponse containing `licenseKey` and `mauThreshold` on success (HTTP 200); otherwise a
     *         GenericResponse with an appropriate error code and message (for example, 402 for payment required,
     *         404/503 when the license API is inaccessible, or 500 for other server-side errors).
     */
    public GenericResponse retrieveLicense() {
        log.info("Inside retrieveLicense method...");
        Integer httpStatus = null;
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

            String retriveLicenseUrl = formatApiUrl(licenseConfiguration.getScanApiHostname(), "/retrieve?org_id=") + licenseConfiguration.getHardwareId();

            // Build the HttpClient
            CloseableHttpClient httpClient = httpService.createHttpsClientWithTlsPolicy(TLS_ENABLED_PROTOCOLS,
                    TLS_ALLOWED_CIPHER_SUITES);

            Map<String, String> headers = new HashMap<>();
            headers.put(AppConstants.AUTHORIZATION, BEARER + tokenResponse.getAccessToken());

            HttpServiceResponse httpServiceResponse = httpService
                    .executeGet(httpClient,
                            retriveLicenseUrl,
                            headers,
                            null);
            String jsonString = null;
            try {
                if (httpServiceResponse.getHttpResponse() != null
                        && httpServiceResponse.getHttpResponse().getStatusLine() != null) {

                    logHttpResponse(retriveLicenseUrl, httpServiceResponse);
                    HttpEntity httpEntity = httpServiceResponse.getHttpResponse().getEntity();
                    httpStatus = httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode();
                    if (httpStatus == 200 && httpEntity != null) {
                        jsonString = httpService.getContent(httpEntity);
                        JsonNode entityNode = mapper.readTree(jsonString);
                        JSONObject entity = new JSONObject(entityNode.toString());

                        Optional<GenericResponse> genericResOptional = handleMissingFieldsInResponse(entity, ErrorResponse.LICENSE_DATA_MISSING.getDescription(), "licenseKey", "mauThreshold");
                        if (genericResOptional.isPresent()) {
                            return genericResOptional.get();
                        }
                        ObjectNode jsonNode = mapper.createObjectNode();
                        jsonNode.put("licenseKey", entity.getString("licenseKey"));
                        jsonNode.put("mauThreshold", entity.getInt("mauThreshold"));
                        return CommonUtils.createGenericResponse(true, 200, "Valid license present.", jsonNode);
                    }
                    //getting error
                    jsonString = httpService.getContent(httpEntity);
                    JsonNode jsonNode = mapper.readValue(jsonString, JsonNode.class);
                    if (httpStatus == 402) {
                        log.error("Payment Required: 402");
                        return CommonUtils.createGenericResponse(false, httpStatus, "Payment Required. Subscribe Admin UI license on Agama Lab.");
                    }
                    if (!Strings.isNullOrEmpty(jsonNode.get(MESSAGE).textValue())) {
                        log.error("{}: {}", LICENSE_RETRIEVE_ERROR_RESPONSE, jsonString);
                        return CommonUtils.createGenericResponse(false, jsonNode.get(CODE).intValue(), jsonNode.get(MESSAGE).textValue());
                    }
                    log.error("{}: {}", LICENSE_RETRIEVE_ERROR_RESPONSE, jsonString);
                }
            } finally {
                if (httpServiceResponse != null) {
                    httpServiceResponse.closeConnection(); // Returns connection to pool
                }
            }
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.RETRIEVE_LICENSE_ERROR.getDescription());
        } catch (Exception e) {
            Optional<GenericResponse> genericResOptional = handleLicenseApiNotAccessible(httpStatus);
            if (genericResOptional.isPresent()) {
                return genericResOptional.get();
            }
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.RETRIEVE_LICENSE_ERROR.getDescription());
        }
    }

    /**
     * Activate a license with the configured License API and persist updated license details.
     *
     * @param licenseRequest request containing the license key to activate
     * @return a GenericResponse indicating success or failure; on success (HTTP 200) the response contains a message and the license's updated custom_fields, on failure the response contains an error message and an appropriate HTTP status code
     */
    public GenericResponse activateLicense(LicenseRequest licenseRequest) {
        Integer httpStatus = null;
        //check is license is already active
        GenericResponse licenseApiResponse = checkLicense();
        if (licenseApiResponse.isSuccess()) {
            return CommonUtils.createGenericResponse(true, 200, ErrorResponse.LICENSE_ALREADY_ACTIVE.getDescription());
        }
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();

            log.debug("Trying to activate License.");
            String activateLicenseUrl = formatApiUrl(licenseConfiguration.getScanApiHostname(), "/activate");

            io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfiguration.getScanAuthServerHostname(), licenseConfiguration.getScanApiClientId(), licenseConfiguration.getScanApiClientSecret());
            if (tokenResponse == null) {
                log.info(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }

            CloseableHttpClient httpClient = httpService.createHttpsClientWithTlsPolicy(TLS_ENABLED_PROTOCOLS,
                    TLS_ALLOWED_CIPHER_SUITES);

            Map<String, String> body = new HashMap<>();
            body.put(LICENSE_KEY, licenseRequest.getLicenseKey());
            body.put(HARDWARE_ID, licenseConfiguration.getHardwareId());

            HttpServiceResponse httpServiceResponse = httpService
                    .executePost(httpClient, activateLicenseUrl, tokenResponse.getAccessToken(), null, mapper.writeValueAsString(body), ContentType.APPLICATION_JSON,
                            BEARER);

            String jsonString = null;
            try {
                if (httpServiceResponse.getHttpResponse() != null
                        && httpServiceResponse.getHttpResponse().getStatusLine() != null) {
                    logHttpResponse(activateLicenseUrl, httpServiceResponse);
                    ObjectMapper mapper = new ObjectMapper();
                    HttpEntity httpEntity = httpServiceResponse.getHttpResponse().getEntity();
                    httpStatus = httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode();
                    if (httpStatus == 200 && httpEntity != null) {
                        jsonString = httpService.getContent(httpEntity);
                        JsonNode entityNode = mapper.readTree(jsonString);
                        JSONObject entity = new JSONObject(entityNode.toString());

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
                                , mapper.readTree(entity.getJSONArray("custom_fields").toString()));
                    }
                    //getting error
                    jsonString = httpService.getContent(httpEntity);
                    JsonNode jsonNode = mapper.readValue(jsonString, JsonNode.class);

                    if (!Strings.isNullOrEmpty(jsonNode.get(MESSAGE).textValue())) {
                        log.error("license Activation error response: {}", jsonString);
                        log.error("{}: {}", LICENSE_ACTIVATE_ERROR_RESPONSE, jsonString);
                        return CommonUtils.createGenericResponse(false, jsonNode.get(CODE).intValue(), jsonNode.get(MESSAGE).textValue());
                    }
                }
            } finally {
                if (httpServiceResponse != null) {
                    httpServiceResponse.closeConnection(); // Returns connection to pool
                }
            }
            log.error("license Activation error response: {}", jsonString);
            log.error("{}: {}", LICENSE_ACTIVATE_ERROR_RESPONSE, jsonString);
            return CommonUtils.createGenericResponse(false, 500, "License is not activated.");
        } catch (Exception e) {
            Optional<GenericResponse> genericResOptional = handleLicenseApiNotAccessible(httpStatus);
            if (genericResOptional.isPresent()) {
                return genericResOptional.get();
            }
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return CommonUtils.createGenericResponse(false, 500, ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription());
        }
    }

    /**
     * Populate the given LicenseConfiguration with values extracted from the provided JSON license entity.
     *
     * <p>The method reads fields such as license key, license type, validity period, product information,
     * customer details, and the MAU threshold (extracted from a custom field named "mau_threshold"). It also
     * sets the configuration's last-updated timestamp to the current date and updates license status flags.
     *
     * @param entity               JSONObject containing license data (expected keys include: "license_key",
     *                             "license_type", "validity_period", "product_details", "customer", and "custom_fields").
     * @param licenseConfiguration The LicenseConfiguration instance to populate.
     * @throws RuntimeException if required data (for example, the "custom_fields" array) is missing or any parsing error occurs.
     */
    private void setToLicenseConfiguration(JSONObject entity, LicenseConfiguration licenseConfiguration) throws RuntimeException {
        try {
            log.info("Inside setToLicenseConfiguration: the method to set licence configuration");
            licenseConfiguration.setLicenseKey(entity.getString("license_key"));
            licenseConfiguration.setLicenseType(entity.getString("license_type"));
            licenseConfiguration.setLicenseDetailsLastUpdatedOn(CommonUtils.convertLocalDateToString(LocalDate.now()));
            licenseConfiguration.setLicenseValidUpto(CommonUtils.convertIsoToDateString(entity.getString("validity_period")));
            licenseConfiguration.setProductName(entity.getJSONObject("product_details").getString("product_name"));
            licenseConfiguration.setProductCode(entity.getJSONObject("product_details").getString("short_code"));
            JSONObject customer = entity.getJSONObject("customer");
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
            if (!entity.has("custom_fields")) {
                throw new RuntimeException("Error in setting licence configuration from persistence: custom-fields containing MAU data is missing");
            }
            JSONArray customFields = entity.getJSONArray("custom_fields");
            Long mauThresholdValue = null;
            for (Object obj : customFields) {
                JSONObject jsonObject = (JSONObject) obj;
                if ("mau_threshold".equals(jsonObject.getString("name"))) {
                    if (!log.isDebugEnabled()) {
                        log.debug(jsonObject.getString("name"));
                        log.debug(jsonObject.getString("value"));
                    }
                    mauThresholdValue = Long.valueOf(!Strings.isNullOrEmpty(jsonObject.getString("value")) ? jsonObject.getString("value") : "0");
                    break; // Exit loop once found
                }
            }
            licenseConfiguration.setLicenseMAUThreshold(mauThresholdValue);
            try {
                licenseConfiguration.setLicenseExpired(entity.getBoolean("is_expired"));
                licenseConfiguration.setLicenseActive(entity.getBoolean("license_active"));
            } catch (Exception ex) {
                //when the method call from activateLicense method where is_expired and license_active fields are absent in response.
                licenseConfiguration.setLicenseExpired(false);
                licenseConfiguration.setLicenseActive(entity.getBoolean("active"));
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error in setting licence configuration from persistence", ex);
        }
    }

    /**
         * Generates a trial license via the configured License API and saves the returned license key to persistence
         * and the in-memory AUI configuration.
         *
         * <p>On success returns a response containing a JSON object with the `license-key`. On failure returns a response
         * with an appropriate error code and message reflecting token generation failures, HTTP errors from the License API,
         * missing fields in the API response, or internal server errors.</p>
         *
         * @return GenericResponse with status `true` and a JSON payload containing `license-key` on success;
         *         `false` with an error code and message otherwise.
         */
    public GenericResponse generateTrialLicense() {
        log.info("Inside generateTrialLicense: the method to generate trial license");
        Integer httpStatus = null;
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();

            log.debug("Trying to generate trial License.");
            String trialLicenseUrl = formatApiUrl(licenseConfiguration.getScanApiHostname(), "/trial");

            io.jans.as.client.TokenResponse tokenResponse = generateToken(licenseConfiguration.getScanAuthServerHostname(), licenseConfiguration.getScanApiClientId(), licenseConfiguration.getScanApiClientSecret());
            if (tokenResponse == null) {
                log.info(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
                return CommonUtils.createGenericResponse(false, 500, ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            }
            CloseableHttpClient httpClient = httpService.createHttpsClientWithTlsPolicy(TLS_ENABLED_PROTOCOLS,
                    TLS_ALLOWED_CIPHER_SUITES);

            Map<String, String> body = new HashMap<>();
            body.put(HARDWARE_ID, licenseConfiguration.getHardwareId());

            HttpServiceResponse httpServiceResponse = httpService
                    .executePost(httpClient, trialLicenseUrl, tokenResponse.getAccessToken(),
                            null, mapper.writeValueAsString(body),
                            ContentType.APPLICATION_JSON,
                            BEARER);
            String jsonString = null;
            try {
                if (httpServiceResponse.getHttpResponse() != null
                        && httpServiceResponse.getHttpResponse().getStatusLine() != null) {
                    logHttpResponse(trialLicenseUrl, httpServiceResponse);
                    HttpEntity httpEntity = httpServiceResponse.getHttpResponse().getEntity();
                    httpStatus = httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode();
                    if (httpStatus == 200 && httpEntity != null) {
                        jsonString = httpService.getContent(httpEntity);
                        JsonNode entityNode = mapper.readTree(jsonString);
                        JSONObject entity = new JSONObject(entityNode.toString());

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

                        ObjectNode jsonNode = mapper.createObjectNode();
                        jsonNode.put("license-key", entity.getString("license"));

                        return CommonUtils.createGenericResponse(true, 200, "Trial license generated.", jsonNode);
                    }
                    //getting error
                    jsonString = httpService.getContent(httpEntity);
                    JsonNode jsonNode = mapper.readValue(jsonString, JsonNode.class);

                    if (!Strings.isNullOrEmpty(jsonNode.get(MESSAGE).textValue())) {
                        log.error("{}: {}", TRIAL_GENERATE_ERROR_RESPONSE, jsonString);
                        return CommonUtils.createGenericResponse(false, jsonNode.get(CODE).intValue(), jsonNode.get(MESSAGE).textValue());
                    }
                }
            } finally {
                if (httpServiceResponse != null) {
                    httpServiceResponse.closeConnection(); // Returns connection to pool
                }
            }
            log.error("{}: {}", TRIAL_GENERATE_ERROR_RESPONSE, jsonString);
            return CommonUtils.createGenericResponse(false, 500, "Error in generating trial license.");
        } catch (Exception e) {
            Optional<GenericResponse> genericResOptional = handleLicenseApiNotAccessible(httpStatus);
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
        log.info("Inside getLicenseDetails: the method to get license details");
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
    private void saveCreateClientInPersistence(String ssa, DCRResponse dcrResponse) {
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
     * Generate an OAuth2 access token using the client credentials grant.
     *
     * @returns the TokenResponse containing the access token and related metadata, or `null` if token generation failed.
     */
    private io.jans.as.client.TokenResponse generateToken(String opHost, String clientId, String clientSecret) {
        try {
            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setScope(LicenseResource.SCOPE_LICENSE_READ);

            log.info("Trying to get access token from auth server.");
            String scanLicenseApiHostname = StringUtils.removeEnd(opHost, "/") +
                    "/jans-auth/restv1/token";
            io.jans.as.client.TokenResponse tokenResponse = null;
            tokenResponse = getToken(tokenRequest, scanLicenseApiHostname);
            return tokenResponse;
        } catch (Exception e) {
            log.error(ErrorResponse.TOKEN_GENERATION_ERROR.getDescription());
            return null;
        }
    }

    /**
     * Map HTTP status codes indicating license API unavailability to a standardized error response.
     *
     * @param httpStatus the HTTP status code returned by the license API; may be {@code null}
     * @return an {@code Optional} containing a {@link GenericResponse} for status {@code 404} or {@code 503}, {@code Optional.empty()} if {@code httpStatus} is {@code null} or any other code
     */
    private Optional<GenericResponse> handleLicenseApiNotAccessible(Integer httpStatus) {
        if (httpStatus == null) {
            return Optional.empty();
        }
        if (httpStatus == 404) {
            log.error("{}", LICENSE_APIS_404);
            return Optional.of(CommonUtils.createGenericResponse(false, httpStatus, LICENSE_APIS_404));
        }
        if (httpStatus == 503) {
            log.error("{}", LICENSE_APIS_503);
            return Optional.of(CommonUtils.createGenericResponse(false, httpStatus, LICENSE_APIS_503));
        }
        return Optional.empty();
    }

    /**
     * Verifies that the given JSON object contains the specified field names and produces an error response if any are missing.
     *
     * @param entity the JSON object to inspect for required fields
     * @param format a printf-style format string used to build the error message when fields are missing; it should contain one placeholder for the comma-separated missing field list
     * @param args   the names of required fields to check for presence in {@code entity}
     * @return       an {@code Optional} containing a {@code GenericResponse} with HTTP status 500 and a formatted message listing missing fields if any are absent, or {@code Optional.empty()} if all fields are present
     */
    private Optional<GenericResponse> handleMissingFieldsInResponse(JSONObject entity, String format, String...
            args) {
        StringBuilder missingFields = new StringBuilder();
        for (String arg : args) {
            if (!entity.has(arg)) {
                missingFields.append(missingFields.length() > 0 ? ", " : "");
                missingFields.append(arg);
            }
        }
        return missingFields.length() > 0 ?
                Optional.of(CommonUtils.createGenericResponse(false, 500, String.format(format, missingFields.toString()))) :
                Optional.empty();
    }

    /**
     * Builds a full License API URL by combining the Scan API hostname with the specified license endpoint.
     *
     * @param scanApiHostname the Scan API hostname or base URL (may include or omit a trailing slash)
     * @param endpoint        the license endpoint path (should begin with a slash, e.g. "/activate")
     * @return                the normalized full URL for the License API (e.g. "{host}/v1/license{endpoint}")
     */
    private String formatApiUrl(String scanApiHostname, String endpoint) {
        return StringUtils.removeEnd(scanApiHostname, "/") + "/v1/license" + endpoint;
    }

    /**
     * Logs detailed information about an HTTP response for the given request URL when debug logging is enabled.
     *
     * If debug logging is disabled or the provided response is null, this method returns without side effects.
     *
     * @param url      the request URL that was called
     * @param response the HttpServiceResponse containing the underlying HTTP response and metadata
     */
    private void logHttpResponse(String url, HttpServiceResponse response) {
        if (!log.isDebugEnabled() || response == null) {
            return;
        }
        log.debug(
                "Response on calling {} --- response:{}, status:{}, entity:{}",
                url,
                response.getHttpResponse(),
                response.getHttpResponse().getStatusLine(),
                response.getHttpResponse().getEntity()
        );
    }
}