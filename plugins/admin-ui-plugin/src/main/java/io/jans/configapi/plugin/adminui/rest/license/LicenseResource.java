package io.jans.configapi.plugin.adminui.rest.license;

import com.licensespring.License;
import com.licensespring.LicenseManager;
import com.licensespring.model.ActivationLicense;
import com.licensespring.model.exceptions.LicenseSpringException;
import io.jans.configapi.plugin.adminui.model.auth.LicenseRequest;
import io.jans.configapi.plugin.adminui.model.config.AUIConfiguration;
import io.jans.configapi.plugin.adminui.service.config.AUIConfigurationService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/admin-ui/license")
public class LicenseResource {

    static final String CHECK_LICENSE = "/checkLicense";
    static final String ACTIVATE_LICENSE = "/activateLicense";

    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @GET
    @Path(CHECK_LICENSE)
    @Produces(MediaType.APPLICATION_JSON)
    public Boolean checkLicense() {
        try {
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            if(!auiConfiguration.getLicenseConfiguration().getEnabled()) {
                log.info("License configuration is disabled. ");
                return true;
            }
            License activeLicense = auiConfiguration.getLicenseConfiguration().getLicenseManager().getCurrent();
            if (activeLicense == null) {
                log.info("Active license for admin-ui not present ");
                return false;
            } else {
                log.info("Active license for admin-ui found :: " + activeLicense.getProduct());
                License updatedLicense = auiConfiguration.getLicenseConfiguration()
                        .getLicenseManager()
                        .checkLicense(activeLicense);
                if(updatedLicense == null) {
                    return false;
                }
                return true;
            }
        } catch (LicenseSpringException e) {
            log.error(e.getCause().getMessage());
            return false;
        }
    }

    @POST
    @Path(ACTIVATE_LICENSE)
    @Produces(MediaType.TEXT_PLAIN)
    Boolean activateLicense(@Valid @NotNull LicenseRequest licenseRequest) throws Exception {
        AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

        LicenseManager licenseManager = auiConfiguration.getLicenseConfiguration().getLicenseManager();
        try {
            log.info("Trying to activate License.");
            ActivationLicense keyBased = ActivationLicense.fromKey(licenseRequest.getLicenseKey());
            License license = licenseManager.activateLicense(keyBased);
            log.info("License activated :: " + license.getProduct());
            return true;
        } catch (LicenseSpringException e) {
            log.error("Error in activating license: ", e);
            return false;
        } catch (Exception e) {
            log.error("Error in activating license: ", e);
            return false;
        }
    }
}
