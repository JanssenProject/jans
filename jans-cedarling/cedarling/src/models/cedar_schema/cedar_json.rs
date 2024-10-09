//! Module contains the JSON representation of a [cedar_policy::Schema]  
//! Support translated schema from human representation to JSON via CLI version `cedar-policy-cli 4.1`.  
//! To translate human redable format to JSON via CLI use next command:  
//! `cedar translate-schema --direction cedar-to-json  -s .\cedar.schema`
//! [cedar json schema grammar](https://docs.cedarpolicy.com/schema/json-schema-grammar.html) - documentation about json structure of cedar schema.

use std::collections::HashMap;

/// JSON representation of a [`cedar_policy::Schema`]
#[derive(Debug, Clone, serde::Deserialize, PartialEq)]
pub(crate) struct CedarSchemaJson {
    #[serde(flatten)]
    pub namespace: HashMap<String, CedarSchemaEntities>,
}

/// CedarSchemaEntities hold all entities and their shapes in the namespace.
//
// It may contain more fields, but we don't need all of them.
#[derive(Debug, Clone, serde::Deserialize, PartialEq)]
pub struct CedarSchemaEntities {
    #[serde(rename = "entityTypes")]
    pub entity_types: HashMap<String, CedarSchemaEntityShape>,
}

/// CedarSchemaEntityShape hold shape of an entity.
#[derive(Debug, Clone, serde::Deserialize, PartialEq)]
pub struct CedarSchemaEntityShape {
    pub shape: Option<CedarSchemaEntityType>,
}

/// CedarSchemaEntityType defines type name and attributes for an entity.
#[derive(Debug, Clone, serde::Deserialize, PartialEq)]
pub struct CedarSchemaEntityType {
    #[serde(rename = "type")]
    pub entity_type: String,
    pub attributes: HashMap<String, CedarSchemaEntityAttribute>,
}

/// CedarSchemaEntityAttribute defines possible type variants of the entity attribute.
/// [cedar json schema grammar](https://docs.cedarpolicy.com/schema/json-schema-grammar.html)
/// Type ::= Primitive | Set | EntityRef | Record | Extension | EntityOrCommon
#[derive(Debug, Clone, serde::Deserialize, PartialEq)]
#[serde(untagged)]
pub enum CedarSchemaEntityAttribute {
    // Typed should be first to match correct fom json
    Typed(EntityType),
    Primitive(PrimitiveType),
}

/// The Primitive element describes  
/// Primitive ::= '"type":' ('"Long"' | '"String"' | '"Boolean"' | TYPENAME)  
/// Based on the [cedar json schema grammar](https://docs.cedarpolicy.com/schema/json-schema-grammar.html)
#[derive(Debug, Clone, serde::Deserialize, PartialEq)]
pub struct PrimitiveType {
    #[serde(rename = "type")]
    kind: String,
}

/// Based on the [cedar json schema grammar](https://docs.cedarpolicy.com/schema/json-schema-grammar.html)
/// This structure can hold `Extension`, `EntityOrCommon`, `EntityRef`
#[derive(Debug, Clone, serde::Deserialize, PartialEq)]
pub struct EntityType {
    #[serde(rename = "type")]
    kind: String,
    name: String,
}

#[cfg(test)]
mod tests {
    use super::*;
    use pretty_assertions::assert_eq;

    /// Test to parse the cedar json schema
    /// to debug deserialize the schema
    #[test]
    fn parse_correct_example() {
        let json_value = include_str!("test_data_cedar.json");

        let parsed_cedar_schema: CedarSchemaJson =
            serde_json::from_str(json_value).expect("failed to parse json");

        let schema_to_compare = CedarSchemaJson {
            namespace: HashMap::from_iter(vec![(
                "Jans".to_string(),
                CedarSchemaEntities {
                    entity_types: HashMap::from_iter(vec![(
                        "Access_token".to_string(),
                        CedarSchemaEntityShape {
                            shape: Some(CedarSchemaEntityType {
                                entity_type: "Record".to_string(),
                                attributes: HashMap::from_iter(vec![
                                    (
                                        "aud".to_string(),
                                        CedarSchemaEntityAttribute::Typed(EntityType {
                                            kind: "EntityOrCommon".to_string(),
                                            name: "String".to_string(),
                                        }),
                                    ),
                                    (
                                        "exp".to_string(),
                                        CedarSchemaEntityAttribute::Typed(EntityType {
                                            kind: "EntityOrCommon".to_string(),
                                            name: "Long".to_string(),
                                        }),
                                    ),
                                    (
                                        "iat".to_string(),
                                        CedarSchemaEntityAttribute::Primitive(PrimitiveType {
                                            kind: "Long".to_string(),
                                        }),
                                    ),
                                ]),
                            }),
                        },
                    )]),
                },
            )]),
        };

        assert_eq!(schema_to_compare, parsed_cedar_schema)
    }
}
