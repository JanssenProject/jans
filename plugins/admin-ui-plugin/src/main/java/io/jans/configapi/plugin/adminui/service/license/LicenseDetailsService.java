package io.jans.configapi.plugin.adminui.service.license;

import com.google.common.base.Strings;
import com.licensespring.License;
import com.licensespring.LicenseManager;
import com.licensespring.management.ManagementConfiguration;
import com.licensespring.management.dto.SearchResult;
import com.licensespring.management.dto.request.SearchLicensesRequest;
import com.licensespring.management.dto.request.UpdateLicenseRequest;
import com.licensespring.management.model.BackOfficeLicense;
import com.licensespring.model.ActivationLicense;
import io.jans.configapi.plugin.adminui.model.auth.LicenseRequest;
import io.jans.configapi.plugin.adminui.model.auth.LicenseResponse;
import io.jans.configapi.plugin.adminui.model.config.AUIConfiguration;
import io.jans.configapi.plugin.adminui.model.exception.ApplicationException;
import io.jans.configapi.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.configapi.plugin.adminui.utils.ErrorResponse;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

@Singleton
public class LicenseDetailsService {
    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    public Boolean checkLicense() {
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            Boolean isLicenseCheckEnabled = auiConfiguration.getLicenseConfiguration().getEnabled();
            if (!Boolean.TRUE.equals(isLicenseCheckEnabled)) {
                log.debug("License configuration is disabled. ");
                return true;
            }
            License activeLicense = auiConfiguration.getLicenseConfiguration().getLicenseManager().getCurrent();
            if (activeLicense == null) {
                log.info("Active license for admin-ui not present ");
                return false;
            } else {
                log.debug("Active license for admin-ui found : {} ", activeLicense.getProduct());
                License updatedLicense = auiConfiguration.getLicenseConfiguration()
                        .getLicenseManager()
                        .checkLicense(activeLicense);
                return updatedLicense != null;
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
            return true;
        } catch (Exception e) {
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return false;
        }
    }

    public LicenseResponse getLicenseDetails() {
        LicenseResponse licenseResponse = new LicenseResponse();
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            Boolean isLicenseCheckEnabled = auiConfiguration.getLicenseConfiguration().getEnabled();
            if (!Boolean.TRUE.equals(isLicenseCheckEnabled)) {
                log.debug("License configuration is disabled.");
                licenseResponse.setIsLicenseEnable(false);
                return licenseResponse;
            }
            License activeLicense = auiConfiguration.getLicenseConfiguration().getLicenseManager().getCurrent();
            if (activeLicense == null) {
                log.debug("Active license for admin-ui not present ");
                licenseResponse.setIsLicenseEnable(false);
                return licenseResponse;
            } else {
                log.debug("Active license for admin-ui found : {}", activeLicense.getProduct());
                licenseResponse.setIsLicenseEnable(true);
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
            licenseResponse.setIsLicenseEnable(false);
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
                licenseResponse.setIsLicenseEnable(false);
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
            licenseResponse.setIsLicenseEnable(true);
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
}