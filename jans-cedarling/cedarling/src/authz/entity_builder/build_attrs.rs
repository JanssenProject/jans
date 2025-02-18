// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::*;
use crate::common::cedar_schema::cedar_json::entity_type::EntityType;
use cedar_policy::RestrictedExpression;
use serde_json::{Value, json};
use std::collections::HashMap;

/// Builds Cedar entity attributes using a JWT.
///
/// This uses claim mapping metadata to unwrap claims into their respective Cedar types
pub fn build_entity_attrs_from_tkn(
    schema: &CedarSchemaJson,
    entity_type: &EntityType,
    token: &Token,
    default_namespace: Option<&str>,
    claim_aliases: Vec<ClaimAliasMap>,
) -> Result<HashMap<String, RestrictedExpression>, BuildAttrError> {
    let mut entity_attrs = HashMap::new();

    let shape = match entity_type.shape.as_ref() {
        Some(shape) => shape,
        None => return Ok(entity_attrs),
    };

    let mut claims = token.claims_value().clone();
    apply_claim_aliases(&mut claims, claim_aliases);

    let claim_mapping = token.claim_mapping();

    for (attr_name, attr) in shape.attrs.iter() {
        let expression = if let Some(mapper) = claim_mapping.and_then(|x| x.get(attr_name))
        {
            let claim = claims.get(attr_name).ok_or_else(|| {
                BuildAttrError::new(
                    attr_name,
                    BuildAttrErrorKind::MissingSource(attr_name.to_string()),
                )
            })?;
            let mapped_claim_json_val = json!(mapper.apply_mapping(claim));

            attr.build_expr(
                attr_name,
                Some(&mapped_claim_json_val),
                default_namespace,
                schema,
            )
            .map_err(|e| BuildAttrError::new(attr_name, e.into()))?
        } else {
            match attr.build_expr(attr_name, claims.get(attr_name), default_namespace, schema) {
                Ok(expr) => expr,
                Err(err) if attr.is_required() => Err(BuildAttrError::new(attr_name, err.into()))?,
                // just skip when attribute isn't required even if it errors
                // TODO: though we should probably log this
                Err(_) => continue,
            }
        };

        if let Some(expr) = expression {
            entity_attrs.insert(attr_name.to_string(), expr);
        }
    }

    Ok(entity_attrs)
}

pub fn build_entity_attrs_from_values(
    schema: &CedarSchemaJson,
    default_namespace: Option<&str>,
    entity_type: &EntityType,
    src: &HashMap<String, Value>,
) -> Result<HashMap<String, RestrictedExpression>, BuildAttrError> {
    let mut entity_attrs = HashMap::new();

    let shape = match entity_type.shape.as_ref() {
        Some(shape) => shape,
        None => return Ok(entity_attrs),
    };

    for (attr_name, attr) in shape.attrs.iter() {
        let val = match src.get(attr_name) {
            Some(val) => val,
            None if attr.is_required() => {
                return Err(BuildAttrError::new(
                    attr_name,
                    BuildAttrErrorKind::MissingSource(attr_name.to_string()),
                ));
            },
            _ => continue,
        };

        let mapped_src = serde_json::from_value::<HashMap<String, Value>>(val.clone());
        let src = if let Ok(mapped_src) = mapped_src.as_ref() {
            mapped_src
        } else {
            src
        };

        let attr_claim_val = src.get(attr_name);

        let expression = match attr.build_expr(attr_name, attr_claim_val, default_namespace, schema)
        {
            Ok(expr) => expr,
            Err(err) if attr.is_required() => {
                return Err(BuildAttrError::new(attr_name, err.into()))?;
            },
            // move on to the next attribute if this isn't required
            Err(_) => continue,
        };

        if let Some(expr) = expression {
            entity_attrs.insert(attr_name.to_string(), expr);
        }
    }

    Ok(entity_attrs)
}

/// Describes how to rename a claim named `from` to `to`
pub struct ClaimAliasMap<'a> {
    from: &'a str,
    to: &'a str,
}

impl<'a> ClaimAliasMap<'a> {
    pub fn new(from: &'a str, to: &'a str) -> Self {
        Self { from, to }
    }
}

fn apply_claim_aliases(claims: &mut HashMap<String, Value>, aliases: Vec<ClaimAliasMap>) {
    for map in aliases {
        if let Some(claim) = claims.get(map.from) {
            claims.insert(map.to.to_string(), claim.clone());
        }
    }
}

#[derive(Debug, thiserror::Error)]
#[error("failed to build `{attr_name}` attribute: {source}")]
pub struct BuildAttrError {
    attr_name: String,
    #[source]
    source: BuildAttrErrorKind,
}

impl BuildAttrError {
    fn new(name: impl ToString, src: BuildAttrErrorKind) -> Self {
        Self {
            attr_name: name.to_string(),
            source: src,
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum BuildAttrErrorKind {
    #[error("missing attribute source: `{0}`")]
    MissingSource(String),
    #[error("failed to build restricted expression: {0}")]
    BuildExpression(#[from] BuildExprError),
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::common::cedar_schema::cedar_json::attribute::Attribute;
    use crate::common::cedar_schema::cedar_json::entity_type::{EntityShape, EntityType};
    use crate::common::policy_store::TrustedIssuer;
    use serde_json::json;
    use std::collections::HashMap;

    #[test]
    fn can_build_entity_attrs_from_tkn() {
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
        let entity_type = EntityType {
            member_of: None,
            tags: None,
            shape: Some(EntityShape {
                required: true,
                attrs: HashMap::from([("client_id".to_string(), Attribute::string())]),
            }),
        };
        let iss = TrustedIssuer::default();
        let token = Token::new(
            "access_token",
            HashMap::from([("client_id".to_string(), json!("workload-123"))]).into(),
            Some(&iss),
        );

        let attrs = build_entity_attrs_from_tkn(
            &schema,
            &entity_type,
            &token,
            None,
            Vec::new(),
        )
        .expect("should build entity attrs");
        // RestrictedExpression does not implement PartialEq so the best we can do is check
        // if the attribute was created
        assert!(
            attrs.contains_key("client_id"),
            "there should be a `client_id` attribute"
        );
    }

    #[test]
    fn errors_when_tkn_missing_src() {
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
        let entity_type = EntityType {
            member_of: None,
            tags: None,
            shape: Some(EntityShape {
                required: true,
                attrs: HashMap::from([("client_id".to_string(), Attribute::string())]),
            }),
        };
        let iss = TrustedIssuer::default();
        let token = Token::new("access_token", HashMap::new().into(), Some(&iss));

        let err = build_entity_attrs_from_tkn(
            &schema,
            &entity_type,
            &token,
            None,
            Vec::new(),
        )
        .expect_err("should error due to missing source");
        assert!(
            matches!(
                err,
                BuildAttrError {
                    attr_name: ref name,
                    source: BuildAttrErrorKind::BuildExpression(BuildExprError::MissingSource(ref src_name))}
                        if name == "client_id" &&
                           src_name == "client_id"
            ),
            "expected MissingSource error but got: {:?}",
            err,
        );
    }

    #[test]
    fn can_build_entity_attrs_from_value() {
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
        let entity_type = EntityType {
            member_of: None,
            tags: None,
            shape: Some(EntityShape {
                required: true,
                attrs: HashMap::from([("client_id".to_string(), Attribute::string())]),
            }),
        };
        let src_values = HashMap::from([("client_id".to_string(), json!("workload-123"))]);

        let attrs =
            build_entity_attrs_from_values(&schema, Some("Jans"), &entity_type, &src_values)
                .expect("should build entity attrs");
        // RestrictedExpression does not implement PartialEq so the best we can do is check
        // if the attribute was created
        assert!(
            attrs.contains_key("client_id"),
            "there should be a `client_id` attribute"
        );
    }

    #[test]
    fn errors_when_values_missing_src() {
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
        let entity_type = EntityType {
            member_of: None,
            tags: None,
            shape: Some(EntityShape {
                required: true,
                attrs: HashMap::from([("client_id".to_string(), Attribute::string())]),
            }),
        };
        let src_values = HashMap::new();

        let err = build_entity_attrs_from_values(&schema, Some("Jans"), &entity_type, &src_values)
            .expect_err("should error due to missing source");
        assert!(
            matches!(
                err, 
                BuildAttrError{
                    attr_name: ref name, 
                    source: BuildAttrErrorKind::MissingSource(ref src_name)} 
                        if name == "client_id" && 
                           src_name == "client_id"),
            "expected MissingSource error but got: {:?}",
            err,
        );
    }
}
