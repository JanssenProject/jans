// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::jwt::trusted_issuers_loader::TrustedIssuerLoaderError;

use super::http_utils::HttpError;
use super::key_service;
use super::status_list::UpdateStatusListError;
use super::validation::ValidateJwtError;

#[derive(Debug, thiserror::Error)]
pub enum JwtProcessingError {
    #[error("Failed to deserialize from Value to String: {0}")]
    StringDeserialization(#[from] serde_json::Error),
    #[error("failed to validate '{0}' token: {1}")]
    ValidateJwt(String, ValidateJwtError),
    #[error(
        "signed authorization is not available because no trusted issuers or JWKS were configured"
    )]
    SignedAuthzUnavailable,
}

#[derive(Debug, thiserror::Error)]
pub(crate) enum JwtServiceInitError {
    #[error(
        "the JWT service is configured to validate signed JWTs but no algorithms were provided"
    )]
    NoSupportedAlgorithms,
    #[error("failed to prepare keys for the KeyService: {0}")]
    PrepareKeys(#[from] key_service::KeyServiceError),
    #[error("failed to GET the openid configuration for the trusted issuers: {0}")]
    GetOpenidConfigurations(#[from] HttpError),
    #[error("failed to update JWT status list: {0}")]
    UpdateStatusList(#[from] UpdateStatusListError),
    #[error("failed to load trusted issuers: {0}")]
    LoadTrustedIssuers(#[from] TrustedIssuerLoaderError),
    #[error(transparent)]
    LoadTrustedIssuersAggregate(AggregatedTrustedIssuerError),
}

#[derive(Debug, derive_more::From)]
pub(crate) struct AggregatedTrustedIssuerError(pub Vec<JwtServiceInitError>);

impl std::fmt::Display for AggregatedTrustedIssuerError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        writeln!(f, "failed to load trusted issuers:")?;
        for (i, err) in self.0.iter().enumerate() {
            if i != 0 {
                writeln!(f, "\n")?;
            }
            writeln!(f, "  - {err}")?;
        }
        Ok(())
    }
}

impl std::error::Error for AggregatedTrustedIssuerError {}
