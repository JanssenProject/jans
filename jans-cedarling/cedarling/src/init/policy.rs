/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use std::collections::HashMap;

use base64::prelude::*;
use cedar_policy::PolicyId;

const MSG_UNABLE_DECODE_POLICY_BASE64: &str = "decode base64 error:";
const MSG_UNABLE_DECODE_POLICY_STRING: &str = "decode to utf8 string error:";
const MSG_UNABLE_DECODE_POLICY_HUMAN_REDABLE: &str = "decode from human redable format error:";

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
            serde::de::Error::custom(format!(
                "could not collect policy store's to policy set, error: {err}"
            ))
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
