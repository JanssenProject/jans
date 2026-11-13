// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The gRPC side of the HTTP client's retry rule, shared by every Lock gRPC client.

use reqwest::StatusCode;
use tonic::{Code, Status};

/// How tonic describes an HTTP answer without `grpc-status`, which comes from a
/// proxy before Lock rather than from Lock.
const NO_GRPC_STATUS: &str = "grpc-status header missing, mapped from HTTP status code ";

/// A failed gRPC call, sorted once by whether repeating it can help.
#[derive(Debug)]
pub(super) enum GrpcFailure {
    /// What the network or a failing Lock may answer differently next time.
    /// `INTERNAL` is Lock's `500` and `UNKNOWN` its answer for an unmapped HTTP status.
    Retryable(Status),
    Final(GrpcRefusal),
}

/// A gRPC call the same request would fail again.
#[derive(Debug, thiserror::Error)]
pub(super) enum GrpcRefusal {
    /// A proxy's `4xx` without `grpc-status`, final as over REST.
    #[error("a proxy before Lock refused the gRPC request with HTTP {0}")]
    Proxy(StatusCode),
    /// Lock sends no `OUT_OF_RANGE`: tonic gives it for an answer over the
    /// response cap, after Lock processed the request.
    #[error("Lock's gRPC answer is over the response cap: {}", .0.message())]
    ResponseTooLarge(Status),
    /// Lock refused the request.
    #[error("Lock refused the gRPC request with {:?}: {}", .0.code(), .0.message())]
    Lock(Status),
}

impl GrpcFailure {
    pub(super) fn sort(status: Status) -> Self {
        match status.code() {
            Code::Unavailable
            | Code::Internal
            | Code::Unknown
            | Code::DeadlineExceeded
            | Code::Cancelled
            | Code::Aborted => match proxy_refusal(&status) {
                Some(http) => Self::Final(GrpcRefusal::Proxy(http)),
                None => Self::Retryable(status),
            },
            Code::OutOfRange => Self::Final(GrpcRefusal::ResponseTooLarge(status)),
            _ => Self::Final(GrpcRefusal::Lock(status)),
        }
    }
}

/// tonic reads a proxy's `400` as `INTERNAL` and a `413` or `405` as `UNKNOWN`,
/// both retried; REST makes such a `4xx` final, and so does this. `408` and
/// `429` still ask to repeat the request.
fn proxy_refusal(status: &Status) -> Option<StatusCode> {
    let code = status
        .message()
        .strip_prefix(NO_GRPC_STATUS)?
        .parse()
        .ok()?;
    let http = StatusCode::from_u16(code).ok()?;
    let repeat = http == StatusCode::REQUEST_TIMEOUT || http == StatusCode::TOO_MANY_REQUESTS;
    (http.is_client_error() && !repeat).then_some(http)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn proxy(code: Code, http: u16) -> Status {
        Status::new(code, format!("{NO_GRPC_STATUS}{http}"))
    }

    #[test]
    fn sorts_by_whether_repeating_can_help() {
        let cases = [
            (Status::unavailable("down"), "Retryable"),
            (Status::internal("lock 500"), "Retryable"),
            (proxy(Code::Unknown, 429), "Retryable"),
            (proxy(Code::Internal, 503), "Retryable"),
            (proxy(Code::Internal, 400), "ProxyRefusal(400)"),
            (proxy(Code::Unknown, 413), "ProxyRefusal(413)"),
            (Status::out_of_range("over the cap"), "ResponseTooLarge"),
            (Status::invalid_argument("bad"), "Refused"),
            (Status::permission_denied("scope"), "Refused"),
            (Status::unauthenticated("token"), "Refused"),
        ];
        for (status, expected) in cases {
            let described = format!("{:?} {}", status.code(), status.message());
            let got = match GrpcFailure::sort(status) {
                GrpcFailure::Retryable(_) => "Retryable".to_string(),
                GrpcFailure::Final(GrpcRefusal::Proxy(http)) => {
                    format!("ProxyRefusal({})", http.as_u16())
                },
                GrpcFailure::Final(GrpcRefusal::ResponseTooLarge(_)) => {
                    "ResponseTooLarge".to_string()
                },
                GrpcFailure::Final(GrpcRefusal::Lock(_)) => "Refused".to_string(),
            };
            assert_eq!(got, expected, "{described} sorted wrongly");
        }
    }
}
