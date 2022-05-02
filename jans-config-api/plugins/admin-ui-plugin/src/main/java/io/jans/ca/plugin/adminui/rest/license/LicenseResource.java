package io.jans.ca.plugin.adminui.rest.license;

import io.jans.as.model.config.adminui.LicenseSpringCredentials;
import io.jans.ca.plugin.adminui.model.auth.LicenseApiResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.model.auth.LicenseRequest;
import io.jans.ca.plugin.adminui.model.auth.LicenseResponse;
import io.jans.ca.plugin.adminui.service.license.LicenseDetailsService;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.rest.ProtectedApi;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin-ui/license")
public class LicenseResource {

    static final String IS_ACTIVE = "/isActive";
    static final String SAVE_API_CREDENTIALS = "/saveApiCredentials";
    static final String ACTIVATE_LICENSE = "/activateLicense";
    static final String LICENSE_DETAILS = "/licenseDetails";

    public static final String SCOPE_OPENID = "openid";
    static final String SCOPE_LICENSE_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/license.readonly";
    static final String SCOPE_LICENSE_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/license.write";

    @Inject
    Logger log;

    @Inject
    LicenseDetailsService licenseDetailsService;

    @GET
    @Path(IS_ACTIVE)
    @ProtectedApi(scopes={SCOPE_LICENSE_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response isActive() {
        LicenseApiResponse licenseResponse = null;
        try {
            log.info("Check if active license present.");
            licenseResponse = licenseDetailsService.checkLicense();
            log.info("Active license present (true/false): {}", licenseResponse.isApiResult());
            return Response.ok(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @POST
    @Path(ACTIVATE_LICENSE)
    @ProtectedApi(scopes={SCOPE_LICENSE_WRITE})
    @Produces(MediaType.APPLICATION_JSON)
    public Response activateLicense(@Valid @NotNull LicenseRequest licenseRequest) {
        LicenseApiResponse licenseResponse = null;
        try {
            log.info("Trying to activate license using licese-key.");
            licenseResponse = licenseDetailsService.activateLicense(licenseRequest);
            log.info("License activated (true/false): {}", licenseResponse.isApiResult());
            return Response.ok(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @POST
    @Path(SAVE_API_CREDENTIALS)
    @ProtectedApi(scopes={SCOPE_LICENSE_WRITE})
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveLicenseCredentials(@Valid @NotNull LicenseSpringCredentials licenseSpringCredentials) {
        LicenseApiResponse licenseResponse = null;
        try {
            log.info("Trying to save license-spring credentials.");
            licenseResponse = licenseDetailsService.saveLicenseSpringCredentials(licenseSpringCredentials);
            log.info("License saved (true/false): {}", licenseResponse.isApiResult());
            return Response.ok(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.SAVE_LICENSE_SPRING_CREDENTIALS_ERROR.getDescription(), e);
            return Response.serverError().entity(licenseResponse).build();
        }
    }

    @GET
    @Path(LICENSE_DETAILS)
    @ProtectedApi(scopes={SCOPE_LICENSE_READ})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLicenseDetails() {
        try {
            log.info("Trying to fetch license details.");
            LicenseResponse licenseResponse = licenseDetailsService.getLicenseDetails();
            return Response.ok(licenseResponse).build();
        } catch (Exception e) {
            log.error(ErrorResponse.GET_LICENSE_DETAILS_ERROR.getDescription(), e);
            return Response.serverError().entity(ErrorResponse.GET_LICENSE_DETAILS_ERROR.getDescription()).build();
        }
    }
}