package org.xdi.oxd.licenser.server.ws;

import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.client.Jackson;
import org.xdi.oxd.license.client.data.CertificateGrantResponse;
import org.xdi.oxd.license.client.data.ErrorType;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.licenser.server.service.LicenseIdService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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

    @GET
    @Path("/grant")
    @Produces({MediaType.APPLICATION_JSON})
    public Response grant(@QueryParam("license_id") String licenseId, @Context HttpServletRequest httpRequest) {
        LOG.trace("/grant, license_id=" + licenseId);
        final LdapLicenseId ldapLicenseId = validateLicenseId(licenseId);
        // todo bound to license_id

        CertificateGrantResponse response = new CertificateGrantResponse();
        response.setExpiresAt(oneYearFromNow());
        LOG.trace("/grant, response OK, entity:" + response);
        return Response.ok().entity(Jackson.asJsonSilently(response)).build();
    }

    public static Date oneYearFromNow() {
        final long now = new Date().getTime();
        final long oneYear = TimeUnit.DAYS.toMillis(364);
        return new Date(now + oneYear);
    }

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
    @Path("/update_ejbca")
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateEjbCa(@FormParam("license_id") String licenseId, @Context HttpServletRequest httpRequest) {
        LOG.trace("/update_ejbca, license_id=" + licenseId);
        final LdapLicenseId ldapLicenseId =validateLicenseId(licenseId);
        // todo increase counter
        LOG.trace("/update_ejbca, response OK, license_id:" + licenseId);
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
