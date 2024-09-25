/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use base64::prelude::*;
use cedar_policy::PolicyId;

// we use camel case to show that it is like a constant
#[allow(non_camel_case_types)]
#[derive(Debug, thiserror::Error)]
enum ParsePolicySetMessage {
    #[error("unable to decode base64")]
    BASE64,
    #[error("unable to decode to utf8 string")]
    STRING,
    #[error("unable to decode from human redable format")]
    HUMAN_REDABLE,
    #[error("could not collect policy store's to policy set")]
    CREATE_POLICY_SET,
}

/// Represents a raw data of the `Policy` in the `PolicyStore`
/// is private and used only in the [`parse_policy_set`] function
#[derive(Debug, serde::Deserialize)]
struct RawPolicy {
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
    let policies = <HashMap<String, RawPolicy> as serde::Deserialize>::deserialize(deserializer)?;

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
            serde::de::Error::custom(format!(
                "{}: {err}",
                ParsePolicySetMessage::CREATE_POLICY_SET
            ))
        })?,
    )
}

// function to deserialize a single policy from `PolicyRaw`
fn parse_policy<'de, D>(id: &str, policy_raw: RawPolicy) -> Result<cedar_policy::Policy, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let decoded = BASE64_STANDARD
        .decode(policy_raw.policy_content.as_str())
        .map_err(|err| {
            serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::BASE64))
        })?;
    let decoded_str = String::from_utf8(decoded).map_err(|err| {
        serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::STRING))
    })?;

    let policy =
        cedar_policy::Policy::parse(Some(PolicyId::new(id)), decoded_str).map_err(|err| {
            serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::HUMAN_REDABLE))
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
            .contains(&ParsePolicySetMessage::BASE64.to_string()));
    }

    #[test]
    fn test_string_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("test_files/policy-store_policy_err_broken_utf8.json");

        let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
        assert!(policy_result
            .unwrap_err()
            .to_string()
            .contains(&ParsePolicySetMessage::STRING.to_string()));
    }

    #[test]
    fn test_policy_error() {
        static POLICY_STORE_RAW: &str =
            include_str!("test_files/policy-store_policy_err_broken_policy.json");

        let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
        let err_msg = policy_result.unwrap_err().to_string();
        assert_eq!(err_msg,"unable to decode policy with id: 840da5d85403f35ea76519ed1a18a33989f855bf1cf8, error: unable to decode from human redable format: unexpected token `)` at line 15 column 1")
    }
}
