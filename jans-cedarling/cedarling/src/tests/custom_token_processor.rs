// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Integration tests for the custom (non-JWT) token processor feature end to end
//! through `Cedarling::authorize_multi_issuer`.

use std::collections::HashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::time::Duration;

use super::utils::cedarling_util::{get_cedarling_with_callback, get_config};
use super::utils::*;
use crate::{
    AuthorizeError, CustomTokenError, CustomTokenProcessor, ProcessedTokenClaims,
    authz::{
        MultiIssuerValidationError,
        request::{
            AuthorizeMultiIssuerRequest, BatchAuthorizeMultiIssuerRequest, BatchItem, EntityData,
            TokenInput,
        },
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
        tokens_mappings:
          "Custom::ApiKey":
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

/// Emits `scope` but never `sub`, to trip the store's `required_claims: [sub]`.
struct NoSubProcessor;

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl CustomTokenProcessor for NoSubProcessor {
    async fn process(
        &self,
        _mapping: &str,
        _payload: &str,
    ) -> Result<ProcessedTokenClaims, CustomTokenError> {
        let mut claims = HashMap::new();
        claims.insert("scope".to_string(), json!("admin"));
        Ok(ProcessedTokenClaims::new(claims, "api-key-1"))
    }
}

/// Sleeps past any realistic deadline before succeeding, to exercise the native
/// processing-timeout path.
struct SlowProcessor;

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl CustomTokenProcessor for SlowProcessor {
    async fn process(
        &self,
        _mapping: &str,
        _payload: &str,
    ) -> Result<ProcessedTokenClaims, CustomTokenError> {
        tokio::time::sleep(Duration::from_millis(300)).await;
        let mut claims = HashMap::new();
        claims.insert("sub".to_string(), json!("api-key-user"));
        claims.insert("scope".to_string(), json!("admin"));
        Ok(ProcessedTokenClaims::new(claims, "api-key-1"))
    }
}

/// Counts invocations so caching behavior can be observed end to end. `cacheable`
/// controls whether the result opts into the token cache.
struct CountingProcessor {
    calls: Arc<AtomicUsize>,
    cacheable: bool,
}

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl CustomTokenProcessor for CountingProcessor {
    async fn process(
        &self,
        _mapping: &str,
        _payload: &str,
    ) -> Result<ProcessedTokenClaims, CustomTokenError> {
        self.calls.fetch_add(1, Ordering::SeqCst);
        let mut claims = HashMap::new();
        claims.insert("sub".to_string(), json!("api-key-user"));
        claims.insert("scope".to_string(), json!("admin"));
        let mut processed = ProcessedTokenClaims::new(claims, "api-key-1");
        processed.cacheable = self.cacheable;
        Ok(processed)
    }
}

/// A `Read`/`Doc` batch item; `id` selects the resource so callers can build both
/// an allowed (`Doc`) and a denied (any other id) item.
fn read_item(resource_id: &str) -> BatchItem {
    BatchItem {
        resource: EntityData::from_json(
            &json!({
                "cedar_entity_mapping": { "entity_type": "Custom::Resource", "id": resource_id },
                "name": "A resource"
            })
            .to_string(),
        )
        .expect("resource entity should build"),
        action: "Custom::Action::\"Read\"".to_string(),
        context: json!({}),
    }
}

/// `required_claims: [sub]` configured on the issuer but the processor omits `sub`
/// → `MissingRequiredClaim` (fail-closed, issuer is `required`).
#[tokio::test]
async fn custom_token_missing_required_claim_fails() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        |_| {},
    )
    .await;
    cedarling.set_custom_token_processor(Some(Arc::new(NoSubProcessor)));

    let err = cedarling
        .authorize_multi_issuer(read_doc_request("secret-admin-key"))
        .await
        .expect_err("missing required claim should fail the request");
    assert!(
        matches!(
            err,
            AuthorizeError::MultiIssuerValidation(MultiIssuerValidationError::CustomToken(
                CustomTokenError::MissingRequiredClaim(ref c)
            )) if c == "sub"
        ),
        "expected MissingRequiredClaim(sub), got: {err:?}"
    );
}

/// A positive `custom_token_processor_timeout_millis` races `process` against the
/// deadline; a slow processor on a `required` issuer surfaces `Timeout`.
#[tokio::test]
async fn custom_token_native_timeout_fails() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        |config| {
            config
                .authorization_config
                .custom_token_processor_timeout_millis = 50;
        },
    )
    .await;
    cedarling.set_custom_token_processor(Some(Arc::new(SlowProcessor)));

    let err = cedarling
        .authorize_multi_issuer(read_doc_request("secret-admin-key"))
        .await
        .expect_err("slow processor past the deadline should fail");
    assert!(
        matches!(
            err,
            AuthorizeError::MultiIssuerValidation(MultiIssuerValidationError::CustomToken(
                CustomTokenError::Timeout(_)
            ))
        ),
        "expected Timeout, got: {err:?}"
    );
}

/// A cacheable result is served from the token cache on the second identical
/// request: `process` runs exactly once.
#[tokio::test]
async fn custom_token_cacheable_result_reused() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        |_| {},
    )
    .await;
    let calls = Arc::new(AtomicUsize::new(0));
    cedarling.set_custom_token_processor(Some(Arc::new(CountingProcessor {
        calls: calls.clone(),
        cacheable: true,
    })));

    for _ in 0..2 {
        cedarling
            .authorize_multi_issuer(read_doc_request("secret-admin-key"))
            .await
            .expect("cacheable custom token should authorize");
    }
    assert_eq!(
        calls.load(Ordering::SeqCst),
        1,
        "cacheable result must be reused on the second identical request"
    );
}

/// Swapping the processor flushes cached verdicts: the replacement runs on the
/// next identical request instead of the previous processor's cached result being
/// served (which would also carry its stale identity metadata).
#[tokio::test]
async fn set_custom_token_processor_swap_invalidates_cache() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        |_| {},
    )
    .await;

    let first_calls = Arc::new(AtomicUsize::new(0));
    cedarling.set_custom_token_processor(Some(Arc::new(CountingProcessor {
        calls: first_calls.clone(),
        cacheable: true,
    })));
    cedarling
        .authorize_multi_issuer(read_doc_request("secret-admin-key"))
        .await
        .expect("first processor should authorize");
    assert_eq!(
        first_calls.load(Ordering::SeqCst),
        1,
        "first processor should run once and cache its verdict"
    );

    // Swap in a replacement for the same payload.
    let second_calls = Arc::new(AtomicUsize::new(0));
    cedarling.set_custom_token_processor(Some(Arc::new(CountingProcessor {
        calls: second_calls.clone(),
        cacheable: true,
    })));
    cedarling
        .authorize_multi_issuer(read_doc_request("secret-admin-key"))
        .await
        .expect("replacement processor should authorize");

    assert_eq!(
        second_calls.load(Ordering::SeqCst),
        1,
        "swapping the processor must flush the cache so the replacement runs, \
         not serve the previous processor's cached verdict"
    );
    assert_eq!(
        first_calls.load(Ordering::SeqCst),
        1,
        "the replaced processor must not be consulted after the swap"
    );
}

#[tokio::test]
async fn custom_token_duplicate_mapping_skips_processing() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        |_| {},
    )
    .await;
    let calls = Arc::new(AtomicUsize::new(0));
    cedarling.set_custom_token_processor(Some(Arc::new(CountingProcessor {
        calls: calls.clone(),
        cacheable: true,
    })));

    let request = AuthorizeMultiIssuerRequest::new_with_fields(
        vec![
            TokenInput::new("Custom::ApiKey".to_string(), "key-one".to_string()),
            TokenInput::new("Custom::ApiKey".to_string(), "key-two".to_string()),
        ],
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
    );
    cedarling
        .authorize_multi_issuer(request)
        .await
        .expect("the first token should authorize the request");

    assert_eq!(
        calls.load(Ordering::SeqCst),
        1,
        "the duplicate (issuer, mapping) token must be skipped before the processor runs"
    );
}

/// A non-cacheable result re-runs `process` on every request.
#[tokio::test]
async fn custom_token_non_cacheable_result_reprocessed() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        |_| {},
    )
    .await;
    let calls = Arc::new(AtomicUsize::new(0));
    cedarling.set_custom_token_processor(Some(Arc::new(CountingProcessor {
        calls: calls.clone(),
        cacheable: false,
    })));

    for _ in 0..2 {
        cedarling
            .authorize_multi_issuer(read_doc_request("secret-admin-key"))
            .await
            .expect("non-cacheable custom token should authorize");
    }
    assert_eq!(
        calls.load(Ordering::SeqCst),
        2,
        "non-cacheable result must re-invoke the processor each request"
    );
}

/// Batch authorization: one validated custom-token set is evaluated against N
/// items. The `Doc` item matches the policy (Allow); a different resource id does
/// not (Deny) — both are `Ok`, never a batch-item error.
#[tokio::test]
async fn batch_custom_token_allow_and_deny_items() {
    let cedarling = get_cedarling_with_callback(
        PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
        |_| {},
    )
    .await;
    cedarling.set_custom_token_processor(Some(Arc::new(ApiKeyProcessor)));

    let request = BatchAuthorizeMultiIssuerRequest::new(
        vec![TokenInput::new(
            "Custom::ApiKey".to_string(),
            "secret-admin-key".to_string(),
        )],
        vec![read_item("Doc"), read_item("OtherDoc")],
    );

    let response = cedarling
        .authorize_multi_issuer_batch(request)
        .await
        .expect("batch authorization should succeed");

    assert_eq!(response.results.len(), 2);
    assert!(
        response.results[0]
            .as_ref()
            .expect("item 0 should be Ok")
            .decision,
        "Doc item should be ALLOW"
    );
    assert!(
        !response.results[1]
            .as_ref()
            .expect("item 1 should be Ok")
            .decision,
        "non-Doc item should be DENY (no matching policy), not a batch error"
    );
}

const SHADOW_STORE_RAW: &str = r#"
cedar_version: v4.0.0
policy_stores:
  shadow_store:
    cedar_version: v4.0.0
    name: ShadowStore
    description: custom mapping shadows a JWT token type
    trusted_issuers:
      AcmeIssuer:
        name: "Acme"
        description: ""
        openid_configuration_endpoint: "https://acme.example/.well-known/openid-configuration"
        token_metadata:
          access_token:
            entity_type_name: "Acme::Access_Token"
    custom_issuers:
      CustomKeys:
        tokens_mappings:
          "Acme::Access_Token": {}
    policies: {}
    default_entities: {}
"#;

#[tokio::test]
async fn custom_mapping_shadowing_jwt_type_fails_init() {
    let mut config = get_config(PolicyStoreSource::Yaml(SHADOW_STORE_RAW.to_string()));
    config.authorization_config.strict_schema_validation = false;

    let err = crate::Cedarling::new(&config)
        .await
        .err()
        .expect("a custom mapping equal to a JWT token entity type must fail init");
    // Fails at build_authz before the JWT service is built, naming the shadowed type.
    let display = err.to_string();
    let debug = format!("{err:?}");
    assert!(
        display.contains("Acme::Access_Token") || debug.contains("Acme::Access_Token"),
        "expected the shadowed type in the init error; display={display} debug={debug}"
    );
}
