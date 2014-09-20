package org.xdi.oxd.licenser.server.ws;

import com.google.inject.Inject;
import org.xdi.oxd.license.client.Jackson;
import org.xdi.oxd.license.client.data.License;
import org.xdi.oxd.licenser.server.LicenseGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/09/2014
 */

@Path("/rest")
public class GenerateLicenseWS {

    @Inject
    LicenseGenerator licenseGenerator;

    private License generateLicense() {
        try {
            return licenseGenerator.generate();
        } catch (InvalidKeySpecException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } catch (NoSuchAlgorithmException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private String generatedLicenseAsString() {
        return Jackson.asJsonSilently(generateLicense());
    }

    @GET
    @Path("/generate")
    @Produces({MediaType.APPLICATION_JSON})
    public Response generateGet(@Context HttpServletRequest httpRequest) {
        return Response.ok().entity(generatedLicenseAsString()).build();
    }

    @POST
    @Path("/generate")
    @Produces({MediaType.APPLICATION_JSON})
    public Response generatePost(@Context HttpServletRequest httpRequest) {
        return Response.ok().entity(generatedLicenseAsString()).build();
    }

}
