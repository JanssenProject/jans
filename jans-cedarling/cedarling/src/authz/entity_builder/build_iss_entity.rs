// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::{BuildEntityError, BuildEntityErrorKind, build_entity_attrs};
use crate::common::policy_store::TrustedIssuer;
use cedar_policy::{Entity, EntityUid};
use serde_json::{Value, json};
use std::collections::{HashMap, HashSet};
use std::str::FromStr;
use url::Origin;

pub fn build_iss_entity(
    iss_type_name: &str,
    id: &str,
    iss: &TrustedIssuer,
) -> Result<(Origin, Entity), BuildEntityError> {
    let origin = iss.oidc_endpoint.origin();

    let attrs = build_entity_attrs(vec![(&iss.entity_attr_srcs()).into()]);

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
        HashMap::from([(
            "issuer_entity_id".to_string(),
            json!(self.oidc_endpoint.to_string()),
        )])
    }
}
