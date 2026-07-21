// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy::{ContextJsonError, ParseErrors, RequestValidationError};
use serde::{Deserialize, Serialize};
use serde_json::Error as SerdeJsonError;

// Re-export commonly used error types
use crate::entity_builder::MultiIssuerEntityError;
use crate::entity_builder::{BuildEntityError, BuildUnsignedEntityError};
use crate::jwt::JwtProcessingError;
use cedar_policy::entities_errors::EntitiesError;

/// Error type for multi-issuer validation
#[derive(Debug, thiserror::Error)]
pub enum MultiIssuerValidationError {
    #[error("Token input validation failed: {0}")]
    TokenInput(#[from] TokenInputError),

    #[error("Empty token array")]
    EmptyTokenArray,

    #[error("Could not validate tokens.")]
    TokenValidationFailed,

    #[error("Invalid JSON in context field")]
    InvalidContextJson,

    #[error("Missing issuer claim in JWT")]
    MissingIssuer,

    #[error("JWT claims are not a JSON object: {0}")]
    InvalidClaims(&'static str),
}

/// Error type for token input validation
#[derive(Debug, thiserror::Error)]
pub enum TokenInputError {
    #[error("Empty mapping string")]
    EmptyMapping,

    #[error("Empty payload")]
    EmptyPayload,
}

/// Error type for batch authorization request validation.
#[derive(Debug, thiserror::Error, PartialEq)]
pub enum BatchValidationError {
    /// The `items` array was empty.
    #[error("Empty items array")]
    EmptyItems,

    /// The `tokens` array was empty (multi-issuer batch only).
    #[error("Empty tokens array")]
    EmptyTokens,

    /// A batch item's context field is not a JSON object.
    #[error("Context for item {index} must be a JSON object")]
    InvalidItemContext { index: usize },
}

/// Per-item build failure surfaced inside a batch authorize response at
/// `results[item_index] = Err(_)`. Serializable + `Clone`-able so bindings
/// can round-trip it through JSON / FFI; the `variant` tag is the wire
/// discriminant.
#[allow(missing_docs)] // per-variant docs above each `#[error(...)]`; fields are self-descriptive
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize, thiserror::Error)]
#[serde(tag = "variant", rename_all = "snake_case")]
pub enum BatchItemError {
    /// Item's action string didn't parse as a Cedar `EntityUid`.
    #[error("item {item_index}: action parse failed: {message}")]
    ActionParse { message: String, item_index: usize },
    /// Item's resource `EntityData` failed to build a Cedar entity (unsigned).
    #[error("item {item_index}: resource build failed: {message}")]
    ResourceBuild { message: String, item_index: usize },
    /// Per-item context couldn't be built (invalid shape, missing entity ref, …).
    #[error("item {item_index}: context build failed: {message}")]
    ContextBuild { message: String, item_index: usize },
    /// Unsigned role-entity build for this item failed.
    #[error("item {item_index}: principal build failed: {message}")]
    PrincipalBuild { message: String, item_index: usize },
    /// Assembled Cedar `Entities` didn't validate against the policy-store schema.
    #[error("item {item_index}: schema validation failed: {message}")]
    SchemaValidation { message: String, item_index: usize },
    /// Multi-issuer resource entity for this item couldn't be built.
    #[error("item {item_index}: multi-issuer entity build failed: {message}")]
    MultiIssuerEntity { message: String, item_index: usize },
    /// Cedar rejected the assembled request (principal / action / resource /
    /// context didn't line up with the schema's `appliesTo`).
    #[error("item {item_index}: request validation failed: {message}")]
    RequestValidation { message: String, item_index: usize },
}

impl BatchItemError {
    /// Stable slug identifying this variant — safe to use as a metric label
    /// or a structured log field.
    #[must_use]
    pub const fn category(&self) -> &'static str {
        match self {
            Self::ActionParse { .. } => "action_parse",
            Self::ResourceBuild { .. } => "resource_build",
            Self::ContextBuild { .. } => "context_build",
            Self::PrincipalBuild { .. } => "principal_build",
            Self::SchemaValidation { .. } => "schema_validation",
            Self::MultiIssuerEntity { .. } => "multi_issuer_entity",
            Self::RequestValidation { .. } => "request_validation",
        }
    }

    /// Position of the failing item in the original `items` vector — matches
    /// its position in the returned `results` vector.
    #[must_use]
    pub const fn item_index(&self) -> usize {
        match self {
            Self::ActionParse { item_index, .. }
            | Self::ResourceBuild { item_index, .. }
            | Self::ContextBuild { item_index, .. }
            | Self::PrincipalBuild { item_index, .. }
            | Self::SchemaValidation { item_index, .. }
            | Self::MultiIssuerEntity { item_index, .. }
            | Self::RequestValidation { item_index, .. } => *item_index,
        }
    }
}

/// Error type for Authorization Service
#[derive(thiserror::Error, Debug)]
pub enum AuthorizeError {
    /// Error encountered while processing JWT token data
    #[error(transparent)]
    ProcessTokens(#[from] JwtProcessingError),
    /// Error encountered while parsing Action to `EntityUid`
    #[error("could not parse action: {0}")]
    Action(Box<ParseErrors>),
    /// Error encountered while parsing an entity type name or action identifier
    #[error("could not parse identifier: {0}")]
    IdentifierParsing(Box<ParseErrors>),
    /// Error encountered while validating context according to the schema
    #[error("could not create context: {0}")]
    CreateContext(#[from] Box<ContextJsonError>),
    /// Error encountered while creating [`cedar_policy::Request`] for entity principal
    #[error(transparent)]
    InvalidPrincipal(#[from] Box<InvalidPrincipalError>),
    /// Error encountered while validating the Cedar request
    #[error("request validation error: {0}")]
    RequestValidation(#[from] Box<RequestValidationError>),
    /// Error encountered while checking if the Entities adhere to the schema
    #[error("failed to validate Cedar entities: {0:?}")]
    ValidateEntities(#[from] Box<EntitiesError>),
    /// Error encountered while parsing all entities to json for logging
    #[error("could not convert entities to json: {0}")]
    EntitiesToJson(SerdeJsonError),
    /// Error encountered while building the context for the request
    #[error("Failed to build context: {0}")]
    BuildContext(#[from] BuildContextError),
    /// Error encountered while building Cedar Entities
    #[error(transparent)]
    BuildEntity(#[from] BuildEntityError),
    #[error("failed to build role entities for unsigned request: {0}")]
    /// Error encountered while building Role entity in an unsigned request
    BuildUnsignedRoleEntity(#[from] BuildUnsignedEntityError),
    /// Error encountered while validating multi-issuer request
    #[error(transparent)]
    MultiIssuerValidation(#[from] MultiIssuerValidationError),
    /// Error encountered while validating a batch authorization request
    #[error(transparent)]
    BatchValidation(#[from] BatchValidationError),
    /// Error encountered while building multi-issuer entities
    #[error("Multi-issuer entity building failed: {0}")]
    MultiIssuerEntity(MultiIssuerEntityError),
}

impl From<ParseErrors> for AuthorizeError {
    fn from(err: ParseErrors) -> Self {
        Self::Action(Box::new(err))
    }
}

impl From<InvalidPrincipalError> for AuthorizeError {
    fn from(err: InvalidPrincipalError) -> Self {
        Self::InvalidPrincipal(Box::new(err))
    }
}

/// Error type for building context
#[derive(Debug, thiserror::Error)]
pub enum BuildContextError {
    /// Error encountered while validating context according to the schema
    #[error("failed to merge JSON objects due to conflicting keys: {0}")]
    KeyConflict(String),
    /// Error encountered while deserializing the Context from JSON
    #[error(transparent)]
    DeserializeFromJson(#[from] Box<ContextJsonError>),
    /// Error encountered if the action being used as the reference to build the Context
    /// is not in the schema
    #[error("failed to find the action `{0}` in the schema")]
    UnknownAction(String),
    /// Error encountered while building entity references in the Context
    #[error("failed to build entity reference for `{0}` since an entity id was not provided")]
    MissingEntityId(String),
    #[error("invalid action context type: {0}. expected: {1}")]
    InvalidKind(String, String),
    #[error("failed to parse the entity name `{0}`: {1}")]
    ParseEntityName(String, Box<ParseErrors>),
    /// Error encountered while creating Cedar context
    #[error("failed to create Cedar context: {0}")]
    ContextCreation(String),
}

impl From<ContextJsonError> for BuildContextError {
    fn from(err: ContextJsonError) -> Self {
        BuildContextError::DeserializeFromJson(Box::new(err))
    }
}

/// Error for invalid principal in authorization request
#[derive(Debug, derive_more::Error, derive_more::Display)]
#[display("The request for `{principal}` does not conform to the schema: {err}")]
pub struct InvalidPrincipalError {
    /// Principal [`EntityUid`] value used for authorization request
    pub principal: cedar_policy::EntityUid,
    /// Error value
    pub err: RequestValidationError,
}

impl InvalidPrincipalError {
    pub fn new(principal: &cedar_policy::EntityUid, err: RequestValidationError) -> Self {
        InvalidPrincipalError {
            principal: principal.clone(),
            err,
        }
    }
}
