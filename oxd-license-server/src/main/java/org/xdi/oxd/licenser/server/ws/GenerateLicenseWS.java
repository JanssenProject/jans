package org.xdi.oxd.licenser.server.ws;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.client.Jackson;
import org.xdi.oxd.license.client.data.LicenseResponse;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.licenser.server.LicenseGenerator;
import org.xdi.oxd.licenser.server.LicenseGeneratorInput;
import org.xdi.oxd.licenser.server.service.LicenseCryptService;
import org.xdi.oxd.licenser.server.service.LicenseIdService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/09/2014
 */

@Path("/rest")
public class GenerateLicenseWS {

    private static final Logger LOG = LoggerFactory.getLogger(GenerateLicenseWS.class);

    @Inject
    LicenseGenerator licenseGenerator;
    @Inject
    LicenseIdService licenseIdService;
    @Inject
    LicenseCryptService licenseCryptService;

    public LicenseResponse generateLicense(String licenseIdStr) {
        try {
            LdapLicenseId licenseId = getLicenseId(licenseIdStr);

            if (licenseId.getLicensesIssuedCount() > 0 && !licenseId.getForceLicenseUpdate()) {
                LOG.debug("License was already issued and there is no update");
                return LicenseResponse.EMPTY;
            }

            LdapLicenseCrypt licenseCrypt = getLicenseCrypt(licenseId.getLicenseCryptDN(), licenseIdStr);

            LicenseGeneratorInput input = new LicenseGeneratorInput();
            input.setExpiredAt(licenseExpirationDate(licenseId));
            input.setCrypt(licenseCrypt);
            input.setMetadata(licenseId.getMetadata());

            final LicenseResponse licenseResponse = licenseGenerator.generate(input);
            updateLicenseId(licenseId);
            return licenseResponse;
        } catch (InvalidKeySpecException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } catch (NoSuchAlgorithmException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private void updateLicenseId(LdapLicenseId licenseId) {
        int licensesIssuedCount = licenseId.getLicensesIssuedCount() + 1;
        licenseId.setLicensesIssuedCount(licensesIssuedCount);
        licenseId.setForceLicenseUpdate(false);
        licenseIdService.merge(licenseId);
    }

    private LdapLicenseCrypt getLicenseCrypt(String licenseCryptDN, String licenseId) {
        try {
            final LdapLicenseCrypt entity = licenseCryptService.get(licenseCryptDN);
            if (entity != null) {
                return entity;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Crypt object is corrupted for License ID: " + licenseId);
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Crypt object is corrupted for License ID: " + licenseId).build());
    }

    private Date licenseExpirationDate(LdapLicenseId licenseId) {

        if (licenseId != null && licenseId.getMetadataAsObject() != null && licenseId.getMetadataAsObject().getLicenseType() != null) {
            switch(licenseId.getMetadataAsObject().getLicenseType()) {
                case PREMIUM:
                    return new Date(Long.MAX_VALUE); // unlimited
                case SHAREWARE:
                    return new Date(new Date().getTime() + TimeUnit.DAYS.toMillis(30));
            }

        }
        return new Date(new Date().getTime() + TimeUnit.DAYS.toMillis(300));
    }

    private LdapLicenseId getLicenseId(String licenseId) {
        try {
            final LdapLicenseId byId = licenseIdService.getById(licenseId);
            if (byId != null) {
                return byId;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.error("Failed to find License ID with id: " + licenseId);
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Failed to find License ID with id: " + licenseId).build());
    }

    private String generatedLicenseAsString(String licenseId) {
        return Jackson.asJsonSilently(generateLicense(licenseId));
    }

    @GET
    @Path("/generate")
    @Produces({MediaType.APPLICATION_JSON})
    public Response generateGet(@QueryParam("licenseId") String licenseId, @Context HttpServletRequest httpRequest) {
        return Response.ok().entity(generatedLicenseAsString(licenseId)).build();
    }

    @POST
    @Path("/generate")
    @Produces({MediaType.APPLICATION_JSON})
    public Response generatePost(@FormParam("licenseId") String licenseId, @Context HttpServletRequest httpRequest) {
        return Response.ok().entity(generatedLicenseAsString(licenseId)).build();
    }

}
