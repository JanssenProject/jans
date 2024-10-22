//! Module contains the JSON representation of a [cedar_policy::Schema]  
//! Support translated schema from human representation to JSON via CLI version `cedar-policy-cli 4.1`.  
//! To translate human redable format to JSON via CLI use next command:  
//! `cedar translate-schema --direction cedar-to-json  -s .\cedar.schema`
//! [cedar json schema grammar](https://docs.cedarpolicy.com/schema/json-schema-grammar.html) - documentation about json structure of cedar schema.

use std::collections::HashMap;

/// JSON representation of a [`cedar_policy::Schema`]
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub(crate) struct CedarSchemaJson {
    #[serde(flatten)]
    pub namespace: HashMap<String, CedarSchemaEntities>,
}

impl CedarSchemaJson {
    /// Get schema record by namespace name and entity type name
    pub fn entity_schema_record(
        &self,
        namespace: &str,
        typename: &str,
    ) -> Option<&CedarSchemaRecord> {
        let namespace = self.namespace.get(namespace)?;
        let entity_shape = namespace.entity_types.get(typename)?;

        entity_shape.shape.as_ref()
    }
}

/// CedarSchemaEntities hold all entities and their shapes in the namespace.
//
// It may contain more fields, but we don't need all of them.
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub struct CedarSchemaEntities {
    #[serde(rename = "entityTypes")]
    pub entity_types: HashMap<String, CedarSchemaEntityShape>,
}

/// CedarSchemaEntityShape hold shape of an entity.
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub struct CedarSchemaEntityShape {
    pub shape: Option<CedarSchemaRecord>,
}

/// CedarSchemaRecord defines type name and attributes for an entity.
/// Record ::= '"type": "Record", "attributes": {' [ RecordAttr { ',' RecordAttr } ] '}'
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub struct CedarSchemaRecord {
    #[serde(rename = "type")]
    pub entity_type: String,
    // represent RecordAttr
    // RecordAttr ::= STR ': {' Type [',' '"required"' ':' ( true | false )] '}'
    pub attributes: HashMap<String, CedarSchemaEntityAttribute>,
}

impl CedarSchemaRecord {
    // if we want to create entity from attributes it should be record
    pub fn is_record(&self) -> bool {
        self.entity_type == "Record"
    }
}

/// CedarSchemaRecordAttr defines possible type variants of the entity attribute.
/// RecordAttr ::= STR ': {' Type [',' '"required"' ':' ( true | false )] '}'
#[derive(Debug, Clone, PartialEq, serde::Serialize)]
pub struct CedarSchemaEntityAttribute {
    cedar_type: CedarSchemaEntityType,
    required: bool,
}

impl<'de> serde::Deserialize<'de> for CedarSchemaEntityAttribute {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value: serde_json::Value = serde::Deserialize::deserialize(deserializer)?;

        // used only for deserialization
        #[derive(serde::Deserialize)]
        pub struct IsRequired {
            required: Option<bool>,
        }

        let is_required = IsRequired::deserialize(&value).map_err(|err| {
            serde::de::Error::custom(format!(
                "could not deserialize CedarSchemaEntityAttribute, field 'is_required':{}",
                err
            ))
        })?;

        let cedar_type = CedarSchemaEntityType::deserialize(value)
            .map_err(|err| serde::de::Error::custom(err))?;

        Ok(CedarSchemaEntityAttribute {
            cedar_type,
            required: is_required.required.unwrap_or(true),
        })
    }
}

#[derive(Debug, Clone, PartialEq, serde::Serialize)]
pub enum CedarSchemaEntityType {
    Set(Box<SetEntityType>),
    Typed(EntityType),
    Primitive(PrimitiveType),
}

impl CedarSchemaEntityAttribute {
    pub fn is_required(&self) -> bool {
        self.required
    }

    pub fn get_type(&self) -> Option<PrimitiveTypeKind> {
        match &self.cedar_type {
            CedarSchemaEntityType::Set(_) => None,
            CedarSchemaEntityType::Typed(entity_type) => entity_type.get_type(),
            CedarSchemaEntityType::Primitive(primitive_type) => Some(primitive_type.kind.clone()),
        }
    }
}

impl<'de> serde::Deserialize<'de> for CedarSchemaEntityType {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        // is used only on deserialization.
        #[derive(serde::Deserialize)]
        struct TypeStruct {
            #[serde(rename = "type")]
            type_name: String,
        }

        let value: serde_json::Value = serde::Deserialize::deserialize(deserializer)?;

        let entity_type = match TypeStruct::deserialize(&value)
            .map_err(serde::de::Error::custom)?
            .type_name
            .as_str()
        {
            "Set" => {
                CedarSchemaEntityType::Set(Box::new(SetEntityType::deserialize(&value).map_err(
                    |err| serde::de::Error::custom(format!("failed to deserialize Set: {}", err)),
                )?))
            },
            "EntityOrCommon" => {
                CedarSchemaEntityType::Typed(EntityType::deserialize(&value).map_err(|err| {
                    serde::de::Error::custom(format!(
                        "failed to deserialize EntityOrCommon: {}",
                        err
                    ))
                })?)
            },
            _ => CedarSchemaEntityType::Primitive(PrimitiveType::deserialize(&value).map_err(
                |err| {
                    serde::de::Error::custom(format!(
                        "failed to deserialize PrimitiveType: {}",
                        err
                    ))
                },
            )?),
        };

        Ok(entity_type)
    }
}

/// The Primitive element describes  
/// Primitive ::= '"type":' ('"Long"' | '"String"' | '"Boolean"' | TYPENAME)  
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub struct PrimitiveType {
    #[serde(rename = "type")]
    kind: PrimitiveTypeKind,
}

/// Variants of primitive type.
/// Primitive ::= '"type":' ('"Long"' | '"String"' | '"Boolean"' | TYPENAME)  
#[derive(Debug, Clone, serde::Serialize, PartialEq)]
pub enum PrimitiveTypeKind {
    Long,
    String,
    Boolean,
    TypeName(String),
}

/// impement custom deserialization to deserialize it correctly
impl<'de> serde::Deserialize<'de> for PrimitiveTypeKind {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let s: String = serde::Deserialize::deserialize(deserializer)?;
        match s.as_str() {
            "Long" => Ok(PrimitiveTypeKind::Long),
            "String" => Ok(PrimitiveTypeKind::String),
            "Boolean" => Ok(PrimitiveTypeKind::Boolean),
            _ => Ok(PrimitiveTypeKind::TypeName(s)),
        }
    }
}

/// This structure can hold `Extension`, `EntityOrCommon`, `EntityRef`
#[derive(Debug, Clone, serde::Deserialize, serde::Serialize, PartialEq)]
pub struct EntityType {
    // it also can be primitive type
    #[serde(rename = "type")]
    kind: String,
    name: String,
}

impl EntityType {
    pub fn get_type(&self) -> Option<PrimitiveTypeKind> {
        if self.kind == "EntityOrCommon" {
            match self.name.as_str() {
                "Long" => Some(PrimitiveTypeKind::Long),
                "String" => Some(PrimitiveTypeKind::String),
                "Boolean" => Some(PrimitiveTypeKind::Boolean),
                type_name => Some(PrimitiveTypeKind::TypeName(type_name.to_string())),
            }
        } else {
            None
        }
    }
}

#[derive(Debug, Clone, serde::Deserialize, PartialEq, serde::Serialize)]
/// Describes the Set element
/// Set ::= '"type": "Set", "element": ' TypeJson
//
// "type": "Set" checked during deserialization
pub struct SetEntityType {
    element: CedarSchemaEntityType,
}

#[cfg(test)]
mod tests {
    use super::*;
    use test_utils::assert_eq;
    use test_utils::SortedJson;

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
                            shape: Some(CedarSchemaRecord {
                                entity_type: "Record".to_string(),
                                attributes: HashMap::from_iter(vec![
                                    (
                                        "aud".to_string(),
                                        CedarSchemaEntityAttribute {
                                            cedar_type: CedarSchemaEntityType::Typed(EntityType {
                                                kind: "EntityOrCommon".to_string(),
                                                name: "String".to_string(),
                                            }),
                                            required: true,
                                        },
                                    ),
                                    (
                                        "exp".to_string(),
                                        CedarSchemaEntityAttribute {
                                            cedar_type: CedarSchemaEntityType::Typed(EntityType {
                                                kind: "EntityOrCommon".to_string(),
                                                name: "Long".to_string(),
                                            }),
                                            required: true,
                                        },
                                    ),
                                    (
                                        "iat".to_string(),
                                        CedarSchemaEntityAttribute {
                                            cedar_type: CedarSchemaEntityType::Primitive(
                                                PrimitiveType {
                                                    kind: PrimitiveTypeKind::Long,
                                                },
                                            ),
                                            required: true,
                                        },
                                    ),
                                    (
                                        "scope".to_string(),
                                        CedarSchemaEntityAttribute {
                                            cedar_type: CedarSchemaEntityType::Set(Box::new(
                                                SetEntityType {
                                                    element: CedarSchemaEntityType::Typed(
                                                        EntityType {
                                                            kind: "EntityOrCommon".to_string(),
                                                            name: "String".to_string(),
                                                        },
                                                    ),
                                                },
                                            )),

                                            required: false,
                                        },
                                    ),
                                ]),
                            }),
                        },
                    )]),
                },
            )]),
        };

        assert_eq!(
            serde_json::json!(parsed_cedar_schema).sorted(),
            serde_json::json!(schema_to_compare).sorted()
        );
    }
}
