package org.xdi.oxauth.model.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author Javier Rojas Blum
 * @version February 15, 2015
 */
public class SubjectIdentifierGenerator {

    public static String generatePairwiseSubjectIdentifier(String sectorIdentifier, String localAccountId, String key, String salt)
            throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] signingInput = (sectorIdentifier + localAccountId + salt).getBytes();
        byte[] hashedOutput = JwtUtil.getSignatureHS256(signingInput, key.getBytes());

        return JwtUtil.base64urlencode(hashedOutput);
    }
}
