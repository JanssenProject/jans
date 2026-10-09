// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Migration from format v0 to v1.
//!
//! v1 only adds the required `policy_store_spec_version`, so every component is
//! copied field by field. Nothing is lost: v0 already ignores unknown keys at
//! parse time, exactly as v1 does.

use crate::common::policy_store::formats::{v0, v1};

/// Migrates a whole v0 store to v1.
pub(crate) fn migrate(doc: v0::PolicyStoreDoc) -> v1::PolicyStoreDoc {
    v1::PolicyStoreDoc {
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

fn migrate_metadata(doc: v0::metadata::MetadataDoc) -> v1::metadata::MetadataDoc {
    let info = doc.policy_store;
    v1::metadata::MetadataDoc {
        policy_store_spec_version: v1::SPEC_VERSION,
        cedar_version: doc.cedar_version,
        policy_store: v1::metadata::PolicyStoreInfoDoc {
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
    parsed: v0::trusted_issuer::ParsedIssuer,
) -> v1::trusted_issuer::ParsedIssuer {
    let issuer = parsed.issuer;
    v1::trusted_issuer::ParsedIssuer {
        id: parsed.id,
        issuer: v1::trusted_issuer::TrustedIssuerDoc {
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
    token: v0::trusted_issuer::TokenMetadataDoc,
) -> v1::trusted_issuer::TokenMetadataDoc {
    v1::trusted_issuer::TokenMetadataDoc {
        trusted: token.trusted,
        entity_type_name: token.entity_type_name,
        token_id: token.token_id,
        required_claims: token.required_claims,
    }
}

fn migrate_custom_issuer(
    parsed: v0::custom_issuer::ParsedCustomIssuer,
) -> v1::custom_issuer::ParsedCustomIssuer {
    v1::custom_issuer::ParsedCustomIssuer {
        id: parsed.id,
        meta: v1::custom_issuer::CustomIssuerDoc {
            tokens_mappings: parsed
                .meta
                .tokens_mappings
                .into_iter()
                .map(|(mapping, token)| {
                    let token = v1::custom_issuer::CustomTokenDoc {
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

    fn v0_metadata() -> v0::metadata::MetadataDoc {
        v0::metadata::MetadataDoc {
            cedar_version: "4.4.0".to_string(),
            policy_store: v0::metadata::PolicyStoreInfoDoc {
                id: "abc123def456".to_string(),
                name: "Store".to_string(),
                description: Some("desc".to_string()),
                version: "1.2.3".to_string(),
                created_date: Some(timestamp("2024-01-01T00:00:00Z")),
                updated_date: Some(timestamp("2024-01-02T00:00:00Z")),
            },
        }
    }

    fn v0_issuer() -> v0::trusted_issuer::ParsedIssuer {
        v0::trusted_issuer::ParsedIssuer {
            id: "jans".to_string(),
            issuer: v0::trusted_issuer::TrustedIssuerDoc {
                name: "Jans".to_string(),
                description: "Jans server".to_string(),
                oidc_endpoint: Url::parse("https://jans.test/.well-known/openid-configuration")
                    .expect("valid url"),
                token_metadata: HashMap::from([(
                    "access_token".to_string(),
                    v0::trusted_issuer::TokenMetadataDoc {
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

    fn v0_custom_issuer() -> v0::custom_issuer::ParsedCustomIssuer {
        v0::custom_issuer::ParsedCustomIssuer {
            id: "acme".to_string(),
            meta: v0::custom_issuer::CustomIssuerDoc {
                tokens_mappings: HashMap::from([(
                    "Acme::Custom".to_string(),
                    v0::custom_issuer::CustomTokenDoc {
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
            migrate_metadata(v0_metadata()),
            v1::metadata::MetadataDoc {
                policy_store_spec_version: v1::SPEC_VERSION,
                cedar_version: "4.4.0".to_string(),
                policy_store: v1::metadata::PolicyStoreInfoDoc {
                    id: "abc123def456".to_string(),
                    name: "Store".to_string(),
                    description: Some("desc".to_string()),
                    version: "1.2.3".to_string(),
                    created_date: Some(timestamp("2024-01-01T00:00:00Z")),
                    updated_date: Some(timestamp("2024-01-02T00:00:00Z")),
                },
            },
            "metadata should be copied and stamped with the v1 spec version"
        );
    }

    #[test]
    fn trusted_issuer_keeps_every_field() {
        let migrated = migrate_trusted_issuer(v0_issuer());
        assert_eq!(migrated.id, "jans", "id mismatch");
        assert_eq!(migrated.filename, "jans.json", "filename mismatch");
        assert_eq!(
            migrated.issuer,
            v1::trusted_issuer::TrustedIssuerDoc {
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
            "every trusted issuer and token metadata field should be copied"
        );
    }

    #[test]
    fn custom_issuer_keeps_every_field() {
        let migrated = migrate_custom_issuer(v0_custom_issuer());
        assert_eq!(migrated.id, "acme", "id mismatch");
        assert_eq!(migrated.filename, "acme.json", "filename mismatch");
        assert_eq!(
            migrated.meta,
            v1::custom_issuer::CustomIssuerDoc {
                tokens_mappings: HashMap::from([(
                    "Acme::Custom".to_string(),
                    v1::custom_issuer::CustomTokenDoc {
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
        let migrated = migrate(v0::PolicyStoreDoc {
            metadata: v0_metadata(),
            trusted_issuers: vec![v0_issuer()],
            custom_issuers: vec![v0_custom_issuer()],
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
            migrate_metadata(v0_metadata()),
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
