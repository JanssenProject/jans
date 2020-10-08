package org.gluu.oxauth.model.authorize;

import org.gluu.oxauth.model.authorize.CodeVerifier;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/03/2016
 */

public class CodeVerifierTest {

    @Test
    public void verifierAndChallengeMatch() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        assertMatch(CodeVerifier.CodeChallengeMethod.PLAIN);
        assertMatch(CodeVerifier.CodeChallengeMethod.S256);

        assertFalse(CodeVerifier.matched(null, "", "invalid_code"));
    }

    @Test
    public void verify() {
        String codeChallenge = CodeVerifier.generateCodeChallenge(CodeVerifier.CodeChallengeMethod.S256, "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");

        assertEquals(codeChallenge, "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }

    @Test
    public void codeVerificationGenerator() {
        for (int i = 0; i < 10; i++) {
            assertTrue(CodeVerifier.isCodeVerifierValid(CodeVerifier.generateCodeVerifier()));
        }
    }

    private static void assertMatch(CodeVerifier.CodeChallengeMethod type) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        CodeVerifier verifier = new CodeVerifier(type);
        System.out.println(verifier);

        if (type == CodeVerifier.CodeChallengeMethod.PLAIN) {
            assertEquals(verifier.getCodeChallenge(), verifier.getCodeVerifier());
            return;
        }

        MessageDigest md = MessageDigest.getInstance(type.getMessageDigestString());
        md.update(verifier.getCodeVerifier().getBytes("UTF-8")); // Change this to "UTF-16" if needed
        byte[] digest = md.digest();

        assertEquals(CodeVerifier.base64UrlEncode(digest), verifier.getCodeChallenge());
    }
}
