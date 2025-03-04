// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::policy_store::ClaimMapping;
use crate::jwt::Token;
use cedar_policy::{ExpressionConstructionError, RestrictedExpression};
use serde_json::{Value, json};
use std::collections::HashMap;
use std::collections::hash_map::Entry;

use super::BuildEntityErrorKind;
use super::value_to_expr::value_to_expr;

/// Builds Cedar entity attributes using the given JWTs
///
/// This function will *JOIN* the token attributes built from each token
pub fn build_entity_attrs(
    attrs_src: EntityAttrsSrc,
) -> Result<HashMap<String, RestrictedExpression>, BuildEntityErrorKind> {
    let mut attrs = HashMap::new();

    for src in attrs_src.0.iter() {
        let value = match src.mapping {
            Some(mapping) => mapping.apply_mapping_value(src.value),
            None => src.value.clone(),
        };

        const STACKABLE_ATTRS: &[&str] = &["role", "group", "memberOf"];
        match attrs.entry(src.attr_name) {
            Entry::Occupied(mut entry) => {
                if let Value::Array(vec) = entry.get_mut() {
                    vec.push(value);
                } else if STACKABLE_ATTRS.contains(&src.attr_name) {
                    entry.insert(json!([entry.get().clone(), value]));
                }
            },
            Entry::Vacant(entry) => {
                entry.insert(value);
            },
        }
    }

    let (attrs, errors): (Vec<_>, Vec<_>) = attrs
        .iter()
        .map(|(name, val)| value_to_expr(val).map(|expr| (name.to_string(), expr)))
        .partition(Result::is_ok);

    if !errors.is_empty() {
        let errors = errors
            .into_iter()
            .flat_map(|e| e.unwrap_err())
            .collect::<Vec<ExpressionConstructionError>>();
        return Err(BuildEntityErrorKind::BuildAttrs(errors.into()));
    }

    let attrs = attrs
        .into_iter()
        .flat_map(|attr| {
            let (name, value) = attr.unwrap();
            value.map(|value| (name, value))
        })
        .collect::<HashMap<String, RestrictedExpression>>();
    Ok(attrs)
}

pub struct EntityAttrsSrc<'a>(Vec<EntityAttrSrc<'a>>);

pub struct EntityAttrSrc<'a> {
    pub attr_name: &'a str,
    pub value: &'a Value,
    pub mapping: Option<&'a ClaimMapping>,
}

impl<'a> EntityAttrsSrc<'a> {
    pub fn new(
        tokens: &'a HashMap<String, Token>,
        src_tkns: &'a [&'a str],
        src_claims: &'a [&'a str],
    ) -> Self {
        let mut srcs = Vec::new();
        for token in src_tkns.iter().flat_map(|name| tokens.get(*name)) {
            for name in src_claims.iter() {
                if let Some(value) = token.get_claim_val(name) {
                    srcs.push(EntityAttrSrc {
                        attr_name: name,
                        value,
                        mapping: token.claim_mapping(name),
                    });
                } else {
                    continue;
                }
            }
        }
        Self(srcs)
    }
}

impl<'a> From<&'a Token<'a>> for EntityAttrsSrc<'a> {
    fn from(token: &'a Token<'a>) -> Self {
        let srcs = token
            .claims_value()
            .iter()
            .map(|(name, value)| EntityAttrSrc {
                attr_name: name,
                value,
                mapping: token.claim_mapping(name),
            })
            .collect();
        Self(srcs)
    }
}

impl<'a> From<&'a HashMap<String, Value>> for EntityAttrsSrc<'a> {
    fn from(hash_map: &'a HashMap<String, Value>) -> Self {
        let srcs = hash_map
            .iter()
            .map(|(name, value)| EntityAttrSrc {
                attr_name: name,
                value,
                mapping: None,
            })
            .collect();
        Self(srcs)
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::authz::entity_builder::test::assert_entity_eq;
    use crate::common::policy_store::ClaimMappings;
    use cedar_policy::{Entity, EntityUid};
    use serde_json::json;
    use std::{collections::HashSet, str::FromStr};

    #[test]
    fn can_build_expression_with_regex_mapping() {
        let claim_mappings = serde_json::from_value::<ClaimMappings>(json!({
            "email": {
                "parser": "regex",
                "type": "Jans::Email",
                "regex_expression" : "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
                "UID": {"attr": "uid", "type":"String"},
                "DOMAIN": {"attr": "domain", "type":"String"},
            },
            "url": {
                "parser": "regex",
                "type": "Jans::Url",
                "regex_expression": r#"^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\/\/(?P<DOMAIN>[^\/]+)(?P<PATH>\/.*)?$"#,
                "SCHEME": {"attr": "scheme", "type": "String"},
                "DOMAIN": {"attr": "domain", "type": "String"},
                "PATH": {"attr": "path", "type": "String"}
            }
        })).unwrap();

        let client_id = json!("some_client");
        let email = json!("test@example.com");
        let url = json!("https://test.example.com/some_path");
        let attr_srcs = EntityAttrsSrc(Vec::from([
            EntityAttrSrc {
                attr_name: "client_id",
                value: &client_id,
                mapping: None,
            },
            EntityAttrSrc {
                attr_name: "email",
                value: &email,
                mapping: claim_mappings.mapping("email"),
            },
            EntityAttrSrc {
                attr_name: "url",
                value: &url,
                mapping: claim_mappings.mapping("url"),
            },
        ]));
        let attrs = build_entity_attrs(attr_srcs.into()).expect("should build entity attributes");

        let entity = Entity::new(
            EntityUid::from_str("Workload::\"some_workload\"")
                .expect("should parse EnityUid from str"),
            attrs,
            HashSet::new(),
        )
        .expect("should build entity");

        assert_entity_eq(
            &entity,
            json!({
                "uid":{"type": "Workload", "id": "some_workload"},
                "attrs": {
                    "client_id": "some_client",
                    "email": {
                        "uid": "test",
                        "domain": "example.com",
                    },
                    "url": {
                        "scheme": "https",
                        "domain": "test.example.com",
                        "path": "/some_path",
                    },
                },
                "parents": []
            }),
            None,
        );
    }
}
