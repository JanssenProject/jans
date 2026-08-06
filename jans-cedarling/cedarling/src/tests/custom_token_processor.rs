// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Integration tests for the custom (non-JWT) token processor feature end to end
//! through `Cedarling::authorize_multi_issuer`.

use std::collections::HashMap;
use std::sync::Arc;

use super::utils::cedarling_util::get_cedarling_with_callback;
#[cfg(feature = "blocking")]
use super::utils::cedarling_util::get_config;
use super::utils::*;
use crate::{
    AuthorizeError, CustomTokenError, CustomTokenProcessor, ProcessedTokenClaims,
    authz::{
        MultiIssuerValidationError,
        request::{AuthorizeMultiIssuerRequest, EntityData, TokenInput},
    },
};
use async_trait::async_trait;
use serde_json::json;

/// Policy store with a single custom issuer `CustomKeys` (mapping `Custom::ApiKey`,
/// `required: true`, `required_claims: [sub]`) and a policy that allows reads when
/// the API key carries the `admin` scope. The schema types
/// `context.tokens.customkeys_apikey` as a `Custom::ApiKey` entity so its claim
/// tags are readable.
const POLICY_STORE_RAW: &str = r#"
cedar_version: v4.0.0
policy_stores:
  custom_token_store:
    cedar_version: v4.0.0
    name: CustomTokenExample
    description: Custom (non-JWT) token processing
    trusted_issuers: {}
    custom_issuers:
      CustomKeys:
        entity_type_name: "Custom::ApiKey"
        required: true
        required_claims:
          - sub
    policies:
      allow_admin_api_key:
        description: Allow reads when the API key carries the admin scope
        creation_date: "2026-01-01T00:00:00.000000"
        policy_content:
          encoding: none
          content_type: cedar
          body: |-
            permit(
                principal,
                action == Custom::Action::"Read",
                resource == Custom::Resource::"Doc"
            ) when {
                context has tokens.customkeys_apikey &&
                context.tokens.customkeys_apikey.hasTag("scope") &&
                context.tokens.customkeys_apikey.getTag("scope").contains("admin")
            };
    schema:
      encoding: none
      content_type: cedar
      body: |-
        namespace Custom {
          entity Any;
          entity Resource = {"name": String};
          entity ApiKey = {
            token_type?: String,
            jti?: String,
            iss?: String,
            exp?: Long,
            validated_at?: Long,
          } tags Set<String>;
          action "Read" appliesTo {
            principal: [Any],
            resource: [Resource],
            context: Context
          };
        }
        type Context = {
          tokens: {
            customkeys_apikey?: Custom::ApiKey,
            total_token_count: Long
          }
        };
    default_entities: {}
"#;

/// Treats the payload as an opaque API key: `secret-admin-key` is valid (emits
/// `sub` + `scope=admin`), anything else is rejected.
struct ApiKeyProcessor;

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl CustomTokenProcessor for ApiKeyProcessor {
    async fn process(
        &self,
        mapping: &str,
        payload: &str,
    ) -> Result<ProcessedTokenClaims, CustomTokenError> {
        if payload != "secret-admin-key" {
            return Err(CustomTokenError::Processing(format!(
                "unknown API key for mapping '{mapping}'"
            )));
        }
        let mut claims = HashMap::new();
        claims.insert("sub".to_string(), json!("api-key-user"));
        claims.insert("scope".to_string(), json!("admin"));
        Ok(ProcessedTokenClaims::new(claims, "api-key-1"))
    }
}

fn read_doc_request(payload: &str) -> AuthorizeMultiIssuerRequest {
    AuthorizeMultiIssuerRequest::new_with_fields(
        vec![TokenInput::new(
            "Custom::ApiKey".to_string(),
            payload.to_string(),
        )],
        EntityData::from_json(
            &json!({
                "cedar_entity_mapping": { "entity_type": "Custom::Resource", "id": "Doc" },
                "name": "A protected document"
            })
            .to_string(),
        )
        .expect("resource entity should build"),
        "Custom::Action::\"Read\"".to_string(),
        None,
    )
}

/// Happy path: a registered processor's claims flow into `context.tokens.*` and
/// are readable by the policy → ALLOW.
#[tokio::test]
async fn custom_token_happy_path_allows() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        |_| {},
    )
    .await;
    cedarling.set_custom_token_processor(Some(Arc::new(ApiKeyProcessor)));

    let result = cedarling
        .authorize_multi_issuer(read_doc_request("secret-admin-key"))
        .await
        .expect("authorization should succeed");
    assert!(
        result.decision,
        "valid admin API key should be ALLOW (claims must reach context.tokens.customkeys_apikey)"
    );
}

/// A registered processor that rejects the payload + a `required` issuer → the
/// whole request fails (not a silent DENY).
#[tokio::test]
async fn custom_token_required_rejection_fails_request() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        |_| {},
    )
    .await;
    cedarling.set_custom_token_processor(Some(Arc::new(ApiKeyProcessor)));

    let err = cedarling
        .authorize_multi_issuer(read_doc_request("not-a-real-key"))
        .await
        .expect_err("required issuer + processor rejection should fail the request");
    assert!(
        matches!(
            err,
            AuthorizeError::MultiIssuerValidation(MultiIssuerValidationError::CustomToken(
                CustomTokenError::Processing(_)
            ))
        ),
        "expected a processing failure, got: {err:?}"
    );
}

/// `set_custom_token_processor(None)` clears a previously registered processor;
/// a subsequent token for a `required` custom mapping falls back to
/// `NoProcessorRegistered` (fail-closed).
#[tokio::test]
async fn set_custom_token_processor_none_clears() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        |_| {},
    )
    .await;
    cedarling.set_custom_token_processor(Some(Arc::new(ApiKeyProcessor)));

    // Sanity: works while registered.
    cedarling
        .authorize_multi_issuer(read_doc_request("secret-admin-key"))
        .await
        .expect("should authorize while a processor is registered");

    // Clear it.
    cedarling.set_custom_token_processor(None);

    let err = cedarling
        .authorize_multi_issuer(read_doc_request("secret-admin-key"))
        .await
        .expect_err("after clearing, a required custom mapping should fail closed");
    assert!(
        matches!(
            err,
            AuthorizeError::MultiIssuerValidation(MultiIssuerValidationError::CustomToken(
                CustomTokenError::NoProcessorRegistered(_)
            ))
        ),
        "expected NoProcessorRegistered, got: {err:?}"
    );
}

/// Blocking client routes `authorize_multi_issuer` through the registered custom
/// processor via `runtime.block_on`.
#[cfg(feature = "blocking")]
#[test]
fn blocking_client_routes_through_custom_processor() {
    let config = get_config(PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()));
    let cedarling =
        crate::blocking::Cedarling::new(&config).expect("blocking Cedarling should initialize");
    cedarling.set_custom_token_processor(Some(Arc::new(ApiKeyProcessor)));

    let result = cedarling
        .authorize_multi_issuer(read_doc_request("secret-admin-key"))
        .expect("blocking authorization should succeed");
    assert!(
        result.decision,
        "blocking client must route through the registered custom processor"
    );
}
