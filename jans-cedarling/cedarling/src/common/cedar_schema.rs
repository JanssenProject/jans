/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

pub(crate) use cedar_json::CedarSchemaJson;
pub(crate) mod cedar_json;

/// cedar_schema value which specifies both encoding and content_type
///
/// encoding is one of none or base64
/// content_type is one of cedar or cedar-json#[derive(Debug, Clone, serde::Deserialize)]
#[derive(Debug, Clone, serde::Deserialize)]
struct EncodedSchema {
    pub encoding: super::Encoding,
    pub content_type: super::ContentType,
    pub body: String,
}

/// Intermediate struct to handle both kinds of cedar_schema values.
///
/// Either
///   "cedar_schema": "cGVybWl0KA..."
/// OR
///   "cedar_schema": { "encoding": "...", "content_type": "...", "body": "permit(...)"}#[derive(Debug, Clone, serde::Deserialize)]
#[derive(Debug, Clone, serde::Deserialize)]
#[serde(untagged)]
enum MaybeEncoded {
    Plain(String),
    Tagged(EncodedSchema),
}

/// Box that holds the [`cedar_policy::Schema`] and
/// JSON representation that is used to create entities from the schema in the policy store.
#[derive(Debug, Clone)]
pub(crate) struct CedarSchema {
    pub schema: cedar_policy::Schema,
    pub json: cedar_json::CedarSchemaJson,
}

impl PartialEq for CedarSchema {
    fn eq(&self, other: &Self) -> bool {
        // Have to check principals, resources, action_groups, entity_types,
        // actions. Those can contain duplicates, and are not stored in comparison order.
        // So use HashSet to compare them.
        use std::collections::HashSet;

        let self_principals = self.schema.principals().collect::<HashSet<_>>();
        let other_principals = other.schema.principals().collect::<HashSet<_>>();
        if self_principals != other_principals {
            return false;
        }

        let self_resources = self.schema.resources().collect::<HashSet<_>>();
        let other_resources = other.schema.resources().collect::<HashSet<_>>();
        if self_resources != other_resources {
            return false;
        }

        let self_action_groups = self.schema.action_groups().collect::<HashSet<_>>();
        let other_action_groups = other.schema.action_groups().collect::<HashSet<_>>();
        if self_action_groups != other_action_groups {
            return false;
        }

        let self_entity_types = self.schema.entity_types().collect::<HashSet<_>>();
        let other_entity_types = other.schema.entity_types().collect::<HashSet<_>>();
        if self_entity_types != other_entity_types {
            return false;
        }

        let self_actions = self.schema.actions().collect::<HashSet<_>>();
        let other_actions = other.schema.actions().collect::<HashSet<_>>();
        if self_actions != other_actions {
            return false;
        }

        // and this only checks the schema anyway
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

        Ok(CedarSchema { schema, json })
    }
}

mod deserialize {
    #[derive(Debug, thiserror::Error)]
    pub enum ParseCedarSchemaSetMessage {
        #[error("Failed to decode Base64 encoded string")]
        Base64,
        #[error("unable to unmarshal cedar policy schema json to the structure")]
        CedarSchemaJsonFormat,
        #[error("unable to parse cedar policy schema")]
        Parse,
        #[error("invalid utf8 detected while decoding cedar policy")]
        Utf8,
    }

    #[cfg(test)]
    mod tests {
        use super::*;
        use crate::common::policy_store::PolicyStore;

        #[test]
        fn test_read_ok() {
            // The human-readable policy and schema file is located in next folder:
            // `test_files\policy-store_ok`
            static POLICY_STORE_RAW: &str =
                include_str!("../../../test_files/policy-store_ok.yaml");

            let policy_result = PolicyStore::load_from_yaml(POLICY_STORE_RAW);
            assert!(policy_result.is_ok(), "{:?}", policy_result.unwrap_err());
        }

        #[test]
        fn test_readable_yaml_ok() {
            static YAML_POLICY_STORE: &str =
                include_str!("../../../test_files/policy-store_readable.yaml");
            let yaml_policy_result = PolicyStore::load_from_yaml(YAML_POLICY_STORE);
            assert!(
                yaml_policy_result.is_ok(),
                "{:?}",
                yaml_policy_result.unwrap_err()
            );
        }

        #[test]
        fn test_read_base64_error() {
            static POLICY_STORE_RAW: &str =
                include_str!("../../../test_files/policy-store_schema_err_base64.json");

            let policy_result = PolicyStore::load_from_json(POLICY_STORE_RAW);
            let err = policy_result.unwrap_err();
            let msg = err.to_string();
            assert!(
                msg.contains(&ParseCedarSchemaSetMessage::Base64.to_string()),
                "{err:?}"
            );
        }

        #[test]
        fn test_read_yaml_error() {
            static POLICY_STORE_RAW_YAML: &str =
                include_str!("../../../test_files/policy-store_schema_err.yaml");

            let policy_result = PolicyStore::load_from_yaml(POLICY_STORE_RAW_YAML);
            let err = policy_result.unwrap_err();
            let msg = err.to_string();
            assert!(msg.contains("unexpected end of input"), "{err:?}");
        }

        #[test]
        fn test_parse_cedar_error() {
            static POLICY_STORE_RAW_YAML: &str =
                include_str!("../../../test_files/policy-store_schema_err_cedar_mistake.yaml");

            let policy_result = PolicyStore::load_from_yaml(POLICY_STORE_RAW_YAML);
            let err_msg = policy_result.unwrap_err().to_string();
            assert!(err_msg.contains("TypeNotDefinedError"));
        }
    }
}
