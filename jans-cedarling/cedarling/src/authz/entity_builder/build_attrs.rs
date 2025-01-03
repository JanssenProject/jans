// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use crate::{common::cedar_schema::cedar_json::entity_type::EntityType, jwt::Token};
use cedar_policy::RestrictedExpression;
use serde_json::Value;
use std::collections::HashMap;

impl EntityBuilder {
    /// Builds Cedar entity attributes using a JWT
    pub fn build_entity_attrs(
        &self,
        entity_type: &EntityType,
        token: &Token,
        claim_aliases: Vec<(&str, &str)>,
    ) -> Result<HashMap<String, RestrictedExpression>, BuildAttrError> {
        let mut entity_attrs = HashMap::new();

        let shape = match entity_type.shape.as_ref() {
            Some(shape) => shape,
            None => return Ok(entity_attrs),
        };

        let mut claims = token.claims_value().clone();
        apply_claim_aliases(&mut claims, &claim_aliases);

        for (attr_name, attr) in shape.attrs.iter() {
            let expression = if let Some(mapping) = token.claim_mapping().get(attr_name) {
                let claim = claims
                    .get(attr_name)
                    .ok_or_else(|| BuildAttrError::MissingClaim(attr_name.to_string()))?;
                let mapped_claim = mapping.apply_mapping(claim);
                attr.build_expr(&mapped_claim, attr_name, &self.schema)?
            } else {
                attr.build_expr(&claims, attr_name, &self.schema)?
            };

            if let Some(expr) = expression {
                entity_attrs.insert(attr_name.to_string(), expr);
            }
        }

        Ok(entity_attrs)
    }
}

fn apply_claim_aliases(
    claims: &mut HashMap<String, Value>,
    aliases: &[(impl AsRef<str>, impl AsRef<str>)],
) {
    for (from, to) in aliases {
        if let Some(claim) = claims.get(from.as_ref()) {
            claims.insert(to.as_ref().to_string(), claim.clone());
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum BuildAttrError {
    #[error("failed to build entity attribute due to a missing claim: `{0}`")]
    MissingClaim(String),
    #[error(transparent)]
    BuildExpression(#[from] BuildExprError),
}
