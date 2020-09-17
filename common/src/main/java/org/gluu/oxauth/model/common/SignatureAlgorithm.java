package org.gluu.oxauth.model.common;

import org.gluu.persist.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gasmyr on 9/17/20.
 */
public enum SignatureAlgorithm implements AttributeEnum {

    NONE("none", "none"),
    HS256("HS256", "HS256"), HS384("HS384", "HS384"), HS512("HS512", "HS512"),
    RS256("RS256", "RS256"), RS384("RS384", "RS384"), RS512("RS512", "RS512"),
    ES256("ES256", "ES256"), ES384("ES384", "ES384"), ES512("ES512", "ES512"),
    PS256("PS256", "PS256"), PS384("PS384", "PS384"), PS512("PS512", "PS512");

    private static Map<String, SignatureAlgorithm> mapByValues = new HashMap<String, SignatureAlgorithm>();

    static {
        for (SignatureAlgorithm enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private String value;
    private String displayName;

    private SignatureAlgorithm(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public static SignatureAlgorithm getByValue(String value) {
        return mapByValues.get(value);
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
