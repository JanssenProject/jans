// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store format v2: every accepted token type, JWT and custom, in one
//! `trusted-issuers.json` keyed by Cedar entity type.

pub(crate) mod metadata;
pub(crate) mod trusted_issuers;

use metadata::MetadataDoc;
use trusted_issuers::TrustedIssuersDoc;

use super::{Files, Layout, ParseStoreError, ReadStoreError, StoreContent};
use crate::common::policy_store::PolicyStore;
use crate::common::policy_store::loader::StoreFs;
use crate::common::policy_store::manager::{ConversionError, PolicyStoreManager};
use crate::common::policy_store::metadata::PolicyStoreMetadata;

pub(crate) const SPEC_VERSION: u32 = 2;

// Where each part of a v2 store lives. Frozen with the version: a store laid
// out differently is a new spec version, so this is never shared with or
// edited for another version.
const LAYOUT: Layout = Layout {
    schema_file: "schema.cedarschema",
    schema_dir: "schemas",
    policies_dir: "policies",
    templates_dir: "templates",
    entities_dir: "entities",
};

/// Every token type of the store; a store without it accepts no tokens.
const TRUSTED_ISSUERS_FILE: &str = "trusted-issuers.json";

/// Where earlier versions kept issuers. Ignored, but reported, since one left
/// behind by a manual migration is likely a mistake.
const LEGACY_ISSUER_DIRS: [&str; 2] = ["trusted-issuers", "custom-issuers"];

/// Reads a v2 store and parses its files; `metadata_json` has already been
/// read by the caller to pick this version.
pub(super) fn read(
    fs: &StoreFs<'_>,
    metadata_json: String,
    strict: bool,
) -> Result<PolicyStoreDoc, ReadStoreError> {
    let files = super::read_files(fs, &LAYOUT, metadata_json, strict)?;
    let trusted_issuers = fs.optional_file(TRUSTED_ISSUERS_FILE)?;
    let ignored_dirs = LEGACY_ISSUER_DIRS
        .into_iter()
        .filter(|dir| fs.has_dir(dir))
        .collect();
    Ok(parse(files, trusted_issuers.as_deref(), ignored_dirs)?)
}

/// A whole v2 store, parsed but not yet converted into runtime types.
#[derive(Debug)]
pub(crate) struct PolicyStoreDoc {
    pub(crate) metadata: MetadataDoc,
    pub(crate) trusted_issuers: TrustedIssuersDoc,
    pub(crate) content: StoreContent,
    /// Issuer directories of earlier versions found in the store, ignored.
    pub(crate) ignored_dirs: Vec<&'static str>,
}

/// Parses the raw files of a v2 store.
fn parse(
    files: Files,
    trusted_issuers_json: Option<&str>,
    ignored_dirs: Vec<&'static str>,
) -> Result<PolicyStoreDoc, ParseStoreError> {
    let Files {
        metadata_json,
        schema,
        schema_source_exists,
        policies,
        templates,
        entities,
    } = files;

    let metadata = metadata::parse(&metadata_json)?;
    let trusted_issuers = trusted_issuers_json
        .map(trusted_issuers::parse)
        .transpose()
        .map_err(|source| ConversionError::TrustedIssuersFile {
            file: TRUSTED_ISSUERS_FILE.to_string(),
            source,
        })?
        .unwrap_or_default();

    Ok(PolicyStoreDoc {
        metadata,
        trusted_issuers,
        content: StoreContent {
            schema,
            schema_source_exists,
            policies,
            templates,
            entities,
        },
        ignored_dirs,
    })
}

/// Converts a v2 store into the runtime store and its metadata.
pub(crate) fn into_runtime(
    doc: PolicyStoreDoc,
) -> Result<(PolicyStore, PolicyStoreMetadata), ParseStoreError> {
    let metadata = metadata::into_runtime(doc.metadata)?;
    let token_types = doc
        .trusted_issuers
        .into_runtime()
        .map_err(ConversionError::from)?;
    let store = PolicyStoreManager::build(
        &doc.content,
        metadata.policy_store.version.clone(),
        token_types,
    )?;
    Ok((store, metadata))
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::common::policy_store::formats::METADATA_FILE;
    use crate::common::policy_store::vfs_adapter::MemoryVfs;

    const METADATA: &str = r#"{
        "policy_store_spec_version": 2,
        "cedar_version": "4.4.0",
        "policy_store": { "id": "abc123def456", "name": "Golden v2", "version": "1.0.0" }
    }"#;

    const TRUSTED_ISSUERS: &str = r#"{
        "Acme::Access_token": {
            "type": "jwt",
            "issuer": "Acme",
            "configuration_endpoints": {
                "configuration": "https://idp.acme.com/.well-known/openid-configuration"
            }
        },
        "Dolphin::Dolphin_token": { "type": "custom", "issuer": "Dolphin" }
    }"#;

    /// Reads a v2 store made of `files` plus metadata and one policy.
    fn read_store(files: &[(&str, &str)]) -> Result<PolicyStoreDoc, ReadStoreError> {
        let vfs = MemoryVfs::new();
        for (path, content) in [
            (METADATA_FILE, METADATA),
            (
                "policies/allow.cedar",
                "permit(principal, action, resource);",
            ),
        ]
        .iter()
        .chain(files)
        {
            vfs.create_file(path, content.as_bytes())
                .expect("in-memory test file should be created");
        }
        let fs = StoreFs::open(&vfs, ".").expect("in-memory root should open");
        let metadata_json = fs
            .read_required_file(METADATA_FILE)
            .expect("metadata should be readable");
        read(&fs, metadata_json, false)
    }

    /// Golden v2 store: both token kinds reach the runtime store.
    #[test]
    fn golden_store_converts_to_runtime() {
        let doc = read_store(&[(TRUSTED_ISSUERS_FILE, TRUSTED_ISSUERS)])
            .expect("golden v2 store should read");
        assert_eq!(
            doc.metadata.policy_store_spec_version, SPEC_VERSION,
            "spec version should be read from metadata.json"
        );
        assert!(doc.ignored_dirs.is_empty(), "nothing should be ignored");

        let (store, metadata) = into_runtime(doc).expect("golden v2 store should convert");
        assert_eq!(
            metadata.policy_store.name, "Golden v2",
            "metadata name mismatch"
        );
        assert_eq!(
            store
                .token_types
                .get("Acme::Access_token")
                .map(|t| t.context_key.as_str()),
            Some("acme_access_token"),
            "the JWT token type should be converted"
        );
        assert_eq!(
            store
                .token_types
                .get("Dolphin::Dolphin_token")
                .map(|t| t.context_key.as_str()),
            Some("dolphin_dolphin_token"),
            "the custom token type should be converted"
        );
    }

    #[test]
    fn missing_file_means_no_trusted_issuers() {
        let doc = read_store(&[]).expect("a store without trusted-issuers.json should read");
        let (store, _) = into_runtime(doc).expect("it should convert");
        assert!(
            store.token_types.jwt_issuers().is_empty()
                && store.token_types.get("Acme::Access_token").is_none(),
            "a store without the file should accept no tokens"
        );
    }

    #[test]
    fn leftover_issuer_directories_are_ignored_and_reported() {
        let doc = read_store(&[
            (TRUSTED_ISSUERS_FILE, TRUSTED_ISSUERS),
            ("trusted-issuers/old.json", r#"{ "name": "Old" }"#),
            ("custom-issuers/old.json", "not even json"),
        ])
        .expect("a v2 store with leftover directories should still read");

        assert_eq!(
            doc.ignored_dirs,
            vec!["trusted-issuers", "custom-issuers"],
            "both leftover directories should be reported"
        );
        let (store, _) = into_runtime(doc).expect("it should convert");
        assert!(
            store.token_types.jwt_issuers().contains_key("Acme")
                && !store.token_types.jwt_issuers().contains_key("Old"),
            "only trusted-issuers.json should be read"
        );
    }

    #[test]
    fn invalid_file_is_reported_with_its_name() {
        let err = read_store(&[(TRUSTED_ISSUERS_FILE, "{ not json")])
            .expect_err("a malformed trusted-issuers.json must be rejected");
        assert!(
            matches!(
                &err,
                ReadStoreError::Parse(ParseStoreError::Conversion(
                    ConversionError::TrustedIssuersFile { file, .. }
                )) if file == TRUSTED_ISSUERS_FILE
            ),
            "expected TrustedIssuersFile naming the file, got: {err:?}"
        );
    }

    #[test]
    fn conflicting_entries_fail_to_convert() {
        let doc = read_store(&[(
            TRUSTED_ISSUERS_FILE,
            r#"{
                "Acme::Access_token": { "type": "custom", "issuer": "Acme" },
                "Other::Access_token": { "type": "custom", "issuer": "ACME" }
            }"#,
        )])
        .expect("each entry is valid on its own");

        let err = into_runtime(doc).expect_err("colliding context.tokens keys must be rejected");
        assert!(
            matches!(
                &err,
                ParseStoreError::Conversion(ConversionError::TokenTypes(
                    crate::common::policy_store::TokenTypesError::DuplicateContextKey { key, .. }
                )) if key == "acme_access_token"
            ),
            "expected DuplicateContextKey, got: {err:?}"
        );
    }
}
