// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::{BuildEntityError, BuildEntityErrorKind, build_entity_attrs};
use crate::common::policy_store::TrustedIssuer;
use cedar_policy::{Entity, EntityUid};
use serde_json::{Value, json};
use smol_str::ToSmolStr;
use std::collections::{HashMap, HashSet};
use std::str::FromStr;
use url::Origin;

pub fn build_iss_entity(
    iss_type_name: &str,
    id: &str,
    iss: &TrustedIssuer,
) -> Result<(Origin, Entity), BuildEntityError> {
    let origin = iss.oidc_endpoint.origin();

    let attrs = build_entity_attrs((&iss.entity_attr_srcs()).into());

    let uid = EntityUid::from_str(&format!("{}::\"{}\"", iss_type_name, id))
        .map_err(|e| BuildEntityErrorKind::from(e).while_building(iss_type_name))?;

    let entity = Entity::new(uid, attrs, HashSet::new())
        .map_err(|e| BuildEntityErrorKind::from(e).while_building(iss_type_name))?;

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
    use crate::authz::entity_builder::build_iss_entity::build_iss_entity;
    use crate::authz::entity_builder::test::cedarling_schema;
    use crate::common::policy_store::TrustedIssuer;
    use cedar_policy::Entities;
    use serde_json::json;

    #[test]
    fn can_build_trusted_issuer_entity() {
        let iss = TrustedIssuer::default();
        let (origin, iss_entity) = build_iss_entity("Jans::TrustedIssuer", "some_iss", &iss)
            .expect("should build TrustedIssuer entity");

        assert_eq!(origin.ascii_serialization(), "https://test.jans.org");
        assert_eq!(
            iss_entity
                .to_json_value()
                .expect("should serialize TrustedIssuer entity to JSON"),
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
            "TrustedIssuer entity should have the correct attrs"
        );

        Entities::from_entities([iss_entity], Some(cedarling_schema()))
            .expect("TrustedIssuer entity should conform to schema");
    }
}
