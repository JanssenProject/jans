// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Migration from format v1 to v2.
//!
//! Both v1 issuer directories merge into the one `trusted-issuers.json` map,
//! keyed by entity type. Everything policy-visible is kept: a JWT issuer's
//! `name` and a custom issuer's sanitized id become the v2 `issuer`, so the
//! `TrustedIssuer` types and `context.tokens` keys stay the same. What v2 has
//! no place for is dropped (`trusted: false` entries, the token keys, ids,
//! descriptions) or reported (`required`, an `iss` that now keeps its path).

use std::collections::HashMap;
use std::collections::hash_map::Entry;
use std::str::FromStr;

use super::{MigrationError, Upgrade};
use crate::common::policy_store::formats::v2::trusted_issuers::{
    CustomTokenConfig, JwtTokenConfig, KeySourceDoc, TokenTypeConfig, TrustedIssuersDoc,
};
use crate::common::policy_store::formats::{PolicyStoreWarning, v1, v2};
use crate::common::policy_store::iss_from_configuration;
use crate::common::policy_store::token_types::{EntityTypeName, IssuerName, sanitize_issuer_name};

/// Where v1 kept each kind of issuer, for naming files in messages.
const TRUSTED_ISSUERS_DIR: &str = "trusted-issuers";
const CUSTOM_ISSUERS_DIR: &str = "custom-issuers";

/// Migrates a whole v1 store to v2.
impl Upgrade for v1::PolicyStoreDoc {
    type Next = v2::PolicyStoreDoc;

    fn upgrade(
        self,
        warnings: &mut Vec<PolicyStoreWarning>,
    ) -> Result<v2::PolicyStoreDoc, MigrationError> {
        let mut entries = Entries::default();

        let mut trusted_issuers = self.trusted_issuers;
        trusted_issuers.sort_by(|a, b| a.filename.cmp(&b.filename));
        for parsed in trusted_issuers {
            migrate_trusted_issuer(parsed, &mut entries, warnings)?;
        }

        let mut custom_issuers = self.custom_issuers;
        custom_issuers.sort_by(|a, b| a.filename.cmp(&b.filename));
        for parsed in custom_issuers {
            migrate_custom_issuer(parsed, &mut entries, warnings)?;
        }

        Ok(v2::PolicyStoreDoc {
            metadata: migrate_metadata(self.metadata),
            trusted_issuers: TrustedIssuersDoc(entries.entries),
            content: self.content,
            ignored_dirs: Vec::new(),
        })
    }
}

/// The merged entries, and the file each entity type came from.
#[derive(Default)]
struct Entries {
    entries: Vec<(EntityTypeName, TokenTypeConfig)>,
    files: HashMap<EntityTypeName, String>,
}

impl Entries {
    /// Adds an entry, rejecting an entity type another file already declares:
    /// the two cannot be merged without picking one silently.
    fn insert(
        &mut self,
        file: &str,
        entity_type: &str,
        config: TokenTypeConfig,
    ) -> Result<(), MigrationError> {
        let entity_type = EntityTypeName::from_str(entity_type).map_err(|e| {
            MigrationError::InvalidEntityType {
                file: file.to_string(),
                entity_type: entity_type.to_string(),
                reason: e.to_string(),
            }
        })?;
        match self.files.entry(entity_type.clone()) {
            Entry::Occupied(first) => Err(MigrationError::DuplicateEntityType {
                entity_type: entity_type.to_string(),
                file_a: first.get().clone(),
                file_b: file.to_string(),
            }),
            Entry::Vacant(slot) => {
                slot.insert(file.to_string());
                self.entries.push((entity_type, config));
                Ok(())
            },
        }
    }
}

fn migrate_metadata(doc: v1::metadata::MetadataDoc) -> v2::metadata::MetadataDoc {
    let info = doc.policy_store;
    v2::metadata::MetadataDoc {
        policy_store_spec_version: v2::SPEC_VERSION,
        cedar_version: doc.cedar_version,
        policy_store: v2::metadata::PolicyStoreInfoDoc {
            id: info.id,
            name: info.name,
            description: info.description,
            version: info.version,
            created_date: info.created_date,
            updated_date: info.updated_date,
        },
    }
}

fn migrate_trusted_issuer(
    parsed: v1::trusted_issuer::ParsedIssuer,
    entries: &mut Entries,
    warnings: &mut Vec<PolicyStoreWarning>,
) -> Result<(), MigrationError> {
    let file = format!("{TRUSTED_ISSUERS_DIR}/{}", parsed.filename);
    let doc = parsed.issuer;

    let issuer =
        IssuerName::from_str(&doc.name).map_err(|_| MigrationError::InvalidIssuerName {
            file: file.clone(),
            name: doc.name.clone(),
        })?;
    let configuration = doc.oidc_endpoint;
    let iss = iss_from_configuration(&configuration).map_err(|source| {
        MigrationError::InvalidConfigurationEndpoint {
            file: file.clone(),
            source,
        }
    })?;
    if iss.path() != "/" {
        warnings.push(PolicyStoreWarning::IssPathKept {
            file: file.clone(),
            iss: iss.to_string(),
        });
    }

    let mut tokens: Vec<v1::trusted_issuer::TokenMetadataDoc> = doc
        .token_metadata
        .into_values()
        // An untrusted entry was never validated, so it is not carried over.
        .filter(|token| token.trusted)
        .collect();
    tokens.sort_by(|a, b| a.entity_type_name.cmp(&b.entity_type_name));
    for token in tokens {
        let config = TokenTypeConfig::Jwt(Box::new(JwtTokenConfig {
            issuer: issuer.clone(),
            configuration_endpoints: KeySourceDoc::Discovery {
                configuration: configuration.clone(),
                iss: iss.clone(),
            },
            token_id: token.token_id,
            required_claims: token.required_claims,
        }));
        entries.insert(&file, &token.entity_type_name, config)?;
    }
    Ok(())
}

fn migrate_custom_issuer(
    parsed: v1::custom_issuer::ParsedCustomIssuer,
    entries: &mut Entries,
    warnings: &mut Vec<PolicyStoreWarning>,
) -> Result<(), MigrationError> {
    let file = format!("{CUSTOM_ISSUERS_DIR}/{}", parsed.filename);
    // The sanitized id was the v1 `context.tokens` prefix; keeping it as the
    // issuer keeps every key.
    let issuer = IssuerName::from_str(&sanitize_issuer_name(&parsed.id)).map_err(|_| {
        MigrationError::InvalidIssuerName {
            file: file.clone(),
            name: parsed.id.clone(),
        }
    })?;

    let mut tokens: Vec<(String, v1::custom_issuer::CustomTokenDoc)> =
        parsed.meta.tokens_mappings.into_iter().collect();
    tokens.sort_by(|a, b| a.0.cmp(&b.0));
    for (entity_type, token) in tokens {
        if token.required {
            warnings.push(PolicyStoreWarning::RequiredIgnored {
                file: file.clone(),
                entity_type: entity_type.clone(),
            });
        }
        let config = TokenTypeConfig::Custom(CustomTokenConfig {
            issuer: issuer.clone(),
            required_claims: token.required_claims,
        });
        entries.insert(&file, &entity_type, config)?;
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use std::collections::HashSet;

    use url::Url;

    use super::*;
    use crate::common::policy_store::formats::StoreContent;
    use crate::common::policy_store::formats::v1::custom_issuer::{
        CustomIssuerDoc, CustomTokenDoc, ParsedCustomIssuer,
    };
    use crate::common::policy_store::formats::v1::trusted_issuer::{
        ParsedIssuer, TokenMetadataDoc, TrustedIssuerDoc,
    };

    fn token(entity_type: &str, trusted: bool) -> TokenMetadataDoc {
        TokenMetadataDoc {
            trusted,
            entity_type_name: entity_type.to_string(),
            token_id: "sid".to_string(),
            required_claims: HashSet::from(["iss".to_string()]),
        }
    }

    fn jwt_file(
        filename: &str,
        name: &str,
        endpoint: &str,
        tokens: Vec<(&str, TokenMetadataDoc)>,
    ) -> ParsedIssuer {
        ParsedIssuer {
            id: "ignored".to_string(),
            issuer: TrustedIssuerDoc {
                name: name.to_string(),
                description: "dropped".to_string(),
                oidc_endpoint: Url::parse(endpoint).expect("valid url"),
                token_metadata: tokens
                    .into_iter()
                    .map(|(key, token)| (key.to_string(), token))
                    .collect(),
            },
            filename: filename.to_string(),
        }
    }

    fn custom_file(filename: &str, id: &str, tokens: &[(&str, bool)]) -> ParsedCustomIssuer {
        ParsedCustomIssuer {
            id: id.to_string(),
            meta: CustomIssuerDoc {
                tokens_mappings: tokens
                    .iter()
                    .map(|(entity_type, required)| {
                        (
                            (*entity_type).to_string(),
                            CustomTokenDoc {
                                required: *required,
                                required_claims: HashSet::from(["sub".to_string()]),
                            },
                        )
                    })
                    .collect(),
            },
            filename: filename.to_string(),
        }
    }

    fn v1_store(
        trusted_issuers: Vec<ParsedIssuer>,
        custom_issuers: Vec<ParsedCustomIssuer>,
    ) -> v1::PolicyStoreDoc {
        v1::PolicyStoreDoc {
            metadata: v1::metadata::MetadataDoc {
                policy_store_spec_version: v1::SPEC_VERSION,
                cedar_version: "4.4.0".to_string(),
                policy_store: v1::metadata::PolicyStoreInfoDoc {
                    id: "abc123def456".to_string(),
                    name: "Store".to_string(),
                    description: None,
                    version: "1.0.0".to_string(),
                    created_date: None,
                    updated_date: None,
                },
            },
            trusted_issuers,
            custom_issuers,
            content: StoreContent {
                schema: None,
                schema_source_exists: false,
                policies: vec![],
                templates: vec![],
                entities: vec![],
            },
        }
    }

    const GLUU: &str = "https://idp.gluu.test/.well-known/openid-configuration";

    fn migrate(
        doc: v1::PolicyStoreDoc,
    ) -> (
        Result<v2::PolicyStoreDoc, MigrationError>,
        Vec<PolicyStoreWarning>,
    ) {
        let mut warnings = Vec::new();
        let result = doc.upgrade(&mut warnings);
        (result, warnings)
    }

    /// The entry migrated for `entity_type`.
    fn entry<'a>(doc: &'a v2::PolicyStoreDoc, entity_type: &str) -> &'a TokenTypeConfig {
        &doc.trusted_issuers
            .0
            .iter()
            .find(|(key, _)| key.as_str() == entity_type)
            .unwrap_or_else(|| panic!("{entity_type} should be migrated"))
            .1
    }

    #[test]
    fn merges_both_directories_into_one_map() {
        let (doc, warnings) = migrate(v1_store(
            vec![jwt_file(
                "gluu.json",
                "Gluu",
                GLUU,
                vec![("access_token", token("Jans::Access_token", true))],
            )],
            vec![custom_file(
                "acme.json",
                "Ac-me",
                &[("Acme::Api_key", false)],
            )],
        ));
        let doc = doc.expect("a consistent v1 store should migrate");

        assert!(
            warnings.is_empty(),
            "nothing should be reported: {warnings:?}"
        );
        assert_eq!(
            doc.metadata.policy_store_spec_version,
            v2::SPEC_VERSION,
            "metadata should be stamped with v2"
        );
        assert_eq!(
            entry(&doc, "Jans::Access_token"),
            &TokenTypeConfig::Jwt(Box::new(JwtTokenConfig {
                issuer: IssuerName::from_str("Gluu").expect("valid name"),
                configuration_endpoints: KeySourceDoc::Discovery {
                    configuration: Url::parse(GLUU).expect("valid url"),
                    iss: Url::parse("https://idp.gluu.test").expect("valid url"),
                },
                token_id: "sid".to_string(),
                required_claims: HashSet::from(["iss".to_string()]),
            })),
            "a JWT token keeps its issuer name, endpoint, token_id and required_claims"
        );
        assert_eq!(
            entry(&doc, "Acme::Api_key"),
            &TokenTypeConfig::Custom(CustomTokenConfig {
                issuer: IssuerName::from_str("ac_me").expect("valid name"),
                required_claims: HashSet::from(["sub".to_string()]),
            }),
            "a custom issuer is named by its sanitized id, the v1 key prefix"
        );
    }

    #[test]
    fn drops_untrusted_tokens() {
        let (doc, _) = migrate(v1_store(
            vec![jwt_file(
                "gluu.json",
                "Gluu",
                GLUU,
                vec![
                    ("access_token", token("Jans::Access_token", true)),
                    ("id_token", token("Jans::Id_token", false)),
                ],
            )],
            vec![],
        ));
        let doc = doc.expect("the store should migrate");
        assert_eq!(
            doc.trusted_issuers
                .0
                .iter()
                .map(|(key, _)| key.as_str())
                .collect::<Vec<_>>(),
            vec!["Jans::Access_token"],
            "a `trusted: false` token should not be migrated"
        );
    }

    #[test]
    fn reports_required_custom_tokens() {
        let (doc, warnings) = migrate(v1_store(
            vec![],
            vec![custom_file("acme.json", "acme", &[("Acme::Api_key", true)])],
        ));
        doc.expect("the store should migrate");
        assert_eq!(
            warnings,
            vec![PolicyStoreWarning::RequiredIgnored {
                file: "custom-issuers/acme.json".to_string(),
                entity_type: "Acme::Api_key".to_string(),
            }]
        );
    }

    #[test]
    fn reports_iss_that_keeps_a_path() {
        let (doc, warnings) = migrate(v1_store(
            vec![jwt_file(
                "kc.json",
                "Kc",
                "https://kc.test/realms/a/.well-known/openid-configuration",
                vec![("access_token", token("Kc::Access_token", true))],
            )],
            vec![],
        ));
        doc.expect("the store should migrate");
        assert_eq!(
            warnings,
            vec![PolicyStoreWarning::IssPathKept {
                file: "trusted-issuers/kc.json".to_string(),
                iss: "https://kc.test/realms/a".to_string(),
            }]
        );
    }

    #[test]
    fn rejects_issuer_name_that_is_not_one_identifier() {
        for name in ["Acme::Corp", "Gluu Server"] {
            let (doc, _) = migrate(v1_store(
                vec![jwt_file(
                    "gluu.json",
                    name,
                    GLUU,
                    vec![("access_token", token("Jans::Access_token", true))],
                )],
                vec![],
            ));
            let err = doc.expect_err("the name cannot become a v2 issuer");
            assert!(
                matches!(&err, MigrationError::InvalidIssuerName { file, name: n }
                    if file == "trusted-issuers/gluu.json" && n == name),
                "expected InvalidIssuerName for {name:?}, got: {err:?}"
            );
        }
    }

    #[test]
    fn rejects_endpoint_iss_cannot_be_derived_from() {
        let (doc, _) = migrate(v1_store(
            vec![jwt_file(
                "gluu.json",
                "Gluu",
                "https://idp.gluu.test/auth",
                vec![("access_token", token("Jans::Access_token", true))],
            )],
            vec![],
        ));
        let err = doc.expect_err("the iss cannot be derived");
        assert!(
            matches!(&err, MigrationError::InvalidConfigurationEndpoint { file, .. }
                if file == "trusted-issuers/gluu.json"),
            "expected InvalidConfigurationEndpoint, got: {err:?}"
        );
    }

    #[test]
    fn rejects_invalid_entity_type() {
        let (doc, _) = migrate(v1_store(
            vec![],
            vec![custom_file("acme.json", "acme", &[("Acme Key", false)])],
        ));
        let err = doc.expect_err("an invalid entity type cannot be a v2 key");
        assert!(
            matches!(&err, MigrationError::InvalidEntityType { file, entity_type, .. }
                if file == "custom-issuers/acme.json" && entity_type == "Acme Key"),
            "expected InvalidEntityType, got: {err:?}"
        );
    }

    #[test]
    fn rejects_entity_type_declared_in_two_files() {
        let (doc, _) = migrate(v1_store(
            vec![jwt_file(
                "gluu.json",
                "Gluu",
                GLUU,
                vec![("access_token", token("Jans::Access_token", true))],
            )],
            vec![custom_file(
                "acme.json",
                "acme",
                &[("Jans::Access_token", false)],
            )],
        ));
        let err = doc.expect_err("two files cannot both declare an entity type");
        assert!(
            matches!(&err, MigrationError::DuplicateEntityType { entity_type, file_a, file_b }
                if entity_type == "Jans::Access_token"
                    && file_a == "trusted-issuers/gluu.json"
                    && file_b == "custom-issuers/acme.json"),
            "expected DuplicateEntityType naming both files, got: {err:?}"
        );
    }
}
