/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
/**
 * @author Javier Rojas Blum Date: 12.03.2012
 */

public enum BlockEncryptionAlgorithm {

    A128CBC_PLUS_HS256("A128CBC+HS256", "CBC", "AES/CBC/PKCS5Padding", "SHA-256", "HMACSHA256", 256, 128, 128),
    A256CBC_PLUS_HS512("A256CBC+HS512", "CBC", "AES/CBC/PKCS5Padding", "SHA-512", "HMACSHA512", 512, 128, 256),
    A128GCM("A128GCM", "GCM", "AES/GCM/NoPadding", 128, 96),
    A256GCM("A256GCM", "GCM", "AES/GCM/NoPadding", 256, 96);

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

    @SuppressWarnings("java:S107")
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