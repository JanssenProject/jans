/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.ctap;

import java.util.HashMap;
import java.util.Map;

/**
 * The ML-DSA post-quantum signature algorithms from the FIDO Server Requirements v2.3 table. Code points
 * are the IANA COSE Algorithms registry values; the provider names are the JCA spellings, which carry the
 * hyphens a Java identifier cannot.
 */
public enum CoseMLDSAAlgorithm {

    ML_DSA_44(-48, "ML-DSA-44"),
    ML_DSA_65(-49, "ML-DSA-65"),
    ML_DSA_87(-50, "ML-DSA-87");

    private static final Map<Integer, CoseMLDSAAlgorithm> ALGORITHM_MAPPINGS = new HashMap<>();

    static {
        for (CoseMLDSAAlgorithm enumType : values()) {
            ALGORITHM_MAPPINGS.put(enumType.getNumericValue(), enumType);
        }
    }

    private final int numericValue;
    private final String algorithmName;

    CoseMLDSAAlgorithm(int value, String algorithmName) {
        this.numericValue = value;
        this.algorithmName = algorithmName;
    }

    public static CoseMLDSAAlgorithm fromNumericValue(int value) {
        return ALGORITHM_MAPPINGS.get(value);
    }

    /**
     * Resolves a configured name, accepting both the IANA spelling ({@code ML-DSA-44}) and the constant
     * spelling ({@code ML_DSA_44}), since the registry name is the one an administrator will reach for.
     */
    public static CoseMLDSAAlgorithm fromName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().replace('-', '_').toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public int getNumericValue() {
        return numericValue;
    }

    /** The JCA name to request from the crypto provider. */
    public String getAlgorithmName() {
        return algorithmName;
    }
}
