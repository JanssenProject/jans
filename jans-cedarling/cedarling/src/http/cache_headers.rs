// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Parses HTTP cache validation headers used by the policy-store refresh worker.
//!
//! Recognized headers (RFC 7234 / RFC 9111):
//! - `Cache-Control` — `max-age=N`, `no-cache`, `no-store` (both directives
//!   force the refresh worker's in-memory freshness window to zero so the
//!   next tick revalidates against the upstream)
//! - `Expires` — RFC 7231 IMF-fixdate
//! - `ETag` — opaque validator quoted-string
//! - `Last-Modified` — RFC 7231 IMF-fixdate (stored verbatim for echoing back)
//!
//! All malformed values are treated as absent. The worker never panics on a
//! surprising header value — it falls back to its configured interval.

use chrono::{DateTime, Utc};
use reqwest::header::HeaderMap;
use std::time::Duration;

/// Cache state extracted from a single HTTP response.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub(crate) struct CacheHeadersState {
    /// Strong or weak `ETag`, including the quote marks and any `W/` prefix.
    pub etag: Option<String>,
    /// `Last-Modified` value as the server sent it. Stored verbatim for echoing
    /// in a subsequent `If-Modified-Since` request.
    pub last_modified: Option<String>,
    /// How long the response is fresh according to the server, after the
    /// `Date` of the response. Derived from `Cache-Control: max-age` (preferred)
    /// or `Expires` minus the current time.
    pub fresh_for: Option<Duration>,
    /// `Cache-Control: no-cache` was present — caller should always revalidate.
    pub no_cache: bool,
}

impl CacheHeadersState {
    /// Parse a [`HeaderMap`] into [`CacheHeadersState`].
    ///
    /// `now` is injected so callers can test deterministically. In production
    /// pass [`Utc::now()`].
    pub(crate) fn from_headers(headers: &HeaderMap, now: DateTime<Utc>) -> Self {
        let etag = headers
            .get(reqwest::header::ETAG)
            .and_then(|v| v.to_str().ok())
            .map(str::trim)
            .filter(|s| !s.is_empty())
            .map(str::to_owned);

        let last_modified = headers
            .get(reqwest::header::LAST_MODIFIED)
            .and_then(|v| v.to_str().ok())
            .map(str::trim)
            .filter(|s| !s.is_empty())
            .map(str::to_owned);

        // RFC 9110 §5.3: a recipient MUST handle multiple field-values for the
        // same header (sent either as one comma-separated line or as separate
        // header lines). Fold across all `Cache-Control` field-values so a
        // directive on the second line still gets honored — `headers.get(...)`
        // alone would silently drop them.
        let (cc_max_age, no_cache) = headers.get_all(reqwest::header::CACHE_CONTROL).iter().fold(
            (None::<u64>, false),
            |(prev_max_age, prev_no_cache), v| {
                let Some(s) = v.to_str().ok() else {
                    return (prev_max_age, prev_no_cache);
                };
                let (this_max_age, this_no_cache) = parse_cache_control(s);
                // `no-cache` is sticky — any field-value carrying it wins.
                // For `max-age`, the most-restrictive (smallest) value wins:
                // RFC 9111 §5.2.1.1 lets caches use any value but the
                // safer choice is the minimum.
                let merged_max_age = match (prev_max_age, this_max_age) {
                    (Some(a), Some(b)) => Some(a.min(b)),
                    (Some(a), None) => Some(a),
                    (None, b) => b,
                };
                (merged_max_age, prev_no_cache || this_no_cache)
            },
        );

        // Cache-Control: max-age wins over Expires when both are present.
        let mut fresh_for = if let Some(secs) = cc_max_age {
            Some(Duration::from_secs(secs))
        } else if let Some(expires_at) = headers
            .get(reqwest::header::EXPIRES)
            .and_then(|v| v.to_str().ok())
            .and_then(parse_http_date)
        {
            // RFC 9111 §5.3: freshness lifetime from `Expires` is computed
            // against the response's `Date` header — not the client's wall
            // clock — so clock skew between client and origin doesn't
            // (mis)classify a fresh response as stale or vice versa. Fall back
            // to `now` only when the server omitted `Date` (allowed by RFC
            // 7231 §7.1.1.2 for origins with no reliable clock).
            let reference_time = headers
                .get(reqwest::header::DATE)
                .and_then(|v| v.to_str().ok())
                .and_then(parse_http_date)
                .unwrap_or(now);
            let delta = expires_at.signed_duration_since(reference_time);
            // Negative or zero ⇒ already stale, treat as fresh_for=0 so the
            // worker will revalidate immediately on its next tick.
            Some(if delta.num_seconds() > 0 {
                Duration::from_secs(delta.num_seconds().cast_unsigned())
            } else {
                Duration::ZERO
            })
        } else {
            None
        };

        // no-cache forces revalidation regardless of any max-age value.
        if no_cache {
            fresh_for = Some(Duration::ZERO);
        }

        Self {
            etag,
            last_modified,
            fresh_for,
            no_cache,
        }
    }

    /// True if any conditional-request validator (`ETag` or `Last-Modified`) is
    /// present. Used by the HEAD-then-GET refresh strategy to decide whether
    /// HEAD added enough information to skip the body fetch.
    pub(crate) fn has_validator(&self) -> bool {
        self.etag.is_some() || self.last_modified.is_some()
    }

    /// Apply cache headers from a fresh response (typically a `304`) to
    /// `self`. Field semantics differ on purpose:
    ///
    /// - **Validators** (`etag`, `last_modified`) identify the *resource
    ///   version*. Per RFC 9110 §15.4.5 a `304` only carries them when
    ///   refreshed; an omitted field must not erase our cached value, or the
    ///   next conditional GET would have nothing to send.
    ///
    /// - **`fresh_for`** is a *per-response* property — "this specific
    ///   response is fresh for N seconds". A new response that omits
    ///   `Cache-Control` / `Expires` is making no freshness claim, and
    ///   carrying forward an old value would pin the worker cadence to a
    ///   long-ago `max-age` indefinitely. We always overwrite, including
    ///   with `None`.
    ///
    /// - **`no_cache`** is a sticky directive: once a response asks for
    ///   revalidation, a later response that omits the directive does not
    ///   automatically unset it.
    pub(crate) fn merge_from(&mut self, other: Self) {
        if other.etag.is_some() {
            self.etag = other.etag;
        }
        if other.last_modified.is_some() {
            self.last_modified = other.last_modified;
        }
        // Per-response, not per-resource — overwrite unconditionally (a `None`
        // means "no freshness claim this time", which the worker should honor
        // rather than pretending an earlier max-age still applies).
        self.fresh_for = other.fresh_for;
        if other.no_cache {
            self.no_cache = true;
        }
    }
}

/// Parse a `Cache-Control` header value into (`max_age`, `no_cache`).
/// Tolerant: malformed directives are skipped. `no-store` is folded into the
/// `no_cache` flag: although the directive's RFC 9111 meaning is "don't
/// persist," a server emitting it is explicitly asking caches to revalidate
/// instead of using stored values — so we treat the in-memory freshness
/// window as zero and let the next tick re-check.
fn parse_cache_control(value: &str) -> (Option<u64>, bool) {
    let mut max_age: Option<u64> = None;
    let mut no_cache = false;

    for raw in value.split(',') {
        let directive = raw.trim();
        if directive.is_empty() {
            continue;
        }
        // Directives are case-insensitive per RFC 9111
        let lower = directive.to_ascii_lowercase();
        if lower == "no-cache" || lower == "no-store" {
            no_cache = true;
        } else if let Some(rest) = lower.strip_prefix("max-age=") {
            // Strip optional quotes; ignore unparseable / negative values.
            let trimmed = rest.trim().trim_matches('"');
            if let Ok(secs) = trimmed.parse::<u64>() {
                max_age = Some(secs);
            }
        }
    }

    (max_age, no_cache)
}

/// Parse an HTTP date (RFC 7231 IMF-fixdate, RFC 850, or ANSI C `asctime`).
/// Returns `None` on any parse failure.
fn parse_http_date(value: &str) -> Option<DateTime<Utc>> {
    let trimmed = value.trim();
    // Try in order: RFC 2822 (close to IMF-fixdate), RFC 3339, then a couple of
    // explicit format strings for the alternates.
    if let Ok(dt) = DateTime::parse_from_rfc2822(trimmed) {
        return Some(dt.with_timezone(&Utc));
    }
    if let Ok(dt) = DateTime::parse_from_rfc3339(trimmed) {
        return Some(dt.with_timezone(&Utc));
    }
    // RFC 850, e.g. "Sunday, 06-Nov-94 08:49:37 GMT"
    if let Ok(naive) = chrono::NaiveDateTime::parse_from_str(trimmed, "%A, %d-%b-%y %H:%M:%S GMT") {
        return Some(DateTime::<Utc>::from_naive_utc_and_offset(naive, Utc));
    }
    // ANSI C asctime(), e.g. "Sun Nov  6 08:49:37 1994"
    if let Ok(naive) = chrono::NaiveDateTime::parse_from_str(trimmed, "%a %b %e %H:%M:%S %Y") {
        return Some(DateTime::<Utc>::from_naive_utc_and_offset(naive, Utc));
    }
    None
}

#[cfg(test)]
mod tests {
    use super::*;
    use reqwest::header::{HeaderMap, HeaderName, HeaderValue};
    use std::str::FromStr;

    fn headers(pairs: &[(&str, &str)]) -> HeaderMap {
        let mut h = HeaderMap::new();
        for (k, v) in pairs {
            h.append(
                HeaderName::from_str(k).unwrap(),
                HeaderValue::from_str(v).unwrap(),
            );
        }
        h
    }

    fn t0() -> DateTime<Utc> {
        DateTime::parse_from_rfc3339("2026-05-22T12:00:00Z")
            .unwrap()
            .with_timezone(&Utc)
    }

    #[test]
    fn empty_headers_produce_empty_validators() {
        let v = CacheHeadersState::from_headers(&HeaderMap::new(), t0());
        assert_eq!(v, CacheHeadersState::default());
        assert!(!v.has_validator());
    }

    #[test]
    fn etag_and_last_modified_are_captured() {
        let h = headers(&[
            ("etag", "\"abc123\""),
            ("last-modified", "Sun, 06 Nov 1994 08:49:37 GMT"),
        ]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert_eq!(v.etag.as_deref(), Some("\"abc123\""));
        assert_eq!(
            v.last_modified.as_deref(),
            Some("Sun, 06 Nov 1994 08:49:37 GMT")
        );
        assert!(v.has_validator());
    }

    #[test]
    fn weak_etag_preserved_verbatim() {
        let h = headers(&[("etag", "W/\"weak-tag\"")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert_eq!(v.etag.as_deref(), Some("W/\"weak-tag\""));
    }

    #[test]
    fn cache_control_max_age_overrides_expires() {
        let h = headers(&[
            ("cache-control", "max-age=600"),
            ("expires", "Sun, 22 May 2027 12:00:00 GMT"),
        ]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert_eq!(v.fresh_for, Some(Duration::from_secs(600)));
    }

    #[test]
    fn cache_control_no_cache_zeros_freshness() {
        let h = headers(&[("cache-control", "max-age=600, no-cache")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert!(v.no_cache);
        assert_eq!(v.fresh_for, Some(Duration::ZERO));
    }

    #[test]
    fn cache_control_no_store_zeros_freshness_alongside_max_age() {
        // A server emitting `no-store` is asking caches to revalidate instead
        // of using stored values; even though we never persist to disk, we
        // still respect the directive by zeroing the in-memory freshness
        // window so the next tick re-checks the upstream.
        let h = headers(&[("cache-control", "no-store, max-age=600")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert!(v.no_cache, "no-store must set no_cache so revalidation fires");
        assert_eq!(
            v.fresh_for,
            Some(Duration::ZERO),
            "no-store must zero freshness even when max-age is present",
        );
    }

    #[test]
    fn cache_control_split_across_multiple_header_lines_is_honored() {
        // RFC 9110 §5.3 permits sending multiple field-values either as one
        // comma-separated line or as separate header lines. We must honor
        // directives from every line — `headers.get(...).single()` would
        // silently drop the second one.
        let h = headers(&[
            ("cache-control", "max-age=600"),
            ("cache-control", "no-store"),
        ]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert!(
            v.no_cache,
            "no-store on a second Cache-Control header line must set no_cache",
        );
        assert_eq!(
            v.fresh_for,
            Some(Duration::ZERO),
            "no-store from any field-value must force zero freshness",
        );
    }

    #[test]
    fn cache_control_min_max_age_wins_across_lines() {
        // When multiple Cache-Control lines each carry a max-age, the
        // smaller wins — RFC 9111 §5.2.1.1 lets caches pick any but the
        // safer choice is the most-restrictive.
        let h = headers(&[
            ("cache-control", "max-age=600"),
            ("cache-control", "max-age=60"),
        ]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert_eq!(
            v.fresh_for,
            Some(Duration::from_secs(60)),
            "smallest max-age across lines must win, got {:?}",
            v.fresh_for,
        );
    }

    #[test]
    fn merge_from_preserves_prior_validators_when_new_is_empty() {
        // Real 304 case: server replies "Cache-Control: max-age=120" and
        // omits ETag / Last-Modified. We must keep the prior validators so
        // the next conditional GET still has something to send — otherwise
        // we'd fall back to an unconditional fetch.
        let mut prior = CacheHeadersState {
            etag: Some("\"v1\"".to_string()),
            last_modified: Some("Mon, 01 Jan 2024 00:00:00 GMT".to_string()),
            fresh_for: Some(Duration::from_secs(60)),
            no_cache: false,
        };
        let incoming = CacheHeadersState {
            fresh_for: Some(Duration::from_secs(120)),
            ..CacheHeadersState::default()
        };
        prior.merge_from(incoming);
        assert_eq!(
            prior.etag.as_deref(),
            Some("\"v1\""),
            "ETag must survive a 304 that omits the field",
        );
        assert_eq!(
            prior.last_modified.as_deref(),
            Some("Mon, 01 Jan 2024 00:00:00 GMT"),
            "Last-Modified must survive a 304 that omits the field",
        );
        assert_eq!(
            prior.fresh_for,
            Some(Duration::from_secs(120)),
            "fresh_for present in the new response must overwrite the prior value",
        );
    }

    #[test]
    fn merge_from_clears_stale_fresh_for_when_new_response_omits_it() {
        // Regression: prior to this fix `merge_from` kept the old `fresh_for`
        // when the new response omitted Cache-Control / Expires, pinning the
        // worker cadence to a long-ago max-age. Concrete scenario the
        // reviewer flagged: bootstrap response sent `Cache-Control:
        // max-age=10`, operator set `REFRESH_INTERVAL=3600`, every later 304
        // omits Cache-Control. Without overwrite-with-None semantics,
        // `next_delay`'s `min(server_fresh, base)` would keep choosing 10s
        // forever — ~360× more upstream traffic than configured.
        let mut prior = CacheHeadersState {
            etag: Some("\"v1\"".to_string()),
            last_modified: Some("Mon".to_string()),
            fresh_for: Some(Duration::from_secs(10)),
            no_cache: false,
        };
        let incoming = CacheHeadersState {
            // No Cache-Control / Expires on this response — only validators.
            etag: Some("\"v1\"".to_string()),
            ..CacheHeadersState::default()
        };
        prior.merge_from(incoming);
        assert_eq!(
            prior.fresh_for, None,
            "fresh_for must clear when the new response makes no freshness claim — otherwise the old per-response max-age locks the worker cadence indefinitely",
        );
        // Validators must still survive — only fresh_for changes semantics.
        assert_eq!(
            prior.etag.as_deref(),
            Some("\"v1\""),
            "validators must remain sticky even when fresh_for clears",
        );
        assert_eq!(
            prior.last_modified.as_deref(),
            Some("Mon"),
            "Last-Modified must remain sticky even when fresh_for clears",
        );
    }

    #[test]
    fn merge_from_overwrites_when_new_provides_value() {
        // 304 with refreshed ETag (server rotated the validator).
        let mut prior = CacheHeadersState {
            etag: Some("\"v1\"".to_string()),
            ..CacheHeadersState::default()
        };
        let incoming = CacheHeadersState {
            etag: Some("\"v2\"".to_string()),
            ..CacheHeadersState::default()
        };
        prior.merge_from(incoming);
        assert_eq!(
            prior.etag.as_deref(),
            Some("\"v2\""),
            "new ETag must replace the prior one when present",
        );
    }

    #[test]
    fn merge_from_no_cache_is_sticky_within_a_session() {
        // If a prior response set no_cache, a subsequent response that omits
        // the directive must not clear the flag — clearing it would resume
        // honoring stale `max-age` until the next no-cache response.
        let mut prior = CacheHeadersState {
            no_cache: true,
            ..CacheHeadersState::default()
        };
        let incoming = CacheHeadersState {
            fresh_for: Some(Duration::from_secs(60)),
            ..CacheHeadersState::default()
        };
        prior.merge_from(incoming);
        assert!(
            prior.no_cache,
            "no_cache must be sticky — omission in a later response does not unset it",
        );
    }

    #[test]
    fn expires_in_future_yields_positive_fresh_for() {
        let h = headers(&[("expires", "Fri, 22 May 2026 13:00:00 GMT")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert_eq!(v.fresh_for, Some(Duration::from_secs(3600)));
    }

    #[test]
    fn expires_in_past_yields_zero_fresh_for() {
        let h = headers(&[("expires", "Mon, 01 Jan 1990 00:00:00 GMT")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert_eq!(v.fresh_for, Some(Duration::ZERO));
    }

    #[test]
    fn expires_freshness_uses_response_date_when_present_per_rfc_9111() {
        // Server: Date=12:00, Expires=12:30 → freshness = 30 minutes
        // Client wall clock: t0()=12:00 here (matches origin), but the
        // assertion holds regardless of skew because we anchor on `Date`.
        let h = headers(&[
            ("date", "Fri, 22 May 2026 12:00:00 GMT"),
            ("expires", "Fri, 22 May 2026 12:30:00 GMT"),
        ]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert_eq!(
            v.fresh_for,
            Some(Duration::from_secs(1800)),
            "freshness must be Expires - Date (RFC 9111 §5.3), independent of client clock",
        );
    }

    #[test]
    fn expires_freshness_immune_to_client_clock_skew() {
        // Server-side timeline: Date=12:00, Expires=12:30 → 30 min freshness.
        // Client's clock is 10 minutes behind the origin (11:50). Without the
        // RFC 9111 fix, we'd compute Expires-now = 40 minutes (overestimated)
        // and the worker would refresh too late. With Date-anchored
        // arithmetic, we get the correct 30 minutes regardless.
        use chrono::TimeZone;
        let client_now_skewed = Utc.with_ymd_and_hms(2026, 5, 22, 11, 50, 0).unwrap();
        let h = headers(&[
            ("date", "Fri, 22 May 2026 12:00:00 GMT"),
            ("expires", "Fri, 22 May 2026 12:30:00 GMT"),
        ]);
        let v = CacheHeadersState::from_headers(&h, client_now_skewed);
        assert_eq!(
            v.fresh_for,
            Some(Duration::from_secs(1800)),
            "Date-anchored freshness must ignore client clock skew",
        );
    }

    #[test]
    fn expires_falls_back_to_client_now_when_date_header_missing() {
        // RFC 7231 §7.1.1.2 lets origins skip the Date header when they have
        // no reliable clock. In that case we fall back to the client's wall
        // clock — best effort.
        let h = headers(&[("expires", "Fri, 22 May 2026 13:00:00 GMT")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert_eq!(
            v.fresh_for,
            Some(Duration::from_secs(3600)),
            "with no Date header, fall back to client `now`",
        );
    }

    #[test]
    fn malformed_max_age_is_ignored() {
        let h = headers(&[("cache-control", "max-age=banana")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert_eq!(v.fresh_for, None);
    }

    #[test]
    fn negative_max_age_is_ignored() {
        let h = headers(&[("cache-control", "max-age=-30")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        // u64::parse rejects "-30" so this is treated as absent.
        assert_eq!(v.fresh_for, None);
    }

    #[test]
    fn malformed_expires_is_ignored() {
        let h = headers(&[("expires", "not a date")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert_eq!(v.fresh_for, None);
    }

    #[test]
    fn directives_are_case_insensitive() {
        let h = headers(&[("cache-control", "MAX-AGE=42, NO-CACHE")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert!(v.no_cache);
        // no-cache zeroes the freshness even when max-age was set.
        assert_eq!(v.fresh_for, Some(Duration::ZERO));
    }

    #[test]
    fn empty_directive_segments_skipped() {
        let h = headers(&[("cache-control", ",,max-age=15,,")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert_eq!(v.fresh_for, Some(Duration::from_secs(15)));
    }

    #[test]
    fn whitespace_only_etag_is_treated_as_absent() {
        let h = headers(&[("etag", "   ")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        assert!(v.etag.is_none());
        assert!(!v.has_validator());
    }

    #[test]
    fn rfc850_date_format_parsed() {
        // chrono parses with a 2-digit year extended to 19xx.
        let h = headers(&[("expires", "Sunday, 22-May-94 12:00:00 GMT")]);
        let v = CacheHeadersState::from_headers(&h, t0());
        // In the past, so fresh_for is zero (not None).
        assert_eq!(v.fresh_for, Some(Duration::ZERO));
    }
}
