/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::{
    authorization_config::AuthorizationConfig, BootstrapConfig, IdTokenTrustMode, JwtConfig,
    LogConfig, LogTypeConfig, MemoryLogConfig, PolicyStoreConfig, PolicyStoreSource,
    TokenValidationConfig,
};
use crate::common::policy_store::PolicyStore;
use jsonwebtoken::Algorithm;
use serde::{Deserialize, Deserializer};
use std::{collections::HashSet, path::Path, str::FromStr};
use typed_builder::TypedBuilder;

#[derive(Deserialize, PartialEq, Debug, TypedBuilder)]
/// Struct that represent mapping mapping `Bootstrap properties` to be JSON and YAML compatible
/// from [link](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan#bootstrap-properties)
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
    pub log_type: LoggerType,

    /// If `log_type` is set to [`LogType::Memory`], this is the TTL (time to live) of
    /// log entities in seconds.
    #[serde(rename = "CEDARLING_LOG_TTL", default)]
    pub log_ttl: Option<u64>,

    /// When `enabled`, Cedar engine authorization is queried for a User principal.
    #[serde(rename = "CEDARLING_USER_AUTHZ", default)]
    pub user_authz: FeatureToggle,

    /// When `enabled`, Cedar engine authorization is queried for a Workload principal.
    #[serde(rename = "CEDARLING_WORKLOAD_AUTHZ", default)]
    pub workload_authz: FeatureToggle,

    /// Specifies what boolean operation to use for the `USER` and `WORKLOAD` when
    /// making authz (authorization) decisions.
    ///
    /// # Available Operations
    /// - **AND**: authz will be successful if `USER` **AND** `WORKLOAD` is valid.
    /// - **OR**: authz will be successful if `USER` **OR** `WORKLOAD` is valid.
    #[serde(rename = "CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION", default)]
    pub usr_workload_bool_op: WorkloadBoolOp,

    /// Mapping name of cedar schema User entity
    #[serde(rename = "CEDARLING_MAPPING_USER", default)]
    pub mapping_user: Option<String>,

    /// Mapping name of cedar schema Workload entity.
    #[serde(rename = "CEDARLING_MAPPING_WORKLOAD", default)]
    pub mapping_workload: Option<String>,

    /// Mapping name of cedar schema id_token entity.
    #[serde(rename = "CEDARLING_MAPPING_ID_TOKEN", default)]
    pub mapping_id_token: Option<String>,

    /// Mapping name of cedar schema access_token entity.
    #[serde(rename = "CEDARLING_MAPPING_ACCESS_TOKEN", default)]
    pub mapping_access_token: Option<String>,

    /// Mapping name of cedar schema userinfo_token entity.
    #[serde(rename = "CEDARLING_MAPPING_USERINFO_TOKEN", default)]
    pub mapping_userinfo_token: Option<String>,

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
    pub jwt_signature_algorithms_supported: HashSet<Algorithm>,

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
    pub id_token_trust_mode: IdTokenTrustMode,

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

    /// Controls whether Cedarling should listen for updates from the Lock Server.
    #[serde(rename = "CEDARLING_LISTEN_SSE", default)]
    pub listen_sse: FeatureToggle,
}

/// Type of logger
#[derive(Debug, PartialEq, Deserialize, Default)]
#[serde(rename_all = "lowercase")]
pub enum LoggerType {
    /// Disabled logger
    #[default]
    Off,
    /// Logger that collect messages in memory.
    /// Log entities available using trait [`LogStorage`](crate::LogStorage)
    Memory,
    /// Logger that print logs to stdout
    #[serde(rename = "std_out")]
    StdOut,
    /// Logger send log messages to `Lock` server
    Lock,
}

impl FromStr for LoggerType {
    type Err = ParseLoggerTypeError;

    /// Parse string to `LoggerType` enum.
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let s = s.to_lowercase();
        match s.as_str() {
            "memory" => Ok(Self::Memory),
            "std_out" => Ok(Self::StdOut),
            "lock" => Ok(Self::Lock),
            "off" => Ok(Self::Off),
            _ => Err(Self::Err { logger_type: s }),
        }
    }
}

/// Enum varians that represent if feature is enabled or disabled
#[derive(Debug, PartialEq, Deserialize, Default, Copy, Clone)]
#[serde(rename_all = "lowercase")]
pub enum FeatureToggle {
    /// Represent as disabled.
    #[default]
    Disabled,
    /// Represent as enabled.
    Enabled,
}

impl From<FeatureToggle> for bool {
    fn from(value: FeatureToggle) -> bool {
        match value {
            FeatureToggle::Disabled => false,
            FeatureToggle::Enabled => true,
        }
    }
}

impl TryFrom<String> for FeatureToggle {
    type Error = ParseFeatureToggleError;

    fn try_from(s: String) -> Result<Self, Self::Error> {
        let s = s.to_lowercase();
        match s.as_str() {
            "enabled" => Ok(FeatureToggle::Enabled),
            "disabled" => Ok(FeatureToggle::Disabled),
            _ => Err(ParseFeatureToggleError { value: s }),
        }
    }
}

impl FromStr for FeatureToggle {
    type Err = ParseFeatureToggleError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let s = s.to_lowercase();
        match s.as_str() {
            "enabled" => Ok(FeatureToggle::Enabled),
            "disabled" => Ok(FeatureToggle::Disabled),
            _ => Err(ParseFeatureToggleError { value: s }),
        }
    }
}

impl FeatureToggle {
    /// Parse bool to `FeatureToggle`.
    pub fn from_bool(v: bool) -> Self {
        match v {
            true => Self::Enabled,
            false => Self::Disabled,
        }
    }

    /// Return true if is enabled.
    pub fn is_enabled(&self) -> bool {
        match self {
            Self::Enabled => true,
            Self::Disabled => false,
        }
    }
}

impl From<bool> for FeatureToggle {
    fn from(val: bool) -> Self {
        FeatureToggle::from_bool(val)
    }
}

#[derive(Default, Clone, Copy, Debug, PartialEq, Deserialize)]
#[serde(rename_all = "UPPERCASE")]
/// Operator that define boolean operator `AND` or `OR`.
pub enum WorkloadBoolOp {
    #[default]
    /// Variant boolean `AND` operator.
    And,
    /// Variant boolean `OR` operator.
    Or,
}

impl FromStr for WorkloadBoolOp {
    type Err = ParseWorkloadBoolOpError;

    /// Parse [`WorkloadBoolOp`] from string.
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let s = s.to_uppercase();
        Ok(match s.as_str() {
            "AND" => Self::And,
            "OR" => Self::Or,
            _ => return Err(ParseWorkloadBoolOpError { payload: s }),
        })
    }
}

impl WorkloadBoolOp {
    /// execute boolean operator for boolean parameters
    pub(crate) fn calc(&self, rhd: bool, lhd: bool) -> bool {
        match self {
            WorkloadBoolOp::And => rhd && lhd,
            WorkloadBoolOp::Or => rhd || lhd,
        }
    }
}

#[derive(Default, Debug, derive_more::Display, derive_more::Error)]
#[display("Could not parce `WorkloadBoolOp` with payload {payload}, should be `AND` or `OR`")]
pub struct ParseWorkloadBoolOpError {
    payload: String,
}

#[derive(Default, Debug, derive_more::Display, derive_more::Error)]
#[display("Invalid `TrustMode`: {trust_mode}. should be `strict` or `none`")]
pub struct ParseTrustModeError {
    trust_mode: String,
}

#[derive(Default, Debug, derive_more::Display, derive_more::Error)]
#[display("Invalid `LoggerType`: {logger_type}. should be `memory`, `std_out`, `lock`, or `off`")]
pub struct ParseLoggerTypeError {
    logger_type: String,
}

#[derive(Default, Debug, derive_more::Display, derive_more::Error)]
#[display("Invalid `FeatureToggle`: {value}. should be `enabled`, or `disabled`")]
pub struct ParseFeatureToggleError {
    value: String,
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

impl BootstrapConfig {
    /// Construct an instance from BootstrapConfigRaw
    pub fn from_raw_config(raw: &BootstrapConfigRaw) -> Result<Self, Box<dyn std::error::Error>> {
        // Decode LogCofig
        let log_type = match raw.log_type {
            LoggerType::Off => LogTypeConfig::Off,
            LoggerType::Memory => LogTypeConfig::Memory(MemoryLogConfig {
                log_ttl: raw.log_ttl.ok_or(BootstrapDecodingError::MissingLogTTL)?,
            }),
            LoggerType::StdOut => LogTypeConfig::StdOut,
            LoggerType::Lock => LogTypeConfig::Lock,
        };
        let log_config = LogConfig { log_type };

        // Decode policy store
        let policy_store_config = match (
            raw.policy_store_uri.clone(),
            raw.policy_store_local_fn.clone(),
        ) {
            // Case: no policy store provided
            (None, None) => Err(BootstrapDecodingError::MissingPolicyStore)?,

            // Case: get the policy store from the lock master
            (Some(policy_store_uri), None) => PolicyStoreConfig {
                source: PolicyStoreSource::LockMaster(policy_store_uri),
            },

            // Case: get the policy store from a local JSON file
            (None, Some(raw_path)) => {
                let path = Path::new(&raw_path);
                let file_ext = Path::new(&path)
                    .extension()
                    .and_then(|ext| ext.to_str())
                    .map(|x| x.to_lowercase());

                let source = match file_ext.as_deref() {
                    Some("json") => PolicyStoreSource::FileJson(path.into()),
                    Some("yaml") | Some("yml") => PolicyStoreSource::FileYaml(path.into()),
                    _ => Err(BootstrapDecodingError::UnsupportedPolicyStoreFileFormat(
                        raw_path,
                    ))?,
                };
                PolicyStoreConfig { source }
            },

            // Case: multiple polict stores were set
            (Some(_), Some(_)) => Err(BootstrapDecodingError::ConflictingPolicyStores)?,
        };

        // JWT Config
        let jwt_config = JwtConfig {
            jwks: None,
            jwt_sig_validation: raw.jwt_sig_validation.into(),
            jwt_status_validation: raw.jwt_status_validation.into(),
            id_token_trust_mode: raw.id_token_trust_mode,
            signature_algorithms_supported: raw.jwt_signature_algorithms_supported.clone(),
            access_token_config: TokenValidationConfig {
                iss_validation: raw.at_iss_validation.into(),
                jti_validation: raw.at_jti_validation.into(),
                nbf_validation: raw.at_nbf_validation.into(),
                exp_validation: raw.at_exp_validation.into(),
                ..Default::default()
            },
            id_token_config: TokenValidationConfig {
                iss_validation: raw.idt_iss_validation.into(),
                aud_validation: raw.idt_aud_validation.into(),
                sub_validation: raw.idt_sub_validation.into(),
                exp_validation: raw.idt_exp_validation.into(),
                iat_validation: raw.idt_iat_validation.into(),
                ..Default::default()
            },
            userinfo_token_config: TokenValidationConfig {
                iss_validation: raw.userinfo_iss_validation.into(),
                aud_validation: raw.userinfo_aud_validation.into(),
                sub_validation: raw.userinfo_sub_validation.into(),
                exp_validation: raw.userinfo_exp_validation.into(),
                ..Default::default()
            },
        };

        let authorization_config = AuthorizationConfig {
            use_user_principal: raw.user_authz.is_enabled(),
            use_workload_principal: raw.workload_authz.is_enabled(),
            user_workload_operator: raw.usr_workload_bool_op,

            mapping_user: raw.mapping_user.clone(),
            mapping_workload: raw.mapping_workload.clone(),
            mapping_id_token: raw.mapping_id_token.clone(),
            mapping_access_token: raw.mapping_access_token.clone(),
            mapping_userinfo_token: raw.mapping_userinfo_token.clone(),
        };

        Ok(Self {
            application_name: raw.application_name.clone(),
            log_config,
            policy_store_config,
            jwt_config,
            authorization_config,
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
