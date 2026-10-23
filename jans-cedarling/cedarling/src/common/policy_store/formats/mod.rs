// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Versioned on-disk formats of the directory / `.cjar` policy store.
//!
//! Each `vN` module declares its layout as a [`Layout`] constant and owns the
//! `Deserialize` structs for its files, and must not import another version
//! module. A released version's layout is frozen: a store laid out differently
//! is a new spec version, so changing one version never breaks another. Reading
//! that layout does not vary between versions, so [`read_files`] is shared.
//! Only [`METADATA_FILE`] is fixed for every version, because it is read before
//! the version is known.
//!
//! Older versions reach the current one through the `migration` chain, and
//! only the current version converts into runtime types. Runtime types never
//! reference `formats::vN`.

pub(crate) mod datetime;
pub(crate) mod file_id;
pub(crate) mod v0;
pub(crate) mod v1;
pub(crate) mod v2;

/// The current spec version: the only one that converts into runtime types,
/// and the one every older version migrates to. Bumping the version points
/// this at the new module.
pub(super) use v2 as current;

use std::borrow::Cow;
use std::convert::Infallible;
use std::fmt;

use jans_trace_core::Digest;
use serde::de::{IgnoredAny, MapAccess, Visitor};
use serde::{Deserialize, Deserializer};

use super::PolicyStoreWithID;
use super::errors::{PolicyStoreError, ValidationError};
use super::loader::{StoreFile, StoreFs};
use super::manager::ConversionError;
use super::migration::v1_to_v2::V1ToV2Warning;
use super::migration::{IntoCurrent, MigrationError};
use super::schema_parser::ParsedSchema;

/// Where every spec version keeps its metadata.
///
/// Read before the version is known, so unlike the rest of the layout it can
/// never move.
pub(super) const METADATA_FILE: &str = "metadata.json";

/// Spec version that runtime types are built from.
pub(crate) const CURRENT_FORMAT_VERSION: u32 = current::SPEC_VERSION;
/// Oldest spec version this Cedarling still reads; also assumed when the field is missing.
/// v0 is the unversioned baseline that predates `policy_store_spec_version`.
pub(crate) const MIN_SUPPORTED_FORMAT_VERSION: u32 = v0::SPEC_VERSION;

/// Where a spec version keeps each part of a store.
///
/// A version owns one of these as a constant, which is what freezes its layout:
/// a store laid out differently is a new spec version. The reading itself does
/// not vary, so every version shares [`read_files`].
pub(super) struct Layout {
    pub(super) schema_file: &'static str,
    pub(super) schema_dir: &'static str,
    pub(super) policies_dir: &'static str,
    pub(super) templates_dir: &'static str,
    pub(super) entities_dir: &'static str,
}

/// The raw files of a store, read from a version's [`Layout`].
#[derive(Debug)]
pub(super) struct Files {
    /// Raw `metadata.json` content, already read to pick the version.
    pub(super) metadata_json: String,
    /// Parsed schema; absent when running without one.
    pub(super) schema: Option<ParsedSchema>,
    /// Whether a schema source was present, regardless of whether it loaded.
    pub(super) schema_source_exists: bool,
    pub(super) policies: Vec<StoreFile>,
    pub(super) templates: Vec<StoreFile>,
    pub(super) entities: Vec<StoreFile>,
}

/// Reads a store's files from the layout its version declares.
pub(super) fn read_files(
    fs: &StoreFs<'_>,
    layout: &Layout,
    metadata_json: String,
    strict: bool,
) -> Result<Files, PolicyStoreError> {
    fs.require_dir(layout.policies_dir)?;
    let schema = fs.schema(layout.schema_file, layout.schema_dir, strict)?;

    Ok(Files {
        metadata_json,
        schema: schema.schema,
        schema_source_exists: schema.source_exists,
        policies: fs.cedar_files(layout.policies_dir)?,
        templates: fs.optional_cedar_files(layout.templates_dir)?,
        entities: fs.optional_json_files(layout.entities_dir)?,
    })
}

/// Where a version keeps one JSON file per issuer, in a directory per kind.
pub(super) struct IssuerDirs {
    pub(super) trusted_issuers: &'static str,
    pub(super) custom_issuers: &'static str,
}

/// The issuer files read from [`IssuerDirs`].
#[derive(Debug)]
pub(super) struct IssuerDirFiles {
    pub(super) trusted_issuers: Vec<StoreFile>,
    pub(super) custom_issuers: Vec<StoreFile>,
}

/// Reads the issuer files of a version that keeps them in [`IssuerDirs`].
pub(super) fn read_issuer_dirs(
    fs: &StoreFs<'_>,
    dirs: &IssuerDirs,
) -> Result<IssuerDirFiles, PolicyStoreError> {
    Ok(IssuerDirFiles {
        trusted_issuers: fs.optional_json_files(dirs.trusted_issuers)?,
        custom_issuers: fs.optional_json_files(dirs.custom_issuers)?,
    })
}

/// Parts of a store whose format does not depend on the spec version.
#[derive(Debug)]
pub(crate) struct StoreContent {
    pub(crate) schema: Option<ParsedSchema>,
    pub(crate) schema_source_exists: bool,
    pub(crate) policies: Vec<StoreFile>,
    pub(crate) templates: Vec<StoreFile>,
    pub(crate) entities: Vec<StoreFile>,
}

/// Non-fatal findings from parsing a store; logged by the caller.
///
/// What a single version or migration step reports lives in that version's or
/// step's own type, so dropping a version removes its variant here. The message
/// names the version or step it came from.
#[derive(Debug, Clone, PartialEq, Eq, Hash, derive_more::Display, derive_more::From)]
pub(crate) enum PolicyStoreWarning {
    /// `metadata.json` has no `policy_store_spec_version`.
    #[display(
        "policy store format is outdated: metadata.json has no policy_store_spec_version, so \
         it was read as spec version {assumed}; please update the policy store to spec \
         version {current}"
    )]
    #[from(ignore)]
    MissingSpecVersion { assumed: u32, current: u32 },
    /// The store declares a supported but older spec version.
    #[display(
        "policy store format is outdated: policy_store_spec_version is {found}; please update \
         the policy store to spec version {current}"
    )]
    #[from(ignore)]
    OutdatedSpecVersion { found: u32, current: u32 },
    /// Reported while reading a v2 store.
    #[display("spec version 2: {_0}")]
    V2(v2::V2Warning),
    /// Reported while migrating a v1 store to v2.
    #[display("spec version 1 to 2: {_0}")]
    V1ToV2(V1ToV2Warning),
}

/// A step that reports nothing converts with no variant here.
impl From<Infallible> for PolicyStoreWarning {
    fn from(never: Infallible) -> Self {
        match never {}
    }
}

/// Error from parsing a store's files, split the same way as the read errors
/// so metadata problems stay distinguishable from content problems.
#[derive(Debug, thiserror::Error)]
pub(crate) enum ParseStoreError {
    #[error(transparent)]
    Validation(#[from] ValidationError),
    #[error(transparent)]
    Conversion(#[from] ConversionError),
}

/// Error from [`read_policy_store`]: either the store's files could not be
/// read, or what was read could not be parsed.
#[derive(Debug, thiserror::Error)]
pub(crate) enum ReadStoreError {
    #[error(transparent)]
    Read(#[from] PolicyStoreError),
    #[error(transparent)]
    Parse(#[from] ParseStoreError),
    #[error(transparent)]
    Migration(#[from] MigrationError),
}

/// Reads only `policy_store_spec_version` out of `metadata.json`, skipping every
/// other key, so no value tree is built for the rest of the file.
struct SpecVersionProbe {
    policy_store_spec_version: Option<SpecVersionField>,
}

impl<'de> Deserialize<'de> for SpecVersionProbe {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        deserializer.deserialize_map(SpecVersionProbeVisitor)
    }
}

struct SpecVersionProbeVisitor;

impl<'de> Visitor<'de> for SpecVersionProbeVisitor {
    type Value = SpecVersionProbe;

    fn expecting(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("a metadata.json object")
    }

    fn visit_map<A: MapAccess<'de>>(self, mut map: A) -> Result<Self::Value, A::Error> {
        let mut policy_store_spec_version = None;
        while let Some(key) = map.next_key::<Cow<'de, str>>()? {
            if key == "policy_store_spec_version" {
                // `Option` maps an explicit `null` to `None`, same as absent.
                policy_store_spec_version = map.next_value()?;
            } else {
                map.next_value::<IgnoredAny>()?;
            }
        }
        Ok(SpecVersionProbe {
            policy_store_spec_version,
        })
    }
}

/// The field as written: a version we can dispatch on, or something we cannot.
///
/// Serde validates the `u32` itself; the raw value is kept only to name it in
/// the error.
#[derive(Deserialize)]
#[serde(untagged)]
enum SpecVersionField {
    Supported(u32),
    Unsupported(serde_json::Value),
}

/// Reads `policy_store_spec_version`; a missing field and an explicit `null`
/// both mean "unversioned", which the caller resolves to the baseline.
fn probe_spec_version(metadata_json: &str) -> Result<Option<u32>, ValidationError> {
    let probe: SpecVersionProbe = serde_json::from_str(metadata_json).map_err(|source| {
        ValidationError::MetadataJsonParseFailed {
            file: METADATA_FILE.to_string(),
            source,
        }
    })?;

    match probe.policy_store_spec_version {
        None => Ok(None),
        Some(SpecVersionField::Supported(version)) => Ok(Some(version)),
        Some(SpecVersionField::Unsupported(value)) => Err(ValidationError::InvalidSpecVersion {
            value: value.to_string(),
        }),
    }
}

/// Supported spec versions; [`select_version`] is the only place that maps
/// numbers to version modules.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum FormatVersion {
    V0,
    V1,
    V2,
}

/// The format version a store resolved to.
#[derive(Debug, Clone, Copy)]
struct SelectedVersion {
    /// What `metadata.json` declared; `None` when the field is absent.
    declared: Option<u32>,
    /// The version the store is read as.
    spec_version: u32,
    format: FormatVersion,
}

/// Picks the format for `metadata.json`, failing closed outside
/// `MIN_SUPPORTED_FORMAT_VERSION..=CURRENT_FORMAT_VERSION`.
fn select_version(metadata_json: &str) -> Result<SelectedVersion, ValidationError> {
    let declared = probe_spec_version(metadata_json)?;
    let spec_version = declared.unwrap_or(MIN_SUPPORTED_FORMAT_VERSION);

    let format = match spec_version {
        v0::SPEC_VERSION => FormatVersion::V0,
        v1::SPEC_VERSION => FormatVersion::V1,
        v2::SPEC_VERSION => FormatVersion::V2,
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
        format,
    })
}

/// Reads [`METADATA_FILE`] and resolves the spec version it declares, before
/// any other file of the store is touched.
fn read_metadata(fs: &StoreFs<'_>) -> Result<(String, SelectedVersion), PolicyStoreError> {
    let metadata_json = fs.read_required_file(METADATA_FILE)?;
    let selected = select_version(&metadata_json)?;
    Ok((metadata_json, selected))
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

/// What parsing reported about a store's format.
///
/// Kept beside the store rather than inside it: the caller logs this and drops
/// it, so nothing format-related reaches the runtime.
#[derive(Debug, Clone, Default)]
pub(crate) struct FormatReport {
    /// Version the store was read as; `None` for Agama YAML, which is unversioned.
    pub(crate) spec_version: Option<u32>,
    /// Non-fatal findings, for the caller to log.
    pub(crate) warnings: Vec<PolicyStoreWarning>,
}

/// A parsed store and what its format reported. Only `store` reaches the runtime.
#[cfg_attr(test, derive(Debug))]
pub(crate) struct ParsedPolicyStore {
    pub(crate) store: PolicyStoreWithID,
    pub(crate) format: FormatReport,
}

/// Reads a directory / `.cjar` store into its runtime form.
///
/// [`METADATA_FILE`] is read first and picks the spec version, so an
/// unsupported store fails with a version error rather than a layout, schema
/// or policy error. That version then reads the rest of the store from its own
/// layout and parses it. Older versions are migrated step by step; only the
/// current version converts into runtime types.
/// `bundle_hash` is `Some` exactly when `fs` is a `.cjar`, and comes from its bytes.
pub(super) fn read_policy_store(
    fs: &StoreFs<'_>,
    strict_schema_validation: bool,
    bundle_hash: Option<Digest>,
) -> Result<ParsedPolicyStore, ReadStoreError> {
    let (metadata_json, selected) = read_metadata(fs)?;
    let strict = strict_schema_validation;

    let mut warnings = warnings_for(selected.declared);
    let doc = match selected.format {
        FormatVersion::V0 => v0::read(fs, metadata_json, strict)?.into_current(&mut warnings)?,
        FormatVersion::V1 => v1::read(fs, metadata_json, strict)?.into_current(&mut warnings)?,
        FormatVersion::V2 => {
            let mut v2_warnings = Vec::new();
            let doc = v2::read(fs, metadata_json, strict, &mut v2_warnings)?;
            warnings.extend(v2_warnings.into_iter().map(PolicyStoreWarning::from));
            doc.into_current(&mut warnings)?
        },
    };

    let (store, metadata) = current::into_runtime(doc)?;

    Ok(ParsedPolicyStore {
        store: PolicyStoreWithID {
            id: metadata.policy_store.id.clone(),
            store,
            metadata: Some(metadata),
            bundle_hash,
        },
        format: FormatReport {
            spec_version: Some(selected.spec_version),
            warnings,
        },
    })
}

/// The raw files of a v0 store, for tests of the v0 layout.
#[cfg(test)]
#[derive(Debug, derive_more::Deref)]
pub(super) struct V0Files {
    #[deref]
    pub(super) files: Files,
    pub(super) trusted_issuers: Vec<StoreFile>,
    pub(super) custom_issuers: Vec<StoreFile>,
}

/// Reads the raw files of a v0 store without parsing them, for tests of the
/// v0 layout. Runs the same metadata and version checks as
/// [`read_policy_store`].
#[cfg(test)]
pub(super) fn read_v0_files(fs: &StoreFs<'_>, strict: bool) -> Result<V0Files, PolicyStoreError> {
    let (metadata_json, selected) = read_metadata(fs)?;
    assert_eq!(
        selected.format,
        FormatVersion::V0,
        "test fixtures for the v0 layout must resolve to v0"
    );
    v0::files(fs, metadata_json, strict)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::common::policy_store::vfs_adapter::MemoryVfs;

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

    /// Reads a store with the given metadata in the layout shared by v0 and v1.
    fn read(metadata_json: &str) -> Result<ParsedPolicyStore, ReadStoreError> {
        let vfs = MemoryVfs::new();
        for (path, content) in [
            (METADATA_FILE, metadata_json),
            (
                "policies/allow.cedar",
                "permit(principal, action, resource);",
            ),
            (
                "trusted-issuers/jans.json",
                r#"{
                    "name": "Jans",
                    "openid_configuration_endpoint": "https://jans.test/.well-known/openid-configuration",
                    "token_metadata": {
                        "access_token": { "entity_type_name": "Jans::Access_token" }
                    }
                }"#,
            ),
            (
                "custom-issuers/acme.json",
                r#"{ "tokens_mappings": { "Acme::Custom": {} } }"#,
            ),
        ] {
            vfs.create_file(path, content.as_bytes())
                .expect("in-memory test file should be created");
        }
        let fs = StoreFs::open(&vfs, ".").expect("in-memory root should open");
        read_policy_store(&fs, false, None)
    }

    /// Reads a store in the v2 layout, describing the same token types as the
    /// store [`read`] builds.
    fn read_v2(metadata_json: &str) -> Result<ParsedPolicyStore, ReadStoreError> {
        read_v2_with(metadata_json, &[])
    }

    /// [`read_v2`] with `extra` files added to the store.
    fn read_v2_with(
        metadata_json: &str,
        extra: &[(&str, &str)],
    ) -> Result<ParsedPolicyStore, ReadStoreError> {
        let vfs = MemoryVfs::new();
        for (path, content) in extra {
            vfs.create_file(path, content.as_bytes())
                .expect("in-memory test file should be created");
        }
        for (path, content) in [
            (METADATA_FILE, metadata_json),
            (
                "policies/allow.cedar",
                "permit(principal, action, resource);",
            ),
            (
                "trusted-issuers.json",
                r#"{
                    "Jans::Access_token": {
                        "type": "jwt",
                        "issuer": "Jans",
                        "configuration_endpoints": {
                            "configuration": "https://jans.test/.well-known/openid-configuration"
                        }
                    },
                    "Acme::Custom": { "type": "custom", "issuer": "acme" }
                }"#,
            ),
        ] {
            vfs.create_file(path, content.as_bytes())
                .expect("in-memory test file should be created");
        }
        let fs = StoreFs::open(&vfs, ".").expect("in-memory root should open");
        read_policy_store(&fs, false, None)
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

    /// Other keys are skipped rather than deserialized, including nested ones
    /// and ones the version parser would later reject.
    #[test]
    fn probe_skips_every_other_key() {
        let json = r#"{
            "cedar_version": { "unexpected": ["shape", 1, null] },
            "policy_store": "not an object either",
            "policy_store_spec_version": 1,
            "trailing": [[[1]]]
        }"#;

        assert_eq!(
            probe_spec_version(json).expect("unrelated keys must not affect the probe"),
            Some(1),
            "the version should be read regardless of the other keys"
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

    /// Only the metadata file is read before the version is known, so a store
    /// of an unknown version fails on its version even when it lacks
    /// directories that every known layout requires.
    #[test]
    fn read_resolves_version_before_reading_the_layout() {
        let vfs = MemoryVfs::new();
        vfs.create_file(METADATA_FILE, metadata_with_version("99").as_bytes())
            .expect("in-memory metadata should be created");
        let fs = StoreFs::open(&vfs, ".").expect("in-memory root should open");

        let err = read_policy_store(&fs, true, None)
            .expect_err("an unsupported version must be rejected");
        assert!(
            matches!(
                err,
                ReadStoreError::Read(PolicyStoreError::Validation(
                    ValidationError::SpecVersionTooNew { found: 99, .. }
                ))
            ),
            "expected SpecVersionTooNew rather than a missing-directory error, got: {err:?}"
        );
    }

    #[test]
    fn probe_rejects_non_integer_versions() {
        for declared in [r#""2""#, "-1", "2.5", "4294967296", "[2]"] {
            let err = probe_spec_version(&metadata_with_version(declared))
                .expect_err("a non-integer version must be rejected");
            assert!(
                matches!(&err, ValidationError::InvalidSpecVersion { value } if value == declared),
                "expected InvalidSpecVersion naming {declared}, got: {err:?}"
            );
        }
    }

    #[test]
    fn missing_version_parses_as_oldest_with_warning() {
        let parsed = read(V0_METADATA).expect("a store without a spec version should parse");
        assert_eq!(
            parsed.format.spec_version,
            Some(MIN_SUPPORTED_FORMAT_VERSION),
            "a missing version should be read as the oldest supported one"
        );
        assert_eq!(
            parsed.format.warnings,
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
        let parsed = read(&metadata_with_version("0")).expect("an explicit v0 store should parse");
        assert_eq!(
            parsed.format.spec_version,
            Some(0),
            "declared version should be kept"
        );
        assert_eq!(
            parsed.format.warnings,
            vec![PolicyStoreWarning::OutdatedSpecVersion {
                found: 0,
                current: CURRENT_FORMAT_VERSION,
            }],
            "an older version should produce exactly one OutdatedSpecVersion warning"
        );
    }

    #[test]
    fn current_version_parses_without_warnings() {
        let parsed = read_v2(&metadata_with_version(&CURRENT_FORMAT_VERSION.to_string()))
            .expect("a current-version store should parse");
        assert_eq!(
            parsed.format.spec_version,
            Some(CURRENT_FORMAT_VERSION),
            "declared version should be kept"
        );
        assert!(
            parsed.format.warnings.is_empty(),
            "a current-version store should not warn, got: {:?}",
            parsed.format.warnings
        );
    }

    #[test]
    fn leftover_issuer_directory_in_v2_store_warns() {
        let parsed = read_v2_with(
            &metadata_with_version("2"),
            &[("custom-issuers/acme.json", r#"{ "tokens_mappings": {} }"#)],
        )
        .expect("a v2 store with a leftover directory should still load");
        assert_eq!(
            parsed.format.warnings,
            vec![PolicyStoreWarning::V2(v2::V2Warning::IgnoredIssuerDir {
                dir: "custom-issuers"
            })],
            "the leftover directory should be reported"
        );
        assert!(
            parsed.format.warnings[0]
                .to_string()
                .starts_with("spec version 2: "),
            "the message should name the spec version that reported it, got: {}",
            parsed.format.warnings[0]
        );
    }

    #[test]
    fn every_version_produces_the_same_runtime_store() {
        let from_missing = read(V0_METADATA).expect("store without a version should parse");
        let from_v0 = read(&metadata_with_version("0")).expect("v0 store should parse");
        let from_v1 = read(&metadata_with_version("1")).expect("v1 store should parse");
        let from_v2 = read_v2(&metadata_with_version("2")).expect("v2 store should parse");

        for (label, parsed) in [("v0", &from_v0), ("v1", &from_v1), ("v2", &from_v2)] {
            assert_eq!(
                parsed.store.store, from_missing.store.store,
                "{label} should build the same runtime store as an unversioned one"
            );
            assert_eq!(
                parsed.store.metadata, from_missing.store.metadata,
                "{label} should build the same runtime metadata as an unversioned one"
            );
        }
        assert_eq!(
            from_v1.store.id, "abc123def456",
            "id should come from policy_store.id"
        );
        assert!(
            from_v1.store.token_types.jwt_issuers().contains_key("Jans"),
            "a JWT issuer should be keyed by its name"
        );
        assert!(
            from_v1.store.token_types.get("Acme::Custom").is_some(),
            "custom issuer settings should reach the runtime store"
        );
    }

    /// `select_version` maps numbers to modules with a `match` on `u32`, which
    /// the compiler cannot check for gaps; this does.
    #[test]
    fn every_supported_version_is_selectable() {
        for version in MIN_SUPPORTED_FORMAT_VERSION..=CURRENT_FORMAT_VERSION {
            let selected = select_version(&metadata_with_version(&version.to_string()))
                .unwrap_or_else(|err| panic!("version {version} should be selectable: {err:?}"));
            assert_eq!(
                selected.spec_version, version,
                "version {version} should be read as itself"
            );
        }
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
        let v2_sources = [
            ("v2/mod.rs", include_str!("v2/mod.rs")),
            ("v2/metadata.rs", include_str!("v2/metadata.rs")),
            (
                "v2/trusted_issuers.rs",
                include_str!("v2/trusted_issuers.rs"),
            ),
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
        check(&v0_sources, &["v1::", "current::", "migration::"]);
        check(&v0_sources, &["v2::"]);
        check(&v1_sources, &["v0::", "v2::", "current::", "migration::"]);
        check(&v2_sources, &["v0::", "v1::", "current::", "migration::"]);
        check(&runtime_sources, &["formats::v", "migration::"]);
    }
}
