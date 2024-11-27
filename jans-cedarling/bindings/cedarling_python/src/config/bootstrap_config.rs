/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use cedarling::bindings::PolicyStore;
use cedarling::{BootstrapConfigRaw, LoggerType, TrustMode, WorkloadBoolOp};
use jsonwebtoken::Algorithm;
use pyo3::exceptions::PyValueError;
use pyo3::prelude::*;
use std::collections::HashSet;
use std::str::FromStr;

/// BootstrapConfig
/// ===================
///
/// A Python wrapper for the Rust `NewBootstrapConfig` struct.
/// Configures the application, including authorization, logging, JWT validation, and policy store settings.
///
/// Attributes
/// ----------
/// :param application_name: A human-friendly identifier for the application.
/// :param policy_store_uri: Optional URI of the policy store JSON file.
/// :param policy_store_id: An identifier for the policy store.
/// :param log_type: Log type, e.g., 'none', 'memory', 'std_out', or 'lock'.
/// :param log_ttl: (Optional) TTL (time to live) in seconds for log entities when `log_type` is 'memory'. The default is 60s.
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
///
/// # Example configuration
/// bootstrap_config = NewBootstrapConfig(
///     application_name="MyApp",
///     policy_store_uri=None,
///     policy_store_id="policy123",
///     log_type="memory",
///     log_ttl=60,
///     user_authz=True,
///     workload_authz=True,
///     usr_workload_bool_op="AND",
///     local_jwks="./path/to/your_jwks.json",
///     local_policy_store=None,
///     policy_store_local_fn="./path/to/your_policy_store.json",
///     jwt_sig_validation=True,
///     jwt_status_validation=False,
///     at_iss_validation=True,
///     at_jti_validation=True,
///     at_nbf_validation=False,
///     idt_iss_validation=True,
///     idt_sub_validation=True,
///     idt_exp_validation=True,
///     idt_iat_validation=True,
///     idt_aud_validation=True,
///     userinfo_iss_validation=True,
///     userinfo_sub_validation=True,
///     userinfo_aud_validation=True,
///     userinfo_exp_validation=True,
///     id_token_trust_mode="Strict",
///     lock=True,
///     lock_master_configuration_uri=None,
///     dynamic_configuration=False,
///     lock_ssa_jwt=None,
///     audit_log_interval=0,
///     audit_health_interval=0,
///     audit_health_telemetry_interval=0,
///     listen_sse=False,
/// )
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

    /// If `log_type` is set to [`LogType::Memory`], this is the TTL (time to live) of
    /// log entities in seconds.
    ///
    /// Defaults to 60 secs if not provided.
    pub log_ttl: Option<u64>,

    /// When enabled, Cedar engine authorization is queried for a User principal.
    pub user_authz: bool,

    /// When enabled, Cedar engine authorization is queried for a Workload principal.
    pub workload_authz: bool,

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
    pub jwt_sig_validation: bool,

    /// Whether to check the status of the JWT. On startup.
    ///
    /// Cedarling will fetch and retreive the latest Status List JWT from the
    /// `.well-known/openid-configuration` via the `status_list_endpoint` claim and
    /// cache it. See the [`IETF Draft`] for more info.
    ///
    /// [`IETF Draft`]: https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/
    pub jwt_status_validation: bool,

    /// Cedarling will only accept tokens signed with these algorithms.
    pub jwt_signature_algorithms_supported: Vec<String>,

    /// When enabled, the `iss` (Issuer) claim must be present in the Access Token and
    /// the scheme must be `https`.
    pub at_iss_validation: bool,

    /// When enabled, the `jti` (JWT ID) claim must be present in the Access Token.
    pub at_jti_validation: bool,

    /// When enabled, the `nbf` (Not Before) claim must be present in the Access Token
    /// and Cedarling will verify that the current date is after the `nbf`.
    pub at_nbf_validation: bool,

    /// When enabled, the `exp` (Expiration) claim must be present in the Access Token
    /// and not past the date specified.
    pub at_exp_validation: bool,

    /// When enabled, the `iss` (Issuer) claim must be present in the ID Token and
    /// the scheme must be https.
    pub idt_iss_validation: bool,

    /// When enabled, the `sub` (Subject) claim must be present in the ID Token.
    pub idt_sub_validation: bool,

    /// When enabled, the `exp` (Expiration) claim must be present in the ID Token
    /// and not past the date specified.
    pub idt_exp_validation: bool,

    /// When enabled, the `iat` (Issued at) claim must be present in the ID Token.
    pub idt_iat_validation: bool,

    /// When enabled, the `aud` ( Audience) claim must be present in the ID Token.
    pub idt_aud_validation: bool,

    /// When enabled, the `iss` (Issuer) claim must be present in the Userinfo Token and
    /// the scheme must be https.
    pub userinfo_iss_validation: bool,

    /// When enabled, the `sub` (Subject) claim must be present in the Userinfo Token.
    pub userinfo_sub_validation: bool,

    /// When enabled, the `aud` (Audience) claim must be present in the Userinfo Token.
    pub userinfo_aud_validation: bool,

    /// When enabled, the `exp` (Expiration) claim must be present in the Userinfo Token
    /// and not past the date specified.
    pub userinfo_exp_validation: bool,

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
    pub lock: bool,

    /// URI where Cedarling can get JSON file with all required metadata about
    /// Lock Master, i.e. .well-known/lock-master-configuration.
    ///
    /// ***Required*** if `LOCK == Enabled`.
    pub lock_master_configuration_uri: Option<String>,

    /// Controls whether Cedarling should listen for SSE config updates.
    pub dynamic_configuration: bool,

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
    pub listen_sse: bool,
}

#[pymethods]
impl BootstrapConfig {
    #[new]
    #[pyo3(signature = (
        application_name,
        policy_store_id,
        policy_store_uri = None,
        log_type = "memory".to_string(),
        log_ttl = Some(60),
        user_authz = true,
        workload_authz = true,
        usr_workload_bool_op = "AND".to_string(),
        local_jwks = None,
        local_policy_store = None,
        policy_store_local_fn = None,
        jwt_sig_validation = true,
        jwt_status_validation = false,
        jwt_signature_algorithms_supported = vec!["RS256".to_string()],
        at_iss_validation = true,
        at_jti_validation = true,
        at_nbf_validation = true,
        at_exp_validation = true,
        idt_iss_validation = true,
        idt_sub_validation = true,
        idt_exp_validation = true,
        idt_iat_validation = true,
        idt_aud_validation = true,
        userinfo_iss_validation = true,
        userinfo_sub_validation = true,
        userinfo_aud_validation = true,
        userinfo_exp_validation = true,
        id_token_trust_mode = "none".to_string(),
        lock = false,
        lock_master_configuration_uri = None,
        dynamic_configuration = false,
        lock_ssa_jwt = None,
        audit_log_interval = 0,
        audit_health_interval = 0,
        audit_health_telemetry_interval = 0,
        listen_sse = false
    ))]
    pub fn new(
        application_name: String,
        policy_store_id: String,
        policy_store_uri: Option<String>,
        log_type: String,
        log_ttl: Option<u64>,
        user_authz: bool,
        workload_authz: bool,
        usr_workload_bool_op: String,
        local_jwks: Option<String>,
        local_policy_store: Option<String>,
        policy_store_local_fn: Option<String>,
        jwt_sig_validation: bool,
        jwt_status_validation: bool,
        jwt_signature_algorithms_supported: Vec<String>,
        at_iss_validation: bool,
        at_jti_validation: bool,
        at_nbf_validation: bool,
        at_exp_validation: bool,
        idt_iss_validation: bool,
        idt_sub_validation: bool,
        idt_exp_validation: bool,
        idt_iat_validation: bool,
        idt_aud_validation: bool,
        userinfo_iss_validation: bool,
        userinfo_sub_validation: bool,
        userinfo_aud_validation: bool,
        userinfo_exp_validation: bool,
        id_token_trust_mode: String,
        lock: bool,
        lock_master_configuration_uri: Option<String>,
        dynamic_configuration: bool,
        lock_ssa_jwt: Option<String>,
        audit_log_interval: u64,
        audit_health_interval: u64,
        audit_health_telemetry_interval: u64,
        listen_sse: bool,
    ) -> PyResult<Self> {
        Ok(Self {
            application_name,
            policy_store_uri,
            policy_store_id,
            log_type,
            log_ttl,
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
        self.jwt_sig_validation = false;
        self.jwt_status_validation = false;

        self.at_iss_validation = false;
        self.at_jti_validation = false;
        self.at_nbf_validation = false;
        self.at_exp_validation = false;

        self.idt_iss_validation = false;
        self.idt_sub_validation = false;
        self.idt_exp_validation = false;
        self.idt_iat_validation = false;
        self.idt_aud_validation = false;
        self.id_token_trust_mode = "none".to_string();

        self.userinfo_iss_validation = false;
        self.userinfo_aud_validation = false;
        self.userinfo_sub_validation = false;
        self.userinfo_exp_validation = false;
    }
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
            user_authz: value.user_authz.into(),
            workload_authz: value.workload_authz.into(),
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
            jwt_sig_validation: value.jwt_sig_validation.into(),
            jwt_status_validation: value.jwt_status_validation.into(),
            jwt_signature_algorithms_supported: signature_algorithms,
            at_iss_validation: value.at_iss_validation.into(),
            at_jti_validation: value.at_jti_validation.into(),
            at_nbf_validation: value.at_nbf_validation.into(),
            at_exp_validation: value.at_exp_validation.into(),
            idt_iss_validation: value.idt_iss_validation.into(),
            idt_sub_validation: value.idt_sub_validation.into(),
            idt_exp_validation: value.idt_exp_validation.into(),
            idt_iat_validation: value.idt_iat_validation.into(),
            idt_aud_validation: value.idt_aud_validation.into(),
            userinfo_iss_validation: value.userinfo_iss_validation.into(),
            userinfo_sub_validation: value.userinfo_sub_validation.into(),
            userinfo_aud_validation: value.userinfo_aud_validation.into(),
            userinfo_exp_validation: value.userinfo_exp_validation.into(),
            id_token_trust_mode: TrustMode::from_str(value.id_token_trust_mode.as_str())
                .unwrap_or_default(),
            lock: value.lock.into(),
            lock_master_configuration_uri: value.lock_master_configuration_uri,
            dynamic_configuration: value.dynamic_configuration.into(),
            lock_ssa_jwt: value.lock_ssa_jwt,
            audit_log_interval: value.audit_log_interval,
            audit_health_interval: value.audit_health_interval,
            audit_health_telemetry_interval: value.audit_health_telemetry_interval,
            listen_sse: value.listen_sse.into(),
        };

        Self::from_raw_config(&raw_config).map_err(|err| {
            PyValueError::new_err(format!("could not parse config from raw config, {err}"))
        })
    }
}
