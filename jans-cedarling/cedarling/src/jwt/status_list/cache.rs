// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::{
    collections::HashMap,
    sync::{Arc, RwLock},
    time::Duration,
};

use jsonwebtoken::DecodingKey;
use tokio_util::sync::CancellationToken;
use url::Url;

use crate::{
    LogLevel, LogWriter,
    common::issuer_utils::IssClaim,
    http::HttpClient,
    jwt::{
        IssuerConfig, TokenCache,
        decode::{DecodeJwtError, decode_jwt},
        key_service::KeyService,
        log_entry::JwtLogEntry,
        token_cache::IndexKey,
        validation::{JwtValidator, JwtValidatorCache, TokenKind, ValidateJwtError, ValidatorInfo},
    },
    log::Logger,
    rt::sleep,
};

use super::{
    CachedStatusList, GetStatusError, StatusAtError, StatusCheckError, StatusListExpiry,
    StatusListJwt, StatusListJwtStr, StatusListState, UnavailableStatusList, UnboundReason,
    UpdateStatusListError,
};

/// The value of the `status_list_uri` claim from a JWT
type StatusListUri = String;

struct StatusListUpdateCtx {
    /// Interval until the next refresh: the JWT `ttl` capped by `refresh_interval_max`,
    /// see [`effective_refresh_interval`].
    refresh_interval: Duration,
    /// Expiry of the list cached at start.
    expiry: Option<StatusListExpiry>,
    /// Upper bound on the refresh interval. The Status List JWT's `ttl`
    /// claim is honored when present, but capped to this value; when the JWT omits
    /// `ttl`, this value is used directly.
    refresh_interval_max: Duration,
    status_list_url: Url,
    iss: IssClaim,
    decoding_key: Option<Arc<DecodingKey>>,
    validator: Arc<JwtValidator>,
    status_lists: Arc<RwLock<HashMap<String, StatusListState>>>,
    logger: Option<Logger>,
    http_client: HttpClient,
    cancel_tkn: CancellationToken,
}

pub(crate) struct InitForIssArgs<'a> {
    pub validators: &'a JwtValidatorCache,
    pub key_service: &'a KeyService,
    pub token_cache: TokenCache,
    pub logger: Option<Logger>,
    pub http_client: HttpClient,
    pub refresh_interval_max: Duration,
    pub cancel_tkn: CancellationToken,
}

/// Floor for an issuer-requested `ttl`, so `ttl: 0` cannot turn the refresh
/// worker into a busy loop.
const MIN_REFRESH_INTERVAL: Duration = Duration::from_secs(1);

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
/// registered claims first, then `typ` and the binding to its URI and issuer, in
/// the order of [draft-ietf-oauth-status-list-21 §8.3] steps 3 and 4.
///
/// [draft-ietf-oauth-status-list-21 §8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
fn parse_status_list_jwt(
    jwt: &StatusListJwtStr,
    validator: &JwtValidator,
    decoding_key: Option<Arc<DecodingKey>>,
    iss: &IssClaim,
    status_list_url: &Url,
) -> Result<ParsedStatusList, UpdateStatusListError> {
    let validated_jwt = validator.validate_jwt(&jwt.0, decoding_key)?;

    let typ = jsonwebtoken::decode_header(&jwt.0)
        .map_err(ValidateJwtError::from)?
        .typ;
    if !typ.as_deref().is_some_and(is_status_list_typ) {
        return Err(UpdateStatusListError::UnexpectedTyp {
            url: status_list_url.to_string(),
            found: typ,
        });
    }

    let status_list_jwt: StatusListJwt = validated_jwt
        .try_into()
        .map_err(DecodeJwtError::DeserializeClaims)?;
    check_bound_to(&status_list_jwt, iss, status_list_url).map_err(|reason| {
        UpdateStatusListError::UnboundStatusList {
            url: status_list_url.to_string(),
            reason,
        }
    })?;
    Ok(ParsedStatusList {
        ttl_secs: status_list_jwt.ttl,
        list: status_list_jwt.into_cached(validator.leeway_secs())?,
    })
}

/// Waits the refresh interval, but no longer than until the cached list expires,
/// so a list is refreshed by its `exp` even without a `ttl`
/// ([draft-ietf-oauth-status-list-21 §13.7]).
///
/// [draft-ietf-oauth-status-list-21 §13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
fn next_refresh_in(
    refresh_interval: Duration,
    expiry: Option<StatusListExpiry>,
    now: u64,
) -> Duration {
    match expiry {
        Some(expiry) => refresh_interval.min(expiry.remaining(now).max(MIN_REFRESH_INTERVAL)),
        None => refresh_interval,
    }
}

/// Contains an `Arc<RwLock<_>>` internally so clone should be fine
#[derive(Debug, Default, Clone)]
pub(crate) struct StatusListCache {
    pub status_lists: Arc<RwLock<HashMap<StatusListUri, StatusListState>>>,
}

impl StatusListCache {
    /// Checks the status of a token whose `status` claim points at entry `idx` of
    /// the list at `uri`. Only VALID passes; every other outcome names the list
    /// and why no statement about the token can be made
    /// ([draft-ietf-oauth-status-list-21 §8.3]).
    ///
    /// [draft-ietf-oauth-status-list-21 §8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
    pub(crate) fn check_status(
        &self,
        uri: &str,
        idx: usize,
        now: u64,
    ) -> Result<(), StatusCheckError> {
        let lists = self
            .status_lists
            .read()
            .expect("obtain status list read lock");
        let list = match lists.get(uri) {
            Some(StatusListState::Usable(list)) => list,
            Some(StatusListState::Unavailable(unavailable)) => {
                return Err(StatusCheckError::Unavailable {
                    uri: uri.to_string(),
                    unavailable: unavailable.clone(),
                });
            },
            None => {
                return Err(StatusCheckError::UnknownStatusList {
                    uri: uri.to_string(),
                });
            },
        };
        let status = list.status_at(idx, now).map_err(|err| {
            let uri = uri.to_string();
            match err {
                StatusAtError::Expired(expiry) => StatusCheckError::Expired { uri, expiry },
                StatusAtError::Get(GetStatusError::IdxOutOfBounds { len }) => {
                    StatusCheckError::IdxOutOfBounds { uri, idx, len }
                },
                StatusAtError::Get(GetStatusError::UnsupportedBitSize) => {
                    StatusCheckError::UnsupportedBitSize { uri }
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

    /// Initializes the statuslist for the given issuer
    ///
    /// An issuer with no `status_list_endpoint`, including every static-form
    /// issuer, is skipped with a WARN log; its tokens carrying `status` are rejected.
    pub(crate) async fn init_for_iss(
        &self,
        iss_config: &IssuerConfig,
        args: InitForIssArgs<'_>,
    ) -> Result<(), UpdateStatusListError> {
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
            return Ok(());
        };
        let status_list_jwt = StatusListJwtStr::get_from_url(status_list_url, &http_client)
            .await
            .map_err(UpdateStatusListError::GetStatusListJwt)?;
        let iss = iss_config.policy.iss_claim().clone();

        let decoded_jwt = decode_jwt(&status_list_jwt.0)?;

        // Get decoding key
        let decoding_key_info = decoded_jwt.decoding_key_info();
        let decoding_key = key_service.get_key(&decoding_key_info);

        let decoded_jwt_iss = decoded_jwt.iss();
        // The response must not pick another trusted issuer's validator.
        if let Some(decoded) = decoded_jwt_iss.as_ref().filter(|decoded| **decoded != iss) {
            return Err(UpdateStatusListError::UnboundStatusList {
                url: status_list_url.to_string(),
                reason: UnboundReason::Iss {
                    found: decoded.clone(),
                },
            });
        }
        // get validator
        let validator_key = ValidatorInfo {
            iss: decoded_jwt_iss.as_ref(),
            token_kind: TokenKind::StatusList,
            algorithm: decoded_jwt.header.alg,
        };
        let validator =
            validators
                .get(&validator_key)
                .ok_or(UpdateStatusListError::MissingValidator(
                    status_list_url.to_string(),
                ))?;

        let parsed = parse_status_list_jwt(
            &status_list_jwt,
            &validator,
            decoding_key.clone(),
            &iss,
            status_list_url,
        )?;

        // Per the IETF `oauth-status-list` spec, the JWT's `ttl` claim drives the
        // refresh cadence, but the bootstrap
        // `CEDARLING_JWT_STATUS_LIST_REFRESH_INTERVAL_MAX` caps it: the issuer can
        // always request a *more frequent* refresh, but never a less frequent one.
        // When the JWT omits `ttl`, the max is used directly so the cache never goes
        // stale forever.
        let refresh_interval = effective_refresh_interval(parsed.ttl_secs, refresh_interval_max);
        let status_list = parsed.list;

        let ctx = StatusListUpdateCtx {
            refresh_interval,
            expiry: status_list.expiry(),
            refresh_interval_max,
            status_list_url: status_list_url.clone(),
            iss: iss.clone(),
            decoding_key,
            validator,
            status_lists: self.status_lists.clone(),
            logger,
            http_client: http_client.clone(),
            cancel_tkn,
        };
        crate::rt::spawn_task(keep_status_list_updated(
            // callback is called on updated status list
            move || {
                token_cache.invalidate_by_index(&IndexKey::Iss(iss.clone()));
            },
            ctx,
        ));

        {
            let mut status_lists = self
                .status_lists
                .write()
                .expect("acquire status_lists write lock");
            status_lists.insert(
                status_list_url.to_string(),
                StatusListState::Usable(status_list),
            );
        }

        Ok(())
    }
}

/// What the refresh task does with the cached list after a refresh failed.
#[derive(Debug, PartialEq)]
enum AfterFailedRefresh {
    /// The list has not expired, so it may still be relied upon
    /// ([draft-ietf-oauth-status-list-21 §13.7]): keep it and retry.
    ///
    /// [draft-ietf-oauth-status-list-21 §13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
    Keep(StatusListExpiry),
    /// The list has expired or has no `exp`: drop it, so the tokens that
    /// reference it are rejected.
    Drop,
}

fn after_failed_refresh(expiry: Option<StatusListExpiry>, now: u64) -> AfterFailedRefresh {
    match expiry.filter(|expiry| !expiry.has_passed(now)) {
        Some(expiry) => AfterFailedRefresh::Keep(expiry),
        None => AfterFailedRefresh::Drop,
    }
}

/// Records that the list at `uri` could not be refreshed, keeping the time it
/// first became unavailable. Returns whether a usable list was replaced, so the
/// caller knows to clear the tokens validated against it.
fn mark_unavailable(
    status_lists: &mut HashMap<StatusListUri, StatusListState>,
    uri: &str,
    now: u64,
    cause: Arc<UpdateStatusListError>,
) -> bool {
    let (unavailable, was_usable) = match status_lists.get(uri) {
        Some(StatusListState::Unavailable(unavailable)) => (unavailable.with_cause(cause), false),
        Some(StatusListState::Usable(_)) => (UnavailableStatusList::new(now, cause), true),
        None => (UnavailableStatusList::new(now, cause), false),
    };
    status_lists.insert(uri.to_string(), StatusListState::Unavailable(unavailable));
    was_usable
}

/// Fetches the list again and runs every check the first fetch ran.
async fn refetch_status_list(
    status_list_url: &Url,
    http_client: &HttpClient,
    validator: &JwtValidator,
    decoding_key: Option<Arc<DecodingKey>>,
    iss: &IssClaim,
) -> Result<ParsedStatusList, UpdateStatusListError> {
    let jwt = StatusListJwtStr::get_from_url(status_list_url, http_client)
        .await
        .map_err(UpdateStatusListError::GetStatusListJwt)?;
    parse_status_list_jwt(&jwt, validator, decoding_key, iss, status_list_url)
}

/// Keeps the status list from the given URL updated, refreshing it every
/// `refresh_interval` and no later than its expiry. `cb` clears the token cache
/// whenever the cached list changes or stops being usable.
async fn keep_status_list_updated<F>(cb: F, ctx: StatusListUpdateCtx)
where
    F: Fn(),
{
    let StatusListUpdateCtx {
        mut refresh_interval,
        mut expiry,
        refresh_interval_max,
        status_list_url,
        iss,
        decoding_key,
        validator,
        status_lists,
        logger,
        http_client,
        cancel_tkn,
    } = ctx;
    let url = status_list_url.as_str();

    loop {
        let wait = next_refresh_in(
            refresh_interval,
            expiry,
            jsonwebtoken::get_current_timestamp(),
        );
        tokio::select! {
            () = sleep(wait) => {},
            () = cancel_tkn.cancelled() => break,
        }

        // The validator already rejects tokens against the expired list; clearing
        // the token cache stops tokens validated before it expired from being
        // served while the refresh below runs.
        if let Some(expired) =
            expiry.filter(|expiry| expiry.has_passed(jsonwebtoken::get_current_timestamp()))
        {
            logger.log_any(JwtLogEntry::new(
                format!(
                    "the cached status list for '{url}' is past its {expired}; tokens that \
                     reference it will be rejected until it refreshes successfully",
                ),
                Some(LogLevel::WARN),
            ));
            cb();
        }

        let parsed = match refetch_status_list(
            &status_list_url,
            &http_client,
            &validator,
            decoding_key.clone(),
            &iss,
        )
        .await
        {
            Ok(parsed) => parsed,
            Err(err) => {
                let err = Arc::new(err);
                let now = jsonwebtoken::get_current_timestamp();
                let outcome = match after_failed_refresh(expiry, now) {
                    AfterFailedRefresh::Keep(expiry) => format!(
                        "the cached list stays in use until its {expiry}; next attempt in {} s",
                        next_refresh_in(refresh_interval, Some(expiry), now).as_secs(),
                    ),
                    AfterFailedRefresh::Drop => {
                        // the list is gone or never expires: later attempts follow
                        // the refresh interval
                        expiry = None;
                        let was_usable = mark_unavailable(
                            &mut status_lists.write().expect("obtain status list write lock"),
                            url,
                            now,
                            err.clone(),
                        );
                        if was_usable {
                            cb();
                        }
                        format!(
                            "no usable list is cached, so tokens that reference it are \
                             rejected until it refreshes successfully; next attempt in {} s",
                            refresh_interval.as_secs(),
                        )
                    },
                };
                logger.log_any(JwtLogEntry::new(
                    format!("failed to refresh the status list from '{url}': {err}; {outcome}"),
                    Some(LogLevel::ERROR),
                ));
                continue;
            },
        };
        // Honor the refreshed JWT's `ttl` but cap it at the configured max; if the
        // refreshed JWT no longer has `ttl`, use the max directly.
        refresh_interval = effective_refresh_interval(parsed.ttl_secs, refresh_interval_max);
        expiry = parsed.list.expiry();

        status_lists
            .write()
            .expect("obtain status list write lock")
            // insert, not update: re-creates the entry if a failed refresh dropped it
            .insert(url.to_string(), StatusListState::Usable(parsed.list));
        cb();
    }
}

#[cfg(test)]
impl StatusListCache {
    fn from_states(states: HashMap<StatusListUri, StatusListState>) -> Self {
        Self {
            status_lists: Arc::new(RwLock::new(states)),
        }
    }
}

/// Caches lists that never expire, for tests that check statuses only.
#[cfg(test)]
impl From<HashMap<String, super::StatusList>> for StatusListCache {
    fn from(status_lists: HashMap<String, super::StatusList>) -> Self {
        let status_lists = status_lists
            .into_iter()
            .map(|(uri, list)| (uri, StatusListState::Usable(list.into())))
            .collect();
        Self {
            status_lists: Arc::new(RwLock::new(status_lists)),
        }
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

    /// The list cached for `uri`, which the test expects to be usable.
    #[track_caller]
    fn usable_list(cache: &StatusListCache, uri: &str) -> StatusList {
        cache
            .status_lists
            .read()
            .expect("obtain status_lists read lock")
            .get(uri)
            .and_then(StatusListState::as_usable)
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
    /// the refresh waits the interval, cut short at the second after `exp` plus
    /// leeway, and never less than [`MIN_REFRESH_INTERVAL`].
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
            Duration::from_secs(71),
            "the list is usable through second 1070, so it is refreshed at 1071"
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
        assert_eq!(
            after_failed_refresh(Some(expiry), 1_060),
            AfterFailedRefresh::Keep(expiry),
            "the list is usable through `exp` plus leeway, so it is kept"
        );
        assert_eq!(
            after_failed_refresh(Some(expiry), 1_061),
            AfterFailedRefresh::Drop,
            "the list has expired, so it is dropped"
        );
        assert_eq!(
            after_failed_refresh(None, 1_000),
            AfterFailedRefresh::Drop,
            "a list without `exp` has no usable-until point, so it is dropped"
        );
    }

    /// A list that could not be refreshed stays marked unavailable from its first
    /// failure, with the latest cause, so the admin sees since when and why.
    #[test]
    fn unavailable_list_keeps_first_failure_time_and_latest_cause() {
        let uri = "https://idp.example.com/statuslist/1";
        let usable = StatusListState::Usable(
            StatusList {
                bit_size: 1u8.try_into().expect("valid bit size"),
                list: vec![0],
            }
            .into(),
        );
        let cache = StatusListCache::from_states(HashMap::from([(uri.to_string(), usable)]));

        let replaced_usable = mark_unavailable(
            &mut cache.status_lists.write().expect("write lock"),
            uri,
            1_767_225_600,
            Arc::new(UpdateStatusListError::MissingValidator("first".into())),
        );
        assert!(
            replaced_usable,
            "the usable list was replaced, so the token cache must be cleared"
        );
        let replaced_usable = mark_unavailable(
            &mut cache.status_lists.write().expect("write lock"),
            uri,
            1_767_225_900,
            Arc::new(UpdateStatusListError::MissingValidator("second".into())),
        );
        assert!(
            !replaced_usable,
            "the list was already unavailable, nothing to clear"
        );

        let err = cache
            .check_status(uri, 0, 1_767_226_000)
            .expect_err("an unavailable list gives no statement about the token");
        assert_eq!(
            err.to_string(),
            "the status list 'https://idp.example.com/statuslist/1' has been unavailable since \
             2026-01-01T00:00:00Z: missing validator for the statuslist JWT from 'second'; \
             tokens that reference it are rejected until a refresh succeeds",
            "the rejection names the list, the first failure time and the latest cause"
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
        let status_list = StatusListCache::default();

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
        validators.init_for_iss(
            &iss_config,
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: false,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
                ..Default::default()
            },
            &status_list,
            None,
        );
        status_list
            .init_for_iss(
                &iss_config,
                InitForIssArgs {
                    validators: &validators,
                    key_service: &key_service,
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
            .unwrap();

        let status_list_uri = mock_server.status_list_endpoint().unwrap().to_string();

        // Check if the status list is the same as we first generated
        assert_eq!(
            usable_list(&status_list, &status_list_uri),
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
            usable_list(&status_list, &status_list_uri),
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
        let status_list = StatusListCache::default();

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
        validators.init_for_iss(
            &iss_config,
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: false,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
                ..Default::default()
            },
            &status_list,
            None,
        );
        // 1-second max so the test runs fast
        status_list
            .init_for_iss(
                &iss_config,
                InitForIssArgs {
                    validators: &validators,
                    key_service: &key_service,
                    token_cache: TokenCache::default(),
                    logger: None,
                    http_client,
                    refresh_interval_max: Duration::from_millis(200),
                    cancel_tkn: CancellationToken::new(),
                },
            )
            .await
            .unwrap();

        let status_list_uri = mock_server.status_list_endpoint().unwrap().to_string();

        // Update server-side list (still without ttl) before refresh fires
        mock_server
            .generate_status_list_endpoint_without_ttl(1u8.try_into().unwrap(), &[0b0000_0001]);

        tokio::time::sleep(Duration::from_millis(600)).await;

        assert_eq!(
            usable_list(&status_list, &status_list_uri),
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
        let status_list = StatusListCache::default();

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
        validators.init_for_iss(
            &iss_config,
            &JwtConfig {
                jwks: None,
                jwt_sig_validation: false,
                jwt_status_validation: true,
                signature_algorithms_supported: HashSet::from([Algorithm::HS256]),
                ..Default::default()
            },
            &status_list,
            None,
        );
        status_list
            .init_for_iss(
                &iss_config,
                InitForIssArgs {
                    validators: &validators,
                    key_service: &key_service,
                    token_cache: TokenCache::default(),
                    logger: None,
                    http_client,
                    refresh_interval_max: Duration::from_millis(200),
                    cancel_tkn: CancellationToken::new(),
                },
            )
            .await
            .unwrap();

        let status_list_uri = mock_server.status_list_endpoint().unwrap().to_string();

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
            usable_list(&status_list, &status_list_uri),
            StatusList {
                bit_size: 1u8.try_into().unwrap(),
                list: vec![0b0000_0001],
            },
            "refresh did not respect the configured cap over the JWT ttl",
        );
    }
}
