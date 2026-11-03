/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.readiness.AssociationFileExporter;
import io.jans.fido2.service.readiness.NativeReadinessService.NotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Exports the native app association files of one configured relying party, to be deployed by the admin on
 * the RP-ID host's {@code /.well-known/} path: {@code GET /rp/{rpConfigId}/association/assetlinks.json} and
 * {@code GET /rp/{rpConfigId}/association/apple-app-site-association}.
 * <p>
 * {@code rpConfigId} is the RP id exactly as configured, as a URL-encoded path segment. The answer is 404 for
 * an id that is not configured, and also when the RP has no app of that platform, since there is nothing to
 * deploy. The body is the file itself, sent as a download; the Apple file's name has no extension, as Apple
 * requires. These endpoints only read configuration and make no outbound request.
 * <p>
 * SECURITY NOTE: as with {@link NativeReadinessController}, authentication and authorization are expected to
 * be enforced at the infrastructure level or via the Config API fido2 plugin, not here.
 *
 * @author Janssen Project
 */
@ApplicationScoped
@Path("/rp/{rpConfigId}/association")
public class AssociationFileController {

    @Inject
    private AssociationFileExporter exporter;

    @Inject
    private DataMapperService dataMapperService;

    @GET
    @Path("/assetlinks.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response assetLinks(@PathParam("rpConfigId") String rpConfigId)
            throws JsonProcessingException {
        try {
            return download(exporter.assetLinks(rpConfigId), "assetlinks.json");
        } catch (NotFound e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/apple-app-site-association")
    @Produces(MediaType.APPLICATION_JSON)
    public Response appleAppSiteAssociation(@PathParam("rpConfigId") String rpConfigId)
            throws JsonProcessingException {
        try {
            return download(exporter.appleAppSiteAssociation(rpConfigId), "apple-app-site-association");
        } catch (NotFound e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private Response download(JsonNode file, String fileName) throws JsonProcessingException {
        return Response.ok(dataMapperService.writeValueAsString(file))
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"").build();
    }
}
