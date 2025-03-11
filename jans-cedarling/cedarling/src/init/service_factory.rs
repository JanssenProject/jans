/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

//! Module to lazily initialize internal cedarling services

use super::service_config::ServiceConfig;
use crate::authz::{Authz, AuthzConfig, AuthzServiceInitError};
use crate::bootstrap_config::BootstrapConfig;
use crate::common::policy_store::PolicyStoreWithID;
use crate::entity_builder::*;
use crate::jwt::{JwtService, JwtServiceInitError};
use crate::log;
use std::sync::Arc;

#[derive(Clone)]
pub(crate) struct ServiceFactory<'a> {
    bootstrap_config: &'a BootstrapConfig,
    service_config: ServiceConfig,
    log_service: log::Logger,
    container: SingletonContainer,
}

/// Structure to store singleton of entities.
#[derive(Clone, Default)]
struct SingletonContainer {
    entity_builder_service: Option<Arc<EntityBuilder>>,
    jwt_service: Option<Arc<JwtService>>,
    authz_service: Option<Arc<Authz>>,
}

impl<'a> ServiceFactory<'a> {
    /// Create new instance of ServiceFactory.
    pub fn new(
        bootstrap_config: &'a BootstrapConfig,
        service_config: ServiceConfig,
        log_service: log::Logger,
    ) -> Self {
        Self {
            bootstrap_config,
            service_config,
            log_service,
            container: Default::default(),
        }
    }

    // get policy store
    pub fn policy_store(&self) -> PolicyStoreWithID {
        self.service_config.policy_store.clone()
    }

    // get log service
    pub fn log_service(&mut self) -> log::Logger {
        self.log_service.clone()
    }

    // get jwt service
    pub async fn jwt_service(&mut self) -> Result<Arc<JwtService>, ServiceInitError> {
        if let Some(jwt_service) = &self.container.jwt_service {
            Ok(jwt_service.clone())
        } else {
            let config = &self.bootstrap_config.jwt_config;
            let trusted_issuers = self.policy_store().trusted_issuers.clone();
            let service = Arc::new(JwtService::new(config, trusted_issuers).await?);
            self.container.jwt_service = Some(service.clone());
            Ok(service)
        }
    }

    // get jwt service
    pub fn entity_builder(&mut self) -> Result<Arc<EntityBuilder>, ServiceInitError> {
        if let Some(entity_builder) = &self.container.entity_builder_service {
            return Ok(entity_builder.clone());
        }

        let config = &self.bootstrap_config.entity_builder_config;
        let trusted_issuers = self
            .policy_store()
            .trusted_issuers
            .clone()
            .unwrap_or_default();
        let schema = &self.policy_store().schema.validator_schema;
        let entity_builder = EntityBuilder::new(config.clone(), &trusted_issuers, Some(schema))?;
        let service = Arc::new(entity_builder);
        self.container.entity_builder_service = Some(service.clone());
        Ok(service)
    }

    // get authz service
    pub async fn authz_service(&mut self) -> Result<Arc<Authz>, ServiceInitError> {
        if let Some(authz) = &self.container.authz_service {
            Ok(authz.clone())
        } else {
            let config = AuthzConfig {
                log_service: self.log_service(),
                policy_store: self.policy_store(),
                jwt_service: self.jwt_service().await?,
                entity_builder: self.entity_builder()?,
                authorization: self.bootstrap_config.authorization_config.clone(),
            };
            let service = Arc::new(Authz::new(config)?);
            self.container.authz_service = Some(service.clone());
            Ok(service)
        }
    }
}

/// Error type for failing to initialize a service
#[derive(Debug, thiserror::Error)]
pub enum ServiceInitError {
    #[error(transparent)]
    AuthzService(#[from] AuthzServiceInitError),
    #[error(transparent)]
    JwtService(#[from] JwtServiceInitError),
    #[error(transparent)]
    EntityBuilder(#[from] InitEntityBuilderError),
}
