package org.xdi.oxd.licenser.server.ws;

import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.client.data.ErrorType;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.licenser.server.service.EjbCaService;
import org.xdi.oxd.licenser.server.service.LicenseIdService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/11/2014
 */

@Path("/rest/certificate")
public class CertificateWS {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateWS.class);

    @Inject
    LicenseIdService licenseIdService;
    @Inject
    ErrorService errorService;
    @Inject
    EjbCaService ejbCaService;

    @POST
    @Path("/notify")
    @Produces({MediaType.APPLICATION_JSON})
    public Response notify(@FormParam("license_id") String licenseId, @Context HttpServletRequest httpRequest) {
        LOG.trace("/notify, license_id=" + licenseId);
        final LdapLicenseId ldapLicenseId =validateLicenseId(licenseId);
        // todo increase counter
        LOG.trace("/notify, response OK, license_id:" + licenseId);
        return Response.ok().entity("").build();
    }

    @POST
    @Path("/sign_csr")
    @Produces({MediaType.APPLICATION_JSON})
    public Response signCsrByEjbCa(@FormParam("license_id") String licenseId, @FormParam("csr_as_pem") String csrAsPem, @Context HttpServletRequest httpRequest) {
        LOG.trace("/sign_csr, license_id=" + licenseId);
        final LdapLicenseId ldapLicenseId = validateLicenseId(licenseId);
        ejbCaService.createUser(ldapLicenseId);
        // todo increase counter
        LOG.trace("/sign_csr, response OK, license_id:" + licenseId);
        return Response.ok().entity("").build();
    }

    private LdapLicenseId validateLicenseId(String licenseId) {
        try {
            if (StringUtils.isBlank(licenseId)) {
                errorService.throwError(Response.Status.BAD_REQUEST, ErrorType.LICENSE_ID_EMPTY);
            }
            final LdapLicenseId ldapLicenseId = licenseIdService.getById(licenseId);
            if (ldapLicenseId == null) {
                errorService.throwError(Response.Status.BAD_REQUEST, ErrorType.LICENSE_ID_INVALID);
            }
            return ldapLicenseId;
        } catch (Exception e) {
            errorService.throwError(Response.Status.BAD_REQUEST, ErrorType.LICENSE_ID_INVALID);
            return null;
        }
    }

}
