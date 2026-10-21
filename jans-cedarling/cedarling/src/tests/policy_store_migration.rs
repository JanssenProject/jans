// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! A golden v1 store, migrated to v2 when loaded, authorizes as it did in v1:
//! the `context.tokens` keys and `TrustedIssuer` types policies rely on are
//! unchanged, including an issuer `name` that differs from its token namespace
//! and a custom issuer named by its file.

use std::collections::HashMap;
use std::sync::Arc;

use async_trait::async_trait;

use super::utils::cedarling_util::get_cedarling_with_callback;
use super::utils::*;
use crate::authz::request::{AuthorizeMultiIssuerRequest, EntityData, TokenInput};
use crate::common::policy_store::test_utils::PolicyStoreTestBuilder;
use crate::log::interface::LogStorage;
use crate::{Cedarling, CustomTokenError, CustomTokenProcessor, ProcessedTokenClaims};

const ISS: &str = "https://idp.gluu.test";

const SCHEMA: &str = r#"
namespace Jans {
    type Url = {"host": String, "path": String, "protocol": String};
    entity Access_token = {
        token_type: String,
        jti: String,
        iss: Gluu::TrustedIssuer,
        validated_at: Long,
        exp: Long,
    } tags Set<String>;
    entity Resource;
    entity Any;
    action "Read" appliesTo {
        principal: [Any],
        resource: [Resource],
        context: Context
    };
}
namespace Gluu {
    entity TrustedIssuer = {"issuer_entity_id": Jans::Url};
}
namespace Acme {
    entity Api_key = {
        token_type: String,
        jti: String,
        iss: String,
        validated_at: Long,
    } tags Set<String>;
}
type Context = {
    tokens: {
        gluu_access_token?: Jans::Access_token,
        acme_api_key?: Acme::Api_key,
        total_token_count: Long,
    }
};
"#;

/// Relies on both v1 keys, the `Gluu` issuer namespace and the custom issuer id.
const POLICY: &str = r#"@id("read-with-both-tokens")
permit(
    principal,
    action == Jans::Action::"Read",
    resource == Jans::Resource::"Doc"
) when {
    context has tokens.gluu_access_token &&
    context.tokens.gluu_access_token.iss == Gluu::TrustedIssuer::"https://idp.gluu.test" &&
    context has tokens.acme_api_key &&
    context.tokens.acme_api_key.iss == "acme"
};"#;

/// The v1 store: issuer `Gluu` issuing `Jans::` tokens, one of them untrusted,
/// and a `required` custom issuer whose id comes from its file name.
fn golden_v1_store() -> Vec<u8> {
    let mut builder = PolicyStoreTestBuilder::new("abc123def456")
        .with_spec_version(1)
        .with_schema(SCHEMA)
        .with_policy("read-with-both-tokens", POLICY)
        .with_trusted_issuer(
            "gluu",
            format!(
                r#"{{
                    "name": "Gluu",
                    "description": "Gluu server",
                    "configuration_endpoint": "{ISS}/.well-known/openid-configuration",
                    "token_metadata": {{
                        "access_token": {{ "entity_type_name": "Jans::Access_token" }},
                        "id_token": {{ "trusted": false, "entity_type_name": "Jans::Id_token" }}
                    }}
                }}"#
            ),
        );
    builder.extra_files.insert(
        "custom-issuers/acme.json".to_string(),
        r#"{ "tokens_mappings": { "Acme::Api_key": { "required": true } } }"#.to_string(),
    );
    builder
        .build_archive()
        .expect("golden v1 store should build")
}

/// Accepts any API key.
struct ApiKeyProcessor;

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl CustomTokenProcessor for ApiKeyProcessor {
    async fn process(
        &self,
        _mapping: &str,
        payload: &str,
    ) -> Result<ProcessedTokenClaims, CustomTokenError> {
        Ok(ProcessedTokenClaims::new(HashMap::new(), payload))
    }
}

async fn cedarling() -> Cedarling {
    let cedarling =
        get_cedarling_with_callback(PolicyStoreSource::ArchiveBytes(golden_v1_store()), |_| {})
            .await;
    cedarling.set_custom_token_processor(Some(Arc::new(ApiKeyProcessor)));
    cedarling
}

fn jwt(iss: &str, jti: &str) -> String {
    generate_token_using_claims(json!({ "iss": iss, "jti": jti, "exp": 2_000_000_000 }))
}

fn read_doc(tokens: Vec<(&str, String)>) -> AuthorizeMultiIssuerRequest {
    AuthorizeMultiIssuerRequest::new_with_fields(
        tokens
            .into_iter()
            .map(|(mapping, payload)| TokenInput::new(mapping.to_string(), payload))
            .collect(),
        EntityData::from_json(
            &json!({ "cedar_entity_mapping": { "entity_type": "Jans::Resource", "id": "Doc" } })
                .to_string(),
        )
        .expect("resource entity should build"),
        "Jans::Action::\"Read\"".to_string(),
        None,
    )
}

#[tokio::test]
async fn migrated_v1_store_keeps_keys_and_issuer_types() {
    let cedarling = cedarling().await;

    let result = cedarling
        .authorize_multi_issuer(read_doc(vec![
            ("Jans::Access_token", jwt(ISS, "a1")),
            ("Acme::Api_key", "key-1".to_string()),
        ]))
        .await
        .expect("both tokens should validate");
    assert!(
        result.decision,
        "the v1 keys gluu_access_token / acme_api_key and Gluu::TrustedIssuer should still \
         reach the policy"
    );
}

#[tokio::test]
async fn migrated_v1_store_keeps_rejecting_what_v1_rejected() {
    let cedarling = cedarling().await;

    for (label, tokens) in [
        (
            "a token from another iss",
            vec![
                ("Jans::Access_token", jwt("https://other.test", "a1")),
                ("Acme::Api_key", "key-1".to_string()),
            ],
        ),
        (
            "a token type that was `trusted: false`",
            vec![
                ("Jans::Id_token", jwt(ISS, "i1")),
                ("Acme::Api_key", "key-1".to_string()),
            ],
        ),
    ] {
        let result = cedarling
            .authorize_multi_issuer(read_doc(tokens))
            .await
            .expect("the API key alone should still validate");
        assert!(!result.decision, "{label} should not satisfy the policy");
        assert_eq!(result.dropped_tokens.len(), 1, "{label} should be dropped");
    }
}

/// `required` no longer fails the request: the token is optional, and the
/// migration says so.
#[tokio::test]
async fn migrated_required_custom_token_is_optional_and_reported() {
    let cedarling = cedarling().await;

    let warnings: Vec<String> = cedarling
        .pop_logs()
        .iter()
        .filter(|log| log["level"] == "WARN")
        .filter_map(|log| log["msg"].as_str().map(str::to_string))
        .collect();
    assert!(
        warnings.iter().any(|msg| msg.contains(
            "'required' on custom token 'Acme::Api_key' in 'custom-issuers/acme.json' is ignored"
        )),
        "the migration should report the ignored `required`, got: {warnings:?}"
    );

    let result = cedarling
        .authorize_multi_issuer(read_doc(vec![("Jans::Access_token", jwt(ISS, "a1"))]))
        .await
        .expect("a missing custom token no longer fails the request");
    assert!(
        !result.decision,
        "a policy needing the API key should deny without it"
    );
}
