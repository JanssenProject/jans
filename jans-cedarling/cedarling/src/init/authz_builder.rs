// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Shared [`Authz`] construction path used by both bootstrap
//! ([`super::service_factory`]) and the refresh worker
//! ([`super::policy_store_refresh`]).

use std::sync::Arc;

use crate::authz::metrics::MetricsCollector;
use crate::authz::{Authz, AuthzConfig};
use crate::bootstrap_config::{AuthorizationConfig, JwtConfig};
use crate::common::policy_store::PolicyStoreWithID;
use crate::context_data_api::DataStore;
use crate::entity_builder::{EntityBuilder, TrustedIssuerIndex};
use crate::http::HttpClient;
use crate::jwt::JwtService;
use crate::log::interface::LogWriter;
use crate::log::{BaseLogEntry, LogEntry, LogLevel, Logger};

#[derive(Debug, thiserror::Error)]
pub(crate) enum BuildAuthzError {
    #[error("trusted issuers validation failed: {0}")]
    TrustedIssuers(String),
    #[error("failed to initialize JWT service: {0}")]
    JwtService(String),
    #[error("failed to initialize entity builder: {0}")]
    EntityBuilder(String),
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

    for warn in policy_store.default_entities.warns() {
        log.log_any(
            LogEntry::new(BaseLogEntry::new_system_opt_request_id(LogLevel::WARN, None))
                .set_message(warn.to_string()),
        );
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
        authorization: authorization_config.clone(),
        data_store,
        metrics,
    };
    Ok(Authz::new(config))
}
