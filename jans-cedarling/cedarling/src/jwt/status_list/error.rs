// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use jsonwebtoken::Algorithm;
use thiserror::Error;

use super::{JwtStatus, StatusListExpiry, UnavailableStatusList};
use crate::common::issuer_utils::IssClaim;
use crate::jwt::{decode::DecodeJwtError, fetch::HttpError, validation::ValidateJwtError};

#[derive(Debug, Error)]
pub enum ParseStatusListError {
    #[error("invalid `bit` size in the status list. expected 1, 2, 4, or 8 but got: {0}")]
    InvalidBitSize(u8),
    #[error("failed to decode status list: {0}")]
    Decode(#[from] base64::DecodeError),
    #[error("failed to read status list: {0}")]
    Read(#[from] std::io::Error),
    #[error("failed to deserialize status list JWT: {0}")]
    DeserializeJwt(#[from] reqwest::Error),
    #[error("the status list JWT is missing the 'lst' claim")]
    JwtMissingListClaim,
    #[error("the status list JWT is missing the 'bits' claim")]
    JwtMissingBitsClaim,
    #[error("the status list JWT's 'bits' claim is not a valid u64: {0:?}")]
    JwtInvalidBitsType(serde_json::Value),
    #[error("failed to convert bits value: {0}")]
    BitsConversion(#[from] std::num::TryFromIntError),
    #[error("decompressed status list exceeds the maximum allowed size of {0} bytes")]
    DecompressedSizeExceeded(u64),
}

/// Why [`StatusList::get_status`](super::StatusList::get_status) gave no status.
#[derive(Debug, PartialEq, Error)]
pub(super) enum GetStatusError {
    #[error("the index is outside the list, which holds {len} entries")]
    IdxOutOfBounds { len: usize },
    #[error("the list is cached with an unsupported bit size")]
    UnsupportedBitSize,
}

/// Why [`CachedStatusList::status_at`](super::CachedStatusList::status_at) gave
/// no status.
#[derive(Debug, PartialEq, Error)]
pub(super) enum StatusAtError {
    #[error("the list is past its {0}")]
    Expired(StatusListExpiry),
    #[error(transparent)]
    Get(#[from] GetStatusError),
}

/// Why a token with a `status` claim fails the status check. Each message names
/// the status list, so an admin can compare it with what the issuer serves.
#[derive(Debug, Error)]
pub enum StatusCheckError {
    #[error(
        "the token references the status list '{uri}', but its issuer publishes no \
         `status_list_endpoint`, so there is no list to check it against"
    )]
    NoStatusList { uri: String },
    #[error(
        "the token references the status list '{found}', but its issuer publishes its \
         status list at '{expected}'; Cedarling checks a token only against its own \
         issuer's list"
    )]
    ForeignStatusList { expected: String, found: String },
    #[error(
        "the status list '{uri}' has been unavailable since {}: {}; tokens that reference \
         it are rejected until a refresh succeeds",
        unavailable.since(),
        unavailable.cause()
    )]
    Unavailable {
        uri: String,
        unavailable: UnavailableStatusList,
    },
    #[error(
        "the status list '{uri}' is past its {expiry} and no refreshed list has replaced \
         it yet"
    )]
    Expired {
        uri: String,
        expiry: StatusListExpiry,
    },
    #[error(
        "the token's index {idx} is outside the status list '{uri}', which holds {len} \
         entries"
    )]
    IdxOutOfBounds { uri: String, idx: usize, len: usize },
    #[error("the status list '{uri}' is cached with an unsupported bit size")]
    UnsupportedBitSize { uri: String },
    #[error("entry {idx} of the status list '{uri}' is {status}")]
    Rejected {
        uri: String,
        idx: usize,
        status: JwtStatus,
    },
}

#[derive(Debug, Error)]
pub(crate) enum UpdateStatusListError {
    #[error("failed to decode the status list JWT: {0}")]
    DecodeStatusListJwt(#[from] DecodeJwtError),
    #[error(
        "the status list JWT from '{url}' is signed with {alg:?}, which is not listed in \
         CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED"
    )]
    UnsupportedAlgorithm { url: String, alg: Algorithm },
    #[error("failed to validate statuslist JWT: {0}")]
    ValidateJwtError(#[from] ValidateJwtError),
    #[error("failed to parse status list: {0}")]
    ParseStatusList(#[from] ParseStatusListError),
    #[error("failed to get status list JWT: {0}")]
    GetStatusListJwt(#[source] HttpError),
    #[error(
        "the status list JWT from '{url}' is not bound to that endpoint and its issuer: {reason}"
    )]
    UnboundStatusList { url: String, reason: UnboundReason },
    #[error("the status list JWT from '{url}' has `typ` {found:?}, expected `statuslist+jwt`")]
    UnexpectedTyp { url: String, found: Option<String> },
}

/// Which claim ties a status list JWT to another list or issuer.
#[derive(Debug, PartialEq, derive_more::Display)]
pub(crate) enum UnboundReason {
    #[display("its `sub` is '{found}'")]
    Sub { found: String },
    #[display("its `iss` is '{found}'")]
    Iss { found: IssClaim },
}
