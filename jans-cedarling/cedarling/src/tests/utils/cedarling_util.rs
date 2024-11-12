/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

pub use crate::{
    BootstrapConfig, Cedarling, JwtConfig, LogConfig, LogTypeConfig, PolicyStoreConfig,
    PolicyStoreSource,
};

/// create [`Cedarling`] from [`PolicyStoreSource`]
pub fn get_cedarling(policy_source: PolicyStoreSource) -> Cedarling {
    Cedarling::new(BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
        },
        policy_store_config: PolicyStoreConfig {
            source: policy_source,
        },
        jwt_config: JwtConfig::Disabled,
    })
    .expect("bootstrap config should initialize correctly")
}
