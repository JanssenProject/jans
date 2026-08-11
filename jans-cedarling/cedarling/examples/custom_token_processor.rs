// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Authorize on a **custom (non-JWT) token** via a [`CustomTokenProcessor`].
//!
//! The policy store declares a `custom_issuers` entry (`CustomKeys`) mapping the
//! request `mapping` `Custom::ApiKey` to that issuer. A registered processor turns
//! the opaque payload into claims; those claims flow through the normal
//! entity-builder/context machinery, landing at `context.tokens.customkeys_apikey`
//! (`{sanitized_issuer_id}_{token_type}`), which the policy reads.
//!
//! Run: `cargo run --example custom_token_processor`

use async_trait::async_trait;
use cedarling::{
    AuthorizationConfig, AuthorizeMultiIssuerRequest, BootstrapConfig, Cedarling, CustomTokenError,
    CustomTokenProcessor, DataStoreConfig, EntityData, HttpClientConfig, JwtConfig, LogConfig,
    LogLevel, LogTypeConfig, PolicyStoreConfig, PolicyStoreSource, ProcessedTokenClaims,
    TokenInput, log_config::StdOutLoggerMode,
};
use serde_json::json;
use std::collections::{HashMap, HashSet};
use std::sync::Arc;

/// Policy store with a single custom issuer `CustomKeys` and a policy that allows
/// reads when the API key carries the `admin` scope. No JWT issuers, no schema
/// (strict schema validation is disabled below to keep the example minimal).
static POLICY_STORE_RAW: &str = r#"
cedar_version: v4.0.0
policy_stores:
  custom_token_store:
    cedar_version: v4.0.0
    name: CustomTokenExample
    description: Demonstrates authorizing on a custom (non-JWT) token
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
          // Non-reserved claims (sub, scope, ...) are attached as entity *tags*,
          // so the policy reads them via hasTag/getTag. Reserved claims
          // (token_type/jti/iss/exp/validated_at) are optional attributes here.
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

/// A trivial custom token processor. It treats the payload as an opaque API key:
/// `secret-admin-key` is a valid admin key, anything else is rejected.
struct ApiKeyProcessor;

#[cfg_attr(not(any(target_arch = "wasm32", target_arch = "wasm64")), async_trait)]
#[cfg_attr(any(target_arch = "wasm32", target_arch = "wasm64"), async_trait(?Send))]
impl CustomTokenProcessor for ApiKeyProcessor {
    async fn process(
        &self,
        mapping: &str,
        payload: &str,
    ) -> Result<ProcessedTokenClaims, CustomTokenError> {
        // `mapping` is the request's TokenInput mapping (e.g. "Custom::ApiKey");
        // dispatch on it when one processor serves several token formats.
        if payload != "secret-admin-key" {
            return Err(CustomTokenError::Processing(format!(
                "unknown API key for mapping '{mapping}'"
            )));
        }

        let mut claims = HashMap::new();
        claims.insert("sub".to_string(), json!("api-key-user"));
        claims.insert("scope".to_string(), json!("admin"));

        let mut processed = ProcessedTokenClaims::new(claims, "api-key-1");
        // Opt out of caching so a revoked key is re-checked on every request.
        processed.cacheable = false;
        Ok(processed)
    }
}

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cedarling = Cedarling::new(&BootstrapConfig {
        application_name: "custom_token_example".to_string(),
        log_config: LogConfig {
            log_type: LogTypeConfig::StdOut(StdOutLoggerMode::Immediate),
            log_level: LogLevel::INFO,
        },
        policy_store_config: PolicyStoreConfig {
            source: PolicyStoreSource::Yaml(POLICY_STORE_RAW.to_string()),
            ..Default::default()
        },
        jwt_config: JwtConfig {
            jwks: None,
            jwt_sig_validation: false,
            jwt_status_validation: false,
            signature_algorithms_supported: HashSet::new(),
            ..Default::default()
        }
        .allow_all_algorithms(),
        authorization_config: AuthorizationConfig {
            decision_log_default_jwt_id: "jti".to_string(),
            // The schema types `context.tokens.customkeys_apikey` as a
            // `Custom::ApiKey` entity so its claim tags are readable in the policy.
            strict_schema_validation: true,
            // Bound the processor call at 500ms; 0 disables the timeout.
            custom_token_processor_timeout_millis: 500,
        },
        lock_config: None,
        max_default_entities: None,
        max_base64_size: None,
        data_store_config: DataStoreConfig::default(),
        http_client_config: HttpClientConfig::default(),
    })
    .await?;

    // Register the processor on the live instance. It survives policy-store
    // refreshes; pass `None` to clear it.
    cedarling.set_custom_token_processor(Some(Arc::new(ApiKeyProcessor)));

    let resource = EntityData::from_json(
        &json!({
            "cedar_entity_mapping": { "entity_type": "Custom::Resource", "id": "Doc" },
            "name": "A protected document"
        })
        .to_string(),
    )?;

    // A valid admin key -> Allow.
    let allow = cedarling
        .authorize_multi_issuer(AuthorizeMultiIssuerRequest::new_with_fields(
            vec![TokenInput::new(
                "Custom::ApiKey".to_string(),
                "secret-admin-key".to_string(),
            )],
            resource.clone(),
            "Custom::Action::\"Read\"".to_string(),
            None,
        ))
        .await?;
    println!("valid admin key   -> decision: {}", allow.decision);

    // An unknown key: the issuer is `required`, so processing failure fails the
    // whole request rather than silently dropping the token.
    let rejected = cedarling
        .authorize_multi_issuer(AuthorizeMultiIssuerRequest::new_with_fields(
            vec![TokenInput::new(
                "Custom::ApiKey".to_string(),
                "not-a-real-key".to_string(),
            )],
            resource,
            "Custom::Action::\"Read\"".to_string(),
            None,
        ))
        .await;
    match rejected {
        Ok(res) => println!("unknown key       -> decision: {}", res.decision),
        Err(e) => println!("unknown key       -> rejected (required issuer): {e}"),
    }

    Ok(())
}
