// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::json_rules::JsonRule;

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Configuration to specify authorization workflow.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct AuthorizationConfig {
    /// Specifies what boolean operation to use for principals when making authz decisions.
    pub principal_bool_operator: JsonRule,

    /// List of claims to map from user entity for decision logging.
    /// `CEDARLING_DECISION_LOG_USER_CLAIMS` in bootstrap properties documentation.
    pub decision_log_user_claims: Vec<String>,

    /// List of claims to map from workload entity for decision logging.
    /// `CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS` in bootstrap properties documentation.
    pub decision_log_workload_claims: Vec<String>,

    /// Token claims that will be used for decision logging.
    /// `CEDARLING_DECISION_LOG_DEFAULT_JWT_ID` in bootstrap properties documentation.
    pub decision_log_default_jwt_id: String,
}

/// Raw authorization config
pub struct AuthorizationConfigRaw {
    /// Principal bool operator
    pub principal_bool_operator: JsonRule,
    /// Decision log default JWT ID
    pub decision_log_default_jwt_id: String,
    /// Decision log user claims
    pub decision_log_user_claims: Vec<String>,
    /// Decision log workload claims
    pub decision_log_workload_claims: Vec<String>,
}

impl Default for AuthorizationConfig {
    fn default() -> Self {
        Self {
            principal_bool_operator: JsonRule::default(),
            decision_log_default_jwt_id: "jti".to_string(),
            decision_log_user_claims: Vec::new(),
            decision_log_workload_claims: Vec::new(),
        }
    }
}

impl From<AuthorizationConfigRaw> for AuthorizationConfig {
    fn from(raw: AuthorizationConfigRaw) -> Self {
        Self {
            principal_bool_operator: raw.principal_bool_operator,
            decision_log_default_jwt_id: raw.decision_log_default_jwt_id,
            decision_log_user_claims: raw.decision_log_user_claims,
            decision_log_workload_claims: raw.decision_log_workload_claims,
        }
    }
}

/// Token entity names
pub struct TokenEntityNames(pub HashMap<String, String>);

impl Default for TokenEntityNames {
    fn default() -> Self {
        Self(HashMap::from([
            ("access_token".to_string(), "Jans::Access_token".to_string()),
            ("id_token".to_string(), "Jans::Id_token".to_string()),
            (
                "userinfo_token".to_string(),
                "Jans::Userinfo_token".to_string(),
            ),
        ]))
    }
}

impl From<HashMap<String, String>> for TokenEntityNames {
    fn from(value: HashMap<String, String>) -> Self {
        Self(value)
    }
}

impl<S: std::hash::BuildHasher + Default> From<TokenEntityNames> for HashMap<String, String, S> {
    fn from(value: TokenEntityNames) -> Self {
        value.0.into_iter().collect::<HashMap<_, _, S>>()
    }
}
