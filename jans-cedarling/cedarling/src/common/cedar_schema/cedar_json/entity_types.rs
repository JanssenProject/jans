/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::{CedarType, GetCedarTypeError};
use std::collections::HashMap;

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
    // attributes as key is used attribute name
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
    pub cedar_type: CedarSchemaEntityType,
    pub required: bool,
}

impl CedarSchemaEntityAttribute {
    pub fn is_required(&self) -> bool {
        self.required
    }

    pub fn get_type(&self) -> Result<CedarType, GetCedarTypeError> {
        self.cedar_type.get_type()
    }
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
                "could not deserialize CedarSchemaEntityAttribute, field 'is_required': {}",
                err
            ))
        })?;

        let cedar_type = CedarSchemaEntityType::deserialize(value).map_err(|err| {
            serde::de::Error::custom(format!(
                "could not deserialize CedarSchemaEntityType: {}",
                err
            ))
        })?;

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

impl CedarSchemaEntityType {
    pub fn get_type(&self) -> Result<CedarType, GetCedarTypeError> {
        match self {
            Self::Set(v) => Ok(CedarType::Set(Box::new(v.element.get_type()?))),
            Self::Typed(v) => v.get_type(),
            Self::Primitive(primitive) => Ok(primitive.kind.get_type()),
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
                    // will newer happen because we know that field "type" is string
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
    pub kind: PrimitiveTypeKind,
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

impl PrimitiveTypeKind {
    pub fn get_type(&self) -> CedarType {
        match self {
            PrimitiveTypeKind::Long => CedarType::Long,
            PrimitiveTypeKind::String => CedarType::String,
            PrimitiveTypeKind::Boolean => CedarType::Boolean,
            PrimitiveTypeKind::TypeName(name) => CedarType::TypeName(name.to_string()),
        }
    }
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
    pub kind: String,
    pub name: String,
}

impl EntityType {
    pub fn get_type(&self) -> Result<CedarType, GetCedarTypeError> {
        if self.kind == "EntityOrCommon" {
            match self.name.as_str() {
                "Long" => Ok(CedarType::Long),
                "String" => Ok(CedarType::String),
                "Boolean" => Ok(CedarType::Boolean),
                type_name => Ok(CedarType::TypeName(type_name.to_string())),
            }
        } else {
            Err(GetCedarTypeError::TypeNotImplemented(self.kind.to_string()))
        }
    }
}

#[derive(Debug, Clone, serde::Deserialize, PartialEq, serde::Serialize)]
/// Describes the Set element
/// Set ::= '"type": "Set", "element": ' TypeJson
//
// "type": "Set" checked during deserialization
pub struct SetEntityType {
    pub element: CedarSchemaEntityType,
}
