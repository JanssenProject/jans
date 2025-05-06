// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::fmt::Display;
use std::path::PathBuf;

use serde::ser::SerializeStruct;
use serde::{Deserialize, Serialize, de};
use url::Url;

/// Config specific to loading the policy store
#[derive(Debug, PartialEq)]
pub struct PolicyStoreConfig {
    /// The policy store
    pub source: PolicyStoreSource,
    /// The identifier of the policy store in case there is more then one policy_store_id in the policy store.
    pub id: Option<String>,
}

impl<'de> Deserialize<'de> for PolicyStoreConfig {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let raw = PolicyStoreConfigRaw::deserialize(deserializer)?;

        let mut sources = [
            (
                raw.policy_store_local
                    .map(|src| PolicyStoreSource::JsonString(src)),
                "policy_store_local",
            ),
            (
                raw.policy_store_local_fn
                    .map(|src| {
                        src.parse()
                            .map(|path| PolicyStoreSource::File(path))
                            .map_err(|e| {
                                de::Error::custom(format!(
                                    "Error while parsing the policy store file path: {e}"
                                ))
                            })
                    })
                    .transpose()?,
                "policy_store_local_fn",
            ),
            (
                raw.policy_store_uri
                    .map(|src| {
                        src.parse()
                            .map(|url| PolicyStoreSource::Url(url))
                            .map_err(|e| {
                                de::Error::custom(format!(
                                    "Error while parsing the policy store uri: {e}"
                                ))
                            })
                    })
                    .transpose()?,
                "policy_store_uri",
            ),
        ]
        .into_iter()
        .filter_map(|src| src.0.map(|s| (s, src.1)))
        .collect::<Vec<_>>();

        if sources.is_empty() {
            return Err(de::Error::custom(
                "missing policy store config. you can provide one by setting one of the environment variables: 'CEDARLING_POLICY_STORE_LOCAL', 'CEDARLING_POLICY_STORE_LOCAL_FN', 'CEDARLING_POLICY_STORE_URI'",
            ));
        }

        if sources.len() > 1 {
            let source_names = sources
                .into_iter()
                .fold(String::new(), |mut acc, (_, name)| {
                    if acc.is_empty() {
                        acc += name;
                    } else {
                        acc += &format!(", {name}");
                    }
                    acc
                });

            return Err(de::Error::custom(format!(
                "Conflicting policy store config. Multiple sources were detected in the config: {}",
                source_names
            )));
        }

        let policy_store_src = sources.pop().unwrap().0;

        Ok(Self {
            source: policy_store_src,
            id: raw.policy_store_id,
        })
    }
}

impl Serialize for PolicyStoreConfig {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        let mut s = serializer.serialize_struct("PolicyStoreConfigRaw", 4)?;
        match &self.source {
            PolicyStoreSource::JsonString(policy_store_str) => {
                s.serialize_field("policy_store_local", policy_store_str)?;
            },
            PolicyStoreSource::File(path) => {
                s.serialize_field("policy_store_local_fn", &path.to_string_lossy())?;
            },
            PolicyStoreSource::Url(url) => {
                s.serialize_field("policy_store_uri", &url.to_string())?;
            },
        }

        s.end()
    }
}

/// Config specific to loading the policy store
#[derive(Debug, Deserialize, Serialize)]
struct PolicyStoreConfigRaw {
    /// JSON object as String with policy store.
    #[serde(alias = "CEDARLING_POLICY_STORE_LOCAL")]
    pub policy_store_local: Option<String>,

    /// Path to a local file with the policy store.
    #[serde(alias = "CEDARLING_POLICY_STORE_LOCAL_FN")]
    pub policy_store_local_fn: Option<String>,

    /// Location of policy store JSON, used if policy store is not local.
    #[serde(alias = "CEDARLING_POLICY_STORE_URI")]
    pub policy_store_uri: Option<String>,

    /// The identifier of the policy store in case there is more then one policy_store_id in the policy store.
    #[serde(alias = "CEDARLING_POLICY_STORE_ID", default)]
    pub policy_store_id: Option<String>,
}

/// The method of loading the policy store
#[derive(Debug, PartialEq)]
pub enum PolicyStoreSource {
    /// JSON object as String.
    JsonString(String),
    /// Path to a local file.
    File(PathBuf),
    /// Remote URL.
    Url(Url),
}

impl Display for PolicyStoreSource {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            PolicyStoreSource::JsonString(string) => write!(f, "{}", string),
            PolicyStoreSource::File(path) => write!(f, "{}", path.to_string_lossy()),
            PolicyStoreSource::Url(url) => write!(f, "{}", url.to_string()),
        }
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use test_utils::assert_eq;

    #[test]
    fn test_serde_policy_store_config() {
        let cases = [
            (
                r#"{"policy_store_local":"{}"}"#,
                PolicyStoreSource::JsonString("{}".parse().unwrap()),
            ),
            (
                r#"{"policy_store_local_fn":"/tmp/policy_store.json"}"#,
                PolicyStoreSource::File("/tmp/policy_store.json".parse().unwrap()),
            ),
            (
                r#"{"policy_store_uri":"https://test.com/config.json"}"#,
                PolicyStoreSource::Url("https://test.com/config.json".parse().unwrap()),
            ),
        ];

        for (i, (source, expected)) in cases.into_iter().enumerate() {
            let deserialized = serde_json::from_str::<PolicyStoreConfig>(source).unwrap();
            let reserialized = serde_json::to_string(&deserialized).unwrap();

            assert_eq!(
                deserialized,
                PolicyStoreConfig {
                    source: expected,
                    id: None
                },
                "wrong deserializion in case {i}: {source}"
            );

            assert_eq!(
                reserialized, source,
                "wrong serializion in case {i}: {source}"
            );
        }
    }
}
