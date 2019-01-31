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

package org.gluu.oxauth.fido2.service.mds;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.codec.binary.Hex;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.service.Base64Service;
import org.gluu.oxauth.fido2.service.DataMapperService;
import org.gluu.oxauth.fido2.service.processors.impl.ResteasyClientFactory;
import org.gluu.oxauth.fido2.service.verifier.CommonVerifiers;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.slf4j.Logger;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.configuration.Fido2Configuration;
import org.xdi.util.StringHelper;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class MdsService {

    @Inject
    private Logger log;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private MdsTocService mdsTocService;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private Base64Service base64Service;

    @Inject
    private ResteasyClientFactory resteasyClientFactory;

    @Inject
    private AppConfiguration appConfiguration;

    public JsonNode fetchMetadata(byte[] aaguidBuffer) {
        String aaguid = deconvert(aaguidBuffer);

        JsonNode tocEntry = mdsTocService.getAuthenticatorsMetadata(aaguid);
        if (tocEntry == null) {
            throw new Fido2RPRuntimeException("Authenticator not in TOC aaguid " + aaguid);
        }

        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if (fido2Configuration == null) {
            throw new Fido2RPRuntimeException("Fido2 configuration not exists");
        }

        String mdsAccessToken = fido2Configuration.getMdsAccessToken();
        if (StringHelper.isEmpty(mdsAccessToken)) {
            throw new Fido2RPRuntimeException("Fido2 MDS access token should be set");
        }

        URI metadataUrl;
        try {
            metadataUrl = new URI(String.format("%s/?token=%s", tocEntry.get("url").asText(), mdsAccessToken));
            log.info("Authenticator AAGUI {} url metadataUrl {} downloaded", aaguid, metadataUrl);
        } catch (URISyntaxException e) {
            throw new Fido2RPRuntimeException("Invalid URI in TOC aaguid " + aaguid);
        }

        verifyTocEntryStatus(aaguid, tocEntry);
        String metadataHash = commonVerifiers.verifyThatString(tocEntry, "hash");

        log.info("Reaching MDS at {}", metadataUrl.toString());

        ResteasyClient resteasyClient = resteasyClientFactory.buildResteasyClient();
        Response response = resteasyClient.target(metadataUrl).request().header("Content-Type", MediaType.APPLICATION_JSON).get();
        String body = response.readEntity(String.class);

        StatusType status = response.getStatusInfo();
        log.info("Response from resource server {}", status);
        if (status.getFamily() == Status.Family.SUCCESSFUL) {
            byte[] bodyBuffer;
            try {
                bodyBuffer = body.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new Fido2RPRuntimeException("Unable to verify metadata hash for aaguid " + deconvert(aaguidBuffer));
            }
            byte[] digest = mdsTocService.getDigester().digest(bodyBuffer);
            if (!Arrays.equals(digest, base64Service.urlDecode(metadataHash))) {
                throw new Fido2RPRuntimeException("Unable to verify metadata hash for aaguid " + deconvert(aaguidBuffer));
            }

            try {
                return dataMapperService.readTree(base64Service.urlDecode(body));
            } catch (IOException e) {
                log.warn("Can't parse payload from the server ");
                throw new Fido2RPRuntimeException("Unable to parse payload from server for aaguid " + deconvert(aaguidBuffer));
            }
        } else {
            throw new Fido2RPRuntimeException("Unable to retrieve metadata for aaguid " + deconvert(aaguidBuffer) + " status " + status);
        }
    }

    private void verifyTocEntryStatus(String aaguid, JsonNode tocEntry) {
        JsonNode statusReports = tocEntry.get("statusReports");

        Iterator<JsonNode> iter = statusReports.elements();
        while (iter.hasNext()) {
            JsonNode statusReport = iter.next();
            AuthenticatorStatus authenticatorStatus = AuthenticatorStatus.valueOf(statusReport.get("status").asText());
            String authenticatorEffectiveDate = statusReport.get("effectiveDate").asText();
            log.info("Authenticator AAGUI {} status {} effective date {}", aaguid, authenticatorStatus, authenticatorEffectiveDate);
            verifyStatusAcceptable(aaguid, authenticatorStatus);
        }
    }

    private String deconvert(byte[] aaguidBuffer) {
        return Hex.encodeHexString(aaguidBuffer).replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)",
                "$1-$2-$3-$4-$5");
    }

    private void verifyStatusAcceptable(String aaguid, AuthenticatorStatus status) {
        final List<AuthenticatorStatus> undesiredAuthenticatorStatus = Arrays
                .asList(new AuthenticatorStatus[] { AuthenticatorStatus.USER_VERIFICATION_BYPASS, AuthenticatorStatus.ATTESTATION_KEY_COMPROMISE,
                        AuthenticatorStatus.USER_KEY_REMOTE_COMPROMISE, AuthenticatorStatus.USER_KEY_PHYSICAL_COMPROMISE,
                        AuthenticatorStatus.ATTESTATION_KEY_COMPROMISE });
        if (undesiredAuthenticatorStatus.contains(status)) {
            throw new Fido2RPRuntimeException("Authenticator " + aaguid + "status undesirable " + status);
        }

    }
}
