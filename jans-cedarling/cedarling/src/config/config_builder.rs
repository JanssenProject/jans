// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use jsonwebtoken::{Algorithm, jwk::JwkSet};
use url::Url;

use crate::JsonRule;

use super::*;

impl Config {
    /// Creates a [`ConfigBuilder`]
    pub fn builder() -> ConfigBuilder {
        ConfigBuilder::default()
    }
}

#[allow(missing_docs)]
impl ConfigBuilder {
    /// Creates a [`Config`]
    pub fn build(self) -> Result<Config, ConfigError> {
        let Some(app_name) = self.application_name else {
            return Err(ConfigError::MissingApplicationName);
        };

        let Some(source) = self.policy_store.source else {
            return Err(ConfigError::MissingPolicySource);
        };

        let policy_store = PolicyStoreConfig {
            source,
            id: self.policy_store.id,
        };

        self.lock.validate()?;

        Ok(Config {
            application_name: AppName(app_name),
            policy_store,
            authz: self.authz,
            entity_mapping: self.entity_mapping,
            logging: self.logging,
            jwt_validation: self.jwt_validation,
            lock: self.lock,
        })
    }

    pub fn application_name(mut self, app_name: String) -> Self {
        self.application_name = Some(app_name);
        self
    }

    pub fn policy_store_src(mut self, policy_store_src: PolicyStoreSource) -> Self {
        self.policy_store.source = Some(policy_store_src);
        self
    }

    pub fn policy_store_id(mut self, policy_store_id: String) -> Self {
        self.policy_store.id = Some(policy_store_id);
        self
    }

    pub fn user_authz(mut self, toggle: FeatureToggle) -> Self {
        self.authz.user_authz = toggle;
        self
    }

    pub fn workload_authz(mut self, toggle: FeatureToggle) -> Self {
        self.authz.workload_authz = toggle;
        self
    }

    pub fn user_workload_bool_op(mut self, json_rule: JsonRule) -> Self {
        self.authz.user_workload_boolean_operation = json_rule;
        self
    }

    pub fn mapping_iss(mut self, mapping: String) -> Self {
        self.entity_mapping.mapping_iss = MappingTrustedIssuer(mapping);
        self
    }

    pub fn mapping_user(mut self, mapping: String) -> Self {
        self.entity_mapping.mapping_user = MappingUser(mapping);
        self
    }

    pub fn mapping_workload(mut self, mapping: String) -> Self {
        self.entity_mapping.mapping_workload = MappingWorkload(mapping);
        self
    }

    pub fn mapping_role(mut self, mapping: String) -> Self {
        self.entity_mapping.mapping_role = MappingRole(mapping);
        self
    }

    pub fn logger_kind(mut self, logger_kind: LoggerKind) -> Self {
        self.logging.logger_kind = logger_kind;
        self
    }

    pub fn log_level(mut self, log_level: LogLevel) -> Self {
        self.logging.log_level = log_level;
        self
    }

    pub fn decision_log_user_claims(mut self, claims: Vec<String>) -> Self {
        self.logging.decision_log_user_claims = claims;
        self
    }

    pub fn decision_log_workload_claims(mut self, claims: Vec<String>) -> Self {
        self.logging.decision_log_workload_claims = claims;
        self
    }

    pub fn decision_log_default_jwt_id(mut self, claim: String) -> Self {
        self.logging.decision_log_default_jwt_id = DecisionLogDefaultJwtId(claim);
        self
    }

    pub fn log_max_items(mut self, max_items: u64) -> Self {
        self.logging.max_items = LogMaxItems(max_items);
        self
    }

    pub fn log_max_item_size(mut self, max_item_size: u64) -> Self {
        self.logging.max_item_size = LogMaxItemSize(max_item_size);
        self
    }

    pub fn local_jwks(mut self, jwk_set: JwkSet) -> Self {
        self.jwt_validation.local_jwks = Some(jwk_set);
        self
    }

    pub fn jwt_sig_validation(mut self, toggle: FeatureToggle) -> Self {
        self.jwt_validation.sig_validation = toggle;
        self
    }

    pub fn jwt_status_validation(mut self, toggle: FeatureToggle) -> Self {
        self.jwt_validation.status_validation = toggle;
        self
    }

    pub fn jwt_algorithms_supported(mut self, algorithms: Vec<Algorithm>) -> Self {
        self.jwt_validation.signature_algorithms_supported = algorithms;
        self
    }

    pub fn jwt_id_token_trust_mode(mut self, mode: IdTokenTrustMode) -> Self {
        self.jwt_validation.id_token_trust_mode = mode;
        self
    }

    pub fn lock(mut self, toggle: FeatureToggle) -> Self {
        self.lock.enabled = toggle;
        self
    }

    pub fn lock_server_config_uri(mut self, uri: Url) -> Self {
        self.lock.server_config_uri = Some(UrlWrapper(uri));
        self
    }

    pub fn lock_dynamic_config(mut self, toggle: FeatureToggle) -> Self {
        self.lock.dynamic_configuration = toggle;
        self
    }

    pub fn lock_ssa_jwt(mut self, jwt: String) -> Self {
        self.lock.ssa_jwt = Some(jwt);
        self
    }

    pub fn lock_log_interval(mut self, interval: u64) -> Self {
        self.lock.log_interval = interval;
        self
    }

    pub fn lock_health_interval(mut self, interval: u64) -> Self {
        self.lock.health_interval = interval;
        self
    }

    pub fn lock_telemetry_interval(mut self, interval: u64) -> Self {
        self.lock.telemetry_interval = interval;
        self
    }

    pub fn lock_listen_sse(mut self, toggle: FeatureToggle) -> Self {
        self.lock.listen_sse = toggle;
        self
    }
}

#[derive(Default)]
#[allow(missing_docs)]
pub struct ConfigBuilder {
    application_name: Option<String>,
    policy_store: PolicyStoreConfigBuilder,
    authz: AuthzConfig,
    entity_mapping: EntityMappingConfig,
    logging: LoggingConfig,
    jwt_validation: JwtValidationConfig,
    lock: LockConfig,
}

#[derive(Default)]
struct PolicyStoreConfigBuilder {
    source: Option<PolicyStoreSource>,
    id: Option<String>,
}
