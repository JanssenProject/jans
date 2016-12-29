package org.xdi.oxauth.model.util;

import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.crypto.CryptoProviderFactory;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;

/**
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class SubjectIdentifierGenerator {

    public static String generatePairwiseSubjectIdentifier(String sectorIdentifier, String localAccountId, String key,
                                                           String salt, AppConfiguration configuration) throws Exception {
        AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(configuration);

        String signingInput = sectorIdentifier + localAccountId + salt;
        return cryptoProvider.sign(signingInput, null, key, SignatureAlgorithm.HS256);
    }
}
