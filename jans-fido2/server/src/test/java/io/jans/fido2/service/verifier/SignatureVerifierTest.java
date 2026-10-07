package io.jans.fido2.service.verifier;

import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.util.security.SecurityProviderUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.security.Signature;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SignatureVerifierTest {

    @InjectMocks
    private SignatureVerifier signatureVerifier;

    @Mock
    private Logger log;
    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @BeforeAll
    static void beforeAll() {
        SecurityProviderUtility.installBCProvider();
    }

    /**
     * Every supported COSE algorithm must resolve its Signature instance through the same explicit
     * BouncyCastle provider. Previously -8 (Ed25519), -257 (RS256) and -65535 (SHA1withRSA) used the
     * default JCA provider, causing inconsistent signature pass/fail across JVMs.
     */
    @Test
    void getSignatureChecker_allSupportedAlgorithms_useBouncyCastleProvider() {
        String bcProvider = SecurityProviderUtility.getBCProviderName();
        int[] algorithms = {-7, -8, -35, -36, -37, -38, -39, -257, -258, -259, -65535};
        for (int algorithm : algorithms) {
            Signature checker = signatureVerifier.getSignatureChecker(algorithm);
            assertEquals(bcProvider, checker.getProvider().getName(),
                    "COSE algorithm " + algorithm + " must resolve through the BC provider");
        }
    }
}
