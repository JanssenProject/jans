// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::fmt::Display;

use super::entity_id_getters::GetEntityIdErrors;
use super::schema::BuildMappingSchemaError;
use super::build_expr::{BuildExprError, BuildExprErrorVec};
use cedar_policy::ExpressionConstructionError;
use smol_str::SmolStr;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum InitEntityBuilderError {
    #[error("error while initializing trusted issuer entities: {0}")]
    BuildIssEntities(BuildEntityErrors),
    #[error("error while initializing the mapping schema: {0}")]
    BuildMappingSchema(#[from] BuildMappingSchemaError),
}

#[derive(Debug, Error)]
pub struct BuildEntityErrors(Vec<BuildEntityError>);

impl From<Vec<BuildEntityError>> for BuildEntityErrors {
    fn from(errors: Vec<BuildEntityError>) -> Self {
        Self(errors)
    }
}

impl Display for BuildEntityErrors {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "[{}]", collect_errors_to_str(&self.0))
    }
}

#[derive(Debug, Error)]
#[error("failed to build \"{entity_type_name}\" entity: {error}")]
pub struct BuildEntityError {
    pub entity_type_name: String,
    pub error: BuildEntityErrorKind,
}

#[derive(Debug, Error)]
pub enum BuildEntityErrorKind {
    #[error("unable to find a valid entity id, tried the following: {0}")]
    MissingEntityId(GetEntityIdErrors),
    #[error("failed to parse entity uid: {0}")]
    FailedToParseUid(#[from] cedar_policy::ParseErrors),
    #[error("failed to evaluate entity attribute or tag: {0}")]
    EntityAttrEval(#[from] cedar_policy::EntityAttrEvaluationError),
    #[error("invalid issuer URL: {0}")]
    InvalidIssUrl(#[from] url::ParseError),
    #[error("failed to build entity attributes: {0}")]
    BuildAttrs(#[from] BuildAttrsErrorVec),
    #[error("no available tokens to build the entity. missing one or more of the following: {0:?}")]
    NoAvailableTokensToBuildEntity(Vec<String>),
    #[error("the entity was not in the schema")]
    EntityNotInSchema,
}

#[derive(Debug, Error)]
pub struct BuildAttrsErrorVec(Vec<BuildAttrsError>);

impl BuildAttrsErrorVec {
    pub fn into_inner(self) -> Vec<BuildAttrsError> {
        self.0
    }
}

impl From<Vec<BuildAttrsError>> for BuildEntityErrorKind {
    fn from(errs: Vec<BuildAttrsError>) -> Self {
        Self::BuildAttrs(BuildAttrsErrorVec(errs))
    }
}

impl From<BuildAttrsError> for BuildEntityErrorKind {
    fn from(err: BuildAttrsError) -> Self {
        Self::BuildAttrs(BuildAttrsErrorVec(Vec::from([err])))
    }
}

impl Display for BuildAttrsErrorVec {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:?}", self.0.iter().map(|e| e.to_string()))
    }
}

#[derive(Debug, Error)]
pub enum BuildAttrsError {
    #[error(transparent)]
    ExpressionConstruction(#[from] ExpressionConstructionError),
    #[error(transparent)]
    BuildRestrictedExpressions(#[from] BuildExprErrorVec),
    #[error("required entity references for {0:?} but none were found")]
    MissingEntityRefs(Vec<SmolStr>),
    #[error("missing the token claims required to build the entity: {0:?}")]
    MissingClaims(Vec<SmolStr>),
}

impl From<Vec<ExpressionConstructionError>> for BuildAttrsErrorVec {
    fn from(errs: Vec<ExpressionConstructionError>) -> Self {
        Self(
            errs.into_iter()
                .map(BuildAttrsError::ExpressionConstruction)
                .collect(),
        )
    }
}

impl From<BuildExprError> for BuildAttrsError {
    fn from(err: BuildExprError) -> Self {
        Self::BuildRestrictedExpressions(BuildExprErrorVec::from(Vec::from([err])))
    }
}

impl BuildEntityErrorKind {
    pub fn while_building(self, entity_type_name: &str) -> BuildEntityError {
        BuildEntityError {
            entity_type_name: entity_type_name.to_string(),
            error: self,
        }
    }
}

fn collect_errors_to_str<T: Display>(errors: &[T]) -> String {
    errors
        .iter()
        .map(|e| e.to_string())
        .collect::<Vec<_>>()
        .join(", ")
}
