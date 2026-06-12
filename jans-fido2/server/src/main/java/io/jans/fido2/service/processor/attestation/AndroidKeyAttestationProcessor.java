/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.processor.attestation;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.fido2.ctap.AttestationFormat;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.net.ssl.X509TrustManager;

/**
 * Attestation processor for fmt = "android-key" (Android Keystore attestation).
 *
 * Implements the WebAuthn android-key verification procedure:
 *  1. Verify {@code sig} over {@code authData ‖ clientDataHash} with the public key of the
 *     leaf x5c certificate (credCert), using the COSE {@code alg}.
 *  2. Verify the credential public key in authData equals credCert's subject public key.
 *  3. Verify the attestation challenge in the key-description extension
 *     (OID 1.3.6.1.4.1.11129.2.1.17) equals clientDataHash.
 *  4. Verify the authorization lists: {@code allApplications} must be absent, the key
 *     {@code origin} must be GENERATED (0) and {@code purpose} must contain SIGN (2).
 *  5. (Attestation enforced) Verify the x5c chain to a trusted root from the authenticator
 *     metadata, mirroring the packed full-attestation flow.
 */
@ApplicationScoped
public class AndroidKeyAttestationProcessor implements AttestationFormatProcessor {

    private static final String KEY_DESCRIPTION_OID = "1.3.6.1.4.1.11129.2.1.17";

    // KeyDescription SEQUENCE field indexes
    private static final int IDX_ATTESTATION_CHALLENGE = 4;
    private static final int IDX_SOFTWARE_ENFORCED = 6;
    private static final int IDX_TEE_ENFORCED = 7;

    // AuthorizationList context-specific tags
    private static final int TAG_PURPOSE = 1;
    private static final int TAG_ALL_APPLICATIONS = 600;
    private static final int TAG_ORIGIN = 702;

    // Keymaster constants
    private static final int KM_ORIGIN_GENERATED = 0;
    private static final int KM_PURPOSE_SIGN = 2;

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
    private CertificateService certificateService;

    @Inject
    private AttestationCertificateService attestationCertificateService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Override
    public AttestationFormat getAttestationFormat() {
        return AttestationFormat.android_key;
    }

    @Override
    public void process(JsonNode attStmt, AuthData authData, Fido2RegistrationData registration, byte[] clientDataHash,
            CredAndCounterData credIdAndCounters) {

        int alg = commonVerifiers.verifyAlgorithm(attStmt.get("alg"), authData.getKeyType());
        String signature = commonVerifiers.verifyBase64String(attStmt.get("sig"));

        if (!attStmt.hasNonNull("x5c")) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.ANDROID_KEY_ERROR,
                    "No certificate in the android-key attestation statement");
        }

        List<X509Certificate> certificates = getCertificates(attStmt);
        if (certificates.isEmpty()) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.ANDROID_KEY_ERROR,
                    "x5c certificates are empty");
        }
        X509Certificate credCert = certificates.get(0);

        // 1. Signature over authData ‖ clientDataHash with the leaf certificate key.
        authenticatorDataVerifier.verifyPackedAttestationSignature(authData.getAuthDataDecoded(), clientDataHash,
                signature, credCert, alg);

        // 2. Credential public key in authData must equal the leaf certificate public key.
        PublicKey publicKeyAuthData = coseService.getPublicKeyFromUncompressedECPoint(authData.getCosePublicKey());
        if (!publicKeyAuthData.equals(credCert.getPublicKey())) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.ANDROID_KEY_ERROR,
                    "Certificate public key does not match the credential public key in authData");
        }

        // 3 & 4. Key-description extension: challenge match + authorization-list constraints.
        verifyKeyDescription(credCert, clientDataHash);

        // 5. Chain to a trusted root (from metadata) when attestation is enforced.
        if (!AttestationMode.DISABLED.getValue()
                .equalsIgnoreCase(appConfiguration.getFido2Configuration().getAttestationMode())) {
            X509TrustManager tm = attestationCertificateService.populateTrustManager(authData, certificates);
            if ((tm == null) || (tm.getAcceptedIssuers().length == 0)) {
                throw errorResponseFactory.badRequestException(AttestationErrorResponseType.ANDROID_KEY_ERROR,
                        "No trusted root certificates in metadata for the android-key authenticator");
            }
            certificateVerifier.verifyAttestationCertificates(certificates, Arrays.asList(tm.getAcceptedIssuers()));
        } else {
            log.debug("AttestationMode is DISABLED, skipping android-key chain validation");
        }

        credIdAndCounters.setAttestationType(getAttestationFormat().getFmt());
        credIdAndCounters.setCredId(base64Service.urlEncodeToString(authData.getCredId()));
        credIdAndCounters.setUncompressedEcPoint(base64Service.urlEncodeToString(authData.getCosePublicKey()));
        credIdAndCounters.setSignatureAlgorithm(alg);
    }

    private List<X509Certificate> getCertificates(JsonNode attStmt) {
        ArrayList<String> certificatePath = new ArrayList<>();
        attStmt.get("x5c").elements().forEachRemaining(node -> certificatePath.add(node.asText()));
        return certificateService.getCertificates(certificatePath);
    }

    /**
     * Parses the Android key-description extension and enforces challenge equality and the
     * authorization-list constraints required by the WebAuthn android-key procedure.
     */
    private void verifyKeyDescription(X509Certificate credCert, byte[] clientDataHash) {
        byte[] extValue = credCert.getExtensionValue(KEY_DESCRIPTION_OID);
        if (extValue == null) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.ANDROID_KEY_ERROR,
                    "Missing Android key-description extension " + KEY_DESCRIPTION_OID);
        }
        try {
            // getExtensionValue returns a DER OCTET STRING wrapping the KeyDescription SEQUENCE.
            byte[] keyDescriptionBytes = ASN1OctetString.getInstance(extValue).getOctets();
            ASN1Sequence keyDescription = ASN1Sequence.getInstance(keyDescriptionBytes);

            // 3. attestationChallenge == clientDataHash
            byte[] attestationChallenge = ASN1OctetString
                    .getInstance(keyDescription.getObjectAt(IDX_ATTESTATION_CHALLENGE)).getOctets();
            if (!Arrays.equals(attestationChallenge, clientDataHash)) {
                throw errorResponseFactory.badRequestException(AttestationErrorResponseType.ANDROID_KEY_ERROR,
                        "Attestation challenge in key-description extension does not match clientDataHash");
            }

            ASN1Sequence softwareEnforced = ASN1Sequence.getInstance(keyDescription.getObjectAt(IDX_SOFTWARE_ENFORCED));
            ASN1Sequence teeEnforced = ASN1Sequence.getInstance(keyDescription.getObjectAt(IDX_TEE_ENFORCED));

            // 4a. allApplications must be absent in both lists (key bound to this RP only).
            if (findTagged(softwareEnforced, TAG_ALL_APPLICATIONS) != null
                    || findTagged(teeEnforced, TAG_ALL_APPLICATIONS) != null) {
                throw errorResponseFactory.badRequestException(AttestationErrorResponseType.ANDROID_KEY_ERROR,
                        "allApplications must not be present in the attested key authorization list");
            }

            // 4b. origin == GENERATED and purpose contains SIGN, in the TEE list (fall back to software).
            verifyOriginAndPurpose(teeEnforced, softwareEnforced);
        } catch (Fido2RuntimeException e) {
            throw e;
        } catch (RuntimeException e) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.ANDROID_KEY_ERROR,
                    "Failed to parse the Android key-description extension: " + e.getMessage());
        }
    }

    private void verifyOriginAndPurpose(ASN1Sequence teeEnforced, ASN1Sequence softwareEnforced) {
        ASN1Encodable origin = firstNonNull(findTagged(teeEnforced, TAG_ORIGIN), findTagged(softwareEnforced, TAG_ORIGIN));
        ASN1Encodable purpose = firstNonNull(findTagged(teeEnforced, TAG_PURPOSE), findTagged(softwareEnforced, TAG_PURPOSE));

        if (origin == null || ASN1Integer.getInstance(origin).getValue().intValue() != KM_ORIGIN_GENERATED) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.ANDROID_KEY_ERROR,
                    "Attested key origin is not KM_ORIGIN_GENERATED");
        }
        if (purpose == null) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.ANDROID_KEY_ERROR,
                    "Attested key purpose is missing");
        }
        ASN1Set purposes = ASN1Set.getInstance(purpose);
        boolean canSign = false;
        for (ASN1Encodable p : purposes) {
            if (ASN1Integer.getInstance(p).getValue().intValue() == KM_PURPOSE_SIGN) {
                canSign = true;
                break;
            }
        }
        if (!canSign) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.ANDROID_KEY_ERROR,
                    "Attested key purpose does not include KM_PURPOSE_SIGN");
        }
    }

    /** Returns the EXPLICIT base object of the context-tagged field {@code tagNo}, or null if absent. */
    private ASN1Encodable findTagged(ASN1Sequence authorizationList, int tagNo) {
        for (ASN1Encodable element : authorizationList) {
            if (element instanceof ASN1TaggedObject) {
                ASN1TaggedObject tagged = (ASN1TaggedObject) element;
                if (tagged.getTagNo() == tagNo) {
                    return tagged.getExplicitBaseObject();
                }
            }
        }
        return null;
    }

    private ASN1Encodable firstNonNull(ASN1Encodable a, ASN1Encodable b) {
        return a != null ? a : b;
    }
}
