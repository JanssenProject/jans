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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.mds.AuthenticatorCertificationStatus;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.client.ResteasyClientFactory;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.service.cdi.event.ApplicationInitialized;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

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

		verifyTocEntryStatus(aaguid, tocEntry);

		return tocEntry;
	}

	

	private void verifyTocEntryStatus(String aaguid, JsonNode tocEntry) {
		JsonNode statusReports = tocEntry.get("statusReports");

		Iterator<JsonNode> iter = statusReports.elements();
		while (iter.hasNext()) {
			JsonNode statusReport = iter.next();
			AuthenticatorCertificationStatus authenticatorStatus = AuthenticatorCertificationStatus
					.valueOf(statusReport.get("status").asText());
			String authenticatorEffectiveDate = statusReport.get("effectiveDate").asText();
			log.debug("Authenticator AAGUID {} status {} effective date {}", aaguid, authenticatorStatus,
					authenticatorEffectiveDate);
			verifyStatusAcceptable(aaguid, authenticatorStatus);
		}
	}

	private String deconvert(byte[] aaguidBuffer) {
		return Hex.encodeHexString(aaguidBuffer).replaceFirst(
				"([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
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