// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use serde::{Deserialize, Serialize};

use super::FeatureToggle;
use crate::JsonRule;

/// Config specific to authorization behavior
#[derive(Debug, Deserialize, Serialize, PartialEq)]
pub struct AuthzConfig {
    /// When [`Enabled`], Cedar engine authorization is queried for a User principal.
    ///
    /// [`Enabled`]: FeatureToggle::Enabled
    #[serde(alias = "CEDARLING_USER_AUTHZ", default)]
    pub user_authz: FeatureToggle,

    /// When [`Enabled`], Cedar engine authorization is queried for a Workload principal.
    ///
    /// [`Enabled`]: FeatureToggle::Enabled
    #[serde(alias = "CEDARLING_WORKLOAD_AUTHZ", default)]
    pub workload_authz: FeatureToggle,

    /// Specifies what boolean operation to use for the `User` and `Workload` when
    /// making authorization decisions.
    ///
    /// Uses [JsonLogic](https://jsonlogic.com/).
    ///
    /// # Default
    ///
    /// ```json
    /// {
    ///     "and" : [
    ///         {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
    ///         {"===": [{"var": "Jans::User"}, "ALLOW"]}
    ///     ]
    /// }
    /// ```
    #[serde(alias = "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION", default)]
    pub user_workload_boolean_operation: JsonRule,
}

#[cfg(test)]
mod test {
    use crate::{JsonRule, config::*};
    use test_utils::assert_eq;

    #[test]
    fn test_deserialize_authz_config() {
        let cases = [
            (
                r#"{"user_authz":"disabled","workload_authz":"disabled"}"#,
                AuthzConfig {
                    user_authz: FeatureToggle::Disabled,
                    workload_authz: FeatureToggle::Disabled,
                    user_workload_boolean_operation: JsonRule::default(),
                },
            ),
            (
                r#"{"user_authz":"enabled","workload_authz":"enabled"}"#,
                AuthzConfig {
                    user_authz: FeatureToggle::Enabled,
                    workload_authz: FeatureToggle::Enabled,
                    user_workload_boolean_operation: JsonRule::default(),
                },
            ),
            (r#"{}"#, AuthzConfig {
                user_authz: FeatureToggle::Disabled,
                workload_authz: FeatureToggle::Disabled,
                user_workload_boolean_operation: JsonRule::default(),
            }),
        ];

        for (i, (source, expected)) in cases.into_iter().enumerate() {
            let deserialized = serde_json::from_str::<AuthzConfig>(source).unwrap();

            assert_eq!(
                deserialized, expected,
                "wrong deserializion in case {i}: {source}",
            );
        }
    }
}
