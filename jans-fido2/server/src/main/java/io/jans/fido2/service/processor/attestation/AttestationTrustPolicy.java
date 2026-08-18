/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.processor.attestation;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.model.error.ErrorResponseFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Centralizes how attestation-trust failures are handled per attestation mode.
 *
 * <p>Core cryptographic and request-integrity checks (signature, challenge, RP-ID, client data)
 * are always enforced by the callers regardless of mode. This policy governs only attestation
 * provenance/trust failures (MDS lookup, certificate-chain trust, self-signed, certificate
 * requirements, AAGUID extension, root cert, REVOKED status):
 * <ul>
 *   <li><b>enforced</b> - the failure is rejected (an exception is thrown).</li>
 *   <li><b>monitor</b> (the default) / <b>disabled</b> - the failure is logged, the credential is
 *       marked as not-trusted, and registration is allowed to complete cleanly.</li>
 * </ul>
 */
@ApplicationScoped
public class AttestationTrustPolicy {

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private ErrorResponseFactory errorResponseFactory;

	/**
	 * @return true when the configured attestation mode is "enforced".
	 */
	public boolean isEnforced() {
		return AttestationMode.ENFORCED.getValue()
				.equalsIgnoreCase(appConfiguration.getFido2Configuration().getAttestationMode());
	}

	/**
	 * Handle an attestation-trust failure according to the configured mode.
	 *
	 * <p>In "enforced" mode this throws and registration is rejected. In any other mode the failure
	 * is logged, the registration data is flagged as not-trusted, and control returns to the caller
	 * so the credential can still be registered.
	 *
	 * @param type     the attestation error type used when rejecting in enforced mode
	 * @param message  human-readable reason for the trust failure
	 * @param authData the parsed authenticator data (used for the AAGUID in the log line)
	 * @param out      the registration data being populated; marked not-trusted when tolerated
	 */
	public void onTrustFailure(AttestationErrorResponseType type, String message, AuthData authData,
			CredAndCounterData out) {
		if (isEnforced()) {
			throw errorResponseFactory.badRequestException(type, message);
		}
		String aaguid = (authData != null && authData.getAaguid() != null)
				? Hex.encodeHexString(authData.getAaguid()) : "unknown";
		log.warn("Attestation not trusted (mode=monitor), aaguid={}: {}", aaguid, message);
		if (out != null) {
			out.setAttestationTrusted(false);
		}
	}
}
