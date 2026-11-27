// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::{
    collections::{HashMap, hash_map::Entry},
    sync::{Arc, Mutex},
    time::Duration,
};

use arc_swap::ArcSwap;
use jsonwebtoken::DecodingKey;
use tokio_util::sync::CancellationToken;
use url::Url;

use crate::{
    LogLevel, LogWriter,
    common::issuer_utils::IssClaim,
    http::HttpClient,
    jwt::{
        IssuerConfig, TokenCache,
        decode::decode_jwt,
        key_service::{DecodingKeyInfo, KeyService},
        log_entry::JwtLogEntry,
        token_cache::IndexKey,
        validation::{JwtValidator, JwtValidatorCache, TokenKind, ValidateJwtError, ValidatorInfo},
    },
    log::Logger,
    rt::sleep,
};

use super::{
    CachedStatusList, IdxOutOfBounds, ParseStatusListJwtError, StatusAtError, StatusCheckError,
    StatusListExpiry, StatusListJwt, StatusListJwtStr, StatusListState, UnavailableStatusList,
    UnboundReason, UnixTime, UpdateStatusListError,
};

struct StatusListUpdateCtx {
    source: StatusListSource,
    /// Interval until the next refresh: the JWT `ttl` capped by `refresh_interval_max`,
    /// see [`effective_refresh_interval`].
    refresh_interval: Duration,
    /// Upper bound on the refresh interval. The Status List JWT's `ttl`
    /// claim is honored when present, but capped to this value; when the JWT omits
    /// `ttl`, this value is used directly.
    refresh_interval_max: Duration,
    logger: Option<Logger>,
    cancel_tkn: CancellationToken,
}

pub(crate) struct InitForIssArgs {
    pub(crate) validators: Arc<JwtValidatorCache>,
    pub(crate) key_service: Arc<KeyService>,
    pub(crate) token_cache: TokenCache,
    pub(crate) logger: Option<Logger>,
    pub(crate) http_client: HttpClient,
    pub(crate) refresh_interval_max: Duration,
    pub(crate) cancel_tkn: CancellationToken,
}

/// Floor for an issuer-requested `ttl` and for a retry while a list nears its
/// expiry, so a small `ttl` or an outage cannot turn the refresh task into a busy
/// loop ([draft-ietf-oauth-status-list-21 §11.5] asks clients to bound these
/// values). It is the same floor as for `CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_MAX`.
///
/// [draft-ietf-oauth-status-list-21 §11.5]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-11.5
const MIN_REFRESH_INTERVAL: Duration =
    Duration::from_secs(crate::jwt_config::MIN_STATUS_LIST_REFRESH_SECS);

/// Resolve effective refresh interval as `min(jwt_ttl, refresh_interval_max)`,
/// or `refresh_interval_max` when the JWT omits `ttl`. Never below
/// [`MIN_REFRESH_INTERVAL`] unless the configured max itself is lower.
fn effective_refresh_interval(jwt_ttl_secs: Option<u64>, max: Duration) -> Duration {
    match jwt_ttl_secs {
        Some(t) => Duration::from_secs(t).max(MIN_REFRESH_INTERVAL).min(max),
        None => max,
    }
}

/// Per the status list spec, `sub` is the status list's own URI; `iss`, when
/// present, must be the issuer whose endpoint served it.
///
/// `sub` must equal the URI exactly, as the spec requires: parsing already
/// ignores host case and a default port, and a trailing slash or a query names
/// another URI.
fn check_bound_to(
    jwt: &StatusListJwt,
    iss: &IssClaim,
    status_list_url: &Url,
) -> Result<(), UnboundReason> {
    if !Url::parse(&jwt.sub).is_ok_and(|sub| sub == *status_list_url) {
        return Err(UnboundReason::Sub {
            found: jwt.sub.clone(),
        });
    }
    match &jwt.iss {
        Some(jwt_iss) if jwt_iss != iss => Err(UnboundReason::Iss {
            found: jwt_iss.clone(),
        }),
        _ => Ok(()),
    }
}

/// The JWT type a status list JWT MUST carry, per [draft-ietf-oauth-status-list-21 §5.1].
///
/// [draft-ietf-oauth-status-list-21 §5.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-5.1
const STATUS_LIST_TYP: &str = "statuslist+jwt";

/// `typ` is a media type, so it compares without case, and a value without `/`
/// stands for `application/<value>` ([RFC 7515 §4.1.9]).
///
/// [RFC 7515 §4.1.9]: https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.9
fn is_status_list_typ(typ: &str) -> bool {
    let subtype = match typ.split_once('/') {
        Some((media_type, subtype)) if media_type.eq_ignore_ascii_case("application") => subtype,
        Some(_) => return false,
        None => typ,
    };
    subtype.eq_ignore_ascii_case(STATUS_LIST_TYP)
}

/// A status list JWT that passed every check, ready to cache.
struct ParsedStatusList {
    list: CachedStatusList,
    ttl_secs: Option<u64>,
}

/// Validates a fetched status list JWT and parses its list: signature and
/// registered claims first, then `typ`, the binding to its URI and issuer, and
/// `exp` against `now`, in the order of [draft-ietf-oauth-status-list-21 §8.3]
/// steps 3 and 4. `exp` is checked here rather than by the validator, so the
/// error names the date the list expired.
///
/// [draft-ietf-oauth-status-list-21 §8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
fn parse_status_list_jwt(
    jwt: &StatusListJwtStr,
    validator: &JwtValidator,
    decoding_key: Option<Arc<DecodingKey>>,
    iss: &IssClaim,
    status_list_url: &Url,
    now: u64,
) -> Result<ParsedStatusList, ParseStatusListJwtError> {
    let validated_jwt = validator.validate_jwt(&jwt.0, decoding_key)?;

    let typ = jsonwebtoken::decode_header(&jwt.0)
        .map_err(ValidateJwtError::from)?
        .typ;
    if !typ.as_deref().is_some_and(is_status_list_typ) {
        return Err(ParseStatusListJwtError::UnexpectedTyp { found: typ });
    }

    let status_list_jwt: StatusListJwt = validated_jwt
        .try_into()
        .map_err(ParseStatusListJwtError::DeserializeClaims)?;
    check_bound_to(&status_list_jwt, iss, status_list_url)
        .map_err(ParseStatusListJwtError::Unbound)?;
    let ttl_secs = status_list_jwt.ttl;
    let list = status_list_jwt.into_cached(validator.leeway_secs())?;
    if let Some(expiry) = list.expiry().filter(|expiry| expiry.has_passed(now)) {
        return Err(ParseStatusListJwtError::Expired(expiry));
    }
    Ok(ParsedStatusList { list, ttl_secs })
}

/// Waits the refresh interval, but no longer than until the cached list's `exp`,
/// so a list is refreshed by its `exp` even without a `ttl`
/// ([draft-ietf-oauth-status-list-21 §13.7]). The clock-skew leeway after `exp`
/// then covers the fetch and its retries, so a healthy issuer never leaves a
/// window in which tokens are rejected.
///
/// [draft-ietf-oauth-status-list-21 §13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
fn next_refresh_in(
    refresh_interval: Duration,
    expiry: Option<StatusListExpiry>,
    now: u64,
) -> Duration {
    match expiry {
        Some(expiry) => refresh_interval.min(expiry.until_exp(now).max(MIN_REFRESH_INTERVAL)),
        None => refresh_interval,
    }
}

/// One issuer's status list: its refresh task writes it and the validators of
/// the issuer's tokens read it, so a token is only checked against the list of
/// its own issuer.
#[derive(Debug)]
pub(crate) struct IssuerStatusList {
    uri: Url,
    state: ArcSwap<StatusListState>,
}

impl IssuerStatusList {
    fn new(uri: Url, state: StatusListState) -> Self {
        Self {
            uri,
            state: ArcSwap::from_pointee(state),
        }
    }

    /// Expiry of the list in use; `None` when there is none or it has no `exp`.
    fn expiry(&self) -> Option<StatusListExpiry> {
        match &**self.state.load() {
            StatusListState::Usable(list) => list.expiry(),
            StatusListState::Unavailable(_) => None,
        }
    }

    /// Checks the status of a token whose `status` claim points at entry `idx` of
    /// the list at `uri`. Only VALID passes; every other outcome names the list
    /// and why no statement about the token can be made
    /// ([draft-ietf-oauth-status-list-21 §8.3]).
    ///
    /// The token must reference this list: Cedarling does not check a token
    /// against another party's list.
    ///
    /// [draft-ietf-oauth-status-list-21 §8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
    pub(crate) fn check_status(
        &self,
        uri: &str,
        idx: usize,
        now: u64,
    ) -> Result<(), StatusCheckError> {
        if !Url::parse(uri).is_ok_and(|found| found == self.uri) {
            return Err(StatusCheckError::ForeignStatusList {
                expected: self.uri.to_string(),
                found: uri.to_string(),
            });
        }
        let uri = self.uri.as_str();
        let state = self.state.load();
        let list = match &**state {
            StatusListState::Usable(list) => list,
            StatusListState::Unavailable(unavailable) => {
                return Err(StatusCheckError::Unavailable {
                    uri: uri.to_string(),
                    unavailable: unavailable.clone(),
                });
            },
        };
        let status = list.status_at(idx, now).map_err(|err| {
            let uri = uri.to_string();
            match err {
                StatusAtError::Expired(expiry) => StatusCheckError::Expired { uri, expiry },
                StatusAtError::IdxOutOfBounds(IdxOutOfBounds { len }) => {
                    StatusCheckError::IdxOutOfBounds { uri, idx, len }
                },
            }
        })?;
        if status.is_valid() {
            Ok(())
        } else {
            Err(StatusCheckError::Rejected {
                uri: uri.to_string(),
                idx,
                status,
            })
        }
    }
}

/// Identifies one status list and its refresh task: the issuer and the URI it
/// publishes the list at.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
struct StatusListKey {
    iss: IssClaim,
    uri: Url,
}

/// The status lists of all trusted issuers, one per issuer and URI. Trusted
/// issuers that share an `iss` and a `status_list_endpoint` share one list and
/// one refresh task.
#[derive(Debug, Default, Clone)]
pub(crate) struct StatusListRegistry {
    lists: Arc<Mutex<HashMap<StatusListKey, Arc<IssuerStatusList>>>>,
}

impl StatusListRegistry {
    /// Returns the status list of the given issuer; the first time, loads it and
    /// starts the task that keeps it fresh.
    ///
    /// A list that cannot be loaded does not fail the issuer: it is recorded as
    /// unavailable, so only the tokens that reference it are rejected, and the
    /// task keeps retrying. An issuer with no `status_list_endpoint`, including
    /// every static-form issuer, has no list (`None`) and is logged as a WARN; its
    /// tokens carrying `status` are rejected.
    pub(crate) async fn register(
        &self,
        iss_config: &IssuerConfig,
        args: InitForIssArgs,
    ) -> Option<Arc<IssuerStatusList>> {
        let InitForIssArgs {
            validators,
            key_service,
            token_cache,
            logger,
            http_client,
            refresh_interval_max,
            cancel_tkn,
        } = args;
        let Some(status_list_url) = iss_config
            .openid_config
            .as_ref()
            .and_then(|openid_config| openid_config.status_list_endpoint.as_ref())
        else {
            if let Some(logger) = &logger {
                let source = if iss_config.openid_config.is_some() {
                    "its discovery document has no status_list_endpoint"
                } else {
                    "its static JWKS comes with no status list"
                };
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "trusted issuer '{}': {source}; with status validation on, its tokens \
                         that carry a `status` claim will be rejected",
                        iss_config.policy.name,
                    ),
                    Some(LogLevel::WARN),
                ));
            }
            return None;
        };
        let iss = iss_config.policy.iss_claim().clone();
        let key = StatusListKey {
            iss: iss.clone(),
            uri: status_list_url.clone(),
        };
        if let Some(list) = self
            .lists
            .lock()
            .expect("status list registry lock")
            .get(&key)
        {
            return Some(list.clone());
        }

        let source = StatusListSource {
            url: status_list_url.clone(),
            iss: iss.clone(),
            validators,
            key_service,
            http_client,
        };
        let FirstLoad {
            state,
            refresh_interval,
        } = first_load(&source, refresh_interval_max).await;
        if let StatusListState::Unavailable(unavailable) = &state {
            logger.log_any(JwtLogEntry::new(
                format!(
                    "trusted issuer '{}': failed to load its status list from '{}': {}; its \
                     tokens that carry a `status` claim are rejected until the list loads; \
                     next attempt in {} s",
                    iss_config.policy.name,
                    source.url,
                    unavailable.cause(),
                    refresh_interval.as_secs(),
                ),
                Some(LogLevel::ERROR),
            ));
        }

        let list = {
            let mut lists = self.lists.lock().expect("status list registry lock");
            match lists.entry(key) {
                // another trusted issuer with the same `iss` loaded it meanwhile
                Entry::Occupied(registered) => return Some(registered.get().clone()),
                Entry::Vacant(slot) => slot
                    .insert(Arc::new(IssuerStatusList::new(
                        status_list_url.clone(),
                        state,
                    )))
                    .clone(),
            }
        };
        let writer = StatusListWriter {
            list: list.clone(),
            // tokens of this `iss` are the only ones checked against this list
            on_change: move || token_cache.invalidate_by_index(&IndexKey::Iss(iss.clone())),
        };
        let ctx = StatusListUpdateCtx {
            source,
            refresh_interval,
            refresh_interval_max,
            logger,
            cancel_tkn,
        };
        crate::rt::spawn_task(keep_status_list_updated(writer, ctx));
        Some(list)
    }
}

/// The state of a status list after its first fetch, and when to refresh it.
struct FirstLoad {
    state: StatusListState,
    refresh_interval: Duration,
}

/// Fetches the list for the first time. A list that cannot be loaded starts as
/// unavailable and is retried at `refresh_interval_max`.
async fn first_load(source: &StatusListSource, refresh_interval_max: Duration) -> FirstLoad {
    match source.fetch().await {
        // Per the IETF `oauth-status-list` spec, the JWT's `ttl` claim drives the
        // refresh cadence, but the bootstrap
        // `CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_MAX` caps it: the issuer can
        // always request a *more frequent* refresh, but never a less frequent one.
        // When the JWT omits `ttl`, the max is used directly so the cache never goes
        // stale forever.
        Ok(parsed) => FirstLoad {
            state: StatusListState::Usable(parsed.list),
            refresh_interval: effective_refresh_interval(parsed.ttl_secs, refresh_interval_max),
        },
        Err(err) => FirstLoad {
            state: StatusListState::Unavailable(UnavailableStatusList::new(
                jsonwebtoken::get_current_timestamp(),
                Arc::new(err),
            )),
            refresh_interval: refresh_interval_max,
        },
    }
}

/// What the refresh task does with the cached list after a refresh failed.
#[derive(Debug, PartialEq, derive_more::Display)]
enum AfterFailedRefresh {
    /// The list has not expired, so it may still be relied upon
    /// ([draft-ietf-oauth-status-list-21 §13.7]): keep it and retry.
    ///
    /// [draft-ietf-oauth-status-list-21 §13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
    #[display("the cached list stays in use until its {_0}")]
    Keep(StatusListExpiry),
    /// The list has expired: drop it, so the tokens that reference it are rejected.
    #[display(
        "the cached list is past its {_0}, so tokens that reference it are rejected until a \
         refresh succeeds"
    )]
    DropExpired(StatusListExpiry),
    /// The list has no `exp`, so nothing says it may still be relied upon: drop it.
    #[display(
        "the cached list has no `exp`, so it is no longer used and tokens that reference it are \
         rejected until a refresh succeeds"
    )]
    DropWithoutExp,
    /// No list is in use: it stays unavailable, with the latest cause.
    #[display(
        "no usable list since {since}; tokens that reference it are rejected until a refresh \
         succeeds"
    )]
    StayUnavailable { since: UnixTime },
}

impl AfterFailedRefresh {
    fn keeps_list(&self) -> bool {
        matches!(self, Self::Keep(_))
    }
}

fn after_failed_refresh(state: &StatusListState, now: u64) -> AfterFailedRefresh {
    match state {
        StatusListState::Unavailable(unavailable) => AfterFailedRefresh::StayUnavailable {
            since: unavailable.since(),
        },
        StatusListState::Usable(list) => match list.expiry() {
            None => AfterFailedRefresh::DropWithoutExp,
            Some(expiry) if expiry.has_passed(now) => AfterFailedRefresh::DropExpired(expiry),
            Some(expiry) => AfterFailedRefresh::Keep(expiry),
        },
    }
}

/// The refresh task's handle on a status list. Every change that makes earlier
/// checks against the list stale also runs `on_change`, which clears the tokens
/// validated against it from the token cache.
struct StatusListWriter<F: Fn()> {
    list: Arc<IssuerStatusList>,
    on_change: F,
}

impl<F: Fn()> StatusListWriter<F> {
    fn replace(&self, list: CachedStatusList) {
        self.list
            .state
            .store(Arc::new(StatusListState::Usable(list)));
        (self.on_change)();
    }

    /// Records that the list could not be refreshed, keeping the time it first
    /// became unavailable and the latest cause.
    fn mark_unavailable(&self, now: u64, cause: Arc<UpdateStatusListError>) {
        let (unavailable, was_usable) = match &**self.list.state.load() {
            StatusListState::Unavailable(unavailable) => (unavailable.with_cause(cause), false),
            StatusListState::Usable(_) => (UnavailableStatusList::new(now, cause), true),
        };
        self.list
            .state
            .store(Arc::new(StatusListState::Unavailable(unavailable)));
        if was_usable {
            (self.on_change)();
        }
    }

    /// The list in use has expired: tokens validated against it must be checked
    /// again.
    fn expired(&self) {
        (self.on_change)();
    }
}

/// Where one issuer's status list comes from, and what checks it.
#[derive(derive_more::Display)]
#[display("the status list '{url}' of the trusted issuer '{iss}'")]
struct StatusListSource {
    url: Url,
    iss: IssClaim,
    validators: Arc<JwtValidatorCache>,
    key_service: Arc<KeyService>,
    http_client: HttpClient,
}

impl StatusListSource {
    /// Fetches the list and runs every check on it. The validator and the key are
    /// looked up on each fetch, so a list signed with a rotated key still loads.
    /// Both belong to the trusted issuer that publishes the list, not to the `iss`
    /// the response names: `iss` is optional in a status list JWT
    /// ([draft-ietf-oauth-status-list-21 §5.1]), and a different one is refused
    /// after the signature check.
    ///
    /// [draft-ietf-oauth-status-list-21 §5.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-5.1
    async fn fetch(&self) -> Result<ParsedStatusList, UpdateStatusListError> {
        let jwt = StatusListJwtStr::get_from_url(&self.url, &self.http_client)
            .await
            .map_err(UpdateStatusListError::GetStatusListJwt)?;
        let header = decode_jwt(&jwt.0)?.header;
        let decoding_key = self.key_service.get_key(&DecodingKeyInfo {
            issuer: Some(self.iss.clone()),
            kid: header.kid,
            algorithm: header.alg,
        });
        let validator = self
            .validators
            .get(&ValidatorInfo {
                iss: Some(&self.iss),
                token_kind: TokenKind::StatusList,
                algorithm: header.alg,
            })
            .ok_or_else(|| UpdateStatusListError::UnsupportedAlgorithm {
                url: self.url.to_string(),
                alg: header.alg,
            })?;

        parse_status_list_jwt(
            &jwt,
            &validator,
            decoding_key,
            &self.iss,
            &self.url,
            jsonwebtoken::get_current_timestamp(),
        )
        .map_err(|reason| UpdateStatusListError::Refused {
            url: self.url.to_string(),
            reason,
        })
    }
}

/// Keeps the status list updated, refreshing it every `refresh_interval` and no
/// later than its expiry.
async fn keep_status_list_updated<F: Fn()>(writer: StatusListWriter<F>, ctx: StatusListUpdateCtx) {
    let StatusListUpdateCtx {
        source,
        mut refresh_interval,
        refresh_interval_max,
        logger,
        cancel_tkn,
    } = ctx;

    loop {
        let wait = next_refresh_in(
            refresh_interval,
            writer.list.expiry(),
            jsonwebtoken::get_current_timestamp(),
        );
        tokio::select! {
            () = sleep(wait) => {},
            () = cancel_tkn.cancelled() => break,
        }

        // The validator already rejects tokens against the expired list; clearing
        // the token cache stops tokens validated before it expired from being
        // served while the refresh below runs. A failed refresh logs the expiry.
        if writer
            .list
            .expiry()
            .is_some_and(|expiry| expiry.has_passed(jsonwebtoken::get_current_timestamp()))
        {
            writer.expired();
        }

        let parsed = match source.fetch().await {
            Ok(parsed) => parsed,
            Err(err) => {
                let err = Arc::new(err);
                let now = jsonwebtoken::get_current_timestamp();
                let outcome = after_failed_refresh(&writer.list.state.load(), now);
                if !outcome.keeps_list() {
                    writer.mark_unavailable(now, err.clone());
                }
                let next_attempt = next_refresh_in(refresh_interval, writer.list.expiry(), now);
                logger.log_any(JwtLogEntry::new(
                    format!(
                        "failed to refresh {source}: {err}; {outcome}; next attempt in {} s",
                        next_attempt.as_secs(),
                    ),
                    Some(LogLevel::ERROR),
                ));
                continue;
            },
        };
        // Honor the refreshed JWT's `ttl` but cap it at the configured max; if the
        // refreshed JWT no longer has `ttl`, use the max directly.
        refresh_interval = effective_refresh_interval(parsed.ttl_secs, refresh_interval_max);
        writer.replace(parsed.list);
    }
}

#[cfg(test)]
impl IssuerStatusList {
    /// A list that never expires, for tests that check statuses only.
    pub(crate) fn for_test(uri: &str, list: super::StatusList) -> Self {
        Self::new(
            Url::parse(uri).expect("test status list URI"),
            StatusListState::Usable(list.into()),
        )
    }
}

#[cfg(test)]
impl StatusListRegistry {
    /// The list registered for `iss` at `uri`.
    pub(crate) fn list_for(&self, iss: &IssClaim, uri: &Url) -> Option<Arc<IssuerStatusList>> {
        self.lists
            .lock()
            .expect("status list registry lock")
            .get(&StatusListKey {
                iss: iss.clone(),
                uri: uri.clone(),
            })
            .cloned()
    }
}

#[cfg(test)]
mod test {
    use jsonwebtoken::Algorithm;

    use super::*;
    use crate::JwtConfig;
    use crate::common::policy_store::TrustedIssuer;
    use crate::http::HttpClientConfig;
    use crate::jwt::status_list::StatusList;
    use crate::jwt::test_utils::MockServer;
    use std::collections::HashSet;
    use std::time::Duration;

    const BOUND_LIST_URL: &str = "https://idp.example.com/statuslist/1";

    fn status_list_jwt(sub: &str, iss: Option<&str>) -> StatusListJwt {
        serde_json::from_value(serde_json::json!({
            "sub": sub,
            "iss": iss,
            "iat": 1,
            "status_list": { "bits": 1, "lst": "eNrbuRgAAhcBXQ" },
        }))
        .expect("status list JWT claims should deserialize")
    }

    /// The list in use, which the test expects to be usable.
    #[track_caller]
    fn usable_list(list: &IssuerStatusList) -> StatusList {
        list.state
            .load()
            .as_usable()
            .expect("a usable status list should be cached")
            .list
            .clone()
    }

    fn bound_url() -> Url {
        Url::parse(BOUND_LIST_URL).expect("valid status list URL")
    }

    #[test]
    fn status_list_from_another_uri_is_not_bound() {
        let jwt = status_list_jwt(
            "https://idp.example.com/statuslist/2",
            Some("https://idp.example.com"),
        );

        let reason = check_bound_to(
            &jwt,
            &IssClaim::new("https://idp.example.com"),
            &bound_url(),
        )
        .expect_err("a status list whose sub names another list must not be used for this URI");
        assert!(
            matches!(&reason, UnboundReason::Sub { found } if found == "https://idp.example.com/statuslist/2"),
            "the mismatch must name the received sub, got: {reason:?}"
        );
    }

    #[test]
    fn trailing_slash_sub_is_not_bound() {
        let jwt = status_list_jwt(
            "https://idp.example.com/statuslist/1/",
            Some("https://idp.example.com"),
        );

        let reason = check_bound_to(
            &jwt,
            &IssClaim::new("https://idp.example.com"),
            &bound_url(),
        )
        .expect_err(
            "the spec requires sub to equal the list URI, so a trailing slash is another URI",
        );
        assert!(
            matches!(reason, UnboundReason::Sub { .. }),
            "the mismatch must be reported on sub, got: {reason:?}"
        );
    }

    #[test]
    fn status_list_from_another_issuer_is_not_bound() {
        let jwt = status_list_jwt(BOUND_LIST_URL, Some("https://attacker.example.com"));

        let reason = check_bound_to(
            &jwt,
            &IssClaim::new("https://idp.example.com"),
            &bound_url(),
        )
        .expect_err("a status list issued by another issuer must not be used for this issuer");
        assert!(
            matches!(&reason, UnboundReason::Iss { found } if found.as_str() == "https://attacker.example.com"),
            "the mismatch must name the received iss, got: {reason:?}"
        );
    }

    #[test]
    fn status_list_without_iss_is_bound_by_sub() {
        let jwt = status_list_jwt(BOUND_LIST_URL, None);

        check_bound_to(
            &jwt,
            &IssClaim::new("https://idp.example.com"),
            &bound_url(),
        )
        .expect("iss is optional in a status list JWT, so a matching sub alone must bind it");
    }

    /// The status list `typ` MUST be `statuslist+jwt`
    /// ([draft-ietf-oauth-status-list-21 §5.1]); as a media type it compares
    /// without case and may carry the `application/` prefix ([RFC 7515 §4.1.9]).
    ///
    /// [draft-ietf-oauth-status-list-21 §5.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-5.1
    /// [RFC 7515 §4.1.9]: https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.9
    #[test]
    fn status_list_typ_matches_as_media_type() {
        for typ in [
            "statuslist+jwt",
            "StatusList+JWT",
            "application/statuslist+jwt",
            "Application/StatusList+JWT",
        ] {
            assert!(
                is_status_list_typ(typ),
                "`{typ}` names the status list media type and must be accepted"
            );
        }
        for typ in [
            "JWT",
            "jwt",
            "",
            "text/statuslist+jwt",
            "statuslist+cwt",
            "application/",
            "statuslist+jwt ",
        ] {
            assert!(
                !is_status_list_typ(typ),
                "`{typ}` is not the status list media type and must be refused"
            );
        }
    }

    /// A list is refreshed no later than its `exp` ([draft-ietf-oauth-status-list-21 §13.7]):
    /// the refresh waits the interval, cut short at `exp`, and never less than
    /// [`MIN_REFRESH_INTERVAL`].
    ///
    /// [draft-ietf-oauth-status-list-21 §13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
    #[test]
    fn next_refresh_is_cut_short_by_expiry() {
        let interval = Duration::from_secs(300);
        let now = 1_000;
        let expiry = |exp| {
            Some(StatusListExpiry {
                exp,
                leeway_secs: 60,
            })
        };

        assert_eq!(
            next_refresh_in(interval, None, now),
            interval,
            "a list without `exp` is refreshed by the interval alone"
        );
        assert_eq!(
            next_refresh_in(interval, expiry(1_010), now),
            Duration::from_secs(10),
            "the list is refreshed at its `exp`, while the leeway still covers the fetch"
        );
        assert_eq!(
            next_refresh_in(interval, expiry(1_002), now),
            MIN_REFRESH_INTERVAL,
            "a refresh, or a retry near `exp`, waits at least the floor"
        );
        assert_eq!(
            next_refresh_in(interval, expiry(5_000), now),
            interval,
            "a list that expires after the interval is refreshed by the interval"
        );
        assert_eq!(
            next_refresh_in(interval, expiry(900), now),
            MIN_REFRESH_INTERVAL,
            "an already expired list is refreshed after the floor, not in a busy loop"
        );
    }

    /// After a failed refresh a list is kept while it may still be relied upon,
    /// up to its `exp` plus leeway ([draft-ietf-oauth-status-list-21 §13.7]); a list
    /// that has expired or has no `exp` is dropped.
    ///
    /// [draft-ietf-oauth-status-list-21 §13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
    #[test]
    fn failed_refresh_keeps_list_until_it_expires() {
        let expiry = StatusListExpiry {
            exp: 1_000,
            leeway_secs: 60,
        };
        let usable = |expiry| {
            StatusListState::Usable(CachedStatusList {
                list: StatusList {
                    bit_size: 1u8.try_into().expect("valid bit size"),
                    list: vec![0],
                },
                expiry,
            })
        };
        assert_eq!(
            after_failed_refresh(&usable(Some(expiry)), 1_060),
            AfterFailedRefresh::Keep(expiry),
            "the list is usable through `exp` plus leeway, so it is kept"
        );
        assert_eq!(
            after_failed_refresh(&usable(Some(expiry)), 1_061),
            AfterFailedRefresh::DropExpired(expiry),
            "the list has expired, so it is dropped"
        );
        assert_eq!(
            after_failed_refresh(&usable(None), 1_000),
            AfterFailedRefresh::DropWithoutExp,
            "a list without `exp` has no usable-until point, so it is dropped"
        );
        let unavailable = StatusListState::Unavailable(UnavailableStatusList::new(
            900,
            Arc::new(unsupported_algorithm(Algorithm::HS256)),
        ));
        assert_eq!(
            after_failed_refresh(&unavailable, 1_000),
            AfterFailedRefresh::StayUnavailable {
                since: UnixTime(900)
            },
            "an unavailable list stays unavailable since its first failure"
        );
    }

    fn unsupported_algorithm(alg: Algorithm) -> UpdateStatusListError {
        UpdateStatusListError::UnsupportedAlgorithm {
            url: BOUND_LIST_URL.to_owned(),
            alg,
        }
    }

    fn usable_issuer_list() -> Arc<IssuerStatusList> {
        Arc::new(IssuerStatusList::for_test(
            BOUND_LIST_URL,
            StatusList {
                bit_size: 1u8.try_into().expect("valid bit size"),
                list: vec![0],
            },
        ))
    }

    /// Every change that makes earlier checks against the list stale clears the
    /// tokens validated against it, so a revoked or unchecked token is not served
    /// from the token cache; a change that does not, leaves them.
    #[test]
    fn writer_clears_cached_tokens_when_checks_go_stale() {
        let cleared = std::cell::Cell::new(0);
        let writer = StatusListWriter {
            list: usable_issuer_list(),
            on_change: || cleared.set(cleared.get() + 1),
        };
        let cause = || Arc::new(unsupported_algorithm(Algorithm::HS256));

        writer.mark_unavailable(1_000, cause());
        assert_eq!(cleared.get(), 1, "a usable list became unavailable");
        writer.mark_unavailable(1_001, cause());
        assert_eq!(
            cleared.get(),
            1,
            "the list was already unavailable, so no token was validated against it"
        );
        writer.replace(
            StatusList {
                bit_size: 1u8.try_into().expect("valid bit size"),
                list: vec![1],
            }
            .into(),
        );
        assert_eq!(
            cleared.get(),
            2,
            "a refreshed list may revoke cached tokens"
        );
        writer.expired();
        assert_eq!(
            cleared.get(),
            3,
            "an expired list no longer vouches for cached tokens"
        );
    }

    /// A list that could not be refreshed stays marked unavailable from its first
    /// failure, with the latest cause, so the admin sees since when and why.
    #[test]
    fn unavailable_list_keeps_first_failure_time_and_latest_cause() {
        let writer = StatusListWriter {
            list: usable_issuer_list(),
            on_change: || {},
        };
        writer.mark_unavailable(
            1_767_225_600,
            Arc::new(unsupported_algorithm(Algorithm::ES256)),
        );
        writer.mark_unavailable(
            1_767_225_900,
            Arc::new(unsupported_algorithm(Algorithm::HS256)),
        );

        let err = writer
            .list
            .check_status(BOUND_LIST_URL, 0, 1_767_226_000)
            .expect_err("an unavailable list gives no statement about the token");
        assert!(
            matches!(
                &err,
                StatusCheckError::Unavailable { uri, unavailable }
                    if uri == BOUND_LIST_URL
                        && unavailable.since() == UnixTime(1_767_225_600)
                        && matches!(
                            unavailable.cause(),
                            UpdateStatusListError::UnsupportedAlgorithm {
                                alg: Algorithm::HS256,
                                ..
                            }
                        )
            ),
            "the rejection names the list, the first failure time and the latest cause, got: \
             {err:?}"
        );
    }

    /// A token is checked only against its own issuer's list; a token that names
    /// another list is rejected with both URIs, so the admin sees the mismatch.
    #[test]
    fn token_referencing_another_list_is_rejected() {
        let other = "https://other.example.com/statuslist/1";
        let err = usable_issuer_list()
            .check_status(other, 0, 1_000)
            .expect_err("the token names a list its issuer does not publish");
        assert!(
            matches!(
                &err,
                StatusCheckError::ForeignStatusList { expected, found }
                    if expected == BOUND_LIST_URL && found == other
            ),
            "the rejection must name the issuer's list and the one the token named, got: {err:?}"
        );
    }

    #[test]
    fn zero_ttl_is_floored() {
        let max = Duration::from_secs(300);
        assert_eq!(
            effective_refresh_interval(Some(0), max),
            MIN_REFRESH_INTERVAL
        );
        assert_eq!(
            effective_refresh_interval(Some(1), max),
            Duration::from_secs(5),
            "an issuer's `ttl` below the 5 s floor of the bootstrap max is raised to it"
        );
        assert_eq!(
            effective_refresh_interval(Some(0), Duration::from_millis(200)),
            Duration::from_millis(200)
        );
        assert_eq!(
            effective_refresh_interval(Some(60), max),
            Duration::from_secs(60)
        );
    }

    #[tokio::test]
    async fn keep_status_list_updated() {
        let http_client = HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
            max_response_size_bytes: None,
        })
        .expect("http client should be constructed");

        // Setup
        let validators = JwtValidatorCache::default();
        let key_service = KeyService::default();
        let mut mock_server = MockServer::new_with_defaults().await.unwrap();
        key_service
            .fetch_keys(
                &mock_server.openid_config().issuer,
                &mock_server.openid_config().jwks_uri,
                None,
                http_client.clone(),
            )
            .await
            .unwrap();
        // we initialize the status list with a 1 sec ttl
        mock_server.generate_status_list_endpoint(1u8.try_into().unwrap(), &[0b1111_1110], Some(1));
        let registry = StatusListRegistry::default();

        let ti = TrustedIssuer::from_configuration(
            "some_iss",
            mock_server.openid_config_endpoint().unwrap(),
        )
        .expect("test configuration endpoint should derive an iss");

        let iss_config = IssuerConfig {
            policy: Arc::new(ti),
            tokens: HashMap::new(),
            openid_config: Some(mock_server.openid_config()),
        };
        validators.init_status_list_validators(
            &iss_config,
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: false,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
                ..Default::default()
            },
        );
        let status_list = registry
            .register(
                &iss_config,
                InitForIssArgs {
                    validators: Arc::new(validators),
                    key_service: Arc::new(key_service),
                    token_cache: TokenCache::default(),
                    logger: None,
                    http_client,
                    // Small cap so the JWT ttl (1s) is capped to a sub-second
                    // value, keeping the test fast.
                    refresh_interval_max: Duration::from_millis(200),
                    cancel_tkn: CancellationToken::new(),
                },
            )
            .await
            .expect("the mock issuer publishes a status list");

        // Check if the status list is the same as we first generated
        assert_eq!(
            usable_list(&status_list),
            StatusList {
                bit_size: 1u8.try_into().unwrap(),
                list: vec![0b1111_1110],
            },
            "the status list is wrong",
        );

        // Update the status in the server
        mock_server.generate_status_list_endpoint(1u8.try_into().unwrap(), &[0b0000_0001], None);

        // Wait for the token to expire and get updated
        tokio::time::sleep(Duration::from_millis(600)).await;

        // Check if the status list got updated
        assert_eq!(
            usable_list(&status_list),
            StatusList {
                bit_size: 1u8.try_into().unwrap(),
                list: vec![0b0000_0001],
            },
            "the status list was not updated",
        );
    }

    /// When the Status List JWT has no `ttl` claim, the bootstrap-config
    /// `status_list_refresh_interval_max` is used directly so the background
    /// refresh task still runs.
    #[tokio::test]
    async fn refresh_max_used_when_jwt_has_no_ttl() {
        let http_client = HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
            max_response_size_bytes: None,
        })
        .expect("http client should be constructed");

        let validators = JwtValidatorCache::default();
        let key_service = KeyService::default();
        let mut mock_server = MockServer::new_with_defaults().await.unwrap();
        key_service
            .fetch_keys(
                &mock_server.openid_config().issuer,
                &mock_server.openid_config().jwks_uri,
                None,
                http_client.clone(),
            )
            .await
            .unwrap();
        // initial JWT has no `ttl` claim - relies on the max to schedule a refresh
        mock_server
            .generate_status_list_endpoint_without_ttl(1u8.try_into().unwrap(), &[0b1111_1110]);
        let registry = StatusListRegistry::default();

        let ti = TrustedIssuer::from_configuration(
            "some_iss",
            mock_server.openid_config_endpoint().unwrap(),
        )
        .expect("test configuration endpoint should derive an iss");

        let iss_config = IssuerConfig {
            policy: Arc::new(ti),
            tokens: HashMap::new(),
            openid_config: Some(mock_server.openid_config()),
        };
        validators.init_status_list_validators(
            &iss_config,
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: false,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
                ..Default::default()
            },
        );
        // 1-second max so the test runs fast
        let status_list = registry
            .register(
                &iss_config,
                InitForIssArgs {
                    validators: Arc::new(validators),
                    key_service: Arc::new(key_service),
                    token_cache: TokenCache::default(),
                    logger: None,
                    http_client,
                    refresh_interval_max: Duration::from_millis(200),
                    cancel_tkn: CancellationToken::new(),
                },
            )
            .await
            .expect("the mock issuer publishes a status list");

        // Update server-side list (still without ttl) before refresh fires
        mock_server
            .generate_status_list_endpoint_without_ttl(1u8.try_into().unwrap(), &[0b0000_0001]);

        tokio::time::sleep(Duration::from_millis(600)).await;

        assert_eq!(
            usable_list(&status_list),
            StatusList {
                bit_size: 1u8.try_into().unwrap(),
                list: vec![0b0000_0001],
            },
            "max interval did not trigger a refresh",
        );
    }

    /// When the Status List JWT's `ttl` exceeds the bootstrap-config
    /// `status_list_refresh_interval_max`, the refresh loop must cap the sleep
    /// at the max so the cache still refreshes before the JWT's own ttl.
    #[tokio::test]
    async fn refresh_capped_when_jwt_ttl_exceeds_max() {
        let http_client = HttpClient::new(HttpClientConfig {
            max_retries: 0,
            retry_delay: Duration::from_millis(3),
            request_timeout: Duration::from_millis(500),
            max_response_size_bytes: None,
        })
        .expect("http client should be constructed");

        let validators = JwtValidatorCache::default();
        let key_service = KeyService::default();
        let mut mock_server = MockServer::new_with_defaults().await.unwrap();
        key_service
            .fetch_keys(
                &mock_server.openid_config().issuer,
                &mock_server.openid_config().jwks_uri,
                None,
                http_client.clone(),
            )
            .await
            .unwrap();
        // JWT ttl = 10s, much larger than refresh_interval_max = 1s
        mock_server.generate_status_list_endpoint(
            1u8.try_into().unwrap(),
            &[0b1111_1110],
            Some(10),
        );
        let registry = StatusListRegistry::default();

        let ti = TrustedIssuer::from_configuration(
            "some_iss",
            mock_server.openid_config_endpoint().unwrap(),
        )
        .expect("test configuration endpoint should derive an iss");

        let iss_config = IssuerConfig {
            policy: Arc::new(ti),
            tokens: HashMap::new(),
            openid_config: Some(mock_server.openid_config()),
        };
        validators.init_status_list_validators(
            &iss_config,
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: false,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
                ..Default::default()
            },
        );
        let status_list = registry
            .register(
                &iss_config,
                InitForIssArgs {
                    validators: Arc::new(validators),
                    key_service: Arc::new(key_service),
                    token_cache: TokenCache::default(),
                    logger: None,
                    http_client,
                    refresh_interval_max: Duration::from_millis(200),
                    cancel_tkn: CancellationToken::new(),
                },
            )
            .await
            .expect("the mock issuer publishes a status list");

        // Update the server-side list before the cap-driven refresh fires.
        mock_server.generate_status_list_endpoint(
            1u8.try_into().unwrap(),
            &[0b0000_0001],
            Some(10),
        );

        // Sleep > refresh_interval_max but < JWT ttl: refresh only fires if
        // the loop honored min(ttl, refresh_interval_max).
        tokio::time::sleep(Duration::from_millis(600)).await;

        assert_eq!(
            usable_list(&status_list),
            StatusList {
                bit_size: 1u8.try_into().unwrap(),
                list: vec![0b0000_0001],
            },
            "refresh did not respect the configured cap over the JWT ttl",
        );
    }
}
