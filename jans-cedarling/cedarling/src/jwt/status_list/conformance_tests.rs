// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Checks Cedarling against the Relying Party rules of [draft-ietf-oauth-status-list-21].
//!
//! Each test serves a status list JWT from a mock issuer, loads it through
//! [`StatusListRegistry::register`] (fetch, signature and claim validation), then
//! validates a Referenced Token against it with
//! [`JwtValidator::validate_jwt`](crate::jwt::validation::JwtValidator::validate_jwt).
//!
//! [draft-ietf-oauth-status-list-21]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html

use std::borrow::Cow;
use std::collections::{HashMap, HashSet};
use std::sync::Arc;
use std::time::Duration;

use jsonwebtoken::Algorithm;
use jsonwebtoken::errors::ErrorKind;
use serde_json::json;
use tokio_util::sync::{CancellationToken, DropGuard};

use super::{
    InitForIssArgs, JwtStatus, StatusBitSize, StatusCheckError, StatusListRegistry,
    UpdateStatusListError,
};
use crate::JwtConfig;
use crate::common::policy_store::token_types::EntityTypeName;
use crate::common::policy_store::{TokenEntityMetadata, TrustedIssuer};
use crate::http::{HttpClient, HttpClientConfig};
use crate::jwt::IssuerConfig;
use crate::jwt::TokenCache;
use crate::jwt::key_service::KeyService;
use crate::jwt::test_utils::{MockServer, MockStatusList, TokenTypeHeader};
use crate::jwt::validation::{
    JwtValidatorCache, TokenKind, ValidateJwtError, ValidatedJwt, ValidatorInfo,
};

/// Far enough that no scheduled refresh runs while a test is checking the first list.
const NO_REFRESH: Duration = Duration::from_secs(60);

/// The clock-skew leeway the status list validator applies to `exp`
/// (the `jsonwebtoken` default, which Cedarling does not override).
fn leeway_secs() -> i64 {
    i64::try_from(jsonwebtoken::Validation::default().leeway).expect("leeway fits in i64")
}

/// `exp` offset for a list that is still accepted at fetch but stops being
/// valid, leeway included, about two seconds later.
fn exp_offset_expiring_soon() -> i64 {
    -(leeway_secs() - 2)
}

fn bits(value: u8) -> StatusBitSize {
    StatusBitSize::try_from(value).expect("test uses a valid status bit size")
}

/// The entity type the mock issuer's Referenced Tokens are mapped to.
const REFERENCED_TOKEN_TYPE: &str = "Jans::Access_Token";

/// How long a test waits for the background refresh before it fails.
const WAIT_LIMIT: Duration = Duration::from_secs(10);

/// Polls `check` every 50 ms until it returns `Some`, or gives up after
/// [`WAIT_LIMIT`]. Tests wait for the background refresh this way, so a slow
/// runner makes them slower instead of failing them.
async fn wait_for<T>(mut check: impl FnMut() -> Option<T>) -> Option<T> {
    let deadline = tokio::time::Instant::now() + WAIT_LIMIT;
    loop {
        if let Some(found) = check() {
            return Some(found);
        }
        if tokio::time::Instant::now() >= deadline {
            return None;
        }
        tokio::time::sleep(Duration::from_millis(50)).await;
    }
}

/// The cause Cedarling recorded for a status list it refused or could not load.
fn unavailable_cause(err: &ValidateJwtError) -> Option<&UpdateStatusListError> {
    match err {
        ValidateJwtError::Status(StatusCheckError::Unavailable { unavailable, .. }) => {
            Some(unavailable.cause())
        },
        _ => None,
    }
}

/// A mock issuer whose status list Cedarling has loaded, with the validators it
/// built for the issuer. Dropping it stops the background refresh task.
struct Case {
    mock: MockServer,
    validators: Arc<JwtValidatorCache>,
    _stop_refresh: DropGuard,
}

impl Case {
    /// Serves `list` and loads it the way Cedarling does at startup, refreshing no
    /// later than `refresh_interval_max`. A list that is refused stays in the
    /// cache as unavailable, with the cause.
    async fn load(list: MockStatusList<'_>, refresh_interval_max: Duration) -> Self {
        let mut mock = MockServer::new_with_defaults()
            .await
            .expect("mock server should start");
        mock.serve_status_list(list);

        let http_client = HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
            max_response_size_bytes: None,
        })
        .expect("http client should be constructed");
        let key_service = KeyService::default();
        key_service
            .fetch_keys(
                &mock.openid_config().issuer,
                &mock.openid_config().jwks_uri,
                None,
                http_client.clone(),
            )
            .await
            .expect("mock JWKS should load");

        let trusted_issuer = TrustedIssuer::from_configuration(
            "some_iss",
            mock.openid_config_endpoint()
                .expect("mock serves an OpenID configuration"),
        )
        .expect("mock configuration endpoint should derive an iss");
        let iss_config = IssuerConfig {
            policy: Arc::new(trusted_issuer),
            tokens: HashMap::from([(
                EntityTypeName::for_test(REFERENCED_TOKEN_TYPE),
                TokenEntityMetadata {
                    token_id: "jti".into(),
                    required_claims: HashSet::new(),
                },
            )]),
            openid_config: Some(mock.openid_config()),
        };

        let jwt_config = JwtConfig {
            jwks: None,
            jwt_sig_validation: true,
            jwt_status_validation: true,
            signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
            ..Default::default()
        };
        let validators = Arc::new(JwtValidatorCache::default());
        validators.init_status_list_validators(&iss_config, &jwt_config);

        let cancel_tkn = CancellationToken::new();
        let status_list = StatusListRegistry::default()
            .register(
                &iss_config,
                InitForIssArgs {
                    validators: validators.clone(),
                    key_service: Arc::new(key_service),
                    token_cache: TokenCache::default(),
                    logger: None,
                    http_client,
                    refresh_interval_max,
                    cancel_tkn: cancel_tkn.clone(),
                },
            )
            .await;
        validators.init_token_validators(&iss_config, &jwt_config, status_list.as_ref(), None);

        Self {
            mock,
            validators,
            _stop_refresh: cancel_tkn.drop_guard(),
        }
    }

    /// The URI the mock serves its status list at.
    fn list_uri(&self) -> String {
        self.mock
            .status_list_endpoint()
            .expect("the mock serves a status list")
            .to_string()
    }

    /// Validates a Referenced Token whose `status` points at index `idx` of the
    /// list the mock serves.
    fn validate(&mut self, idx: usize) -> Result<ValidatedJwt, ValidateJwtError> {
        let mut claims = json!({
            "sub": "some_user",
            "exp": chrono::Utc::now().timestamp() + 3600,
        });
        let token = self
            .mock
            .generate_token_with_hs256sig(&mut claims, Some(idx))
            .expect("referenced token should be signed");
        self.validate_token(&token)
    }

    /// Validates `token` with the validator Cedarling built for the mock issuer.
    fn validate_token(&self, token: &str) -> Result<ValidatedJwt, ValidateJwtError> {
        let iss = self.mock.issuer();
        let validator = self
            .validators
            .get(&ValidatorInfo {
                iss: Some(&iss),
                token_kind: TokenKind::AuthorizeMultiIssuer(Cow::Borrowed(REFERENCED_TOKEN_TYPE)),
                algorithm: Algorithm::HS256,
            })
            .expect("a validator is built for the issuer's Referenced Tokens");
        validator.validate_jwt(
            token,
            Some(self.mock.jwt_decoding_key().expect("mock decoding key")),
        )
    }

    /// Waits until validating index `idx` gives an error that `expected` accepts.
    async fn wait_for_rejection(
        &mut self,
        idx: usize,
        expected: impl Fn(&ValidateJwtError) -> bool,
    ) -> Option<ValidateJwtError> {
        wait_for(|| self.validate(idx).err().filter(|err| expected(err))).await
    }
}

/// Whether `err` rejects index `idx` of the list at `uri` with `status`.
fn is_rejected(err: &ValidateJwtError, uri: &str, idx: usize, status: &JwtStatus) -> bool {
    matches!(
        err,
        ValidateJwtError::Status(StatusCheckError::Rejected {
            uri: found_uri,
            idx: found_idx,
            status: found_status,
        }) if found_uri == uri && *found_idx == idx && found_status == status
    )
}

/// Whether `err` says the list at `uri` is unavailable because the issuer's
/// endpoint failed.
fn is_unavailable_after_failed_fetch(err: &ValidateJwtError, uri: &str) -> bool {
    matches!(
        err,
        ValidateJwtError::Status(StatusCheckError::Unavailable { uri: found_uri, unavailable })
            if found_uri == uri
                && matches!(unavailable.cause(), UpdateStatusListError::GetStatusListJwt(_))
    )
}

/// Loads `list`, validates a token at `idx` and checks it is rejected with `status`.
async fn assert_rejected_with(list: MockStatusList<'_>, idx: usize, status: JwtStatus) {
    let mut case = Case::load(list, NO_REFRESH).await;
    let err = case
        .validate(idx)
        .expect_err("a status other than VALID rejects the token");
    assert!(
        is_rejected(&err, &case.list_uri(), idx, &status),
        "the rejection must name the list, index {idx} and status {status}, got: {err:?}"
    );
}

/// Status `0x00` is VALID ([§7.1]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
#[tokio::test]
async fn accepts_valid_status() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0b0000_0010],
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;
    case.validate(0)
        .expect("index 0 holds 0x00 (VALID), so the token is accepted");
}

/// Status `0x01` is INVALID: the token is revoked ([§7.1]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
#[tokio::test]
async fn rejects_invalid_status() {
    assert_rejected_with(
        MockStatusList {
            list: &[0b0000_0010],
            ..Default::default()
        },
        1,
        JwtStatus::Invalid,
    )
    .await;
}

/// Status `0x02` is SUSPENDED: the token is temporarily invalid ([§7.1]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
#[tokio::test]
async fn rejects_suspended_status() {
    assert_rejected_with(
        MockStatusList {
            bits: bits(2),
            list: &[0b0000_1000],
            ..Default::default()
        },
        1,
        JwtStatus::Suspended,
    )
    .await;
}

/// `0x03` is application specific ([§7.1]). Cedarling defines no meaning for it,
/// so no statement about the token can be made and it is rejected ([§8.3] step 7
/// and the closing paragraph).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_application_specific_status_0x03() {
    assert_rejected_with(
        MockStatusList {
            bits: bits(2),
            list: &[0b0000_0011],
            ..Default::default()
        },
        0,
        JwtStatus::ApplicationSpecific(0x03),
    )
    .await;
}

/// `0x0C` to `0x0F` are application specific ([§7.1]); rejected as in
/// [`rejects_application_specific_status_0x03`] ([§8.3]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_application_specific_status_0x0c() {
    assert_rejected_with(
        MockStatusList {
            bits: bits(4),
            list: &[0x0C],
            ..Default::default()
        },
        0,
        JwtStatus::ApplicationSpecific(0x0C),
    )
    .await;
}

/// `0x04` to `0x0B` are reserved for future registration ([§7.1]); an unknown
/// status gives no statement about the token, so it is rejected ([§8.3]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_reserved_status_0x05() {
    assert_rejected_with(
        MockStatusList {
            bits: bits(4),
            list: &[0x05],
            ..Default::default()
        },
        0,
        JwtStatus::Reserved(0x05),
    )
    .await;
}

/// `0x10` to `0xFF` are reserved for future registration ([§7.1]); rejected as
/// in [`rejects_reserved_status_0x05`] ([§8.3]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_reserved_status_0xff() {
    assert_rejected_with(
        MockStatusList {
            bits: bits(8),
            list: &[0xFF],
            ..Default::default()
        },
        0,
        JwtStatus::Reserved(0xFF),
    )
    .await;
}

/// An index past the end of the list gives no statement about the token, which
/// MUST be rejected ([§8.3] step 6).
///
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_index_out_of_bounds() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0],
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;
    let err = case
        .validate(8)
        .expect_err("a one-byte, 1-bit list has no index 8, so the token is rejected");
    let list_uri = case.list_uri();
    assert!(
        matches!(
            &err,
            ValidateJwtError::Status(StatusCheckError::IdxOutOfBounds { uri, idx: 8, len: 8 })
                if *uri == list_uri
        ),
        "the rejection must name the list, the index and the list size, got: {err:?}"
    );
}

/// The status list JWT `typ` MUST be `statuslist+jwt` ([§5.1]); a token that is
/// not valid per [§5.1] is not used ([§8.3] step 4).
///
/// [§5.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-5.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_list_with_wrong_typ() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0],
            typ: Some(TokenTypeHeader::Jwt),
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;
    let err = case.validate(0).expect_err(
        "a list served with `typ: JWT` is refused, so the token that references it is rejected",
    );
    assert!(
        matches!(
            unavailable_cause(&err),
            Some(UpdateStatusListError::UnexpectedTyp { found, .. })
                if found.as_deref() == Some("JWT")
        ),
        "a status list JWT with the wrong `typ` must be refused, got: {err:?}"
    );
}

/// `typ` is REQUIRED ([§5.1]), so a status list JWT without it is not used either.
///
/// [§5.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-5.1
#[tokio::test]
async fn rejects_list_without_typ() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0],
            typ: None,
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;
    let err = case.validate(0).expect_err(
        "a list served without `typ` is refused, so the token that references it is rejected",
    );
    assert!(
        matches!(
            unavailable_cause(&err),
            Some(UpdateStatusListError::UnexpectedTyp { found: None, .. })
        ),
        "a status list JWT without `typ` must be refused, got: {err:?}"
    );
}

/// `iat` is REQUIRED in a status list JWT ([§5.1], [§8.3] step 3).
///
/// [§5.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-5.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_list_without_iat() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0],
            with_iat: false,
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;
    let err = case
        .validate(0)
        .expect_err("a list without `iat` is refused, so the token that references it is rejected");
    assert!(
        matches!(
            unavailable_cause(&err),
            Some(UpdateStatusListError::ValidateJwtError(
                ValidateJwtError::MissingClaims(claims)
            )) if claims == &["iat"]
        ),
        "a status list JWT without `iat` must be refused, got: {err:?}"
    );
}

/// `exp` is RECOMMENDED, not required ([§5.1]); a list without it stays usable
/// until it is refreshed.
///
/// [§5.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-5.1
#[tokio::test]
async fn accepts_list_without_exp() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0],
            exp_offset_secs: None,
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;
    case.validate(0)
        .expect("a list without `exp` has no expiry, so a VALID entry is accepted");
}

/// An expired status list MUST NOT be used ([§8.3] step 4, [§13.7]).
///
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
/// [§13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
#[tokio::test]
async fn rejects_list_expired_at_fetch() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0],
            exp_offset_secs: Some(-(leeway_secs() + 60)),
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;
    let err = case.validate(0).expect_err(
        "a list whose `exp` passed beyond the leeway is refused, so the token that \
         references it is rejected",
    );
    assert!(
        matches!(
            unavailable_cause(&err),
            Some(UpdateStatusListError::ValidateJwtError(
                ValidateJwtError::ValidateJwt(e)
            )) if *e.kind() == ErrorKind::ExpiredSignature
        ),
        "an expired status list JWT must be refused, got: {err:?}"
    );
}

/// The draft asks the Relying Party to "account a minimal offset" for the
/// issuer's signing and distribution ([§13.7]); Cedarling allows the validator's
/// clock-skew leeway, so a list whose `exp` passed within it is still used.
///
/// [§13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
#[tokio::test]
async fn accepts_list_expired_within_leeway() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0],
            exp_offset_secs: Some(-10),
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;
    case.validate(0)
        .expect("`exp` 10 s ago is inside the leeway, so a VALID entry is accepted");
}

/// Once `exp` passes, the cached list "MUST NOT be relied upon any longer"
/// ([§13.7]); with no fresh copy, no statement about the token can be made and
/// it is rejected ([§8.3] step 4 and the closing paragraph). Cedarling drops the
/// expired list as it does after a failed refresh.
///
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
/// [§13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
#[tokio::test(flavor = "multi_thread")]
async fn rejects_token_once_cached_list_expires() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0],
            exp_offset_secs: Some(exp_offset_expiring_soon()),
            ttl_secs: None,
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;
    case.validate(0)
        .expect("the cached list has not expired yet, so a VALID entry is accepted");

    case.mock.fail_status_list_endpoint(1);
    let list_uri = case.list_uri();
    let err = case
        .wait_for_rejection(0, |err| is_unavailable_after_failed_fetch(err, &list_uri))
        .await;
    assert!(
        err.is_some(),
        "the cached list expired and could not be refreshed, so it must be dropped (fail \
         closed) and name the failed fetch"
    );
}

/// Without `ttl`, the Relying Party SHOULD check for updates no later than `exp`
/// ([§13.7]), even when the configured refresh interval is longer.
///
/// [§13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
#[tokio::test(flavor = "multi_thread")]
async fn refreshes_list_by_exp_when_ttl_is_absent() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0],
            exp_offset_secs: Some(exp_offset_expiring_soon()),
            ttl_secs: None,
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;

    // the issuer revokes index 0 in its next list
    case.mock.serve_status_list(MockStatusList {
        list: &[0b0000_0001],
        ttl_secs: None,
        ..Default::default()
    });
    let list_uri = case.list_uri();
    let err = case
        .wait_for_rejection(0, |err| is_rejected(err, &list_uri, 0, &JwtStatus::Invalid))
        .await;
    assert!(
        err.is_some(),
        "the list must be refreshed at its `exp`, well before the 60 s refresh interval, \
         so the revocation is seen"
    );
}

/// A failed refresh does not end a list before its `exp`: until then it may be
/// relied upon ([§13.7]), and `ttl` only says when a newer list may be available.
/// Cedarling keeps using it and retries.
///
/// [§13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
#[tokio::test(flavor = "multi_thread")]
async fn keeps_unexpired_list_when_refresh_fails() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0],
            ttl_secs: Some(1),
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;

    // Two failed attempts: the loop handles one before it starts the next.
    case.mock.fail_status_list_endpoint(2);
    let refreshed = wait_for(|| case.mock.status_list_endpoint_matched().then_some(())).await;
    assert!(
        refreshed.is_some(),
        "the list must be refreshed at its `ttl`, and both attempts must fail"
    );

    case.validate(0).expect(
        "the refresh at `ttl` failed but the list has not reached its `exp`, so a VALID \
         entry is still accepted",
    );
}

/// A list without `exp` has no point up to which it may be relied upon, so a
/// failed refresh drops it and the tokens that reference it are rejected
/// ([§8.3] closing paragraph).
///
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test(flavor = "multi_thread")]
async fn drops_list_without_exp_when_refresh_fails() {
    let mut case = Case::load(
        MockStatusList {
            list: &[0],
            exp_offset_secs: None,
            ttl_secs: Some(1),
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;

    case.mock.fail_status_list_endpoint(1);
    let list_uri = case.list_uri();
    let err = case
        .wait_for_rejection(0, |err| is_unavailable_after_failed_fetch(err, &list_uri))
        .await;
    assert!(
        err.is_some(),
        "the list has no `exp` and could not be refreshed, so it must be dropped (fail \
         closed) and name the failed fetch"
    );
}

/// Cedarling checks a token only against its own issuer's status list. The draft
/// allows a Status Issuer other than the token's issuer ([§1]), but Cedarling
/// cannot tell whether the issuer authorized it, so a token that names another
/// list is rejected, naming both lists ([§8.3] closing paragraph).
///
/// [§1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_token_referencing_another_issuers_list() {
    let mut issuer = Case::load(
        MockStatusList {
            list: &[0],
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;
    let other_issuer = Case::load(
        MockStatusList {
            list: &[0],
            ..Default::default()
        },
        NO_REFRESH,
    )
    .await;
    let other_list = other_issuer.list_uri();

    let mut claims = json!({
        "sub": "some_user",
        "exp": chrono::Utc::now().timestamp() + 3600,
        "status": {"status_list": {"idx": 0, "uri": other_list}},
    });
    let token = issuer
        .mock
        .generate_token_with_hs256sig(&mut claims, None)
        .expect("referenced token should be signed");

    let err = issuer
        .validate_token(&token)
        .expect_err("the token names another issuer's list, so it is rejected");
    let own_list = issuer.list_uri();
    assert!(
        matches!(
            &err,
            ValidateJwtError::Status(StatusCheckError::ForeignStatusList { expected, found })
                if *expected == own_list && *found == other_list
        ),
        "the rejection must name the issuer's list and the one the token named, got: {err:?}"
    );
}
