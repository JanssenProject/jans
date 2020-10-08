package org.gluu.oxauth.model.crypto.binding;

/**
 * <pre>
 *  enum {
 *     rsa2048_pkcs1.5(0), rsa2048_pss(1), ecdsap256(2), (255)
 *  } TokenBindingKeyParameters;
 *  </pre>
 *
 * @author Yuriy Zabrovarnyy
 */
public enum TokenBindingKeyParameters {
    RSA2048_PKCS1_5("rsa2048_pkcs1.5", 0),
    RSA2048_PSS("rsa2048_pss", 1),
    ECDSAP256("ecdsap256", 2);

    private final String value;
    private final int byteValue;

    TokenBindingKeyParameters(String value, int byteValue) {
        this.value = value;
        this.byteValue = byteValue;
    }

    public String getValue() {
        return value;
    }

    public int getByteValue() {
        return byteValue;
    }

    public static TokenBindingKeyParameters valueOf(int byteValue) {
        for (TokenBindingKeyParameters v : values()) {
            if (v.getByteValue() == byteValue) {
                return v;
            }
        }
        throw new RuntimeException("Failed to identify TokenBindingKeyParameters by byteValue");
    }

    @Override
    public String toString() {
        return "TokenBindingKeyParameters{" +
                "value='" + value + '\'' +
                ", byteValue=" + byteValue +
                "} " + super.toString();
    }
}
