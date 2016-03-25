package org.xdi.oxauth.model.authorize;

import org.apache.commons.codec.binary.Hex;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/03/2016
 */

public class CodeVerifierTest {

    @Test
    public void verifierAndChallengeMatch() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        assertMatch(CodeVerifier.TransformationType.PLAIN);
        assertMatch(CodeVerifier.TransformationType.S256);
    }

    private void assertMatch(CodeVerifier.TransformationType type) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        CodeVerifier verifier = new CodeVerifier(type);
        System.out.println(verifier);

        if (type == CodeVerifier.TransformationType.PLAIN) {
            assertEquals(verifier.getCodeChallenge(), verifier.getCodeVerifier());
            return;
        }

        MessageDigest md = MessageDigest.getInstance(type.getMessageDigestString());
        md.update(verifier.getCodeVerifier().getBytes("UTF-8")); // Change this to "UTF-16" if needed
        byte[] digest = md.digest();

        assertEquals(Hex.encodeHexString(digest), verifier.getCodeChallenge());
    }
}
