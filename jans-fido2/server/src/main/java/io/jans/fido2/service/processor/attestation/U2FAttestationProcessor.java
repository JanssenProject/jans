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

import java.io.IOException;
import java.security.PublicKey;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.exception.Fido2MissingAttestationCertException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.UserVerificationVerifier;

import org.bouncycastle.cert.X509CertificateHolder;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Attestation processor for attestations of fmt =fido-u2f
 *
 */
@ApplicationScoped
public class U2FAttestationProcessor implements AttestationFormatProcessor {

	@Inject
	private Logger log;

	@Inject
	private CommonVerifiers commonVerifiers;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private AuthenticatorDataVerifier authenticatorDataVerifier;

	@Inject
	private UserVerificationVerifier userVerificationVerifier;

	@Inject
	private AttestationCertificateService attestationCertificateService;

	@Inject
	private CertificateVerifier certificateVerifier;

	@Inject
	private CoseService coseService;

	@Inject
	private Base64Service base64Service;

	@Inject
	private CertificateService certificateService;

	@Inject
	private ErrorResponseFactory errorResponseFactory;

	@Override
	public AttestationFormat getAttestationFormat() {
		return AttestationFormat.fido_u2f;
	}

	@Override
	public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData registration, byte[] clientDataHash,
			CredAndCounterData credIdAndCounters) {
		int alg = -7;

		String signature = commonVerifiers.verifyBase64String(attStmt.get("sig"));
		commonVerifiers.verifyAAGUIDZeroed(authData);

		userVerificationVerifier.verifyUserPresent(authData);
		commonVerifiers.verifyRpIdHash(authData, registration.getOrigin());

		// if attestation mode is enabled in the global config
		if (appConfiguration.getFido2Configuration().getAttestationMode()
				.equalsIgnoreCase(AttestationMode.DISABLED.getValue()) == false) {
			if (attStmt.hasNonNull("x5c")) {
				Iterator<JsonNode> i = attStmt.get("x5c").elements();
				ArrayList<String> certificatePath = new ArrayList<>();
				while (i.hasNext()) {
					certificatePath.add(i.next().asText());
				}
				List<X509Certificate> certificates = certificateService.getCertificates(certificatePath);

				credIdAndCounters.setSignatureAlgorithm(alg);
				JsonNode metaData = null;
				try {
					// Get the first certificate from the chain
					for (X509Certificate cert : certificates) {
						X509CertificateHolder certificateHolder = convertToX509CertificateHolder(cert);
						// Extract Subject Key Identifier
						Extensions extensions = certificateHolder.getExtensions();

						Extension ext = certificateHolder.getExtension(Extension.subjectKeyIdentifier);

						if (extensions != null) {
							log.debug(extensions.getExtensionOIDs().toString());
							for (ASN1ObjectIdentifier oid : extensions.getExtensionOIDs()) {
								log.debug("oid : " + oid.getId() + " : " + Extension.subjectKeyIdentifier.getId());
							}
						} else {
							System.out.println("No extensions found in the certificate.");
						}

						if (ext != null) {
							byte[] ski = certificateHolder.getExtension(Extension.subjectKeyIdentifier).getExtnValue()
									.getEncoded();
							log.debug("Attestation Certificate Key Identifier:" + bytesToHex(ski));
							metaData = attestationCertificateService.getMetadataForU2fAuthenticator(bytesToHex(ski));
							break;
						}

					}

					if (metaData == null) {
						log.debug("ski not present in MDS3");
					}

					List<X509Certificate> trustAnchorCertificates = attestationCertificateService
							.getAttestationRootCertificates(metaData, certificates);

					X509Certificate verifiedCert = certificateVerifier.verifyAttestationCertificates(certificates,
							trustAnchorCertificates);
					authenticatorDataVerifier.verifyU2FAttestationSignature(authData, clientDataHash, signature,
							verifiedCert, alg);
					log.debug("Completed  verification of signature verifyU2FAttestationSignature ");
				} catch (Fido2MissingAttestationCertException ex) {
					if (!certificates.isEmpty()) {
						X509Certificate certificate = certificates.get(0);
						String issuerDN = certificate.getIssuerDN().getName();
						log.warn("Failed to find attestation validation signature public certificate with DN: '{}'",
								issuerDN);
					}
					throw errorResponseFactory.badRequestException(AttestationErrorResponseType.FIDO_U2F_ERROR,
							"Error on verify attestation mds: " + ex.getMessage());
				} catch (Exception e) {
					log.warn("Failed to find Attestation Certificate Key Identifier:", e.getMessage());
					throw errorResponseFactory . badRequestException(AttestationErrorResponseType.INVALID_CERTIFICATE,
							"Error on verify attestation mds: " + e.getMessage());
				}

			} else if (attStmt.hasNonNull("ecdaaKeyId")) {
				String ecdaaKeyId = attStmt.get("ecdaaKeyId").asText();
				log.warn("Fido-U2F unsupported EcdaaKeyId: {}", ecdaaKeyId);
				throw errorResponseFactory.badRequestException(AttestationErrorResponseType.FIDO_U2F_ERROR,
						"ecdaaKeyId is not supported");
			} else {
				PublicKey publicKey = coseService.getPublicKeyFromUncompressedECPoint(authData.getCosePublicKey());
				authenticatorDataVerifier.verifyPackedSurrogateAttestationSignature(authData.getAuthDataDecoded(),
						clientDataHash, signature, publicKey, alg);
			}
		} else {
			log.debug(
					"In Global fido configuration, AttestationMode is DISABLED, hence skipping the attestation check");
		}

		credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());
		credIdAndCounters.setCredId(base64Service.urlEncodeToString(authData.getCredId()));
		credIdAndCounters.setUncompressedEcPoint(base64Service.urlEncodeToString(authData.getCosePublicKey()));
		//credIdAndCounters.setAuthenticatorName(attestationCertificateService.getAttestationAuthenticatorName(authData));
	}

	// Convert X509Certificate to X509CertificateHolder
	public static X509CertificateHolder convertToX509CertificateHolder(X509Certificate certificate) throws Exception {
		byte[] encoded = certificate.getEncoded();
	    if (encoded == null) {
	        throw new IllegalArgumentException("Certificate encoding is null");
	    }
		return new X509CertificateHolder(certificate.getEncoded());
	}

	// Convert byte array to Hexadecimal string
	private static String bytesToHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder();
		for (byte b : bytes) {
			hex.append(String.format("%02X", b));
		}
		return hex.toString();
	}
}
