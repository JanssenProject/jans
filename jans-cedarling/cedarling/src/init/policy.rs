/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use base64::prelude::*;
use cedar_policy::PolicyId;

const MSG_UNABLE_DECODE_POLICY_BASE64: &str = "decode base64, error:";
const MSG_UNABLE_DECODE_POLICY_STRING: &str = "decode to utf8 string, error:";
const MSG_UNABLE_DECODE_POLICY_HUMAN_REDABLE: &str = "decode from human redable format, error:";
const MSG_UNABLE_TO_CREATE_POLICY_SET: &str =
    "could not collect policy store's to policy set, error:";

/// Represents a raw data of the `Policy` in the `PolicyStore`
/// is private and used only in the [`parse_policy_set`] function
#[derive(Debug, serde::Deserialize)]
struct PolicyRaw {
    // unused fields
    // pub description: String,
    // pub creation_date: String,
    pub policy_content: String,
}

/// A custom deserializer for Cedar's policy set.
//
// is used to deserialize field `policies` in `models::policy_store::PolicyStore`
pub(crate) fn parse_policy_set<'de, D>(deserializer: D) -> Result<cedar_policy::PolicySet, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let policies = <HashMap<String, PolicyRaw> as serde::Deserialize>::deserialize(deserializer)?;

    let policy_vec = policies
        .into_iter()
        .map(|(id, policy_raw)| {
            let policy = parse_policy::<D>(&id, policy_raw).map_err(|err| {
                serde::de::Error::custom(format!(
                    "unable to decode policy with id: {id}, error: {err}"
                ))
            })?;
            Ok(policy)
        })
        .collect::<Result<Vec<cedar_policy::Policy>, _>>()?;

    Ok(
        cedar_policy::PolicySet::from_policies(policy_vec.into_iter()).map_err(|err| {
            serde::de::Error::custom(format!("{MSG_UNABLE_TO_CREATE_POLICY_SET} {err}"))
        })?,
    )
}

// function to deserialize a single policy from `PolicyRaw`
fn parse_policy<'de, D>(id: &str, policy_raw: PolicyRaw) -> Result<cedar_policy::Policy, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let decoded = BASE64_STANDARD
        .decode(policy_raw.policy_content.as_str())
        .map_err(|err| {
            serde::de::Error::custom(format!("{MSG_UNABLE_DECODE_POLICY_BASE64} {}", err))
        })?;
    let decoded_str = String::from_utf8(decoded).map_err(|err| {
        serde::de::Error::custom(format!("{MSG_UNABLE_DECODE_POLICY_STRING} {}", err))
    })?;

    let policy =
        cedar_policy::Policy::parse(Some(PolicyId::new(id)), decoded_str).map_err(|err| {
            serde::de::Error::custom(format!("{MSG_UNABLE_DECODE_POLICY_HUMAN_REDABLE} {}", err))
        })?;

    Ok(policy)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::policy_store::PolicyStoreMap;

    #[test]
    fn test_ok() {
        static POLICY_STORE_RAW: &str = include_str!("test_files/policy-store_ok.json");

        let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
        assert!(policy_result.is_ok());
    }

    #[test]
    fn test_base64_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("test_files/policy-store_policy_err_base64.json");

        let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
        assert!(policy_result
            .unwrap_err()
            .to_string()
            .contains(MSG_UNABLE_DECODE_POLICY_BASE64));
    }

    #[test]
    fn test_string_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("test_files/policy-store_policy_err_broken_utf8.json");

        let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
        assert!(policy_result
            .unwrap_err()
            .to_string()
            .contains(MSG_UNABLE_DECODE_POLICY_STRING));
    }

    #[test]
    fn test_policy_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("test_files/policy-store_policy_err_broken_policy.json");

        let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
        // in this scenario error message looks like:
        // "unable to decode policy with id: 840da5d85403f35ea76519ed1a18a33989f855bf1cf8, error: decode from human redable format error: unexpected token `)`
        let err_msg = policy_result.unwrap_err().to_string();
        assert!(err_msg.contains(MSG_UNABLE_DECODE_POLICY_HUMAN_REDABLE));
        assert!(err_msg.contains("unexpected token `)`"));
    }
}
