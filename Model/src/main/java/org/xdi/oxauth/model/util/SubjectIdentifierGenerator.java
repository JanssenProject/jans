package org.xdi.oxauth.model.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author Javier Rojas Blum
 * @version 0.9 March 11, 2015
 */
public class SubjectIdentifierGenerator {

    private String salt;

    public SubjectIdentifierGenerator(String salt) {
        this.salt = salt;
    }

    public String generatePairwiseSubjectIdentifier(String sectorIdentifier, String localAccountId, byte[] key)
            throws NoSuchAlgorithmException, InvalidKeyException {

        byte[] signingInput = (sectorIdentifier + localAccountId + salt).getBytes();
        byte[] hashedOutput = JwtUtil.getSignatureHS256(signingInput, key);

        return JwtUtil.base64urlencode(hashedOutput);
    }
}
