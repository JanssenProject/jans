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
use std::collections::hash_map::Entry;
use std::collections::{HashMap, HashSet};
use std::hash::{Hash, Hasher};
use std::str::FromStr;
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};

use smol_str::{SmolStr, ToSmolStr};
use url::Url;

use crate::common::issuer_utils::IssClaim;

#[cfg(test)]
use super::CustomIssuerMetadata;
use super::{CustomTokenMetadata, KeySource, TokenEntityMetadata, TrustedIssuer};

/// An issuer's identity: the namespace of its `TrustedIssuer` entity and,
/// lowercased, the prefix of its `context.tokens` keys.
///
/// Keeps the parsed `{name}::TrustedIssuer` type, so each token's `iss` entity
/// is built without parsing it again.
#[derive(Debug, Clone, derive_more::Display)]
#[display("{name}")]
pub(crate) struct IssuerName {
    name: SmolStr,
    trusted_issuer_type: cedar_policy::EntityTypeName,
}

impl IssuerName {
    /// Parses a name that a test writes as a valid one.
    #[cfg(test)]
    pub(crate) fn for_test(name: &str) -> Self {
        name.parse()
            .expect("test issuer name should be a single Cedar identifier")
    }

    /// The `context.tokens` key prefix of this issuer's tokens.
    pub(crate) fn key_prefix(&self) -> String {
        self.name.to_lowercase()
    }

    /// `{name}::TrustedIssuer`, the type of this issuer's entity.
    pub(crate) fn trusted_issuer_type(&self) -> &cedar_policy::EntityTypeName {
        &self.trusted_issuer_type
    }
}

impl FromStr for IssuerName {
    type Err = InvalidIssuerName;

    /// Accepts a single Cedar identifier, `^[A-Za-z_][A-Za-z0-9_]*$`, that Cedar
    /// also accepts as a namespace, so reserved words such as `if` are refused.
    fn from_str(name: &str) -> Result<Self, Self::Err> {
        let mut chars = name.chars();
        let identifier = chars
            .next()
            .is_some_and(|c| c.is_ascii_alphabetic() || c == '_')
            && chars.all(|c| c.is_ascii_alphanumeric() || c == '_');
        if !identifier {
            return Err(InvalidIssuerName::NotIdentifier {
                name: name.to_string(),
            });
        }
        // The issuer is the namespace of its `TrustedIssuer` entity type.
        let trusted_issuer_type = cedar_policy::EntityTypeName::from_str(&format!(
            "{name}::TrustedIssuer"
        ))
        .map_err(|source| InvalidIssuerName::NotNamespace {
            name: name.to_string(),
            source: Box::new(source),
        })?;
        Ok(Self {
            name: name.to_smolstr(),
            trusted_issuer_type,
        })
    }
}

// Equal, ordered and hashed as the name alone: the parsed type follows from it,
// and `Borrow<str>` needs the hash of the name.
impl PartialEq for IssuerName {
    fn eq(&self, other: &Self) -> bool {
        self.name == other.name
    }
}

impl Eq for IssuerName {}

impl PartialOrd for IssuerName {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}

impl Ord for IssuerName {
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        self.name.cmp(&other.name)
    }
}

impl Hash for IssuerName {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.name.hash(state);
    }
}

/// Why a string is not an [`IssuerName`].
#[derive(Debug, thiserror::Error)]
pub enum InvalidIssuerName {
    #[error("issuer '{name}' must be a single Cedar identifier, ^[A-Za-z_][A-Za-z0-9_]*$")]
    NotIdentifier { name: String },
    #[error("issuer '{name}' must be a single Cedar identifier usable as a namespace: {source}")]
    NotNamespace {
        name: String,
        #[source]
        source: Box<cedar_policy::ParseErrors>,
    },
}

// Hashes as the inner string, so an issuer can be looked up by its name as text.
impl Borrow<str> for IssuerName {
    fn borrow(&self) -> &str {
        &self.name
    }
}

/// A Cedar entity type name, written exactly as a request `mapping` names it.
///
/// Keeps Cedar's parsed form next to the text, so building an entity or matching
/// policies never parses it again.
#[derive(Debug, Clone, derive_more::Display)]
#[display("{name}")]
pub(crate) struct EntityTypeName {
    name: SmolStr,
    cedar: cedar_policy::EntityTypeName,
}

impl EntityTypeName {
    /// Parses an entity type that a test writes as a valid one.
    #[cfg(test)]
    pub(crate) fn for_test(entity_type: &str) -> Self {
        entity_type
            .parse()
            .expect("test entity type should be a canonical Cedar entity type")
    }

    pub(crate) fn as_str(&self) -> &str {
        &self.name
    }

    pub(crate) fn cedar(&self) -> &cedar_policy::EntityTypeName {
        &self.cedar
    }
}

impl FromStr for EntityTypeName {
    type Err = InvalidEntityType;

    /// Accepts what Cedar accepts, which includes only its canonical spelling, so
    /// that config and request agree on what a valid entity type is.
    fn from_str(entity_type: &str) -> Result<Self, Self::Err> {
        let cedar = cedar_policy::EntityTypeName::from_str(entity_type).map_err(|source| {
            InvalidEntityType {
                entity_type: entity_type.to_string(),
                source: Box::new(source),
            }
        })?;
        Ok(Self {
            name: entity_type.to_smolstr(),
            cedar,
        })
    }
}

// Equal and hashed as the text alone: the parsed form follows from it, and
// `Borrow<str>` needs the hash of the text.
impl PartialEq for EntityTypeName {
    fn eq(&self, other: &Self) -> bool {
        self.name == other.name
    }
}

impl Eq for EntityTypeName {}

impl Hash for EntityTypeName {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.name.hash(state);
    }
}

/// A string that is not an [`EntityTypeName`].
#[derive(Debug, thiserror::Error)]
#[error("'{entity_type}' is not a valid Cedar entity type: {source}")]
pub struct InvalidEntityType {
    pub(crate) entity_type: String,
    #[source]
    source: Box<cedar_policy::ParseErrors>,
}

// Hashes as the text, so a request `mapping` can look up an entry without being
// parsed.
impl Borrow<str> for EntityTypeName {
    fn borrow(&self) -> &str {
        &self.name
    }
}

/// The `context.tokens` key holding the number of tokens in the request.
pub(crate) const TOTAL_TOKEN_COUNT_KEY: &str = "total_token_count";

/// `context.tokens` keys Cedarling fills itself, which no token type may derive.
const RESERVED_TOKEN_KEYS: &[&str] = &[TOTAL_TOKEN_COUNT_KEY];

/// A token type's key under `context.tokens`, derived once when the store loads
/// so that every request puts the token where policies expect it.
#[derive(Debug, Clone, PartialEq, Eq, Hash, derive_more::Display)]
pub(crate) struct ContextKey(SmolStr);

impl ContextKey {
    /// `{lowercase(issuer)}_{simplified type}`.
    fn derive(issuer: &IssuerName, entity_type: &EntityTypeName) -> Self {
        Self(
            format!(
                "{}_{}",
                issuer.key_prefix(),
                simplify_token_type(entity_type.as_str())
            )
            .to_smolstr(),
        )
    }

    /// A key a test writes as already derived.
    #[cfg(test)]
    pub(crate) fn for_test(key: &str) -> Self {
        Self(key.to_smolstr())
    }

    pub(crate) fn as_str(&self) -> &str {
        &self.0
    }

    fn is_reserved(&self) -> bool {
        RESERVED_TOKEN_KEYS.contains(&self.as_str())
    }
}

// Hashes as the inner string, so a token entity can be looked up by its key as text.
impl Borrow<str> for ContextKey {
    fn borrow(&self) -> &str {
        &self.0
    }
}

/// Simplify token type for Cedar compatibility
fn simplify_token_type(mapping: &str) -> String {
    // Split by namespace separator and use the last part
    // Keep underscores in token type names as specified in design
    mapping.split("::").last().unwrap_or(mapping).to_lowercase()
}

/// How a token type is validated.
#[derive(Debug, Clone, PartialEq)]
pub(crate) enum TokenTypeKind {
    /// Validated as a JWT of `issuer`, which every token type of that issuer shares.
    Jwt {
        issuer: Arc<TrustedIssuer>,
        metadata: TokenEntityMetadata,
    },
    Custom(CustomTokenMetadata),
}

/// One accepted token type.
#[derive(Debug, Clone, PartialEq)]
pub(crate) struct TokenType {
    pub(crate) issuer: IssuerName,
    pub(crate) context_key: ContextKey,
    pub(crate) kind: TokenTypeKind,
}

/// The validated token types of a policy store, keyed by entity type.
#[derive(Debug, Clone, Default)]
pub(crate) struct TokenTypes {
    tokens: HashMap<EntityTypeName, Arc<TokenType>>,
    /// JWT issuers by name; their token types are in `tokens`.
    jwt_issuers: HashMap<IssuerName, Arc<TrustedIssuer>>,
    /// Unique to each build, so a result cached under one store is never served
    /// under another, even to a request that started before a refresh.
    generation: u64,
}

/// The generation the next built [`TokenTypes`] gets.
static NEXT_GENERATION: AtomicU64 = AtomicU64::new(1);

// Tests compare what two stores accept, not which build they come from.
#[cfg(test)]
impl PartialEq for TokenTypes {
    fn eq(&self, other: &Self) -> bool {
        self.tokens == other.tokens && self.jwt_issuers == other.jwt_issuers
    }
}

/// A conflict in the token-type configuration, found before anything loads.
#[derive(Debug, Clone, PartialEq, thiserror::Error)]
pub enum TokenTypesError {
    #[error("entity type '{entity_type}' is declared by issuers '{issuer_a}' and '{issuer_b}'")]
    DuplicateEntityType {
        entity_type: String,
        issuer_a: String,
        issuer_b: String,
    },
    #[error(
        "issuer '{issuer}' is described differently by '{entity_type_a}' and \
         '{entity_type_b}' ({difference}): one issuer has exactly one iss and one key source"
    )]
    IssuerConflict {
        issuer: String,
        entity_type_a: String,
        entity_type_b: String,
        /// What differs, with both values.
        difference: String,
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
    #[error(
        "issuers '{issuer_a}' and '{issuer_b}' declare iss '{iss}' with different JWKS \
         ('{jwks_a}' and '{jwks_b}')"
    )]
    ConflictingJwks {
        iss: String,
        issuer_a: String,
        jwks_a: String,
        issuer_b: String,
        jwks_b: String,
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

/// Collects token types one at a time and checks each against those already
/// added, so a conflict is reported for the entries that cause it.
///
/// Entries are checked in the order they are added; callers add them in a
/// stable order so the same conflict is always reported the same way.
#[derive(Default)]
pub(crate) struct TokenTypesBuilder {
    /// Every token type added so far, JWT and custom, by entity type.
    tokens: HashMap<EntityTypeName, Arc<TokenType>>,
    /// JWT issuers by name; their token types are in `tokens`.
    jwt_issuers: HashMap<IssuerName, Arc<TrustedIssuer>>,
    /// Custom issuers by name, each with one of its entity types to name in a
    /// kind conflict.
    custom_issuers: HashMap<IssuerName, EntityTypeName>,
    /// The entity type that derives each `context.tokens` key.
    context_keys: HashMap<ContextKey, EntityTypeName>,
}

impl TokenTypesBuilder {
    /// Adds a JWT token type of `issuer`. Entries of one issuer must agree on
    /// `iss` and key source; `iss` is compared normalized, as tokens are matched
    /// against it.
    pub(crate) fn add_jwt(
        &mut self,
        entity_type: EntityTypeName,
        issuer: IssuerName,
        iss: Url,
        key_source: KeySource,
        token: TokenEntityMetadata,
    ) -> Result<(), TokenTypesError> {
        if let Some(custom_entity_type) = self.custom_issuers.get(&issuer) {
            return Err(TokenTypesError::MixedIssuerKinds {
                issuer: issuer.to_string(),
                jwt_entity_type: entity_type.to_string(),
                custom_entity_type: custom_entity_type.to_string(),
            });
        }

        let trusted_issuer = match self.jwt_issuers.entry(issuer.clone()) {
            Entry::Vacant(slot) => slot
                .insert(Arc::new(TrustedIssuer::new(
                    issuer.clone(),
                    iss,
                    key_source,
                )))
                .clone(),
            Entry::Occupied(existing) => {
                let existing = existing.get().clone();
                let difference = if *existing.iss_claim() != IssClaim::new(iss.as_str()) {
                    Some(format!(
                        "iss '{}' and '{}'",
                        existing.iss_claim(),
                        IssClaim::new(iss.as_str())
                    ))
                } else if existing.key_source != key_source {
                    Some(format!(
                        "key source {} and {}",
                        describe_key_source(&existing.key_source),
                        describe_key_source(&key_source)
                    ))
                } else {
                    None
                };
                if let Some(difference) = difference {
                    return Err(TokenTypesError::IssuerConflict {
                        issuer: issuer.to_string(),
                        entity_type_a: self.any_jwt_entity_type(&issuer),
                        entity_type_b: entity_type.to_string(),
                        difference,
                    });
                }
                existing
            },
        };

        let kind = TokenTypeKind::Jwt {
            issuer: trusted_issuer,
            metadata: token,
        };
        self.insert_token(entity_type, issuer, kind)
    }

    /// Adds a custom token type of `issuer`.
    pub(crate) fn add_custom(
        &mut self,
        entity_type: EntityTypeName,
        issuer: IssuerName,
        token: CustomTokenMetadata,
    ) -> Result<(), TokenTypesError> {
        if self.jwt_issuers.contains_key(&issuer) {
            return Err(TokenTypesError::MixedIssuerKinds {
                issuer: issuer.to_string(),
                jwt_entity_type: self.any_jwt_entity_type(&issuer),
                custom_entity_type: entity_type.to_string(),
            });
        }
        self.insert_token(
            entity_type.clone(),
            issuer.clone(),
            TokenTypeKind::Custom(token),
        )?;
        self.custom_issuers.entry(issuer).or_insert(entity_type);
        Ok(())
    }

    /// Runs the checks that need every entry, and returns the token types.
    pub(crate) fn finish(self) -> Result<TokenTypes, TokenTypesError> {
        self.check_static_jwks()?;
        Ok(TokenTypes {
            tokens: self.tokens,
            jwt_issuers: self.jwt_issuers,
            generation: NEXT_GENERATION.fetch_add(1, Ordering::Relaxed),
        })
    }

    fn insert_token(
        &mut self,
        entity_type: EntityTypeName,
        issuer: IssuerName,
        kind: TokenTypeKind,
    ) -> Result<(), TokenTypesError> {
        let context_key = ContextKey::derive(&issuer, &entity_type);
        if context_key.is_reserved() {
            return Err(TokenTypesError::ReservedContextKey {
                key: context_key.to_string(),
                entity_type: entity_type.to_string(),
            });
        }

        match self.tokens.entry(entity_type) {
            Entry::Occupied(existing) => Err(TokenTypesError::DuplicateEntityType {
                entity_type: existing.key().to_string(),
                issuer_a: existing.get().issuer.to_string(),
                issuer_b: issuer.to_string(),
            }),
            Entry::Vacant(slot) => {
                if let Some(owner) = self
                    .context_keys
                    .insert(context_key.clone(), slot.key().clone())
                {
                    return Err(TokenTypesError::DuplicateContextKey {
                        key: context_key.to_string(),
                        entity_type_a: owner.to_string(),
                        entity_type_b: slot.key().to_string(),
                    });
                }
                slot.insert(Arc::new(TokenType {
                    issuer,
                    context_key,
                    kind,
                }));
                Ok(())
            },
        }
    }

    /// Names one of `issuer`'s JWT entity types in an error, or `<none>`.
    fn any_jwt_entity_type(&self, issuer: &IssuerName) -> String {
        self.tokens
            .iter()
            .filter(|(_, token)| {
                &token.issuer == issuer && matches!(token.kind, TokenTypeKind::Jwt { .. })
            })
            .map(|(entity_type, _)| entity_type.as_str())
            .min()
            .map_or_else(|| "<none>".to_string(), ToString::to_string)
    }

    /// Rejects two static issuers with one `iss` but different JWKS, which would
    /// let one plant keys under the other's `iss`. Keys are stored per
    /// normalized `iss`, so that is what is compared.
    fn check_static_jwks(&self) -> Result<(), TokenTypesError> {
        let mut issuers: Vec<&Arc<TrustedIssuer>> = self.jwt_issuers.values().collect();
        issuers.sort_by(|a, b| a.name.cmp(&b.name));

        let mut jwks_by_iss: HashMap<&IssClaim, (&Url, &IssuerName)> = HashMap::new();
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
                            jwks_a: existing_jwks.to_string(),
                            issuer_b: issuer.name.to_string(),
                            jwks_b: jwks.to_string(),
                        });
                    }
                },
            }
        }
        Ok(())
    }
}

/// A JWT issuer with the token types it declares, keyed by entity type, as
/// tests describe one for [`TokenTypes::build`].
#[cfg(test)]
#[derive(Debug, Clone)]
pub(crate) struct TestJwtIssuer {
    pub(crate) issuer: TrustedIssuer,
    pub(crate) tokens: HashMap<String, TokenEntityMetadata>,
}

/// The `Jans` test issuer with access, id and userinfo tokens.
#[cfg(test)]
impl Default for TestJwtIssuer {
    fn default() -> Self {
        TrustedIssuer::default().with_tokens(HashMap::from([
            (
                "Jans::Access_token".into(),
                TokenEntityMetadata::access_token(),
            ),
            ("Jans::Id_token".into(), TokenEntityMetadata::id_token()),
            (
                "Jans::Userinfo_token".into(),
                TokenEntityMetadata::userinfo_token(),
            ),
        ]))
    }
}

impl TokenTypes {
    /// Builds token types from JWT issuers and custom issuers, in a stable
    /// order. Test shorthand for [`TokenTypesBuilder`].
    #[cfg(test)]
    pub(crate) fn build(
        jwt_issuers: impl IntoIterator<Item = TestJwtIssuer>,
        custom_issuers: impl IntoIterator<Item = (String, CustomIssuerMetadata)>,
    ) -> Result<Self, TokenTypesError> {
        let mut builder = TokenTypesBuilder::default();

        let mut jwt_issuers: Vec<TestJwtIssuer> = jwt_issuers.into_iter().collect();
        jwt_issuers.sort_by(|a, b| {
            (&a.issuer.name, a.issuer.iss()).cmp(&(&b.issuer.name, b.issuer.iss()))
        });
        for TestJwtIssuer { issuer, tokens } in jwt_issuers {
            let name = issuer.name.clone();
            let mut tokens: Vec<_> = tokens.into_iter().collect();
            tokens.sort_by(|a, b| a.0.cmp(&b.0));
            for (entity_type, token) in tokens {
                builder.add_jwt(
                    EntityTypeName::for_test(&entity_type),
                    name.clone(),
                    issuer.iss.clone(),
                    issuer.key_source.clone(),
                    token,
                )?;
            }
        }

        let mut custom_issuers: Vec<_> = custom_issuers.into_iter().collect();
        custom_issuers.sort_by(|a, b| a.0.cmp(&b.0));
        for (name, issuer) in custom_issuers {
            let name = IssuerName::for_test(&name);
            let mut tokens: Vec<_> = issuer.tokens_mappings.into_iter().collect();
            tokens.sort_by(|a, b| a.0.cmp(&b.0));
            for (entity_type, token) in tokens {
                builder.add_custom(EntityTypeName::for_test(&entity_type), name.clone(), token)?;
            }
        }

        builder.finish()
    }

    /// The token type a request `mapping` names.
    #[cfg(test)]
    pub(crate) fn get(&self, mapping: &str) -> Option<&TokenType> {
        self.tokens.get(mapping).map(AsRef::as_ref)
    }

    /// The entity type and token type a request `mapping` names, for a
    /// validated token to carry instead of being looked up again.
    pub(crate) fn resolve(&self, mapping: &str) -> Option<(&EntityTypeName, &Arc<TokenType>)> {
        self.tokens.get_key_value(mapping)
    }

    /// Which build these token types come from; see the field.
    pub(crate) fn generation(&self) -> u64 {
        self.generation
    }

    /// JWT issuers by name.
    pub(crate) fn jwt_issuers(&self) -> &HashMap<IssuerName, Arc<TrustedIssuer>> {
        &self.jwt_issuers
    }

    /// The JWT token types of the issuer named `issuer`, by entity type.
    pub(crate) fn jwt_tokens_of<'a>(
        &'a self,
        issuer: &'a IssuerName,
    ) -> impl Iterator<Item = (&'a EntityTypeName, &'a TokenEntityMetadata)> {
        self.tokens
            .iter()
            .filter_map(move |(entity_type, token)| match &token.kind {
                TokenTypeKind::Jwt { metadata, .. } if &token.issuer == issuer => {
                    Some((entity_type, metadata))
                },
                _ => None,
            })
    }

    /// Whether both have the same JWT issuers and JWT token types, so whatever
    /// was loaded for one, keys and validators, serves the other too.
    pub(crate) fn same_jwt_config(&self, other: &Self) -> bool {
        fn jwt_tokens(types: &TokenTypes) -> HashMap<&EntityTypeName, &Arc<TokenType>> {
            types
                .tokens
                .iter()
                .filter(|(_, token)| matches!(token.kind, TokenTypeKind::Jwt { .. }))
                .collect()
        }
        self.jwt_issuers == other.jwt_issuers && jwt_tokens(self) == jwt_tokens(other)
    }

    /// How many custom issuers declare at least one token type.
    pub(crate) fn custom_issuer_count(&self) -> usize {
        self.tokens
            .values()
            .filter(|token| matches!(token.kind, TokenTypeKind::Custom(_)))
            .map(|token| &token.issuer)
            .collect::<HashSet<_>>()
            .len()
    }
}

/// Names a key source and its URL in an error.
fn describe_key_source(key_source: &KeySource) -> String {
    match key_source {
        KeySource::Discovery { configuration } => format!("configuration '{configuration}'"),
        KeySource::Static { jwks } => format!("jwks '{jwks}'"),
    }
}

#[cfg(test)]
mod tests {
    use url::Url;

    use super::*;

    fn jwt(name: &str, configuration: &str, entity_types: &[&str]) -> TestJwtIssuer {
        TrustedIssuer::from_configuration(name, Url::parse(configuration).expect("valid url"))
            .expect("test configuration endpoint should derive an iss")
            .with_tokens(
                entity_types
                    .iter()
                    .map(|t| ((*t).to_string(), TokenEntityMetadata::builder().build()))
                    .collect(),
            )
    }

    fn static_jwt(name: &str, iss: &str, jwks: &str, entity_type: &str) -> TestJwtIssuer {
        TrustedIssuer::new(
            IssuerName::for_test(name),
            Url::parse(iss).expect("valid url"),
            KeySource::Static {
                jwks: Url::parse(jwks).expect("valid url"),
            },
        )
        .with_tokens(HashMap::from([(
            entity_type.to_string(),
            TokenEntityMetadata::builder().build(),
        )]))
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
                token.issuer.to_string(),
                issuer,
                "{entity_type} issuer mismatch"
            );
            assert_eq!(
                token.context_key.as_str(),
                key,
                "{entity_type} key mismatch"
            );
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
    fn counts_custom_issuers_once_each() {
        let types = TokenTypes::build(
            [jwt("Acme", ACME, &["Acme::Access_token"])],
            [
                custom("Dolphin", &["Dolphin::Api_key", "Dolphin::Session"]),
                custom("Orca", &["Orca::Api_key"]),
            ],
        )
        .expect("a consistent configuration should build");
        assert_eq!(types.jwt_issuers().len(), 1, "one JWT issuer");
        assert_eq!(types.custom_issuer_count(), 2, "two custom issuers");
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
            types.jwt_tokens_of(&IssuerName::for_test("Acme")).count(),
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
            let err = IssuerName::from_str(name)
                .expect_err("a name that is not one identifier must be rejected");
            assert!(
                matches!(&err, InvalidIssuerName::NotIdentifier { name: n } if n == name),
                "{name:?} should be rejected as not one identifier, got {err:?}"
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
            let err =
                IssuerName::from_str(name).expect_err("a reserved issuer name must be rejected");
            assert!(
                matches!(&err, InvalidIssuerName::NotNamespace { name: n, .. } if n == name),
                "{name:?} should be rejected as a Cedar namespace, got {err:?}"
            );
        }
    }

    #[test]
    fn rejects_invalid_entity_type() {
        for entity_type in ["Acme Token", "Acme::", "Acme :: Token"] {
            let err = EntityTypeName::from_str(entity_type)
                .expect_err("an invalid entity type must be rejected");
            assert_eq!(
                err.entity_type, entity_type,
                "{entity_type:?} should be rejected as an entity type"
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
                difference: "iss 'https://idp.acme.com' and 'https://idp.beta.com'".into(),
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
        assert_eq!(
            err,
            TokenTypesError::IssuerConflict {
                issuer: "Acme".into(),
                entity_type_a: "Acme::Access_token".into(),
                entity_type_b: "Acme::Id_token".into(),
                difference: "key source configuration \
                             'https://idp.acme.com/.well-known/openid-configuration' and jwks \
                             'https://idp.acme.com/keys'"
                    .into(),
            }
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
                jwks_a: "https://evil.example/keys".into(),
                issuer_b: "Partner".into(),
                jwks_b: "https://auth.partner.io/keys".into(),
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
                jwks_a: "https://evil.example/keys".into(),
                issuer_b: "Partner".into(),
                jwks_b: "https://idp.x/keys".into(),
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
            types
                .jwt_tokens_of(&IssuerName::for_test("Partner"))
                .count(),
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
                difference: "iss 'https://idp.x/realms/a' and 'https://idp.x/realms/b'".into(),
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
