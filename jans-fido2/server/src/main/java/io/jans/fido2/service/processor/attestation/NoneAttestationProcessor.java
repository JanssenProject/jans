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

package io.jans.fido2.service.processor.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

/**
 * Attestation processor for attestations of fmt = none One of the attestation
 * formats called 'none'. When you getting it, that means two things:
 * <p>
 * 1. You really don't need attestation, and so you are deliberately ignoring
 * it.
 * <p>
 * 2. You forgot to set attestation flag to 'direct' when making credential.
 * <p>
 * If you are getting attestation with fmt set to none, then no attestation is
 * provided, and you don't have anything to verify. Simply extract user relevant
 * information as specified below and save it to the database.
 */
@ApplicationScoped
public class NoneAttestationProcessor implements AttestationFormatProcessor {

	@Inject
	private Logger log;

	@Inject
	private Base64Service base64Service;

	@Inject
	private CommonVerifiers commonVerifiers;

	@Override
	public AttestationFormat getAttestationFormat() {
		return AttestationFormat.none;
	}

	@Override
	public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData credential, byte[] clientDataHash,
			CredAndCounterData credIdAndCounters) {
		
		log.debug("None attestation {}", attStmt);
		if (!attStmt.isEmpty()) {
			log.error("Problem with None attestation");
			throw new Fido2RuntimeException("Problem with None attestation");
		}

		
		credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());
		
		log.debug("authData : " + authData);
		credIdAndCounters.setCredId(base64Service.urlEncodeToString(authData.getCredId()));
		credIdAndCounters.setUncompressedEcPoint(base64Service.urlEncodeToString(authData.getCosePublicKey()));
		
		log.debug("Algorithm : " + authData.getKeyType());
		credIdAndCounters.setSignatureAlgorithm(authData.getKeyType());
		
		log.debug("credIdAndCounters" + credIdAndCounters.toString());

	}
}
