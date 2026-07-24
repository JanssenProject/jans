// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedarling::{self as core, bindings::cedar_policy};

#[derive(Debug, uniffi::Record)]
pub struct AuthorizeResult {
    pub response: Response,
    pub decision: bool,
    pub request_id: String,
}

#[derive(Debug, uniffi::Record)]
pub struct Response {
    pub decision: Decision,
    pub diagnostics: Diagnostics,
}

#[derive(Debug, uniffi::Enum)]
pub enum Decision {
    Allow,
    Deny,
}

#[derive(Debug, uniffi::Record)]
pub struct Diagnostics {
    pub reasons: Vec<String>,
    pub errors: Vec<String>,
}

// Conversion implementations
impl From<cedar_policy::Decision> for Decision {
    fn from(decision: cedar_policy::Decision) -> Self {
        match decision {
            cedar_policy::Decision::Allow => Decision::Allow,
            cedar_policy::Decision::Deny => Decision::Deny,
        }
    }
}

impl From<&cedar_policy::Diagnostics> for Diagnostics {
    fn from(diag: &cedar_policy::Diagnostics) -> Self {
        Diagnostics {
            reasons: diag.reason().map(|id| id.to_string()).collect(),
            errors: diag.errors().map(|e| e.to_string()).collect(),
        }
    }
}

impl From<cedar_policy::Response> for Response {
    fn from(response: cedar_policy::Response) -> Self {
        Response {
            decision: response.decision().into(),
            diagnostics: response.diagnostics().into(),
        }
    }
}

impl From<core::AuthorizeResult> for AuthorizeResult {
    fn from(result: core::AuthorizeResult) -> Self {
        AuthorizeResult {
            response: result.response.into(),
            decision: result.decision,
            request_id: result.request_id,
        }
    }
}

/// Result of multi-issuer authorization
#[derive(Debug, uniffi::Record)]
pub struct MultiIssuerAuthorizeResult {
    pub response: Response,
    pub decision: bool,
    pub request_id: String,
}

impl From<core::MultiIssuerAuthorizeResult> for MultiIssuerAuthorizeResult {
    fn from(result: core::MultiIssuerAuthorizeResult) -> Self {
        MultiIssuerAuthorizeResult {
            response: result.response.into(),
            decision: result.decision,
            request_id: result.request_id,
        }
    }
}

/// Per-item build failure surfaced inside a batch response when Cedar
/// couldn't be reached for that item. `category` is the stable variant slug
/// (`action_parse`, `resource_build`, …), `item_index` is the position of
/// the failing item in the original `items` list.
#[derive(Debug, uniffi::Record)]
pub struct BatchItemError {
    pub category: String,
    // i64 (not u64) so the Kotlin/Java binding exposes it as a signed Long
    // rather than a JVM-hostile ULong. usize casts fit trivially.
    pub item_index: i64,
    pub message: String,
}

impl From<core::BatchItemError> for BatchItemError {
    fn from(e: core::BatchItemError) -> Self {
        let message = e.to_string();
        Self {
            category: e.category().to_string(),
            item_index: e.item_index() as i64,
            message,
        }
    }
}

/// One slot in `BatchAuthorizeUnsignedResponse.results`. Pattern-match on
/// `Success(_)` to read the Cedar decision, or `Failed(_)` to read the
/// per-item build failure.
#[derive(Debug, uniffi::Enum)]
pub enum BatchItemUnsignedOutcome {
    Success { result: AuthorizeResult },
    Failed { error: BatchItemError },
}

impl From<Result<core::AuthorizeResult, core::BatchItemError>> for BatchItemUnsignedOutcome {
    fn from(r: Result<core::AuthorizeResult, core::BatchItemError>) -> Self {
        match r {
            Ok(ok) => Self::Success { result: ok.into() },
            Err(err) => Self::Failed { error: err.into() },
        }
    }
}

/// Multi-issuer analog of [`BatchItemUnsignedOutcome`].
#[derive(Debug, uniffi::Enum)]
pub enum BatchItemMultiIssuerOutcome {
    Success { result: MultiIssuerAuthorizeResult },
    Failed { error: BatchItemError },
}

impl From<Result<core::MultiIssuerAuthorizeResult, core::BatchItemError>>
    for BatchItemMultiIssuerOutcome
{
    fn from(r: Result<core::MultiIssuerAuthorizeResult, core::BatchItemError>) -> Self {
        match r {
            Ok(ok) => Self::Success { result: ok.into() },
            Err(err) => Self::Failed { error: err.into() },
        }
    }
}

/// Result of `Cedarling.authorize_unsigned_batch`. Carries a shared `batch_id`
/// (UUIDv7) alongside per-item outcomes. `results[i]` corresponds to the
/// `items[i]` supplied in the request.
#[derive(Debug, uniffi::Record)]
pub struct BatchAuthorizeUnsignedResponse {
    pub batch_id: String,
    pub results: Vec<BatchItemUnsignedOutcome>,
}

impl From<core::BatchAuthorizeResponse<Result<core::AuthorizeResult, core::BatchItemError>>>
    for BatchAuthorizeUnsignedResponse
{
    fn from(
        value: core::BatchAuthorizeResponse<
            Result<core::AuthorizeResult, core::BatchItemError>,
        >,
    ) -> Self {
        Self {
            batch_id: value.batch_id.to_string(),
            results: value.results.into_iter().map(Into::into).collect(),
        }
    }
}

/// Result of `Cedarling.authorize_multi_issuer_batch`. Same shape as
/// [`BatchAuthorizeUnsignedResponse`] with multi-issuer results.
#[derive(Debug, uniffi::Record)]
pub struct BatchAuthorizeMultiIssuerResponse {
    pub batch_id: String,
    pub results: Vec<BatchItemMultiIssuerOutcome>,
}

impl
    From<
        core::BatchAuthorizeResponse<
            Result<core::MultiIssuerAuthorizeResult, core::BatchItemError>,
        >,
    > for BatchAuthorizeMultiIssuerResponse
{
    fn from(
        value: core::BatchAuthorizeResponse<
            Result<core::MultiIssuerAuthorizeResult, core::BatchItemError>,
        >,
    ) -> Self {
        Self {
            batch_id: value.batch_id.to_string(),
            results: value.results.into_iter().map(Into::into).collect(),
        }
    }
}
