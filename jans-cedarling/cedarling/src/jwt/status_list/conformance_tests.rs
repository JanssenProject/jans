// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Checks Cedarling against the Relying Party rules of [draft-ietf-oauth-status-list-21].
//!
//! Each test serves a status list JWT from a mock issuer, loads it through
//! [`StatusListCache::init_for_iss`] (fetch, signature and claim validation), then
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
    InitForIssArgs, JwtStatus, JwtStatusError, StatusBitSize, StatusListCache,
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

/// A status list loaded into a [`StatusListCache`], with the validators
/// Cedarling built for the issuer. Dropping it stops the background refresh task.
struct LoadedStatusList {
    validators: JwtValidatorCache,
    _stop_refresh: DropGuard,
}

#[derive(Debug)]
enum StatusCheckError {
    /// The status list JWT was refused when it was fetched.
    Fetch(UpdateStatusListError),
    /// The Referenced Token was refused when it was validated.
    Validate(ValidateJwtError),
}

/// Fetches the status list `mock` serves, the way Cedarling does at startup.
async fn load_status_list(
    mock: &MockServer,
    refresh_interval_max: Duration,
) -> Result<LoadedStatusList, UpdateStatusListError> {
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

    let cache = StatusListCache::default();
    let validators = JwtValidatorCache::default();
    validators.init_for_iss(
        &iss_config,
        &JwtConfig {
            jwks: None,
            jwt_sig_validation: true,
            jwt_status_validation: true,
            signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
            ..Default::default()
        },
        &cache,
        None,
    );

    let cancel_tkn = CancellationToken::new();
    cache
        .init_for_iss(
            &iss_config,
            InitForIssArgs {
                validators: &validators,
                key_service: &key_service,
                token_cache: TokenCache::default(),
                logger: None,
                http_client,
                refresh_interval_max,
                cancel_tkn: cancel_tkn.clone(),
            },
        )
        .await?;

    Ok(LoadedStatusList {
        validators,
        _stop_refresh: cancel_tkn.drop_guard(),
    })
}

/// Validates a Referenced Token whose `status` points at index `idx` of the
/// list `mock` serves.
fn validate_referenced_token(
    mock: &mut MockServer,
    loaded: &LoadedStatusList,
    idx: usize,
) -> Result<ValidatedJwt, ValidateJwtError> {
    let mut claims = json!({
        "sub": "some_user",
        "exp": chrono::Utc::now().timestamp() + 3600,
    });
    let token = mock
        .generate_token_with_hs256sig(&mut claims, Some(idx))
        .expect("referenced token should be signed");
    let iss = mock.issuer();
    let validator = loaded
        .validators
        .get(&ValidatorInfo {
            iss: Some(&iss),
            token_kind: TokenKind::AuthorizeMultiIssuer(Cow::Borrowed(REFERENCED_TOKEN_TYPE)),
            algorithm: Algorithm::HS256,
        })
        .expect("a validator is built for the issuer's Referenced Tokens");
    validator.validate_jwt(
        &token,
        Some(mock.jwt_decoding_key().expect("mock decoding key")),
    )
}

/// Serves `list`, loads it and validates a Referenced Token at index `idx`.
async fn check_status(
    list: MockStatusList<'_>,
    idx: usize,
) -> Result<ValidatedJwt, StatusCheckError> {
    let mut mock = MockServer::new_with_defaults()
        .await
        .expect("mock server should start");
    mock.serve_status_list(list);
    let loaded = load_status_list(&mock, NO_REFRESH)
        .await
        .map_err(StatusCheckError::Fetch)?;
    validate_referenced_token(&mut mock, &loaded, idx).map_err(StatusCheckError::Validate)
}

/// Status `0x00` is VALID ([§7.1]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
#[tokio::test]
async fn accepts_valid_status() {
    check_status(
        MockStatusList {
            list: &[0b0000_0010],
            ..Default::default()
        },
        0,
    )
    .await
    .expect("index 0 holds 0x00 (VALID), so the token is accepted");
}

/// Status `0x01` is INVALID: the token is revoked ([§7.1]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
#[tokio::test]
async fn rejects_invalid_status() {
    let err = check_status(
        MockStatusList {
            list: &[0b0000_0010],
            ..Default::default()
        },
        1,
    )
    .await
    .expect_err("index 1 holds 0x01 (INVALID), so the token is rejected");
    assert!(
        matches!(
            err,
            StatusCheckError::Validate(ValidateJwtError::RejectJwtStatus(JwtStatus::Invalid))
        ),
        "an INVALID entry must reject the token with that status, got: {err:?}"
    );
}

/// Status `0x02` is SUSPENDED: the token is temporarily invalid ([§7.1]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
#[tokio::test]
async fn rejects_suspended_status() {
    let err = check_status(
        MockStatusList {
            bits: bits(2),
            list: &[0b0000_1000],
            ..Default::default()
        },
        1,
    )
    .await
    .expect_err("index 1 holds 0x02 (SUSPENDED), so the token is rejected");
    assert!(
        matches!(
            err,
            StatusCheckError::Validate(ValidateJwtError::RejectJwtStatus(JwtStatus::Suspended))
        ),
        "a SUSPENDED entry must reject the token with that status, got: {err:?}"
    );
}

/// `0x03` is application specific ([§7.1]). Cedarling defines no meaning for it,
/// so no statement about the token can be made and it is rejected ([§8.3] step 7
/// and the closing paragraph).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_application_specific_status_0x03() {
    let err = check_status(
        MockStatusList {
            bits: bits(2),
            list: &[0b0000_0011],
            ..Default::default()
        },
        0,
    )
    .await
    .expect_err("Cedarling gives 0x03 no meaning, so the token is rejected");
    assert!(
        matches!(
            err,
            StatusCheckError::Validate(ValidateJwtError::RejectJwtStatus(JwtStatus::Custom(0x03)))
        ),
        "an application-specific status must reject the token, got: {err:?}"
    );
}

/// `0x0C` to `0x0F` are application specific ([§7.1]); rejected as in
/// [`rejects_application_specific_status_0x03`] ([§8.3]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_application_specific_status_0x0c() {
    let err = check_status(
        MockStatusList {
            bits: bits(4),
            list: &[0x0C],
            ..Default::default()
        },
        0,
    )
    .await
    .expect_err("Cedarling gives 0x0C no meaning, so the token is rejected");
    assert!(
        matches!(
            err,
            StatusCheckError::Validate(ValidateJwtError::RejectJwtStatus(JwtStatus::Custom(0x0C)))
        ),
        "an application-specific status must reject the token, got: {err:?}"
    );
}

/// `0x04` to `0x0B` are reserved for future registration ([§7.1]); an unknown
/// status gives no statement about the token, so it is rejected ([§8.3]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_reserved_status_0x05() {
    let err = check_status(
        MockStatusList {
            bits: bits(4),
            list: &[0x05],
            ..Default::default()
        },
        0,
    )
    .await
    .expect_err("0x05 is reserved, so the token is rejected");
    assert!(
        matches!(
            err,
            StatusCheckError::Validate(ValidateJwtError::RejectJwtStatus(JwtStatus::Custom(0x05)))
        ),
        "a reserved status must reject the token, got: {err:?}"
    );
}

/// `0x10` to `0xFF` are reserved for future registration ([§7.1]); rejected as
/// in [`rejects_reserved_status_0x05`] ([§8.3]).
///
/// [§7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_reserved_status_0xff() {
    let err = check_status(
        MockStatusList {
            bits: bits(8),
            list: &[0xFF],
            ..Default::default()
        },
        0,
    )
    .await
    .expect_err("0xFF is reserved, so the token is rejected");
    assert!(
        matches!(
            err,
            StatusCheckError::Validate(ValidateJwtError::RejectJwtStatus(JwtStatus::Custom(0xFF)))
        ),
        "a reserved status must reject the token, got: {err:?}"
    );
}

/// An index past the end of the list gives no statement about the token, which
/// MUST be rejected ([§8.3] step 6).
///
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_index_out_of_bounds() {
    let err = check_status(
        MockStatusList {
            list: &[0],
            ..Default::default()
        },
        8,
    )
    .await
    .expect_err("a one-byte, 1-bit list has no index 8, so the token is rejected");
    assert!(
        matches!(
            err,
            StatusCheckError::Validate(ValidateJwtError::GetJwtStatus(
                JwtStatusError::StatusListIdxOutOfBounds
            ))
        ),
        "an out-of-bounds index must reject the token, got: {err:?}"
    );
}

/// The status list JWT `typ` MUST be `statuslist+jwt` ([§5.1]); a token that is
/// not valid per [§5.1] is not used ([§8.3] step 4).
///
/// [§5.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-5.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_list_with_wrong_typ() {
    let err = check_status(
        MockStatusList {
            list: &[0],
            typ: TokenTypeHeader::Jwt,
            ..Default::default()
        },
        0,
    )
    .await
    .expect_err("a list served with `typ: JWT` is refused at fetch");
    assert!(
        matches!(
            &err,
            StatusCheckError::Fetch(UpdateStatusListError::UnexpectedTyp { found, .. })
                if found.as_deref() == Some("JWT")
        ),
        "a status list JWT with the wrong `typ` must be refused, got: {err:?}"
    );
}

/// `iat` is REQUIRED in a status list JWT ([§5.1], [§8.3] step 3).
///
/// [§5.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-5.1
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
#[tokio::test]
async fn rejects_list_without_iat() {
    let err = check_status(
        MockStatusList {
            list: &[0],
            with_iat: false,
            ..Default::default()
        },
        0,
    )
    .await
    .expect_err("a list without `iat` is refused at fetch");
    assert!(
        matches!(
            &err,
            StatusCheckError::Fetch(UpdateStatusListError::ValidateJwtError(
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
    check_status(
        MockStatusList {
            list: &[0],
            exp_offset_secs: None,
            ..Default::default()
        },
        0,
    )
    .await
    .expect("a list without `exp` has no expiry, so a VALID entry is accepted");
}

/// An expired status list MUST NOT be used ([§8.3] step 4, [§13.7]).
///
/// [§8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
/// [§13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
#[tokio::test]
async fn rejects_list_expired_at_fetch() {
    let err = check_status(
        MockStatusList {
            list: &[0],
            exp_offset_secs: Some(-(leeway_secs() + 60)),
            ..Default::default()
        },
        0,
    )
    .await
    .expect_err("a list whose `exp` passed beyond the leeway is refused at fetch");
    assert!(
        matches!(
            &err,
            StatusCheckError::Fetch(UpdateStatusListError::ValidateJwtError(
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
    check_status(
        MockStatusList {
            list: &[0],
            exp_offset_secs: Some(-10),
            ..Default::default()
        },
        0,
    )
    .await
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
    let mut mock = MockServer::new_with_defaults()
        .await
        .expect("mock server should start");
    mock.serve_status_list(MockStatusList {
        list: &[0],
        exp_offset_secs: Some(exp_offset_expiring_soon()),
        ttl_secs: None,
        ..Default::default()
    });
    let loaded = load_status_list(&mock, NO_REFRESH)
        .await
        .expect("the list is inside the leeway at fetch, so it loads");
    validate_referenced_token(&mut mock, &loaded, 0)
        .expect("the cached list has not expired yet, so a VALID entry is accepted");

    mock.fail_status_list_endpoint();
    tokio::time::sleep(Duration::from_secs(4)).await;

    let err = validate_referenced_token(&mut mock, &loaded, 0)
        .expect_err("the cached list expired and no fresh copy exists, so the token is rejected");
    assert!(
        matches!(err, ValidateJwtError::MissingStatusList),
        "the expired list must be dropped (fail closed), got: {err:?}"
    );
}

/// Without `ttl`, the Relying Party SHOULD check for updates no later than `exp`
/// ([§13.7]), even when the configured refresh interval is longer.
///
/// [§13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
#[tokio::test(flavor = "multi_thread")]
async fn refreshes_list_by_exp_when_ttl_is_absent() {
    let mut mock = MockServer::new_with_defaults()
        .await
        .expect("mock server should start");
    mock.serve_status_list(MockStatusList {
        list: &[0],
        exp_offset_secs: Some(exp_offset_expiring_soon()),
        ttl_secs: None,
        ..Default::default()
    });
    let loaded = load_status_list(&mock, NO_REFRESH)
        .await
        .expect("the list is inside the leeway at fetch, so it loads");

    // the issuer revokes index 0 in its next list
    mock.serve_status_list(MockStatusList {
        list: &[0b0000_0001],
        ttl_secs: None,
        ..Default::default()
    });
    tokio::time::sleep(Duration::from_secs(4)).await;

    let err = validate_referenced_token(&mut mock, &loaded, 0)
        .expect_err("the list was refreshed at its `exp`, so the revocation is seen");
    assert!(
        matches!(err, ValidateJwtError::RejectJwtStatus(JwtStatus::Invalid)),
        "the refreshed list must reject the revoked token, got: {err:?}"
    );
}
