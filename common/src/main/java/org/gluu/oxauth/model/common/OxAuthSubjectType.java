package org.gluu.oxauth.model.common;

import org.gluu.persist.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gasmyr on 9/17/20.
 */
public enum OxAuthSubjectType implements AttributeEnum {

    PAIRWISE("pairwise", "pairwise"),
    PUBLIC("public", "public");

    private static Map<String, OxAuthSubjectType> mapByValues = new HashMap<String, OxAuthSubjectType>();

    static {
        for (OxAuthSubjectType enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private String value;
    private String displayName;

    private OxAuthSubjectType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public static OxAuthSubjectType getByValue(String value) {
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

