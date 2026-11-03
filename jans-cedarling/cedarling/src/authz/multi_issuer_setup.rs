// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Turns the tokens of one multi-issuer request into the token entities and
//! the record of which inputs were used or dropped.

use super::{AuthorizeError, DropReason, DroppedToken, MultiIssuerValidationError};
use crate::TokenInput;
use crate::entity_builder::{
    MultiIssuerEntitiesBuilder, MultiIssuerEntityError, MultiIssuerSetupEntities,
};
use crate::jwt::{MultiIssuerTokenValidator, TokenValidation, ValidatedToken};
use crate::log::interface::LogWriter;
use crate::log::{BaseLogEntry, LogEntry, LogLevel};

/// Resource-independent setup shared across all items of a multi-issuer
/// request. Every request input is in either `used_tokens` or `dropped`, never
/// both.
#[derive(Debug)]
pub(super) struct MultiIssuerSetup {
    pub(super) entities: MultiIssuerSetupEntities,
    /// Tokens whose entity is in `entities.tokens`, in input order.
    pub(super) used_tokens: Vec<ValidatedToken>,
    /// Tokens that were not used, in input order. Shared by every item in a
    /// batch.
    pub(super) dropped: Vec<DroppedToken>,
}

/// Error of [`MultiIssuerSetup::validate_and_build`].
#[derive(Debug, thiserror::Error)]
pub(super) enum MultiIssuerSetupError {
    #[error(transparent)]
    Validation(#[from] MultiIssuerValidationError),
    #[error(transparent)]
    Entity(#[from] MultiIssuerEntityError),
}

impl From<MultiIssuerSetupError> for AuthorizeError {
    fn from(err: MultiIssuerSetupError) -> Self {
        match err {
            MultiIssuerSetupError::Validation(e) => Self::MultiIssuerValidation(e),
            MultiIssuerSetupError::Entity(e) => Self::MultiIssuerEntity(e),
        }
    }
}

impl MultiIssuerSetup {
    /// Validates the tokens of one multi-issuer request and builds their
    /// entities in one pass, recording each input as used or dropped in
    /// request order.
    pub(super) async fn validate_and_build(
        tokens: &[TokenInput],
        mut validator: MultiIssuerTokenValidator<'_>,
        mut entities: MultiIssuerEntitiesBuilder<'_>,
        log_service: &impl LogWriter,
    ) -> Result<Self, MultiIssuerSetupError> {
        if tokens.is_empty() {
            return Err(MultiIssuerValidationError::EmptyTokenArray.into());
        }

        let mut used_tokens = Vec::new();
        let mut dropped = Vec::new();
        for (index, input) in tokens.iter().enumerate() {
            let reason = match validator.validate(index, input).await? {
                TokenValidation::Accepted(validated) => match entities.add(&validated) {
                    Ok(()) => {
                        used_tokens.push(validated);
                        continue;
                    },
                    Err(err) => {
                        log_service.log_any(
                            LogEntry::new(BaseLogEntry::new_system_opt_request_id(
                                LogLevel::ERROR,
                                None,
                            ))
                            .set_message(format!(
                                "Failed to build token entity for token '{}' at index {index}",
                                input.mapping
                            ))
                            .set_error(err.to_string()),
                        );
                        DropReason::EntityBuildFailed
                    },
                },
                TokenValidation::Dropped(reason) => reason,
            };
            dropped.push(DroppedToken::new(input.mapping.as_str(), index, reason));
        }
        validator.ensure_any_validated()?;

        Ok(Self {
            entities: entities.finish(log_service)?,
            used_tokens,
            dropped,
        })
    }
}
