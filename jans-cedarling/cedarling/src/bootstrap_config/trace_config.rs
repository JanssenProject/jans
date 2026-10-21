// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::fmt;
use std::num::NonZeroUsize;
use std::path::PathBuf;

use url::Url;

use super::{BootstrapConfigLoadingError, BootstrapConfigRaw, PolicyStoreSource};
use crate::trace::{ProducerId, TraceSigningKid};

/// TRACE producer settings. Exists only when `CEDARLING_TRACE` is enabled, so a
/// configuration with TRACE on but without a key cannot be represented.
#[derive(Debug, Clone, PartialEq)]
pub struct TraceConfig {
    /// Producer id signed into every record (`CEDARLING_TRACE_PRODUCER_ID`, or
    /// `CEDARLING_APPLICATION_NAME` when unset).
    pub producer_id: ProducerId,
    /// Ed25519 private key that signs every record.
    pub signing_key: TraceSigningKey,
    /// Explicit `kid` (`CEDARLING_TRACE_SIGNING_KID`). `None` means the key
    /// thumbprint, which is what an admin registers in Lock by default.
    pub signing_kid: Option<TraceSigningKid>,
    /// Where TRACE bulk requests go (`CEDARLING_TRACE_ENDPOINT`).
    pub endpoint: TraceEndpoint,
    /// Maximum records per bulk request (`CEDARLING_TRACE_BULK_MAX_RECORDS`).
    pub bulk_max_records: NonZeroUsize,
    /// Maximum bytes per bulk request (`CEDARLING_TRACE_BULK_MAX_BYTES`).
    pub bulk_max_bytes: NonZeroUsize,
}

impl TraceConfig {
    /// Lock's default `maxBulkRecords` for the bulk endpoint.
    pub(super) const DEFAULT_BULK_MAX_RECORDS: NonZeroUsize = NonZeroUsize::new(100).unwrap();
    /// Lock's default `maxBulkRequestBytes` (4 MiB) for the bulk endpoint.
    pub(super) const DEFAULT_BULK_MAX_BYTES: NonZeroUsize = NonZeroUsize::new(4_194_304).unwrap();
    /// Lock's default `maxRequestBytes` for one record, plus the `[` and `]` of a
    /// bulk body: a smaller bulk limit could never send the largest record.
    const MIN_BULK_MAX_BYTES: usize = 262_144 + 2;
}

/// Where the TRACE signing key comes from: an unencrypted PKCS#8 PEM Ed25519
/// private key, either in a file or inline in the bootstrap configuration.
///
/// `Debug` never prints the inline key, so the bootstrap configuration can be
/// logged safely.
#[derive(Clone, PartialEq)]
pub enum TraceSigningKey {
    /// PEM file (`CEDARLING_TRACE_SIGNING_KEY`). Takes priority when both are set,
    /// because a file keeps the secret out of the configuration.
    File(PathBuf),
    /// PEM text (`CEDARLING_TRACE_SIGNING_KEY_PEM`), for hosts that inject the
    /// secret as a value rather than a file.
    Pem(String),
}

impl TraceSigningKey {
    /// Bootstrap property the key was configured with, for errors and logs.
    #[must_use]
    pub fn property(&self) -> &'static str {
        match self {
            Self::File(_) => "CEDARLING_TRACE_SIGNING_KEY",
            Self::Pem(_) => "CEDARLING_TRACE_SIGNING_KEY_PEM",
        }
    }
}

impl fmt::Debug for TraceSigningKey {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::File(path) => f.debug_tuple("File").field(path).finish(),
            Self::Pem(_) => f.write_str("Pem(<redacted>)"),
        }
    }
}

/// Source of the TRACE bulk endpoint.
#[derive(Debug, Clone, PartialEq)]
pub enum TraceEndpoint {
    /// `trace_bulk_endpoint` from the Lock Server well-known configuration.
    Discovered,
    /// Explicit URL that overrides discovery.
    Url(Url),
}

/// What TRACE needs from the rest of the configuration is missing.
#[derive(Debug, Clone, PartialEq, Eq, thiserror::Error)]
pub enum TraceSetupError {
    /// TRACE records are sent to Lock Server.
    #[error(
        "`CEDARLING_TRACE` is `enabled` but `CEDARLING_LOCK` is not: TRACE records are sent to \
         Lock Server"
    )]
    RequiresLock,
    /// `bundle_hash` is the hash of the `.cjar` bytes; other sources have no artifact.
    #[error(
        "`CEDARLING_TRACE` requires a `.cjar` policy store (local file, URL or Lock Server): \
         `bundle_hash` is computed from the archive bytes"
    )]
    RequiresArchivePolicyStore,
    /// The MVP signs on trusted hosts only, never in the browser.
    #[cfg(target_arch = "wasm32")]
    #[error("`CEDARLING_TRACE` is not supported on WASM")]
    NotSupportedOnWasm,
}

/// Checks what TRACE needs besides its own properties. Runs when the bootstrap
/// configuration is decoded and again when Cedarling starts, so a
/// `BootstrapConfig` built directly cannot skip it.
pub(crate) fn check_trace_setup(
    lock_enabled: bool,
    policy_store_source: &PolicyStoreSource,
) -> Result<(), TraceSetupError> {
    #[cfg(target_arch = "wasm32")]
    {
        let _ = (lock_enabled, policy_store_source);
        Err(TraceSetupError::NotSupportedOnWasm)
    }
    #[cfg(not(target_arch = "wasm32"))]
    {
        if !lock_enabled {
            return Err(TraceSetupError::RequiresLock);
        }
        if !policy_store_source.is_archive() {
            return Err(TraceSetupError::RequiresArchivePolicyStore);
        }
        Ok(())
    }
}

/// Builds the TRACE settings; `None` when TRACE is disabled. Invalid combinations
/// stop startup, because a producer that cannot emit valid records should not
/// look healthy.
pub(super) fn build_trace_config(
    raw: &BootstrapConfigRaw,
    policy_store_source: &PolicyStoreSource,
) -> Result<Option<TraceConfig>, BootstrapConfigLoadingError> {
    if !raw.trace.is_enabled() {
        return Ok(None);
    }
    check_trace_setup(raw.lock.is_enabled(), policy_store_source)?;
    build_enabled_trace_config(raw).map(Some)
}

fn build_enabled_trace_config(
    raw: &BootstrapConfigRaw,
) -> Result<TraceConfig, BootstrapConfigLoadingError> {
    let (property, producer_id) = match &raw.trace_producer_id {
        Some(id) => ("CEDARLING_TRACE_PRODUCER_ID", id.as_str()),
        None => ("CEDARLING_APPLICATION_NAME", raw.application_name.as_str()),
    };
    let producer_id = producer_id.parse().map_err(|source| {
        BootstrapConfigLoadingError::InvalidTraceProducerId { property, source }
    })?;

    let not_blank = |value: &Option<String>| value.clone().filter(|v| !v.trim().is_empty());
    let signing_key = match (
        not_blank(&raw.trace_signing_key),
        not_blank(&raw.trace_signing_key_pem),
    ) {
        (Some(path), _) => TraceSigningKey::File(path.into()),
        (None, Some(pem)) => TraceSigningKey::Pem(pem),
        (None, None) => return Err(BootstrapConfigLoadingError::MissingTraceSigningKey),
    };

    // A blank property means unset: the kid falls back to the key thumbprint.
    let signing_kid = not_blank(&raw.trace_signing_kid)
        .map(|kid| kid.parse())
        .transpose()
        .map_err(BootstrapConfigLoadingError::InvalidTraceSigningKid)?;

    // A blank property means unset, as for the kid: the endpoint is discovered.
    let endpoint = match not_blank(&raw.trace_endpoint).as_deref() {
        None | Some("discovered") => TraceEndpoint::Discovered,
        Some(url) => TraceEndpoint::Url(
            Url::parse(url).map_err(BootstrapConfigLoadingError::InvalidTraceEndpoint)?,
        ),
    };

    let non_zero = |value, property| {
        NonZeroUsize::new(value).ok_or(BootstrapConfigLoadingError::ZeroTraceBulkLimit(property))
    };

    Ok(TraceConfig {
        producer_id,
        signing_key,
        signing_kid,
        endpoint,
        bulk_max_records: non_zero(
            raw.trace_bulk_max_records,
            "CEDARLING_TRACE_BULK_MAX_RECORDS",
        )?,
        bulk_max_bytes: bulk_max_bytes(non_zero(
            raw.trace_bulk_max_bytes,
            "CEDARLING_TRACE_BULK_MAX_BYTES",
        )?)?,
    })
}

fn bulk_max_bytes(value: NonZeroUsize) -> Result<NonZeroUsize, BootstrapConfigLoadingError> {
    if value.get() < TraceConfig::MIN_BULK_MAX_BYTES {
        return Err(BootstrapConfigLoadingError::TraceBulkMaxBytesTooSmall {
            min: TraceConfig::MIN_BULK_MAX_BYTES,
        });
    }
    Ok(value)
}

#[cfg(test)]
mod tests {
    use serde_json::{Value, json};

    use super::*;
    use crate::bootstrap_config::{
        BootstrapConfig, BootstrapConfigLoadingError, BootstrapConfigRaw,
    };
    use crate::trace::ParseTraceSigningKidError;

    const KEY_PATH: &str = "/etc/cedarling/trace-key.pem";

    /// Smallest raw config with TRACE enabled that passes validation.
    fn trace_raw() -> serde_json::Map<String, Value> {
        let Value::Object(map) = json!({
            "CEDARLING_APPLICATION_NAME": "cedarling-fleet-1",
            "CEDARLING_POLICY_STORE_LOCAL_FN": "/does/not/exist/store.cjar",
            "CEDARLING_LOCK": "enabled",
            "CEDARLING_LOCK_SERVER_CONFIGURATION_URI":
                "https://lock.example.com/.well-known/lock-server-configuration",
            "CEDARLING_TRACE": "enabled",
            "CEDARLING_TRACE_SIGNING_KEY": KEY_PATH,
        }) else {
            unreachable!("json! object literal is an object")
        };
        map
    }

    fn decode(
        map: serde_json::Map<String, Value>,
    ) -> Result<BootstrapConfig, BootstrapConfigLoadingError> {
        let raw: BootstrapConfigRaw =
            serde_json::from_value(Value::Object(map)).expect("raw config should deserialize");
        BootstrapConfig::from_raw_config(&raw)
    }

    fn decode_with(
        changes: &[(&str, Option<Value>)],
    ) -> Result<BootstrapConfig, BootstrapConfigLoadingError> {
        let mut map = trace_raw();
        for (key, value) in changes {
            match value {
                Some(value) => map.insert((*key).to_string(), value.clone()),
                None => map.remove(*key),
            };
        }
        decode(map)
    }

    fn trace_config(changes: &[(&str, Option<Value>)]) -> TraceConfig {
        decode_with(changes)
            .expect("config should decode")
            .trace_config
            .expect("TRACE should be enabled")
    }

    #[test]
    fn trace_disabled_by_default() {
        let config = decode_with(&[("CEDARLING_TRACE", None)])
            .expect("config without CEDARLING_TRACE should decode");
        assert!(
            config.trace_config.is_none(),
            "TRACE must be off by default"
        );

        let config = decode_with(&[
            ("CEDARLING_TRACE", Some(json!("disabled"))),
            ("CEDARLING_LOCK", None),
            ("CEDARLING_TRACE_SIGNING_KEY", None),
        ])
        .expect("disabled TRACE must not validate its other properties");
        assert!(
            config.trace_config.is_none(),
            "disabled TRACE gives no config"
        );
    }

    #[test]
    fn trace_enabled_builds_config() {
        let config = trace_config(&[]);
        assert_eq!(
            config,
            TraceConfig {
                producer_id: "cedarling-fleet-1".parse().expect("valid producer id"),
                signing_key: TraceSigningKey::File(KEY_PATH.into()),
                signing_kid: None,
                endpoint: TraceEndpoint::Discovered,
                bulk_max_records: NonZeroUsize::new(100).expect("non-zero"),
                bulk_max_bytes: NonZeroUsize::new(4_194_304).expect("non-zero"),
            },
            "defaults: producer id from application name, kid from thumbprint, \
             discovered endpoint, Lock bulk limits"
        );
    }

    #[test]
    fn explicit_producer_id_wins_over_application_name() {
        let config = trace_config(&[
            ("CEDARLING_APPLICATION_NAME", Some(json!("My App"))),
            ("CEDARLING_TRACE_PRODUCER_ID", Some(json!("billing-pdp"))),
        ]);
        assert_eq!(
            config.producer_id.as_str(),
            "billing-pdp",
            "CEDARLING_TRACE_PRODUCER_ID should override the application name"
        );
    }

    #[test]
    fn trace_without_lock_is_error() {
        let err = decode_with(&[("CEDARLING_LOCK", None)])
            .expect_err("TRACE without Lock should be rejected");
        assert!(
            matches!(
                err,
                BootstrapConfigLoadingError::TraceSetup(TraceSetupError::RequiresLock)
            ),
            "expected TraceSetup(RequiresLock), got {err:?}"
        );
    }

    const KEY_PEM: &str = "-----BEGIN PRIVATE KEY-----\n\
                           MC4CAQAwBQYDK2VwBCIEINTuctv5E1hK1bbY8fdp+K06/nwoy/HU++CXqI9EdVhC\n\
                           -----END PRIVATE KEY-----\n";

    #[test]
    fn inline_pem_key_is_used_without_path() {
        let config = trace_config(&[
            ("CEDARLING_TRACE_SIGNING_KEY", None),
            ("CEDARLING_TRACE_SIGNING_KEY_PEM", Some(json!(KEY_PEM))),
        ]);
        assert_eq!(
            config.signing_key,
            TraceSigningKey::Pem(KEY_PEM.to_string()),
            "inline PEM should be used when no key path is set"
        );
        assert_eq!(
            config.signing_key.property(),
            "CEDARLING_TRACE_SIGNING_KEY_PEM",
            "inline key should name its property"
        );
    }

    #[test]
    fn key_path_wins_over_inline_pem() {
        let config = trace_config(&[("CEDARLING_TRACE_SIGNING_KEY_PEM", Some(json!(KEY_PEM)))]);
        assert_eq!(
            config.signing_key,
            TraceSigningKey::File(KEY_PATH.into()),
            "the key path should take priority over inline PEM"
        );
        assert_eq!(
            config.signing_key.property(),
            "CEDARLING_TRACE_SIGNING_KEY",
            "key file should name its property"
        );
    }

    #[test]
    fn debug_never_shows_inline_key() {
        let config = decode_with(&[
            ("CEDARLING_TRACE_SIGNING_KEY", None),
            ("CEDARLING_TRACE_SIGNING_KEY_PEM", Some(json!(KEY_PEM))),
        ])
        .expect("config with inline key should decode");
        let debug = format!("{config:?}");
        assert!(
            !debug.contains("MC4CAQAw") && !debug.contains("PRIVATE KEY"),
            "Debug of the bootstrap config must not leak the key, got: {debug}"
        );
    }

    #[test]
    fn trace_without_signing_key_is_error() {
        let err = decode_with(&[("CEDARLING_TRACE_SIGNING_KEY", None)])
            .expect_err("TRACE without a signing key should be rejected");
        assert!(
            matches!(err, BootstrapConfigLoadingError::MissingTraceSigningKey),
            "expected MissingTraceSigningKey, got {err:?}"
        );

        assert!(
            err.to_string().contains("CEDARLING_TRACE_SIGNING_KEY_PEM"),
            "the error should name both key properties, got: {err}"
        );

        let err = decode_with(&[
            ("CEDARLING_TRACE_SIGNING_KEY", Some(json!("   "))),
            ("CEDARLING_TRACE_SIGNING_KEY_PEM", Some(json!(" \n "))),
        ])
        .expect_err("TRACE with blank key properties should be rejected");
        assert!(
            matches!(err, BootstrapConfigLoadingError::MissingTraceSigningKey),
            "expected MissingTraceSigningKey for blank values, got {err:?}"
        );
    }

    #[test]
    fn app_name_with_space_is_invalid_producer_id() {
        let err = decode_with(&[("CEDARLING_APPLICATION_NAME", Some(json!("My App")))])
            .expect_err("`My App` is not a valid producer id");
        assert!(
            matches!(
                err,
                BootstrapConfigLoadingError::InvalidTraceProducerId {
                    property: "CEDARLING_APPLICATION_NAME",
                    ..
                }
            ),
            "expected InvalidTraceProducerId from CEDARLING_APPLICATION_NAME, got {err:?}"
        );
        assert!(
            err.to_string().contains("CEDARLING_TRACE_PRODUCER_ID"),
            "the error should tell how to fix it, got: {err}"
        );
    }

    #[test]
    fn invalid_explicit_producer_id_names_its_property() {
        let err = decode_with(&[("CEDARLING_TRACE_PRODUCER_ID", Some(json!("-fleet")))])
            .expect_err("`-fleet` is not a valid producer id");
        assert!(
            matches!(
                err,
                BootstrapConfigLoadingError::InvalidTraceProducerId {
                    property: "CEDARLING_TRACE_PRODUCER_ID",
                    ..
                }
            ),
            "expected InvalidTraceProducerId from CEDARLING_TRACE_PRODUCER_ID, got {err:?}"
        );
    }

    #[test]
    fn directory_store_with_trace_is_error() {
        let dir = std::env::temp_dir();
        let err = decode_with(&[(
            "CEDARLING_POLICY_STORE_LOCAL_FN",
            Some(json!(dir.to_str().expect("temp dir path is UTF-8"))),
        )])
        .expect_err("TRACE with a directory store should be rejected");
        assert!(
            matches!(
                err,
                BootstrapConfigLoadingError::TraceSetup(
                    TraceSetupError::RequiresArchivePolicyStore
                )
            ),
            "expected TraceSetup(RequiresArchivePolicyStore) for a directory, got {err:?}"
        );

        let err = decode_with(&[
            ("CEDARLING_POLICY_STORE_LOCAL_FN", None),
            (
                "CEDARLING_POLICY_STORE_LOCAL",
                Some(json!("cedar_version: v4.0.0\npolicy_stores: {}")),
            ),
        ])
        .expect_err("TRACE with an inline YAML store should be rejected");
        assert!(
            matches!(
                err,
                BootstrapConfigLoadingError::TraceSetup(
                    TraceSetupError::RequiresArchivePolicyStore
                )
            ),
            "expected TraceSetup(RequiresArchivePolicyStore) for YAML, got {err:?}"
        );
    }

    #[test]
    fn blank_kid_means_thumbprint() {
        let config = trace_config(&[("CEDARLING_TRACE_SIGNING_KID", Some(json!("  ")))]);
        assert_eq!(
            config.signing_kid, None,
            "a blank kid falls back to the thumbprint"
        );

        let config = trace_config(&[("CEDARLING_TRACE_SIGNING_KID", Some(json!("fleet-key-1")))]);
        assert_eq!(
            config.signing_kid.as_ref().map(TraceSigningKid::as_str),
            Some("fleet-key-1"),
            "an explicit kid is kept as is"
        );
    }

    #[test]
    fn kid_over_255_is_error() {
        let config = trace_config(&[("CEDARLING_TRACE_SIGNING_KID", Some(json!("k".repeat(255))))]);
        assert_eq!(
            config.signing_kid.map(|kid| kid.as_str().len()),
            Some(255),
            "a 255-character kid is accepted"
        );

        let err = decode_with(&[("CEDARLING_TRACE_SIGNING_KID", Some(json!("k".repeat(256))))])
            .expect_err("a 256-character kid should be rejected");
        assert!(
            matches!(
                err,
                BootstrapConfigLoadingError::InvalidTraceSigningKid(
                    ParseTraceSigningKidError::TooLong(256)
                )
            ),
            "expected InvalidTraceSigningKid(TooLong(256)), got {err:?}"
        );
    }

    #[test]
    fn explicit_endpoint_is_url() {
        let url = "https://lock.example.com/jans-lock/v1/trace/bulk";
        let config = trace_config(&[("CEDARLING_TRACE_ENDPOINT", Some(json!(url)))]);
        assert_eq!(
            config.endpoint,
            TraceEndpoint::Url(url.parse().expect("valid URL")),
            "an explicit endpoint overrides discovery"
        );

        let config = trace_config(&[("CEDARLING_TRACE_ENDPOINT", Some(json!("discovered")))]);
        assert_eq!(
            config.endpoint,
            TraceEndpoint::Discovered,
            "`discovered` keeps the endpoint from Lock well-known"
        );
    }

    #[test]
    fn bad_endpoint_is_error() {
        let err = decode_with(&[("CEDARLING_TRACE_ENDPOINT", Some(json!("not a url")))])
            .expect_err("an unparsable endpoint should be rejected");
        assert!(
            matches!(err, BootstrapConfigLoadingError::InvalidTraceEndpoint(_)),
            "expected InvalidTraceEndpoint, got {err:?}"
        );
    }

    #[test]
    fn zero_bulk_limit_is_error() {
        for property in [
            "CEDARLING_TRACE_BULK_MAX_RECORDS",
            "CEDARLING_TRACE_BULK_MAX_BYTES",
        ] {
            let err = decode_with(&[(property, Some(json!(0)))])
                .expect_err("a zero bulk limit should be rejected");
            assert!(
                matches!(err, BootstrapConfigLoadingError::ZeroTraceBulkLimit(p) if p == property),
                "expected ZeroTraceBulkLimit({property}), got {err:?}"
            );
        }
    }

    #[test]
    fn blank_endpoint_is_discovered() {
        let config = trace_config(&[("CEDARLING_TRACE_ENDPOINT", Some(json!("   ")))]);
        assert_eq!(
            config.endpoint,
            TraceEndpoint::Discovered,
            "a blank endpoint means unset, like a blank signing kid"
        );
    }

    #[test]
    fn bulk_max_bytes_below_one_record_is_rejected() {
        let err = decode_with(&[("CEDARLING_TRACE_BULK_MAX_BYTES", Some(json!(262_145)))])
            .expect_err("a bulk limit that cannot hold one record should be rejected");
        assert!(
            matches!(
                err,
                BootstrapConfigLoadingError::TraceBulkMaxBytesTooSmall { min: 262_146 }
            ),
            "expected TraceBulkMaxBytesTooSmall {{ min: 262146 }}, got {err:?}"
        );
    }

    #[test]
    fn bulk_max_bytes_one_record_is_ok() {
        let config = trace_config(&[("CEDARLING_TRACE_BULK_MAX_BYTES", Some(json!(262_146)))]);
        assert_eq!(
            config.bulk_max_bytes.get(),
            262_146,
            "a bulk limit that holds exactly one record should be accepted"
        );
    }
}
