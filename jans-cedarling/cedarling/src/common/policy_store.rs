/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::cedar_schema::CedarSchema;
use base64::prelude::*;
use cedar_policy::PolicyId;
use std::collections::HashMap;

/// Represents the store of policies used for JWT validation and policy evaluation in Cedarling.
///
/// The `PolicyStore` contains the schema and a set of policies encoded in base64,
/// which are parsed during deserialization.
#[derive(Debug, Clone, serde::Deserialize)]
pub struct PolicyStore {
    /// Cedar schema in base64-encoded string format.
    ///
    /// Extracted from the `policy_store.json` file.
    pub cedar_schema: CedarSchema,

    /// Cedar policy set in base64-encoded string format.
    ///
    /// Extracted from the `policy_store.json` file and deserialized using `deserialize_policies`.
    #[serde(deserialize_with = "parse_cedar_policy")]
    pub cedar_policies: cedar_policy::PolicySet,
}

/// Enum representing the various error messages that can occur while parsing policy sets.
///
/// This enum is used to provide detailed error information during the deserialization
/// of policy sets in the `PolicyStore`.
#[derive(Debug, thiserror::Error)]
enum ParsePolicySetMessage {
    /// Error indicating failure to decode policy content as base64.
    #[error("unable to decode policy_content as base64")]
    Base64,

    /// Error indicating failure to decode policy content to a UTF-8 string.
    #[error("unable to decode policy_content to utf8 string")]
    String,

    /// Error indicating failure to decode policy content from a human-readable format.
    #[error("unable to decode policy_content from human readable format")]
    HumanReadable,

    /// Error indicating failure to collect policies into a policy set.
    #[error("could not collect policy store's to policy set")]
    CreatePolicySet,
}

/// Represents a raw policy entry from the `PolicyStore`.
///
/// This is a helper struct used internally for parsing base64-encoded policies.
#[derive(Debug, serde::Deserialize)]
struct RawPolicy {
    /// Base64-encoded content of the policy.
    pub policy_content: String,
}

/// Custom deserializer for converting base64-encoded policies into a `PolicySet`.
///
/// This function is used to deserialize the `policies` field in `PolicyStore`.
pub fn parse_cedar_policy<'de, D>(deserializer: D) -> Result<cedar_policy::PolicySet, D::Error>
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

    cedar_policy::PolicySet::from_policies(policy_vec).map_err(|err| {
        serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::CreatePolicySet))
    })
}

/// Parses a single policy from its base64-encoded format.
///
/// This function is responsible for decoding the base64-encoded policy content,
/// converting it to a UTF-8 string, and parsing it into a `Policy`.
fn parse_policy<'de, D>(id: &str, policy_raw: RawPolicy) -> Result<cedar_policy::Policy, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let decoded = BASE64_STANDARD
        .decode(policy_raw.policy_content.as_str())
        .map_err(|err| {
            serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::Base64))
        })?;
    let decoded_str = String::from_utf8(decoded).map_err(|err| {
        serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::String))
    })?;

    let policy =
        cedar_policy::Policy::parse(Some(PolicyId::new(id)), decoded_str).map_err(|err| {
            serde::de::Error::custom(format!("{}: {err}", ParsePolicySetMessage::HumanReadable))
        })?;

    Ok(policy)
}

#[cfg(test)]
mod test {
    use super::ParsePolicySetMessage;
    use super::PolicyStore;

    #[test]
    fn test_policy_store_deserialization_success() {
        static POLICY_STORE_RAW: &str = include_str!("../../../test_files/policy-store_ok.json");
        assert!(serde_json::from_str::<PolicyStore>(POLICY_STORE_RAW).is_ok());
    }

    #[test]
    fn test_base64_decoding_error_in_policy_store() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../test_files/policy-store_policy_err_base64.json");

        let policy_result = serde_json::from_str::<PolicyStore>(POLICY_STORE_RAW);
        assert!(policy_result
            .unwrap_err()
            .to_string()
            .contains(&ParsePolicySetMessage::Base64.to_string()));
    }

    #[test]
    fn test_policy_parsing_error_in_policy_store() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../test_files/policy-store_policy_err_broken_utf8.json");

        let policy_result = serde_json::from_str::<PolicyStore>(POLICY_STORE_RAW);
        assert!(policy_result
            .unwrap_err()
            .to_string()
            .contains(&ParsePolicySetMessage::String.to_string()));
    }

    #[test]
    fn test_broken_policy_parsing_error_in_policy_store() {
        static POLICY_STORE_RAW: &str =
            include_str!("../../../test_files/policy-store_policy_err_broken_policy.json");

        let policy_result = serde_json::from_str::<PolicyStore>(POLICY_STORE_RAW);
        let err_msg = policy_result.unwrap_err().to_string();

        // TODO: this isn't really a human readable format so idk why the error message is
        // like this.
        assert_eq!(err_msg, "unable to decode policy with id: 840da5d85403f35ea76519ed1a18a33989f855bf1cf8, error: unable to decode policy_content from human readable format: unexpected token `)` at line 8 column 5")
    }
}
