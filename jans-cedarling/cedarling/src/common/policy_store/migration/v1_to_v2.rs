// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Migration from format v1 to v2.
//!
//! v2 only adds the required `policy_store_spec_version`, so every component is
//! copied field by field. Nothing is lost: v1 already ignores unknown keys at
//! parse time, exactly as v2 does.

use crate::common::policy_store::formats::{v1, v2};

/// Migrates a whole v1 store to v2.
pub(crate) fn migrate(doc: v1::PolicyStoreDoc) -> v2::PolicyStoreDoc {
    v2::PolicyStoreDoc {
        metadata: migrate_metadata(doc.metadata),
        trusted_issuers: doc
            .trusted_issuers
            .into_iter()
            .map(migrate_trusted_issuer)
            .collect(),
        custom_issuers: doc
            .custom_issuers
            .into_iter()
            .map(migrate_custom_issuer)
            .collect(),
        content: doc.content,
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
) -> v2::trusted_issuer::ParsedIssuer {
    let issuer = parsed.issuer;
    v2::trusted_issuer::ParsedIssuer {
        id: parsed.id,
        issuer: v2::trusted_issuer::TrustedIssuerDoc {
            name: issuer.name,
            description: issuer.description,
            oidc_endpoint: issuer.oidc_endpoint,
            token_metadata: issuer
                .token_metadata
                .into_iter()
                .map(|(key, token)| (key, migrate_token_metadata(token)))
                .collect(),
        },
        filename: parsed.filename,
    }
}

fn migrate_token_metadata(
    token: v1::trusted_issuer::TokenMetadataDoc,
) -> v2::trusted_issuer::TokenMetadataDoc {
    v2::trusted_issuer::TokenMetadataDoc {
        trusted: token.trusted,
        entity_type_name: token.entity_type_name,
        token_id: token.token_id,
        required_claims: token.required_claims,
    }
}

fn migrate_custom_issuer(
    parsed: v1::custom_issuer::ParsedCustomIssuer,
) -> v2::custom_issuer::ParsedCustomIssuer {
    v2::custom_issuer::ParsedCustomIssuer {
        id: parsed.id,
        meta: v2::custom_issuer::CustomIssuerDoc {
            tokens_mappings: parsed
                .meta
                .tokens_mappings
                .into_iter()
                .map(|(mapping, token)| {
                    let token = v2::custom_issuer::CustomTokenDoc {
                        required: token.required,
                        required_claims: token.required_claims,
                    };
                    (mapping, token)
                })
                .collect(),
        },
        filename: parsed.filename,
    }
}

#[cfg(test)]
mod tests {
    use std::collections::{HashMap, HashSet};

    use chrono::{DateTime, Utc};
    use url::Url;

    use super::*;
    use crate::common::policy_store::formats::StoreContent;

    fn timestamp(value: &str) -> DateTime<Utc> {
        DateTime::parse_from_rfc3339(value)
            .expect("valid timestamp")
            .with_timezone(&Utc)
    }

    fn v1_metadata() -> v1::metadata::MetadataDoc {
        v1::metadata::MetadataDoc {
            cedar_version: "4.4.0".to_string(),
            policy_store: v1::metadata::PolicyStoreInfoDoc {
                id: "abc123def456".to_string(),
                name: "Store".to_string(),
                description: Some("desc".to_string()),
                version: "1.2.3".to_string(),
                created_date: Some(timestamp("2024-01-01T00:00:00Z")),
                updated_date: Some(timestamp("2024-01-02T00:00:00Z")),
            },
        }
    }

    fn v1_issuer() -> v1::trusted_issuer::ParsedIssuer {
        v1::trusted_issuer::ParsedIssuer {
            id: "jans".to_string(),
            issuer: v1::trusted_issuer::TrustedIssuerDoc {
                name: "Jans".to_string(),
                description: "Jans server".to_string(),
                oidc_endpoint: Url::parse("https://jans.test/.well-known/openid-configuration")
                    .expect("valid url"),
                token_metadata: HashMap::from([(
                    "access_token".to_string(),
                    v1::trusted_issuer::TokenMetadataDoc {
                        trusted: false,
                        entity_type_name: "Jans::Access_token".to_string(),
                        token_id: "sid".to_string(),
                        required_claims: HashSet::from(["iss".to_string()]),
                    },
                )]),
            },
            filename: "jans.json".to_string(),
        }
    }

    fn v1_custom_issuer() -> v1::custom_issuer::ParsedCustomIssuer {
        v1::custom_issuer::ParsedCustomIssuer {
            id: "acme".to_string(),
            meta: v1::custom_issuer::CustomIssuerDoc {
                tokens_mappings: HashMap::from([(
                    "Acme::Custom".to_string(),
                    v1::custom_issuer::CustomTokenDoc {
                        required: true,
                        required_claims: HashSet::from(["sub".to_string()]),
                    },
                )]),
            },
            filename: "acme.json".to_string(),
        }
    }

    #[test]
    fn metadata_gains_spec_version_and_keeps_fields() {
        assert_eq!(
            migrate_metadata(v1_metadata()),
            v2::metadata::MetadataDoc {
                policy_store_spec_version: v2::SPEC_VERSION,
                cedar_version: "4.4.0".to_string(),
                policy_store: v2::metadata::PolicyStoreInfoDoc {
                    id: "abc123def456".to_string(),
                    name: "Store".to_string(),
                    description: Some("desc".to_string()),
                    version: "1.2.3".to_string(),
                    created_date: Some(timestamp("2024-01-01T00:00:00Z")),
                    updated_date: Some(timestamp("2024-01-02T00:00:00Z")),
                },
            },
            "metadata should be copied and stamped with the v2 spec version"
        );
    }

    #[test]
    fn trusted_issuer_keeps_every_field() {
        let migrated = migrate_trusted_issuer(v1_issuer());
        assert_eq!(migrated.id, "jans", "id mismatch");
        assert_eq!(migrated.filename, "jans.json", "filename mismatch");
        assert_eq!(
            migrated.issuer,
            v2::trusted_issuer::TrustedIssuerDoc {
                name: "Jans".to_string(),
                description: "Jans server".to_string(),
                oidc_endpoint: Url::parse("https://jans.test/.well-known/openid-configuration")
                    .expect("valid url"),
                token_metadata: HashMap::from([(
                    "access_token".to_string(),
                    v2::trusted_issuer::TokenMetadataDoc {
                        trusted: false,
                        entity_type_name: "Jans::Access_token".to_string(),
                        token_id: "sid".to_string(),
                        required_claims: HashSet::from(["iss".to_string()]),
                    },
                )]),
            },
            "every trusted issuer and token metadata field should be copied"
        );
    }

    #[test]
    fn custom_issuer_keeps_every_field() {
        let migrated = migrate_custom_issuer(v1_custom_issuer());
        assert_eq!(migrated.id, "acme", "id mismatch");
        assert_eq!(migrated.filename, "acme.json", "filename mismatch");
        assert_eq!(
            migrated.meta,
            v2::custom_issuer::CustomIssuerDoc {
                tokens_mappings: HashMap::from([(
                    "Acme::Custom".to_string(),
                    v2::custom_issuer::CustomTokenDoc {
                        required: true,
                        required_claims: HashSet::from(["sub".to_string()]),
                    },
                )]),
            },
            "every custom issuer field should be copied"
        );
    }

    #[test]
    fn whole_store_migrates_every_component() {
        let migrated = migrate(v1::PolicyStoreDoc {
            metadata: v1_metadata(),
            trusted_issuers: vec![v1_issuer()],
            custom_issuers: vec![v1_custom_issuer()],
            content: StoreContent {
                schema: None,
                schema_source_exists: false,
                policies: vec![],
                templates: vec![],
                entities: vec![],
            },
        });

        assert_eq!(
            migrated.metadata,
            migrate_metadata(v1_metadata()),
            "metadata mismatch"
        );
        assert_eq!(
            migrated.trusted_issuers.len(),
            1,
            "trusted issuers should all be migrated"
        );
        assert_eq!(
            migrated.custom_issuers.len(),
            1,
            "custom issuers should all be migrated"
        );
        assert!(
            migrated.content.policies.is_empty() && !migrated.content.schema_source_exists,
            "version-agnostic content should pass through unchanged"
        );
    }
}
