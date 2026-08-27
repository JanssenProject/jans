// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Shared [`Authz`] construction path used by both bootstrap
//! ([`super::service_factory`]) and the refresh worker
//! ([`super::policy_store_refresh`]).

use std::collections::HashSet;
use std::sync::Arc;

use crate::authz::metrics::MetricsCollector;
use crate::authz::{Authz, AuthzConfig};
use crate::bootstrap_config::{AuthorizationConfig, JwtConfig};
use crate::common::policy_store::PolicyStoreWithID;
use crate::context_data_api::DataStore;
use crate::entity_builder::{EntityBuilder, TrustedIssuerIndex, sanitize_issuer_name};
use crate::http::HttpClient;
use crate::jwt::{CustomIssuerIndex, CustomIssuerIndexError, JwtService};
use crate::log::Logger;

#[derive(Debug, thiserror::Error)]
pub(crate) enum BuildAuthzError {
    #[error("trusted issuers validation failed: {0}")]
    TrustedIssuers(String),
    #[error("failed to initialize JWT service: {0}")]
    JwtService(String),
    #[error("failed to initialize entity builder: {0}")]
    EntityBuilder(String),
    #[error(
        "custom issuer id '{0}' collides with a JWT trusted-issuer name after sanitization; \
         issuer names must be unique across the context.tokens key namespace"
    )]
    IssuerNamespaceCollision(String),
    #[error(
        "custom issuer declares Cedar entity type '{0}', which a JWT trusted issuer already \
         owns; a custom mapping must not shadow a signature-validated JWT token type"
    )]
    CustomMappingShadowsJwt(String),
    #[error("failed to index custom issuers: {0}")]
    CustomIssuers(#[from] CustomIssuerIndexError),
}

/// Build an [`Authz`] from a loaded policy store.
///
/// Pass `prior_jwt_service = Some(existing)` to reuse a [`JwtService`] when
/// `trusted_issuers` is unchanged (refresh path). Pass `None` to always build
/// fresh (bootstrap path). Bootstrap-specific logging stays in the caller.
#[allow(clippy::too_many_arguments)]
pub(crate) async fn build_authz(
    policy_store: PolicyStoreWithID,
    jwt_config: &JwtConfig,
    authorization_config: &AuthorizationConfig,
    http_client: HttpClient,
    log: &Logger,
    data_store: Arc<DataStore>,
    metrics: Arc<MetricsCollector>,
    prior_jwt_service: Option<Arc<JwtService>>,
) -> Result<Authz, BuildAuthzError> {
    policy_store
        .validate_trusted_issuers()
        .map_err(|e| BuildAuthzError::TrustedIssuers(e.to_string()))?;

    let custom_issuer_index = Arc::new(CustomIssuerIndex::build(&policy_store.custom_issuers)?);
    if !custom_issuer_index.is_empty() {
        let jwt_names: HashSet<String> = policy_store
            .trusted_issuers
            .iter()
            .flatten()
            .map(|(_, ti)| sanitize_issuer_name(&ti.name))
            .collect();
        for id in custom_issuer_index.sanitized_ids() {
            if jwt_names.contains(id) {
                return Err(BuildAuthzError::IssuerNamespaceCollision(id.to_string()));
            }
        }

        let jwt_token_types: HashSet<&str> = policy_store
            .trusted_issuers
            .iter()
            .flatten()
            .flat_map(|(_, ti)| ti.token_metadata.values())
            .map(|meta| meta.entity_type_name.as_str())
            .collect();
        for mapping in custom_issuer_index.mappings() {
            if jwt_token_types.contains(mapping) {
                return Err(BuildAuthzError::CustomMappingShadowsJwt(mapping.to_string()));
            }
        }
    }

    let trusted_issuers = policy_store.trusted_issuers.clone();
    let jwt_service = match prior_jwt_service {
        Some(existing) => existing,
        None => Arc::new(
            JwtService::new(
                jwt_config,
                trusted_issuers.clone(),
                Some(log.clone()),
                metrics.clone(),
                http_client,
            )
            .await
            .map_err(|e| BuildAuthzError::JwtService(e.to_string()))?,
        ),
    };

    let issuers_map = trusted_issuers.unwrap_or_default();
    let issuers_index = TrustedIssuerIndex::new(&issuers_map, Some(log));
    let schema = policy_store.schema.as_ref().map(|s| &s.validator_schema);
    let entity_builder = Arc::new(
        EntityBuilder::new(
            issuers_index,
            if authorization_config.strict_schema_validation {
                schema
            } else {
                None
            },
            policy_store.default_entities.entities().to_owned(),
        )
        .map_err(|e| BuildAuthzError::EntityBuilder(e.to_string()))?,
    );

    let config = AuthzConfig {
        log_service: log.clone(),
        policy_store,
        jwt_service,
        entity_builder,
        custom_issuer_index,
        authorization: authorization_config.clone(),
        data_store,
        metrics,
    };
    Ok(Authz::new(config))
}
