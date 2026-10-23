// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `trusted-issuers.json` parse structs for format v2.
//!
//! The file is one JSON object keyed by Cedar entity type; each entry fully
//! describes one token type, including its issuer and kind.

use std::collections::HashSet;
use std::fmt;
use std::str::FromStr;

use serde::Deserialize;
use serde::de::{self, Deserializer, MapAccess, Visitor};
use url::Url;

use crate::common::policy_store::token_types::{EntityTypeName, IssuerName, TokenTypesBuilder};
use crate::common::policy_store::{
    CustomTokenMetadata, KeySource, TokenEntityMetadata, TokenTypes, TokenTypesError,
    iss_from_configuration,
};

/// The whole file, in file order. Duplicate keys are rejected while parsing:
/// a plain map would silently keep the last one.
#[derive(Debug, Clone, Default, PartialEq)]
pub(crate) struct TrustedIssuersDoc(pub(crate) Vec<(EntityTypeName, TokenTypeConfig)>);

/// One token type; `type` selects which fields are allowed.
#[derive(Debug, Clone, PartialEq, Deserialize)]
#[serde(tag = "type", rename_all = "snake_case")]
pub(crate) enum TokenTypeConfig {
    Jwt(Box<JwtTokenConfig>),
    Custom(CustomTokenConfig),
}

#[derive(Debug, Clone, PartialEq, Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct JwtTokenConfig {
    #[serde(deserialize_with = "de_issuer")]
    pub(crate) issuer: IssuerName,
    pub(crate) configuration_endpoints: KeySourceDoc,
    #[serde(default = "default_token_id", deserialize_with = "de_token_id")]
    pub(crate) token_id: String,
    #[serde(default, deserialize_with = "de_required_claims")]
    pub(crate) required_claims: HashSet<String>,
}

#[derive(Debug, Clone, PartialEq, Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct CustomTokenConfig {
    #[serde(deserialize_with = "de_issuer")]
    pub(crate) issuer: IssuerName,
    #[serde(default, deserialize_with = "de_required_claims")]
    pub(crate) required_claims: HashSet<String>,
}

// Frozen v2 default; independent of the runtime `DEFAULT_TKN_ID`.
fn default_token_id() -> String {
    "jti".to_string()
}

fn de_token_id<'de, D: Deserializer<'de>>(deserializer: D) -> Result<String, D::Error> {
    let token_id = String::deserialize(deserializer)?;
    if token_id.is_empty() {
        return Err(de::Error::custom("`token_id` must not be empty"));
    }
    Ok(token_id)
}

/// A set would silently merge a repeated claim, so it is rejected instead.
fn de_required_claims<'de, D: Deserializer<'de>>(
    deserializer: D,
) -> Result<HashSet<String>, D::Error> {
    let claims = Vec::<String>::deserialize(deserializer)?;
    let mut unique = HashSet::with_capacity(claims.len());
    for claim in claims {
        if claim.is_empty() {
            return Err(de::Error::custom(
                "`required_claims` must not contain an empty claim name",
            ));
        }
        if let Some(claim) = unique.replace(claim) {
            return Err(de::Error::custom(format!(
                "`required_claims` lists '{claim}' more than once"
            )));
        }
    }
    Ok(unique)
}

fn de_issuer<'de, D: Deserializer<'de>>(deserializer: D) -> Result<IssuerName, D::Error> {
    let name = String::deserialize(deserializer)?;
    IssuerName::from_str(&name).map_err(de::Error::custom)
}

/// Where an issuer's keys and expected `iss` come from.
#[derive(Debug, Clone, PartialEq)]
pub(crate) enum KeySourceDoc {
    /// OIDC discovery; `iss` is derived from `configuration` while parsing.
    Discovery { configuration: Url, iss: Url },
    /// Keys from `jwks`, with `iss` given explicitly.
    Static { jwks: Url, iss: Url },
}

/// Hand-written so a store author sees the actual problem rather than
/// serde's "did not match any variant".
impl<'de> Deserialize<'de> for KeySourceDoc {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        deserializer.deserialize_map(KeySourceVisitor)
    }
}

struct KeySourceVisitor;

impl<'de> Visitor<'de> for KeySourceVisitor {
    type Value = KeySourceDoc;

    fn expecting(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("an object with `configuration`, or `jwks` and `iss`")
    }

    fn visit_map<A: MapAccess<'de>>(self, mut map: A) -> Result<Self::Value, A::Error> {
        const FIELDS: &[&str] = &["configuration", "jwks", "iss"];
        let mut configuration: Option<Url> = None;
        let mut jwks: Option<Url> = None;
        let mut iss: Option<Url> = None;

        while let Some(key) = map.next_key::<String>()? {
            let slot = match key.as_str() {
                "configuration" => &mut configuration,
                "jwks" => &mut jwks,
                "iss" => &mut iss,
                _ => return Err(de::Error::unknown_field(&key, FIELDS)),
            };
            if slot.is_some() {
                return Err(de::Error::custom(format!("duplicate field `{key}`")));
            }
            let value = map.next_value::<String>()?;
            let url = Url::parse(&value).map_err(|e| {
                de::Error::custom(format!("`{key}` is not a valid URL '{value}': {e}"))
            })?;
            // Keys and discovery are only fetched over HTTP; `http` is kept for local setups.
            if !matches!(url.scheme(), "http" | "https") {
                return Err(de::Error::custom(format!(
                    "`{key}` must be an http or https URL, got '{value}'"
                )));
            }
            *slot = Some(url);
        }

        match (configuration, jwks, iss) {
            (Some(_), Some(_), _) => Err(de::Error::custom(
                "`configuration` and `jwks` are mutually exclusive",
            )),
            (Some(_), None, Some(_)) => Err(de::Error::custom(
                "`iss` is derived from `configuration`; it is only allowed with `jwks`",
            )),
            (Some(configuration), None, None) => {
                let iss = iss_from_configuration(&configuration).map_err(de::Error::custom)?;
                Ok(KeySourceDoc::Discovery { configuration, iss })
            },
            (None, Some(_), None) => Err(de::Error::custom("`jwks` requires `iss`")),
            (None, Some(_), Some(iss)) if iss.query().is_some() || iss.fragment().is_some() => {
                // An issuer identifier has no query or fragment (RFC 8414 §2).
                Err(de::Error::custom(format!(
                    "`iss` must not have a query or fragment, got '{iss}'"
                )))
            },
            (None, Some(jwks), Some(iss)) => Ok(KeySourceDoc::Static { jwks, iss }),
            (None, None, Some(_)) => Err(de::Error::custom("`iss` requires `jwks`")),
            (None, None, None) => Err(de::Error::custom("expected `configuration` or `jwks`")),
        }
    }
}

impl<'de> Deserialize<'de> for TrustedIssuersDoc {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        deserializer.deserialize_map(TrustedIssuersVisitor)
    }
}

struct TrustedIssuersVisitor;

impl<'de> Visitor<'de> for TrustedIssuersVisitor {
    type Value = TrustedIssuersDoc;

    fn expecting(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("an object keyed by Cedar entity type")
    }

    fn visit_map<A: MapAccess<'de>>(self, mut map: A) -> Result<Self::Value, A::Error> {
        let mut seen = HashSet::new();
        let mut entries = Vec::new();
        while let Some(key) = map.next_key::<String>()? {
            if !seen.insert(key.clone()) {
                return Err(de::Error::custom(format!("duplicate entity type '{key}'")));
            }
            let entity_type = EntityTypeName::from_str(&key).map_err(de::Error::custom)?;
            let config = map
                .next_value::<TokenTypeConfig>()
                .map_err(|e| de::Error::custom(format!("invalid entry '{key}': {e}")))?;
            entries.push((entity_type, config));
        }
        Ok(TrustedIssuersDoc(entries))
    }
}

/// Parses `trusted-issuers.json` content.
pub(crate) fn parse(json: &str) -> Result<TrustedIssuersDoc, serde_json::Error> {
    serde_json::from_str(json)
}

impl TrustedIssuersDoc {
    /// Validates the entries as a whole and converts them into runtime types.
    pub(crate) fn into_runtime(self) -> Result<TokenTypes, TokenTypesError> {
        let mut builder = TokenTypesBuilder::default();
        for (entity_type, config) in self.0 {
            match config {
                TokenTypeConfig::Jwt(jwt) => {
                    let jwt = *jwt;
                    let (iss, key_source) = match jwt.configuration_endpoints {
                        KeySourceDoc::Discovery { configuration, iss } => {
                            (iss, KeySource::Discovery { configuration })
                        },
                        KeySourceDoc::Static { jwks, iss } => (iss, KeySource::Static { jwks }),
                    };
                    let token = TokenEntityMetadata::builder()
                        .token_id(jwt.token_id)
                        .required_claims(jwt.required_claims)
                        .build();
                    builder.add_jwt(entity_type, jwt.issuer, iss, key_source, token)?;
                },
                TokenTypeConfig::Custom(custom) => {
                    let token = CustomTokenMetadata {
                        required_claims: custom.required_claims,
                    };
                    builder.add_custom(entity_type, custom.issuer, token)?;
                },
            }
        }
        builder.finish()
    }
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::*;
    use crate::common::policy_store::TokenTypeKind;

    /// The example file of the design.
    fn example() -> serde_json::Value {
        json!({
            "Acme::Access_token": {
                "type": "jwt",
                "issuer": "Acme",
                "configuration_endpoints": {
                    "configuration": "https://idp.acme.com/.well-known/openid-configuration"
                },
                "token_id": "jti",
                "required_claims": ["jti", "iss", "aud", "sub", "exp"]
            },
            "Acme::Id_token": {
                "type": "jwt",
                "issuer": "Acme",
                "configuration_endpoints": {
                    "configuration": "https://idp.acme.com/.well-known/openid-configuration"
                },
                "required_claims": ["iss", "aud", "sub", "exp"]
            },
            "Partner::Access_token": {
                "type": "jwt",
                "issuer": "Partner",
                "configuration_endpoints": {
                    "jwks": "https://auth.partner.io/keys",
                    "iss": "https://auth.partner.io"
                },
                "required_claims": ["iss", "exp"]
            },
            "Dolphin::Dolphin_token": {
                "type": "custom",
                "issuer": "Dolphin",
                "required_claims": ["sub"]
            }
        })
    }

    /// Parses one entry under `Acme::Access_token`.
    fn parse_entry(entry: &serde_json::Value) -> Result<TrustedIssuersDoc, serde_json::Error> {
        parse(&json!({ "Acme::Access_token": entry }).to_string())
    }

    #[track_caller]
    fn assert_rejected(entry: &serde_json::Value, expected: &str) {
        let err = parse_entry(entry).expect_err("the entry must be rejected");
        assert!(
            err.to_string().contains(expected),
            "error should say `{expected}`, got: {err}"
        );
    }

    #[test]
    fn example_parses_into_token_types() {
        let types = parse(&example().to_string())
            .expect("the example should parse")
            .into_runtime()
            .expect("the example should be consistent");

        let acme = &types.jwt_issuers()["Acme"];
        assert_eq!(
            acme.iss().as_str(),
            "https://idp.acme.com/",
            "iss is derived"
        );
        assert_eq!(
            types.jwt_tokens_of(&IssuerName::for_test("Acme")).count(),
            2,
            "both Acme entries share one issuer"
        );
        let partner = &types.jwt_issuers()["Partner"];
        assert_eq!(
            partner.key_source,
            KeySource::Static {
                jwks: Url::parse("https://auth.partner.io/keys").expect("valid url"),
            },
            "the static form should keep its jwks"
        );
        assert_eq!(
            partner.iss().as_str(),
            "https://auth.partner.io/",
            "explicit iss"
        );

        let id_token = types.get("Acme::Id_token").expect("Acme::Id_token");
        assert!(
            matches!(&id_token.kind, TokenTypeKind::Jwt { metadata, .. } if metadata.token_id == "jti"),
            "token_id should default to jti"
        );
        let dolphin = types.get("Dolphin::Dolphin_token").expect("Dolphin token");
        assert_eq!(dolphin.context_key, "dolphin_dolphin_token", "custom key");
        assert!(
            matches!(&dolphin.kind, TokenTypeKind::Custom(token) if token.required_claims.contains("sub")),
            "a custom entry should keep its required claims"
        );
    }

    #[test]
    fn rejects_duplicate_entity_type() {
        let json = r#"{
            "Acme::Access_token": { "type": "custom", "issuer": "Acme" },
            "Acme::Access_token": { "type": "custom", "issuer": "Other" }
        }"#;
        let err = parse(json).expect_err("a duplicate key must not be last-wins");
        assert!(
            err.to_string()
                .contains("duplicate entity type 'Acme::Access_token'"),
            "error should name the key, got: {err}"
        );
    }

    #[test]
    fn rejects_invalid_entity_type_key() {
        let err = parse(r#"{ "Acme Token": { "type": "custom", "issuer": "Acme" } }"#)
            .expect_err("an invalid entity type must be rejected");
        assert!(
            err.to_string()
                .contains("'Acme Token' is not a valid Cedar entity type"),
            "error should name the key, got: {err}"
        );
    }

    #[test]
    fn rejects_jwt_field_on_custom_entry() {
        assert_rejected(
            &json!({
                "type": "custom",
                "issuer": "Acme",
                "configuration_endpoints": { "jwks": "https://a/k", "iss": "https://a" }
            }),
            "unknown field `configuration_endpoints`",
        );
    }

    #[test]
    fn rejects_missing_or_unknown_type() {
        assert_rejected(&json!({ "issuer": "Acme" }), "missing field `type`");
        assert_rejected(
            &json!({ "type": "saml", "issuer": "Acme" }),
            "unknown variant `saml`",
        );
    }

    #[test]
    fn rejects_empty_token_id() {
        assert_rejected(
            &json!({
                "type": "jwt",
                "issuer": "Acme",
                "configuration_endpoints": { "jwks": "https://a/k", "iss": "https://a" },
                "token_id": ""
            }),
            "`token_id` must not be empty",
        );
    }

    #[test]
    fn rejects_empty_or_repeated_required_claims() {
        let jwt = |claims: serde_json::Value| {
            json!({
                "type": "jwt",
                "issuer": "Acme",
                "configuration_endpoints": { "jwks": "https://a/k", "iss": "https://a" },
                "required_claims": claims
            })
        };
        let custom = |claims: serde_json::Value| json!({ "type": "custom", "issuer": "Acme", "required_claims": claims });
        for entry in [jwt(json!(["sub", ""])), custom(json!([""]))] {
            assert_rejected(
                &entry,
                "`required_claims` must not contain an empty claim name",
            );
        }
        for entry in [
            jwt(json!(["sub", "exp", "sub"])),
            custom(json!(["sub", "sub"])),
        ] {
            assert_rejected(&entry, "`required_claims` lists 'sub' more than once");
        }
    }

    #[test]
    fn rejects_issuer_that_is_not_one_identifier() {
        for issuer in ["Acme Incorporated", "Acme::Corp", "if", "__cedar"] {
            assert_rejected(
                &json!({ "type": "custom", "issuer": issuer }),
                &format!("issuer '{issuer}' must be a single Cedar identifier"),
            );
        }
    }

    #[test]
    fn key_source_errors_name_the_problem() {
        let jwt = |endpoints: serde_json::Value| json!({ "type": "jwt", "issuer": "Acme", "configuration_endpoints": endpoints });
        for (endpoints, expected) in [
            (json!({}), "expected `configuration` or `jwks`"),
            (json!({ "jwks": "https://a/keys" }), "`jwks` requires `iss`"),
            (json!({ "iss": "https://a" }), "`iss` requires `jwks`"),
            (
                json!({
                    "configuration": "https://a/.well-known/openid-configuration",
                    "jwks": "https://a/keys"
                }),
                "`configuration` and `jwks` are mutually exclusive",
            ),
            (
                json!({
                    "configuration": "https://a/.well-known/openid-configuration",
                    "iss": "https://a"
                }),
                "it is only allowed with `jwks`",
            ),
            (
                json!({ "jwks_uri": "https://a/keys" }),
                "unknown field `jwks_uri`",
            ),
            (
                json!({ "jwks": "not a url", "iss": "https://a" }),
                "`jwks` is not a valid URL",
            ),
            (
                json!({ "configuration": "file:///.well-known/openid-configuration" }),
                "`configuration` must be an http or https URL, got 'file:///.well-known/openid-configuration'",
            ),
            (
                json!({ "configuration": "ftp://a/.well-known/openid-configuration" }),
                "`configuration` must be an http or https URL",
            ),
            (
                json!({ "jwks": "file:///etc/passwd", "iss": "https://a" }),
                "`jwks` must be an http or https URL, got 'file:///etc/passwd'",
            ),
            (
                json!({ "jwks": "https://a/keys", "iss": "urn:example:a" }),
                "`iss` must be an http or https URL",
            ),
            (
                json!({ "jwks": "https://a/keys", "iss": "https://idp.x/a?x=1" }),
                "`iss` must not have a query or fragment, got 'https://idp.x/a?x=1'",
            ),
            (
                json!({ "jwks": "https://a/keys", "iss": "https://idp.x/a#f" }),
                "`iss` must not have a query or fragment, got 'https://idp.x/a#f'",
            ),
        ] {
            assert_rejected(&jwt(endpoints), expected);
        }
    }

    #[test]
    fn rejects_configuration_not_of_discovery_shape() {
        assert_rejected(
            &json!({
                "type": "jwt",
                "issuer": "Acme",
                "configuration_endpoints": { "configuration": "https://idp.acme.com/auth" }
            }),
            "configuration endpoint 'https://idp.acme.com/auth' must be the issuer URL",
        );
    }

    #[test]
    fn conflicts_between_entries_fail_conversion() {
        let doc = parse(
            &json!({
                "Acme::Access_token": {
                    "type": "jwt",
                    "issuer": "Acme",
                    "configuration_endpoints": {
                        "configuration": "https://idp.acme.com/.well-known/openid-configuration"
                    }
                },
                "Acme::Id_token": {
                    "type": "jwt",
                    "issuer": "Acme",
                    "configuration_endpoints": {
                        "configuration": "https://other.acme.com/.well-known/openid-configuration"
                    }
                }
            })
            .to_string(),
        )
        .expect("each entry is valid on its own");

        let err = doc
            .into_runtime()
            .expect_err("one issuer must have one iss");
        assert_eq!(
            err,
            TokenTypesError::IssuerConflict {
                issuer: "Acme".into(),
                entity_type_a: "Acme::Access_token".into(),
                entity_type_b: "Acme::Id_token".into(),
                difference: "iss 'https://idp.acme.com' and 'https://other.acme.com'".into(),
            }
        );
    }

    /// `schema/trusted_issuers_schema.json`, the published shape of this file.
    fn schema_validator() -> jsonschema::Validator {
        let path = std::path::Path::new(env!("CARGO_MANIFEST_DIR"))
            .join("../schema/trusted_issuers_schema.json");
        let schema: serde_json::Value = serde_json::from_str(
            &std::fs::read_to_string(&path).expect("the schema file should be readable"),
        )
        .expect("the schema should be JSON");
        jsonschema::draft202012::new(&schema).expect("the schema should compile")
    }

    /// The example and every v2 fixture in the repository pass the schema and load.
    #[test]
    fn schema_and_parser_accept_the_fixtures() {
        let validator = schema_validator();
        let root = std::path::Path::new(env!("CARGO_MANIFEST_DIR")).join("..");
        let mut stores = vec![("example".to_string(), example())];
        for fixture in [
            "bindings/cedarling-java/src/test/resources/config/multiIssuer/policy-store",
            "bindings/cedarling_python/example_files/policy-store-multi-issuer",
        ] {
            let path = root.join(fixture).join("trusted-issuers.json");
            let json = std::fs::read_to_string(&path).expect("the fixture should be readable");
            stores.push((
                fixture.to_string(),
                serde_json::from_str(&json).expect("the fixture should be JSON"),
            ));
        }

        for (name, store) in stores {
            let errors: Vec<String> = validator
                .iter_errors(&store)
                .map(|e| e.to_string())
                .collect();
            assert!(
                errors.is_empty(),
                "{name} should pass the schema: {errors:?}"
            );
            parse(&store.to_string())
                .unwrap_or_else(|e| panic!("{name} should parse: {e}"))
                .into_runtime()
                .unwrap_or_else(|e| panic!("{name} should convert: {e}"));
        }
    }

    /// Entries the schema can judge on their own are rejected by both, so the two
    /// cannot drift apart unnoticed.
    #[test]
    fn schema_and_parser_reject_the_same_entries() {
        let validator = schema_validator();
        let jwt = |extra: serde_json::Value| {
            let mut entry = json!({
                "type": "jwt",
                "issuer": "Acme",
                "configuration_endpoints": { "jwks": "https://a/k", "iss": "https://a" }
            });
            entry
                .as_object_mut()
                .expect("an object")
                .extend(extra.as_object().expect("an object").clone());
            entry
        };
        let endpoints =
            |endpoints: serde_json::Value| jwt(json!({ "configuration_endpoints": endpoints }));
        for (label, entry) in [
            ("empty token_id", jwt(json!({ "token_id": "" }))),
            ("empty claim", jwt(json!({ "required_claims": [""] }))),
            (
                "repeated claim",
                jwt(json!({ "required_claims": ["sub", "sub"] })),
            ),
            ("unknown field", jwt(json!({ "trusted": true }))),
            ("reserved issuer", jwt(json!({ "issuer": "if" }))),
            ("issuer with ::", jwt(json!({ "issuer": "Acme::Corp" }))),
            (
                "missing key source",
                json!({ "type": "jwt", "issuer": "Acme" }),
            ),
            ("unknown type", json!({ "type": "saml", "issuer": "Acme" })),
            (
                "custom with key source",
                json!({
                    "type": "custom",
                    "issuer": "Acme",
                    "configuration_endpoints": { "jwks": "https://a/k", "iss": "https://a" }
                }),
            ),
            (
                "jwks without iss",
                endpoints(json!({ "jwks": "https://a/k" })),
            ),
            (
                "both forms",
                endpoints(json!({
                    "configuration": "https://a/.well-known/openid-configuration",
                    "jwks": "https://a/k"
                })),
            ),
            (
                "not a discovery URL",
                endpoints(json!({ "configuration": "https://a/config" })),
            ),
            (
                "file scheme",
                endpoints(json!({ "jwks": "file:///etc/passwd", "iss": "https://a" })),
            ),
            (
                "iss with query",
                endpoints(json!({ "jwks": "https://a/k", "iss": "https://a?x=1" })),
            ),
            (
                "iss with fragment",
                endpoints(json!({ "jwks": "https://a/k", "iss": "https://a#f" })),
            ),
        ] {
            let store = json!({ "Acme::Access_token": entry });
            assert!(
                !validator.is_valid(&store),
                "the schema should reject {label}"
            );
            assert!(
                parse(&store.to_string()).is_err(),
                "the parser should reject {label}"
            );
        }
    }
}
