/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2018 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.oxauth.fido2.ws.rs.controller;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.gluu.oxauth.fido2.service.DataMapperService;
import org.gluu.oxauth.fido2.ws.rs.service.AttestationService;
import org.xdi.oxauth.model.configuration.AppConfiguration;

import com.fasterxml.jackson.databind.JsonNode;

@Path("/fido2/attestation")
@ApplicationScoped
public class AttestationController {

    @Inject
    private AttestationService attestationService;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private AppConfiguration appConfiguration;

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/options")
    public Response register(String content) throws IOException {
        if ((appConfiguration.getFido2Configuration() == null) || appConfiguration.getFido2Configuration().isDisable()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        JsonNode params = dataMapperService.readTree(content);
        JsonNode result = attestationService.options(params);

        ResponseBuilder builder = Response.ok().entity(result.toString());
        return builder.build();
    }

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/result")
    public Response verify(String content) throws IOException {
        if ((appConfiguration.getFido2Configuration() == null) || appConfiguration.getFido2Configuration().isDisable()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        JsonNode params = dataMapperService.readTree(content);
        JsonNode result = attestationService.verify(params);

        ResponseBuilder builder = Response.ok().entity(result.toString());
        return builder.build();
    }
}
