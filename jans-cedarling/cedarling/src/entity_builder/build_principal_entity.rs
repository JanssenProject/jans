// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod user;
mod workload;

use super::entity_id_getters::*;
use super::*;
use cedar_policy::Entity;
use std::collections::HashSet;

type TokenClaims = HashMap<String, Value>;
type AttrSrc<'a> = (&'a TokenClaims, Option<&'a ClaimMappings>);

impl EntityBuilder {
    fn build_principal_entity<'a>(
        &self,
        type_name: &str,
        id_srcs: Vec<EntityIdSrc<'a>>,
        attrs_srcs: Vec<AttrSrc>,
        tkn_principal_mappings: &TokenPrincipalMappings,
        built_entities: &BuiltEntities,
        parents: HashSet<EntityUid>,
    ) -> Result<Entity, BuildEntityError> {
        let attrs_shape = self
            .schema
            .as_ref()
            .and_then(|s| s.get_entity_shape(type_name));

        let entity_id =
            get_first_valid_entity_id(&id_srcs).map_err(|e| e.while_building(type_name))?;

        // Extract attributes from sources
        let (user_attrs, errs): (Vec<_>, Vec<_>) = attrs_srcs
            .into_iter()
            .map(|(src, mappings)| build_entity_attrs(src, built_entities, attrs_shape, mappings))
            .partition_result();

        let mut entity_attrs: HashMap<String, RestrictedExpression> = if errs.is_empty() {
            // what should happen if claims have the same name but different values?
            user_attrs
                .into_iter()
                .fold(HashMap::new(), |mut acc, attrs| {
                    acc.extend(attrs);
                    acc
                })
        } else {
            let errs: Vec<_> = errs.into_iter().flat_map(|e| e.into_inner()).collect();
            return Err(BuildEntityErrorKind::from(errs).while_building(type_name));
        };

        // Apply token mappings if the schema/shape is not present since that's the only
        // time it's really necessary
        if attrs_shape.is_none() {
            tkn_principal_mappings.apply(type_name, &mut entity_attrs);
        }

        let entity = build_cedar_entity(type_name, &entity_id, entity_attrs, parents)?;

        Ok(entity)
    }
}

/// Information on how to get a principal's ID.
///
/// Used to define default values for the resolvers.
///
/// - [`user::UserIdSrcResolver`]     
/// - [`workload::WorkloadIdSrcResolver`]     
#[derive(Clone, Copy)]
pub struct PrincipalIdSrc<'a> {
    token: &'a str,
    claim: &'a str,
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::common::policy_store::TrustedIssuer;
    use std::collections::HashMap;

    #[test]
    fn err_on_missing_entity_id() {
        let schema_src = r#"namespace Jans {
            entity TrustedIssuer;
            entity Test;
        }
        "#;
        let validator_schema =
            ValidatorSchema::from_str(schema_src).expect("build cedar ValidatorSchema");
        let iss = TrustedIssuer::default();
        let issuers = HashMap::from([("some_iss".into(), iss.clone())]);
        let builder = EntityBuilder::new(
            EntityBuilderConfig::default().with_workload(),
            &issuers,
            Some(&validator_schema),
        )
        .expect("should init entity builder");

        let type_name = "Test";
        let id_token = Token::new("token", HashMap::from([]).into(), Some(&iss));
        let attrs_srcs = Vec::default();
        let tkn_principal_mappings = TokenPrincipalMappings::default();
        let built_entities = BuiltEntities::default();
        let parents = HashSet::default();

        // Case where there's no available sources
        let id_srcs = Vec::default();
        let error = builder
            .build_principal_entity(
                type_name,
                id_srcs,
                attrs_srcs.clone(),
                &tkn_principal_mappings,
                &built_entities,
                parents.clone(),
            )
            .expect_err("should fail to build entity");
        assert!(matches!(error, BuildEntityError {
            ref entity_type_name,
            ref error,
        } if
            entity_type_name == "Test" &&
            matches!(error, BuildEntityErrorKind::MissingEntityId(_))
        ));

        // Case where there's available sources but the token is missing the claim
        let id_srcs = vec![EntityIdSrc {
            token: &id_token,
            claim: "missing_claim",
        }];
        let error = builder
            .build_principal_entity(
                type_name,
                id_srcs,
                attrs_srcs,
                &tkn_principal_mappings,
                &built_entities,
                parents,
            )
            .expect_err("should fail to build entity");
        assert!(matches!(error, BuildEntityError {
            ref entity_type_name,
            ref error,
        } if
            entity_type_name == "Test" &&
            matches!(error, BuildEntityErrorKind::MissingEntityId(_))
        ));
    }
}
