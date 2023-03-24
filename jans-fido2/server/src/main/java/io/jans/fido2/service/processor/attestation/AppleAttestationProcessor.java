package io.jans.fido2.service.processor.attestation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.kerby.asn1.parse.Asn1Container;
import org.apache.kerby.asn1.parse.Asn1ParseResult;
import org.apache.kerby.asn1.parse.Asn1Parser;
import org.apache.kerby.asn1.type.Asn1OctetString;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.exception.AttestationException;
import io.jans.fido2.exception.Fido2MissingAttestationCertException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.UserVerificationVerifier;

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

	private final String KEY_DESCRIPTION_OID = "1.2.840.113635.100.8.2";

	@Override
	public AttestationFormat getAttestationFormat() {
		return AttestationFormat.apple;
	}

	/**
	 * Apple WebAuthn Root CA PEM - Downloaded from
	 * https://www.apple.com/certificateauthority/Apple_WebAuthn_Root_CA.pem
	 *
	 * Valid until 03/14/2045 @ 5:00 PM PST
	 */
	private static final String APPLE_WEBAUTHN_ROOT_CA = "/etc/jans/conf/fido2/apple/";

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

			// the first certificate in x5c
			X509Certificate credCert = certificates.get(0);

			List<X509Certificate> trustAnchorCertificates = new ArrayList<X509Certificate>();
			trustAnchorCertificates.addAll(certificateService.getCertificates(APPLE_WEBAUTHN_ROOT_CA));
			try {
				log.debug("APPLE_WEBAUTHN_ROOT_CA root certificate" + trustAnchorCertificates.size());
				X509Certificate verifiedCert = certificateVerifier.verifyAttestationCertificates(certificates,
						trustAnchorCertificates);
				log.info("Step 1 completed  ");

			} catch (Fido2MissingAttestationCertException ex) {
				X509Certificate certificate = certificates.get(0);
				String issuerDN = certificate.getIssuerDN().getName();
				log.warn("Failed to find attestation validation signature public certificate with DN: '{}'", issuerDN);

			}

			// 2. Concatenate |authenticatorData| and |clientDataHash| to form
			// |nonceToHash|.

			byte[] authDataInBytes = authData.getAuthDataDecoded();
			// byte[] nonceToHash = new byte[authDataInBytes.length +
			// clientDataHash.length];

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				baos.write(authDataInBytes);
				baos.write(clientDataHash);
			} catch (IOException e) {
				throw new AttestationException(
						"Concatenate |authenticatorData| and |clientDataHash| to form |nonceToHash|." + e.getMessage());
			}

			byte[] nonceToHash = baos.toByteArray();
			log.info("Step 2 completed");

			// 3. Perform SHA-256 hash of |nonceToHash| to produce |nonce|.
			byte[] nonce = DigestUtils.getSha256Digest().digest(nonceToHash);
			log.info("Step 3 completed");

			// 4. Verify |nonce| matches the value of the extension with OID (
			// 1.2.840.113635.100.8.2 ) in |credCert|.

			byte[] attestationChallenge = getExtension(credCert);

			if (!Arrays.equals(nonce, attestationChallenge)) {
				throw new AttestationException("Certificate 1.2.840.113635.100.8.2 extension does not match nonce");
			} else
				log.info("Step 4 completed");

			// 5. Verify credential public key matches the Subject Public Key of
			// |credCert|.

			PublicKey publicKeyAuthData = coseService.getPublicKeyFromUncompressedECPoint(authData.getCosePublicKey());

			PublicKey publicKeyCredCert = credCert.getPublicKey();

			if (!publicKeyAuthData.equals(publicKeyCredCert)) {
				throw new AttestationException(
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
			int alg = -7;// commonVerifiers.verifyAlgorithm(attStmt.get("alg"), authData.getKeyType());
			credIdAndCounters.setSignatureAlgorithm(alg);
		}

	}

	/*-
	[
	   {
	       "type": "OBJECT_IDENTIFIER",
	       "data": "1.2.840.113635.100.8.2"
	   },
	   {
	       "type": "OCTET_STRING",
	       "data": [
	           {
	               "type": "SEQUENCE",
	               "data": [
	                   {
	                       "type": "[1]",
	                       "data": [
	                           {
	                               "type": "OCTET_STRING",
	                               "data": {
	                                   "type": "Buffer",
	                                   "data": [92, 219, 157, 144, 115, 64, 69, 91, 99, 115, 230, 117, 43, 115, 252, 54, 132, 83, 96, 34, 21, 250, 234, 187, 124, 22, 95, 11, 173, 172, 7, 204]
	                               }
	                           }
	                       ]
	                   }
	               ]
	           }
	       ]
	   }
	]
	*/

	public byte[] getExtension(X509Certificate attestationCert) {
		byte[] extensionValue = attestationCert.getExtensionValue(KEY_DESCRIPTION_OID);
		byte[] extracted;
		try {
			Asn1OctetString extensionEnvelope = new Asn1OctetString();
			extensionEnvelope.decode(extensionValue);
			extensionEnvelope.getValue();
			byte[] extensionEnvelopeValue = extensionEnvelope.getValue();
			Asn1Container container = (Asn1Container) Asn1Parser.parse(ByteBuffer.wrap(extensionEnvelopeValue));
			Asn1ParseResult firstElement = container.getChildren().get(0);
			Asn1OctetString octetString = new Asn1OctetString();
			octetString.decode(firstElement);
			extracted = octetString.getValue();
			return extracted;
		} catch (IOException | RuntimeException e) {
			throw new AttestationException("Failed to extract nonce from Apple anonymous attestation statement.");
		}
	}

}
