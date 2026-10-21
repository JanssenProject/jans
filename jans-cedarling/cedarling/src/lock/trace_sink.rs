// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Where TRACE records go and the token they go with. TRACE always uses REST:
//! Lock has no TRACE endpoint over gRPC.

use std::fmt;

use tokio::sync::RwLock;

use super::register_client::TokenSource;
use super::{InitLockServiceError, LockAccessToken};
use crate::TraceEndpoint;
use crate::http::HttpClient;

/// The Lock TRACE bulk endpoint and the client that posts to it. Holds its own
/// copy of the token, so a `401` can swap in a reissued one.
pub(crate) struct TraceSink {
    pub(super) endpoint: url::Url,
    #[expect(
        dead_code,
        reason = "used to post bulk requests; remove with that code"
    )]
    pub(super) http: HttpClient,
    #[expect(
        dead_code,
        reason = "used to post bulk requests; remove with that code"
    )]
    pub(super) token: RwLock<LockAccessToken>,
    /// `None` when the token was given as is and cannot be reissued.
    pub(super) source: Option<TokenSource>,
}

impl fmt::Debug for TraceSink {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("TraceSink")
            .field("endpoint", &self.endpoint.as_str())
            .field("source", &self.source)
            .finish_non_exhaustive()
    }
}

/// The explicit endpoint wins over discovery. `Ok(None)` when Lock publishes
/// none. Only `http` and `https` carry the bulk request, so any other scheme
/// stops startup instead of failing every record later.
pub(super) fn resolve_trace_endpoint(
    endpoint: &TraceEndpoint,
    discovered: Option<&url::Url>,
) -> Result<Option<url::Url>, InitLockServiceError> {
    let url = match endpoint {
        TraceEndpoint::Url(url) => url,
        TraceEndpoint::Discovered => return Ok(discovered.cloned()),
    };
    match url.scheme() {
        "http" | "https" => Ok(Some(url.clone())),
        scheme => Err(InitLockServiceError::InvalidTraceEndpointScheme(
            scheme.to_string(),
        )),
    }
}
