/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use super::cedar_schema::CedarSchema;
use serde::Deserialize;
use std::collections::HashMap;
use std::fmt;

/// PolicyStoreMap it is a store for `PolicyStore` accessible by key.
#[derive(Debug, Deserialize)]
pub(crate) struct PolicyStoreMap {
    #[serde(flatten)]
    pub policy_stores: HashMap<String, PolicyStore>,
}

/// PolicyStore contains all the data the Cedarling needs to verify JWT tokens and evaluate policies
#[derive(Debug, Clone, Deserialize)]
pub struct PolicyStore {
    pub schema: CedarSchema,
    #[serde(deserialize_with = "deserialize_policy_set::parse")]
    pub policies: cedar_policy::PolicySet,
    #[allow(dead_code)]
    pub trusted_issuers: Option<Vec<TrustedIssuer>>,
}

#[derive(Debug, Clone, Deserialize)]
#[allow(dead_code)]
pub struct TrustedIssuer {
    pub name: String,
    pub description: String,
    pub openid_configuration_endpoint: String,
    pub token_metadata: Option<Vec<TokenMetadata>>,
}

#[derive(Debug, Clone, Deserialize)]
#[allow(dead_code)]
pub struct TokenMetadata {
    #[serde(
        rename = "type",
        deserialize_with = "deserialize_policy_set::parse_token_kind"
    )]
    pub kind: TokenKind,
    pub person_id: String, // the claim used to create the person entity
    pub role_mapping: Option<String>, // the claim used to create a role for the token
}

#[derive(Debug, Clone, Deserialize)]
#[allow(dead_code)]
pub enum TokenKind {
    Access,
    Id,
    Userinfo,
    Transaction,
}

impl fmt::Display for TokenKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let kind_str = match self {
            TokenKind::Access => "access",
            TokenKind::Id => "id",
            TokenKind::Userinfo => "userinfo",
            TokenKind::Transaction => "transaction",
        };
        write!(f, "{}", kind_str)
    }
}

// Deserialization to [`cedar_policy::PolicySet`] moved here
// to be close to structure where it is used.
// And current module is small so it is OK to store it here.
mod deserialize_policy_set {
    use super::TokenKind;
    use base64::prelude::*;
    use cedar_policy::PolicyId;
    use serde::{Deserialize, Deserializer};
    use std::{collections::HashMap, str::FromStr};

    // we use camel case to show that it is like a constant
    #[derive(Debug, thiserror::Error)]
    enum ParsePolicySetMessage {
        #[error("unable to decode policy_content as base64")]
        Base64,
        #[error("unable to decode policy_content to utf8 string")]
        String,
        #[error("unable to decode policy_content from human readable format")]
        HumanReadable,
        #[error("could not collect policy store's to policy set")]
        CreatePolicySet,
    }

    /// Represents a raw data of the `Policy` in the `PolicyStore`
    /// is private and used only in the [`parse_policy_set`] function
    #[derive(Debug, serde::Deserialize)]
    struct RawPolicy {
        pub policy_content: String,
    }

    /// A custom deserializer for Cedar's policy set.
    //
    // is used to deserialize field `policies` in `models::policy_store::PolicyStore`
    pub(crate) fn parse<'de, D>(deserializer: D) -> Result<cedar_policy::PolicySet, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let policies =
            <HashMap<String, RawPolicy> as serde::Deserialize>::deserialize(deserializer)?;

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

    // function to deserialize a single policy from `PolicyRaw`
    fn parse_policy<'de, D>(
        id: &str,
        policy_raw: RawPolicy,
    ) -> Result<cedar_policy::Policy, D::Error>
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

    impl FromStr for TokenKind {
        type Err = Box<str>;

        fn from_str(s: &str) -> Result<Self, Self::Err> {
            match s.to_lowercase().as_str() {
                "id" => Ok(TokenKind::Id),
                "userinfo" => Ok(TokenKind::Userinfo),
                "access" => Ok(TokenKind::Access),
                "transaction" => Ok(TokenKind::Transaction),
                _ => Err(s.into()),
            }
        }
    }

    // function to deserialize a string into `TokenKind`
    pub fn parse_token_kind<'de, D>(deserializer: D) -> Result<TokenKind, D::Error>
    where
        D: Deserializer<'de>,
    {
        let s = String::deserialize(deserializer)?;
        TokenKind::from_str(&s)
            .map_err(|_| serde::de::Error::custom(format!("invalid token type: `{}`", s)))
    }

    #[cfg(test)]
    mod tests {
        use test_utils::assert_eq;

        use super::*;
        use crate::common::policy_store::PolicyStoreMap;

        #[test]
        fn test_ok() {
            static POLICY_STORE_RAW: &str =
                include_str!("../../../test_files/policy-store_ok.json");

            let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
            assert!(policy_result.is_ok());
        }

        #[test]
        fn test_base64_error() {
            static POLICY_STORE_RAW: &str =
                include_str!("../../../test_files/policy-store_policy_err_base64.json");

            let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
            assert!(policy_result
                .unwrap_err()
                .to_string()
                .contains(&ParsePolicySetMessage::Base64.to_string()));
        }

        #[test]
        fn test_string_error() {
            static POLICY_STORE_RAW: &str =
                include_str!("../../../test_files/policy-store_policy_err_broken_utf8.json");

            let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
            assert!(policy_result
                .unwrap_err()
                .to_string()
                .contains(&ParsePolicySetMessage::String.to_string()));
        }

        #[test]
        fn test_policy_error() {
            static POLICY_STORE_RAW: &str =
                include_str!("../../../test_files/policy-store_policy_err_broken_policy.json");

            let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
            let err_msg = policy_result.unwrap_err().to_string();
            assert_eq!(err_msg,"unable to decode policy with id: 840da5d85403f35ea76519ed1a18a33989f855bf1cf8, error: unable to decode policy_content from human readable format: unexpected token `)` at line 15 column 1")
        }

        #[test]
        fn can_parse_policy_store_with_trusted_issuers() {
            static POLICY_STORE_RAW: &str =
                include_str!("../../../test_files/policy-store_with_trusted_issuers_ok.json");

            let policy_result = serde_json::from_str::<PolicyStoreMap>(POLICY_STORE_RAW);
            assert!(policy_result.is_ok());
        }
    }
}
