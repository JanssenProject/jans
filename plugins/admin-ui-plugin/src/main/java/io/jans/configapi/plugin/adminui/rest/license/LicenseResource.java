package io.jans.configapi.plugin.adminui.rest.license;

import io.jans.configapi.plugin.adminui.model.auth.LicenseRequest;
import io.jans.configapi.plugin.adminui.model.auth.LicenseResponse;
import io.jans.configapi.plugin.adminui.model.exception.ApplicationException;
import io.jans.configapi.plugin.adminui.service.license.LicenseDetailsService;
import io.jans.configapi.plugin.adminui.utils.ErrorResponse;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/admin-ui/license")
public class LicenseResource {

    static final String CHECK_LICENSE = "/checkLicense";
    static final String ACTIVATE_LICENSE = "/activateLicense";
    static final String GET_LICENSE_DETAILS = "/getLicenseDetails";
    static final String UPDATE_LICENSE_DETAILS = "/updateLicenseDetails";

    @Inject
    Logger log;

    @Inject
    LicenseDetailsService licenseDetailsService;

    @GET
    @Path(CHECK_LICENSE)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkLicense() {
        try {
            log.info("Check if active license present.");
            Boolean isLicenseActive = licenseDetailsService.checkLicense();
            log.info("Active license present (true/false): {}", isLicenseActive);
            return Response.ok(isLicenseActive).build();
        } catch (Exception e) {
            log.error(ErrorResponse.CHECK_LICENSE_ERROR.getDescription(), e);
            return Response.serverError().entity(false).build();
        }
    }

    @POST
    @Path(ACTIVATE_LICENSE)
    @Produces(MediaType.TEXT_PLAIN)
    public Response activateLicense(@Valid @NotNull LicenseRequest licenseRequest) {
        try {
            log.info("Trying to activate license using licese-key.");
            Boolean isLicenseActive = licenseDetailsService.activateLicense(licenseRequest);
            log.info("License activated (true/false): {}", isLicenseActive);
            return Response.ok(isLicenseActive).build();
        } catch (Exception e) {
            log.error(ErrorResponse.ACTIVATE_LICENSE_ERROR.getDescription(), e);
            return Response.serverError().entity(false).build();
        }
    }

    @GET
    @Path(GET_LICENSE_DETAILS)
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

    @PUT
    @Path(UPDATE_LICENSE_DETAILS)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLicenseDetails(@Valid @NotNull LicenseRequest licenseRequest) {
        try {
            log.info("Trying to update license details.");
            LicenseResponse licenseResponse = licenseDetailsService.updateLicenseDetails(licenseRequest);
            return Response.ok(licenseResponse).build();
        } catch (ApplicationException e) {
            log.error(ErrorResponse.UPDATE_LICENSE_DETAILS_ERROR.getDescription(), e);
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.error(ErrorResponse.UPDATE_LICENSE_DETAILS_ERROR.getDescription(), e);
            return Response.serverError().entity(ErrorResponse.UPDATE_LICENSE_DETAILS_ERROR.getDescription()).build();
        }
    }
}