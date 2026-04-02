// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use crate::common::json_rules::JsonRule;

use serde::{Deserialize, Serialize};

/// Configuration to specify authorization workflow.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct AuthorizationConfig {
    /// Specifies what boolean operation to use for principals when making authz decisions.
    pub principal_bool_operator: JsonRule,

    /// Claim name used for decision logging (e.g. which JWT claim identifies the token).
    /// `CEDARLING_DECISION_LOG_DEFAULT_JWT_ID` in bootstrap properties documentation.
    pub decision_log_default_jwt_id: String,
}

impl Default for AuthorizationConfig {
    fn default() -> Self {
        Self {
            principal_bool_operator: JsonRule::default(),
            decision_log_default_jwt_id: "jti".to_string(),
        }
    }
}
