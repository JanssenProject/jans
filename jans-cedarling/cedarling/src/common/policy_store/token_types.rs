// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The token types a policy store accepts, validated as a whole.
//!
//! Every check that spans more than one token type lives here, so a store that
//! loads has unique entity types, one consistent description per issuer and
//! unique `context.tokens` keys.

use std::borrow::Borrow;
use std::collections::HashMap;
use std::collections::hash_map::Entry;
use std::str::FromStr;

use smol_str::{SmolStr, ToSmolStr};

use crate::authz::RESERVED_TOKEN_KEYS;
use crate::common::issuer_utils::IssClaim;

use super::{
    CustomIssuerMetadata, CustomTokenMetadata, KeySource, TokenEntityMetadata, TrustedIssuer,
};

/// An issuer's identity: the namespace of its `TrustedIssuer` entity and,
/// lowercased, the prefix of its `context.tokens` keys.
#[derive(Debug, Clone, PartialEq, Eq, Hash, derive_more::Display)]
pub(crate) struct IssuerName(SmolStr);

impl IssuerName {
    pub(crate) fn as_str(&self) -> &str {
        &self.0
    }

    /// The `context.tokens` key prefix of this issuer's tokens.
    pub(crate) fn key_prefix(&self) -> String {
        self.0.to_lowercase()
    }
}

impl FromStr for IssuerName {
    type Err = TokenTypesError;

    /// Accepts a single Cedar identifier, `^[A-Za-z_][A-Za-z0-9_]*$`, that Cedar
    /// also accepts as a namespace, so reserved words such as `if` are refused.
    fn from_str(name: &str) -> Result<Self, Self::Err> {
        let invalid = |reason: String| TokenTypesError::InvalidIssuerName {
            name: name.to_string(),
            reason,
        };
        let mut chars = name.chars();
        let identifier = chars
            .next()
            .is_some_and(|c| c.is_ascii_alphabetic() || c == '_')
            && chars.all(|c| c.is_ascii_alphanumeric() || c == '_');
        if !identifier {
            return Err(invalid("expected ^[A-Za-z_][A-Za-z0-9_]*$".to_string()));
        }
        // The issuer is the namespace of its `TrustedIssuer` entity type.
        cedar_policy::EntityTypeName::from_str(&format!("{name}::TrustedIssuer"))
            .map_err(|e| invalid(e.to_string()))?;
        Ok(Self(name.to_smolstr()))
    }
}

/// A Cedar entity type name, written exactly as a request `mapping` names it.
#[derive(Debug, Clone, PartialEq, Eq, Hash, derive_more::Display)]
pub(crate) struct EntityTypeName(SmolStr);

impl EntityTypeName {
    pub(crate) fn as_str(&self) -> &str {
        &self.0
    }
}

impl FromStr for EntityTypeName {
    type Err = TokenTypesError;

    /// Accepts what Cedar accepts, in its canonical spelling, so that config and
    /// request agree on what a valid entity type is.
    fn from_str(entity_type: &str) -> Result<Self, Self::Err> {
        let invalid = |reason: String| TokenTypesError::InvalidEntityType {
            entity_type: entity_type.to_string(),
            reason,
        };
        let parsed = cedar_policy::EntityTypeName::from_str(entity_type)
            .map_err(|e| invalid(e.to_string()))?;
        if parsed.to_string() != entity_type {
            return Err(invalid(format!("must be written as '{parsed}'")));
        }
        Ok(Self(entity_type.to_smolstr()))
    }
}

// Hashes as the inner string, so a request `mapping` can look up an entry
// without being parsed.
impl Borrow<str> for EntityTypeName {
    fn borrow(&self) -> &str {
        &self.0
    }
}

/// The v1 rule that turns an issuer name into its `context.tokens` prefix,
/// kept for stores that still name issuers that way.
pub(crate) fn sanitize_issuer_name(name: &str) -> String {
    name.replace(['.', ' ', '-'], "_").to_lowercase()
}

/// Simplify token type for Cedar compatibility
pub(crate) fn simplify_token_type(mapping: &str) -> String {
    // Split by namespace separator and use the last part
    // Keep underscores in token type names as specified in design
    mapping.split("::").last().unwrap_or(mapping).to_lowercase()
}

/// How a token type is validated.
#[derive(Debug, Clone, PartialEq)]
pub(crate) enum TokenTypeKind {
    Jwt(TokenEntityMetadata),
    Custom(CustomTokenMetadata),
}

/// One accepted token type.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct TokenType {
    pub(crate) issuer: IssuerName,
    /// Key under `context.tokens`: `{lowercase(issuer)}_{simplified type}`.
    pub(crate) context_key: SmolStr,
    pub(crate) kind: TokenTypeKind,
}

/// The validated token types of a policy store, keyed by entity type.
#[derive(Debug, Clone, Default)]
#[cfg_attr(test, derive(PartialEq))]
pub(crate) struct TokenTypes {
    tokens: HashMap<EntityTypeName, TokenType>,
    /// JWT issuers by name, each holding every token type it issues.
    jwt_issuers: HashMap<String, TrustedIssuer>,
}

/// A conflict in the token-type configuration, found before anything loads.
#[derive(Debug, Clone, PartialEq, thiserror::Error)]
pub enum TokenTypesError {
    #[error("issuer '{name}' must be a single Cedar identifier usable as a namespace: {reason}")]
    InvalidIssuerName { name: String, reason: String },
    #[error("'{entity_type}' is not a valid Cedar entity type: {reason}")]
    InvalidEntityType { entity_type: String, reason: String },
    #[error("entity type '{entity_type}' is declared by issuers '{issuer_a}' and '{issuer_b}'")]
    DuplicateEntityType {
        entity_type: String,
        issuer_a: String,
        issuer_b: String,
    },
    #[error(
        "issuer '{issuer}' is described differently by '{entity_type_a}' and \
         '{entity_type_b}': one issuer has exactly one iss and one key source"
    )]
    IssuerConflict {
        issuer: String,
        entity_type_a: String,
        entity_type_b: String,
    },
    #[error(
        "issuer '{issuer}' has JWT token '{jwt_entity_type}' and custom token \
         '{custom_entity_type}': one issuer has exactly one kind"
    )]
    MixedIssuerKinds {
        issuer: String,
        jwt_entity_type: String,
        custom_entity_type: String,
    },
    #[error("issuers '{issuer_a}' and '{issuer_b}' declare iss '{iss}' with different JWKS")]
    ConflictingJwks {
        iss: String,
        issuer_a: String,
        issuer_b: String,
    },
    #[error(
        "entity types '{entity_type_a}' and '{entity_type_b}' share the context.tokens key \
         '{key}'"
    )]
    DuplicateContextKey {
        key: String,
        entity_type_a: String,
        entity_type_b: String,
    },
    #[error(
        "entity type '{entity_type}' derives the context.tokens key '{key}', which Cedarling \
         reserves"
    )]
    ReservedContextKey { key: String, entity_type: String },
}

impl TokenTypes {
    /// Validates and indexes every JWT issuer and custom issuer of a store.
    ///
    /// JWT issuers with the same name are one issuer and are merged; they must
    /// agree on `iss` and key source.
    pub(crate) fn build(
        jwt_issuers: impl IntoIterator<Item = TrustedIssuer>,
        custom_issuers: impl IntoIterator<Item = (String, CustomIssuerMetadata)>,
    ) -> Result<Self, TokenTypesError> {
        let mut types = Self::default();
        let mut context_keys: HashMap<SmolStr, EntityTypeName> = HashMap::new();

        // Sorted so the same conflict is always reported the same way.
        let mut jwt_issuers: Vec<TrustedIssuer> = jwt_issuers.into_iter().collect();
        jwt_issuers.sort_by(|a, b| (&a.name, a.iss()).cmp(&(&b.name, b.iss())));
        for issuer in jwt_issuers {
            let name = IssuerName::from_str(&issuer.name)?;
            let mut tokens: Vec<(String, TokenEntityMetadata)> = issuer
                .token_metadata
                .iter()
                .map(|(entity_type, token)| (entity_type.clone(), token.clone()))
                .collect();
            tokens.sort_by(|a, b| a.0.cmp(&b.0));
            types.merge_jwt_issuer(issuer)?;
            for (entity_type, token) in tokens {
                types.insert_token(
                    &mut context_keys,
                    &entity_type,
                    &name,
                    TokenTypeKind::Jwt(token),
                )?;
            }
        }
        types.check_static_jwks()?;

        let mut custom_issuers: Vec<(String, CustomIssuerMetadata)> =
            custom_issuers.into_iter().collect();
        custom_issuers.sort_by(|a, b| a.0.cmp(&b.0));
        for (name, issuer) in custom_issuers {
            let name = IssuerName::from_str(&name)?;
            let mut tokens: Vec<(String, CustomTokenMetadata)> =
                issuer.tokens_mappings.into_iter().collect();
            tokens.sort_by(|a, b| a.0.cmp(&b.0));
            for (entity_type, token) in tokens {
                if let Some(jwt) = types.jwt_issuers.get(name.as_str()) {
                    return Err(TokenTypesError::MixedIssuerKinds {
                        issuer: name.to_string(),
                        jwt_entity_type: any_entity_type(jwt),
                        custom_entity_type: entity_type,
                    });
                }
                types.insert_token(
                    &mut context_keys,
                    &entity_type,
                    &name,
                    TokenTypeKind::Custom(token),
                )?;
            }
        }

        Ok(types)
    }

    fn insert_token(
        &mut self,
        context_keys: &mut HashMap<SmolStr, EntityTypeName>,
        entity_type: &str,
        issuer: &IssuerName,
        kind: TokenTypeKind,
    ) -> Result<(), TokenTypesError> {
        let entity_type = EntityTypeName::from_str(entity_type)?;
        let context_key = format!(
            "{}_{}",
            issuer.key_prefix(),
            simplify_token_type(entity_type.as_str())
        )
        .to_smolstr();
        if RESERVED_TOKEN_KEYS.contains(&context_key.as_str()) {
            return Err(TokenTypesError::ReservedContextKey {
                key: context_key.to_string(),
                entity_type: entity_type.to_string(),
            });
        }

        match self.tokens.entry(entity_type.clone()) {
            Entry::Occupied(existing) => Err(TokenTypesError::DuplicateEntityType {
                entity_type: entity_type.to_string(),
                issuer_a: existing.get().issuer.to_string(),
                issuer_b: issuer.to_string(),
            }),
            Entry::Vacant(slot) => {
                if let Some(owner) = context_keys.insert(context_key.clone(), entity_type.clone()) {
                    return Err(TokenTypesError::DuplicateContextKey {
                        key: context_key.to_string(),
                        entity_type_a: owner.to_string(),
                        entity_type_b: entity_type.to_string(),
                    });
                }
                slot.insert(TokenType {
                    issuer: issuer.clone(),
                    context_key,
                    kind,
                });
                Ok(())
            },
        }
    }

    /// Adds `issuer`'s token types to the issuer of the same name, which must
    /// carry the same `iss` and key source. `iss` is compared normalized, as
    /// tokens are matched against it.
    fn merge_jwt_issuer(&mut self, issuer: TrustedIssuer) -> Result<(), TokenTypesError> {
        match self.jwt_issuers.entry(issuer.name.clone()) {
            Entry::Vacant(slot) => {
                slot.insert(issuer);
                Ok(())
            },
            Entry::Occupied(mut existing) => {
                let existing = existing.get_mut();
                if existing.iss_claim() != issuer.iss_claim()
                    || existing.key_source != issuer.key_source
                {
                    return Err(TokenTypesError::IssuerConflict {
                        issuer: issuer.name.clone(),
                        entity_type_a: any_entity_type(existing),
                        entity_type_b: any_entity_type(&issuer),
                    });
                }
                existing.token_metadata.extend(issuer.token_metadata);
                Ok(())
            },
        }
    }

    /// Rejects two static issuers with one `iss` but different JWKS, which would
    /// let one plant keys under the other's `iss`. Keys are stored per
    /// normalized `iss`, so that is what is compared.
    fn check_static_jwks(&self) -> Result<(), TokenTypesError> {
        let mut issuers: Vec<&TrustedIssuer> = self.jwt_issuers.values().collect();
        issuers.sort_by(|a, b| a.name.cmp(&b.name));

        let mut jwks_by_iss: HashMap<IssClaim, (&url::Url, &str)> = HashMap::new();
        for issuer in issuers {
            let KeySource::Static { jwks } = &issuer.key_source else {
                continue;
            };
            match jwks_by_iss.entry(issuer.iss_claim()) {
                Entry::Vacant(slot) => {
                    slot.insert((jwks, &issuer.name));
                },
                Entry::Occupied(existing) => {
                    let (existing_jwks, existing_name) = existing.get();
                    if *existing_jwks != jwks {
                        return Err(TokenTypesError::ConflictingJwks {
                            iss: issuer.iss_claim().to_string(),
                            issuer_a: (*existing_name).to_string(),
                            issuer_b: issuer.name.clone(),
                        });
                    }
                },
            }
        }
        Ok(())
    }

    /// The token type a request `mapping` names.
    pub(crate) fn get(&self, mapping: &str) -> Option<&TokenType> {
        self.tokens.get(mapping)
    }

    /// Every token type, keyed by entity type.
    pub(crate) fn iter(&self) -> impl Iterator<Item = (&EntityTypeName, &TokenType)> {
        self.tokens.iter()
    }

    /// JWT issuers by name.
    pub(crate) fn jwt_issuers(&self) -> &HashMap<String, TrustedIssuer> {
        &self.jwt_issuers
    }
}

/// Names one of an issuer's entity types in an error, or `<none>`.
fn any_entity_type(issuer: &TrustedIssuer) -> String {
    issuer
        .token_metadata
        .keys()
        .min()
        .map_or_else(|| "<none>".to_string(), Clone::clone)
}

#[cfg(test)]
mod tests {
    use url::Url;

    use super::*;

    fn jwt(name: &str, configuration: &str, entity_types: &[&str]) -> TrustedIssuer {
        TrustedIssuer::from_configuration(
            name.to_string(),
            String::new(),
            Url::parse(configuration).expect("valid url"),
            entity_types
                .iter()
                .map(|t| ((*t).to_string(), TokenEntityMetadata::builder().build()))
                .collect(),
        )
        .expect("test configuration endpoint should derive an iss")
    }

    fn static_jwt(name: &str, iss: &str, jwks: &str, entity_type: &str) -> TrustedIssuer {
        TrustedIssuer::new(
            name.to_string(),
            String::new(),
            Url::parse(iss).expect("valid url"),
            KeySource::Static {
                jwks: Url::parse(jwks).expect("valid url"),
            },
            HashMap::from([(
                entity_type.to_string(),
                TokenEntityMetadata::builder().build(),
            )]),
        )
    }

    fn custom(name: &str, entity_types: &[&str]) -> (String, CustomIssuerMetadata) {
        (
            name.to_string(),
            CustomIssuerMetadata {
                tokens_mappings: entity_types
                    .iter()
                    .map(|t| ((*t).to_string(), CustomTokenMetadata::default()))
                    .collect(),
            },
        )
    }

    const ACME: &str = "https://idp.acme.com/.well-known/openid-configuration";
    const BETA: &str = "https://idp.beta.com/.well-known/openid-configuration";

    #[test]
    fn indexes_every_token_type_with_its_key() {
        let types = TokenTypes::build(
            [jwt("Acme", ACME, &["Acme::Access_token", "Acme::Id_token"])],
            [custom("Dolphin", &["Dolphin::Dolphin_token"])],
        )
        .expect("a consistent configuration should build");

        for (entity_type, issuer, key) in [
            ("Acme::Access_token", "Acme", "acme_access_token"),
            ("Acme::Id_token", "Acme", "acme_id_token"),
            ("Dolphin::Dolphin_token", "Dolphin", "dolphin_dolphin_token"),
        ] {
            let token = types
                .get(entity_type)
                .expect("token type should be indexed");
            assert_eq!(
                token.issuer.as_str(),
                issuer,
                "{entity_type} issuer mismatch"
            );
            assert_eq!(token.context_key, key, "{entity_type} key mismatch");
        }
        assert!(
            matches!(
                types.get("Dolphin::Dolphin_token").map(|t| &t.kind),
                Some(TokenTypeKind::Custom(_))
            ),
            "a custom token type should keep its kind"
        );
        assert!(
            types.get("Nope::Token").is_none(),
            "an unknown mapping should not resolve"
        );
    }

    #[test]
    fn simplify_token_type_keeps_the_last_segment_lowercased() {
        for (mapping, simplified) in [
            ("Jans::Access_Token", "access_token"),
            ("Acme::DolphinToken", "dolphintoken"),
            ("A::B::Employee_Token", "employee_token"),
            ("SimpleToken", "simpletoken"),
        ] {
            assert_eq!(simplify_token_type(mapping), simplified, "{mapping}");
        }
    }

    #[test]
    fn same_issuer_across_entries_is_merged() {
        let types = TokenTypes::build(
            [
                jwt("Acme", ACME, &["Acme::Access_token"]),
                jwt("Acme", ACME, &["Acme::Id_token"]),
            ],
            [],
        )
        .expect("entries describing one issuer the same way should merge");

        assert_eq!(types.jwt_issuers().len(), 1, "one issuer should remain");
        assert_eq!(
            types.jwt_issuers()["Acme"].token_metadata.len(),
            2,
            "the merged issuer should carry both token types"
        );
    }

    #[test]
    fn issuers_sharing_one_iss_are_accepted() {
        let types = TokenTypes::build(
            [
                jwt("Acme", ACME, &["Acme::Access_token"]),
                jwt("Beta", ACME, &["Beta::Access_token"]),
            ],
            [],
        )
        .expect("two issuers may share one iss and its keys");
        assert_eq!(types.jwt_issuers().len(), 2, "both issuers should be kept");
    }

    #[test]
    fn rejects_issuer_name_that_is_not_one_identifier() {
        for name in ["Acme Incorporated", "Acme::Corp", "1acme", ""] {
            let err = TokenTypes::build([jwt(name, ACME, &["Acme::Access_token"])], [])
                .expect_err("a name that is not one identifier must be rejected");
            assert!(
                matches!(&err, TokenTypesError::InvalidIssuerName { name: n, .. } if n == name),
                "{name:?} should be rejected by name, got {err:?}"
            );
        }
    }

    /// The issuer becomes a Cedar namespace, so a word Cedar reserves is refused
    /// when the store loads rather than when entities are first built.
    #[test]
    fn rejects_issuer_name_cedar_reserves() {
        for name in [
            "if", "true", "in", "is", "has", "like", "then", "else", "__cedar",
        ] {
            let jwt_err = TokenTypes::build([jwt(name, ACME, &["Acme::Access_token"])], [])
                .expect_err("a reserved JWT issuer name must be rejected");
            let custom_err = TokenTypes::build([], [custom(name, &["Acme::Api_key"])])
                .expect_err("a reserved custom issuer name must be rejected");
            for err in [jwt_err, custom_err] {
                assert!(
                    matches!(&err, TokenTypesError::InvalidIssuerName { name: n, .. } if n == name),
                    "{name:?} should be rejected as an issuer name, got {err:?}"
                );
            }
        }
    }

    #[test]
    fn rejects_invalid_entity_type() {
        for entity_type in ["Acme Token", "Acme::", "Acme :: Token"] {
            let err = TokenTypes::build([], [custom("acme", &[entity_type])])
                .expect_err("an invalid entity type must be rejected");
            assert!(
                matches!(&err, TokenTypesError::InvalidEntityType { entity_type: t, .. } if t == entity_type),
                "{entity_type:?} should be rejected as an entity type, got {err:?}"
            );
        }
    }

    #[test]
    fn rejects_entity_type_declared_by_two_issuers() {
        let err = TokenTypes::build(
            [
                jwt("Acme", ACME, &["Jans::Access_token"]),
                jwt("Beta", BETA, &["Jans::Access_token"]),
            ],
            [],
        )
        .expect_err("one entity type must have one issuer");
        assert_eq!(
            err,
            TokenTypesError::DuplicateEntityType {
                entity_type: "Jans::Access_token".into(),
                issuer_a: "Acme".into(),
                issuer_b: "Beta".into(),
            }
        );
    }

    #[test]
    fn rejects_entity_type_declared_as_jwt_and_custom() {
        let err = TokenTypes::build(
            [jwt("Acme", ACME, &["Jans::Access_token"])],
            [custom("keys", &["Jans::Access_token"])],
        )
        .expect_err("a custom type must not shadow a JWT type");
        assert_eq!(
            err,
            TokenTypesError::DuplicateEntityType {
                entity_type: "Jans::Access_token".into(),
                issuer_a: "Acme".into(),
                issuer_b: "keys".into(),
            }
        );
    }

    #[test]
    fn rejects_one_issuer_with_two_iss() {
        let err = TokenTypes::build(
            [
                jwt("Acme", ACME, &["Acme::Access_token"]),
                jwt("Acme", BETA, &["Acme::Id_token"]),
            ],
            [],
        )
        .expect_err("one issuer must have one iss");
        assert_eq!(
            err,
            TokenTypesError::IssuerConflict {
                issuer: "Acme".into(),
                entity_type_a: "Acme::Access_token".into(),
                entity_type_b: "Acme::Id_token".into(),
            }
        );
    }

    #[test]
    fn rejects_one_issuer_with_two_key_sources() {
        let err = TokenTypes::build(
            [
                jwt("Acme", ACME, &["Acme::Access_token"]),
                static_jwt(
                    "Acme",
                    "https://idp.acme.com",
                    "https://idp.acme.com/keys",
                    "Acme::Id_token",
                ),
            ],
            [],
        )
        .expect_err("one issuer must have one key source");
        assert!(
            matches!(&err, TokenTypesError::IssuerConflict { issuer, .. } if issuer == "Acme"),
            "expected IssuerConflict for Acme, got {err:?}"
        );
    }

    #[test]
    fn rejects_jwt_and_custom_under_one_issuer() {
        let err = TokenTypes::build(
            [jwt("Acme", ACME, &["Acme::Access_token"])],
            [custom("Acme", &["Acme::Api_key"])],
        )
        .expect_err("one issuer must have one kind");
        assert_eq!(
            err,
            TokenTypesError::MixedIssuerKinds {
                issuer: "Acme".into(),
                jwt_entity_type: "Acme::Access_token".into(),
                custom_entity_type: "Acme::Api_key".into(),
            }
        );
    }

    #[test]
    fn rejects_static_issuers_sharing_iss_with_different_jwks() {
        let iss = "https://auth.partner.io";
        let err = TokenTypes::build(
            [
                static_jwt(
                    "Partner",
                    iss,
                    "https://auth.partner.io/keys",
                    "Partner::Access_token",
                ),
                static_jwt(
                    "Other",
                    iss,
                    "https://evil.example/keys",
                    "Other::Access_token",
                ),
            ],
            [],
        )
        .expect_err("two key sets for one iss must be rejected");
        assert_eq!(
            err,
            TokenTypesError::ConflictingJwks {
                iss: "https://auth.partner.io".into(),
                issuer_a: "Other".into(),
                issuer_b: "Partner".into(),
            }
        );
    }

    /// A trailing slash does not make a different `iss`: tokens are matched
    /// against the normalized value, so the checks compare that too.
    #[test]
    fn rejects_static_issuers_whose_iss_differs_only_by_trailing_slash() {
        let err = TokenTypes::build(
            [
                static_jwt(
                    "Partner",
                    "https://idp.x/realms/a",
                    "https://idp.x/keys",
                    "Partner::Access_token",
                ),
                static_jwt(
                    "Other",
                    "https://idp.x/realms/a/",
                    "https://evil.example/keys",
                    "Other::Access_token",
                ),
            ],
            [],
        )
        .expect_err("a trailing slash must not hide a second key set for one iss");
        assert_eq!(
            err,
            TokenTypesError::ConflictingJwks {
                iss: "https://idp.x/realms/a".into(),
                issuer_a: "Other".into(),
                issuer_b: "Partner".into(),
            }
        );
    }

    #[test]
    fn merges_entries_whose_iss_differs_only_by_trailing_slash() {
        let types = TokenTypes::build(
            [
                static_jwt(
                    "Partner",
                    "https://idp.x/realms/a",
                    "https://idp.x/keys",
                    "Partner::Access_token",
                ),
                static_jwt(
                    "Partner",
                    "https://idp.x/realms/a/",
                    "https://idp.x/keys",
                    "Partner::Id_token",
                ),
            ],
            [],
        )
        .expect("entries naming one iss with and without a trailing slash are one issuer");
        assert_eq!(
            types.jwt_issuers()["Partner"].token_metadata.len(),
            2,
            "both token types should belong to the one issuer"
        );
    }

    #[test]
    fn rejects_one_issuer_with_two_iss_differing_by_path() {
        let err = TokenTypes::build(
            [
                static_jwt(
                    "Partner",
                    "https://idp.x/realms/a",
                    "https://idp.x/keys",
                    "Partner::Access_token",
                ),
                static_jwt(
                    "Partner",
                    "https://idp.x/realms/b/",
                    "https://idp.x/keys",
                    "Partner::Id_token",
                ),
            ],
            [],
        )
        .expect_err("one issuer must have one iss");
        assert_eq!(
            err,
            TokenTypesError::IssuerConflict {
                issuer: "Partner".into(),
                entity_type_a: "Partner::Access_token".into(),
                entity_type_b: "Partner::Id_token".into(),
            }
        );
    }

    #[test]
    fn accepts_static_issuers_sharing_iss_and_jwks() {
        let (iss, jwks) = ("https://auth.partner.io", "https://auth.partner.io/keys");
        TokenTypes::build(
            [
                static_jwt("Partner", iss, jwks, "Partner::Access_token"),
                static_jwt("Other", iss, jwks, "Other::Access_token"),
            ],
            [],
        )
        .expect("issuers sharing an iss and its keys should be accepted");
    }

    #[test]
    fn rejects_colliding_context_keys() {
        let err = TokenTypes::build(
            [
                jwt("Acme", ACME, &["Acme::Access_token"]),
                jwt("ACME", BETA, &["Other::Access_token"]),
            ],
            [],
        )
        .expect_err("two token types must not share a context.tokens key");
        assert_eq!(
            err,
            TokenTypesError::DuplicateContextKey {
                key: "acme_access_token".into(),
                entity_type_a: "Other::Access_token".into(),
                entity_type_b: "Acme::Access_token".into(),
            }
        );
    }

    /// `total_token_count` is filled by Cedarling after the token keys, so a token
    /// type deriving it would be overwritten and `has` would always hold.
    #[test]
    fn rejects_token_type_deriving_a_reserved_context_key() {
        let err = TokenTypes::build([], [custom("total", &["A::Token_count"])])
            .expect_err("a token type must not derive a reserved context.tokens key");
        assert_eq!(
            err,
            TokenTypesError::ReservedContextKey {
                key: "total_token_count".into(),
                entity_type: "A::Token_count".into(),
            }
        );
    }
}
