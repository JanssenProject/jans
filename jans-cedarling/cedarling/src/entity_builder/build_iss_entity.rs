// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::build_entity_attrs::build_entity_attrs;
use super::built_entities::BuiltEntities;
use super::schema::MappingSchema;
use super::{BuildEntityError, BuildEntityErrorKind, build_cedar_entity};
use crate::common::policy_store::TrustedIssuer;
use cedar_policy::Entity;
use serde_json::{Value, json};
use smol_str::ToSmolStr;
use std::collections::{HashMap, HashSet};
use url::Origin;

pub fn build_iss_entity(
    iss_type_name: &str,
    id: &str,
    iss: &TrustedIssuer,
    schema: Option<&MappingSchema>,
) -> Result<(Origin, Entity), BuildEntityError> {
    let origin = iss.oidc_endpoint.origin();

    let attrs_shape = schema
        .as_ref()
        .map(|s| {
            s.get_entity_shape(iss_type_name)
                .ok_or(BuildEntityErrorKind::EntityNotInSchema.while_building(iss_type_name))
        })
        .transpose()?;

    let attrs = build_entity_attrs(
        &iss.entity_attr_srcs(),
        &BuiltEntities::default(),
        attrs_shape,
        None,
    )
    .map_err(|e| BuildEntityErrorKind::from(e).while_building(iss_type_name))?;

    let entity = build_cedar_entity(iss_type_name, id, attrs, HashSet::new())?;

    Ok((origin, entity))
}

impl TrustedIssuer {
    /// This function should return the [`Value`]-s needed to to create the
    /// `"TrustedIssuer"` entity.
    ///
    /// This currently hard-coded since we don't have specifications yet on
    /// how to configure this using the policy store.
    fn entity_attr_srcs(&self) -> HashMap<String, Value> {
        let oidc_endpoint = &self.oidc_endpoint;
        HashMap::from([(
            "issuer_entity_id".into(),
            json!({
                "host": oidc_endpoint.host().map(|h| h.to_smolstr()),
                "path": "/",
                "protocol": oidc_endpoint.scheme(),
            }),
        )])
    }
}

#[cfg(test)]
mod test {
    use super::super::build_iss_entity::build_iss_entity;
    use super::super::test::*;
    use crate::common::policy_store::TrustedIssuer;
    use crate::entity_builder::schema::MappingSchema;
    use serde_json::json;

    #[test]
    fn can_build_trusted_issuer_entity_with_schema() {
        let iss: TrustedIssuer = serde_json::from_value(json!({
            "name": "some_iss",
            "description": "some_desc",
            "openid_configuration_endpoint": "https://test.jans.org",
            "tokens_metadata": {},
        }))
        .expect("should be a valid trusted issuer");
        let schema: MappingSchema = cedarling_validator_schema()
            .try_into()
            .expect("should initialize mapping schema");
        let (origin, iss_entity) =
            build_iss_entity("Jans::TrustedIssuer", "some_iss", &iss, Some(&schema))
                .expect("should build TrustedIssuer entity");

        assert_eq!(origin.ascii_serialization(), "https://test.jans.org");

        assert_entity_eq(
            &iss_entity,
            json!({
                "uid": {"type": "Jans::TrustedIssuer", "id": "some_iss"},
                "attrs": {
                    "issuer_entity_id": {
                        "protocol": "https",
                        "host": "test.jans.org",
                        "path": "/",
                    }
                },
                "parents": [],
            }),
            Some(cedarling_schema()),
        );
    }

    #[test]
    fn can_build_trusted_issuer_entity_without_schema() {
        let iss: TrustedIssuer = serde_json::from_value(json!({
            "name": "some_iss",
            "description": "some_desc",
            "openid_configuration_endpoint": "https://test.jans.org",
            "tokens_metadata": {},
        }))
        .expect("should be a valid trusted issuer");
        let (origin, iss_entity) = build_iss_entity("TrustedIssuer", "some_iss", &iss, None)
            .expect("should build TrustedIssuer entity");

        assert_eq!(origin.ascii_serialization(), "https://test.jans.org");

        assert_entity_eq(
            &iss_entity,
            json!({
                "uid": {"type": "TrustedIssuer", "id": "some_iss"},
                "attrs": {
                    "issuer_entity_id": {
                        "protocol": "https",
                        "host": "test.jans.org",
                        "path": "/",
                    },
                },
                "parents": [],
            }),
            None,
        );
    }
}
