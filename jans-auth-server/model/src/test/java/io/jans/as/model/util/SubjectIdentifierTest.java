package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.CryptoProviderFactory;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.testng.Assert.assertNotNull;

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
}
