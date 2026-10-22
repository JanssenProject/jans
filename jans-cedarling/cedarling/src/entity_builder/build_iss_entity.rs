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

pub(super) fn build_iss_entity(
    iss_type_name: &str,
    id: &str,
    iss: &TrustedIssuer,
    schema: Option<&MappingSchema>,
) -> Result<Entity, BuildEntityError> {
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
    )
    .map_err(|e| BuildEntityErrorKind::from(e).while_building(iss_type_name))?;

    build_cedar_entity(iss_type_name, id, attrs, HashSet::new())
}

impl TrustedIssuer {
    /// This function should return the [`Value`]-s needed to to create the
    /// `"TrustedIssuer"` entity.
    ///
    /// This currently hard-coded since we don't have specifications yet on
    /// how to configure this using the policy store.
    fn entity_attr_srcs(&self) -> HashMap<String, Value> {
        let iss = self.iss();
        HashMap::from([(
            "issuer_entity_id".into(),
            json!({
                "host": iss.host().map(|h| h.to_smolstr()),
                "path": iss.path(),
                "protocol": iss.scheme(),
            }),
        )])
    }
}

#[cfg(test)]
mod test {
    use std::collections::HashMap;

    use serde_json::json;
    use url::Url;

    use super::super::build_iss_entity::build_iss_entity;
    use super::super::test::*;
    use crate::common::policy_store::TrustedIssuer;
    use crate::entity_builder::schema::MappingSchema;

    fn test_trusted_issuer() -> TrustedIssuer {
        TrustedIssuer::discovery_at(
            "some_iss".to_string(),
            Url::parse("https://test.jans.org").unwrap(),
            HashMap::new(),
        )
    }

    #[test]
    fn can_build_trusted_issuer_entity_with_schema() {
        let iss = test_trusted_issuer();
        let schema: MappingSchema = (&*CEDARLING_VALIDATOR_SCHEMA)
            .try_into()
            .expect("should initialize mapping schema");
        let iss_entity = build_iss_entity("Jans::TrustedIssuer", "some_iss", &iss, Some(&schema))
            .expect("should build TrustedIssuer entity");

        assert_entity_eq(
            &iss_entity,
            &json!({
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
            Some(&CEDARLING_API_SCHEMA),
        );
    }

    #[test]
    fn can_build_trusted_issuer_entity_without_schema() {
        let iss = test_trusted_issuer();
        let iss_entity = build_iss_entity("TrustedIssuer", "some_iss", &iss, None)
            .expect("should build TrustedIssuer entity");

        assert_entity_eq(
            &iss_entity,
            &json!({
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

    /// Path-based issuers on one origin keep their own path.
    #[test]
    fn issuer_entity_id_keeps_the_iss_path() {
        for path in ["/realms/a", "/realms/b"] {
            let iss = TrustedIssuer::discovery_at(
                "Kc".to_string(),
                Url::parse(&format!("https://kc.example{path}")).unwrap(),
                HashMap::new(),
            );
            let iss_entity = build_iss_entity("TrustedIssuer", "kc", &iss, None)
                .expect("should build TrustedIssuer entity");

            assert_entity_eq(
                &iss_entity,
                &json!({
                    "uid": {"type": "TrustedIssuer", "id": "kc"},
                    "attrs": {
                        "issuer_entity_id": {
                            "protocol": "https",
                            "host": "kc.example",
                            "path": path,
                        },
                    },
                    "parents": [],
                }),
                None,
            );
        }
    }
}
