/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::bindings::PolicyStore;
use cedarling::{BootstrapConfigRaw, IdTokenTrustMode, LoggerType, WorkloadBoolOp};
use jsonwebtoken::Algorithm;
use pyo3::exceptions::{PyKeyError, PyValueError};
use pyo3::prelude::*;
use pyo3::types::PyDict;
use std::collections::HashSet;
use std::str::FromStr;

/// BootstrapConfig
/// ===================
///
/// A Python wrapper for the Rust `BootstrapConfig` struct.
/// Configures the application, including authorization, logging, JWT validation, and policy store settings.
///
/// Attributes
/// ----------
/// :param application_name: A human-friendly identifier for the application.
/// :param policy_store_uri: Optional URI of the policy store JSON file.
/// :param policy_store_id: An identifier for the policy store.
/// :param log_type: Log type, e.g., 'none', 'memory', 'std_out', or 'lock'.
/// :param log_ttl: (Optional) TTL (time to live) in seconds for log entities when `log_type` is 'memory'. The default is 60s.
/// :param decision_log_user_claims: List of claims to map from user entity, such as ["sub", "email", "username", ...]
/// :param decision_log_workload_claims: List of claims to map from user entity, such as ["client_id", "rp_id", ...]
/// :param decision_log_default_jwt_id: Token claims that will be used for decision logging. Default is "jti".
/// :param user_authz: Enables querying Cedar engine authorization for a User principal.
/// :param workload_authz: Enables querying Cedar engine authorization for a Workload principal.
/// :param usr_workload_bool_op: Boolean operation ('AND' or 'OR') for combining `USER` and `WORKLOAD` authz results.
/// :param local_jwks: Path to a local file containing a JWKS.
/// :param local_policy_store: A JSON string containing a policy store.
/// :param policy_store_local_fn: Path to a policy store JSON file.
/// :param jwt_sig_validation: Validates JWT signatures if enabled.
/// :param jwt_status_validation: Validates JWT status on startup if enabled.
/// :param jwt_signature_algorithms_supported: A list of supported JWT signature algorithms.
/// :param at_iss_validation: When enabled, the `iss` (Issuer) claim must be present in the Access Token and thescheme must be `https`.
/// :param at_jti_validation: When enabled, the `jti` (JWT ID) claim must be present in the Access Token.
/// :param at_nbf_validation: When enabled, the `nbf` (Not Before) claim must be present in the Access Token.
/// :param at_exp_validation: When enabled, the `exp` (Expiration) claim must be present in the Access Token.
/// :param idt_iss_validation: When enabled, the `iss` (Issuer) claim must be present in the ID Token.
/// :param idt_sub_validation: When enabled, the `sub` (Subject) claim must be present in the ID Token.
/// :param idt_exp_validation: When enabled, the `exp` (Expiration) claim must be present in the ID Token.
/// :param idt_iat_validation: When enabled, the `iat` (Issued At) claim must be present in the ID Token.
/// :param idt_aud_validation: When enabled, the `aud` (Audience) claim must be present in the ID Token.
/// :param userinfo_iss_validation: When enabled, the `iss` (Issuer) claim must be present in the Userinfo Token.
/// :param userinfo_sub_validation: When enabled, the `sub` (Subject) claim must be present in the Userinfo Token.
/// :param userinfo_aud_validation: When enabled, the `aud` (Audience) claim must be present in the Userinfo Token.
/// :param userinfo_exp_validation: When enabled, the `exp` (Expiration) claim must be present in the Userinfo Token.
/// :param id_token_trust_mode: Trust mode for ID tokens, either 'None' or 'Strict'.
/// :param lock: Enables integration with Lock Master for policies and SSE events.
/// :param lock_master_configuration_uri: URI where Cedarling can get JSON file with all required metadata about Lock Master, i.e. .well-known/lock-master-configuration.
/// :param dynamic_configuration: Toggles listening for SSE config updates.
/// :param lock_ssa_jwt: SSA for DCR in a Lock Master deployment. Cedarling will validate this SSA JWT prior to DCR.
/// :param audit_log_interval: Interval (in seconds) for sending log messages to Lock Master (0 to disable).
/// :param audit_health_interval: Interval (in seconds) for sending health updates to Lock Master (0 to disable).
/// :param audit_health_telemetry_interval: Interval (in seconds) for sending telemetry updates to Lock Master (0 to disable).
/// :param listen_sse: Toggles listening for updates from the Lock Server.
///
/// Example
/// -------
/// ```python
/// from cedarling import BootstrapConfig
/// # Example configuration
/// bootstrap_config = BootstrapConfig({
///     "application_name": "MyApp",
///     "policy_store_uri": None,
///     "policy_store_id": "policy123",
///     "log_type": "memory",
///     "log_ttl": 86400,
///     "decision_log_user_claims": ["sub", "email", "username"]
///     "decision_log_workload_claims": ["client_id", "rp_id"]
///     "decision_log_default_jwt_id":"jti"
///     "user_authz": "enabled",
///     "workload_authz": "enabled",
///     "usr_workload_bool_op": "AND",
///     "local_jwks": "./path/to/your_jwks.json",
///     "local_policy_store": None,
///     "policy_store_local_fn": "./path/to/your_policy_store.json",
///     "jwt_sig_validation": "enabled",
///     "jwt_status_validation": "disabled",
///     "at_iss_validation": "enabled",
///     "at_jti_validation": "enabled",
///     "at_nbf_validation": "disabled",
///     "idt_iss_validation": "enabled",
///     "idt_sub_validation": "enabled",
///     "idt_exp_validation": "enabled",
///     "idt_iat_validation": "enabled",
///     "idt_aud_validation": "enabled",
///     "userinfo_iss_validation": "enabled",
///     "userinfo_sub_validation": "enabled",
///     "userinfo_aud_validation": "enabled",
///     "userinfo_exp_validation": "enabled",
///     "id_token_trust_mode": "Strict",
///     "lock": "disabled",
///     "lock_master_configuration_uri": None,
///     "dynamic_configuration": "disabled",
///     "lock_ssa_jwt": None,
///     "audit_log_interval": 0,
///     "audit_health_interval": 0,
///     "audit_health_telemetry_interval": 0,
///     "listen_sse": "disabled",
/// })
/// ```
#[derive(Debug, Clone)]
#[pyclass]
#[pyo3(get_all, set_all)]
pub struct BootstrapConfig {
    ///  Human friendly identifier for the application
    pub application_name: String,

    /// Location of policy store JSON, used if policy store is not local, or retreived
    /// from Lock Master.
    pub policy_store_uri: Option<String>,

    /// An identifier for the policy store.
    pub policy_store_id: String,

    /// How the logs will be presented.
    ///
    /// Could be set to: 'off' | 'memory' | 'std_out' | 'lock'
    pub log_type: String,

    /// List of claims to map from user entity, such as ["sub", "email", "username", ...]
    pub decision_log_user_claims: Vec<String>,

    /// List of claims to map from user entity, such as ["client_id", "rp_id", ...]
    pub decision_log_workload_claims: Vec<String>,

    /// Token claims that will be used for decision logging.
    /// Default is jti, but perhaps some other claim is needed.
    pub decision_log_default_jwt_id: String,

    /// If `log_type` is set to [`LogType::Memory`], this is the TTL (time to live) of
    /// log entities in seconds.
    ///
    /// Defaults to 60 secs if not provided.
    pub log_ttl: Option<u64>,

    /// When enabled, Cedar engine authorization is queried for a User principal.
    pub user_authz: String,

    /// When enabled, Cedar engine authorization is queried for a Workload principal.
    pub workload_authz: String,

    /// Specifies what boolean operation to use for the `USER` and `WORKLOAD` when
    /// making authz (authorization) decisions.
    ///
    /// # Available Operations
    /// - **'AND'**: authz will be successful if `USER` **AND** `WORKLOAD` is valid.
    /// - **'OR'**: authz will be successful if `USER` **OR** `WORKLOAD` is valid.
    pub usr_workload_bool_op: String,

    /// Path to a local file containing a JWKS.
    pub local_jwks: Option<String>,

    /// JSON object with policy store
    pub local_policy_store: Option<String>,

    /// Path to a Policy Store JSON file
    pub policy_store_local_fn: Option<String>,

    /// Whether to check the signature of all JWT tokens.
    ///
    /// This requires that an `iss` (Issuer) claim is present on each token.
    pub jwt_sig_validation: String,

    /// Whether to check the status of the JWT. On startup.
    ///
    /// Cedarling will fetch and retreive the latest Status List JWT from the
    /// `.well-known/openid-configuration` via the `status_list_endpoint` claim and
    /// cache it. See the [`IETF Draft`] for more info.
    ///
    /// [`IETF Draft`]: https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/
    pub jwt_status_validation: String,

    /// Cedarling will only accept tokens signed with these algorithms.
    pub jwt_signature_algorithms_supported: Vec<String>,

    /// When enabled, the `iss` (Issuer) claim must be present in the Access Token and
    /// the scheme must be `https`.
    pub at_iss_validation: String,

    /// When enabled, the `jti` (JWT ID) claim must be present in the Access Token.
    pub at_jti_validation: String,

    /// When enabled, the `nbf` (Not Before) claim must be present in the Access Token
    /// and Cedarling will verify that the current date is after the `nbf`.
    pub at_nbf_validation: String,

    /// When enabled, the `exp` (Expiration) claim must be present in the Access Token
    /// and not past the date specified.
    pub at_exp_validation: String,

    /// When enabled, the `iss` (Issuer) claim must be present in the ID Token and
    /// the scheme must be https.
    pub idt_iss_validation: String,

    /// When enabled, the `sub` (Subject) claim must be present in the ID Token.
    pub idt_sub_validation: String,

    /// When enabled, the `exp` (Expiration) claim must be present in the ID Token
    /// and not past the date specified.
    pub idt_exp_validation: String,

    /// When enabled, the `iat` (Issued at) claim must be present in the ID Token.
    pub idt_iat_validation: String,

    /// When enabled, the `aud` ( Audience) claim must be present in the ID Token.
    pub idt_aud_validation: String,

    /// When enabled, the `iss` (Issuer) claim must be present in the Userinfo Token and
    /// the scheme must be https.
    pub userinfo_iss_validation: String,

    /// When enabled, the `sub` (Subject) claim must be present in the Userinfo Token.
    pub userinfo_sub_validation: String,

    /// When enabled, the `aud` (Audience) claim must be present in the Userinfo Token.
    pub userinfo_aud_validation: String,

    /// When enabled, the `exp` (Expiration) claim must be present in the Userinfo Token
    /// and not past the date specified.
    pub userinfo_exp_validation: String,

    /// Varying levels of validations based on the preference of the developer.
    ///
    /// Could be set to either `'none'` or `'strict'`.
    ///
    /// # Strict Mode
    ///
    /// Strict mode requires:
    ///     1. id_token aud matches the access_token client_id;
    ///     2. if a Userinfo token is present, the sub matches the id_token, and that
    ///         the aud matches the access token client_id.
    pub id_token_trust_mode: String,

    /// If Enabled, the Cedarling will connect to the Lock Master for policies,
    /// and subscribe for SSE events.
    pub lock: String,

    /// URI where Cedarling can get JSON file with all required metadata about
    /// Lock Master, i.e. .well-known/lock-master-configuration.
    ///
    /// ***Required*** if `LOCK == Enabled`.
    pub lock_master_configuration_uri: Option<String>,

    /// Controls whether Cedarling should listen for SSE config updates.
    pub dynamic_configuration: String,

    /// SSA for DCR in a Lock Master deployment. The Cedarling will validate this
    /// SSA JWT prior to DCR.
    pub lock_ssa_jwt: Option<String>,

    /// How often to send log messages to Lock Master (0 to turn off trasmission).
    pub audit_log_interval: u64,

    /// How often to send health messages to Lock Master (0 to turn off transmission).
    pub audit_health_interval: u64,

    /// How often to send telemetry messages to Lock Master (0 to turn off transmission).
    pub audit_health_telemetry_interval: u64,

    // Controls whether Cedarling should listen for updates from the Lock Server.
    pub listen_sse: String,
}

#[pymethods]
impl BootstrapConfig {
    #[new]
    pub fn new(options: &Bound<'_, PyDict>) -> PyResult<Self> {
        let app_name = get_required(options, "application_name")?;
        let policy_store_id = get_required(options, "policy_store_id")?;
        let policy_store_uri = get_optional(options, "policy_store_uri")?;
        let log_type = get_with_default(options, "log_type", "memory".to_string())?;
        let log_ttl = get_with_default(options, "log_ttl", Some(60))?;
        let decision_log_user_claims =
            get_with_default(options, "decision_log_user_claims", Vec::default())?;
        let decision_log_workload_claims =
            get_with_default(options, "decision_log_workload_claims", Vec::default())?;
        let decision_log_default_jwt_id =
            get_with_default(options, "decision_log_default_jwt_id", "jti".to_string())?;
        let user_authz = get_with_default(options, "user_authz", "enabled".to_string())?;
        let workload_authz = get_with_default(options, "workload_authz", "enabled".to_string())?;
        let usr_workload_bool_op =
            get_with_default(options, "usr_workload_bool_op", "AND".to_string())?;
        let local_jwks = get_optional(options, "local_jwks")?;
        let local_policy_store = get_optional(options, "local_policy_store")?;
        let policy_store_local_fn = get_optional(options, "policy_store_local_fn")?;
        let jwt_sig_validation =
            get_with_default(options, "jwt_sig_validation", "enabled".to_string())?;
        let jwt_status_validation =
            get_with_default(options, "jwt_status_validation", "enabled".to_string())?;
        let jwt_signature_algorithms_supported =
            get_with_default(options, "jwt_signature_algorithms_supported", Vec::new())?;
        let at_iss_validation =
            get_with_default(options, "at_iss_validation", "enabled".to_string())?;
        let at_jti_validation =
            get_with_default(options, "at_jti_validation", "enabled".to_string())?;
        let at_nbf_validation =
            get_with_default(options, "at_nbf_validation", "enabled".to_string())?;
        let at_exp_validation =
            get_with_default(options, "at_exp_validation", "enabled".to_string())?;
        let idt_iss_validation =
            get_with_default(options, "idt_iss_validation", "enabled".to_string())?;
        let idt_sub_validation =
            get_with_default(options, "idt_sub_validation", "enabled".to_string())?;
        let idt_exp_validation =
            get_with_default(options, "idt_exp_validation", "enabled".to_string())?;
        let idt_iat_validation =
            get_with_default(options, "idt_iat_validation", "enabled".to_string())?;
        let idt_aud_validation =
            get_with_default(options, "idt_aud_validation", "enabled".to_string())?;
        let userinfo_iss_validation =
            get_with_default(options, "userinfo_iss_validation", "enabled".to_string())?;
        let userinfo_sub_validation =
            get_with_default(options, "userinfo_sub_validation", "enabled".to_string())?;
        let userinfo_aud_validation =
            get_with_default(options, "userinfo_aud_validation", "enabled".to_string())?;
        let userinfo_exp_validation =
            get_with_default(options, "userinfo_exp_validation", "enabled".to_string())?;
        let id_token_trust_mode =
            get_with_default(options, "id_token_trust_mode", "strict".to_string())?;
        let lock = get_with_default(options, "lock", "disabled".to_string())?;
        let lock_master_configuration_uri = get_optional(options, "lock_master_configuration_uri")?;
        let dynamic_configuration =
            get_with_default(options, "dynamic_configuration", "disabled".to_string())?;
        let lock_ssa_jwt = get_optional(options, "lock_ssa_jwt")?;
        let audit_log_interval = get_with_default(options, "audit_log_interval", 0)?;
        let audit_health_interval = get_with_default(options, "audit_health_interval", 0)?;
        let audit_health_telemetry_interval =
            get_with_default(options, "audit_health_telemetry_interval", 0)?;
        let listen_sse = get_with_default(options, "listen_sse", "disabled".to_string())?;

        Ok(Self {
            application_name: app_name,
            policy_store_id,
            policy_store_uri,
            log_type,
            log_ttl,
            decision_log_user_claims,
            decision_log_workload_claims,
            decision_log_default_jwt_id,
            user_authz,
            workload_authz,
            usr_workload_bool_op,
            local_jwks,
            local_policy_store,
            policy_store_local_fn,
            jwt_sig_validation,
            jwt_status_validation,
            jwt_signature_algorithms_supported,
            at_iss_validation,
            at_jti_validation,
            at_nbf_validation,
            at_exp_validation,
            idt_iss_validation,
            idt_sub_validation,
            idt_exp_validation,
            idt_iat_validation,
            idt_aud_validation,
            userinfo_iss_validation,
            userinfo_sub_validation,
            userinfo_aud_validation,
            userinfo_exp_validation,
            id_token_trust_mode,
            lock,
            lock_master_configuration_uri,
            dynamic_configuration,
            lock_ssa_jwt,
            audit_log_interval,
            audit_health_interval,
            audit_health_telemetry_interval,
            listen_sse,
        })
    }

    pub fn disable_all_jwt_validation(&mut self) {
        self.jwt_sig_validation = "disabled".to_string();
        self.jwt_status_validation = "disabled".to_string();

        self.at_iss_validation = "disabled".to_string();
        self.at_jti_validation = "disabled".to_string();
        self.at_nbf_validation = "disabled".to_string();
        self.at_exp_validation = "disabled".to_string();

        self.idt_iss_validation = "disabled".to_string();
        self.idt_sub_validation = "disabled".to_string();
        self.idt_exp_validation = "disabled".to_string();
        self.idt_iat_validation = "disabled".to_string();
        self.idt_aud_validation = "disabled".to_string();
        self.id_token_trust_mode = "none".to_string();

        self.userinfo_iss_validation = "disabled".to_string();
        self.userinfo_aud_validation = "disabled".to_string();
        self.userinfo_sub_validation = "disabled".to_string();
        self.userinfo_exp_validation = "disabled".to_string();
    }
}

fn get_required<'py, T>(options: &Bound<'py, PyDict>, key: &str) -> PyResult<T>
where
    T: FromPyObject<'py>,
{
    options
        .get_item(key)?
        .ok_or_else(|| PyKeyError::new_err(format!("Missing required config: `{}`", key)))?
        .extract()
}

fn get_optional<'py, T>(options: &Bound<'py, PyDict>, key: &str) -> PyResult<Option<T>>
where
    T: FromPyObject<'py>,
{
    // let opt = options.get_item(key)?;
    options.get_item(key)?.map(|x| x.extract()).transpose()
}

fn get_with_default<'py, T>(options: &Bound<'py, PyDict>, key: &str, default: T) -> PyResult<T>
where
    T: FromPyObject<'py> + Clone,
{
    Ok(options
        .get_item(key)?
        .map(|x| x.extract())
        .transpose()?
        .unwrap_or(default))
}

impl TryFrom<BootstrapConfig> for cedarling::BootstrapConfig {
    type Error = PyErr;

    fn try_from(value: BootstrapConfig) -> Result<Self, Self::Error> {
        let mut signature_algorithms = HashSet::new();
        for alg in value.jwt_signature_algorithms_supported.iter() {
            let alg = Algorithm::from_str(alg).map_err(|e| PyValueError::new_err(e.to_string()))?;
            signature_algorithms.insert(alg);
        }

        // I think it would be better to include `BootstrapConfigRaw` within the `BootstrapConfig` structure.
        let raw_config = BootstrapConfigRaw {
            application_name: value.application_name,
            policy_store_uri: value.policy_store_uri,
            policy_store_id: value.policy_store_id,
            log_type: LoggerType::from_str(value.log_type.as_str()).unwrap_or_default(),
            log_ttl: value.log_ttl,
            decision_log_user_claims: value.decision_log_user_claims,
            decision_log_workload_claims: value.decision_log_workload_claims,
            decision_log_default_jwt_id: value.decision_log_default_jwt_id,
            user_authz: value.user_authz.try_into().unwrap_or_default(),
            workload_authz: value.workload_authz.try_into().unwrap_or_default(),
            usr_workload_bool_op: WorkloadBoolOp::from_str(value.usr_workload_bool_op.as_str())
                .map_err(|err| {
                    PyValueError::new_err(format!(
                        "could not parce field: usr_workload_bool_op, {err}"
                    ))
                })?,
            local_jwks: value.local_jwks,
            local_policy_store: if let Some(policy_store_str) = value.local_policy_store {
                let store: PolicyStore =
                    serde_json::from_str(policy_store_str.as_str()).map_err(|err| {
                        PyValueError::new_err(format!(
                            "could not parse field local_policy_store as json to PolicyStore, {err}"
                        ))
                    })?;

                Some(store)
            } else {
                None
            },
            policy_store_local_fn: value.policy_store_local_fn,
            jwt_sig_validation: value
                .jwt_sig_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            jwt_status_validation: value
                .jwt_status_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            jwt_signature_algorithms_supported: signature_algorithms,
            at_iss_validation: value
                .at_iss_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            at_jti_validation: value
                .at_jti_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            at_nbf_validation: value
                .at_nbf_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            at_exp_validation: value
                .at_exp_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            idt_iss_validation: value
                .idt_iss_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            idt_sub_validation: value
                .idt_sub_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            idt_exp_validation: value
                .idt_exp_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            idt_iat_validation: value
                .idt_iat_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            idt_aud_validation: value
                .idt_aud_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            userinfo_iss_validation: value
                .userinfo_iss_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            userinfo_sub_validation: value
                .userinfo_sub_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            userinfo_aud_validation: value
                .userinfo_aud_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            userinfo_exp_validation: value
                .userinfo_exp_validation
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            id_token_trust_mode: IdTokenTrustMode::from_str(value.id_token_trust_mode.as_str())
                .unwrap_or_default(),
            lock: value
                .lock
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            lock_master_configuration_uri: value.lock_master_configuration_uri,
            dynamic_configuration: value
                .dynamic_configuration
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
            lock_ssa_jwt: value.lock_ssa_jwt,
            audit_log_interval: value.audit_log_interval,
            audit_health_interval: value.audit_health_interval,
            audit_health_telemetry_interval: value.audit_health_telemetry_interval,
            listen_sse: value
                .listen_sse
                .try_into()
                .map_err(|e| PyValueError::new_err(format!("{}", e)))?,
        };

        Self::from_raw_config(&raw_config).map_err(|err| {
            PyValueError::new_err(format!("could not parse config from raw config, {err}"))
        })
    }
}
