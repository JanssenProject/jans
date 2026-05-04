// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Bridge from JSON session data to [`cedarling::blocking::Cedarling`] authorization APIs:
//! [`authorize_multi_issuer`](cedarling::blocking::Cedarling::authorize_multi_issuer) (JWT path) and
//! [`authorize_unsigned`](cedarling::blocking::Cedarling::authorize_unsigned) (pre-built entities, no JWTs).

use cedarling::blocking::Cedarling;
use cedarling::{AuthorizeMultiIssuerRequest, RequestUnsigned};
use serde_json::json;
use thiserror::Error;

use crate::resource;
use crate::token_bundle;

/// Errors building or running a multi-issuer authorization call.
#[derive(Debug, Error)]
pub enum AuthorizeBridgeError {
    #[error(transparent)]
    TokenBundle(#[from] token_bundle::TokenBundleError),
    #[error(transparent)]
    Resource(#[from] resource::ResourceEntityDataError),
    #[error("authorization request is invalid: {0}")]
    RequestInvalid(String),
    #[error(transparent)]
    Authorize(#[from] cedarling::AuthorizeError),
}

/// Errors building or running an `authorize_unsigned` call.
#[derive(Debug, Error)]
pub enum UnsignedBridgeError {
    #[error("invalid principal entity JSON: {0}")]
    Principal(resource::ResourceEntityDataError),
    #[error(transparent)]
    Resource(#[from] resource::ResourceEntityDataError),
    #[error(transparent)]
    ContextParse(#[from] serde_json::Error),
    #[error("Cedar request context must be a JSON object")]
    ContextNotObject,
    #[error(transparent)]
    Authorize(#[from] cedarling::AuthorizeError),
}

/// Parses `token_bundle_json` and `resource_json`, validates the request, and returns Cedar’s decision bit (`true` = allow).
///
/// The name uses **`decision`** (not “allow”) because it mirrors [`MultiIssuerAuthorizeResult::decision`](cedarling::MultiIssuerAuthorizeResult):
/// `false` means deny. “Allow” alone would read like a grant API rather than a boolean outcome.
pub fn authorize_multi_issuer_decision(
    engine: &Cedarling,
    token_bundle_json: &str,
    resource_json: &str,
    action: &str,
    context_json: Option<&str>,
) -> Result<bool, AuthorizeBridgeError> {
    let tokens = token_bundle::parse_token_inputs_from_json(token_bundle_json)?;

    let resource = resource::resource_entity_data_from_json_str(resource_json)?;
    let context =
        parse_optional_context_json_object(context_json).map_err(AuthorizeBridgeError::RequestInvalid)?;
    let request =
        AuthorizeMultiIssuerRequest::new_with_fields(tokens, resource, action.to_string(), context);
    request
        .validate()
        .map_err(|e| AuthorizeBridgeError::RequestInvalid(e.to_string()))?;
    let result = engine.authorize_multi_issuer(request)?;
    Ok(result.decision)
}

fn parse_optional_context_json_object(
    context_json: Option<&str>,
) -> Result<Option<serde_json::Value>, String> {
    let Some(raw) = context_json else {
        return Ok(None);
    };
    let trimmed = raw.trim();
    if trimmed.is_empty() {
        return Ok(None);
    }
    let value: serde_json::Value =
        serde_json::from_str(trimmed).map_err(|e| format!("invalid context JSON: {e}"))?;
    if value.is_object() {
        Ok(Some(value))
    } else {
        Err("context must be a JSON object".to_string())
    }
}

fn parse_optional_principal_json(
    principal_json: Option<&str>,
) -> Result<Option<cedarling::EntityData>, UnsignedBridgeError> {
    let Some(raw) = principal_json else {
        return Ok(None);
    };
    let trimmed = raw.trim();
    if trimmed.is_empty() {
        return Ok(None);
    }
    resource::resource_entity_data_from_json_str(trimmed)
        .map(Some)
        .map_err(UnsignedBridgeError::Principal)
}

fn parse_request_context_json(
    context_json: &str,
) -> Result<serde_json::Value, UnsignedBridgeError> {
    let trimmed = context_json.trim();
    let value = if trimmed.is_empty() {
        json!({})
    } else {
        serde_json::from_str(trimmed)?
    };
    if !value.is_object() {
        return Err(UnsignedBridgeError::ContextNotObject);
    }
    Ok(value)
}

/// Builds a [`RequestUnsigned`] from JSON parts (no Cedarling engine required).
///
/// `principal_json`: use `None` or `Some("")` for no principal (SQL `NULL` / blank).
/// `context_json`: must deserialize to a JSON **object** (use `"{}"` when unused), matching [`RequestUnsigned::context`](cedarling::RequestUnsigned).
pub fn unsigned_request_from_json_parts(
    principal_json: Option<&str>,
    resource_json: &str,
    action: &str,
    context_json: &str,
) -> Result<RequestUnsigned, UnsignedBridgeError> {
    let principal = parse_optional_principal_json(principal_json)?;
    let resource = resource::resource_entity_data_from_json_str(resource_json)?;
    let context = parse_request_context_json(context_json)?;
    Ok(RequestUnsigned {
        principal,
        resource,
        action: action.to_string(),
        context,
    })
}

/// Runs [`Cedarling::authorize_unsigned`](cedarling::blocking::Cedarling::authorize_unsigned) on a built request.
pub fn authorize_unsigned_decision_for_request(
    engine: &Cedarling,
    request: RequestUnsigned,
) -> Result<bool, UnsignedBridgeError> {
    let result = engine.authorize_unsigned(request)?;
    Ok(result.decision)
}

/// Parses JSON parts then returns Cedar’s decision bit (`true` = allow).
pub fn authorize_unsigned_decision(
    engine: &Cedarling,
    principal_json: Option<&str>,
    resource_json: &str,
    action: &str,
    context_json: &str,
) -> Result<bool, UnsignedBridgeError> {
    let request =
        unsigned_request_from_json_parts(principal_json, resource_json, action, context_json)?;
    authorize_unsigned_decision_for_request(engine, request)
}

#[cfg(test)]
mod smoke {
    use std::fs;
    use std::io::Write;

    use serde_json::json;
    use tempfile::tempdir;
    use test_utils::token_claims::generate_token_using_claims;

    use super::{authorize_multi_issuer_decision, authorize_unsigned_decision};
    use crate::engine::try_init_cedarling_from_bootstrap_path;

    use super::parse_optional_context_json_object;

    const POLICY_MULTI_ISSUER: &str = include_str!(concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/../test_files/policy-store-multi-issuer-basic.yaml"
    ));

    const POLICY_UNSIGNED: &str = include_str!(concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/../test_files/policy-store_no_trusted_issuers.yaml"
    ));

    fn write_bootstrap(dir: &std::path::Path, policy_path: &std::path::Path) -> std::path::PathBuf {
        let bootstrap_path = dir.join("bootstrap.yaml");
        let policy_lit = policy_path.to_string_lossy();
        let contents = format!(
            "CEDARLING_APPLICATION_NAME: cedarling_pg_test\n\
             CEDARLING_POLICY_STORE_URI: ''\n\
             CEDARLING_LOG_TYPE: memory\n\
             CEDARLING_LOG_LEVEL: DEBUG\n\
             CEDARLING_LOG_TTL: 60\n\
             CEDARLING_LOCAL_JWKS: null\n\
             CEDARLING_POLICY_STORE_LOCAL: null\n\
             CEDARLING_POLICY_STORE_LOCAL_FN: {policy_lit}\n\
             CEDARLING_JWT_SIG_VALIDATION: disabled\n\
             CEDARLING_JWT_STATUS_VALIDATION: disabled\n\
             CEDARLING_LOCK: disabled\n\
             CEDARLING_LOCK_SERVER_CONFIGURATION_URI: null\n\
             CEDARLING_LOCK_DYNAMIC_CONFIGURATION: disabled\n\
             CEDARLING_LOCK_HEALTH_INTERVAL: 0\n\
             CEDARLING_LOCK_TELEMETRY_INTERVAL: 0\n\
             CEDARLING_LOCK_LISTEN_SSE: disabled\n"
        );
        let mut f = fs::File::create(&bootstrap_path).expect("bootstrap file");
        f.write_all(contents.as_bytes()).expect("write bootstrap");
        bootstrap_path
    }

    #[test]
    fn authorize_multi_issuer_decision_smoke_dolphin_tokens() {
        let dir = tempdir().expect("tempdir");
        let policy_path = dir.path().join("policy-store.yaml");
        fs::write(&policy_path, POLICY_MULTI_ISSUER.as_bytes()).expect("write policy store");

        let bootstrap_path = write_bootstrap(dir.path(), &policy_path);
        let engine = try_init_cedarling_from_bootstrap_path(
            bootstrap_path
                .to_str()
                .expect("bootstrap path is valid UTF-8"),
        )
        .expect("Cedarling should bootstrap from temp files");

        let dolphin_access_token = generate_token_using_claims(json!({
            "iss": "https://idp.dolphin.sea",
            "sub": "dolphin_user_123",
            "jti": "dolphin123",
            "client_id": "dolphin_client_123",
            "aud": "dolphin_audience",
            "location": ["miami", "orlando"],
            "scope": ["read", "write"],
            "exp": 2_000_000_000,
            "iat": 1_516_239_022
        }));

        let dolphin_user_token = generate_token_using_claims(json!({
            "iss": "https://idp.dolphin.sea",
            "sub": "dolphin_user_123",
            "jti": "dolphin_user_123",
            "client_id": "dolphin_client_123",
            "aud": "dolphin_audience",
            "exp": 2_000_000_000,
            "iat": 1_516_239_022
        }));

        let token_bundle = json!([
            {"mapping": "Dolphin::Access_Token", "payload": dolphin_access_token},
            {"mapping": "Dolphin::Dolphin_Token", "payload": dolphin_user_token},
        ])
        .to_string();

        let resource = json!({
            "cedar_entity_mapping": {
                "entity_type": "Acme::Resource",
                "id": "1694c954f8a3"
            },
        })
        .to_string();

        let allowed = authorize_multi_issuer_decision(
            engine.as_ref(),
            &token_bundle,
            &resource,
            "Acme::Action::\"GetFood\"",
            None,
        )
        .expect("authorize_multi_issuer should run");

        assert!(
            allowed,
            "expected ALLOW for Dolphin access + dolphin token against default resource"
        );
    }

    #[test]
    fn authorize_unsigned_decision_smoke_principal_allow() {
        let dir = tempdir().expect("tempdir");
        let policy_path = dir.path().join("policy-store.yaml");
        fs::write(&policy_path, POLICY_UNSIGNED.as_bytes()).expect("write policy store");

        let bootstrap_path = write_bootstrap(dir.path(), &policy_path);
        let engine = try_init_cedarling_from_bootstrap_path(
            bootstrap_path
                .to_str()
                .expect("bootstrap path is valid UTF-8"),
        )
        .expect("Cedarling should bootstrap for unsigned tests");

        let principal = json!({
            "cedar_entity_mapping": {
                "entity_type": "Jans::TestPrincipal1",
                "id": "id1"
            },
            "is_ok": true
        })
        .to_string();

        let resource = json!({
            "cedar_entity_mapping": {
                "entity_type": "Jans::Issue",
                "id": "random_id"
            },
            "org_id": "some_long_id",
            "country": "US"
        })
        .to_string();

        let allowed = authorize_unsigned_decision(
            engine.as_ref(),
            Some(&principal),
            &resource,
            "Jans::Action::\"UpdateForTestPrincipals\"",
            "{}",
        )
        .expect("authorize_unsigned should run");

        assert!(allowed, "expected ALLOW for unsigned principal+resource");
    }

    #[test]
    fn parse_optional_context_accepts_object_and_rejects_array() {
        let parsed = parse_optional_context_json_object(Some(r#"{"tenant":"acme"}"#))
            .expect("object context should parse");
        assert!(parsed.is_some(), "object context should be preserved");

        let err = parse_optional_context_json_object(Some(r#"["x"]"#))
            .expect_err("array context should be rejected");
        assert!(
            err.contains("object"),
            "non-object context should include object requirement"
        );
    }
}
