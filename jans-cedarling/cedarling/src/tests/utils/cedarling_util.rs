// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::{AuthorizationConfig, JwtConfig};
pub(crate) use crate::{
    BootstrapConfig, Cedarling, DataStoreConfig, LogConfig, LogTypeConfig, PolicyStoreConfig,
    PolicyStoreSource,
};

/// fixture for [`BootstrapConfig`]
pub(crate) fn get_config(policy_source: PolicyStoreSource) -> BootstrapConfig {
    BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::Memory(crate::MemoryLogConfig {
                log_ttl: 60,
                max_items: None,
                max_item_size: None,
            }),
            log_level: crate::LogLevel::DEBUG,
        },
        policy_store_config: PolicyStoreConfig {
            source: policy_source,
        },
        jwt_config: JwtConfig::new_without_validation(),
        authorization_config: AuthorizationConfig::default(),
        lock_config: None,
        max_default_entities: None,
        max_base64_size: None,
        data_store_config: DataStoreConfig::default(),
    }
}

/// create [`Cedarling`] from [`PolicyStoreSource`] with a callback to modify bootstrap config.
pub(crate) async fn get_cedarling_with_callback<F>(
    policy_source: PolicyStoreSource,
    cb: F,
) -> Cedarling
where
    F: FnOnce(&mut BootstrapConfig),
{
    let mut config = get_config(policy_source);
    cb(&mut config); // Apply the callback function

    Cedarling::new(&config)
        .await
        .expect("bootstrap config should initialize correctly")
}
