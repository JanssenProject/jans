// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::policy_store::ClaimMappings;
use crate::entity_builder::schema::AttrSrc;
use cedar_policy::RestrictedExpression;
use serde_json::Value;
use smol_str::{SmolStr, ToSmolStr};
use std::collections::HashMap;

use super::built_entities::BuiltEntities;
use super::schema::AttrsShape;
use super::value_to_expr::value_to_expr;
use super::{BuildAttrsError, BuildAttrsErrorVec};

/// Builds Cedar entity attributes using the given attribute source
///
/// This function will *JOIN* the token attributes built from each token
pub fn build_entity_attrs(
    attrs_src: &HashMap<String, Value>,
    entities: &BuiltEntities,
    attrs_shape: Option<&HashMap<SmolStr, AttrsShape>>,
    claim_mappings: Option<&ClaimMappings>,
) -> Result<HashMap<String, RestrictedExpression>, BuildAttrsErrorVec> {
    if let Some(attrs_shape) = attrs_shape {
        build_entity_attrs_with_shape(attrs_src, entities, attrs_shape, claim_mappings)
    } else {
        build_entity_attrs_without_schema(attrs_src, claim_mappings)
    }
}

/// Uses the schema as guide in building the attributes
pub fn build_entity_attrs_with_shape(
    attrs_src: &HashMap<String, Value>,
    entities: &BuiltEntities,
    attrs_shape: &HashMap<SmolStr, AttrsShape>,
    claim_mappings: Option<&ClaimMappings>,
) -> Result<HashMap<String, RestrictedExpression>, BuildAttrsErrorVec> {
    let mut errs = Vec::new();
    let mut attrs = HashMap::new();

    for (attr_name, attr_req) in attrs_shape.iter() {
        match attr_req.src() {
            AttrSrc::JwtClaim(claim_src) => {
                let mut required_missing_claims: Vec<SmolStr> = Vec::new();

                // skip if the source couldn't be found and was not required
                let Some(src) = attrs_src.get(attr_name.as_str()) else {
                    if attr_req.is_required() {
                        required_missing_claims.push(attr_name.to_smolstr());
                    }

                    continue;
                };

                if !required_missing_claims.is_empty() {
                    errs.push(BuildAttrsError::MissingClaims(required_missing_claims));
                    continue;
                }

                let Some(mapping) = claim_mappings.and_then(|m| m.mapping(attr_name)) else {
                    // without claim mapping
                    match claim_src.build_expr(src) {
                        Ok(Some(expr)) => {
                            attrs.insert(attr_name.to_string(), expr);
                        },
                        Err(e) => {
                            errs.push(BuildAttrsError::from(e));
                        },
                        _ => {},
                    }
                    continue;
                };

                // with claim mapping
                let mapped_src = mapping.apply_mapping_value(src);
                match claim_src.build_expr(&mapped_src) {
                    Ok(Some(expr)) => {
                        attrs.insert(attr_name.to_string(), expr);
                    },
                    Err(e) => {
                        errs.push(BuildAttrsError::from(e));
                    },
                    _ => {},
                }
            },
            AttrSrc::EntityRef(entity_ref_src) => {
                let mut missing_refs: Vec<SmolStr> = Vec::new();

                // skip if the source couldn't be found and was not required
                let Some(id) = entities.get_single(entity_ref_src) else {
                    if attr_req.is_required() {
                        missing_refs.push((*entity_ref_src).clone());
                    }
                    continue;
                };

                if !missing_refs.is_empty() {
                    errs.push(BuildAttrsError::MissingEntityRefs(missing_refs));
                    continue;
                }

                match entity_ref_src.build_expr(id) {
                    Ok(src) => {
                        attrs.insert(attr_name.to_string(), src);
                    },
                    Err(e) => {
                        errs.push(BuildAttrsError::from(e));
                    },
                }
            },
            AttrSrc::EntityRefSet(entity_ref_set_src) => {
                let mut missing_refs: Vec<SmolStr> = Vec::new();

                let Some(eids) = entities.get_multiple(entity_ref_set_src) else {
                    if attr_req.is_required() {
                        missing_refs.push((*entity_ref_set_src).clone());
                    }
                    continue;
                };

                if !missing_refs.is_empty() {
                    errs.push(BuildAttrsError::MissingEntityRefs(missing_refs));
                    continue;
                }

                match entity_ref_set_src.build_expr(eids) {
                    Ok(src) => {
                        attrs.insert(attr_name.to_string(), src);
                    },
                    Err(e) => {
                        errs.push(BuildAttrsError::from(e));
                    },
                }
            },
        }
    }

    Ok(attrs)
}

/// Will do it's best to create the entity without a schema
pub fn build_entity_attrs_without_schema(
    attrs_src: &HashMap<String, Value>,
    claim_mappings: Option<&ClaimMappings>,
) -> Result<HashMap<String, RestrictedExpression>, BuildAttrsErrorVec> {
    let mut errs = Vec::new();
    let mut attrs = HashMap::new();

    for (name, src) in attrs_src.iter() {
        let Some(mappings) = claim_mappings.and_then(|m| m.mapping(name)) else {
            // without claim mapping
            match value_to_expr(src) {
                Ok(Some(expr)) => {
                    attrs.insert(name.to_string(), expr);
                },
                Err(e) => {
                    errs.push(e);
                },
                _ => {},
            }
            continue;
        };

        // with claim mapping
        let mapped_src = mappings.apply_mapping_value(src);
        match value_to_expr(&mapped_src) {
            Ok(Some(expr)) => {
                attrs.insert(name.to_string(), expr);
            },
            Err(e) => {
                errs.push(e);
            },
            _ => {},
        }
    }

    if errs.is_empty() {
        Ok(attrs)
    } else {
        Err(BuildAttrsErrorVec::from(
            errs.into_iter().flatten().collect::<Vec<_>>(),
        ))
    }
}

#[cfg(test)]
mod test {
    use super::super::test::assert_entity_eq;
    use super::*;
    use crate::{common::policy_store::ClaimMappings, entity_builder::schema::MappingSchema};
    use cedar_policy::{Entity, Schema};
    use cedar_policy_validator::ValidatorSchema;
    use serde_json::json;
    use std::{collections::HashSet, str::FromStr};

    #[test]
    fn can_build_entity_with_schema() {
        let schema_src = r#"
        namespace SomeNamespace {
            entity AnotherEntity;
            entity SomeEntity {
                bool_attr: Bool,
                str_attr: String,
                long_attr: Long,
                set_bool_attr: Set<Bool>,
                set_set_bool_attr: Set<Set<Bool>>,
                record_attr: {
                    inner_record_attr: Bool,
                },
                record_record_attr: {
                    inner_record_attr: {
                        inner_inner_record_attr: Bool,
                    },
                },
                entity_ref_attr: AnotherEntity,
                decimal_attr: decimal,
                ip_attr: ipaddr,
                optional_attr?: Bool,
            };
        }
        "#;
        let cedar_schema = Schema::from_str(schema_src).expect("builds cedar Schema");
        let mapping_schema: MappingSchema = (&ValidatorSchema::from_str(schema_src)
            .expect("builds ValidatorSchema"))
            .try_into()
            .expect("builds MappingSchema");
        let entity_name = "SomeNamespace::SomeEntity";
        let attrs_src = HashMap::from([
            ("bool_attr".into(), json!(true)),
            ("str_attr".into(), json!("some_str")),
            ("long_attr".into(), json!(1234)),
            ("set_bool_attr".into(), json!([true, true])),
            ("set_set_bool_attr".into(), json!([[true], [true]])),
            ("record_attr".into(), json!({"inner_record_attr": true})),
            (
                "record_record_attr".into(),
                json!({"inner_record_attr": {"inner_inner_record_attr": true}}),
            ),
            ("decimal_attr".into(), json!("0.0")),
            ("ip_attr".into(), json!("0.0.0.0")),
        ]);
        let mut built_entities = BuiltEntities::default();
        built_entities.insert(
            &"SomeNamespace::AnotherEntity::\"another_id\""
                .parse()
                .expect("a valid entity uid"),
        );

        let attrs_shape = mapping_schema
            .get_entity_shape(entity_name)
            .expect("get entity requirements");
        let attrs = build_entity_attrs(&attrs_src, &built_entities, Some(&attrs_shape), None)
            .expect("builds entity attrs");

        let dummy_entity = Entity::new(
            "SomeNamespace::SomeEntity::\"some_id\""
                .parse()
                .expect("a valid entity uid"),
            attrs,
            HashSet::new(),
        )
        .expect("builds dummy entity");

        assert_entity_eq(
            &dummy_entity,
            json!({
                "uid": {"type": "SomeNamespace::SomeEntity", "id": "some_id"},
                "attrs": {
                    "bool_attr": true,
                    "str_attr": "some_str",
                    "long_attr": 1234,
                    "set_bool_attr": [true],
                    "set_set_bool_attr": [[true]],
                    "record_attr": {
                        "inner_record_attr": true,
                    },
                    "record_record_attr": {
                        "inner_record_attr": {
                            "inner_inner_record_attr": true,
                        },
                    },
                    "entity_ref_attr": {"__entity": {
                        "type": "SomeNamespace::AnotherEntity", "id": "another_id",
                    }},
                    "decimal_attr": {"__extn": {
                        "fn": "decimal",
                        "arg": "0.0",
                    }},
                    "ip_attr": {"__extn": {
                        "fn": "ip",
                        "arg": "0.0.0.0",
                    }},
                },
                "parents": [],
            }),
            Some(&cedar_schema),
        );
    }

    #[test]
    fn can_build_with_claim_mappings_with_schema() {
        let schema_src = r#"
        type Email = {
            "uid": String,
            "domain": String,
        };
        type Url = {
            "scheme": String,
            "domain": String,
            "path": String,
        };
        entity SomeEntity {
            email_attr: Email,
            url_attr: Url,
        };
        "#;
        let claim_mappings = serde_json::from_value::<ClaimMappings>(json!({
            "email_attr": {
                "parser": "regex",
                "type": "Jans::Email",
                "regex_expression" : "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
                "UID": {"attr": "uid", "type":"String"},
                "DOMAIN": {"attr": "domain", "type":"String"},
            },
            "url_attr": {
                "parser": "regex",
                "type": "Jans::Url",
                "regex_expression": r#"^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\/\/(?P<DOMAIN>[^\/]+)(?P<PATH>\/.*)?$"#,
                "SCHEME": {"attr": "scheme", "type": "String"},
                "DOMAIN": {"attr": "domain", "type": "String"},
                "PATH": {"attr": "path", "type": "String"}
            }
        })).expect("valid claim mappings");
        let cedar_schema = Schema::from_str(schema_src).expect("builds cedar Schema");
        let mapping_schema: MappingSchema = (&ValidatorSchema::from_str(schema_src)
            .expect("builds ValidatorSchema"))
            .try_into()
            .expect("builds MappingSchema");
        let entity_name = "SomeEntity";
        let attrs_src = HashMap::from([
            ("email_attr".into(), json!("test@example.com")),
            (
                "url_attr".into(),
                json!("https://test.example.com/some_path"),
            ),
        ]);
        let built_entities = BuiltEntities::default();

        let attrs_shape = mapping_schema
            .get_entity_shape(entity_name)
            .expect("get entity requirements");
        let attrs = build_entity_attrs(
            &attrs_src,
            &built_entities,
            Some(&attrs_shape),
            Some(&claim_mappings),
        )
        .expect("builds entity attrs");

        let dummy_entity = Entity::new(
            "SomeEntity::\"some_id\""
                .parse()
                .expect("a valid entity uid"),
            attrs,
            HashSet::new(),
        )
        .expect("builds dummy entity");

        assert_entity_eq(
            &dummy_entity,
            json!({
                "uid": {"type": "SomeEntity", "id": "some_id"},
                "attrs": {
                    "email_attr": {
                        "uid": "test",
                        "domain": "example.com",
                    },
                    "url_attr": {
                        "scheme": "https",
                        "domain": "test.example.com",
                        "path": "/some_path",
                    },
                },
                "parents": [],
            }),
            Some(&cedar_schema),
        );
    }

    #[test]
    fn can_build_with_claim_mappings_without_schema() {
        let claim_mappings = serde_json::from_value::<ClaimMappings>(json!({
            "email_attr": {
                "parser": "regex",
                "type": "Jans::Email",
                "regex_expression" : "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
                "UID": {"attr": "uid", "type":"String"},
                "DOMAIN": {"attr": "domain", "type":"String"},
            },
            "url_attr": {
                "parser": "regex",
                "type": "Jans::Url",
                "regex_expression": r#"^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\/\/(?P<DOMAIN>[^\/]+)(?P<PATH>\/.*)?$"#,
                "SCHEME": {"attr": "scheme", "type": "String"},
                "DOMAIN": {"attr": "domain", "type": "String"},
                "PATH": {"attr": "path", "type": "String"}
            }
        })).expect("valid claim mappings");
        let attrs_src = HashMap::from([
            ("email_attr".into(), json!("test@example.com")),
            (
                "url_attr".into(),
                json!("https://test.example.com/some_path"),
            ),
        ]);
        let built_entities = BuiltEntities::default();

        let attrs = build_entity_attrs(&attrs_src, &built_entities, None, Some(&claim_mappings))
            .expect("builds entity attrs");

        let dummy_entity = Entity::new(
            "SomeEntity::\"some_id\""
                .parse()
                .expect("a valid entity uid"),
            attrs,
            HashSet::new(),
        )
        .expect("builds dummy entity");

        assert_entity_eq(
            &dummy_entity,
            json!({
                "uid": {"type": "SomeEntity", "id": "some_id"},
                "attrs": {
                    "email_attr": {
                        "uid": "test",
                        "domain": "example.com",
                    },
                    "url_attr": {
                        "scheme": "https",
                        "domain": "test.example.com",
                        "path": "/some_path",
                    },
                },
                "parents": [],
            }),
            None,
        );
    }
}
