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

/// util function for convenient conversion Reason ID to string
pub fn get_policy_id(resp: &Option<cedar_policy::Response>) -> Option<Vec<String>> {
    resp.as_ref().map(|v| {
        v.diagnostics()
            .reason()
            .map(|policy_id| policy_id.to_string())
            .collect::<Vec<_>>()
    })
}

// macro to remove code duplication when compare policy_id-s
//
// macro is always defined in the root space.
// to avoid problems with functions calling better to use full path
#[doc(hidden)]
#[macro_export]
macro_rules! cmp_policy {
    ($resp:expr, $vec_policy_id:expr, $msg:expr) => {
        assert_eq!(
            crate::tests::utils::cedarling_util::get_policy_id(&$resp),
            Some($vec_policy_id.iter().map(|v| v.to_string()).collect()),
            $msg
        )
    };
}

/// util function for convenient conversion Decision
pub fn get_decision(resp: &Option<cedar_policy::Response>) -> Option<cedar_policy::Decision> {
    resp.as_ref().map(|v| v.decision())
}

// macro to remove code duplication when compare decision
#[doc(hidden)]
#[macro_export]
macro_rules! cmp_decision {
    ($resp:expr, $decision:expr, $msg:expr) => {
        assert_eq!(
            crate::tests::utils::cedarling_util::get_decision(&$resp),
            Some($decision),
            $msg
        )
    };
}
