/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.server.attestation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import io.jans.as.model.configuration.AttestationVerifierProfile;
import io.jans.as.server.attestation.tpm.TpmPublic;

public class AkBindingVerifierTest {

    private static final byte[] NONCE = AttestationUtil.decodeBase64(AttestationUtil.randomBase64Url(32));

    private final AkBindingVerifier verifier = new AkBindingVerifier();

    @Test
    public void runtimeDataCommitment_whenAkIsCommittedAndReportDataMatches_isShown() {
        MaaSimulator maa = new MaaSimulator();
        MaaSimulator.Scenario scenario = maa.scenario(NONCE);

        AkBindingResult result = verifier.verify(maa.profile(), scenario.evidence(), scenario.resultClaims(), ak(maa.ak, TpmTestSupport.AK_ATTRIBUTES));

        assertTrue(result.isShown(), result.toString());
        assertEquals(result.getMethod(), AttestationVerifierProfile.AK_BINDING_RUNTIME_DATA_COMMITMENT);
    }

    @Test
    public void runtimeDataCommitment_whenSignerIsHclTransferKey_isNotShown() {
        MaaSimulator maa = new MaaSimulator();
        MaaSimulator.Scenario scenario = maa.scenario(NONCE);

        AkBindingResult result = verifier.verify(maa.profile(), scenario.evidence(), scenario.resultClaims(),
                ak(maa.transferKey, TpmTestSupport.DECRYPT_KEY_ATTRIBUTES));

        assertFalse(result.isShown());
    }

    @Test
    public void runtimeDataCommitment_whenSignerIsUncommittedRestrictedKey_isNotShown() {
        MaaSimulator maa = new MaaSimulator();
        MaaSimulator.Scenario scenario = maa.scenario(NONCE);
        scenario.akInRuntimeData = false;

        AkBindingResult result = verifier.verify(maa.profile(), scenario.evidence(), scenario.resultClaims(),
                ak(TpmTestSupport.rsaKeyPair(), TpmTestSupport.AK_ATTRIBUTES));

        assertFalse(result.isShown());
    }

    @Test
    public void runtimeDataCommitment_whenAkIsAbsentFromRuntimeData_isNotShown() {
        MaaSimulator maa = new MaaSimulator();
        MaaSimulator.Scenario scenario = maa.scenario(NONCE);
        scenario.akInRuntimeData = false;

        AkBindingResult result = verifier.verify(maa.profile(), scenario.evidence(), scenario.resultClaims(), ak(maa.ak, TpmTestSupport.AK_ATTRIBUTES));

        assertFalse(result.isShown());
    }

    @Test
    public void runtimeDataCommitment_whenReportDataDoesNotCommitRuntimeData_isNotShown() {
        MaaSimulator maa = new MaaSimulator();
        MaaSimulator.Scenario scenario = maa.scenario(NONCE);
        scenario.reportDataOverride = new byte[64];

        AkBindingResult result = verifier.verify(maa.profile(), scenario.evidence(), scenario.resultClaims(), ak(maa.ak, TpmTestSupport.AK_ATTRIBUTES));

        assertFalse(result.isShown());
    }

    @Test
    public void runtimeDataCommitment_whenRuntimeDataIsAbsent_isNotShown() {
        MaaSimulator maa = new MaaSimulator();
        MaaSimulator.Scenario scenario = maa.scenario(NONCE);
        JSONObject evidence = scenario.evidence();
        evidence.remove(AkBindingVerifier.HCL_RUNTIME_DATA);

        assertFalse(verifier.verify(maa.profile(), evidence, scenario.resultClaims(), ak(maa.ak, TpmTestSupport.AK_ATTRIBUTES)).isShown());
    }

    @Test
    public void verify_whenProfileEnablesNoMethod_isNotShown() {
        MaaSimulator maa = new MaaSimulator();
        MaaSimulator.Scenario scenario = maa.scenario(NONCE);
        AttestationVerifierProfile profile = maa.profile();
        profile.setAkBindingMethods(Collections.emptyList());

        assertFalse(verifier.verify(profile, scenario.evidence(), scenario.resultClaims(), ak(maa.ak, TpmTestSupport.AK_ATTRIBUTES)).isShown());
    }

    @Test
    public void certChain_whenAkCertificateChainsToTrustAnchor_isShown() throws Exception {
        KeyPair root = TpmTestSupport.rsaKeyPair();
        KeyPair akPair = TpmTestSupport.rsaKeyPair();
        X509Certificate rootCert = certificate("CN=Test TPM Root", root, "CN=Test TPM Root", root, true);
        X509Certificate akCert = certificate("CN=Test AK", akPair, "CN=Test TPM Root", root, false);

        AkBindingResult result = verifier.verify(certChainProfile(rootCert), certChainEvidence(akCert), new JSONObject(),
                ak(akPair, TpmTestSupport.AK_ATTRIBUTES));

        assertTrue(result.isShown(), result.toString());
        assertEquals(result.getMethod(), AttestationVerifierProfile.AK_BINDING_CERT_CHAIN);
    }

    @Test
    public void certChain_whenRootIsNotTrusted_isNotShown() throws Exception {
        KeyPair root = TpmTestSupport.rsaKeyPair();
        KeyPair otherRoot = TpmTestSupport.rsaKeyPair();
        KeyPair akPair = TpmTestSupport.rsaKeyPair();
        X509Certificate otherRootCert = certificate("CN=Other Root", otherRoot, "CN=Other Root", otherRoot, true);
        X509Certificate akCert = certificate("CN=Test AK", akPair, "CN=Test TPM Root", root, false);

        AkBindingResult result = verifier.verify(certChainProfile(otherRootCert), certChainEvidence(akCert), new JSONObject(),
                ak(akPair, TpmTestSupport.AK_ATTRIBUTES));

        assertFalse(result.isShown());
    }

    @Test
    public void certChain_whenCertificateKeyIsNotTheSigner_isNotShown() throws Exception {
        KeyPair root = TpmTestSupport.rsaKeyPair();
        KeyPair akPair = TpmTestSupport.rsaKeyPair();
        X509Certificate rootCert = certificate("CN=Test TPM Root", root, "CN=Test TPM Root", root, true);
        X509Certificate akCert = certificate("CN=Test AK", akPair, "CN=Test TPM Root", root, false);

        AkBindingResult result = verifier.verify(certChainProfile(rootCert), certChainEvidence(akCert), new JSONObject(),
                ak(TpmTestSupport.rsaKeyPair(), TpmTestSupport.AK_ATTRIBUTES));

        assertFalse(result.isShown());
    }

    @Test
    public void certChain_whenChainIsAbsent_isNotShown() throws Exception {
        KeyPair root = TpmTestSupport.rsaKeyPair();
        X509Certificate rootCert = certificate("CN=Test TPM Root", root, "CN=Test TPM Root", root, true);

        AkBindingResult result = verifier.verify(certChainProfile(rootCert), new JSONObject().put("key_certification", new JSONObject()),
                new JSONObject(), ak(TpmTestSupport.rsaKeyPair(), TpmTestSupport.AK_ATTRIBUTES));

        assertFalse(result.isShown());
    }

    @Test
    public void verify_whenSignerIsNotRestrictedSigningKey_isNotShownEvenWithValidCertificate() throws Exception {
        KeyPair root = TpmTestSupport.rsaKeyPair();
        KeyPair keyPair = TpmTestSupport.rsaKeyPair();
        X509Certificate rootCert = certificate("CN=Test TPM Root", root, "CN=Test TPM Root", root, true);
        X509Certificate cert = certificate("CN=Test AK", keyPair, "CN=Test TPM Root", root, false);

        AkBindingResult result = verifier.verify(certChainProfile(rootCert), certChainEvidence(cert), new JSONObject(),
                ak(keyPair, TpmTestSupport.APP_KEY_ATTRIBUTES));

        assertFalse(result.isShown());
    }

    private static TpmPublic ak(KeyPair keyPair, long attributes) {
        return TpmPublic.parse(TpmTestSupport.tpmtPublic(keyPair.getPublic(), attributes));
    }

    private static AttestationVerifierProfile certChainProfile(X509Certificate root) throws Exception {
        AttestationVerifierProfile profile = new AttestationVerifierProfile();
        profile.setKind("tpm2");
        profile.setAkBindingMethods(Collections.singletonList(AttestationVerifierProfile.AK_BINDING_CERT_CHAIN));
        String pem = "-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder().encodeToString(root.getEncoded()) + "\n-----END CERTIFICATE-----\n";
        profile.setAkCertTrustAnchors(Collections.singletonList(pem));
        return profile;
    }

    private static JSONObject certChainEvidence(X509Certificate akCert) throws Exception {
        return new JSONObject().put("key_certification", new JSONObject()
                .put(AkBindingVerifier.AK_CERT_CHAIN, new JSONArray().put(Base64.getEncoder().encodeToString(akCert.getEncoded()))));
    }

    private static X509Certificate certificate(String subject, KeyPair subjectKey, String issuer, KeyPair issuerKey, boolean ca) throws Exception {
        long now = System.currentTimeMillis();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(new X500Name(issuer), BigInteger.valueOf(now),
                new Date(now - 60_000), new Date(now + 3_600_000), new X500Name(subject), subjectKey.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
        return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey.getPrivate())));
    }
}
