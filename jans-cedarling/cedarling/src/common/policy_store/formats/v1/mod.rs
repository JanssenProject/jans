// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store format v1: the v0 layout with a required
//! `policy_store_spec_version` in `metadata.json`.
//!
//! v1 stores are migrated to the current version before conversion into
//! runtime types.

pub(crate) mod custom_issuer;
pub(crate) mod metadata;
pub(crate) mod trusted_issuer;

use custom_issuer::{CustomIssuerParser, ParsedCustomIssuer};
use metadata::MetadataDoc;
use trusted_issuer::{IssuerParser, ParsedIssuer};

use super::{
    Files, IssuerDirFiles, IssuerDirs, Layout, ParseStoreError, ReadStoreError, StoreContent,
};
use crate::common::policy_store::loader::StoreFs;
use crate::common::policy_store::manager::ConversionError;

pub(crate) const SPEC_VERSION: u32 = 1;

// Where each part of a v1 store lives. Frozen with the version: a store laid
// out differently is a new spec version, so this is never shared with or
// edited for another version.
const LAYOUT: Layout = Layout {
    schema_file: "schema.cedarschema",
    schema_dir: "schemas",
    policies_dir: "policies",
    templates_dir: "templates",
    entities_dir: "entities",
};

const ISSUER_DIRS: IssuerDirs = IssuerDirs {
    trusted_issuers: "trusted-issuers",
    custom_issuers: "custom-issuers",
};

/// Reads a v1 store and parses its files; `metadata_json` has already been
/// read by the caller to pick this version.
pub(super) fn read(
    fs: &StoreFs<'_>,
    metadata_json: String,
    strict: bool,
) -> Result<PolicyStoreDoc, ReadStoreError> {
    let files = super::read_files(fs, &LAYOUT, metadata_json, strict)?;
    let issuers = super::read_issuer_dirs(fs, &ISSUER_DIRS)?;
    Ok(parse(files, issuers)?)
}

/// A whole v1 store, parsed but not yet converted into runtime types.
#[derive(Debug)]
pub(crate) struct PolicyStoreDoc {
    pub(crate) metadata: MetadataDoc,
    pub(crate) trusted_issuers: Vec<ParsedIssuer>,
    pub(crate) custom_issuers: Vec<ParsedCustomIssuer>,
    pub(crate) content: StoreContent,
}

/// Parses the raw files of a v1 store.
fn parse(files: Files, issuers: IssuerDirFiles) -> Result<PolicyStoreDoc, ParseStoreError> {
    let Files {
        metadata_json,
        schema,
        schema_source_exists,
        policies,
        templates,
        entities,
    } = files;
    let IssuerDirFiles {
        trusted_issuers,
        custom_issuers,
    } = issuers;

    let metadata = metadata::parse(&metadata_json)?;

    let mut parsed_issuers = Vec::with_capacity(trusted_issuers.len());
    for file in &trusted_issuers {
        let parsed = IssuerParser::parse_issuer(&file.content, &file.name).map_err(|e| {
            ConversionError::TrustedIssuerParse {
                file: file.name.clone(),
                source: Box::new(e),
            }
        })?;
        parsed_issuers.extend(parsed);
    }

    let parsed_custom_issuers = custom_issuers
        .iter()
        .map(|file| {
            CustomIssuerParser::parse(&file.content, &file.name)
                .map_err(ConversionError::CustomIssuerParse)
        })
        .collect::<Result<Vec<_>, _>>()?;

    Ok(PolicyStoreDoc {
        metadata,
        trusted_issuers: parsed_issuers,
        custom_issuers: parsed_custom_issuers,
        content: StoreContent {
            schema,
            schema_source_exists,
            policies,
            templates,
            entities,
        },
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::common::policy_store::errors::PolicyStoreError;
    use crate::common::policy_store::loader::StoreFile;

    const METADATA: &str = r#"{
        "policy_store_spec_version": 1,
        "cedar_version": "4.4.0",
        "policy_store": { "id": "abc123def456", "name": "Golden v1", "version": "1.0.0" }
    }"#;

    fn issuer_file(name: &str, id: &str) -> StoreFile {
        StoreFile {
            name: name.to_string(),
            content: format!(
                r#"{{
                    "id": "{id}",
                    "name": "{id}",
                    "openid_configuration_endpoint": "https://{id}.test/.well-known/openid-configuration",
                    "token_metadata": {{
                        "access_token": {{ "entity_type_name": "{id}::Access_token" }}
                    }}
                }}"#
            ),
        }
    }

    fn loaded(
        trusted_issuers: Vec<StoreFile>,
        custom_issuers: Vec<StoreFile>,
    ) -> (Files, IssuerDirFiles) {
        let files = Files {
            metadata_json: METADATA.to_string(),
            schema: None,
            schema_source_exists: false,
            policies: vec![StoreFile {
                name: "allow.cedar".to_string(),
                content: "permit(principal, action, resource);".to_string(),
            }],
            templates: vec![],
            entities: vec![],
        };
        let issuers = IssuerDirFiles {
            trusted_issuers,
            custom_issuers,
        };
        (files, issuers)
    }

    /// Golden v1 store: every version-specific file parses with v1 semantics.
    #[test]
    fn golden_store_parses() {
        let (files, issuers) = loaded(
            vec![issuer_file("a.json", "Alpha")],
            vec![StoreFile {
                name: "acme.json".to_string(),
                content:
                    r#"{ "tokens_mappings": { "Acme::Custom": { "required_claims": ["sub"] } } }"#
                        .to_string(),
            }],
        );
        let doc = parse(files, issuers).expect("golden v1 store should parse");

        assert_eq!(
            doc.metadata.policy_store_spec_version, SPEC_VERSION,
            "spec version should be read from metadata.json"
        );
        assert_eq!(
            doc.trusted_issuers[0].issuer.token_metadata["access_token"].entity_type_name,
            "Alpha::Access_token",
            "trusted issuer token metadata should be parsed"
        );
        assert_eq!(
            doc.custom_issuers[0].id, "acme",
            "custom issuer id comes from the file"
        );
        assert!(
            doc.custom_issuers[0].meta.tokens_mappings["Acme::Custom"]
                .required_claims
                .contains("sub"),
            "custom issuer required_claims should be parsed"
        );
    }

    #[test]
    fn invalid_issuer_file_is_reported_with_file_name() {
        let err = {
            let (files, issuers) = loaded(
                vec![StoreFile {
                    name: "bad.json".to_string(),
                    content: "{ not json".to_string(),
                }],
                vec![],
            );
            parse(files, issuers)
        }
        .expect_err("malformed issuer file must be rejected");
        assert!(
            matches!(
                &err,
                ParseStoreError::Conversion(ConversionError::TrustedIssuerParse { file, source })
                    if file == "bad.json"
                        && matches!(**source, PolicyStoreError::JsonParsing { .. })
            ),
            "expected TrustedIssuerParse naming the file, got: {err:?}"
        );
    }
}
