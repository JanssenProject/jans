use super::{
    BootstrapConfig, JwtConfig, LogConfig, LogTypeConfig, MemoryLogConfig, PolicyStoreConfig,
    PolicyStoreSource,
};
use crate::common::policy_store::PolicyStore;
use serde::{de, Deserialize, Deserializer};

#[derive(Deserialize, PartialEq, Debug)]
#[allow(dead_code)]
pub struct BootstrapConfigRaw {
    ///  Human friendly identifier for the application
    #[serde(rename = "CEDARLING_APPLICATION_NAME")]
    pub application_name: String,

    /// Location of policy store JSON, used if policy store is not local, or retreived from Lock Master.
    #[serde(
        rename = "CEDARLING_POLICY_STORE_URI",
        default,
        deserialize_with = "parse_option_string"
    )]
    pub policy_store_uri: Option<String>,

    /// An identifier for the policy store.
    #[serde(rename = "CEDARLING_POLICY_STORE_ID", default)]
    pub policy_store_id: String,

    /// How the Logs will be presented.
    #[serde(rename = "CEDARLING_LOG_TYPE", default)]
    pub log_type: LogType,

    /// If `log_type` is set to [`LogType::Memory`], this is the TTL (time to live) of
    /// log entities in seconds.
    #[serde(rename = "CEDARLING_LOG_TTL", default)]
    pub log_ttl: Option<u64>,

    /// Path to a local file pointing containing a JWKS.
    #[serde(
        rename = "CEDARLING_LOCAL_JWKS",
        default,
        deserialize_with = "parse_option_string"
    )]
    pub local_jwks: Option<String>,

    /// JSON object with policy store
    #[serde(rename = "CEDARLING_LOCAL_POLICY_STORE", default)]
    pub local_policy_store: Option<PolicyStore>,

    /// Path to a Policy Store JSON file
    #[serde(
        rename = "CEDARLING_POLICY_STORE_LOCAL_FN",
        default,
        deserialize_with = "parse_option_string"
    )]
    pub policy_store_local_fn: Option<String>,

    /// Whether to check the signature of all JWT tokens.
    ///
    /// This requires that an `iss` (Issuer) claim is present on each token.
    #[serde(rename = "CEDARLING_JWT_SIG_VALIDATION", default)]
    pub jwt_sig_validation: FeatureToggle,

    /// Whether to check the status of the JWT. On startup.
    ///
    /// Cedarling will fetch and retreive the latest Status List JWT from the
    /// `.well-known/openid-configuration` via the `status_list_endpoint` claim and
    /// cache it. See the [`IETF Draft`] for more info.
    ///
    /// [`IETF Draft`]: https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/
    #[serde(rename = "CEDARLING_JWT_STATUS_VALIDATION", default)]
    pub jwt_status_validation: FeatureToggle,

    /// Cedarling will only accept tokens signed with these algorithms.
    #[serde(rename = "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED", default)]
    pub jwt_signature_algorithms_supported: Vec<String>,

    /// When enabled, the `iss` (Issuer) claim must be present in the Access Token and
    /// the scheme must be https.
    #[serde(rename = "CEDARLING_AT_ISS_VALIDATION", default)]
    pub at_iss_validation: FeatureToggle,

    /// When enabled, the `jti` (JWT ID) claim must be present in the Access Token.
    #[serde(rename = "CEDARLING_AT_JTI_VALIDATION", default)]
    pub at_jti_validation: FeatureToggle,

    /// When enabled, the `nbf` (Not Before) claim must be present in the Access Token
    /// and Cedarling will verify that the current date is after the `nbf`.
    #[serde(rename = "CEDARLING_AT_NBF_VALIDATION", default)]
    pub at_nbf_validation: FeatureToggle,

    /// When enabled, the `exp` (Expiration) claim must be present in the Access Token
    /// and not past the date specified.
    #[serde(rename = "CEDARLING_AT_EXP_VALIDATION", default)]
    pub at_exp_validation: FeatureToggle,

    /// When enabled, the `iss` (Issuer) claim must be present in the ID Token and
    /// the scheme must be https.
    #[serde(rename = "CEDARLING_IDT_ISS_VALIDATION", default)]
    pub idt_iss_validation: FeatureToggle,

    /// When enabled, the `sub` (Subject) claim must be present in the ID Token.
    #[serde(rename = "CEDARLING_IDT_SUB_VALIDATION", default)]
    pub idt_sub_validation: FeatureToggle,

    /// When enabled, the `exp` (Expiration) claim must be present in the ID Token
    /// and not past the date specified.
    #[serde(rename = "CEDARLING_IDT_EXP_VALIDATION", default)]
    pub idt_exp_validation: FeatureToggle,

    /// When enabled, the `iat` (Issued at) claim must be present in the ID Token.
    #[serde(rename = "CEDARLING_IDT_IAT_VALIDATION", default)]
    pub idt_iat_validation: FeatureToggle,

    /// When enabled, the `aud` ( Audience) claim must be present in the ID Token.
    #[serde(rename = "CEDARLING_IDT_AUD_VALIDATION", default)]
    pub idt_aud_validation: FeatureToggle,

    /// When enabled, the `iss` (Issuer) claim must be present in the Userinfo Token and
    /// the scheme must be https.
    #[serde(rename = "CEDARLING_USERINFO_ISS_VALIDATION", default)]
    pub userinfo_iss_validation: FeatureToggle,

    /// When enabled, the `sub` (Subject) claim must be present in the Userinfo Token.
    #[serde(rename = "CEDARLING_USERINFO_SUB_VALIDATION", default)]
    pub userinfo_sub_validation: FeatureToggle,

    /// When enabled, the `aud` (Audience) claim must be present in the Userinfo Token.
    #[serde(rename = "CEDARLING_USERINFO_AUD_VALIDATION", default)]
    pub userinfo_aud_validation: FeatureToggle,

    /// When enabled, the `exp` (Expiration) claim must be present in the Userinfo Token
    /// and not past the date specified.
    #[serde(rename = "CEDARLING_USERINFO_EXP_VALIDATION", default)]
    pub userinfo_exp_validation: FeatureToggle,

    /// Varying levels of validations based on the preference of the developer.
    ///
    /// # Strict Mode
    ///
    /// Strict mode requires:
    ///     1. id_token aud matches the access_token client_id;
    ///     2. if a Userinfo token is present, the sub matches the id_token, and that
    ///         the aud matches the access token client_id.
    #[serde(rename = "CEDARLING_ID_TOKEN_TRUST_MODE", default)]
    pub id_token_trust_mode: TrustMode,

    /// If Enabled, the Cedarling will connect to the Lock Master for policies,
    /// and subscribe for SSE events.
    #[serde(rename = "CEDARLING_LOCK", default)]
    pub lock: FeatureToggle,

    /// URI where Cedarling can get JSON file with all required metadata about
    /// Lock Master, i.e. .well-known/lock-master-configuration.
    ///
    /// ***Required*** if `LOCK == Enabled`.
    #[serde(rename = "CEDARLING_LOCK_MASTER_CONFIGURATION_URI", default)]
    pub lock_master_configuration_uri: Option<String>,

    /// Controls whether Cedarling should listen for SSE config updates.
    #[serde(rename = "CEDARLING_DYNAMIC_CONFIGURATION", default)]
    pub dynamic_configuration: FeatureToggle,

    /// SSA for DCR in a Lock Master deployment. The Cedarling will validate this
    /// SSA JWT prior to DCR.
    #[serde(
        rename = "CEDARLING_LOCK_SSA_JWT",
        default,
        deserialize_with = "parse_option_string"
    )]
    pub lock_ssa_jwt: Option<String>,

    /// How often to send log messages to Lock Master (0 to turn off trasmission).
    #[serde(rename = "CEDARLING_AUDIT_LOG_INTERVAL", default)]
    pub audit_log_interval: u64,

    /// How often to send health messages to Lock Master (0 to turn off transmission).
    #[serde(rename = "CEDARLING_AUDIT_HEALTH_INTERVAL", default)]
    pub audit_health_interval: u64,

    /// How often to send telemetry messages to Lock Master (0 to turn off transmission).
    #[serde(rename = "CEDARLING_AUDIT_TELEMETRY_INTERVAL", default)]
    pub audit_health_telemetry_interval: u64,

    // Controls whether Cedarling should listen for updates from the Lock Server.
    #[serde(rename = "CEDARLING_LISTEN_SSE", default)]
    pub listen_sse: FeatureToggle,
}

#[derive(Default, Debug, PartialEq, Deserialize)]
pub enum TrustMode {
    #[default]
    Strict,
    None,
}

#[derive(Debug, PartialEq, Deserialize, Default)]
pub enum LogType {
    #[default]
    Off,
    Memory,
    StdOut,
    Lock,
}

#[derive(Debug, PartialEq, Deserialize, Default)]
pub enum FeatureToggle {
    #[default]
    Disabled,
    Enabled,
}

#[derive(Debug, thiserror::Error)]
pub enum BootstrapDecodingError {
    #[error("Failed to deserialize Bootstrap config: {0}")]
    Deserialization(#[from] serde_json::Error),
    #[error("Missing bootstrap property: `CEDARLING_LOG_TTL`. This property is required if `CEDARLING_LOG_TYPE` is set to Memory.")]
    MissingLogTTL,
    #[error("Multiple store options were provided. Make sure you only one of these properties is set: `CEDARLING_POLICY_STORE_URI` or `CEDARLING_LOCAL_POLICY_STORE`")]
    ConflictingPolicyStores,
    #[error("No Policy store was provided.")]
    MissingPolicyStore,
    #[error(
        "Unsupported policy store file format for: {0}. Supported formats include: JSON, YAML"
    )]
    UnsupportedPolicyStoreFileFormat(String),
}

impl<'de> Deserialize<'de> for BootstrapConfig {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let raw = BootstrapConfigRaw::deserialize(deserializer)?;

        // Decode LogCofig
        let log_type = match raw.log_type {
            LogType::Off => LogTypeConfig::Off,
            LogType::Memory => LogTypeConfig::Memory(MemoryLogConfig {
                log_ttl: raw
                    .log_ttl
                    .ok_or(de::Error::custom(BootstrapDecodingError::MissingLogTTL))?,
            }),
            LogType::StdOut => LogTypeConfig::StdOut,
            LogType::Lock => LogTypeConfig::Lock,
        };
        let log_config = LogConfig { log_type };

        // Decode policy store
        let policy_store_config = match (raw.policy_store_uri, raw.policy_store_local_fn) {
            // Case: no policy store provided
            (None, None) => Err(de::Error::custom(
                BootstrapDecodingError::MissingPolicyStore,
            ))?,

            // Case: get the policy store from the lock master
            (Some(policy_store_uri), None) => PolicyStoreConfig {
                source: PolicyStoreSource::LockMaster(policy_store_uri),
            },

            // Case: get the policy store from a local JSON file
            (None, Some(path)) => {
                let source = match path.to_lowercase().as_str() {
                    _ if path.ends_with(".json") => PolicyStoreSource::FileJson(path),
                    _ if path.ends_with(".yaml") || path.ends_with(".yml") => {
                        PolicyStoreSource::FileYaml(path)
                    },
                    _ => Err(de::Error::custom(
                        BootstrapDecodingError::UnsupportedPolicyStoreFileFormat(path),
                    ))?,
                };
                PolicyStoreConfig { source }
            },

            // Case: multiple polict stores were set
            (Some(_), Some(_)) => Err(de::Error::custom(
                BootstrapDecodingError::ConflictingPolicyStores,
            ))?,
        };

        // Decode JWT Config
        // TODO: update this once Jwt Service implements the new bootstrap properties
        let jwt_config = match raw.jwt_sig_validation {
            FeatureToggle::Disabled => JwtConfig::Disabled,
            FeatureToggle::Enabled => JwtConfig::Enabled {
                signature_algorithms: raw.jwt_signature_algorithms_supported,
            },
        };

        Ok(Self {
            application_name: raw.application_name,
            log_config,
            policy_store_config,
            jwt_config,
        })
    }
}

/// Custom parser for an Option<String> which returns `None` if the string is empty.
pub fn parse_option_string<'de, D>(deserializer: D) -> Result<Option<String>, D::Error>
where
    D: Deserializer<'de>,
{
    let value = Option::<String>::deserialize(deserializer)?;

    Ok(value.filter(|s| !s.is_empty()))
}