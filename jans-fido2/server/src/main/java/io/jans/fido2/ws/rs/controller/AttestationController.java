/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import org.slf4j.Logger;

import io.jans.fido2.model.attestation.AttestationOptions;
import io.jans.fido2.model.attestation.AttestationResult;
import io.jans.fido2.model.attestation.PublicKeyCredentialCreationOptions;
import io.jans.fido2.model.common.AttestationOrAssertionResponse;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.operation.AttestationService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * serves request for /attestation endpoint exposed by FIDO2 sever
 *
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
@Path("/attestation")
public class AttestationController {

	@Inject
	private Logger log;

	@Inject
	private AttestationService attestationService;

	@Inject
	private DataMapperService dataMapperService;

	@Inject
	private CommonVerifiers commonVerifiers;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private ErrorResponseFactory errorResponseFactory;

	@POST
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	@Path("/options")
	public Response register(@NotNull AttestationOptions attestationOptions) {
		return processRequest(() -> {
			if (appConfiguration.getFido2Configuration() == null) {
				throw errorResponseFactory.forbiddenException();
			}
			PublicKeyCredentialCreationOptions result = attestationService.options(attestationOptions);
			return Response.ok().entity(result).build();
		});
	}

	@POST
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	@Path("/result")
	public Response verify(@NotNull AttestationResult attestationResult) {
		return processRequest(() -> {
			if (appConfiguration.getFido2Configuration() == null) {
				throw errorResponseFactory.forbiddenException();
			}
			AttestationOrAssertionResponse result = attestationService.verify(attestationResult);
			return Response.ok().entity(result).build();
		});
	}

	private Response processRequest(RequestProcessor processor) {
		try {
			return processor.process();
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			log.error("Unknown Error: {}", e.getMessage(), e);
			throw errorResponseFactory.unknownError(e.getMessage());
		}
	}

	@FunctionalInterface
	private interface RequestProcessor {
		Response process() throws Exception;
	}
}
