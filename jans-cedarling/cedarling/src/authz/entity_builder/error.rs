// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::fmt::Display;

use super::entity_id_getters::GetEntityIdErrors;
use crate::jwt::TokenClaimTypeError;
use cedar_policy::ExpressionConstructionError;

#[derive(Debug, thiserror::Error)]
pub enum InitEntityBuilderError {
    #[error("error while building trusted issuer entities: {0}")]
    BuildIssEntities(BuildEntityErrors),
}

#[derive(Debug, thiserror::Error)]
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

#[derive(Debug, thiserror::Error)]
#[error("failed to build `\"{entity_type_name}\"` entity: {error}")]
pub struct BuildEntityError {
    pub entity_type_name: String,
    pub error: BuildEntityErrorKind,
}

#[derive(Debug, thiserror::Error)]
pub enum BuildEntityErrorKind {
    #[error("unable to find a valid entity id, tried the following: {0}")]
    MissingEntityId(GetEntityIdErrors),
    #[error(transparent)]
    TokenClaimTypeMismatch(#[from] TokenClaimTypeError),
    #[error("failed to parse entity uid: {0}")]
    FailedToParseUid(#[from] cedar_policy::ParseErrors),
    #[error("failed to evaluate entity attribute or tag: {0}")]
    EntityAttrEval(#[from] cedar_policy::EntityAttrEvaluationError),
    #[error("invalid issuer URL: {0}")]
    InvalidIssUrl(#[from] url::ParseError),
    #[error("missing required token: {0}")]
    MissingRequiredToken(String),
    #[error("failed to build entity attributes: {0}")]
    BuildAttrs(BuildAttrsErrors),
}

#[derive(Debug, thiserror::Error)]
pub struct BuildAttrsErrors(Vec<ExpressionConstructionError>);

impl From<Vec<ExpressionConstructionError>> for BuildAttrsErrors {
    fn from(errors: Vec<ExpressionConstructionError>) -> Self {
        Self(errors)
    }
}

impl Display for BuildAttrsErrors {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "[{}]", collect_errors_to_str(&self.0))
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

fn collect_errors_to_str<T: Display>(errors: &Vec<T>) -> String {
    errors
        .iter()
        .map(|e| e.to_string())
        .collect::<Vec<_>>()
        .join(", ")
}
