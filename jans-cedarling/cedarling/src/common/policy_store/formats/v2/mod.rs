// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store format v2: the v1 layout with a required
//! `policy_store_spec_version` in `metadata.json`.
//!
//! This is the current version, so it is the only one that converts into
//! runtime types.

pub(crate) mod custom_issuer;
pub(crate) mod metadata;
pub(crate) mod trusted_issuer;

use std::collections::HashMap;

use custom_issuer::{CustomIssuerParser, ParsedCustomIssuer};
use metadata::MetadataDoc;
use trusted_issuer::{IssuerParser, ParsedIssuer};

use super::{ParseStoreError, StoreContent};
use crate::common::policy_store::loader::LoadedPolicyStore;
use crate::common::policy_store::manager::{ConversionError, PolicyStoreManager};
use crate::common::policy_store::metadata::PolicyStoreMetadata;
use crate::common::policy_store::{CustomIssuerMetadata, PolicyStore, TrustedIssuer};

pub(crate) const SPEC_VERSION: u32 = 2;

/// A whole v2 store, parsed but not yet converted into runtime types.
#[derive(Debug)]
pub(crate) struct PolicyStoreDoc {
    pub(crate) metadata: MetadataDoc,
    pub(crate) trusted_issuers: Vec<ParsedIssuer>,
    pub(crate) custom_issuers: Vec<ParsedCustomIssuer>,
    pub(crate) content: StoreContent,
}

/// Parses the version-specific files of a loaded v2 store.
pub(crate) fn parse(loaded: LoadedPolicyStore) -> Result<PolicyStoreDoc, ParseStoreError> {
    let LoadedPolicyStore {
        metadata_json,
        schema,
        schema_source_exists,
        policies,
        templates,
        entities,
        trusted_issuers,
        custom_issuers,
    } = loaded;

    let metadata = metadata::parse(&metadata_json)?;

    let mut parsed_issuers = Vec::with_capacity(trusted_issuers.len());
    for file in &trusted_issuers {
        let parsed = IssuerParser::parse_issuer(&file.content, &file.name).map_err(|e| {
            ConversionError::IssuerConversion(format!("Failed to parse '{}': {}", file.name, e))
        })?;
        parsed_issuers.extend(parsed);
    }

    let parsed_custom_issuers = custom_issuers
        .iter()
        .map(|file| {
            CustomIssuerParser::parse(&file.content, &file.name)
                .map_err(ConversionError::IssuerConversion)
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

/// Converts a v2 store into the runtime store and its metadata.
pub(crate) fn into_runtime(
    doc: PolicyStoreDoc,
    strict_schema_validation: bool,
) -> Result<(PolicyStore, PolicyStoreMetadata), ParseStoreError> {
    let metadata = metadata::into_runtime(doc.metadata)?;
    let trusted_issuers = trusted_issuers_into_runtime(doc.trusted_issuers)?;
    let custom_issuers = custom_issuers_into_runtime(doc.custom_issuers)?;

    let store = PolicyStoreManager::build(
        &doc.content,
        metadata.policy_store.version.clone(),
        trusted_issuers,
        custom_issuers,
        strict_schema_validation,
    )?;

    Ok((store, metadata))
}

/// Rejects duplicate issuer ids across files; `None` when there are no issuers.
fn trusted_issuers_into_runtime(
    issuers: Vec<ParsedIssuer>,
) -> Result<Option<HashMap<String, TrustedIssuer>>, ConversionError> {
    if issuers.is_empty() {
        return Ok(None);
    }
    IssuerParser::validate_issuers(&issuers)
        .map_err(|errors| ConversionError::IssuerConversion(errors.join("; ")))?;
    Ok(Some(IssuerParser::create_issuer_map(issuers)))
}

/// Rejects duplicate custom issuer ids across files.
fn custom_issuers_into_runtime(
    issuers: Vec<ParsedCustomIssuer>,
) -> Result<HashMap<String, CustomIssuerMetadata>, ConversionError> {
    if issuers.is_empty() {
        return Ok(HashMap::new());
    }
    CustomIssuerParser::validate(&issuers)
        .map_err(|errors| ConversionError::IssuerConversion(errors.join("; ")))?;
    Ok(CustomIssuerParser::create_map(issuers))
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::common::policy_store::loader::{CustomIssuerFile, IssuerFile, PolicyFile};

    const METADATA: &str = r#"{
        "policy_store_spec_version": 2,
        "cedar_version": "4.4.0",
        "policy_store": { "id": "abc123def456", "name": "Golden v2", "version": "1.0.0" }
    }"#;

    fn issuer_file(name: &str, id: &str) -> IssuerFile {
        IssuerFile {
            name: name.to_string(),
            content: format!(
                r#"{{
                    "id": "{id}",
                    "name": "Issuer {id}",
                    "openid_configuration_endpoint": "https://{id}.test/.well-known/openid-configuration",
                    "token_metadata": {{
                        "access_token": {{ "entity_type_name": "{id}::Access_token" }}
                    }}
                }}"#
            ),
        }
    }

    fn loaded(
        trusted_issuers: Vec<IssuerFile>,
        custom_issuers: Vec<CustomIssuerFile>,
    ) -> LoadedPolicyStore {
        LoadedPolicyStore {
            metadata_json: METADATA.to_string(),
            schema: None,
            schema_source_exists: false,
            policies: vec![PolicyFile {
                name: "allow.cedar".to_string(),
                content: "permit(principal, action, resource);".to_string(),
            }],
            templates: vec![],
            entities: vec![],
            trusted_issuers,
            custom_issuers,
        }
    }

    /// Golden v2 store: every version-specific file reaches the runtime store.
    #[test]
    fn golden_store_converts_to_runtime() {
        let doc = parse(loaded(
            vec![issuer_file("a.json", "Alpha")],
            vec![CustomIssuerFile {
                name: "acme.json".to_string(),
                content:
                    r#"{ "tokens_mappings": { "Acme::Custom": { "required_claims": ["sub"] } } }"#
                        .to_string(),
            }],
        ))
        .expect("golden v2 store should parse");
        assert_eq!(
            doc.metadata.policy_store_spec_version, SPEC_VERSION,
            "spec version should be read from metadata.json"
        );

        let (store, metadata) = into_runtime(doc, false).expect("golden v2 store should convert");
        assert_eq!(
            metadata.policy_store.name, "Golden v2",
            "metadata name mismatch"
        );
        assert_eq!(
            store.version.as_deref(),
            Some("1.0.0"),
            "content version should come from policy_store.version"
        );

        let issuer = &store
            .trusted_issuers
            .as_ref()
            .expect("trusted issuers should be present")["Alpha"];
        assert_eq!(issuer.name, "Issuer Alpha", "issuer name mismatch");
        assert_eq!(
            issuer.token_metadata["access_token"].entity_type_name, "Alpha::Access_token",
            "token metadata should be converted"
        );
        assert!(
            store.custom_issuers["acme"].tokens_mappings["Acme::Custom"]
                .required_claims
                .contains("sub"),
            "custom issuer required_claims should be converted"
        );
    }

    #[test]
    fn no_issuers_convert_to_empty_runtime_fields() {
        let doc = parse(loaded(vec![], vec![])).expect("store without issuers should parse");
        let (store, _) = into_runtime(doc, false).expect("store without issuers should convert");
        assert!(
            store.trusted_issuers.is_none(),
            "no trusted issuer files should leave trusted_issuers unset"
        );
        assert!(
            store.custom_issuers.is_empty(),
            "no custom issuer files should leave custom_issuers empty"
        );
    }

    #[test]
    fn duplicate_issuer_ids_are_rejected() {
        let doc = parse(loaded(
            vec![issuer_file("a.json", "Dup"), issuer_file("b.json", "Dup")],
            vec![],
        ))
        .expect("each issuer file is valid on its own");

        let err = into_runtime(doc, false).expect_err("duplicate issuer ids must be rejected");
        assert!(
            matches!(
                &err,
                ParseStoreError::Conversion(ConversionError::IssuerConversion(msg))
                    if msg.contains("Dup") && msg.contains("a.json") && msg.contains("b.json")
            ),
            "expected an IssuerConversion error naming the id and both files, got: {err:?}"
        );
    }

    #[test]
    fn invalid_issuer_file_is_reported_with_file_name() {
        let err = parse(loaded(
            vec![IssuerFile {
                name: "bad.json".to_string(),
                content: "{ not json".to_string(),
            }],
            vec![],
        ))
        .expect_err("malformed issuer file must be rejected");
        assert!(
            matches!(
                &err,
                ParseStoreError::Conversion(ConversionError::IssuerConversion(msg))
                    if msg.starts_with("Failed to parse 'bad.json'")
            ),
            "expected an IssuerConversion error naming the file, got: {err:?}"
        );
    }
}
