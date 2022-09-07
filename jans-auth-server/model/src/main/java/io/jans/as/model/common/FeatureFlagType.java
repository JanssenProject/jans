package io.jans.as.model.common;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yuriy Z
 */
public enum FeatureFlagType {

    UNKNOWN("unknown"),
    HEALTH_CHECK("health_check"),
    USERINFO("userinfo"),
    CLIENTINFO("clientinfo"),
    ID_GENERATION("id_generation"),
    REGISTRATION("registration"),
    INTROSPECTION("introspection"),
    REVOKE_TOKEN("revoke_token"),
    REVOKE_SESSION("revoke_session"),
    ACTIVE_SESSION("active_session"),
    END_SESSION("end_session"),
    STATUS_SESSION("status_session"),
    JANS_CONFIGURATION("jans_configuration"), // /.well-known/jans-configuration
    CIBA("ciba"),
    UMA("uma"),
    U2F("u2f"),
    DEVICE_AUTHZ("device_authz"),
    METRIC("metric"),
    STAT("stat"),
    PAR("par");

    private final String value;

    FeatureFlagType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FeatureFlagType fromValue(String v) {
        if (StringUtils.isBlank(v)) {
            return UNKNOWN;
        }
        for (FeatureFlagType t : values()) {
            if (t.getValue().equalsIgnoreCase(v)) {
                return t;
            }
        }

        return UNKNOWN;
    }

    public static Set<FeatureFlagType> fromValues(List<String> values) {
        Set<FeatureFlagType> result = new HashSet<>();
        if (values == null || values.isEmpty()) {
            return result;
        }

        for (String v : values) {
            final FeatureFlagType t = fromValue(v);
            if (t != UNKNOWN) {
                result.add(t);
            }
        }
        return result;
    }
}
