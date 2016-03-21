package org.xdi.oxauth.model.authorize;

import com.google.common.base.Preconditions;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/03/2016
 */

public class CodeVerifier {

    public enum TransformationType {
        PLAIN,
        S256
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
        return UUID.randomUUID().toString();
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
}
