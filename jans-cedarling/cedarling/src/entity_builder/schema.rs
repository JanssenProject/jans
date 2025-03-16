// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod attr_src;

use attr_src::BuildAttrSrcError;
use cedar_policy_validator::ValidatorSchema;
use smol_str::SmolStr;
use std::{collections::HashMap, fmt::Display};
use thiserror::Error;

pub use attr_src::*;

use super::PartitionResult;

type EntityTypeName = String;
type AttrName = SmolStr;

/// Cedar Schema wrapper that makes retrieving the instructions on how to build an
/// entity easier
pub struct MappingSchema {
    entities: HashMap<EntityTypeName, HashMap<AttrName, AttrsShape>>,
}

impl TryFrom<&ValidatorSchema> for MappingSchema {
    type Error = BuildMappingSchemaError;

    fn try_from(src: &ValidatorSchema) -> Result<Self, Self::Error> {
        let mut entities = HashMap::new();

        for (type_name, entity_type) in src.entity_type_names().filter_map(|entity_type| {
            src.get_entity_type(entity_type)
                .map(|validator_entity_type| {
                    (entity_type.name().to_string(), validator_entity_type)
                })
        }) {
            let (attrs, errs): (Vec<_>, Vec<_>) = entity_type
                .attributes()
                .iter()
                .map(|(attr_name, attr_type)| {
                    AttrSrc::from_type(attr_name, &attr_type.attr_type).map(|attr_src| {
                        (attr_name.clone(), AttrsShape {
                            is_required: attr_type.is_required(),
                            attr_src,
                        })
                    })
                })
                .partition_result();

            if !errs.is_empty() {
                return Err(BuildMappingSchemaError(errs));
            }

            entities.insert(type_name, attrs.into_iter().collect());
        }

        Ok(Self { entities })
    }
}

#[derive(Debug, Error)]
pub struct BuildMappingSchemaError(Vec<BuildAttrSrcError>);

impl Display for BuildMappingSchemaError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:?}", self.0.iter().map(|e| e.to_string()))
    }
}

impl MappingSchema {
    /// Returns the entity's shape if the entity exists in the schema but
    /// returns `None` for unknown entities.
    pub fn get_entity_shape(&self, type_name: &str) -> Option<&HashMap<AttrName, AttrsShape>> {
        self.entities.get(type_name)
    }
}

/// Info on how to build the attributes
#[derive(Debug)]
pub struct AttrsShape {
    is_required: bool,
    attr_src: AttrSrc,
}

impl AttrsShape {
    pub fn is_required(&self) -> bool {
        self.is_required
    }

    pub fn src(&self) -> &AttrSrc {
        &self.attr_src
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::entity_builder::schema::attr_src::TknClaimAttrSrc;
    use cedar_policy_validator::ValidatorSchema;
    use std::{collections::HashSet, str::FromStr};
    use test_utils::assert_eq;

    #[test]
    fn parse_valid_schema() {
        let schema = r#"
        namespace Jans {
            type TestType = {
                test_type_attr: String,
            };
            entity TestEntity = {
                bool_attr: Bool,
                opt_bool_attr?: Bool,
                str_attr: String,
                long_attr: Long,
                set_str_attr: Set<String>,
                set_set_str_attr: Set<Set<String>>,
                record_attr: {
                    inner_record_attr: String,
                },
                decimal_attr: decimal,
                ip_attr: ipaddr,
            };
        }
        "#;

        let schema: MappingSchema = (&ValidatorSchema::from_str(schema)
            // note that unknown types will be handled already by the implementation
            // of the cedar_policy_validator::ValidatorSchema's parser
            .expect("should parse validator schema"))
            .try_into()
            .expect("should build mapping schema");

        let entity_reqs = schema
            .get_entity_shape("Jans::TestEntity")
            .expect("should get `TestEntity` requirements");
        let attr_reqs = entity_reqs
            .iter()
            .map(|(attr_name, attr_req)| {
                let name = attr_name.clone();
                let src = attr_req.src();
                let is_required = attr_req.is_required();
                (name, (src, is_required))
            })
            .collect::<HashMap<SmolStr, (&AttrSrc, bool)>>();

        assert_eq!(
            attr_reqs.keys().cloned().collect::<HashSet<SmolStr>>(),
            [
                "bool_attr",
                "opt_bool_attr",
                "str_attr",
                "long_attr",
                "set_str_attr",
                "set_set_str_attr",
                "record_attr",
                "decimal_attr",
                "ip_attr",
            ]
            .iter()
            .map(SmolStr::new)
            .collect(),
            "the required keys should match the expected"
        );

        assert_eq!(
            attr_reqs.get("bool_attr").expect("has bool_attr"),
            &(
                &AttrSrc::JwtClaim(TknClaimAttrSrc {
                    claim: "bool_attr".to_string(),
                    expected_type: ExpectedClaimType::Bool
                }),
                true
            ),
            "incorrect Bool attr requirement"
        );

        assert_eq!(
            attr_reqs.get("opt_bool_attr").expect("has opt_bool_attr"),
            &(
                &AttrSrc::JwtClaim(TknClaimAttrSrc {
                    claim: "opt_bool_attr".to_string(),
                    expected_type: ExpectedClaimType::Bool
                }),
                false
            ),
            "incorrect optional Bool attr requirement"
        );

        assert_eq!(
            attr_reqs.get("str_attr").expect("has str_attr"),
            &(
                &AttrSrc::JwtClaim(TknClaimAttrSrc {
                    claim: "str_attr".to_string(),
                    expected_type: ExpectedClaimType::String
                }),
                true
            ),
            "incorrect String attr requirement"
        );

        assert_eq!(
            attr_reqs.get("long_attr").expect("has long_attr"),
            &(
                &AttrSrc::JwtClaim(TknClaimAttrSrc {
                    claim: "long_attr".to_string(),
                    expected_type: ExpectedClaimType::Number
                }),
                true
            ),
            "incorrect Long attr requirement"
        );

        assert_eq!(
            attr_reqs.get("set_str_attr").expect("has set_str_attr"),
            &(
                &AttrSrc::JwtClaim(TknClaimAttrSrc {
                    claim: "set_str_attr".to_string(),
                    expected_type: ExpectedClaimType::Array(Box::new(ExpectedClaimType::String)),
                }),
                true
            ),
            "incorrect Set<String> attr requirement"
        );

        assert_eq!(
            attr_reqs
                .get("set_set_str_attr")
                .expect("has set_set_str_attr"),
            &(
                &AttrSrc::JwtClaim(TknClaimAttrSrc {
                    claim: "set_set_str_attr".to_string(),
                    expected_type: ExpectedClaimType::Array(Box::new(ExpectedClaimType::Array(
                        Box::new(ExpectedClaimType::String)
                    ))),
                }),
                true
            ),
            "incorrect Set<String> attr requirement"
        );

        assert_eq!(
            attr_reqs.get("record_attr").expect("has record_attr"),
            &(
                &AttrSrc::JwtClaim(TknClaimAttrSrc {
                    claim: "record_attr".to_string(),
                    expected_type: ExpectedClaimType::Object(HashMap::from([(
                        "inner_record_attr".into(),
                        ExpectedClaimType::String
                    )]))
                }),
                true
            ),
            "incorrect Record attr requirement"
        );

        assert_eq!(
            attr_reqs.get("decimal_attr").expect("has decimal_attr"),
            &(
                &AttrSrc::JwtClaim(TknClaimAttrSrc {
                    claim: "decimal_attr".to_string(),
                    expected_type: ExpectedClaimType::Extension("decimal".into())
                }),
                true
            ),
            "incorrect decimal_attr requirement"
        );

        assert_eq!(
            attr_reqs.get("ip_attr").expect("has ip_attr"),
            &(
                &AttrSrc::JwtClaim(TknClaimAttrSrc {
                    claim: "ip_attr".to_string(),
                    expected_type: ExpectedClaimType::Extension("ipaddr".into())
                }),
                true
            ),
            "incorrect ip_attr requirement"
        );
    }
}
