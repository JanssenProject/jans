package org.gluu.oxauth.model.authorize;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.BaseNCodec;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/03/2016
 */

public class CodeVerifier {

    private static final int MAX_CODE_VERIFIER_LENGTH = 128;
    private static final int MIN_CODE_VERIFIER_LENGTH = 43;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public enum CodeChallengeMethod {
        PLAIN("plain", ""),
        S256("s256", "SHA-256");

        private String pkceString;
        private String messageDigestString;

        private CodeChallengeMethod(String pkceString, String messageDigestString) {
            this.pkceString = pkceString;
            this.messageDigestString = messageDigestString;
        }

        public String getMessageDigestString() {
            return messageDigestString;
        }

        public String getPkceString() {
            return pkceString;
        }

        public static CodeChallengeMethod fromString(String value) {
            for (CodeChallengeMethod type : values()) {
                if (type.getPkceString().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return null;
        }
    }

    private String codeVerifier;
    private String codeChallenge;
    private CodeChallengeMethod transformationType;

    public CodeVerifier() {
        this(CodeChallengeMethod.S256);
    }

    public CodeVerifier(CodeChallengeMethod transformationType) {
        this.codeVerifier = generateCodeVerifier();
        this.transformationType = transformationType;
        this.codeChallenge = generateCodeChallenge(transformationType, codeVerifier);
    }

    public static String generateCodeChallenge(CodeChallengeMethod codeChallengeMethod, String codeVerifier) {
        Preconditions.checkNotNull(codeChallengeMethod);
        Preconditions.checkNotNull(codeVerifier);

        switch (codeChallengeMethod) {
            case PLAIN:
                return codeVerifier;
            case S256:
                return s256(codeVerifier);
        }
        throw new RuntimeException("Unsupported code challenge method: " + codeChallengeMethod);
    }

    public static boolean matched(String codeChallenge, String codeChallengeMethod, String codeVerifier) {
        return matched(codeChallenge, CodeChallengeMethod.fromString(codeChallengeMethod), codeVerifier);
    }

    public static boolean matched(String codeChallenge, CodeChallengeMethod codeChallengeMethod, String codeVerifier) {
        if (Strings.isNullOrEmpty(codeChallenge) || codeChallengeMethod == null || Strings.isNullOrEmpty(codeVerifier)) {
            return false;
        }
        return generateCodeChallenge(codeChallengeMethod, codeVerifier).equals(codeChallenge);
    }

    public static String s256(String codeVerifier) {
        byte[] sha256 = DigestUtils.sha256(codeVerifier);
        return base64UrlEncode(sha256);
    }

    public static String base64UrlEncode(byte[] input) {
        Base64 base64 = new Base64(BaseNCodec.MIME_CHUNK_SIZE, EMPTY_BYTE_ARRAY, true);
        return base64.encodeAsString(input);
    }

    public static String generateCodeVerifier() {
        String alphabetic = "abcdefghijklmnopqrstuvwxyz";
        String chars = alphabetic + alphabetic.toUpperCase()
                + "1234567890" + "-._~";
        String code = RandomStringUtils.random(MAX_CODE_VERIFIER_LENGTH, chars);
        Preconditions.checkState(isCodeVerifierValid(code));
        return code;
    }

    public static boolean isCodeVerifierValid(String codeVerifier) {
        if (codeVerifier == null) {
            return false;
        }
        int length = codeVerifier.length();
        if (length > MAX_CODE_VERIFIER_LENGTH || length < MIN_CODE_VERIFIER_LENGTH) {
            return false;
        }
        return true;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public CodeChallengeMethod getTransformationType() {
        return transformationType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CodeVerifier");
        sb.append("{codeVerifier='").append(codeVerifier).append('\'');
        sb.append(", codeChallenge='").append(codeChallenge).append('\'');
        sb.append(", transformationType=").append(transformationType);
        sb.append('}');
        return sb.toString();
    }
}
