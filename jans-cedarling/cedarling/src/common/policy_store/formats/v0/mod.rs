// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store format v0: the directory / `.cjar` layout without
//! `policy_store_spec_version`.
//!
//! Also used when the field is missing. v0 stores are migrated to the current
//! version before conversion into runtime types.

pub(crate) mod custom_issuer;
pub(crate) mod metadata;
pub(crate) mod trusted_issuer;

use custom_issuer::{CustomIssuerParser, ParsedCustomIssuer};
use metadata::MetadataDoc;
use trusted_issuer::{IssuerParser, ParsedIssuer};

use super::{ParseStoreError, StoreContent};
use crate::common::policy_store::loader::LoadedPolicyStore;
use crate::common::policy_store::manager::ConversionError;

pub(crate) const SPEC_VERSION: u32 = 0;

/// A whole v0 store, parsed but not yet migrated.
#[derive(Debug)]
pub(crate) struct PolicyStoreDoc {
    pub(crate) metadata: MetadataDoc,
    pub(crate) trusted_issuers: Vec<ParsedIssuer>,
    pub(crate) custom_issuers: Vec<ParsedCustomIssuer>,
    pub(crate) content: StoreContent,
}

/// Parses the version-specific files of a loaded v0 store.
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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::common::policy_store::loader::{CustomIssuerFile, IssuerFile, PolicyFile};

    /// Golden v0 store: no `policy_store_spec_version`, and every version-specific
    /// file parses with v0 semantics, including the frozen `jti` default.
    #[test]
    fn golden_store_parses() {
        let loaded = LoadedPolicyStore {
            metadata_json: r#"{
                "cedar_version": "4.4.0",
                "policy_store": { "id": "abc123def456", "name": "Golden v0", "version": "1.0.0" }
            }"#
            .to_string(),
            schema: None,
            schema_source_exists: false,
            policies: vec![PolicyFile {
                name: "allow.cedar".to_string(),
                content: "permit(principal, action, resource);".to_string(),
            }],
            templates: vec![],
            entities: vec![],
            trusted_issuers: vec![IssuerFile {
                name: "jans.json".to_string(),
                content: r#"{
                    "name": "Jans Server",
                    "description": "Jans OpenID Connect Provider",
                    "configuration_endpoint": "https://jans.test/.well-known/openid-configuration",
                    "token_metadata": {
                        "access_token": { "entity_type_name": "Jans::Access_token" }
                    }
                }"#
                .to_string(),
            }],
            custom_issuers: vec![CustomIssuerFile {
                name: "acme.json".to_string(),
                content:
                    r#"{ "tokens_mappings": { "Acme::Custom": { "required_claims": ["sub"] } } }"#
                        .to_string(),
            }],
        };

        let doc = parse(loaded).expect("golden v0 store should parse");

        assert_eq!(
            doc.metadata.policy_store.name, "Golden v0",
            "metadata name mismatch"
        );
        assert_eq!(
            doc.metadata.policy_store.version, "1.0.0",
            "content version mismatch"
        );

        assert_eq!(
            doc.trusted_issuers.len(),
            1,
            "one issuer file should yield one issuer"
        );
        let token = &doc.trusted_issuers[0].issuer.token_metadata["access_token"];
        assert_eq!(
            token.entity_type_name, "Jans::Access_token",
            "token metadata mismatch"
        );
        assert_eq!(
            token.token_id, "jti",
            "v0 must keep its frozen `jti` default"
        );

        assert!(
            doc.custom_issuers[0].meta.tokens_mappings["Acme::Custom"]
                .required_claims
                .contains("sub"),
            "custom issuer should parse"
        );
        assert_eq!(
            doc.content.policies.len(),
            1,
            "policy files should pass through"
        );
    }
}
