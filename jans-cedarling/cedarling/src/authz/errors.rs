// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy::{ContextJsonError, ParseErrors, RequestValidationError};
use serde_json::Error as SerdeJsonError;

// Re-export commonly used error types
pub use crate::common::json_rules::ApplyRuleError;
pub use crate::entity_builder::{
    BuildEntityError, BuildUnsignedEntityError, InitEntityBuilderError,
};
pub use crate::jwt::{JwtProcessingError, TokenClaimTypeError};
pub use cedar_policy::entities_errors::EntitiesError;

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
}

/// Error type for token input validation
#[derive(Debug, thiserror::Error)]
pub enum TokenInputError {
    #[error("Empty mapping string")]
    EmptyMapping,

    #[error("Empty payload")]
    EmptyPayload,
}

/// Error type for Authorization Service initialization
#[derive(Debug, thiserror::Error)]
pub enum AuthzServiceInitError {
    #[error(transparent)]
    InitEntityBuilder(#[from] InitEntityBuilderError),
}

/// Error type for Authorization Service
#[derive(thiserror::Error, Debug)]
pub enum AuthorizeError {
    /// Error encountered while processing JWT token data
    #[error(transparent)]
    ProcessTokens(#[from] JwtProcessingError),
    /// Error encountered while parsing Action to EntityUid
    #[error("could not parse action: {0}")]
    Action(ParseErrors),
    /// Error encountered while validating context according to the schema
    #[error("could not create context: {0}")]
    CreateContext(#[from] ContextJsonError),
    /// Error encountered while creating [`cedar_policy::Request`] for entity principal
    #[error(transparent)]
    InvalidPrincipal(#[from] InvalidPrincipalError),
    /// Error encountered while checking if the Entities adhere to the schema
    #[error("failed to validate Cedar entities: {0:?}")]
    ValidateEntities(#[from] EntitiesError),
    /// Error encountered while parsing all entities to json for logging
    #[error("could convert entities to json: {0}")]
    EntitiesToJson(SerdeJsonError),
    /// Error encountered while building the context for the request
    #[error("Failed to build context: {0}")]
    BuildContext(#[from] BuildContextError),
    /// Error encountered while building the context for the request
    #[error("error while running on strict id token trust mode: {0}")]
    IdTokenTrustMode(#[from] IdTokenTrustModeError),
    /// Error encountered while building Cedar Entities
    #[error(transparent)]
    BuildEntity(#[from] BuildEntityError),
    /// Error encountered while executing the rule for principals
    #[error(transparent)]
    ExecuteRule(#[from] ApplyRuleError),
    #[error("failed to build role entities for unsigned request: {0}")]
    /// Error encountered while building Role entity in an unsigned request
    BuildUnsignedRoleEntity(#[from] BuildUnsignedEntityError),
    /// Error encountered while validating multi-issuer request
    #[error(transparent)]
    MultiIssuerValidation(#[from] MultiIssuerValidationError),
}

/// Error type for ID token trust mode validation
#[derive(Debug, thiserror::Error)]
pub enum IdTokenTrustModeError {
    #[error("the access token's `client_id` does not match with the id token's `aud`")]
    AccessTokenClientIdMismatch,
    #[error("an access token is required when using strict mode")]
    MissingAccessToken,
    #[error("an id token is required when using strict mode")]
    MissingIdToken,
    #[error("the id token's `sub` does not match with the userinfo token's `sub`")]
    SubMismatchIdTokenUserinfo,
    #[error("the access token's `client_id` does not match with the userinfo token's `aud`")]
    ClientIdUserinfoAudMismatch,
    #[error("missing a required claim `{0}` from `{1}` token")]
    MissingRequiredClaim(String, String),
    #[error("invalid claim type in {0} token: {1}")]
    TokenClaimTypeError(String, TokenClaimTypeError),
}

/// Error type for building context
#[derive(Debug, thiserror::Error)]
pub enum BuildContextError {
    /// Error encountered while validating context according to the schema
    #[error("failed to merge JSON objects due to conflicting keys: {0}")]
    KeyConflict(String),
    /// Error encountered while deserializing the Context from JSON
    #[error(transparent)]
    DeserializeFromJson(#[from] ContextJsonError),
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
    ParseEntityName(String, ParseErrors),
}

/// Error for creating request role entities
#[derive(Debug, derive_more::Error, derive_more::Display)]
#[display("could not create request user entity principal for {uid}: {err}")]
pub struct CreateRequestRoleError {
    /// Error value
    pub err: RequestValidationError,
    /// Role ID [`EntityUid`] value used for authorization request
    pub uid: cedar_policy::EntityUid,
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
