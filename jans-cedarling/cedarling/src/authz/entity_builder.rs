// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod build_attrs;
mod build_expr;
mod build_resource_entity;
mod build_role_entity;
mod build_token_entities;
mod build_user_entity;
mod build_workload_entity;
mod mapping;

use crate::common::cedar_schema::CEDAR_NAMESPACE_SEPARATOR;
use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use crate::common::policy_store::TokenKind;
use crate::jwt::{Token, TokenClaimTypeError};
use crate::{AuthorizationConfig, ResourceData};
use build_attrs::{BuildAttrError, ClaimAliasMap, build_entity_attrs_from_tkn};
use build_expr::*;
use build_resource_entity::{BuildResourceEntityError, JsonTypeError};
use build_role_entity::BuildRoleEntityError;
pub use build_token_entities::BuildTokenEntityError;
use build_user_entity::BuildUserEntityError;
use build_workload_entity::BuildWorkloadEntityError;
use cedar_policy::{Entity, EntityId, EntityTypeName, EntityUid};
use serde_json::Value;
use std::collections::{HashMap, HashSet};
use std::convert::Infallible;
use std::fmt;
use std::str::FromStr;

use super::AuthorizeEntitiesData;

const DEFAULT_WORKLOAD_ENTITY_NAME: &str = "Workload";
const DEFAULT_USER_ENTITY_NAME: &str = "User";
const DEFAULT_ACCESS_TKN_ENTITY_NAME: &str = "Access_token";
const DEFAULT_ID_TKN_ENTITY_NAME: &str = "id_token";
const DEFAULT_USERINFO_TKN_ENTITY_NAME: &str = "Userinfo_token";
const DEFAULT_ROLE_ENTITY_NAME: &str = "Role";

pub struct DecodedTokens {
    pub access: Option<Token>,
    pub id: Option<Token>,
    pub userinfo: Option<Token>,
}

/// The names of the entities in the schema
pub struct EntityNames {
    user: String,
    workload: String,
    id_token: String,
    access_token: String,
    userinfo_token: String,
    role: String,
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
            id_token: config
                .mapping_id_token
                .clone()
                .unwrap_or_else(|| DEFAULT_ID_TKN_ENTITY_NAME.to_string()),
            access_token: config
                .mapping_access_token
                .clone()
                .unwrap_or_else(|| DEFAULT_ACCESS_TKN_ENTITY_NAME.to_string()),
            userinfo_token: config
                .mapping_userinfo_token
                .clone()
                .unwrap_or_else(|| DEFAULT_USERINFO_TKN_ENTITY_NAME.to_string()),
            // TODO: implement a bootstrap property to set the Role entity name
            role: DEFAULT_ROLE_ENTITY_NAME.to_string(),
        }
    }
}

impl Default for EntityNames {
    fn default() -> Self {
        Self {
            user: DEFAULT_USER_ENTITY_NAME.to_string(),
            workload: DEFAULT_WORKLOAD_ENTITY_NAME.to_string(),
            id_token: DEFAULT_ID_TKN_ENTITY_NAME.to_string(),
            access_token: DEFAULT_ACCESS_TKN_ENTITY_NAME.to_string(),
            userinfo_token: DEFAULT_USERINFO_TKN_ENTITY_NAME.to_string(),
            role: DEFAULT_ROLE_ENTITY_NAME.to_string(),
        }
    }
}

pub struct EntityBuilder {
    schema: CedarSchemaJson,
    entity_names: EntityNames,
    build_workload: bool,
    build_user: bool,
}

impl EntityBuilder {
    pub fn new(
        schema: CedarSchemaJson,
        entity_names: EntityNames,
        build_workload: bool,
        build_user: bool,
    ) -> Self {
        Self {
            schema,
            entity_names,
            build_workload,
            build_user,
        }
    }

    pub fn build_entities(
        &self,
        tokens: &DecodedTokens,
        resource: &ResourceData,
    ) -> Result<AuthorizeEntitiesData, BuildCedarlingEntityError> {
        let workload = if self.build_workload {
            Some(self.build_workload_entity(tokens)?)
        } else {
            None
        };

        let (user, roles) = if self.build_user {
            let roles = self.try_build_role_entities(tokens)?;
            let parents = roles
                .iter()
                .map(|role| role.uid())
                .collect::<HashSet<EntityUid>>();
            (Some(self.build_user_entity(tokens, parents)?), roles)
        } else {
            (None, vec![])
        };

        let access_token = if let Some(token) = tokens.access.as_ref() {
            Some(
                self.build_access_tkn_entity(token)
                    .map_err(BuildCedarlingEntityError::AccessToken)?,
            )
        } else {
            None
        };

        let id_token = if let Some(token) = tokens.id.as_ref() {
            Some(
                self.build_id_tkn_entity(token)
                    .map_err(BuildCedarlingEntityError::IdToken)?,
            )
        } else {
            None
        };

        let userinfo_token = if let Some(token) = tokens.userinfo.as_ref() {
            Some(
                self.build_userinfo_tkn_entity(token)
                    .map_err(BuildCedarlingEntityError::UserinfoToken)?,
            )
        } else {
            None
        };

        let resource = self.build_resource_entity(resource)?;

        Ok(AuthorizeEntitiesData {
            workload,
            user,
            access_token,
            id_token,
            userinfo_token,
            resource,
            roles,
        })
    }
}

/// Builds a Cedar Entity using a JWT
fn build_entity(
    schema: &CedarSchemaJson,
    entity_name: &str,
    token: &Token,
    id_src_claim: &str,
    claim_aliases: Vec<ClaimAliasMap>,
    parents: HashSet<EntityUid>,
) -> Result<Entity, BuildEntityError> {
    // Get entity Id from the specified token claim
    let entity_id = token
        .get_claim(id_src_claim)
        .ok_or(BuildEntityError::MissingClaim(id_src_claim.to_string()))?
        .as_str()?
        .to_owned();

    // Get entity namespace and type
    let mut entity_name = entity_name.to_string();
    let (namespace, entity_type) = schema
        .get_entity_from_base_name(&entity_name)
        .ok_or(BuildEntityError::EntityNotInSchema(entity_name.to_string()))?;
    if !namespace.is_empty() {
        entity_name = [namespace.as_str(), &entity_name].join(CEDAR_NAMESPACE_SEPARATOR);
    }

    // Build entity attributes
    let entity_attrs = build_entity_attrs_from_tkn(schema, entity_type, token, claim_aliases)
        .map_err(BuildEntityError::BuildAttribute)?;

    // Build cedar entity
    let entity_type_name =
        EntityTypeName::from_str(&entity_name).map_err(BuildEntityError::ParseEntityTypeName)?;
    let entity_id = EntityId::from_str(&entity_id).map_err(BuildEntityError::ParseEntityId)?;
    let entity_uid = EntityUid::from_type_name_and_id(entity_type_name, entity_id);
    Ok(Entity::new(entity_uid, entity_attrs, parents)?)
}

/// Errors encountered when building a Cedarling-specific entity
#[derive(Debug, thiserror::Error)]
pub enum BuildCedarlingEntityError {
    #[error(transparent)]
    Workload(#[from] BuildWorkloadEntityError),
    #[error(transparent)]
    User(#[from] BuildUserEntityError),
    #[error(transparent)]
    Role(#[from] BuildRoleEntityError),
    #[error("failed to build resource entity: {0}")]
    Resource(#[from] BuildResourceEntityError),
    #[error("error while building Access Token entity: {0}")]
    AccessToken(#[source] BuildTokenEntityError),
    #[error("error while building Id Token entity: {0}")]
    IdToken(#[source] BuildTokenEntityError),
    #[error("error while building Userinfo Token entity: {0}")]
    UserinfoToken(#[source] BuildTokenEntityError),
}

#[derive(Debug, thiserror::Error)]
pub enum BuildEntityError {
    #[error("failed to parse entity type name: {0}")]
    ParseEntityTypeName(#[source] cedar_policy::ParseErrors),
    #[error("failed to parse entity id: {0}")]
    ParseEntityId(#[source] Infallible),
    #[error("failed to evaluate entity or tag: {0}")]
    AttrEvaluation(#[from] cedar_policy::EntityAttrEvaluationError),
    #[error("failed to build entity since a token was not provided")]
    TokenUnavailable,
    #[error("the given token is missing a `{0}` claim")]
    MissingClaim(String),
    #[error(transparent)]
    TokenClaimTypeMismatch(#[from] TokenClaimTypeError),
    #[error(transparent)]
    JsonTypeError(#[from] JsonTypeError),
    #[error("the entity `{0}` is not defined in the schema")]
    EntityNotInSchema(String),
    #[error(transparent)]
    BuildAttribute(#[from] BuildAttrError),
    #[error("got {0} token, expected: {1}")]
    InvalidToken(TokenKind, TokenKind),
}

impl BuildEntityError {
    pub fn json_type_err(expected_type_name: &str, got_value: &Value) -> Self {
        Self::JsonTypeError(JsonTypeError::type_mismatch(expected_type_name, got_value))
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
    use crate::common::policy_store::TrustedIssuer;
    use crate::jwt::{Token, TokenClaims};
    use cedar_policy::EvalResult;
    use serde_json::json;
    use std::collections::HashMap;
    use std::sync::Arc;

    #[test]
    fn can_build_entity_using_jwt() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Workload": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "client_id": { "type": "String" },
                        "name": { "type": "String" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let iss = Arc::new(TrustedIssuer::default());
        let token = Token::new_access(
            TokenClaims::new(HashMap::from([
                ("client_id".to_string(), json!("workload-123")),
                ("name".to_string(), json!("somename")),
            ])),
            Some(iss.clone()),
        );
        let entity = build_entity(
            &schema,
            "Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
        )
        .expect("should successfully build entity");

        assert_eq!(entity.uid().to_string(), "Jans::Workload::\"workload-123\"");
        assert_eq!(
            entity
                .attr("client_id")
                .expect("expected workload entity to have a `client_id` attribute")
                .unwrap(),
            EvalResult::String("workload-123".to_string()),
        );
        assert_eq!(
            entity
                .attr("name")
                .expect("expected workload entity to have a `name` attribute")
                .unwrap(),
            EvalResult::String("somename".to_string()),
        );
    }

    #[test]
    fn errors_on_invalid_entity_type_name() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Workload!": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "client_id": { "type": "String" },
                        "name": { "type": "String" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let iss = Arc::new(TrustedIssuer::default());
        let token = Token::new_access(
            TokenClaims::new(HashMap::from([
                ("client_id".to_string(), json!("workload-123")),
                ("name".to_string(), json!("somename")),
            ])),
            Some(iss.clone()),
        );

        let err = build_entity(
            &schema,
            "Workload!",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
        )
        .expect_err("should error while parsing entity type name");

        assert!(
            matches!(err, BuildEntityError::ParseEntityTypeName(_)),
            "expected ParseEntityTypeName error but got: {:?}",
            err
        );
    }

    #[test]
    fn errors_when_token_is_missing_entity_id_claim() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({}))
            .expect("should successfully build schema");
        let iss = Arc::new(TrustedIssuer::default());
        let token = Token::new_access(TokenClaims::new(HashMap::new()), Some(iss.clone()));

        let err = build_entity(
            &schema,
            "Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
        )
        .expect_err("should error while parsing entity type name");

        assert!(
            matches!(
                err,
                BuildEntityError::MissingClaim(ref claim_name)
                if claim_name =="client_id"
            ),
            "expected MissingClaim error but got: {}",
            err
        );
    }

    #[test]
    fn errors_token_claim_has_unexpected_type() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({
            "Jans": { "entityTypes": { "Workload": {
                "shape": {
                    "type": "Record",
                    "attributes":  {
                        "client_id": { "type": "String" },
                    },
                }
            }}}
        }))
        .expect("should successfully build schema");
        let iss = Arc::new(TrustedIssuer::default());
        let token = Token::new_access(
            TokenClaims::new(HashMap::from([("client_id".to_string(), json!(123))])),
            Some(iss.clone()),
        );
        let err = build_entity(
            &schema,
            "Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
        )
        .expect_err("should error due to unexpected json type");

        assert!(
            matches!(
                err,
                BuildEntityError::TokenClaimTypeMismatch(ref err)
                if err == &TokenClaimTypeError::type_mismatch("client_id", "String", &json!(123))
            ),
            "expected TokenClaimTypeMismatch error but got: {:?}",
            err
        );
    }

    #[test]
    fn errors_when_entity_not_in_schema() {
        let schema = serde_json::from_value::<CedarSchemaJson>(json!({}))
            .expect("should successfully build schema");
        let iss = Arc::new(TrustedIssuer::default());
        let token = Token::new_access(
            TokenClaims::new(HashMap::from([(
                "client_id".to_string(),
                json!("client-123"),
            )])),
            Some(iss.clone()),
        );

        let err = build_entity(
            &schema,
            "Workload",
            &token,
            "client_id",
            Vec::new(),
            HashSet::new(),
        )
        .expect_err("should error due to entity not being in the schema");
        assert!(
            matches!(
                err,
                BuildEntityError::EntityNotInSchema(ref type_name)
                if type_name == "Workload"
            ),
            "expected EntityNotInSchema error but got: {:?}",
            err
        );
    }
}
