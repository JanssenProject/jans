// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy_core::extensions::Extensions;
use cedar_policy_core::validator::ValidatorSchema;
use serde::Deserialize;

fn trimmed_string<'de, D>(deserializer: D) -> Result<String, D::Error>
where
    D: serde::Deserializer<'de>,
{
    let s = String::deserialize(deserializer)?;
    Ok(s.trim_end().to_string())
}

pub(crate) mod cedar_json;
pub(crate) const CEDAR_NAMESPACE_SEPARATOR: &str = "::";

/// cedar_schema value which specifies both encoding and content_type
///
/// encoding is one of none or base64
/// content_type is one of cedar or cedar-json#[derive(Debug, Clone, serde::Deserialize)]
#[derive(Debug, Clone, serde::Deserialize)]
struct EncodedSchema {
    pub encoding: super::Encoding,
    pub content_type: super::ContentType,
    #[serde(deserialize_with = "trimmed_string")]
    pub body: String,
}

/// Intermediate struct to handle both kinds of cedar_schema values.
///
/// Either
///   "cedar_schema": "cGVybWl0KA..."
/// OR
///   "cedar_schema": { "encoding": "...", "content_type": "...", "body": "permit(...)"}
#[derive(Debug, Clone, serde::Deserialize)]
#[serde(untagged)]
enum MaybeEncoded {
    Plain(String),
    Tagged(EncodedSchema),
}

/// Box that holds the [`cedar_policy::Schema`] and
/// JSON representation that is used to create entities from the schema in the policy store.
#[derive(Debug, Clone)]
pub struct CedarSchema {
    pub schema: cedar_policy::Schema,
    pub json: cedar_json::CedarSchemaJson,
    pub validator_schema: ValidatorSchema,
}

#[cfg(test)]
impl PartialEq for CedarSchema {
    fn eq(&self, other: &Self) -> bool {
        // Compare only the JSON representation, which is the canonical form.
        // The schema and validator_schema are derived from the JSON representation,
        // so if the JSON is equal, the schemas are semantically equal.
        self.json == other.json
    }
}

impl<'de> serde::Deserialize<'de> for CedarSchema {
    fn deserialize<D: serde::Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        //  Read the next thing as either a String or a Map, using the MaybeEncoded enum to distinguish
        let encoded_schema = match <MaybeEncoded as serde::Deserialize>::deserialize(deserializer)?
        {
            MaybeEncoded::Plain(body) => EncodedSchema {
                // These are the default if the encoding is not specified.
                encoding: super::Encoding::Base64,
                content_type: super::ContentType::CedarJson,
                body,
            },
            MaybeEncoded::Tagged(encoded_schema) => encoded_schema,
        };

        let decoded_body = match encoded_schema.encoding {
            super::Encoding::None => encoded_schema.body,
            super::Encoding::Base64 => {
                use base64::prelude::*;
                let buf = BASE64_STANDARD.decode(encoded_schema.body).map_err(|err| {
                    serde::de::Error::custom(format!(
                        "{}: {}",
                        deserialize::ParseCedarSchemaSetMessage::Base64,
                        err
                    ))
                })?;
                String::from_utf8(buf).map_err(|err| {
                    serde::de::Error::custom(format!(
                        "{}: {}",
                        deserialize::ParseCedarSchemaSetMessage::Utf8,
                        err
                    ))
                })?
            },
        };
        let decoded_body = decoded_body.trim_end().to_string();

        // Need both of these because CedarSchema wants both.
        let (schema_fragment, json_string) = match encoded_schema.content_type {
            super::ContentType::Cedar => {
                // parse cedar policy from the cedar representation
                // TODO must log warnings or something
                let (schema_fragment, _warning) =
                    cedar_policy::SchemaFragment::from_cedarschema_str(&decoded_body).map_err(
                        |err| {
                            serde::de::Error::custom(format!(
                                "{}: {}",
                                deserialize::ParseCedarSchemaSetMessage::Parse,
                                err
                            ))
                        },
                    )?;

                // urgh now recreate the json representation
                let json_string = schema_fragment.to_json_string().map_err(|err| {
                    serde::de::Error::custom(format!(
                        "{}: {}",
                        deserialize::ParseCedarSchemaSetMessage::CedarSchemaJsonFormat,
                        err
                    ))
                })?;

                (schema_fragment, json_string)
            },
            super::ContentType::CedarJson => {
                // parse cedar policy from the json representation
                let schema_fragment = cedar_policy::SchemaFragment::from_json_str(&decoded_body)
                    .map_err(|err| {
                        serde::de::Error::custom(format!(
                            "{}: {}",
                            deserialize::ParseCedarSchemaSetMessage::CedarSchemaJsonFormat,
                            err
                        ))
                    })?;
                (schema_fragment, decoded_body)
            },
        };

        // create the schema
        let fragment_iter = std::iter::once(schema_fragment);
        let schema = cedar_policy::Schema::from_schema_fragments(fragment_iter.into_iter())
            .map_err(|err| {
                serde::de::Error::custom(format!(
                    "{}: {}",
                    deserialize::ParseCedarSchemaSetMessage::Parse,
                    err
                ))
            })?;

        let json = serde_json::from_str(&json_string).map_err(|err| {
            serde::de::Error::custom(format!(
                "{}: {}",
                deserialize::ParseCedarSchemaSetMessage::CedarSchemaJsonFormat,
                err
            ))
        })?;

        let validator_schema =
            ValidatorSchema::from_json_str(&json_string, Extensions::all_available()).map_err(
                |err| {
                    serde::de::Error::custom(format!(
                        "{}: {}",
                        deserialize::ParseCedarSchemaSetMessage::ParseCedarSchemaJson,
                        err
                    ))
                },
            )?;

        Ok(CedarSchema {
            schema,
            json,
            validator_schema,
        })
    }
}

mod deserialize {
    #[derive(Debug, thiserror::Error)]
    pub enum ParseCedarSchemaSetMessage {
        #[error("unable to decode cedar policy schema base64")]
        Base64,
        #[error("unable to unmarshal cedar policy schema json to the structure")]
        CedarSchemaJsonFormat,
        #[error("unable to parse cedar policy schema")]
        Parse,
        #[error("invalid utf8 detected while decoding cedar policy")]
        Utf8,
        #[error("failed to parse cedar schema from JSON")]
        ParseCedarSchemaJson,
    }

    #[cfg(test)]
    mod tests {
        use test_utils::assert_eq;

        use crate::common::policy_store::{AgamaPolicyStore, PolicyStore};

        #[test]
        fn test_read_ok() {
            // The human-readable policy and schema file is located in next folder:
            // `test_files\policy-store_ok`
            static POLICY_STORE_RAW: &str =
                include_str!("../../../../test_files/policy-store_ok.yaml");

            let policy_result = serde_yml::from_str::<AgamaPolicyStore>(POLICY_STORE_RAW);
            assert!(policy_result.is_ok(), "{:?}", policy_result.unwrap_err());
        }

        #[test]
        fn test_agama_ok() {
            static POLICY_STORE_RAW: &str = include_str!("../../../../test_files/agama-store.yaml");

            let policy_result = serde_yml::from_str::<AgamaPolicyStore>(POLICY_STORE_RAW);
            assert!(policy_result.is_ok(), "{:?}", policy_result.unwrap_err());
        }

        #[test]
        fn test_readable_json_ok() {
            static POLICY_STORE_RAW: &str =
                include_str!("../../../../test_files/policy-store_readable.json");

            let policy_result = serde_json::from_str::<AgamaPolicyStore>(POLICY_STORE_RAW);
            assert!(policy_result.is_ok(), "{:?}", policy_result.unwrap_err());
        }

        #[test]
        fn test_readable_yaml_ok() {
            static YAML_POLICY_STORE: &str =
                include_str!("../../../../test_files/policy-store_readable.yaml");
            let yaml_policy_result = serde_yml::from_str::<AgamaPolicyStore>(YAML_POLICY_STORE);
            assert!(
                yaml_policy_result.is_ok(),
                "{:?}",
                yaml_policy_result.unwrap_err()
            );
        }

        #[test]
        fn test_readable_yaml_identical_readable_json() {
            static YAML_POLICY_STORE: &str =
                include_str!("../../../../test_files/policy-store_readable.yaml");
            let yaml_policy_result = serde_yml::from_str::<AgamaPolicyStore>(YAML_POLICY_STORE);

            static JSON_POLICY_STORE: &str =
                include_str!("../../../../test_files/policy-store_readable.json");
            let json_policy_result = serde_yml::from_str::<AgamaPolicyStore>(JSON_POLICY_STORE);

            assert_eq!(yaml_policy_result.unwrap(), json_policy_result.unwrap());
        }

        #[test]
        fn test_both_ok() {
            static POLICY_STORE_RAW: &str =
                include_str!("../../../../test_files/policy-store_blobby.json");

            let policy_result = serde_json::from_str::<PolicyStore>(POLICY_STORE_RAW);
            let err = policy_result.unwrap_err();
            let msg = err.to_string();
            assert!(msg.contains("missing required field 'name' in policy store entry"));
        }

        #[test]
        fn test_read_base64_error() {
            static POLICY_STORE_RAW: &str =
                include_str!("../../../../test_files/policy-store_schema_err_base64.json");

            let policy_result = serde_json::from_str::<AgamaPolicyStore>(POLICY_STORE_RAW);
            let err = policy_result.unwrap_err();
            let msg = err.to_string();
            assert!(msg.contains("missing required field 'name' in policy store entry"));
        }

        #[test]
        fn test_read_json_error() {
            static POLICY_STORE_RAW_YAML: &str =
                include_str!("../../../../test_files/policy-store_schema_err.yaml");

            let policy_result = serde_yml::from_str::<AgamaPolicyStore>(POLICY_STORE_RAW_YAML);
            let err = policy_result.unwrap_err();
            let msg = err.to_string();
            assert!(msg.contains("missing required field 'name' in policy store entry"));
        }

        #[test]
        fn test_parse_cedar_error() {
            static POLICY_STORE_RAW_YAML: &str =
                include_str!("../../../../test_files/policy-store_schema_err_cedar_mistake.yaml");

            let policy_result = serde_yml::from_str::<AgamaPolicyStore>(POLICY_STORE_RAW_YAML);
            let err_msg = policy_result.unwrap_err().to_string();
            assert_eq!(
                err_msg,
                "error parsing policy store 'a1bf93115de86de760ee0bea1d529b521489e5a11747': missing required field 'name' in policy store entry"
            );
        }
    }
}
