package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonValue;
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

    @DocFeatureFlag(description = "Enables the OAuth 2.0-protected Clientinfo endpoint, which allows an authorized client to retrieve claims and information about a registered client. For more details, see the [ClientInfo Endpoint Documentation](../../../auth-server/endpoints/clientinfo.md).",
            defaultValue = "Enabled")
    CLIENTINFO("clientinfo"),
    @DocFeatureFlag(description = "Enable/Disable ID Generation endpoint",
            defaultValue = "Enabled")
    ID_GENERATION("id_generation"),
    @DocFeatureFlag(description = "Enable/Disable client registration endpoint",
            defaultValue = "Enabled")
    REGISTRATION("registration"),
    @DocFeatureFlag(description = "Enables the OAuth 2.0 Token Introspection endpoint in Janssen Server. For more details, see the [Introspection endpoint documentation](../../../auth-server/endpoints/introspection.md).",
            defaultValue = "Enabled")
    INTROSPECTION("introspection"),
    @DocFeatureFlag(description = "Enable/Disable token revocation endpoint",
            defaultValue = "Enabled")
    REVOKE_TOKEN("revoke_token"),
    @DocFeatureFlag(description = "Enables the Global Token Revocation endpoint, which invalidates all tokens and sessions associated with a user. For more details about, see the [Global Token Revocation endpoint documentation](../../../auth-server/endpoints/global-token-revocation.md).",
            defaultValue = "Enabled")
    GLOBAL_TOKEN_REVOCATION("global_token_revocation"),
    @DocFeatureFlag(description = "Enable/Disable status list endpoint",
            defaultValue = "Enabled")
    STATUS_LIST("status_list"),
    @DocFeatureFlag(description = "Enables Logout Status JWT support in Janssen Server. For more details, see the [Logout Status JWT documentation](../../../auth-server/tokens/logout-status-jwt.md).",
            defaultValue = "Enabled")
    LOGOUT_STATUS_JWT("logout_status_jwt"),
    @DocFeatureFlag(description = "Enable/Disable active session endpoint",
            defaultValue = "Enabled")
    ACTIVE_SESSION("active_session"),
    @DocFeatureFlag(description = "Enables the OpenID Connect RP-Initiated Logout end-session endpoint in Janssen Server. For more details, see the [End Session endpoint documentation](../../../auth-server/endpoints/end-session.md).",
            defaultValue = "Enabled")
    END_SESSION("end_session"),
    @DocFeatureFlag(description = "Enable/Disable session status check endpoint",
            defaultValue = "Enabled")
    STATUS_SESSION("status_session"),
    @DocFeatureFlag(description = "Enables the Janssen Server *.well-known* OpenID Connect configuration endpoint used for service discovery. For more details, see the [OpenID Configuration endpoint documentation](../../../auth-server/endpoints/configuration.md).",
            defaultValue = "Enabled")
    JANS_CONFIGURATION("jans_configuration"), // /.well-known/jans-configuration
    @DocFeatureFlag(description = "Enables OpenID Connect Client Initiated Backchannel Authentication (CIBA) support in Janssen Server. For more details, see the [Janssen OIDC CIBA Documentation](../../../auth-server/openid-features/ciba.md).",
            defaultValue = "Enabled")
    CIBA("ciba"),
    @DocFeatureFlag(description = "Enable/Disable support for User-Managed Access (UMA)",
            defaultValue = "Disabled")
    UMA("uma"),
    @DocFeatureFlag(description = "Enable/Disable support for Universal 2nd Factor(U2F) protocol",
            defaultValue = "Disabled")
    U2F("u2f"),
    @DocFeatureFlag(description = "Enables the OAuth 2.0 Device Authorization Grant in Janssen Server. For details about the device authorization flow, see the [Device Authorization endpoint documentation](../../../auth-server/endpoints/device-authorization.md).",
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
    @DocFeatureFlag(description = "Enables the AuthZEN Access Evaluation API in Janssen Server. For details about its behavior, requests, responses, and authorization decisions, see the [Access Evaluation endpoint documentation](../../../auth-server/endpoints/access-evaluation.md).",
            defaultValue = "Enabled")
    ACCESS_EVALUATION("access_evaluation"),
    @DocFeatureFlag(description = "Enable/Disable Rate Limit",
            defaultValue = "Enabled")
    RATE_LIMIT("rate_limit"),
    @DocFeatureFlag(description = "Enable/Disable Software Statement Assertion(SSA) feature",
            defaultValue = "Enabled")
    SSA("ssa"),
    @DocFeatureFlag(description = "Enables Client ID Metadata Document (CIMD) support, allowing client applications to use a URL as their client_id. For more details, see the [Client ID Metadata Document (CIMD) Documentation](../../../auth-server/oauth-features/cimd.md).",
            defaultValue = "Disabled")
    CLIENT_ID_METADATA_DOCUMENT("client_id_metadata_document"),
    @DocFeatureFlag(description = "Enable/Disable Identity Assertion Authorization Grant (Cross-App Access / ID-JAG) support",
            defaultValue = "Disabled")
    IDENTITY_ASSERTION_AUTHZ_GRANT("identity_assertion_authz_grant");

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
	
    @Override
    @JsonValue
    public String toString() {
        return value;
    }
}
