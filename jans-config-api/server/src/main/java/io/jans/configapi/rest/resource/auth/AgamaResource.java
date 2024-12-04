/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.configapi.rest.resource.auth;

import io.jans.agama.dsl.Transpiler;
import io.jans.agama.dsl.TranspilerException;
import io.jans.agama.dsl.error.SyntaxException;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path(ApiConstants.AGAMA)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgamaResource extends ConfigBaseResource {

    @Operation(summary = "Determine if the text passed is valid Agama code", description = "Determine if the text passed is valid Agama code", operationId = "agama-syntax-check", tags = {
            "Agama - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.AGAMA_READ_ACCESS, ApiAccessConstants.AGAMA_WRITE_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agama Syntax Check message", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Exception.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.AGAMA_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path("/syntax-check/" + ApiConstants.QNAME_PATH)
    public Response doSyntaxCheck(@Parameter(description = "Agama Flow name") @PathParam(ApiConstants.QNAME) String qname, String source) {

        Exception e = null;
        try {
            Transpiler.runSyntaxCheck(qname, source);
            e = new TranspilerException("");
        } catch (SyntaxException | TranspilerException te) {
            logger.info("Syntax check failed");
            e = te;
        }
        e.setStackTrace(new StackTraceElement[0]);
        return Response.ok().entity(e).build();
    }

}
