package io.jans.fido2.service.processor.attestation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.util.AppleUtilService;
import io.jans.fido2.service.util.CommonUtilService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.codec.digest.DigestUtils;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;

/**
 * For Apple's anonymous attestation fmt="apple"
 *
 * @author madhumitas
 *
 */
@ApplicationScoped
public class AppleAttestationProcessor implements AttestationFormatProcessor {
	@Inject
	private Logger log;

	@Inject
    private CommonVerifiers commonVerifiers;
	
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

	@Inject
	private CommonUtilService commonUtilService;

	@Inject
	private AppleUtilService appleUtilService;

	private static final String SUBJECT_DN = "st=california, o=apple inc., cn=apple webauthn root ca";

	@Override
	public AttestationFormat getAttestationFormat() {
		return AttestationFormat.apple;
	}

	// @Override
	public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData credential, byte[] clientDataHash,
			CredAndCounterData credIdAndCounters) {

		log.info("AttStmt: " + attStmt.asText());
		
		
		// Check attStmt and it contains "x5c" then its a FULL attestation.
		if (attStmt.hasNonNull("x5c")) {

			// 1. Verify |x5c| is a valid certificate chain starting from the |credCert|
			// (the first certificate in x5c) to
			// the Apple WebAuthn root certificate.

			Iterator<JsonNode> i = attStmt.get("x5c").elements();
			List<X509Certificate> certificates = new ArrayList<X509Certificate>();

			while (i.hasNext()) {
				String c = i.next().asText();
				X509Certificate cert = certificateService.getCertificate(c);
				certificates.add(cert);
			}

			if (certificates.isEmpty()) {
				throw errorResponseFactory.badRequestException(AttestationErrorResponseType.APPLE_ERROR, "x5c certificates is empty");
			}

			// the first certificate in x5c
			X509Certificate credCert = certificates.get(0);

			try {
				List<X509Certificate> trustAnchorCertificates = attestationCertificateService.getRootCertificatesBySubjectDN(SUBJECT_DN);
				log.debug("APPLE_WEBAUTHN_ROOT_CA root certificate: " + trustAnchorCertificates.size());
				X509Certificate verifiedCert = certificateVerifier.verifyAttestationCertificates(certificates, trustAnchorCertificates);
				log.info("Step 1 completed");
			} catch (Fido2RuntimeException e) {
//					X509Certificate certificate = certificates.get(0);
				String issuerDN = credCert.getIssuerDN().getName();
				log.warn("Failed to find attestation validation signature public certificate with DN: '{}'", issuerDN);
				throw errorResponseFactory.badRequestException(AttestationErrorResponseType.APPLE_ERROR,
						"Failed to find attestation validation signature public certificate with DN: " + issuerDN);
			}


			// 2. Concatenate |authenticatorData| and |clientDataHash| to form
			// |nonceToHash|.

			byte[] authDataInBytes = authData.getAuthDataDecoded();
			// byte[] nonceToHash = new byte[authDataInBytes.length +
			// clientDataHash.length];

			ByteArrayOutputStream baos;
			try {
				baos = commonUtilService.writeOutputStreamByteList(Arrays.asList(authDataInBytes, clientDataHash));
			} catch (IOException e) {
				throw errorResponseFactory.badRequestException(AttestationErrorResponseType.APPLE_ERROR,
						"Concatenate |authenticatorData| and |clientDataHash| to form |nonceToHash| : " + e.getMessage());
			}

			byte[] nonceToHash = baos.toByteArray();
			log.info("Step 2 completed");

			// 3. Perform SHA-256 hash of |nonceToHash| to produce |nonce|.
			byte[] nonce = DigestUtils.getSha256Digest().digest(nonceToHash);
			log.info("Step 3 completed");

			// 4. Verify |nonce| matches the value of the extension with OID (
			// 1.2.840.113635.100.8.2 ) in |credCert|.

			byte[] attestationChallenge = appleUtilService.getExtension(credCert);

			if (!Arrays.equals(nonce, attestationChallenge)) {
				throw errorResponseFactory.badRequestException(AttestationErrorResponseType.APPLE_ERROR, "Certificate 1.2.840.113635.100.8.2 extension does not match nonce");
			} else
				log.info("Step 4 completed");

			// 5. Verify credential public key matches the Subject Public Key of
			// |credCert|.

			PublicKey publicKeyAuthData = coseService.getPublicKeyFromUncompressedECPoint(authData.getCosePublicKey());

			PublicKey publicKeyCredCert = credCert.getPublicKey();

			if (!publicKeyAuthData.equals(publicKeyCredCert)) {
				throw errorResponseFactory.badRequestException(AttestationErrorResponseType.APPLE_ERROR,
						"The public key in the first certificate in x5c doesn't matches the credentialPublicKey in the attestedCredentialData in authenticatorData.");
			} else {
				log.info("Step 5 completed");
			}

			// this is Gluu implementation specific where setUncompressedEcPoint is saved in
			// LDAP registrationData and read during assertion
			credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());
			credIdAndCounters.setCredId(base64Service.urlEncodeToString(authData.getCredId()));
			credIdAndCounters.setUncompressedEcPoint(base64Service.urlEncodeToString(authData.getCosePublicKey()));
			// log.info("attStmt.get(\"alg\")"+attStmt.get("alg"));
			int alg = commonVerifiers.verifyAlgorithm(attStmt.get("alg"), authData.getKeyType());
			credIdAndCounters.setSignatureAlgorithm(alg);
			credIdAndCounters.setAuthenticatorName(attestationCertificateService.getAttestationAuthenticatorName(authData));
		}
	}
}
