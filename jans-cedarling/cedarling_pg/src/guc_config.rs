// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Grand Unified Configuration (`GUC`) parameters for `cedarling_pg`.

use std::ffi::{CStr, CString};

use pgrx::guc::{GucContext, GucFlags, GucRegistry, GucSetting, PostgresGucEnum};
use pgrx::pg_sys;
use pgrx::prelude::*;

use crate::validate;

/// How authorization results are applied.
#[derive(Clone, Copy, Debug, Eq, PartialEq, PostgresGucEnum)]
pub enum CedarlingMode {
    /// Apply policy decisions (`false` → deny row).
    #[name = c"enforcement"]
    Enforcement,
    /// Evaluate policies and emit logs but still return the underlying decision to callers.
    /// Useful for observability without changing behavior.
    #[name = c"instrumentation"]
    Instrumentation,
    /// Evaluate policies for logging only; the authorize function always returns `true` so
    /// RLS lets every row through. Traces still record the would-be decision.
    #[name = c"shadow"]
    Shadow,
}

/// Fail-open vs fail-closed behavior on engine errors.
#[derive(Clone, Copy, Debug, Eq, PartialEq, PostgresGucEnum)]
pub enum CedarlingFailMode {
    #[name = c"closed"]
    Closed,
    #[name = c"open"]
    Open,
}

/// Log verbosity for extension-side messages.
#[derive(Clone, Copy, Debug, Eq, PartialEq, PostgresGucEnum)]
pub enum CedarlingLogLevelGuc {
    #[name = c"debug"]
    Debug,
    #[name = c"info"]
    Info,
    #[name = c"warn"]
    Warn,
    #[name = c"error"]
    Error,
}

/// Access strategy for rows that fail authorization.
#[derive(Clone, Copy, Debug, Eq, PartialEq, PostgresGucEnum)]
pub enum CedarlingStrategy {
    /// Exclude unauthorized rows from the query result (standard RLS semantics).
    #[name = c"filter"]
    Filter,
    /// Return the row but mask one or more columns according to `cedarling.mask_rules`.
    /// The masking subsystem is delivered in Phase 3.
    #[name = c"mask"]
    Mask,
}

static MODE: GucSetting<CedarlingMode> =
    GucSetting::<CedarlingMode>::new(CedarlingMode::Enforcement);
static FAIL_MODE: GucSetting<CedarlingFailMode> =
    GucSetting::<CedarlingFailMode>::new(CedarlingFailMode::Closed);
static LOG_LEVEL: GucSetting<CedarlingLogLevelGuc> =
    GucSetting::<CedarlingLogLevelGuc>::new(CedarlingLogLevelGuc::Info);
static STRATEGY: GucSetting<CedarlingStrategy> =
    GucSetting::<CedarlingStrategy>::new(CedarlingStrategy::Filter);
static CACHE_TTL: GucSetting<i32> = GucSetting::<i32>::new(300);
static CACHE_SIZE: GucSetting<i32> = GucSetting::<i32>::new(8192);
static AUDIT_FAIL_OPEN: GucSetting<bool> = GucSetting::<bool>::new(true);
static TOKENS: GucSetting<Option<CString>> = GucSetting::<Option<CString>>::new(None);
static CONTEXT: GucSetting<Option<CString>> = GucSetting::<Option<CString>>::new(None);
static BOOTSTRAP_CONFIG: GucSetting<Option<CString>> = GucSetting::<Option<CString>>::new(None);
static POLICY_VERSION: GucSetting<Option<CString>> = GucSetting::<Option<CString>>::new(None);
static TRACE_BUFFER_SIZE: GucSetting<i32> = GucSetting::<i32>::new(1024);
static POLICY_HISTORY_SIZE: GucSetting<i32> = GucSetting::<i32>::new(16);

/// Current `cedarling.mode`.
#[must_use]
pub fn mode() -> CedarlingMode {
    MODE.get()
}

/// Current `cedarling.fail_mode`.
#[must_use]
pub fn fail_mode() -> CedarlingFailMode {
    FAIL_MODE.get()
}

/// Current `cedarling.log_level`.
#[must_use]
pub fn log_level() -> CedarlingLogLevelGuc {
    LOG_LEVEL.get()
}

/// Current `cedarling.strategy`.
#[must_use]
pub fn strategy() -> CedarlingStrategy {
    STRATEGY.get()
}

/// Current `cedarling.cache_ttl` in seconds (0 disables result cache).
#[must_use]
pub fn cache_ttl_seconds() -> i32 {
    CACHE_TTL.get()
}

/// Current `cedarling.cache_size` — maximum in-memory decision entries per backend process.
#[must_use]
pub fn cache_size() -> i32 {
    CACHE_SIZE.get()
}

/// Current `cedarling.audit_fail_open`: when fail-open, whether to emit a structured audit log.
#[must_use]
pub fn audit_fail_open() -> bool {
    AUDIT_FAIL_OPEN.get()
}

/// Current `cedarling.tokens` as a UTF-8 string, if set and valid UTF-8.
#[must_use]
pub fn tokens_utf8() -> Option<String> {
    TOKENS.get().and_then(|c| c.into_string().ok())
}

/// Current `cedarling.context` as a UTF-8 string, if set and valid UTF-8.
///
/// Empty / whitespace-only values are treated as unset.
#[must_use]
pub fn context_utf8() -> Option<String> {
    CONTEXT.get().and_then(|c| {
        let s = c.into_string().ok()?;
        let t = s.trim();
        if t.is_empty() {
            None
        } else {
            Some(t.to_string())
        }
    })
}

/// Path from `cedarling.bootstrap_config` (bootstrap YAML / JSON / TOML for Cedarling), if set and valid UTF-8.
#[must_use]
pub fn bootstrap_config_path_utf8() -> Option<String> {
    BOOTSTRAP_CONFIG.get().and_then(|c| c.into_string().ok())
}

/// Current `cedarling.policy_version`, if explicitly pinned. Empty / unset means "latest".
#[must_use]
pub fn policy_version_utf8() -> Option<String> {
    POLICY_VERSION.get().and_then(|c| {
        let s = c.into_string().ok()?;
        let t = s.trim();
        if t.is_empty() {
            None
        } else {
            Some(t.to_string())
        }
    })
}

/// Current `cedarling.trace_buffer_size` (per-backend trace ring capacity).
#[must_use]
pub fn trace_buffer_size() -> i32 {
    TRACE_BUFFER_SIZE.get()
}

/// Current `cedarling.policy_history_size` (rows retained in `cedarling.policy_history`).
#[must_use]
pub fn policy_history_size() -> i32 {
    POLICY_HISTORY_SIZE.get()
}

/// Register all GUCs with `PostgreSQL`. Call once from `_PG_init`.
pub fn register_gucs() {
    unsafe {
        register_gucs_inner();
    }
}

#[pg_guard]
unsafe extern "C-unwind" fn cedarling_tokens_check_hook(
    newval: *mut *mut std::ffi::c_char,
    _extra: *mut *mut std::ffi::c_void,
    _source: pg_sys::GucSource::Type,
) -> bool {
    if newval.is_null() || (*newval).is_null() {
        return true;
    }
    let Ok(s) = CStr::from_ptr(*newval).to_str() else {
        return false;
    };
    validate::tokens_json_is_valid(s)
}

#[pg_guard]
unsafe extern "C-unwind" fn cedarling_context_check_hook(
    newval: *mut *mut std::ffi::c_char,
    _extra: *mut *mut std::ffi::c_void,
    _source: pg_sys::GucSource::Type,
) -> bool {
    if newval.is_null() || (*newval).is_null() {
        return true;
    }
    let Ok(s) = CStr::from_ptr(*newval).to_str() else {
        return false;
    };
    validate::context_json_is_valid_object(s)
}

unsafe fn register_gucs_inner() {
    GucRegistry::define_enum_guc(
        c"cedarling.mode",
        c"Cedarling operation mode.",
        c"enforcement: apply policy decisions. instrumentation: evaluate and log, still return the decision. shadow: evaluate and trace but always return true so RLS stays permissive (useful for rollout dry-runs).",
        &MODE,
        GucContext::Userset,
        GucFlags::empty(),
    );

    GucRegistry::define_enum_guc(
        c"cedarling.fail_mode",
        c"Fail-closed vs fail-open on authorization errors.",
        c"closed: deny on errors (default). open: allow reads on errors (use with caution).",
        &FAIL_MODE,
        GucContext::Userset,
        GucFlags::empty(),
    );

    GucRegistry::define_enum_guc(
        c"cedarling.log_level",
        c"Minimum log level for extension diagnostics.",
        c"One of debug, info, warn, error. Controls server log output from cedarling_authorized / cedarling_authorize_unsigned (debug is most verbose). Messages never include raw JWTs.",
        &LOG_LEVEL,
        GucContext::Userset,
        GucFlags::empty(),
    );

    GucRegistry::define_enum_guc(
        c"cedarling.strategy",
        c"Access strategy for rows that fail authorization.",
        c"filter: exclude the row (standard RLS). mask: return the row with masked columns (requires configured cedarling.mask_rules).",
        &STRATEGY,
        GucContext::Userset,
        GucFlags::empty(),
    );

    GucRegistry::define_int_guc(
        c"cedarling.cache_ttl",
        c"Authorization result cache TTL in seconds.",
        c"0 disables caching. Successful Cedarling evaluations are keyed by bootstrap path fingerprint, tokens/resource/action (JWT path), or principal/resource/action/context (unsigned path). Maximum 604800 (7 days).",
        &CACHE_TTL,
        0,
        604_800,
        GucContext::Userset,
        GucFlags::empty(),
    );

    GucRegistry::define_int_guc(
        c"cedarling.cache_size",
        c"Maximum entries in the per-backend authorization result cache.",
        c"New connections pick up the updated limit; an existing backend session keeps its allocated cache size until disconnect. Range 0..=1048576. Set to 0 to disable caching regardless of cedarling.cache_ttl.",
        &CACHE_SIZE,
        0,
        1_048_576,
        GucContext::Userset,
        GucFlags::empty(),
    );

    GucRegistry::define_bool_guc(
        c"cedarling.audit_fail_open",
        c"Emit a structured audit log entry whenever fail-open allows a request that otherwise would have denied.",
        c"Defaults to true. Disable only if a downstream log collector is already recording decisions.",
        &AUDIT_FAIL_OPEN,
        GucContext::Userset,
        GucFlags::empty(),
    );

    GucRegistry::define_string_guc_with_hooks(
        c"cedarling.tokens",
        c"JWT token bundle JSON for Cedarling.",
        c"JSON object (e.g. access_token, id_token). Empty clears. Invalid JSON is rejected at SET time.",
        &TOKENS,
        GucContext::Userset,
        GucFlags::empty(),
        Some(cedarling_tokens_check_hook),
        None,
        None,
    );

    GucRegistry::define_string_guc_with_hooks(
        c"cedarling.context",
        c"Optional Cedar request context JSON object.",
        c"JSON object merged into Cedar authorization context for cedarling_authorized and cedarling_authorized_row. Empty clears the value.",
        &CONTEXT,
        GucContext::Userset,
        GucFlags::empty(),
        Some(cedarling_context_check_hook),
        None,
        None,
    );

    GucRegistry::define_string_guc(
        c"cedarling.bootstrap_config",
        c"Filesystem path to Cedarling bootstrap configuration (YAML, JSON, or TOML).",
        c"Must be readable by the PostgreSQL server process. Used to construct the Cedarling engine on first authorization request.",
        &BOOTSTRAP_CONFIG,
        GucContext::Userset,
        GucFlags::empty(),
    );

    GucRegistry::define_string_guc(
        c"cedarling.policy_version",
        c"Policy version to pin this session to.",
        c"Empty / unset means the most recently loaded version. Values are matched against versions registered via cedarling_use_policy().",
        &POLICY_VERSION,
        GucContext::Userset,
        GucFlags::empty(),
    );

    GucRegistry::define_int_guc(
        c"cedarling.trace_buffer_size",
        c"Maximum traces retained in the per-backend authorization ring buffer.",
        c"0 disables in-memory trace retention. Range 0..=65536.",
        &TRACE_BUFFER_SIZE,
        0,
        65_536,
        GucContext::Userset,
        GucFlags::empty(),
    );

    GucRegistry::define_int_guc(
        c"cedarling.policy_history_size",
        c"Maximum rows retained in cedarling.policy_history.",
        c"0 keeps no history rows; each policy operation prunes to this cap. Range 0..=100000.",
        &POLICY_HISTORY_SIZE,
        0,
        100_000,
        GucContext::Userset,
        GucFlags::empty(),
    );
}
