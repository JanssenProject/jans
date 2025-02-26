// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod build_expr;
mod build_iss_entity;
mod build_resource_entity;
mod build_token_entities;
mod build_user_entity;
mod build_workload_entity;
mod built_entities;

use super::AuthorizeEntitiesData;
use crate::common::policy_store::{ClaimMappings, TrustedIssuer};
use crate::jwt::{Token, TokenClaimTypeError};
use crate::{AuthorizationConfig, ResourceData};
use build_iss_entity::build_iss_entity;
pub(crate) use built_entities::BuiltEntities;
use cedar_policy::{Entity, EntityUid, RestrictedExpression};
use derive_more::Display;
use serde_json::Value;
use smol_str::{SmolStr, ToSmolStr};
use std::collections::{HashMap, HashSet};
use std::fmt;
use std::net::IpAddr;
use std::str::FromStr;
use url::Origin;

const DEFAULT_WORKLOAD_ENTITY_NAME: &str = "Jans::Workload";
const DEFAULT_USER_ENTITY_NAME: &str = "Jans::User";
const DEFAULT_ISS_ENTITY_NAME: &str = "Jans::TrustedIssuer";
const DEFAULT_ACCESS_TKN_ENTITY_NAME: &str = "Jans::Access_token";
const DEFAULT_ID_TKN_ENTITY_NAME: &str = "Jans::id_token";
const DEFAULT_USERINFO_TKN_ENTITY_NAME: &str = "Jans::Userinfo_token";
const DEFAULT_ROLE_ENTITY_NAME: &str = "Jans::Role";

/// The names of the entities in the schema
///
/// Note that the entity names for the tokens can be found in the trusted issuer
/// struct under their respective token entity metadata. The entity names here
/// only belong to the entity names that could be set using the bootstrap
/// properties
#[derive(Debug)]
pub struct EntityNames {
    user: String,
    workload: String,
    role: String,
    iss: String,
}

impl From<&AuthorizationConfig> for EntityNames {
    fn from(config: &AuthorizationConfig) -> Self {
        Self {
            user: config
                .mapping_user
                .clone()
                .unwrap_or_else(|| DEFAULT_USER_ENTITY_NAME.to_string()),
            workload: config
                .mapping_workload
                .clone()
                .unwrap_or_else(|| DEFAULT_WORKLOAD_ENTITY_NAME.to_string()),
            role: config
                .mapping_role
                .clone()
                .unwrap_or_else(|| DEFAULT_ROLE_ENTITY_NAME.to_string()),
            // TODO: we should probably also add a default entity name for the
            // trusted issuer entity
            ..Default::default()
        }
    }
}

impl Default for EntityNames {
    fn default() -> Self {
        Self {
            user: DEFAULT_USER_ENTITY_NAME.to_string(),
            workload: DEFAULT_WORKLOAD_ENTITY_NAME.to_string(),
            role: DEFAULT_ROLE_ENTITY_NAME.to_string(),
            iss: DEFAULT_ISS_ENTITY_NAME.to_string(),
        }
    }
}

pub struct EntityBuilder {
    entity_names: EntityNames,
    build_workload: bool,
    build_user: bool,
    iss_entities: HashMap<Origin, Entity>,
}

impl EntityBuilder {
    // TODO: we can probably combine build_workload and build_user into a single HashSet of
    // the names of the entities to be built
    pub fn new(
        entity_names: EntityNames,
        build_workload: bool,
        build_user: bool,
        trusted_issuers: &HashMap<String, TrustedIssuer>,
    ) -> Self {
        let (ok, errs): (Vec<_>, Vec<_>) = trusted_issuers
            .iter()
            .map(|(iss_id, iss)| build_iss_entity(&entity_names.iss, iss_id, iss))
            .partition(|result| result.is_ok());

        if !errs.is_empty() {
            // TODO: gracefully handle errors
            panic!("error while initializing entity builder: {:#?}", errs);
        }

        let iss_entities = ok
            .into_iter()
            .flatten()
            .collect::<HashMap<Origin, Entity>>();

        Self {
            entity_names,
            build_workload,
            build_user,
            iss_entities,
        }
    }

    pub fn build_entities(
        &self,
        tokens: &HashMap<String, Token>,
        resource: &ResourceData,
    ) -> Result<AuthorizeEntitiesData, BuildEntityError> {
        let mut tkn_principal_mappings = TokenPrincipalMappings::default();

        let mut token_entities = HashMap::new();
        for (tkn_name, tkn) in tokens.iter() {
            let entity_name = tkn
                .iss
                .and_then(|iss| iss.tokens_metadata.get(tkn_name))
                .map(|metadata| metadata.entity_type_name.as_str())
                .or_else(|| default_tkn_entity_name(tkn_name));

            let entity_name = if let Some(entity_name) = entity_name {
                entity_name
            } else {
                continue;
            };

            let entity = self.build_tkn_entity(entity_name, tkn, &mut tkn_principal_mappings)?;
            token_entities.insert(tkn_name.to_string(), entity);
        }

        let workload = if self.build_workload {
            let workload_entity = self.build_workload_entity(tokens, &tkn_principal_mappings)?;
            Some(workload_entity)
        } else {
            None
        };

        let (user, roles) = if self.build_user {
            let (user, roles) = self.build_user_entity(tokens, &tkn_principal_mappings)?;
            (Some(user), roles)
        } else {
            (None, Vec::new())
        };

        let resource = self.build_resource_entity(resource)?;

        Ok(AuthorizeEntitiesData {
            workload,
            user,
            resource,
            roles,
            tokens: token_entities,
        })
    }
}

#[derive(Copy, Clone)]
pub struct EntityIdSrc<'a> {
    token: &'a Token<'a>,
    claim: &'a str,
}

pub fn get_first_valid_entity_id<'a>(
    id_srcs: &[EntityIdSrc],
) -> Result<SmolStr, BuildEntityErrorKind> {
    let mut errors = Vec::with_capacity(4);

    println!("trying {} srcs", id_srcs.len());
    for src in id_srcs.iter() {
        println!("trying {} from {}", src.claim, src.token.name);
        let claim = match src.token.get_claim_val(src.claim) {
            Some(claim) => claim,
            None => {
                errors.push(GetEntityIdError {
                    token: src.token.name.clone(),
                    claim: src.claim.to_string(),
                    reason: GetEntityIdErrorReason::MissingClaim,
                });
                continue;
            },
        };

        let claim = claim.to_string();
        let id = claim.trim_matches('"');

        if id.is_empty() {
            errors.push(GetEntityIdError {
                token: src.token.name.clone(),
                claim: src.claim.to_string(),
                reason: GetEntityIdErrorReason::EmptyString,
            });
            continue;
        }

        return Ok(id.to_smolstr());
    }

    Err(BuildEntityErrorKind::MissingEntityId(errors.into()))
}

pub fn collect_all_valid_entity_ids<'a>(id_srcs: &[EntityIdSrc]) -> Vec<SmolStr> {
    id_srcs
        .iter()
        .flat_map(|src| {
            src.token.get_claim_val(src.claim).and_then(|claim| {
                let claim = claim.to_string();
                let id = claim.trim_matches('"');

                if id.is_empty() {
                    None
                } else {
                    Some(id.to_smolstr())
                }
            })
        })
        .collect()
}

pub fn build_cedar_entity(
    type_name: &str,
    id: &str,
    attrs: HashMap<String, RestrictedExpression>,
    parents: HashSet<EntityUid>,
) -> Result<Entity, BuildEntityError> {
    let uid = EntityUid::from_str(&format!("{}::\"{}\"", type_name, id))
        .map_err(|e| BuildEntityErrorKind::from(e).while_building(type_name))?;
    let entity = Entity::new(uid, attrs, parents)
        .map_err(|e| BuildEntityErrorKind::from(e).while_building(type_name))?;

    Ok(entity)
}

fn default_tkn_entity_name(tkn_name: &str) -> Option<&'static str> {
    match tkn_name {
        "access_token" => Some(DEFAULT_ACCESS_TKN_ENTITY_NAME),
        "id_token" => Some(DEFAULT_ID_TKN_ENTITY_NAME),
        "userinfo_token" => Some(DEFAULT_USERINFO_TKN_ENTITY_NAME),
        _ => None,
    }
}

#[derive(Debug, thiserror::Error)]
#[error("failed to build `\"{entity_type_name}\"`: {error}")]
pub struct BuildEntityError {
    pub entity_type_name: String,
    pub error: BuildEntityErrorKind,
}

#[derive(Debug, thiserror::Error)]
pub enum BuildEntityErrorKind {
    #[error("unable to find a valid entity id: {0}")]
    MissingEntityId(GetEntityIdErrors),
    #[error(transparent)]
    TokenClaimTypeMismatch(#[from] TokenClaimTypeError),
    #[error("failed to parse entity uid: {0}")]
    FailedToParseUid(#[from] cedar_policy::ParseErrors),
    #[error("failed to evaluate entity attribute or tag: {0}")]
    EntityAttrEval(#[from] cedar_policy::EntityAttrEvaluationError),
    #[error("invalid issuer URL: {0}")]
    InvalidIssUrl(#[from] url::ParseError),
    #[error("missing required token: {0}")]
    MissingRequiredToken(String),
}

#[derive(Debug)]
pub struct GetEntityIdErrors(Vec<GetEntityIdError>);

#[cfg(test)]
impl GetEntityIdErrors {
    pub fn contains(&self, err: &GetEntityIdError) -> bool {
        self.0.contains(err)
    }

    pub fn len(&self) -> usize {
        self.0.len()
    }
}

impl From<Vec<GetEntityIdError>> for GetEntityIdErrors {
    fn from(errors: Vec<GetEntityIdError>) -> Self {
        Self(errors)
    }
}

impl Display for GetEntityIdErrors {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let err_msgs = self
            .0
            .iter()
            .map(|err| err.to_string())
            .collect::<Vec<_>>()
            .join(", ");
        write!(f, "[{}]", err_msgs)
    }
}

#[derive(Debug, thiserror::Error, PartialEq)]
#[error("failed to use {claim} from {token} since {reason}")]
pub struct GetEntityIdError {
    token: String,
    claim: String,
    reason: GetEntityIdErrorReason,
}

#[derive(Debug, thiserror::Error, PartialEq)]
pub enum GetEntityIdErrorReason {
    #[error("the claim cannot be an empty string")]
    EmptyString,
    #[error("the claim was not present in the token")]
    MissingClaim,
}

impl BuildEntityErrorKind {
    pub fn while_building(self, entity_type_name: &str) -> BuildEntityError {
        BuildEntityError {
            entity_type_name: entity_type_name.to_string(),
            error: self,
        }
    }
}

pub struct EntityAttrSrc<'a> {
    values: &'a HashMap<String, Value>,
    mappings: Option<&'a ClaimMappings>,
}

impl<'a> From<&'a Token<'a>> for EntityAttrSrc<'a> {
    fn from(token: &'a Token) -> Self {
        Self {
            values: token.claims_value(),
            mappings: token.claim_mapping(),
        }
    }
}

impl<'a> From<&'a HashMap<String, Value>> for EntityAttrSrc<'a> {
    fn from(map: &'a HashMap<String, Value>) -> Self {
        Self {
            values: map,
            mappings: None,
        }
    }
}

/// Builds Cedar entity attributes using the given JWTs
///
/// This function will *JOIN* the token attributes bu&Token<'_>ilt from each token
pub fn build_entity_attrs(attr_srcs: Vec<EntityAttrSrc>) -> HashMap<String, RestrictedExpression> {
    attr_srcs.iter().fold(HashMap::new(), |mut acc, src| {
        let attrs = src.values.iter().flat_map(|(name, value)| {
            if let Some(mapping) = src.mappings.and_then(|m| m.mapping(name)) {
                let mapped_claims = mapping.apply_mapping(value);
                let attrs_from_mapped_claims =
                    mapped_claims
                        .iter()
                        .fold(HashMap::new(), |mut acc, (name, value)| {
                            if let Some(expr) = value_to_expr(value) {
                                acc.insert(name.to_string(), expr);
                            }
                            acc
                        });
                let expr = RestrictedExpression::new_record(attrs_from_mapped_claims)
                    .expect("TODO: handle error");

                Some((name.into(), expr))
            } else if let Some(expr) = value_to_expr(value) {
                Some((name.into(), expr))
            } else {
                println!("skipped building attr for {}", name);
                None
            }
        });

        acc.extend(attrs);
        acc
    })
}

/// Adds token entity references to a principal entity's attributes if a
/// token->principal mapping is present.
pub fn add_token_references(
    entity_type_name: &str,
    mut attrs: HashMap<String, RestrictedExpression>,
    tkn_principal_mappings: &TokenPrincipalMappings,
) -> HashMap<String, RestrictedExpression> {
    if let Some(mapping) = tkn_principal_mappings.get(entity_type_name) {
        attrs.extend(mapping.clone());
    }

    attrs
}

/// Converts a [`Value`] to a [`RestrictedExpression`]
fn value_to_expr(value: &Value) -> Option<RestrictedExpression> {
    let expr = match value {
        Value::Null => return None,
        Value::Bool(val) => RestrictedExpression::new_bool(*val),
        Value::Number(val) => {
            if let Some(int) = val.as_i64() {
                RestrictedExpression::new_long(int)
            } else if let Some(float) = val.as_f64() {
                RestrictedExpression::new_decimal(float.to_string())
            } else {
                return None;
            }
        },
        Value::String(val) => {
            if IpAddr::from_str(val).is_ok() {
                RestrictedExpression::new_ip(val)
            } else {
                RestrictedExpression::new_string(val.to_string())
            }
        },
        Value::Array(values) => {
            let exprs = values.iter().filter_map(value_to_expr).collect::<Vec<_>>();
            RestrictedExpression::new_set(exprs)
        },
        Value::Object(map) => {
            let fields = map
                .iter()
                .filter_map(|(key, val)| value_to_expr(val).map(|expr| (key.to_string(), expr)))
                .collect::<Vec<_>>();
            // TODO: handle error
            RestrictedExpression::new_record(fields).expect("there shouldn't be duplicate keys")
        },
    };
    Some(expr)
}

#[derive(Default)]
pub struct TokenPrincipalMappings(HashMap<String, Vec<(String, RestrictedExpression)>>);

/// Represents a token and it's UID
pub struct TokenPrincipalMapping {
    /// The principal where token will be inserted
    principal: String,
    /// The name of the attribute of the token
    attr_name: String,
    /// An EntityUID reference to the token
    expr: RestrictedExpression,
}

impl TokenPrincipalMappings {
    pub fn insert(&mut self, value: TokenPrincipalMapping) {
        let entry = self.0.entry(value.principal).or_default();
        entry.push((value.attr_name, value.expr));
    }

    pub fn get(&self, principal: &str) -> Option<&Vec<(String, RestrictedExpression)>> {
        self.0.get(principal)
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::jwt::Token;
    use cedar_policy::EvalResult;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_get_first_valid_eid() {
        let expected_aud = "some_aud";
        let expected_client_id = "some_client_id";

        let token = Token::new(
            "test_token",
            HashMap::from([("aud".into(), json!(expected_aud))]).into(),
            None,
        );

        // Using the token's aud
        let id = get_first_valid_entity_id(&vec![EntityIdSrc {
            token: &token,
            claim: "aud",
        }])
        .expect("should get entity id from token's aud");
        assert_eq!(id, expected_aud);

        // Using the token's aud if the client_id is not present
        let id = get_first_valid_entity_id(&vec![
            EntityIdSrc {
                token: &token,
                claim: "client_id",
            },
            EntityIdSrc {
                token: &token,
                claim: "aud",
            },
        ])
        .expect("should get entity id from token's aud");
        assert_eq!(id, expected_aud);

        let token = Token::new(
            "test_token",
            HashMap::from([
                ("aud".into(), json!(expected_aud)),
                ("client_id".into(), json!(expected_client_id)),
            ])
            .into(),
            None,
        );

        // Using the first valid id even if others are also valid
        let id = get_first_valid_entity_id(&vec![
            EntityIdSrc {
                token: &token,
                claim: "client_id",
            },
            EntityIdSrc {
                token: &token,
                claim: "aud",
            },
        ])
        .expect("should get entity id from token's client_id");
        assert_eq!(id, expected_client_id);

        let token = Token::new(
            "test_token",
            HashMap::from([("empty".into(), json!(""))]).into(),
            None,
        );

        // Errors when no valid ids found
        let err = get_first_valid_entity_id(&vec![
            EntityIdSrc {
                token: &token,
                claim: "empty",
            },
            EntityIdSrc {
                token: &token,
                claim: "missing",
            },
        ])
        .expect_err("should error while getting id");
        let expected_errs = vec![
            GetEntityIdError {
                token: "test_token".into(),
                claim: "empty".into(),
                reason: GetEntityIdErrorReason::EmptyString,
            },
            GetEntityIdError {
                token: "test_token".into(),
                claim: "missing".into(),
                reason: GetEntityIdErrorReason::MissingClaim,
            },
        ];
        assert!(matches!(
            err,
            BuildEntityErrorKind::MissingEntityId(GetEntityIdErrors(ref errs))
                if *errs == expected_errs
        ));
    }

    #[test]
    fn test_collect_all_entity_ids() {
        let token1 = Token::new(
            "tkn1",
            HashMap::from([("role".into(), json!("role1"))]).into(),
            None,
        );
        let token2 = Token::new(
            "tkn1",
            HashMap::from([("role".into(), json!("role2"))]).into(),
            None,
        );

        // Collecting one valid id
        let ids = collect_all_valid_entity_ids(&vec![EntityIdSrc {
            token: &token1,
            claim: "role",
        }]);
        assert_eq!(ids, vec!["role1"]);

        // Collecting multiple valid ids
        let ids = collect_all_valid_entity_ids(&vec![
            EntityIdSrc {
                token: &token1,
                claim: "role",
            },
            EntityIdSrc {
                token: &token2,
                claim: "role",
            },
        ]);
        assert_eq!(ids, vec!["role1", "role2"]);

        // Ignore invalid ids
        let ids = collect_all_valid_entity_ids(&vec![
            EntityIdSrc {
                token: &token1,
                claim: "role",
            },
            EntityIdSrc {
                token: &token1,
                claim: "missing",
            },
            EntityIdSrc {
                token: &token1,
                claim: "",
            },
            EntityIdSrc {
                token: &token2,
                claim: "role",
            },
        ]);
        assert_eq!(ids, vec!["role1", "role2"]);
    }

    #[test]
    pub fn test_adding_token_refs_to_principals() {
        // Define mappings
        let mut mappings = TokenPrincipalMappings::default();
        mappings.insert(TokenPrincipalMapping {
            principal: "Workload".into(),
            attr_name: "access_token".into(),
            expr: RestrictedExpression::new_entity_uid(
                EntityUid::from_str("Access_token::\"some_access_tkn\"").unwrap(),
            ),
        });
        mappings.insert(TokenPrincipalMapping {
            principal: "User".into(),
            attr_name: "id_token".into(),
            expr: RestrictedExpression::new_entity_uid(
                EntityUid::from_str("Id_token::\"some_id_tkn\"").unwrap(),
            ),
        });
        mappings.insert(TokenPrincipalMapping {
            principal: "User".into(),
            attr_name: "userinfo_token".into(),
            expr: RestrictedExpression::new_entity_uid(
                EntityUid::from_str("Userinfo_token::\"some_userinfo_tkn\"").unwrap(),
            ),
        });

        // Test for Workload
        let attrs = HashMap::new();
        let attrs = add_token_references("Workload", attrs, &mappings);
        let entity = Entity::new(
            EntityUid::from_str("Workload::\"some_workload\"")
                .expect("should parse EntityUid from str"),
            attrs,
            HashSet::new(),
        )
        .expect("should build workload entity")
        .to_json_value()
        .expect("should serialize entity to JSON");
        assert_eq!(
            entity,
            json!({
                "uid": {"type": "Workload", "id": "some_workload"},
                "attrs": {
                    "access_token": { "__entity": {
                        "type": "Access_token",
                        "id": "some_access_tkn"
                    }}
                },
                "parents": [],
            })
        );

        // Test for User
        let attrs = HashMap::new();
        let attrs = add_token_references("User", attrs, &mappings);
        let entity = Entity::new(
            EntityUid::from_str("User::\"some_user\"").expect("should parse EntityUid from str"),
            attrs,
            HashSet::new(),
        )
        .expect("should build user entity")
        .to_json_value()
        .expect("should serialize entity to JSON");
        assert_eq!(
            entity,
            json!({
                "uid": {"type": "User", "id": "some_user"},
                "attrs": {
                    "id_token": { "__entity": {
                        "type": "Id_token",
                        "id": "some_id_tkn"
                    }},
                    "userinfo_token": { "__entity": {
                        "type": "Userinfo_token",
                        "id": "some_userinfo_tkn"
                    }}
                },
                "parents": [],
            })
        );
    }

    #[test]
    pub fn test_value_to_expr() {
        // Value::
        let attrs = HashMap::from_iter(
            [
                ("test_null", value_to_expr(&json!(Value::Null))),
                ("test_bool", value_to_expr(&json!(true))),
                ("test_long", value_to_expr(&json!(521))),
                ("test_decimal", value_to_expr(&json!(12.5))),
                ("test_str", value_to_expr(&json!("some str"))),
                ("test_set", value_to_expr(&json!(["a", 1]))),
                ("test_record", value_to_expr(&json!({"a": 1, "b": "b"}))),
            ]
            .into_iter()
            .flat_map(|(key, expr)| expr.map(|expr| (key.to_string(), expr))),
        );

        let entity = Entity::new(
            EntityUid::from_str("Test::\"test\"").expect("should parse EntityUid"),
            attrs,
            HashSet::new(),
        )
        .expect("should create entity");

        assert!(matches!(
            entity.attr("test_bool").expect("entity should have a `test_bool` attribute").expect("should be a valid value"),
            EvalResult::Bool(ref val)
                if *val == true,
        ));

        assert!(matches!(
            entity.attr("test_long").expect("entity should have a `test_long` attribute").expect("should be a valid value"),
            EvalResult::Long(ref val)
                if *val == 521,
        ));

        assert!(matches!(
            entity.attr("test_decimal").expect("entity should have a `test_decimal` attribute").expect("should be a valid value"),
            EvalResult::ExtensionValue(ref val)
                if *val == "decimal(\"12.5\")",
        ));

        assert!(matches!(
            entity.attr("test_str").expect("entity should have a `test_str` attribute").expect("should be a valid value"),
            EvalResult::String(ref val)
                if *val == "some str",
        ));

        assert!(matches!(
            entity.attr("test_set").expect("entity should have a `test_set` attribute").expect("should be a valid value"),
            EvalResult::Set(set)
                if
                    set.len() == 2 &&
                    set.contains(&EvalResult::String("a".into())) &&
                    set.contains(&EvalResult::Long(1))
        ));

        assert!(matches!(
            entity.attr("test_record").expect("entity should have a `test_record` attribute").expect("should be a valid value"),
            EvalResult::Record(record)
                if
                    record.len() == 2 &&
                    record.get("a") == Some(&EvalResult::Long(1)) &&
                    record.get("b") == Some(&EvalResult::String("b".into()))
        ));
    }
}
