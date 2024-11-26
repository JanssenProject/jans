/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use crate::{AuthorizationConfig, WorkloadBoolOp};
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
        authorization_config: AuthorizationConfig {
            use_user_principal: true,
            use_workload_principal: true,
            user_workload_operator: WorkloadBoolOp::And,
        },
    })
    .expect("bootstrap config should initialize correctly")
}

/// create [`Cedarling`] from [`PolicyStoreSource`]
pub fn get_cedarling_with_authorization_conf(
    policy_source: PolicyStoreSource,
    auth_conf: AuthorizationConfig,
) -> Cedarling {
    Cedarling::new(BootstrapConfig {
        application_name: "test_app".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut,
        },
        policy_store_config: PolicyStoreConfig {
            source: policy_source,
        },
        jwt_config: JwtConfig::Disabled,
        authorization_config: auth_conf,
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

/// This macro removes code duplication when comparing policy IDs.
///
/// It is designed to assert that the policy ID retrieved from a response
/// matches a vector of policy IDs (converted to a specific format).
/// Before checking it sort arrays to be consistent
///
/// # Arguments
///
/// - `$resp`: The response object that contains the policy ID.
/// - `$vec_policy_id`: A vector of policy IDs to compare against.
/// - `$msg`: A custom error message that will be shown if the assertion fails.
///
/// # Example usage:
/// ```rust
/// cmp_policy!(response, policy_ids, "Policy IDs do not match");
/// ```
#[doc(hidden)]
#[macro_export]
macro_rules! cmp_policy {
    ($resp:expr, $vec_policy_id:expr, $msg:expr) => {
        let policy_ids_resp =
            crate::tests::utils::cedarling_util::get_policy_id(&$resp).map(|mut v| {
                v.sort();
                v
            });

        let mut policy_ids_test = $vec_policy_id
            .iter()
            .map(|v| v.to_string())
            .collect::<Vec<_>>();
        policy_ids_test.sort();

        assert_eq!(policy_ids_resp, Some(policy_ids_test), $msg)
    };
}

/// util function for convenient conversion Decision
pub fn get_decision(resp: &Option<cedar_policy::Response>) -> Option<cedar_policy::Decision> {
    resp.as_ref().map(|v| v.decision())
}

/// This macro removes code duplication when comparing a decision in tests.
///
/// It simplifies the process of asserting that the decision retrieved from
/// a response matches the expected value.
///
/// # Arguments
///
/// - `$resp`: The response object that contains the decision to be checked.
/// - `$decision`: The expected decision value that should match the one in `$resp`.
/// - `$msg`: A custom error message that will be displayed if the assertion fails.
///
/// # Example usage:
/// ```rust
/// cmp_decision!(response, expected_decision, "Decision does not match");
/// ```
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
