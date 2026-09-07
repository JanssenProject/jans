package io.jans.fido2.ctap;

import java.util.HashMap;
import java.util.Map;

// Constant names are the IANA COSE Algorithms registry spellings, which are transmitted verbatim in
// enabledFidoAlgorithms configuration, so they do not follow the SCREAMING_CASE convention.
@SuppressWarnings("java:S115")
public enum CoseEdDSAAlgorithm {

    EdDSA(-8); // the v2.3 table's separate Ed25519 entry is -19, not this

    private static final Map<Integer, CoseEdDSAAlgorithm> ALGORITHM_MAPPINGS = new HashMap<>();

    static {
        for (CoseEdDSAAlgorithm enumType : values()) {
            ALGORITHM_MAPPINGS.put(enumType.getNumericValue(), enumType);
        }
    }

    private final int numericValue;

    CoseEdDSAAlgorithm(int value) {
        this.numericValue = value;
    }

    public static CoseEdDSAAlgorithm fromNumericValue(int value) {
        return ALGORITHM_MAPPINGS.get(value);
    }

    public int getNumericValue() {
        return numericValue;
    }
}
