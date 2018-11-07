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

package org.gluu.oxauth.fido2.attestation;

import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.databind.JsonNode;

@Path("/fido2/attestation")
class AttestationController {
    @Inject
    AttestationService attestationService;

    @Inject
    @Named("base64UrlEncoder")
    private Base64.Encoder base64UrlEncoder;

    @Inject
    @Named("base64UrlDecoder")
    private Base64.Decoder base64UrlDecoder;

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/options")
    JsonNode register(JsonNode params) {
        return attestationService.options(params);
    }

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/result")
    JsonNode verify(JsonNode params) {
        return attestationService.verify(params);
    }

}
