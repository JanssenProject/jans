/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.verifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import io.jans.util.security.SecurityProviderUtility;

/**
 * RFC 8230 fixes the PSS salt length at the hash length for each COSE PS algorithm. A checker built with
 * a shorter salt still constructs, so {@link SignatureVerifier#isSupported(int)} reports it as usable and
 * the algorithm gets advertised - the mismatch only surfaces when a real authenticator signature arrives.
 * These tests sign with the parameters the RFC mandates and require the checker to accept them.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignatureVerifierPssTest {

    @Mock
    private Logger log;

    @InjectMocks
    private SignatureVerifier signatureVerifier;

    @BeforeAll
    static void beforeAll() {
        SecurityProviderUtility.installBCProvider();
    }

    @ParameterizedTest(name = "COSE {0} uses a {2}-byte salt over {1}")
    @CsvSource({ "-37, SHA-256, 32", "-38, SHA-384, 48", "-39, SHA-512, 64" })
    void pssChecker_acceptsASignatureMadeWithTheRfc8230Parameters(int codePoint, String hash, int saltLength)
            throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(3072);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        byte[] signatureBase = "signature base".getBytes(StandardCharsets.UTF_8);

        Signature signer = Signature.getInstance("RSASSA-PSS", SecurityProviderUtility.getBCProvider());
        signer.setParameter(new PSSParameterSpec(hash, "MGF1", new MGF1ParameterSpec(hash), saltLength, 1));
        signer.initSign(keyPair.getPrivate());
        signer.update(signatureBase);
        byte[] signature = signer.sign();

        Signature checker = signatureVerifier.getSignatureChecker(codePoint);
        checker.initVerify(keyPair.getPublic());
        checker.update(signatureBase);

        assertTrue(checker.verify(signature), "COSE " + codePoint + " rejected an RFC 8230 signature");
    }
}
