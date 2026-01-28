// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

mod attr_src;

use attr_src::BuildAttrSrcError;
use cedar_policy_core::validator::ValidatorSchema;
use smol_str::SmolStr;
use std::{collections::HashMap, fmt::Display};
use thiserror::Error;

pub(super) use attr_src::*;

use super::PartitionResult;

type EntityTypeName = String;
type AttrName = SmolStr;

/// Cedar Schema wrapper that makes retrieving the instructions on how to build an
/// entity easier
pub(super) struct MappingSchema {
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
                        (
                            attr_name.clone(),
                            AttrsShape {
                                is_required: attr_type.is_required(),
                                attr_src,
                            },
                        )
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
        write!(
            f,
            "{:?}",
            self.0.iter().map(ToString::to_string).collect::<Vec<_>>()
        )
    }
}

impl MappingSchema {
    /// Returns the entity's shape if the entity exists in the schema but
    /// returns `None` for unknown entities.
    pub(super) fn get_entity_shape(
        &self,
        type_name: &str,
    ) -> Option<&HashMap<AttrName, AttrsShape>> {
        self.entities.get(type_name)
    }
}

/// Info on how to build the attributes
#[derive(Debug)]
pub(super) struct AttrsShape {
    is_required: bool,
    attr_src: AttrSrc,
}

impl AttrsShape {
    pub(super) fn is_required(&self) -> bool {
        self.is_required
    }

    pub(super) fn src(&self) -> &AttrSrc {
        &self.attr_src
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::entity_builder::schema::attr_src::TknClaimAttrSrc;
    use cedar_policy_core::validator::ValidatorSchema;
    use std::str::FromStr;
    use test_utils::assert_eq;

    #[test]
    fn parse_valid_schema() {
        let schema = r"
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
        ";

        let schema: MappingSchema = (&ValidatorSchema::from_str(schema)
            // note that unknown types will be handled already by the implementation
            // of the cedar_policy_core::validator::ValidatorSchema's parser
            .expect("should parse validator schema"))
            .try_into()
            .expect("should build mapping schema");

        let entity_reqs = schema
            .get_entity_shape("Jans::TestEntity")
            .expect("should get `TestEntity` requirements");

        let test_cases = [
            ("bool_attr", ExpectedClaimType::Bool, true),
            ("opt_bool_attr", ExpectedClaimType::Bool, false),
            ("str_attr", ExpectedClaimType::String, true),
            ("long_attr", ExpectedClaimType::Number, true),
            (
                "set_str_attr",
                ExpectedClaimType::Array(Box::new(ExpectedClaimType::String)),
                true,
            ),
            (
                "set_set_str_attr",
                ExpectedClaimType::Array(Box::new(ExpectedClaimType::Array(Box::new(
                    ExpectedClaimType::String,
                )))),
                true,
            ),
            (
                "record_attr",
                ExpectedClaimType::Object(HashMap::from([(
                    "inner_record_attr".into(),
                    ExpectedClaimType::String,
                )])),
                true,
            ),
            (
                "decimal_attr",
                ExpectedClaimType::Extension("decimal".into()),
                true,
            ),
            (
                "ip_attr",
                ExpectedClaimType::Extension("ipaddr".into()),
                true,
            ),
        ];

        for (attr_name, expected_type, is_required) in test_cases {
            let attr_req = entity_reqs
                .get(&SmolStr::new(attr_name))
                .unwrap_or_else(|| panic!("missing attribute: {attr_name}"));

            assert_eq!(
                attr_req.is_required(),
                is_required,
                "{}: incorrect is_required",
                attr_name
            );

            let expected_src = AttrSrc::JwtClaim(TknClaimAttrSrc {
                claim: attr_name.to_string(),
                expected_type,
            });

            assert_eq!(
                attr_req.src(),
                &expected_src,
                "{}: incorrect AttrSrc",
                attr_name
            );
        }
    }
}
