// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod build_entity_attrs;
mod build_expr;
mod build_iss_entity;
mod build_resource_entity;
mod build_token_entities;
mod build_user_entity;
mod build_workload_entity;
mod built_entities;
mod entity_id_getters;
mod value_to_expr;

use super::AuthorizeEntitiesData;
use crate::common::policy_store::TrustedIssuer;
use crate::jwt::{Token, TokenClaimTypeError};
use crate::{AuthorizationConfig, ResourceData};
use build_entity_attrs::*;
use build_iss_entity::build_iss_entity;
pub(crate) use built_entities::BuiltEntities;
use cedar_policy::{Entity, EntityUid, RestrictedExpression};
use entity_id_getters::GetEntityIdErrors;
use std::collections::hash_map::Entry;
use std::collections::{HashMap, HashSet};
use std::str::FromStr;
use url::{Origin, Url};

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
        let mut iss_entities = HashSet::with_capacity(3);

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

            let (tkn_entity, iss_entity) =
                self.build_tkn_entity(entity_name, tkn, &mut tkn_principal_mappings)?;
            iss_entity.map(|e| iss_entities.insert(e));
            token_entities.insert(tkn_name.to_string(), tkn_entity);
        }

        let workload = if self.build_workload {
            let (workload_entity, iss_entity) =
                self.build_workload_entity(tokens, &tkn_principal_mappings)?;
            iss_entity.map(|e| iss_entities.insert(e));
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
            issuers: iss_entities,
            workload,
            user,
            resource,
            roles,
            tokens: token_entities,
        })
    }

    /// Replaces the String `iss` attribute with a TrustedIssuer entity if
    /// available
    pub fn replace_iss_with_entity(
        &self,
        token: &Token,
        attrs: &mut HashMap<String, RestrictedExpression>,
    ) -> Result<Option<Entity>, BuildEntityErrorKind> {
        /// The name of the claim in the token as defined in RFC 7519
        const REGISTERED_ISS_CLAIM_NAME: &str = "iss";
        /// The name attribute that will contain the reference to the issuer entity
        const DEFAULT_ISS_ATTR_NAME: &str = "iss";

        let mut iss_entry = match attrs.entry(DEFAULT_ISS_ATTR_NAME.into()) {
            Entry::Occupied(entry) => entry,
            Entry::Vacant(_) => return Ok(None),
        };

        let iss_url = match token.get_claim(REGISTERED_ISS_CLAIM_NAME) {
            Some(claim) => {
                let iss_claim_value = claim.as_str()?;
                let iss_url = Url::parse(iss_claim_value)?;
                iss_url
            },
            None => return Ok(None),
        };

        if let Some(entity) = self.iss_entities.get(&iss_url.origin()) {
            let entity_ref = RestrictedExpression::new_entity_uid(entity.uid());
            iss_entry.insert(entity_ref);
            return Ok(Some(entity.clone()));
        }

        Ok(None)
    }
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
    #[error("unable to find a valid entity id, tried the following: {0}")]
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

impl BuildEntityErrorKind {
    pub fn while_building(self, entity_type_name: &str) -> BuildEntityError {
        BuildEntityError {
            entity_type_name: entity_type_name.to_string(),
            error: self,
        }
    }
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
    } else {
    }

    attrs
}

#[derive(Default)]
pub struct TokenPrincipalMappings(HashMap<String, Vec<(String, RestrictedExpression)>>);

impl From<Vec<TokenPrincipalMapping>> for TokenPrincipalMappings {
    fn from(value: Vec<TokenPrincipalMapping>) -> Self {
        Self(value.into_iter().fold(HashMap::new(), |mut acc, mapping| {
            acc.entry(mapping.principal.clone())
                .or_default()
                .push((mapping.attr_name.clone(), mapping.expr.clone()));
            acc
        }))
    }
}

/// Represents a token and it's UID
#[derive(Clone)]
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
    use cedar_policy::{Entities, Schema};
    use serde_json::Value;
    use serde_json::json;
    use std::{collections::HashMap, sync::OnceLock};

    static CEDARLING_SCHEMA: OnceLock<Schema> = OnceLock::new();

    pub fn cedarling_schema<'a>() -> &'a Schema {
        CEDARLING_SCHEMA.get_or_init(|| {
            Schema::from_str(include_str!("../../../schema/cedarling_core.cedarschema"))
                .expect("should be a valid Cedar schema")
        })
    }

    /// Helper function for asserting entities for better error readability
    pub fn assert_entity_eq(entity: &Entity, expected: Value, schema: Option<&Schema>) {
        let entity_json = entity
            .clone()
            .to_json_value()
            .expect("should serialize entity to JSON");

        // Check if the entity has the correct uid
        assert_eq!(
            entity_json["uid"], expected["uid"],
            "the entity uid does not match with the expected",
        );

        // Check if the entity has the correct attributes
        let expected_attrs =
            serde_json::from_value::<HashMap<String, Value>>(expected["attrs"].clone()).unwrap();
        for (name, expected_val) in expected_attrs.iter() {
            assert_eq!(
                &entity_json["attrs"][name],
                expected_val,
                "the {}'s `{}` attribute does not match with the expected",
                entity.uid().to_string(),
                name
            );
        }

        // Check if the entity has the correct parents
        assert_eq!(
            serde_json::from_value::<HashSet<Value>>(entity_json["parents"].clone()).unwrap(),
            serde_json::from_value::<HashSet<Value>>(expected["parents"].clone()).unwrap(),
            "the {} entity's parents does not match with the expected",
            entity.uid().to_string(),
        );

        // Check if the entity conforms to the schema
        Entities::from_entities([entity.clone()], schema).expect(&format!(
            "{} entity should conform to the schema",
            entity.uid().to_string()
        ));
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
        .expect("should build workload entity");

        assert_entity_eq(
            &entity,
            json!({
                "uid": {"type": "Workload", "id": "some_workload"},
                "attrs": {
                    "access_token": { "__entity": {
                        "type": "Access_token",
                        "id": "some_access_tkn"
                    }}
                },
                "parents": [],
            }),
            None,
        );

        // Test for User
        let attrs = HashMap::new();
        let attrs = add_token_references("User", attrs, &mappings);
        let entity = Entity::new(
            EntityUid::from_str("User::\"some_user\"").expect("should parse EntityUid from str"),
            attrs,
            HashSet::new(),
        )
        .expect("should build user entity");

        assert_entity_eq(
            &entity,
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
            }),
            None,
        );
    }
}
