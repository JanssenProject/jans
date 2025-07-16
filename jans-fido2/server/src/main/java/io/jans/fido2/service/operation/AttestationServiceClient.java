package io.jans.fido2.service.operation;

import io.jans.fido2.model.attestation.AttestationResult;
import io.jans.fido2.model.common.AttestationOrAssertionResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public interface AttestationServiceClient {

    @POST
    @Path("/result") // Updated from /verify to /result
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    AttestationOrAssertionResponse verify(AttestationResult attestationResult);

}
