package org.gluu.oxauth.model.common;

import org.gluu.persist.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gasmyr on 9/17/20.
 */
public enum OxAuthApplicationType implements AttributeEnum {

    WEB("web", "Web"), NATIVE("native", "Native");

    private static Map<String, OxAuthApplicationType> mapByValues = new HashMap<String, OxAuthApplicationType>();

    static {
        for (OxAuthApplicationType enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private String value;
    private String displayName;

    private OxAuthApplicationType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public static OxAuthApplicationType getByValue(String value) {
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
