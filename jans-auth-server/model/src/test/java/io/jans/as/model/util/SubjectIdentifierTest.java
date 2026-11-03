package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.CryptoProviderFactory;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class SubjectIdentifierTest extends BaseTest {

    @Test
    public void generatePairwiseSubjectIdentifier_validInfo_correctIntValue() throws Exception {
        showTitle("generatePairwiseSubjectIdentifier_validInfo_correctIntValue");
        String sectorIdentifier = "sector1";
        String localAccountId = "123";
        String key = "keyId123";
        String salt = "112233";
        try (MockedStatic<CryptoProviderFactory> cryptoProvider = Mockito.mockStatic(CryptoProviderFactory.class)) {
            cryptoProvider.when(() -> CryptoProviderFactory.getCryptoProvider(any())).thenReturn(new AuthCryptoProvider());
            String resultString = SubjectIdentifierGenerator.generatePairwiseSubjectIdentifier(sectorIdentifier, localAccountId, key, salt, new AppConfiguration());
            assertNotNull(resultString);
        }
    }

    @Test
    public void generateEphemeralSubjectIdentifier_whenCalled_shouldReturnUrlSafeValueWith256Bits() {
        final String sub = SubjectIdentifierGenerator.generateEphemeralSubjectIdentifier();

        assertEquals(sub.length(), 43);
        assertTrue(sub.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    public void generateEphemeralSubjectIdentifier_whenCalledRepeatedly_shouldNeverRepeat() {
        final Set<String> values = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            values.add(SubjectIdentifierGenerator.generateEphemeralSubjectIdentifier());
        }

        assertEquals(values.size(), 1000);
    }
}
