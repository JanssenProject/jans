// This software is available under the Apache-2.0.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::{
    AuthorizeError, DataError,
    authz::{BuildContextError, MultiIssuerValidationError},
    entity_builder::{BuildUnsignedEntityError, MultiIssuerEntityError},
    jwt::{TrustedIssuerError, ValidateJwtError},
};

/// Trait for error types that map to a telemetry metric key.
pub(crate) trait ErrorMetricKey {
    /// Returns the dot-separated metric key for this error variant
    /// (e.g., `"jwt.decode_failed"`, `"data.invalid_key"`).
    fn metric_key(&self) -> &'static str;
}

impl ErrorMetricKey for MultiIssuerValidationError {
    fn metric_key(&self) -> &'static str {
        match self {
            Self::TokenInput(_) => "multi_issuer.token_input_invalid",
            Self::EmptyTokenArray => "multi_issuer.empty_token_array",
            Self::TokenValidationFailed => "multi_issuer.all_tokens_failed",
            Self::InvalidContextJson => "multi_issuer.invalid_context",
            Self::MissingIssuer => "multi_issuer.missing_issuer",
        }
    }
}

impl ErrorMetricKey for AuthorizeError {
    fn metric_key(&self) -> &'static str {
        match self {
            Self::ProcessTokens(_) => "authz.process_tokens",
            Self::Action(_) => "authz.invalid_action",
            Self::IdentifierParsing(_) => "authz.identifier_parsing",
            Self::CreateContext(_) => "authz.invalid_context",
            Self::InvalidPrincipal(_) => "authz.invalid_principal",
            Self::RequestValidation(_) => "authz.request_validation",
            Self::ValidateEntities(_) => "authz.entity_validation",
            Self::EntitiesToJson(_) => "authz.entities_to_json",
            Self::BuildContext(e) => e.metric_key(),
            Self::BuildEntity(_) => "authz.entity_build",
            Self::BuildUnsignedRoleEntity(e) => e.metric_key(),
            Self::MultiIssuerValidation(e) => e.metric_key(),
            Self::MultiIssuerEntity(e) => e.metric_key(),
        }
    }
}

impl ErrorMetricKey for BuildContextError {
    fn metric_key(&self) -> &'static str {
        "authz.context_build"
    }
}

impl ErrorMetricKey for BuildUnsignedEntityError {
    fn metric_key(&self) -> &'static str {
        "authz.unsigned_role_build"
    }
}

impl ErrorMetricKey for MultiIssuerEntityError {
    fn metric_key(&self) -> &'static str {
        "authz.entity_build"
    }
}

impl ErrorMetricKey for ValidateJwtError {
    fn metric_key(&self) -> &'static str {
        match self {
            Self::DecodeJwt(_) => "jwt.decode_failed",
            Self::MissingValidationKey => "jwt.missing_key",
            Self::MissingValidator(_) => "jwt.missing_validator",
            Self::ValidateJwt(_) => "jwt.validation_failed",
            Self::MissingClaims(_) => "jwt.missing_claims",
            Self::GetJwtStatus(_) | Self::DeserializeStatusClaim(_) => "jwt.status_check_failed",
            Self::RejectJwtStatus(_) => "jwt.status_rejected",
            Self::MissingStatusList => "jwt.missing_status_list",
            Self::TrustedIssuerValidation(e) => e.metric_key(),
        }
    }
}

impl ErrorMetricKey for DataError {
    fn metric_key(&self) -> &'static str {
        match self {
            Self::InvalidKey => "data.invalid_key",
            Self::KeyNotFound { .. } => "data.key_not_found",
            Self::StorageLimitExceeded { .. } => "data.storage_limit",
            Self::TTLExceeded { .. } => "data.ttl_exceeded",
            Self::ValueTooLarge { .. } => "data.value_too_large",
            Self::SerializationError(_) => "data.serialization",
        }
    }
}

impl ErrorMetricKey for TrustedIssuerError {
    fn metric_key(&self) -> &'static str {
        match self {
            Self::UntrustedIssuer(_) => "jwt.untrusted_issuer",
            Self::MissingRequiredClaim { .. } | Self::EmptyEntityTypeName { .. } => {
                "jwt.missing_required_claim"
            },
        }
    }
}
