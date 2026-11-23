/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.server.attestation;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nimbusds.jose.jwk.JWK;

import io.jans.as.model.configuration.AttestationVerifierProfile;
import io.jans.as.server.attestation.tpm.TpmFormatException;
import io.jans.as.server.attestation.tpm.TpmPublic;

/**
 * Check (2) of design §9.3a: proves that the attestation key (AK) that signed {@code TPM2_Certify} belongs to the
 * platform the verifier appraised. The AK is never selected by name or position in a runtime-key list: it is the key
 * that signed the certification, and it must be shown bound by one of the methods the profile enables.
 *
 * <ul>
 *     <li>{@code ak_cert_chain}: an AK certificate chain in the evidence validates to a configured trust anchor and
 *     its leaf key is the AK.</li>
 *     <li>{@code runtime_data_commitment}: {@code SHA-256(hcl_runtime_data)} equals the verifier-attested report data,
 *     the AK public key is one of the keys in that runtime data, and the AK is a restricted signing key (which rules
 *     out encryption keys such as {@code HCLTransferKey}).</li>
 * </ul>
 */
public class AkBindingVerifier {

    public static final String HCL_RUNTIME_DATA = "hcl_runtime_data";
    public static final String AK_CERT_CHAIN = "ak_cert_chain";

    public AkBindingResult verify(AttestationVerifierProfile profile, JSONObject evidence, JSONObject verifiedClaims, TpmPublic ak) {
        if (profile == null || ak == null) {
            return AkBindingResult.notShown("No profile or attestation key");
        }
        if (!ak.isAttestationKey()) {
            return AkBindingResult.notShown("Certification signer is not a restricted, non-duplicable signing key");
        }

        List<String> failures = new ArrayList<>();
        if (profile.hasAkBindingMethod(AttestationVerifierProfile.AK_BINDING_CERT_CHAIN)) {
            String failure = verifyCertChain(profile, evidence, ak);
            if (failure == null) {
                return AkBindingResult.shown(AttestationVerifierProfile.AK_BINDING_CERT_CHAIN);
            }
            failures.add(failure);
        }
        if (profile.hasAkBindingMethod(AttestationVerifierProfile.AK_BINDING_RUNTIME_DATA_COMMITMENT)) {
            String failure = verifyRuntimeDataCommitment(profile, evidence, verifiedClaims, ak);
            if (failure == null) {
                return AkBindingResult.shown(AttestationVerifierProfile.AK_BINDING_RUNTIME_DATA_COMMITMENT);
            }
            failures.add(failure);
        }
        if (failures.isEmpty()) {
            return AkBindingResult.notShown("Profile enables no AK binding method");
        }
        return AkBindingResult.notShown(String.join("; ", failures));
    }

    String verifyCertChain(AttestationVerifierProfile profile, JSONObject evidence, TpmPublic ak) {
        JSONObject keyCertification = evidence != null ? evidence.optJSONObject("key_certification") : null;
        JSONArray chain = keyCertification != null ? keyCertification.optJSONArray(AK_CERT_CHAIN) : null;
        if (chain == null || chain.isEmpty()) {
            return "ak_cert_chain is absent";
        }
        if (profile.getAkCertTrustAnchors().isEmpty()) {
            return "Profile has no akCertTrustAnchors";
        }
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            List<X509Certificate> certificates = new ArrayList<>();
            for (int i = 0; i < chain.length(); i++) {
                byte[] der = AttestationUtil.decodeBase64(chain.optString(i));
                if (der == null) {
                    return "ak_cert_chain entry " + i + " is not base64";
                }
                certificates.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der)));
            }

            Set<TrustAnchor> anchors = new HashSet<>();
            for (String pem : profile.getAkCertTrustAnchors()) {
                X509Certificate anchor = (X509Certificate) factory.generateCertificate(
                        new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
                anchors.add(new TrustAnchor(anchor, null));
            }

            CertPath path = factory.generateCertPath(certificates);
            PKIXParameters parameters = new PKIXParameters(anchors);
            parameters.setRevocationEnabled(false);
            CertPathValidator.getInstance("PKIX").validate(path, parameters);

            PublicKey leafKey = certificates.get(0).getPublicKey();
            if (!Arrays.equals(leafKey.getEncoded(), ak.toPublicKey().getEncoded())) {
                return "AK certificate key is not the certification signer";
            }
            return null;
        } catch (Exception e) {
            return "AK certificate chain is not valid: " + e.getMessage();
        }
    }

    String verifyRuntimeDataCommitment(AttestationVerifierProfile profile, JSONObject evidence, JSONObject verifiedClaims, TpmPublic ak) {
        byte[] runtimeData = evidence != null ? AttestationUtil.decodeBase64(evidence.optString(HCL_RUNTIME_DATA, null)) : null;
        if (runtimeData == null || runtimeData.length == 0) {
            return "hcl_runtime_data is absent";
        }
        if (StringUtils.isBlank(profile.getReportDataClaimPath())) {
            return "Profile has no reportDataClaimPath";
        }
        byte[] reportData = AttestationUtil.decodeFlexible(ClaimPathResolver.resolveString(verifiedClaims, profile.getReportDataClaimPath()));
        byte[] runtimeDigest = AttestationUtil.sha256(runtimeData);
        if (reportData == null || reportData.length < runtimeDigest.length
                || !MessageDigest.isEqual(Arrays.copyOf(reportData, runtimeDigest.length), runtimeDigest)) {
            return "SHA-256(hcl_runtime_data) does not equal the attested report data";
        }

        final JSONArray keys;
        try {
            keys = new JSONObject(new String(runtimeData, StandardCharsets.UTF_8)).optJSONArray("keys");
        } catch (JSONException e) {
            return "hcl_runtime_data is not JSON";
        }
        if (keys == null) {
            return "hcl_runtime_data has no keys";
        }

        final String akJkt;
        try {
            akJkt = AttestationUtil.thumbprint(ak.toJwk());
        } catch (TpmFormatException | IllegalArgumentException e) {
            return "Attestation key is not usable: " + e.getMessage();
        }
        for (int i = 0; i < keys.length(); i++) {
            JSONObject key = keys.optJSONObject(i);
            if (key == null) {
                continue;
            }
            try {
                JWK jwk = JWK.parse(key.toMap());
                if (AttestationUtil.constantTimeEquals(AttestationUtil.thumbprint(jwk.toPublicJWK()), akJkt)) {
                    return null;
                }
            } catch (Exception e) {
                // not a usable public key; keep looking
            }
        }
        return "Certification signer is not a key committed in hcl_runtime_data";
    }
}
