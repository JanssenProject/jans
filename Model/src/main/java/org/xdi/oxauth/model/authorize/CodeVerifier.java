package org.xdi.oxauth.model.authorize;

import org.apache.commons.codec.digest.DigestUtils;
import org.xdi.oxauth.model.util.Preconditions;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/03/2016
 */

public class CodeVerifier {

    public enum TransformationType {
        PLAIN("plain", ""),
        S256("s256", "SHA-256");

        private String pkceString;
        private String messageDigestString;

        private TransformationType(String pkceString, String messageDigestString) {
            this.pkceString = pkceString;
            this.messageDigestString = messageDigestString;
        }

        public String getMessageDigestString() {
            return messageDigestString;
        }

        public String getPkceString() {
            return pkceString;
        }
    }

    private String codeVerifier;
    private String codeChallenge;
    private TransformationType transformationType;

    public CodeVerifier() {
        this(TransformationType.S256);
    }

    public CodeVerifier(TransformationType transformationType) {
        this.codeVerifier = generateCodeVerifier();
        this.transformationType = transformationType;
        this.codeChallenge = generateCodeChallenge(transformationType, codeVerifier);
    }

    public static String generateCodeChallenge(TransformationType transformationType, String codeVerifier) {
        Preconditions.checkNotNull(transformationType);
        Preconditions.checkNotNull(codeVerifier);

        switch (transformationType) {
            case PLAIN:
                return codeVerifier;
            case S256:
                return s256(codeVerifier);
        }
        throw new RuntimeException("Unsupported transformation type: " + transformationType);
    }

    public static String s256(String codeVerifier) {
        return DigestUtils.sha256Hex(codeVerifier);
    }

    public static String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        return UUID.randomUUID().toString() + new BigInteger(130, random).toString(32);
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public TransformationType getTransformationType() {
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
