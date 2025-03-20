// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy_validator::types::{EntityRecordKind, Primitive, Type};
use derive_more::derive::Deref;
use smol_str::{SmolStr, ToSmolStr};
use std::{collections::HashMap, fmt::Display};
use thiserror::Error;

pub type EntityTypeName = SmolStr;

/// Represents different sources for building a Cedar attribute.
///
/// Each variant corresponds to a different type of attribute source.
/// To construct a [`RestrictedExpression`], call `.build_expr()` on the variant's inner
/// value.
///
/// [`RestrictedExpression`]: cedar_policy::RestrictedExpression
#[derive(Debug, PartialEq)]
pub enum AttrSrc {
    JwtClaim(TknClaimAttrSrc),
    EntityRef(EntityRefAttrSrc),
    EntityRefSet(EntityRefSetSrc),
}

#[derive(Debug, PartialEq, Deref)]
pub struct EntityRefAttrSrc(pub EntityTypeName);

#[derive(Debug, PartialEq, Deref)]
pub struct EntityRefSetSrc(pub EntityTypeName);

#[derive(Debug, PartialEq)]
pub struct TknClaimAttrSrc {
    pub claim: String,
    pub expected_type: ExpectedClaimType,
}

#[derive(Debug, PartialEq)]
pub enum ExpectedClaimType {
    Null,
    Bool,
    Number,
    String,
    Array(Box<Self>),
    Object(HashMap<SmolStr, Self>),
    Extension(SmolStr),
}

impl AttrSrc {
    /// Resolves [`AttrSrc`] from [`cedar_policy_validator::types::Type`]
    pub fn from_type(attr_name: &str, value: &Type) -> Result<Self, BuildAttrSrcError> {
        let attr_src: Self = match value {
            Type::Never => {
                return Err(
                    BuildAttrSrcErrorKind::InvalidType(value.clone()).while_building(attr_name)
                );
            },
            Type::True => Self::JwtClaim(TknClaimAttrSrc {
                claim: attr_name.to_string(),
                expected_type: ExpectedClaimType::Bool,
            }),
            Type::False => Self::JwtClaim(TknClaimAttrSrc {
                claim: attr_name.to_string(),
                expected_type: ExpectedClaimType::Bool,
            }),
            Type::Primitive { primitive_type } => Self::from_primitive(attr_name, primitive_type),
            Type::Set { element_type } => {
                let element_type = element_type.as_ref().ok_or(
                    BuildAttrSrcErrorKind::SetElementTypeNotDefined.while_building(attr_name),
                )?;

                let element_type = Self::from_type(attr_name, element_type)?;

                match element_type {
                    AttrSrc::JwtClaim(src) => Self::JwtClaim(TknClaimAttrSrc {
                        claim: src.claim,
                        expected_type: ExpectedClaimType::Array(Box::new(src.expected_type)),
                    }),
                    AttrSrc::EntityRef(entity_ref_attr_src) => {
                        Self::EntityRefSet(EntityRefSetSrc(entity_ref_attr_src.to_smolstr()))
                    },
                    AttrSrc::EntityRefSet(type_name) => Self::EntityRefSet(type_name),
                }
            },
            Type::EntityOrRecord(entity_record_kind) => {
                Self::from_entity_record_kind(attr_name, entity_record_kind)?
            },
            Type::ExtensionType { name } => Self::JwtClaim(TknClaimAttrSrc {
                claim: attr_name.to_string(),
                expected_type: ExpectedClaimType::Extension(name.to_smolstr()),
            }),
        };

        Ok(attr_src)
    }

    fn from_primitive(attr_name: &str, value: &Primitive) -> Self {
        match value {
            Primitive::Bool => Self::JwtClaim(TknClaimAttrSrc {
                claim: attr_name.to_string(),
                expected_type: ExpectedClaimType::Bool,
            }),
            Primitive::Long => Self::JwtClaim(TknClaimAttrSrc {
                claim: attr_name.to_string(),
                expected_type: ExpectedClaimType::Number,
            }),
            Primitive::String => Self::JwtClaim(TknClaimAttrSrc {
                claim: attr_name.to_string(),
                expected_type: ExpectedClaimType::String,
            }),
        }
    }

    fn from_entity_record_kind(
        attr_name: &str,
        value: &EntityRecordKind,
    ) -> Result<Self, BuildAttrSrcError> {
        let src = match value {
            EntityRecordKind::Record { attrs, .. } => {
                let attrs = attrs
                    .iter()
                    .map(|(name, attr_kind)| {
                        (name.clone(), ExpectedClaimType::from(&attr_kind.attr_type))
                    })
                    .collect::<HashMap<SmolStr, ExpectedClaimType>>();
                Self::JwtClaim(TknClaimAttrSrc {
                    claim: attr_name.to_string(),
                    expected_type: ExpectedClaimType::Object(attrs),
                })
            },
            EntityRecordKind::AnyEntity => {
                return Err(
                    BuildAttrSrcErrorKind::InvalidEntityRecordKind(value.clone())
                        .while_building(attr_name),
                );
            },
            EntityRecordKind::Entity(entity_lub) => {
                let entity = entity_lub.get_single_entity().ok_or(
                    BuildAttrSrcErrorKind::MissingEntityTypeName.while_building(attr_name),
                )?;
                Self::EntityRef(EntityRefAttrSrc(entity.name().to_smolstr()))
            },
            EntityRecordKind::ActionEntity { .. } => {
                return Err(BuildAttrSrcErrorKind::ActionAttrNotAllowed.while_building(attr_name));
            },
        };

        Ok(src)
    }
}

#[derive(Debug, Error)]
#[error("failed to build attribute source for {attr_name}: {kind}")]
pub struct BuildAttrSrcError {
    attr_name: String,
    kind: BuildAttrSrcErrorKind,
}

#[derive(Debug, Error)]
pub enum BuildAttrSrcErrorKind {
    #[error("can't use {0:?} as an attribute source")]
    InvalidType(Type),
    #[error("the type of the elements of a Set was not defined")]
    SetElementTypeNotDefined,
    #[error("can't use {0:?} as an entity or record attribute source")]
    InvalidEntityRecordKind(EntityRecordKind),
    #[error("the entity type name was not defined")]
    MissingEntityTypeName,
    #[error("action entities cannot become attributes to other entities")]
    ActionAttrNotAllowed,
}

#[derive(Debug, Error)]
pub struct BuildObjectClaimTypeError(Vec<BuildAttrSrcErrorKind>);

impl Display for BuildObjectClaimTypeError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:?}", self.0.iter().map(|e| e.to_string()))
    }
}

impl BuildAttrSrcErrorKind {
    fn while_building(self, attr_name: &str) -> BuildAttrSrcError {
        BuildAttrSrcError {
            attr_name: attr_name.to_string(),
            kind: self,
        }
    }
}

impl From<&Type> for ExpectedClaimType {
    fn from(value: &Type) -> Self {
        match value {
            Type::Never => ExpectedClaimType::Null,
            Type::True => ExpectedClaimType::Bool,
            Type::False => ExpectedClaimType::Bool,
            Type::Primitive { primitive_type } => primitive_type.into(),
            Type::Set { element_type } => element_type
                .as_ref()
                .map(|t| Self::from(&**t))
                // see: https://docs.rs/cedar-policy-validator/4.3.3/cedar_policy_validator/types/enum.Type.html#variant.Set
                .expect("this should always be `Some`"),
            Type::EntityOrRecord(entity_record_kind) => entity_record_kind.into(),
            Type::ExtensionType { name } => Self::Extension(name.to_smolstr()),
        }
    }
}

impl From<&Primitive> for ExpectedClaimType {
    fn from(value: &Primitive) -> Self {
        match value {
            Primitive::Bool => Self::Bool,
            Primitive::Long => Self::Number,
            Primitive::String => Self::String,
        }
    }
}

impl From<&EntityRecordKind> for ExpectedClaimType {
    fn from(value: &EntityRecordKind) -> Self {
        match value {
            EntityRecordKind::Record { attrs, .. } => {
                let attrs = attrs
                    .iter()
                    .map(|(name, attr_kind)| (name.clone(), Self::from(&attr_kind.attr_type)))
                    .collect::<HashMap<SmolStr, Self>>();
                Self::Object(attrs)
            },
            // We will not build entities using single claims
            // ... should we use TryFrom instead of returning null?
            EntityRecordKind::AnyEntity => Self::Null,
            EntityRecordKind::Entity(_entity_lub) => Self::Null,
            EntityRecordKind::ActionEntity { .. } => Self::Null,
        }
    }
}
