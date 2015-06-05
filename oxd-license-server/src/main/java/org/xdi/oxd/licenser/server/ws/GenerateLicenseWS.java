package org.xdi.oxd.licenser.server.ws;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.client.Jackson;
import org.xdi.oxd.license.client.data.LicenseResponse;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.license.client.js.LicenseMetadata;
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
import java.io.IOException;
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

            LdapLicenseCrypt licenseCrypt = getLicenseCrypt(licenseId.getLicenseCryptDN(), licenseIdStr);

            final LicenseMetadata metadata = Jackson.createJsonMapper().readValue(licenseId.getMetadata(), LicenseMetadata.class);

            if (licenseId.getLicensesIssuedCount() >= metadata.getLicenseCountLimit()) {
                LOG.debug("License ID count limit exceeded, licenseId: " + licenseIdStr);
                throw new WebApplicationException(
                        Response.status(Response.Status.BAD_REQUEST).entity("License ID count limit exceeded.").
                                build());
            }

            final Date expiredAt = metadata.getExpirationDate() != null ? metadata.getExpirationDate() : licenseExpirationDate(licenseId);
            final Date now = new Date();

            if (!now.before(expiredAt)) {
                LOG.debug("License ID is expired, licenseId: " + licenseIdStr + ", expiredAt: " + expiredAt + ", now: " + now);
                throw new WebApplicationException(
                        Response.status(Response.Status.BAD_REQUEST).entity("License ID expires.").
                                build());
            }

            LicenseGeneratorInput input = new LicenseGeneratorInput();
            input.setExpiredAt(expiredAt);
            input.setCrypt(licenseCrypt);
            input.setMetadata(licenseId.getMetadata());

            final LicenseResponse licenseResponse = licenseGenerator.generate(input);
            updateLicenseId(licenseId);
            return licenseResponse;
        } catch (InvalidKeySpecException e) {
            throw new WebApplicationException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new WebApplicationException(e);
        } catch (JsonMappingException e) {
            throw new WebApplicationException(e);
        } catch (JsonParseException e) {
            throw new WebApplicationException(e);
        } catch (IOException e) {
            throw new WebApplicationException(e);
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
            switch (licenseId.getMetadataAsObject().getLicenseType()) {
                case FREE:
                case PAID:
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
            if (!Strings.isNullOrEmpty(licenseId)) {
                final LdapLicenseId byId = licenseIdService.getById(licenseId);
                if (byId != null) {
                    return byId;
                }
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
