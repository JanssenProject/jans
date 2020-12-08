/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

/*
 * Copyright (c) 2018 Mastercard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jans.fido2.service.mds;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.codec.binary.Hex;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.mds.AuthenticatorCertificationStatus;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.client.ResteasyClientFactory;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.util.StringHelper;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class MdsService {

    @Inject
    private Logger log;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private TocService tocService;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private Base64Service base64Service;

    @Inject
    private ResteasyClientFactory resteasyClientFactory;

    @Inject
    private AppConfiguration appConfiguration;

    private Map<String, JsonNode> mdsEntries;

    public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
        this.mdsEntries = Collections.synchronizedMap(new HashMap<String, JsonNode>());
    }

    public JsonNode fetchMetadata(byte[] aaguidBuffer) {
        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if (fido2Configuration == null) {
            throw new Fido2RuntimeException("Fido2 configuration not exists");
        }

        String mdsAccessToken = fido2Configuration.getMdsAccessToken();
        if (StringHelper.isEmpty(mdsAccessToken)) {
            throw new Fido2RuntimeException("Fido2 MDS access token should be set");
        }

        String aaguid = deconvert(aaguidBuffer);
        
        JsonNode mdsEntry = mdsEntries.get(aaguid);
        if (mdsEntry != null) {
            log.debug("Get MDS by aaguid {} from cache", aaguid);
            return mdsEntry;
        }

        JsonNode tocEntry = tocService.getAuthenticatorsMetadata(aaguid);
        if (tocEntry == null) {
            throw new Fido2RuntimeException("Authenticator not in TOC aaguid " + aaguid);
        }

        String tocEntryUrl = tocEntry.get("url").asText();
        URI metadataUrl;
        try {
            metadataUrl = new URI(String.format("%s/?token=%s", tocEntryUrl, mdsAccessToken));
            log.debug("Authenticator AAGUI {} url metadataUrl {} downloaded", aaguid, metadataUrl);
        } catch (URISyntaxException e) {
            throw new Fido2RuntimeException("Invalid URI in TOC aaguid " + aaguid);
        }

        verifyTocEntryStatus(aaguid, tocEntry);
        String metadataHash = commonVerifiers.verifyThatFieldString(tocEntry, "hash");

        log.debug("Reaching MDS at {}", tocEntryUrl);

        mdsEntry = downloadMdsFromServer(aaguid, metadataUrl, metadataHash);

        mdsEntries.put(aaguid, mdsEntry);
        
        return mdsEntry;
    }

	private JsonNode downloadMdsFromServer(String aaguid, URI metadataUrl, String metadataHash) {
		ResteasyClient resteasyClient = resteasyClientFactory.buildResteasyClient();
        Response response = resteasyClient.target(metadataUrl).request().header("Content-Type", MediaType.APPLICATION_JSON).get();
        String body = response.readEntity(String.class);

        StatusType status = response.getStatusInfo();
        log.debug("Response from resource server {}", status);
        if (status.getFamily() == Status.Family.SUCCESSFUL) {
            byte[] bodyBuffer;
            try {
                bodyBuffer = body.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new Fido2RuntimeException("Unable to verify metadata hash for aaguid " + aaguid);
            }

            byte[] digest = tocService.getDigester().digest(bodyBuffer);
            if (!Arrays.equals(digest, base64Service.urlDecode(metadataHash))) {
                throw new Fido2RuntimeException("Unable to verify metadata hash for aaguid " + aaguid);
            }

            try {
            	return dataMapperService.readTree(base64Service.urlDecode(body));
            } catch (IOException e) {
                log.error("Can't parse payload from the server");
                throw new Fido2RuntimeException("Unable to parse payload from server for aaguid " + aaguid);
            }
        } else {
            throw new Fido2RuntimeException("Unable to retrieve metadata for aaguid " + aaguid + " status " + status);
        }
	}

    private void verifyTocEntryStatus(String aaguid, JsonNode tocEntry) {
        JsonNode statusReports = tocEntry.get("statusReports");

        Iterator<JsonNode> iter = statusReports.elements();
        while (iter.hasNext()) {
            JsonNode statusReport = iter.next();
            AuthenticatorCertificationStatus authenticatorStatus = AuthenticatorCertificationStatus.valueOf(statusReport.get("status").asText());
            String authenticatorEffectiveDate = statusReport.get("effectiveDate").asText();
            log.debug("Authenticator AAGUI {} status {} effective date {}", aaguid, authenticatorStatus, authenticatorEffectiveDate);
            verifyStatusAcceptable(aaguid, authenticatorStatus);
        }
    }

    private String deconvert(byte[] aaguidBuffer) {
        return Hex.encodeHexString(aaguidBuffer).replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)",
                "$1-$2-$3-$4-$5");
    }

    private void verifyStatusAcceptable(String aaguid, AuthenticatorCertificationStatus status) {
        final List<AuthenticatorCertificationStatus> undesiredAuthenticatorStatus = Arrays
                .asList(new AuthenticatorCertificationStatus[] { AuthenticatorCertificationStatus.USER_VERIFICATION_BYPASS, AuthenticatorCertificationStatus.ATTESTATION_KEY_COMPROMISE,
                        AuthenticatorCertificationStatus.USER_KEY_REMOTE_COMPROMISE, AuthenticatorCertificationStatus.USER_KEY_PHYSICAL_COMPROMISE,
                        AuthenticatorCertificationStatus.ATTESTATION_KEY_COMPROMISE });
        if (undesiredAuthenticatorStatus.contains(status)) {
            throw new Fido2RuntimeException("Authenticator " + aaguid + "status undesirable " + status);
        }

    }
    
    public void clear() {
        this.mdsEntries.clear();
    }

}
