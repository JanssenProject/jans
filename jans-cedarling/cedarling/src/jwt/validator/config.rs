use jsonwebtoken::Algorithm;

use super::IssuerId;
use crate::common::policy_store::TrustedIssuer;
use std::{
    collections::{HashMap, HashSet},
    rc::Rc,
};

/// Validation options related to JSON Web Tokens (JWT).
///
/// This struct provides the configuration for validating common JWT claims (`iss`,
/// `aud`, `sub`, `jti`, `exp`, `nbf`) across different types of JWTs.
///
/// The default configuration for Access Tokens, ID Tokens, and Userinfo Tokens
/// can be easily instantiated via the provided methods.
#[derive(Default)]
#[allow(dead_code)]
pub struct JwtValidatorConfig {
    /// Requires the `iss` claim to be present in the JWT, the scheme
    /// must be `https`, and the `iss` to be in the `trusted_issuers`.
    pub iss_validation: bool,
    /// Requires the `aud` claim to be present in the JWT.
    pub aud_validation: bool,
    /// Requires the `sub` claim to be present in the JWT.
    pub sub_validation: bool,
    /// Requires the `jti` claim to be present in the JWT.
    pub jti_validation: bool,
    /// Requires the `exp` claim to be present in the JWT and the current
    /// timestamp isn't past the specified timestamp in the token.
    pub exp_validation: bool,
    /// Requires the `nbf` claim to be present in the JWT.
    pub nbf_validation: bool,
    /// Validate the signature of the JWT.
    pub sig_validation: Rc<bool>,
    /// Validate the status of the JWT.
    ///
    /// The JWT status could be obatained from the `.well-known/openid-configuration` via
    /// the `status_list_endpoint`. See the [`IETF Draft`] for more info.
    ///
    /// [`IETF Draft`]: https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/
    pub status_validation: Rc<bool>,
    pub trusted_issuers: Rc<Option<HashMap<IssuerId, TrustedIssuer>>>,
    pub algs_supported: Rc<HashSet<Algorithm>>,
}

#[allow(dead_code)]
impl JwtValidatorConfig {
    /// Returns a default configuration for validating Access Tokens.
    ///
    /// This configuration requires and validates following claims:
    /// - `iss` (issuer)
    /// - `jti` (JWT ID)
    /// - `exp` (expiration)
    ///
    /// Claims like `aud` (audience) and `sub` (subject) are not required for
    /// Access Tokens.
    fn access_token(
        sig_validation: Rc<bool>,
        status_validation: Rc<bool>,
        trusted_issuers: Rc<Option<HashMap<IssuerId, TrustedIssuer>>>,
        algs_supported: Rc<HashSet<Algorithm>>,
    ) -> Self {
        Self {
            iss_validation: true,
            aud_validation: false,
            sub_validation: false,
            jti_validation: true,
            exp_validation: true,
            nbf_validation: false,
            sig_validation,
            status_validation,
            trusted_issuers,
            algs_supported,
        }
    }

    /// Returns a default configuration for validating ID Tokens.
    ///
    /// This configuration requires and validates following claims:
    /// - `iss` (issuer)
    /// - `aud` (audience)
    /// - `sub` (subject)
    /// - `exp` (expiration)
    ///
    /// `jti` (JWT ID) and `nbf` (not before) are not required for ID Tokens.
    fn id_token(
        sig_validation: Rc<bool>,
        status_validation: Rc<bool>,
        trusted_issuers: Rc<Option<HashMap<IssuerId, TrustedIssuer>>>,
        algs_supported: Rc<HashSet<Algorithm>>,
    ) -> Self {
        Self {
            iss_validation: true,
            aud_validation: true,
            sub_validation: true,
            jti_validation: false,
            exp_validation: true,
            nbf_validation: false,
            sig_validation,
            status_validation,
            trusted_issuers,
            algs_supported,
        }
    }

    /// Returns a default configuration for validating Userinfo Tokens.
    ///
    /// This configuration requires the following:
    /// - `iss` (issuer) validation
    /// - `aud` (audience) validation
    /// - `sub` (subject) validation
    /// - `exp` (expiration) validation
    ///
    /// `jti` (JWT ID) and `nbf` (not before) are not required for Userinfo Tokens.
    fn userinfo_token(
        sig_validation: Rc<bool>,
        status_validation: Rc<bool>,
        trusted_issuers: Rc<Option<HashMap<IssuerId, TrustedIssuer>>>,
        algs_supported: Rc<HashSet<Algorithm>>,
    ) -> Self {
        Self {
            iss_validation: true,
            aud_validation: true,
            sub_validation: true,
            jti_validation: false,
            exp_validation: true,
            nbf_validation: false,
            sig_validation,
            status_validation,
            trusted_issuers,
            algs_supported,
        }
    }

    /// Enables the validation of the `iss` claim (issuer).
    fn validate_iss(mut self) -> Self {
        self.iss_validation = true;
        self
    }

    /// Enables the validation of the `aud` claim (audience).
    fn validate_aud(mut self) -> Self {
        self.aud_validation = true;
        self
    }

    /// Enables the validation of the `sub` claim (subject).
    fn validate_sub(mut self) -> Self {
        self.sub_validation = true;
        self
    }

    /// Enables the validation of the `jti` claim (JWT ID).
    fn validate_jti(mut self) -> Self {
        self.jti_validation = true;
        self
    }

    /// Enables the validation of the `exp` claim (expiration).
    fn validate_exp(mut self) -> Self {
        self.exp_validation = true;
        self
    }

    /// Enables the validation of the `nbf` claim (not before).
    fn validate_nbf(mut self) -> Self {
        self.nbf_validation = true;
        self
    }
}
