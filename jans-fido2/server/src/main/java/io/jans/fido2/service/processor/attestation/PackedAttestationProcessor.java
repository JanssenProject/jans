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

import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.error.ErrorResponseFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Attestation processor for attestations of fmt = packed
 *
 */
@ApplicationScoped
public class PackedAttestationProcessor implements AttestationFormatProcessor {

    @Inject
    private Logger log;
    
    @Inject
	private AppConfiguration appConfiguration;

    @Inject
    private CommonVerifiers commonVerifiers;

    @Inject
    private AuthenticatorDataVerifier authenticatorDataVerifier;

    @Inject
    private CertificateVerifier certificateVerifier;

    @Inject
    private CoseService coseService;

    @Inject
    private Base64Service base64Service;

    @Inject
    private AttestationCertificateService attestationCertificateService;

    @Inject
    private CertificateService certificateService;


    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.packed;
    }

    @Override
    public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData registration, byte[] clientDataHash,
            CredAndCounterData credIdAndCounters) {
        int alg = commonVerifiers.verifyAlgorithm(attStmt.get("alg"), authData.getKeyType());
        String signature = commonVerifiers.verifyBase64String(attStmt.get("sig"));

        // if attestation mode is enabled in the global config

        if (!appConfiguration.getFido2Configuration().getAttestationMode().equalsIgnoreCase(AttestationMode.DISABLED.getValue()))
        {
        	if (attStmt.hasNonNull("x5c")) {

                List<X509Certificate> attestationCertificates = getAttestationCertificates(attStmt);
                X509TrustManager tm = attestationCertificateService.populateTrustManager(authData, attestationCertificates);
                if ((tm == null) || (tm.getAcceptedIssuers().length == 0)) {
                    throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR, "Packed full attestation but no certificates in metadata for authenticator " + Hex.encodeHexString(authData.getAaguid()));
                }
                X509Certificate verifiedCert = certificateVerifier.verifyAttestationCertificates(attestationCertificates, Arrays.asList(tm.getAcceptedIssuers()));
                authenticatorDataVerifier.verifyPackedAttestationSignature(authData.getAuthDataDecoded(), clientDataHash, signature, verifiedCert, alg);
                if (certificateVerifier.isSelfSigned(verifiedCert)) {
                    throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR, "Self signed certificate");
                }


                // If the attestation certificate carries the id-fido-gen-ce-aaguid extension, it must
                // match the AAGUID in the authenticator data.
                verifyAaguidExtension(attestationCertificates.get(0), authData);
                // WebAuthn packed full-attestation certificate requirements (v3, CA:false, subject fields).
                verifyPackedAttestationCertRequirements(attestationCertificates.get(0));


            } else if (attStmt.hasNonNull("ecdaaKeyId")) {
                String ecdaaKeyId = attStmt.get("ecdaaKeyId").asText();
                throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR, ecdaaKeyId + " is not supported");
            } else {
                PublicKey publicKey = coseService.getPublicKeyFromUncompressedECPoint(authData.getCosePublicKey());
                authenticatorDataVerifier.verifyPackedSurrogateAttestationSignature(authData.getAuthDataDecoded(), clientDataHash, signature, publicKey, alg);
            }
        }
        else
		{
			log.debug("In Global fido configuration, AttestationMode is DISABLED, hence skipping the attestation check");
		}
        credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());
        credIdAndCounters.setCredId(base64Service.urlEncodeToString(authData.getCredId()));
        credIdAndCounters.setUncompressedEcPoint(base64Service.urlEncodeToString(authData.getCosePublicKey()));
        credIdAndCounters.setSignatureAlgorithm(alg);
    }

	/**
	 * Verifies the optional id-fido-gen-ce-aaguid (1.3.6.1.4.1.45724.1.1.4) extension: when present in
	 * the packed attestation certificate, its AAGUID must equal the AAGUID in the authenticator data.
	 * getExtensionValue returns a DER OCTET STRING wrapping an OCTET STRING that holds the raw AAGUID.
	 * Package-private for unit testing.
	 */
	void verifyAaguidExtension(X509Certificate attestationCertificate, AuthData authData) {
		byte[] ext = attestationCertificate.getExtensionValue("1.3.6.1.4.1.45724.1.1.4");
		if (ext != null && ext.length > 0) {
			byte[] aaguidInCert;
			try {
				byte[] inner = ASN1OctetString.getInstance(ext).getOctets();
				aaguidInCert = ASN1OctetString.getInstance(inner).getOctets();
			} catch (RuntimeException e) {
				log.error("Malformed id-fido-gen-ce-aaguid extension in packed attestation certificate", e);
				throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR,
						"Malformed id-fido-gen-ce-aaguid extension in packed attestation certificate");
			}
			if (!Arrays.equals(aaguidInCert, authData.getAaguid())) {
				log.error("Packed attestation certificate AAGUID does not match authenticator data AAGUID");
				throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR,
						"AAGUID in packed attestation certificate does not match authenticator data");
			}
		}
	}

	/**
	 * Enforces the WebAuthn packed full-attestation certificate requirements: X.509 version 3,
	 * BasicConstraints CA component false, Subject-OU exactly "Authenticator Attestation", non-empty
	 * Subject-O and Subject-CN, and a valid ISO 3166 Subject-C. Package-private for unit testing.
	 */
	void verifyPackedAttestationCertRequirements(X509Certificate attestationCertificate) {
		if (attestationCertificate.getVersion() != 3) {
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR,
					"Packed attestation certificate must be version 3");
		}
		// The Basic Constraints extension MUST be present with the CA component set to false.
		// getBasicConstraints() returns -1 for both CA=false and an absent extension, so the
		// presence of the extension (OID 2.5.29.19) is verified explicitly as well.
		if (attestationCertificate.getExtensionValue("2.5.29.19") == null
				|| attestationCertificate.getBasicConstraints() != -1) {
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR,
					"Packed attestation certificate BasicConstraints CA component must be false");
		}
		X500Name subject;
		try {
			subject = new JcaX509CertificateHolder(attestationCertificate).getSubject();
		} catch (CertificateEncodingException e) {
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR,
					"Unable to read packed attestation certificate subject");
		}
		String organisation = subjectField(subject, BCStyle.O);
		String commonName = subjectField(subject, BCStyle.CN);
		String organisationUnit = subjectField(subject, BCStyle.OU);
		String country = subjectField(subject, BCStyle.C);

		if (organisation == null || organisation.isEmpty()) {
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR,
					"Packed attestation certificate Subject-O must be present");
		}
		if (commonName == null || commonName.isEmpty()) {
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR,
					"Packed attestation certificate Subject-CN must be present");
		}
		if (!"Authenticator Attestation".equals(organisationUnit)) {
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR,
					"Packed attestation certificate Subject-OU must be 'Authenticator Attestation'");
		}
		if (!isValidCountryCode(country)) {
			throw errorResponseFactory.badRequestException(AttestationErrorResponseType.PACKED_ERROR,
					"Packed attestation certificate Subject-C must be a valid ISO 3166 country code");
		}
	}

	private String subjectField(X500Name subject, ASN1ObjectIdentifier oid) {
		RDN[] rdns = subject.getRDNs(oid);
		if (rdns == null || rdns.length == 0) {
			return null;
		}
		return IETFUtils.valueToString(rdns[0].getFirst().getValue());
	}

	private boolean isValidCountryCode(String country) {
		return (country != null) && (country.length() == 2)
				&& Arrays.asList(Locale.getISOCountries()).contains(country.toUpperCase(Locale.ROOT));
	}

	private List<X509Certificate> getAttestationCertificates(JsonNode attStmt) {
		Iterator<JsonNode> i = attStmt.get("x5c").elements();
		ArrayList<String> certificatePath = new ArrayList<>();
		while (i.hasNext()) {
		    certificatePath.add(i.next().asText());
		}
		return certificateService.getCertificates(certificatePath);
	}
}
