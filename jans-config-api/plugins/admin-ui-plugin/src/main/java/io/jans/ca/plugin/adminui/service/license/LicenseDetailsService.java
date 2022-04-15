package io.jans.ca.plugin.adminui.service.license;

import com.google.common.base.Strings;
import com.licensespring.License;
import com.licensespring.LicenseManager;
import com.licensespring.management.ManagementConfiguration;
import com.licensespring.management.dto.SearchResult;
import com.licensespring.management.dto.request.SearchLicensesRequest;
import com.licensespring.management.dto.request.UpdateLicenseRequest;
import com.licensespring.management.model.BackOfficeLicense;
import com.licensespring.model.ActivationLicense;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.config.adminui.AdminRole;
import io.jans.as.model.config.adminui.LicenseSpringCredentials;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.config.LicenseConfiguration;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.ca.plugin.adminui.model.auth.LicenseRequest;
import io.jans.ca.plugin.adminui.model.auth.LicenseResponse;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.orm.PersistenceEntryManager;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.util.List;

@Singleton
public class LicenseDetailsService {

    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    private PersistenceEntryManager entryManager;

    public Boolean saveLicenseSpringCredentials(LicenseSpringCredentials licenseSpringCredentials) {
        try {
            //set license-spring configuration
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
            LicenseConfiguration licenseConfiguration = initializeLicenseManager(licenseSpringCredentials);
            auiConfiguration.setLicenseConfiguration(licenseConfiguration);

            License activeLicense = auiConfiguration.getLicenseConfiguration().getLicenseManager().getCurrent();
            if (activeLicense == null) {
                log.info("Error in verifying entered licenseSpring credentials. Please check if the credentials are correct.");
                return false;
            }
            //save license spring credentials
            AdminConf adminConf = entryManager.find(AdminConf.class, AppConstants.CONFIG_DN);
            adminConf.getDynamic().setLicenseSpringCredentials(licenseSpringCredentials);
            entryManager.merge(adminConf);
            return true;
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_LICENSE_SPRING_CREDENTIALS_ERROR.getDescription(), e);
            return false;
        }
    }

    public Boolean checkLicense() {
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            License activeLicense = auiConfiguration.getLicenseConfiguration().getLicenseManager().getCurrent();
            if (activeLicense == null) {
                log.info("Active license for admin-ui not present ");
                return false;
            } else {
                log.debug("Active license for admin-ui found : {} ", activeLicense.getProduct());
                License updatedLicense = auiConfiguration.getLicenseConfiguration()
                        .getLicenseManager()
                        .checkLicense(activeLicense);
                return updatedLicense != null && !activeLicense.getData().isExpired();
            }
        } catch (Exception e) {
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return false;
        }
    }

    public Boolean activateLicense(LicenseRequest licenseRequest) {
        AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

        LicenseManager licenseManager = auiConfiguration.getLicenseConfiguration().getLicenseManager();
        try {
            log.debug("Trying to activate License.");
            ActivationLicense keyBased = ActivationLicense.fromKey(licenseRequest.getLicenseKey());
            License license = licenseManager.activateLicense(keyBased);
            log.debug("License activated : {} ", license.getProduct());
            return !license.getData().isExpired();
        } catch (Exception e) {
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return false;
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

    private LicenseConfiguration initializeLicenseManager(LicenseSpringCredentials licenseSpringCredentials) {
        LicenseConfiguration licenseConfiguration = new LicenseConfiguration();
        licenseConfiguration.setApiKey(licenseSpringCredentials.getApiKey());
        licenseConfiguration.setProductCode(licenseSpringCredentials.getProductCode());
        licenseConfiguration.setSharedKey(licenseSpringCredentials.getSharedKey());
        licenseConfiguration.setManagementKey(licenseSpringCredentials.getManagementKey());
        licenseConfiguration.initializeLicenseManager();
        return licenseConfiguration;
    }
}