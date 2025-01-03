// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Module to lazily initialize internal cedarling services

use super::service_config::ServiceConfig;
use crate::authz::entity_builder::{self, EntityBuilder, EntityNames};
use crate::authz::{Authz, AuthzConfig};
use crate::bootstrap_config::BootstrapConfig;
use crate::common::app_types;
use crate::common::cedar_schema::cedar_json::CedarSchemaJson;
use crate::common::policy_store::PolicyStoreWithID;
use crate::jwt::{JwtService, JwtServiceInitError};
use crate::log;
use std::sync::Arc;

#[derive(Clone)]
pub(crate) struct ServiceFactory<'a> {
    bootstrap_config: &'a BootstrapConfig,
    service_config: ServiceConfig,
    // it is initialized before ServiceFactory is created
    pdp_id: app_types::PdpID,
    log_service: log::Logger,

    container: SingletonContainer,
}

/// Structure to store singleton of entities.
#[derive(Clone, Default)]
struct SingletonContainer {
    jwt_service: Option<Arc<JwtService>>,
    authz_service: Option<Arc<Authz>>,
}

impl<'a> ServiceFactory<'a> {
    /// Create new instance of ServiceFactory.
    pub fn new(
        bootstrap_config: &'a BootstrapConfig,
        service_config: ServiceConfig,
        log_service: log::Logger,
        pdp_id: app_types::PdpID,
    ) -> Self {
        Self {
            bootstrap_config,
            service_config,
            log_service,
            container: Default::default(),
            pdp_id,
        }
    }

    // get PdpID
    pub fn pdp_id(&self) -> app_types::PdpID {
        self.pdp_id
    }

    // get application name
    pub fn application_name(&self) -> app_types::ApplicationName {
        app_types::ApplicationName(self.bootstrap_config.application_name.to_string())
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
    pub fn jwt_service(&mut self) -> Result<Arc<JwtService>, ServiceInitError> {
        if let Some(jwt_service) = &self.container.jwt_service {
            Ok(jwt_service.clone())
        } else {
            let config = &self.bootstrap_config.jwt_config;
            let trusted_issuers = self.policy_store().trusted_issuers.clone();
            let service = Arc::new(JwtService::new(config, trusted_issuers)?);
            self.container.jwt_service = Some(service.clone());
            Ok(service)
        }
    }

    // get authz service
    pub fn authz_service(&mut self) -> Result<Arc<Authz>, ServiceInitError> {
        if let Some(authz) = &self.container.authz_service {
            Ok(authz.clone())
        } else {
            // TODO: move this inside authz::new
            let entity_builder = entity_builder::EntityBuilder::new_from_config(
                self.bootstrap_config,
                self.policy_store().schema.json.clone(),
            );

            let config = AuthzConfig {
                log_service: self.log_service(),
                pdp_id: self.pdp_id(),
                application_name: self.application_name(),
                policy_store: self.policy_store(),
                jwt_service: self.jwt_service()?,
                authorization: self.bootstrap_config.authorization_config.clone(),
                entity_builder,
            };
            let service = Arc::new(Authz::new(config));
            self.container.authz_service = Some(service.clone());
            Ok(service)
        }
    }
}

/// Error type for failing to initialize a service
#[derive(Debug, thiserror::Error)]
pub enum ServiceInitError {
    #[error(transparent)]
    JwtService(#[from] JwtServiceInitError),
}

impl EntityBuilder {
    fn new_from_config(config: &BootstrapConfig, schema: CedarSchemaJson) -> Self {
        let authz_conf = &config.authorization_config;
        let entity_names: EntityNames = authz_conf.into();
        let build_workload = config.authorization_config.use_workload_principal;
        let build_user = config.authorization_config.use_user_principal;
        Self::new(schema, entity_names, build_workload, build_user)
    }
}
