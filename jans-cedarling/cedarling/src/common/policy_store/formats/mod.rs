// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Versioned on-disk formats of the directory / `.cjar` policy store.
//!
//! Each `vN` module owns the `Deserialize` structs for its version and must not
//! import another version module. Older versions reach the current one through
//! the `migration` chain, and only the current version converts into runtime
//! types. Runtime types never reference `formats::vN`.

pub(crate) mod datetime;
pub(crate) mod file_id;
pub(crate) mod v0;
pub(crate) mod v1;

use std::fmt;

use super::PolicyStoreWithID;
use super::errors::ValidationError;
use super::loader::{EntityFile, LoadedPolicyStore, PolicyFile};
use super::manager::ConversionError;
use super::migration;
use super::schema_parser::ParsedSchema;

/// Spec version that runtime types are built from.
pub(crate) const CURRENT_FORMAT_VERSION: u32 = v1::SPEC_VERSION;
/// Oldest spec version this Cedarling still reads; also assumed when the field is missing.
/// v0 is the unversioned baseline that predates `policy_store_spec_version`.
pub(crate) const MIN_SUPPORTED_FORMAT_VERSION: u32 = v0::SPEC_VERSION;

/// Parts of a store whose format does not depend on the spec version.
#[derive(Debug)]
pub(crate) struct StoreContent {
    pub(crate) schema: Option<ParsedSchema>,
    pub(crate) schema_source_exists: bool,
    pub(crate) policies: Vec<PolicyFile>,
    pub(crate) templates: Vec<PolicyFile>,
    pub(crate) entities: Vec<EntityFile>,
}

/// Non-fatal findings from parsing a store; logged by the caller.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub(crate) enum PolicyStoreWarning {
    /// `metadata.json` has no `policy_store_spec_version`.
    MissingSpecVersion { assumed: u32, current: u32 },
    /// The store declares a supported but older spec version.
    OutdatedSpecVersion { found: u32, current: u32 },
}

impl fmt::Display for PolicyStoreWarning {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::MissingSpecVersion { assumed, current } => write!(
                f,
                "policy store format is outdated: metadata.json has no \
                 policy_store_spec_version, so it was read as spec version {assumed}; \
                 please update the policy store to spec version {current}"
            ),
            Self::OutdatedSpecVersion { found, current } => write!(
                f,
                "policy store format is outdated: policy_store_spec_version is {found}; \
                 please update the policy store to spec version {current}"
            ),
        }
    }
}

/// Error from [`parse_policy_store`], split the same way as the load errors
/// so metadata problems stay distinguishable from content problems.
#[derive(Debug, thiserror::Error)]
pub(crate) enum ParseStoreError {
    #[error(transparent)]
    Validation(#[from] ValidationError),
    #[error(transparent)]
    Conversion(#[from] ConversionError),
}

/// Reads `policy_store_spec_version`; `null` counts as missing.
///
/// Only requires `metadata.json` to be a JSON object, so it works for every version.
fn probe_spec_version(metadata_json: &str) -> Result<Option<u32>, ValidationError> {
    let root: serde_json::Map<String, serde_json::Value> = serde_json::from_str(metadata_json)
        .map_err(|e| ValidationError::MetadataJsonParseFailed {
            file: "metadata.json".to_string(),
            source: e,
        })?;
    match root.get("policy_store_spec_version") {
        None | Some(serde_json::Value::Null) => Ok(None),
        Some(value) => value
            .as_u64()
            .and_then(|v| u32::try_from(v).ok())
            .map(Some)
            .ok_or_else(|| ValidationError::InvalidSpecVersion {
                value: value.to_string(),
            }),
    }
}

/// Supported spec versions; the dispatch table below is the only place
/// that maps numbers to version modules.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum FormatVersion {
    V0,
    V1,
}

/// The format version a store resolved to, decided once by the loader.
///
/// Fields stay private so [`select_version`] is the only way to build one.
#[derive(Debug, Clone, Copy)]
pub(crate) struct SelectedVersion {
    /// What `metadata.json` declared; `None` when the field is absent.
    declared: Option<u32>,
    /// The version the store is read as.
    spec_version: u32,
    format: FormatVersion,
}

/// Picks the parser for `metadata.json`, failing closed outside
/// `MIN_SUPPORTED_FORMAT_VERSION..=CURRENT_FORMAT_VERSION`.
///
/// Called by the loader so an unsupported store fails with a version error
/// rather than a schema or policy error, and so nothing re-decides later.
pub(crate) fn select_version(metadata_json: &str) -> Result<SelectedVersion, ValidationError> {
    let declared = probe_spec_version(metadata_json)?;
    let spec_version = declared.unwrap_or(MIN_SUPPORTED_FORMAT_VERSION);

    let version = match spec_version {
        v0::SPEC_VERSION => FormatVersion::V0,
        v1::SPEC_VERSION => FormatVersion::V1,
        found if found > CURRENT_FORMAT_VERSION => {
            return Err(ValidationError::SpecVersionTooNew {
                found,
                max: CURRENT_FORMAT_VERSION,
            });
        },
        found => {
            return Err(ValidationError::SpecVersionTooOld {
                found,
                min: MIN_SUPPORTED_FORMAT_VERSION,
            });
        },
    };
    Ok(SelectedVersion {
        declared,
        spec_version,
        format: version,
    })
}

fn warnings_for(declared: Option<u32>) -> Vec<PolicyStoreWarning> {
    match declared {
        None => vec![PolicyStoreWarning::MissingSpecVersion {
            assumed: MIN_SUPPORTED_FORMAT_VERSION,
            current: CURRENT_FORMAT_VERSION,
        }],
        Some(found) if found < CURRENT_FORMAT_VERSION => {
            vec![PolicyStoreWarning::OutdatedSpecVersion {
                found,
                current: CURRENT_FORMAT_VERSION,
            }]
        },
        Some(_) => Vec::new(),
    }
}

/// Parses a loaded directory / `.cjar` store into its runtime form.
///
/// Older versions are migrated step by step; only the current version
/// converts into runtime types.
pub(crate) fn parse_policy_store(
    loaded: LoadedPolicyStore,
    strict_schema_validation: bool,
) -> Result<PolicyStoreWithID, ParseStoreError> {
    let SelectedVersion {
        declared,
        spec_version,
        format,
    } = loaded.spec_version;

    let doc = match format {
        FormatVersion::V0 => migration::v0_to_v1::migrate(v0::parse(loaded)?),
        FormatVersion::V1 => v1::parse(loaded)?,
    };

    let (store, metadata) = v1::into_runtime(doc, strict_schema_validation)?;

    Ok(PolicyStoreWithID {
        id: metadata.policy_store.id.clone(),
        store,
        metadata: Some(metadata),
        spec_version: Some(spec_version),
        warnings: warnings_for(declared),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    const V0_METADATA: &str = r#"{
        "cedar_version": "4.4.0",
        "policy_store": { "id": "abc123def456", "name": "Test Store", "version": "1.0.0" }
    }"#;

    fn metadata_with_version(version: &str) -> String {
        format!(
            r#"{{
                "policy_store_spec_version": {version},
                "cedar_version": "4.4.0",
                "policy_store": {{ "id": "abc123def456", "name": "Test Store", "version": "1.0.0" }}
            }}"#
        )
    }

    /// A loaded store whose version the loader has already resolved.
    fn loaded(metadata_json: &str) -> LoadedPolicyStore {
        LoadedPolicyStore {
            spec_version: select_version(metadata_json)
                .expect("test metadata should declare a supported version"),
            metadata_json: metadata_json.to_string(),
            schema: None,
            schema_source_exists: false,
            policies: vec![PolicyFile {
                name: "allow.cedar".to_string(),
                content: "permit(principal, action, resource);".to_string(),
            }],
            templates: vec![],
            entities: vec![],
            trusted_issuers: vec![super::super::loader::IssuerFile {
                name: "jans.json".to_string(),
                content: r#"{
                    "name": "Jans",
                    "openid_configuration_endpoint": "https://jans.test/.well-known/openid-configuration",
                    "token_metadata": {
                        "access_token": { "entity_type_name": "Jans::Access_token" }
                    }
                }"#
                .to_string(),
            }],
            custom_issuers: vec![super::super::loader::CustomIssuerFile {
                name: "acme.json".to_string(),
                content: r#"{ "tokens_mappings": { "Acme::Custom": { "required": true } } }"#
                    .to_string(),
            }],
        }
    }

    #[test]
    fn probe_reads_integer_versions() {
        assert_eq!(
            probe_spec_version(&metadata_with_version("1")).expect("1 is a valid version"),
            Some(1),
            "an integer version should be returned as-is"
        );
        assert_eq!(
            probe_spec_version(V0_METADATA).expect("missing version is valid"),
            None,
            "a missing field should read as None"
        );
        assert_eq!(
            probe_spec_version(&metadata_with_version("null")).expect("null is valid"),
            None,
            "null should be treated the same as a missing field"
        );
    }

    #[test]
    fn probe_rejects_malformed_metadata() {
        for json in ["{ not json", "[]"] {
            let err = probe_spec_version(json).expect_err("metadata.json must be a JSON object");
            assert!(
                matches!(err, ValidationError::MetadataJsonParseFailed { .. }),
                "expected MetadataJsonParseFailed for {json}, got: {err:?}"
            );
        }
    }

    /// The loader resolves the version, so an unsupported store is rejected
    /// there rather than after the schema and policies have been read.
    #[test]
    fn select_version_fails_closed_before_parsing() {
        select_version(V0_METADATA).expect("a store without a version is supported");
        let err = select_version(&metadata_with_version("99"))
            .expect_err("an unsupported version must be rejected when it is resolved");
        assert!(
            matches!(err, ValidationError::SpecVersionTooNew { found: 99, .. }),
            "expected SpecVersionTooNew, got: {err:?}"
        );
    }

    #[test]
    fn probe_rejects_non_integer_versions() {
        for value in [r#""2""#, "-1", "2.5", "4294967296", "[2]"] {
            let err = probe_spec_version(&metadata_with_version(value))
                .expect_err("a non-integer version must be rejected");
            assert!(
                matches!(err, ValidationError::InvalidSpecVersion { .. }),
                "expected InvalidSpecVersion for {value}, got: {err:?}"
            );
        }
    }

    #[test]
    fn missing_version_parses_as_oldest_with_warning() {
        let parsed = parse_policy_store(loaded(V0_METADATA), false)
            .expect("a store without a spec version should parse");
        assert_eq!(
            parsed.spec_version,
            Some(MIN_SUPPORTED_FORMAT_VERSION),
            "a missing version should be read as the oldest supported one"
        );
        assert_eq!(
            parsed.warnings,
            vec![PolicyStoreWarning::MissingSpecVersion {
                assumed: MIN_SUPPORTED_FORMAT_VERSION,
                current: CURRENT_FORMAT_VERSION,
            }],
            "a missing version should produce exactly one MissingSpecVersion warning"
        );
    }

    /// An explicit `0` names the unversioned baseline, so it is accepted and
    /// warns just like a missing field.
    #[test]
    fn explicit_baseline_version_parses_with_warning() {
        let parsed = parse_policy_store(loaded(&metadata_with_version("0")), false)
            .expect("an explicit v0 store should parse");
        assert_eq!(
            parsed.spec_version,
            Some(0),
            "declared version should be kept"
        );
        assert_eq!(
            parsed.warnings,
            vec![PolicyStoreWarning::OutdatedSpecVersion {
                found: 0,
                current: CURRENT_FORMAT_VERSION,
            }],
            "an older version should produce exactly one OutdatedSpecVersion warning"
        );
    }

    #[test]
    fn current_version_parses_without_warnings() {
        let parsed = parse_policy_store(
            loaded(&metadata_with_version(&CURRENT_FORMAT_VERSION.to_string())),
            false,
        )
        .expect("a current-version store should parse");
        assert_eq!(
            parsed.spec_version,
            Some(CURRENT_FORMAT_VERSION),
            "declared version should be kept"
        );
        assert!(
            parsed.warnings.is_empty(),
            "a current-version store should not warn, got: {:?}",
            parsed.warnings
        );
    }

    #[test]
    fn every_version_produces_the_same_runtime_store() {
        let from_missing = parse_policy_store(loaded(V0_METADATA), false)
            .expect("store without a version should parse");
        let from_v0 = parse_policy_store(loaded(&metadata_with_version("0")), false)
            .expect("v0 store should parse");
        let from_v1 = parse_policy_store(loaded(&metadata_with_version("1")), false)
            .expect("v1 store should parse");

        for (label, parsed) in [("v0", &from_v0), ("v1", &from_v1)] {
            assert_eq!(
                parsed.store, from_missing.store,
                "{label} should build the same runtime store as an unversioned one"
            );
            assert_eq!(
                parsed.metadata, from_missing.metadata,
                "{label} should build the same runtime metadata as an unversioned one"
            );
        }
        assert_eq!(
            from_v1.id, "abc123def456",
            "id should come from policy_store.id"
        );
        let issuers = from_v1
            .trusted_issuers
            .as_ref()
            .expect("trusted issuers should be present");
        assert!(
            issuers.contains_key("jans"),
            "issuer id should be derived from the file name"
        );
        assert!(
            from_v1.custom_issuers["acme"].tokens_mappings["Acme::Custom"].required,
            "custom issuer settings should reach the runtime store"
        );
    }

    #[test]
    fn newer_version_fails_closed() {
        let err = select_version(&metadata_with_version(
            &(CURRENT_FORMAT_VERSION + 1).to_string(),
        ))
        .expect_err("a version newer than CURRENT must be rejected");
        assert!(
            matches!(
                err,
                ValidationError::SpecVersionTooNew { found, max }
                    if found == CURRENT_FORMAT_VERSION + 1 && max == CURRENT_FORMAT_VERSION
            ),
            "expected SpecVersionTooNew, got: {err:?}"
        );
    }

    /// `SpecVersionTooOld` cannot be reached while `MIN_SUPPORTED` is 0, since no
    /// `u32` is below it. Kept for the first bump that drops baseline support.
    #[test]
    fn too_old_version_reports_the_supported_floor() {
        assert_eq!(MIN_SUPPORTED_FORMAT_VERSION, 0, "v0 is the baseline format");
        let err = ValidationError::SpecVersionTooOld { found: 0, min: 1 };
        assert_eq!(
            err.to_string(),
            "Unsupported policy store: policy_store_spec_version 0 is no longer supported; \
             the oldest supported version is 1",
            "the error should name both the rejected version and the floor"
        );
    }

    #[test]
    fn malformed_metadata_reports_parse_error() {
        let err =
            select_version("{ not json").expect_err("malformed metadata.json must be rejected");
        assert!(
            matches!(err, ValidationError::MetadataJsonParseFailed { .. }),
            "expected MetadataJsonParseFailed, got: {err:?}"
        );
    }

    #[test]
    fn warning_text_is_stable() {
        assert_eq!(
            PolicyStoreWarning::MissingSpecVersion {
                assumed: 0,
                current: 1
            }
            .to_string(),
            "policy store format is outdated: metadata.json has no policy_store_spec_version, \
             so it was read as spec version 0; please update the policy store to spec version 1",
            "the documented warning text must not drift"
        );
        assert_eq!(
            PolicyStoreWarning::OutdatedSpecVersion {
                found: 0,
                current: 1
            }
            .to_string(),
            "policy store format is outdated: policy_store_spec_version is 0; \
             please update the policy store to spec version 1",
            "the documented warning text must not drift"
        );
    }

    /// Version modules must not import each other, and runtime modules must
    /// not import any version module.
    ///
    /// A substring check over sources: an alias such as `use ...::v1 as cur;`
    /// gets past it, and new files must be added to the lists below by hand.
    #[test]
    fn version_modules_are_isolated() {
        let v0_sources = [
            ("v0/mod.rs", include_str!("v0/mod.rs")),
            ("v0/metadata.rs", include_str!("v0/metadata.rs")),
            ("v0/trusted_issuer.rs", include_str!("v0/trusted_issuer.rs")),
            ("v0/custom_issuer.rs", include_str!("v0/custom_issuer.rs")),
        ];
        let v1_sources = [
            ("v1/mod.rs", include_str!("v1/mod.rs")),
            ("v1/metadata.rs", include_str!("v1/metadata.rs")),
            ("v1/trusted_issuer.rs", include_str!("v1/trusted_issuer.rs")),
            ("v1/custom_issuer.rs", include_str!("v1/custom_issuer.rs")),
        ];
        let runtime_sources = [
            ("policy_store.rs", include_str!("../../policy_store.rs")),
            ("metadata.rs", include_str!("../metadata.rs")),
            (
                "token_entity_metadata.rs",
                include_str!("../token_entity_metadata.rs"),
            ),
            (
                "custom_issuer_metadata.rs",
                include_str!("../custom_issuer_metadata.rs"),
            ),
            ("manager.rs", include_str!("../manager.rs")),
        ];

        let check = |sources: &[(&str, &str)], forbidden: &[&str]| {
            for (file, source) in sources {
                for pattern in forbidden {
                    assert!(
                        !source.contains(pattern),
                        "{file} must not reference `{pattern}`"
                    );
                }
            }
        };
        check(&v0_sources, &["v1::", "migration::"]);
        check(&v1_sources, &["v0::", "migration::"]);
        check(&runtime_sources, &["formats::v", "migration::"]);
    }
}
