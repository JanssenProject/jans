package org.gluu.oxauth.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.lang.StringUtils;

/**
 * @author Yuriy Zabrovarnyy
 */
public enum SoftwareStatementValidationType {
    NONE("none"),
    JWKS("jwks"),
    JWKS_URI("jwks_uri"),
    SCRIPT("script");

    public static final SoftwareStatementValidationType DEFAULT = SCRIPT;

    private final String value;

    SoftwareStatementValidationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SoftwareStatementValidationType fromString(String param) {
        if (StringUtils.isBlank(param)) {
            return null;
        }

        for (SoftwareStatementValidationType v : SoftwareStatementValidationType.values()) {
            if (param.equals(v.value)) {
                return v;
            }
        }
        return null;
    }
}
