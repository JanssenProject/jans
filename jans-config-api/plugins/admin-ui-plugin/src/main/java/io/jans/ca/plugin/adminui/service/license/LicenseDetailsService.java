package io.jans.ca.plugin.adminui.service.license;

import com.google.common.base.Strings;
import com.licensespring.License;
import com.licensespring.LicenseManager;
import com.licensespring.internal.services.NowDateProvider;
import com.licensespring.management.ManagementConfiguration;
import com.licensespring.management.dto.SearchResult;
import com.licensespring.management.dto.request.SearchLicensesRequest;
import com.licensespring.management.dto.request.UpdateLicenseRequest;
import com.licensespring.management.model.BackOfficeLicense;
import com.licensespring.model.ActivationLicense;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.LicenseSpringCredentials;
import io.jans.ca.plugin.adminui.model.auth.LicenseApiResponse;
import io.jans.ca.plugin.adminui.model.auth.LicenseRequest;
import io.jans.ca.plugin.adminui.model.auth.LicenseResponse;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.config.LicenseConfiguration;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.orm.PersistenceEntryManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.slf4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Singleton
public class LicenseDetailsService {

    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    private PersistenceEntryManager entryManager;

    public LicenseApiResponse saveLicenseSpringCredentials(LicenseSpringCredentials licenseSpringCredentials) {
        try {
            if (!licenseCredentialsValid(licenseSpringCredentials)) {
                return createLicenseResponse(false, 400, "The license credentials are not valid.");
            }
            //check is license is already active
            LicenseApiResponse licenseApiResponse = checkLicense();
            if (licenseApiResponse.isApiResult()) {
                return createLicenseResponse(false, 500, "The license has been already activated.");
            }
            //set license-spring configuration
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            LicenseConfiguration licenseConfiguration = new LicenseConfiguration(licenseSpringCredentials.getApiKey(),
                    licenseSpringCredentials.getProductCode(),
                    licenseSpringCredentials.getSharedKey(),
                    licenseSpringCredentials.getManagementKey(),
                    Boolean.TRUE);

            auiConfiguration.setLicenseConfiguration(licenseConfiguration);
            auiConfigurationService.setAuiConfiguration(auiConfiguration);

            //save license spring credentials
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.CONFIG_DN);
            adminConf.getDynamic().setLicenseSpringCredentials(licenseSpringCredentials);
            entryManager.merge(adminConf);

            return createLicenseResponse(true, 201, "Success!!");
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_LICENSE_SPRING_CREDENTIALS_ERROR.getDescription(), e);
            return createLicenseResponse(false, 500, ErrorResponse.SAVE_LICENSE_SPRING_CREDENTIALS_ERROR.getDescription());
        }
    }

    public LicenseApiResponse checkLicense() {
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            License activeLicense = auiConfiguration.getLicenseConfiguration().getLicenseManager().getCurrent();
            if (activeLicense == null) {
                log.info("Active license for admin-ui not present ");
                return createLicenseResponse(false, 500, "Active license not present.");
            } else {
                log.debug("Active license for admin-ui found : {} ", activeLicense.getProduct());
                License updatedLicense = auiConfiguration.getLicenseConfiguration()
                        .getLicenseManager()
                        .checkLicense(activeLicense);
                return createLicenseResponse(updatedLicense != null && !activeLicense.getData().isExpired(), 200, "");
            }
        } catch (Exception e) {
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return createLicenseResponse(false, 500, ErrorResponse.CHECK_LICENSE_ERROR.getDescription());
        }
    }

    public LicenseApiResponse activateLicense(LicenseRequest licenseRequest) {
        //check is license is already active
        LicenseApiResponse licenseApiResponse = checkLicense();
        if (licenseApiResponse.isApiResult()) {
            return createLicenseResponse(false, 500, "The license has been already activated.");
        }
        AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

        LicenseManager licenseManager = auiConfiguration.getLicenseConfiguration().getLicenseManager();
        try {
            log.debug("Trying to activate License.");
            ActivationLicense keyBased = ActivationLicense.fromKey(licenseRequest.getLicenseKey());
            License license = licenseManager.activateLicense(keyBased);
            log.debug("License activated : {} ", license.getProduct());
            return createLicenseResponse(!license.getData().isExpired(), 200, "");
        } catch (Exception e) {
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return createLicenseResponse(false, 500, ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription());
        }
    }

    public LicenseResponse getLicenseDetails() {
        LicenseResponse licenseResponse = new LicenseResponse();
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            License activeLicense = auiConfiguration.getLicenseConfiguration().getLicenseManager().getCurrent();
            if (activeLicense == null) {
                log.debug("Active license for admin-ui not present ");
                licenseResponse.setLicenseEnabled(false);
                return licenseResponse;
            } else {
                log.debug("Active license for admin-ui found : {}", activeLicense.getProduct());
                licenseResponse.setLicenseEnabled(true);
                licenseResponse.setProductName(activeLicense.getProduct().getProductName());
                licenseResponse.setProductCode(activeLicense.getProduct().getShortCode());
                licenseResponse.setLicenseType(activeLicense.getData().getLicenseType().name());
                licenseResponse.setMaxActivations(activeLicense.getData().getMaxActivations());
                licenseResponse.setLicenseKey(activeLicense.getIdentity().getLicenseKey());
                licenseResponse.setValidityPeriod(activeLicense.getData().getValidityPeriod().toString());
                licenseResponse.setCompanyName(activeLicense.getData().getCustomer().getCompanyName());
                licenseResponse.setCustomerEmail(activeLicense.getData().getCustomer().getEmail());
                licenseResponse.setCustomerFirstName(activeLicense.getData().getCustomer().getFirstName());
                licenseResponse.setCustomerLastName(activeLicense.getData().getCustomer().getLastName());
                licenseResponse.setLicenseActive(!activeLicense.getData().isExpired());
                return licenseResponse;
            }
        } catch (Exception e) {
            log.error(ErrorResponse.GET_LICENSE_DETAILS_ERROR.getDescription(), e);
            licenseResponse.setLicenseEnabled(false);
            return licenseResponse;
        }

    }

    public LicenseResponse updateLicenseDetails(LicenseRequest licenseRequest) throws ApplicationException {
        LicenseResponse licenseResponse = new LicenseResponse();
        log.debug("LicenseRequest params: {}", licenseRequest);
        try {
            if (Strings.isNullOrEmpty(licenseRequest.getValidityPeriod())) {
                log.error(ErrorResponse.LICENSE_VALIDITY_PERIOD_NOT_FOUND.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.LICENSE_VALIDITY_PERIOD_NOT_FOUND.getDescription());
            }
            if (licenseRequest.getMaxActivations() < 1) {
                log.error(ErrorResponse.INVALID_MAXIMUM_ACTIVATIONS.getDescription());
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.INVALID_MAXIMUM_ACTIVATIONS.getDescription());
            }
            if (licenseRequest.getValidityPeriod().length() > 10) {
                licenseRequest.setValidityPeriod(licenseRequest.getValidityPeriod().substring(0, 10));
            }
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            ManagementConfiguration configuration = ManagementConfiguration.builder()
                    .managementKey(auiConfiguration.getLicenseConfiguration().getManagementKey())
                    .requestLogging(feign.Logger.Level.FULL)
                    .build();
            //search license by license-key
            License activeLicense = auiConfiguration.getLicenseConfiguration().getLicenseManager().getCurrent();
            if (activeLicense == null) {
                licenseResponse.setLicenseEnabled(false);
                return licenseResponse;
            }
            SearchLicensesRequest request = SearchLicensesRequest.builder()
                    .licenseKey(activeLicense.getIdentity().getLicenseKey())
                    .limit(1)
                    .build();

            com.licensespring.management.LicenseService licenseService = new com.licensespring.management.LicenseService(configuration);
            SearchResult<BackOfficeLicense> response = licenseService.searchLicenses(request);
            //update license details
            UpdateLicenseRequest update = UpdateLicenseRequest.builder()
                    .isTrial(false)
                    .validityPeriod(licenseRequest.getValidityPeriod())
                    .maxActivations(licenseRequest.getMaxActivations())
                    .enabled(licenseRequest.getLicenseActive())
                    .build();

            BackOfficeLicense updated = licenseService.updateLicense(response.getResults().get(0).getId(), update);

            //create LicenseResponse
            licenseResponse.setLicenseEnabled(true);
            licenseResponse.setProductName(activeLicense.getProduct().getProductName());
            licenseResponse.setProductCode(activeLicense.getProduct().getShortCode());
            licenseResponse.setLicenseType(activeLicense.getData().getLicenseType().name());
            licenseResponse.setLicenseKey(activeLicense.getIdentity().getLicenseKey());
            licenseResponse.setCompanyName(activeLicense.getData().getCustomer().getCompanyName());
            licenseResponse.setCustomerEmail(activeLicense.getData().getCustomer().getEmail());
            licenseResponse.setCustomerFirstName(activeLicense.getData().getCustomer().getFirstName());
            licenseResponse.setCustomerLastName(activeLicense.getData().getCustomer().getLastName());

            licenseResponse.setMaxActivations(updated.getMaxActivations());
            licenseResponse.setLicenseActive(updated.getActive());
            licenseResponse.setValidityPeriod(updated.getValidityPeriod());
            return licenseResponse;

        } catch (Exception e) {
            log.error(ErrorResponse.UPDATE_LICENSE_DETAILS_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.UPDATE_LICENSE_DETAILS_ERROR.getDescription());
        }
    }

    private MultivaluedMap<String, Object> createHeaderMap(LicenseSpringCredentials licenseSpringCredentials) {
        NowDateProvider provider = new NowDateProvider();
        String formattedDate = provider.getFormattedDate();
        String signing_string = "licenseSpring\ndate: " + formattedDate;
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");

            SecretKeySpec secret_key = new SecretKeySpec(licenseSpringCredentials.getSharedKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            String signature = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(signing_string.getBytes(StandardCharsets.UTF_8)));

            MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putSingle("Content-Type", "application/json");
            headers.putSingle("Date", formattedDate);
            headers.putSingle("Authorization", "algorithm=\"hmac-sha256\",headers=\"date\",signature=\"" + signature + "\",apiKey=\"" + licenseSpringCredentials.getApiKey() + "\"");
            return headers;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error in generating authorization header", e);
            return null;
        }

    }

    private boolean licenseCredentialsValid(LicenseSpringCredentials licenseSpringCredentials) {
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine();

        ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        ResteasyWebTarget target = client.target("https://api.licensespring.com/api/v4/product_details?product=" + licenseSpringCredentials.getProductCode());
        MultivaluedMap<String, Object> headers = createHeaderMap(licenseSpringCredentials);

        Response response = target.request()
                .headers(headers)
                .get();
        log.info("license Credentials request status code: {}", response.getStatus());
        if (response.getStatus() == 200) {
            JsonObject entity = response.readEntity(JsonObject.class);
            log.info("Product Information: {}", entity.toString());
            return true;
        }
        return false;
    }

    private LicenseApiResponse createLicenseResponse(boolean result, int responseCode, String responseMessage) {
        LicenseApiResponse licenseResponse = new LicenseApiResponse();
        licenseResponse.setResponseCode(responseCode);
        licenseResponse.setResponseMessage(responseMessage);
        licenseResponse.setApiResult(result);
        return licenseResponse;
    }
}