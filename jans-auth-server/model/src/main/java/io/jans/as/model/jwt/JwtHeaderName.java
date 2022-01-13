/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwt;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public final class JwtHeaderName {

    public static final String TYPE = "typ";
    public static final String ALGORITHM = "alg";
    public static final String KEY_ID = "kid";
    public static final String CONTENT_TYPE = "cty";
    public static final String ENCRYPTION_METHOD = "enc";
    public static final String EPHEMERAL_PUBLIC_KEY = "epk";
    public static final String COMPRESSION_ALGORITHM = "zip";
    public static final String AGREEMENT_PARTY_U_INFO = "apu";
    public static final String AGREEMENT_PARTY_V_INFO = "apv";
    public static final String ENCRYPTION_PARTY_U_INFO = "epu";
    public static final String ENCRYPTION_PARTY_V_INFO = "epv";
    public static final String JWK = "jwk";

    /**
     * The caller references the constants using <tt>JwtClaimName.TYPE</tt>,
     * and so on. Thus, the caller should be prevented from constructing objects of
     * this class, by declaring this private constructor.
     */
    private JwtHeaderName() {
        // this prevents even the native class from calling this constructor as well
        throw new AssertionError();
    }
}
