/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Block Encryption Algorithm values; 
 * "enc" (Encryption Algorithm) Header Parameter Values for JWE (RFC 7518);
 * 
 * @author Javier Rojas Blum Date: 12.03.2012
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public enum BlockEncryptionAlgorithm {

    A128CBC_PLUS_HS256("A128CBC+HS256", BlockEncryptionAlgorithm.DEF_CBC, BlockEncryptionAlgorithm.DEF_AES_CBC_PKCS5PADDING, BlockEncryptionAlgorithm.DEF_SHA_256, BlockEncryptionAlgorithm.DEF_HMACSHA256, 256, 128, 256),
    A256CBC_PLUS_HS512("A256CBC+HS512", BlockEncryptionAlgorithm.DEF_CBC, BlockEncryptionAlgorithm.DEF_AES_CBC_PKCS5PADDING, BlockEncryptionAlgorithm.DEF_SHA_512, BlockEncryptionAlgorithm.DEF_HMACSHA512, 512, 128, 512),
    A128CBC_HS256("A128CBC-HS256", BlockEncryptionAlgorithm.DEF_CBC, BlockEncryptionAlgorithm.DEF_AES_CBC_PKCS5PADDING, BlockEncryptionAlgorithm.DEF_SHA_256, BlockEncryptionAlgorithm.DEF_HMACSHA256, 256, 128, 256),
    A192CBC_HS384("A192CBC-HS384", BlockEncryptionAlgorithm.DEF_CBC, BlockEncryptionAlgorithm.DEF_AES_CBC_PKCS5PADDING, BlockEncryptionAlgorithm.DEF_SHA_384, BlockEncryptionAlgorithm.DEF_HMACSHA384, 384, 128, 284),
    A256CBC_HS512("A256CBC-HS512", BlockEncryptionAlgorithm.DEF_CBC, BlockEncryptionAlgorithm.DEF_AES_CBC_PKCS5PADDING, BlockEncryptionAlgorithm.DEF_SHA_512, BlockEncryptionAlgorithm.DEF_HMACSHA512, 512, 128, 512),
    A128GCM("A128GCM", BlockEncryptionAlgorithm.DEF_GCM, BlockEncryptionAlgorithm.DEF_AES_GCM_NOPADDING, 128, 128),
    A192GCM("A192GCM", BlockEncryptionAlgorithm.DEF_GCM, BlockEncryptionAlgorithm.DEF_AES_GCM_NOPADDING, 192, 128),
    A256GCM("A256GCM", BlockEncryptionAlgorithm.DEF_GCM, BlockEncryptionAlgorithm.DEF_AES_GCM_NOPADDING, 256, 128);

    public static final String DEF_CBC = "CBC";
    public static final String DEF_GCM = "GCM";

    public static final String DEF_AES_CBC_PKCS5PADDING = "AES/CBC/PKCS5Padding";
    public static final String DEF_AES_GCM_NOPADDING = "AES/GCM/NoPadding";

    public static final String DEF_SHA_256 = "SHA-256";
    public static final String DEF_SHA_384 = "SHA-384";
    public static final String DEF_SHA_512 = "SHA-512";

    public static final String DEF_HMACSHA256 = "HMACSHA256";
    public static final String DEF_HMACSHA384 = "HMACSHA384";
    public static final String DEF_HMACSHA512 = "HMACSHA512";

    private final String name;
    private final String family;
    private final String algorithm;
    private final String messageDiggestAlgorithm;
    private final String integrityValueAlgorithm;
    private final int cmkLength;
    private final int initVectorLength;
    private final Integer cekLength;

    BlockEncryptionAlgorithm(String name, String family, String algorithm, int cmkLength, int initVectorLength) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.messageDiggestAlgorithm = null;
        this.integrityValueAlgorithm = null;
        this.cmkLength = cmkLength;
        this.initVectorLength = initVectorLength;
        this.cekLength = null;
    }

    BlockEncryptionAlgorithm(String name, String family, String algorithm, String messageDiggestAlgorithm,
                             String integrityValueAlgorithm, int cmkLength, int initVectorLength, int cekLength) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.messageDiggestAlgorithm = messageDiggestAlgorithm;
        this.integrityValueAlgorithm = integrityValueAlgorithm;
        this.cmkLength = cmkLength;
        this.initVectorLength = initVectorLength;
        this.cekLength = cekLength;
    }

    public String getName() {
        return name;
    }

    public String getFamily() {
        return family;
    }

    public String getMessageDiggestAlgorithm() {
        return messageDiggestAlgorithm;
    }

    public String getIntegrityValueAlgorithm() {
        return integrityValueAlgorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getCmkLength() {
        return cmkLength;
    }

    public int getInitVectorLength() {
        return initVectorLength;
    }

    public Integer getCekLength() {
        return cekLength;
    }

    @JsonCreator
    public static BlockEncryptionAlgorithm fromName(String name) {
        if (name != null) {
            for (BlockEncryptionAlgorithm a : BlockEncryptionAlgorithm.values()) {
                if (name.equals(a.name)) {
                    return a;
                }
            }
        }
        return null;
    }

    @Override
    @JsonValue
    public String toString() {
        return name;
    }
}