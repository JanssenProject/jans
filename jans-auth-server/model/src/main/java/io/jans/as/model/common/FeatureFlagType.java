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
    @DocFeatureFlag(description = "Enables the Auth Server health-check endpoint, which reports the operational status of Janssen Server. For more details, see the [Health Check documentation](../../../install/install-faq.md#use-the-janssen-server-health-check-endpoint).",
            defaultValue = "Enabled")
    HEALTH_CHECK("health_check"),
    @DocFeatureFlag(description = "Enables the OpenID Connect UserInfo endpoint in Janssen Server. For more details, see the [UserInfo endpoint documentation](../../../auth-server/endpoints/userinfo.md).",
            defaultValue = "Enabled")
    USERINFO("userinfo"),

    @DocFeatureFlag(description = "Enables the OAuth 2.0-protected Clientinfo endpoint, which allows an authorized client to retrieve claims and information about a registered client. For more details, see the [ClientInfo Endpoint Documentation](../../../auth-server/endpoints/clientinfo.md).",
            defaultValue = "Enabled")
    CLIENTINFO("clientinfo"),
    @DocFeatureFlag(description = "Enables custom ID generation support in Janssen Server. The ID Generator allows administrators to replace the default ID generation logic with custom rules for identifiers such as person and client entries. For more details, see the [ID Generator documentation](../../../../script-catalog/id_generator/id-generator.md).",
            defaultValue = "Enabled")
    ID_GENERATION("id_generation"),
    @DocFeatureFlag(description = "Enables the Client Registration endpoint in Janssen Server. For more details, see the [Client Registration endpoint documentation](../../../auth-server/endpoints/client-registration.md).",
            defaultValue = "Enabled")
    REGISTRATION("registration"),
    @DocFeatureFlag(description = "Enables the OAuth 2.0 Token Introspection endpoint in Janssen Server. For more details, see the [Introspection endpoint documentation](../../../auth-server/endpoints/introspection.md).",
            defaultValue = "Enabled")
    INTROSPECTION("introspection"),
    @DocFeatureFlag(description = "Enables the OAuth 2.0 Token Revocation endpoint in Janssen Server. For more details, see the [Token Revocation endpoint documentation](../../../auth-server/endpoints/token-revocation.md).",
            defaultValue = "Enabled")
    REVOKE_TOKEN("revoke_token"),
    @DocFeatureFlag(description = "Enables the Global Token Revocation endpoint, which invalidates all tokens and sessions associated with a user. For more details, see the [Global Token Revocation endpoint documentation](../../../auth-server/endpoints/global-token-revocation.md).",
            defaultValue = "Enabled")
    GLOBAL_TOKEN_REVOCATION("global_token_revocation"),
    @DocFeatureFlag(description = "Enables the Token Status List endpoint in Janssen Server, which enables the client to query token status. For more details, see the [Logout Status JWT documentation](../../../auth-server/tokens/logout-status-jwt.md).",
            defaultValue = "Enabled")
    STATUS_LIST("status_list"),
    @DocFeatureFlag(description = "Enables Logout Status JWT support in Janssen Server. For more details, see the [Logout Status JWT documentation](../../../auth-server/tokens/logout-status-jwt.md).",
            defaultValue = "Enabled")
    LOGOUT_STATUS_JWT("logout_status_jwt"),
    @DocFeatureFlag(description = "Enables the Active Session endpoint in Janssen Server. The endpoint allows authorized applications to retrieve information about the user's active authentication session.",
            defaultValue = "Enabled")
    ACTIVE_SESSION("active_session"),
    @DocFeatureFlag(description = "Enables the OpenID Connect RP-Initiated Logout end-session endpoint in Janssen Server. For more details, see the [End Session endpoint documentation](../../../auth-server/endpoints/end-session.md).",
            defaultValue = "Enabled")
    END_SESSION("end_session"),
    @DocFeatureFlag(description = "Enables the session status check endpoint in Janssen Server, which allows an application to check the current status of an authenticated user session.",
            defaultValue = "Enabled")
    STATUS_SESSION("status_session"),
    @DocFeatureFlag(description = "Enables the Janssen Server *.well-known* OpenID Connect configuration endpoint used for service discovery. For more details, see the [OpenID Configuration endpoint documentation](../../../auth-server/endpoints/configuration.md).",
            defaultValue = "Enabled")
    JANS_CONFIGURATION("jans_configuration"), // /.well-known/jans-configuration
    @DocFeatureFlag(description = "Enables OpenID Connect Client Initiated Backchannel Authentication (CIBA) support in Janssen Server. For more details, see the [Janssen OIDC CIBA Documentation](../../../auth-server/openid-features/ciba.md).",
            defaultValue = "Enabled")
    CIBA("ciba"),
    @DocFeatureFlag(description = "Enables User-Managed Access (UMA) support in Janssen Server. For more details, see the [UMA documentation](../../../auth-server/uma-features/README.md).",
            defaultValue = "Disabled")
    UMA("uma"),
    @DocFeatureFlag(description = "Enables support for FIDO U2F in Janssen Server, allowing applications to use legacy U2F authenticators for registration and authentication. For more details, see the [FIDO Administration Guide](../../../../contribute/implementation-design/jans-fido2-design/README.md).",
            defaultValue = "Disabled")
    U2F("u2f"),
    @DocFeatureFlag(description = "Enables the OAuth 2.0 Device Authorization Grant in Janssen Server. For details about the device authorization flow, see the [Device Authorization endpoint documentation](../../../auth-server/endpoints/device-authorization.md).",
            defaultValue = "Enabled")
    DEVICE_AUTHZ("device_authz"),
    @DocFeatureFlag(description = "Enables metric reporting in Janssen Server. Metric data can be used to monitor and report on Authorization Server activity, including user activity, issued tokens, health checks, and audit information. For details, see the [Reporting and Metrics documentation](../../../auth-server/reporting-metrics/README.md).",
            defaultValue = "Enabled")
    METRIC("metric"),
    @DocFeatureFlag(description = "Enables the Authorization Server Statistic service, which provides statistical data such as monthly active users and issued-token information. For more details, see the [Statistic endpoint](../../../auth-server/reporting-metrics/README.md#statistic-endpoint).",
            defaultValue = "Enabled")
    STAT("stat"),
    @DocFeatureFlag(description = "Enables OAuth 2.0 Pushed Authorization Requests (PAR) in Janssen Server. For more details, see the [PAR endpoint documentation](../../../auth-server/endpoints/par.md).",
            defaultValue = "Enabled")
    PAR("par"),
    @DocFeatureFlag(description = "Enables the AuthZEN Access Evaluation API in Janssen Server. For details about its behavior, requests, responses, and authorization decisions, see the [Access Evaluation endpoint documentation](../../../auth-server/endpoints/access-evaluation.md).",
            defaultValue = "Enabled")
    ACCESS_EVALUATION("access_evaluation"),
    @DocFeatureFlag(description = "Enables request rate limiting in the Janssen Authorization Server. For more details, see the [Rate Limit Configuration](../../../config-guide/auth-server-config/rate-limit.md).",
            defaultValue = "Enabled")
    RATE_LIMIT("rate_limit"),
    @DocFeatureFlag(description = "Enables the Software Statement Assertion (SSA) endpoint in Janssen Server. For more details, see the [SSA endpoint documentation](../../../auth-server/endpoints/ssa.md).",
            defaultValue = "Enabled")
    SSA("ssa"),
    @DocFeatureFlag(description = "Enables Client ID Metadata Document (CIMD) support, allowing client applications to use a URL as their client_id. For more details, see the [Client ID Metadata Document (CIMD) Documentation](../../../auth-server/oauth-features/cimd.md).",
            defaultValue = "Disabled")
    CLIENT_ID_METADATA_DOCUMENT("client_id_metadata_document"),
    @DocFeatureFlag(description = "Enables Identity Assertion Authorization Grant (Cross-App Access / ID-JAG) support in Janssen Server. It allows a client authenticated with one Identity Provider (IdP) to obtain an access token from a trusted Resource Authorization Server without starting a new browser-based SSO flow.",
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
