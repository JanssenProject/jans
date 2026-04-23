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
    #[name = c"enforcement"]
    Enforcement,
    #[name = c"instrumentation"]
    Instrumentation,
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

static MODE: GucSetting<CedarlingMode> =
    GucSetting::<CedarlingMode>::new(CedarlingMode::Enforcement);
static FAIL_MODE: GucSetting<CedarlingFailMode> =
    GucSetting::<CedarlingFailMode>::new(CedarlingFailMode::Closed);
static LOG_LEVEL: GucSetting<CedarlingLogLevelGuc> =
    GucSetting::<CedarlingLogLevelGuc>::new(CedarlingLogLevelGuc::Info);
static CACHE_TTL: GucSetting<i32> = GucSetting::<i32>::new(300);
static TOKENS: GucSetting<Option<CString>> = GucSetting::<Option<CString>>::new(None);
static BOOTSTRAP_CONFIG: GucSetting<Option<CString>> = GucSetting::<Option<CString>>::new(None);

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

/// Current `cedarling.cache_ttl` in seconds (0 disables result cache).
#[must_use]
pub fn cache_ttl_seconds() -> i32 {
    CACHE_TTL.get()
}

/// Current `cedarling.tokens` as a UTF-8 string, if set and valid UTF-8.
#[must_use]
pub fn tokens_utf8() -> Option<String> {
    TOKENS.get().and_then(|c| c.into_string().ok())
}

/// Path from `cedarling.bootstrap_config` (bootstrap YAML / JSON / TOML for Cedarling), if set and valid UTF-8.
#[must_use]
pub fn bootstrap_config_path_utf8() -> Option<String> {
    BOOTSTRAP_CONFIG.get().and_then(|c| c.into_string().ok())
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

unsafe fn register_gucs_inner() {
    GucRegistry::define_enum_guc(
        c"cedarling.mode",
        c"Cedarling operation mode.",
        c"enforcement: apply policy decisions. instrumentation: still evaluates policies; `cedarling_authorized` returns the same boolean as enforcement for safe RLS (extra logging may be added later).",
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
        c"One of debug, info, warn, error.",
        &LOG_LEVEL,
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

    GucRegistry::define_string_guc(
        c"cedarling.bootstrap_config",
        c"Filesystem path to Cedarling bootstrap configuration (YAML, JSON, or TOML).",
        c"Must be readable by the PostgreSQL server process. Used to construct the Cedarling engine on first authorization request.",
        &BOOTSTRAP_CONFIG,
        GucContext::Userset,
        GucFlags::empty(),
    );
}
