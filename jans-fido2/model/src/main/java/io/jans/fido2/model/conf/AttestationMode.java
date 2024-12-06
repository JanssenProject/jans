package io.jans.fido2.model.conf;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.jans.orm.annotation.AttributeEnum;
import jakarta.xml.bind.annotation.XmlEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Shekhar L. on 06/08/2024
 */

@XmlEnum(String.class)
public enum AttestationMode implements AttributeEnum  {

    DISABLED("disabled", "none"), MONITOR("monitor", "direct"), ENFORCED("enforced", "direct");

    private String value;
    private String displayName;

    private AttestationMode(String value, String displayName) {
        this.setValue(value);
        this.setDisplayName(displayName);
    }

    private static final Map<String, AttestationMode> mapByValues = new HashMap<>();

    static {
        for (AttestationMode enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    @JsonCreator
    public static AttestationMode forValues(String value) {
        return getByValue(value);
    }

    public static AttestationMode getByValue(String value) {
        return mapByValues.get(value);
    }

    @Override
    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }


    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return value;
    }
}
