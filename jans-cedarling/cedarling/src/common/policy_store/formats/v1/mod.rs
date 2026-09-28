// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Policy store format v1: the directory / `.cjar` layout without
//! `policy_store_spec_version`.
//!
//! Also used when the field is missing. v1 stores are migrated to the current
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

pub(crate) const SPEC_VERSION: u32 = 1;

/// A whole v1 store, parsed but not yet migrated.
#[derive(Debug)]
pub(crate) struct PolicyStoreDoc {
    pub(crate) metadata: MetadataDoc,
    pub(crate) trusted_issuers: Vec<ParsedIssuer>,
    pub(crate) custom_issuers: Vec<ParsedCustomIssuer>,
    pub(crate) content: StoreContent,
}

/// Parses the version-specific files of a loaded v1 store.
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
