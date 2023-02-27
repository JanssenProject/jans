package io.jans.ca.plugin.adminui.service.license;

import com.google.common.base.Strings;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.ca.plugin.adminui.model.auth.LicenseApiResponse;
import io.jans.ca.plugin.adminui.model.auth.LicenseRequest;
import io.jans.ca.plugin.adminui.model.auth.LicenseResponse;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.config.LicenseConfiguration;
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
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class LicenseDetailsService {

    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    private PersistenceEntryManager entryManager;

    /**
     * The function checks the license key and the api key and returns a response object
     *
     * @return A LicenseApiResponse object is being returned.
     */
    public LicenseApiResponse checkLicense() {
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();
            if (licenseConfiguration == null || Strings.isNullOrEmpty(licenseConfiguration.getApiKey())) {
                log.info("License api-keys not present ");
                return createLicenseResponse(false, 500, "License api-keys not present.");
            }
            if (Strings.isNullOrEmpty(licenseConfiguration.getLicenseKey())) {
                log.info("Active license for admin-ui not present ");
                return createLicenseResponse(false, 500, "Active license not present.");
            }
            //check license-key
            String checkLicenseUrl = (new StringBuffer()).append(AppConstants.LICENSE_SPRING_API_URL)
                    .append("check_license?license_key=")
                    .append(licenseConfiguration.getLicenseKey())
                    .append("&product=")
                    .append(licenseConfiguration.getProductCode())
                    .append("&hardware_id=")
                    .append(licenseConfiguration.getHardwareId()).toString();

            MultivaluedMap<String, Object> headers = createHeaderMap(licenseConfiguration);
            Invocation.Builder request = ClientFactory.instance().getClientBuilder(checkLicenseUrl);
            request.headers(headers);
            Response response = request.get();

            log.info("license Credentials request status code: {}", response.getStatus());
            if (response.getStatus() == 200) {
                JsonObject entity = response.readEntity(JsonObject.class);
                if (entity.getBoolean("license_active") && !entity.getBoolean("is_expired")) {
                    return createLicenseResponse(true, 200, "Valid license present.");
                }
            }
            log.error("license Credentials error response: {}", response.readEntity(String.class));
            return createLicenseResponse(false, 500, "Active license not present.");

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
            return createLicenseResponse(false, 500, "The license has been already activated.");
        }
        AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
        LicenseConfiguration licenseConfiguration = auiConfiguration.getLicenseConfiguration();

        try {
            log.debug("Trying to activate License.");
            String activateLicenseUrl = (new StringBuffer()).append(AppConstants.LICENSE_SPRING_API_URL)
                    .append("activate_license").toString();

            MultivaluedMap<String, Object> headers = createHeaderMap(licenseConfiguration);
            Invocation.Builder request = ClientFactory.instance().getClientBuilder(activateLicenseUrl);
            request.headers(headers);

            Map<String, String> body = new HashMap<>();
            body.put("license_key", licenseRequest.getLicenseKey());
            body.put("hardware_id", licenseConfiguration.getHardwareId());
            body.put("product", licenseConfiguration.getProductCode());

            Response response = request
                    .post(Entity.entity(body, MediaType.APPLICATION_JSON));
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
            log.error("license Activation error response: {}", response.readEntity(String.class));
            return createLicenseResponse(false, response.getStatus(), "License is not activated.");
        } catch (Exception e) {
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return createLicenseResponse(false, 500, ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription());
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
            String checkLicenseUrl = (new StringBuffer()).append(AppConstants.LICENSE_SPRING_API_URL)
                    .append("check_license?license_key=")
                    .append(licenseConfiguration.getLicenseKey())
                    .append("&product=")
                    .append(licenseConfiguration.getProductCode())
                    .append("&hardware_id=")
                    .append(licenseConfiguration.getHardwareId()).toString();

            MultivaluedMap<String, Object> headers = createHeaderMap(licenseConfiguration);
            Invocation.Builder request = ClientFactory.instance().getClientBuilder(checkLicenseUrl);
            request.headers(headers);

            Response response = request.get();

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

    private MultivaluedMap<String, Object> createHeaderMap(LicenseConfiguration licenseConfiguration) {
        String formattedDate = CommonUtils.getFormattedDate();
        String signing_string = "licenseSpring\ndate: " + formattedDate;
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");

            SecretKeySpec secret_key = new SecretKeySpec(licenseConfiguration.getSharedKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            String signature = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(signing_string.getBytes(StandardCharsets.UTF_8)));
            log.debug("header signature for license api: {}", signature);
            log.debug("header signature date for license api: {}", formattedDate);
            MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putSingle("Content-Type", "application/json");
            headers.putSingle("Date", formattedDate);
            headers.putSingle("Authorization", "algorithm=\"hmac-sha256\",headers=\"date\",signature=\"" + signature + "\",apiKey=\"" + licenseConfiguration.getApiKey() + "\"");
            return headers;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error in generating authorization header", e);
            return null;
        }

    }

    private LicenseApiResponse createLicenseResponse(boolean result, int responseCode, String responseMessage) {
        LicenseApiResponse licenseResponse = new LicenseApiResponse();
        licenseResponse.setResponseCode(responseCode);
        licenseResponse.setResponseMessage(responseMessage);
        licenseResponse.setApiResult(result);
        return licenseResponse;
    }
}