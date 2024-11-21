package io.jans.as.model.common;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.doc.annotation.DocFeatureFlag;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yuriy Z
 */
public enum FeatureFlagType {

    UNKNOWN("unknown"),
    @DocFeatureFlag(description = "Enable/Disable health-check endpoint",
            defaultValue = "Enabled")
    HEALTH_CHECK("health_check"),
    @DocFeatureFlag(description = "Enable/Disable OpenID Connect [userinfo endpoint](https://openid.net/specs/openid-connect-core-1_0.html#UserInfo)",
            defaultValue = "Enabled")
    USERINFO("userinfo"),

    @DocFeatureFlag(description = "Enable/Disable client info endpoint",
            defaultValue = "Enabled")
    CLIENTINFO("clientinfo"),
    @DocFeatureFlag(description = "Enable/Disable ID Generation endpoint",
            defaultValue = "Enabled")
    ID_GENERATION("id_generation"),
    @DocFeatureFlag(description = "Enable/Disable client registration endpoint",
            defaultValue = "Enabled")
    REGISTRATION("registration"),
    @DocFeatureFlag(description = "Enable/Disable token introspection endpoint",
            defaultValue = "Enabled")
    INTROSPECTION("introspection"),
    @DocFeatureFlag(description = "Enable/Disable token revocation endpoint",
            defaultValue = "Enabled")
    REVOKE_TOKEN("revoke_token"),
    @DocFeatureFlag(description = "Enable/Disable session revocation endpoint",
            defaultValue = "Enabled")
    REVOKE_SESSION("revoke_session"),
    @DocFeatureFlag(description = "Enable/Disable global token revocation endpoint",
            defaultValue = "Enabled")
    GLOBAL_TOKEN_REVOCATION("global_token_revocation"),
    @DocFeatureFlag(description = "Enable/Disable status list endpoint",
            defaultValue = "Enabled")
    STATUS_LIST("status_list"),
    @DocFeatureFlag(description = "Enable/Disable active session endpoint",
            defaultValue = "Enabled")
    ACTIVE_SESSION("active_session"),
    @DocFeatureFlag(description = "Enable/Disable end session endpoint",
            defaultValue = "Enabled")
    END_SESSION("end_session"),
    @DocFeatureFlag(description = "Enable/Disable session status check endpoint",
            defaultValue = "Enabled")
    STATUS_SESSION("status_session"),
    @DocFeatureFlag(description = "Enable/Disable *.well-known* configuration endpoint",
            defaultValue = "Enabled")
    JANS_CONFIGURATION("jans_configuration"), // /.well-known/jans-configuration
    @DocFeatureFlag(description = "Enable/Disable OpenID Connect Client Initiated Backchannel Authentication Flow(CIBA) flow support",
            defaultValue = "Enabled")
    CIBA("ciba"),
    @DocFeatureFlag(description = "Enable/Disable support for User-Managed Access (UMA)",
            defaultValue = "Disabled")
    UMA("uma"),
    @DocFeatureFlag(description = "Enable/Disable support for Universal 2nd Factor(U2F) protocol",
            defaultValue = "Disabled")
    U2F("u2f"),
    @DocFeatureFlag(description = "Enable/Disable support for device authorization",
            defaultValue = "Enabled")
    DEVICE_AUTHZ("device_authz"),
    @DocFeatureFlag(description = "Enable/Disable metric reporter feature",
            defaultValue = "Enabled")
    METRIC("metric"),
    @DocFeatureFlag(description = "Enable/Disable Stat service",
            defaultValue = "Enabled")
    STAT("stat"),
    @DocFeatureFlag(description = "Enable/Disable Pushed Authorization Requests(PAR) feature",
            defaultValue = "Enabled")
    PAR("par"),
    @DocFeatureFlag(description = "Enable/Disable Access Evaluation Endpoint",
            defaultValue = "Enabled")
    ACCESS_EVALUATION("access_evaluation"),
    @DocFeatureFlag(description = "Enable/Disable Software Statement Assertion(SSA) feature",
            defaultValue = "Enabled")
    SSA("ssa");

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

    public static Set<FeatureFlagType> from(AppConfiguration appConfiguration) {
        return fromValues(appConfiguration.getFeatureFlags());
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
