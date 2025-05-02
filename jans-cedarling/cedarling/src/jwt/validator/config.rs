// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use super::IssuerId;
use crate::common::policy_store::TrustedIssuer;
use jsonwebtoken::Algorithm;
use std::collections::{HashMap, HashSet};
use std::sync::Arc;

/// Validation options related to JSON Web Tokens (JWT).
///
/// This struct provides the configuration for validating common JWT claims (`iss`,
/// `aud`, `sub`, `jti`, `exp`, `nbf`) across different types of JWTs.
///
/// The default configuration for Access Tokens, ID Tokens, and Userinfo Tokens
/// can be easily instantiated via the provided methods.
#[derive(Default)]
pub struct JwtValidatorConfig {
    /// Validate the signature of the JWT.
    pub sig_validation: Arc<bool>,
    /// Validate the status of the JWT.
    ///
    /// The JWT status could be obatained from the `.well-known/openid-configuration` via
    /// the `status_list_endpoint`. See the [`IETF Draft`] for more info.
    ///
    /// [`IETF Draft`]: https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/
    // TODO: implement token status validation
    #[allow(dead_code)]
    pub status_validation: Arc<bool>,
    /// List of trusted issuers used to check the JWT status.
    pub trusted_issuers: Arc<Option<HashMap<IssuerId, TrustedIssuer>>>,
    /// Algorithms supported as defined in the Bootstrap properties.
    ///
    /// Tokens not signed with an algorithm within this HashSet will immediately be invalid.
    pub algs_supported: Arc<HashSet<Algorithm>>,
    /// Required claims that the JWTs are required to have.
    //
    /// Tokens with a missing required claim will immediately be invalid.
    pub required_claims: HashSet<Box<str>>,
    /// Validate the `exp` (Expiration) claim of the token if it's present.
    pub validate_exp: bool,
    /// Validate the `nbf` (Not Before) claim of the token if it's present.
    pub validate_nbf: bool,
}
