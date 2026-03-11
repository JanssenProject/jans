// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::{
    AttrSrc, BuildUnsignedEntityError, BuiltEntities, BuiltPrincipalUnsigned, EntityBuilder,
    EntityData, EntityIdSrc, EntityUid, HashMap, TokenPrincipalMappings, Value, build_cedar_entity,
};
use cedar_policy::Entity;
use std::collections::HashSet;

impl EntityBuilder {
    pub(crate) fn build_principal_unsigned(
        &self,
        principal: &EntityData,
        built_entities: &BuiltEntities,
    ) -> Result<BuiltPrincipalUnsigned, BuildUnsignedEntityError> {
        let type_name: &str = &principal.cedar_mapping.entity_type;
        let id_srcs = vec![EntityIdSrc::String(&principal.cedar_mapping.id)];

        let roles = self.build_role_entities_unsigned(principal)?;
        let role_uids = roles
            .iter()
            .map(Entity::uid)
            .collect::<HashSet<EntityUid>>();
        let attrs_srcs = vec![AttrSrc::Unsigned(&principal.attributes)];

        let principal = self
            .build_principal_entity(
                type_name,
                &id_srcs,
                attrs_srcs,
                &TokenPrincipalMappings::default(),
                built_entities,
                role_uids,
            )
            .map_err(Box::new)?;

        Ok(BuiltPrincipalUnsigned {
            principal,
            parents: roles,
        })
    }

    /// Builds the role entities from the `role` or `roles` attributes from the
    /// [`UnsignedPrincipal`].
    ///
    /// Used for the [`authorize_unsigned`] interface.
    ///
    /// [`authorize_unsigned`]: crate::Cedarling::authorize_unsigned
    fn build_role_entities_unsigned(
        &self,
        principal: &EntityData,
    ) -> Result<Vec<Entity>, BuildUnsignedEntityError> {
        let mut role_ids = Vec::<String>::new();

        if let Some(role) = principal.attributes.get(&*self.config.unsigned_role_id_src) {
            role_ids.append(&mut extract_roles_from_value(role)?);
        }

        let mut role_entities = Vec::with_capacity(role_ids.len());
        for id in &role_ids {
            let role_entity = build_cedar_entity(
                &self.config.entity_names.role,
                id,
                HashMap::new(),
                HashSet::new(),
            )
            .map_err(Box::new)?;
            role_entities.push(role_entity);
        }

        Ok(role_entities)
    }
}

fn extract_roles_from_value(value: &Value) -> Result<Vec<String>, BuildUnsignedEntityError> {
    match value {
        Value::String(role) => Ok(vec![role.clone()]),
        Value::Array(vals) => {
            let mut roles = Vec::new();
            for role in vals {
                let Value::String(role) = role else {
                    return Err(BuildUnsignedEntityError::InvalidType(role.clone()));
                };
                roles.push(role.clone());
            }
            Ok(roles)
        },
        value => Err(BuildUnsignedEntityError::InvalidType(value.clone())),
    }
}

#[cfg(test)]
mod test {
    use super::super::super::test::*;
    use super::*;
    use crate::common::default_entities::DefaultEntities;
    use crate::{CedarEntityMapping, EntityBuilderConfig, entity_builder::TrustedIssuerIndex};
    use cedar_policy::Schema;
    use cedar_policy_core::validator::ValidatorSchema;
    use serde_json::json;
    use std::collections::HashMap;
    use std::str::FromStr;

    #[test]
    fn can_build_unsigned_role_entities() {
        let schema_src = r"
            namespace Jans {
                entity Role;
            }
        ";
        let schema = Schema::from_str(schema_src).expect("build cedar Schema");
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("build cedar ValidatorSchema");
        let expected_value = json!({
            "uid": {"type": "Jans::Role", "id": "some_role"},
            "attrs": {},
            "parents": [],
        });

        let builder = EntityBuilder::new(
            EntityBuilderConfig::default().with_workload(),
            TrustedIssuerIndex::new(&HashMap::new(), None),
            Some(&validator_schema),
            DefaultEntities::default(),
        )
        .expect("should init entity builder");

        // Case: String in the `role` attribute
        let principal = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::User".to_string(),
                id: "some_user".to_string(),
            },
            attributes: HashMap::from([("role".to_string(), json!("some_role"))]),
        };
        let token_entities = builder
            .build_role_entities_unsigned(&principal)
            .expect("build role entities");
        assert_eq!(token_entities.len(), 1, "one token entity");
        assert_entity_eq(&token_entities[0], &expected_value, Some(&schema));

        // Case: Array in the `role` attribute
        let principal = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::User".to_string(),
                id: "some_user".to_string(),
            },
            attributes: HashMap::from([("role".to_string(), json!(["some_role", "another_role"]))]),
        };
        let token_entities = builder
            .build_role_entities_unsigned(&principal)
            .expect("build role entities");
        assert_eq!(token_entities.len(), 2, "two token entities");
        assert_entity_eq(&token_entities[0], &expected_value, Some(&schema));
        assert_entity_eq(
            &token_entities[1],
            &json!({
                "uid": {"type": "Jans::Role", "id": "another_role"},
                "attrs": {},
                "parents": [],
            }),
            Some(&schema),
        );
        // Case: Array in the `role` attribute
        let principal = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::User".to_string(),
                id: "some_user".to_string(),
            },
            attributes: HashMap::from([("role".to_string(), json!(["some_role", "another_role"]))]),
        };
        let token_entities = builder
            .build_role_entities_unsigned(&principal)
            .expect("build role entities");
        assert_eq!(token_entities.len(), 2, "two token entities");
        assert_entity_eq(&token_entities[0], &expected_value, Some(&schema));
        assert_entity_eq(
            &token_entities[1],
            &json!({
                "uid": {"type": "Jans::Role", "id": "another_role"},
                "attrs": {},
                "parents": [],
            }),
            Some(&schema),
        );

        // Case: no `role` attribute
        let principal = EntityData {
            cedar_mapping: CedarEntityMapping {
                entity_type: "Jans::User".to_string(),
                id: "some_user".to_string(),
            },
            attributes: HashMap::new(),
        };
        let token_entities = builder
            .build_role_entities_unsigned(&principal)
            .expect("build role entities");
        assert_eq!(token_entities.len(), 0, "no token entities");
    }
}
