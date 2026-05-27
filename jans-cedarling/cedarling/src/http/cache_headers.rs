// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Parses HTTP cache validation headers used by the policy-store refresh worker.
//!
//! Recognized headers (RFC 7234 / RFC 9111):
//! - `Cache-Control` — `max-age=N`, `no-cache`, `no-store`
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
pub(crate) struct CacheValidators {
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
    /// `Cache-Control: no-store` was present — caller should not retain the body
    /// across restarts (we still keep it in-memory; this flag is informational).
    pub no_store: bool,
}

impl CacheValidators {
    /// Parse a [`HeaderMap`] into [`CacheValidators`].
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

        let (cc_max_age, no_cache, no_store) = headers
            .get(reqwest::header::CACHE_CONTROL)
            .and_then(|v| v.to_str().ok())
            .map_or((None, false, false), parse_cache_control);

        // Cache-Control: max-age wins over Expires when both are present.
        let mut fresh_for = if let Some(secs) = cc_max_age {
            Some(Duration::from_secs(secs))
        } else if let Some(expires_at) = headers
            .get(reqwest::header::EXPIRES)
            .and_then(|v| v.to_str().ok())
            .and_then(parse_http_date)
        {
            let delta = expires_at.signed_duration_since(now);
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
            no_store,
        }
    }

    /// True if any conditional-request validator (`ETag` or `Last-Modified`) is
    /// present. Only used by tests today; production code calls
    /// [`HttpClient::get_bytes_conditional`] which inspects the fields directly.
    #[cfg(test)]
    pub(crate) fn has_validator(&self) -> bool {
        self.etag.is_some() || self.last_modified.is_some()
    }
}

/// Parse a `Cache-Control` header value into (`max_age`, `no_cache`, `no_store`).
/// Tolerant: malformed directives are skipped.
fn parse_cache_control(value: &str) -> (Option<u64>, bool, bool) {
    let mut max_age: Option<u64> = None;
    let mut no_cache = false;
    let mut no_store = false;

    for raw in value.split(',') {
        let directive = raw.trim();
        if directive.is_empty() {
            continue;
        }
        // Directives are case-insensitive per RFC 9111
        let lower = directive.to_ascii_lowercase();
        if lower == "no-cache" {
            no_cache = true;
        } else if lower == "no-store" {
            no_store = true;
        } else if let Some(rest) = lower.strip_prefix("max-age=") {
            // Strip optional quotes; ignore unparseable / negative values.
            let trimmed = rest.trim().trim_matches('"');
            if let Ok(secs) = trimmed.parse::<u64>() {
                max_age = Some(secs);
            }
        }
    }

    (max_age, no_cache, no_store)
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
        let v = CacheValidators::from_headers(&HeaderMap::new(), t0());
        assert_eq!(v, CacheValidators::default());
        assert!(!v.has_validator());
    }

    #[test]
    fn etag_and_last_modified_are_captured() {
        let h = headers(&[
            ("etag", "\"abc123\""),
            ("last-modified", "Sun, 06 Nov 1994 08:49:37 GMT"),
        ]);
        let v = CacheValidators::from_headers(&h, t0());
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
        let v = CacheValidators::from_headers(&h, t0());
        assert_eq!(v.etag.as_deref(), Some("W/\"weak-tag\""));
    }

    #[test]
    fn cache_control_max_age_overrides_expires() {
        let h = headers(&[
            ("cache-control", "max-age=600"),
            ("expires", "Sun, 22 May 2027 12:00:00 GMT"),
        ]);
        let v = CacheValidators::from_headers(&h, t0());
        assert_eq!(v.fresh_for, Some(Duration::from_secs(600)));
    }

    #[test]
    fn cache_control_no_cache_zeros_freshness() {
        let h = headers(&[("cache-control", "max-age=600, no-cache")]);
        let v = CacheValidators::from_headers(&h, t0());
        assert!(v.no_cache);
        assert_eq!(v.fresh_for, Some(Duration::ZERO));
    }

    #[test]
    fn cache_control_no_store_flag() {
        let h = headers(&[("cache-control", "no-store")]);
        let v = CacheValidators::from_headers(&h, t0());
        assert!(v.no_store);
    }

    #[test]
    fn expires_in_future_yields_positive_fresh_for() {
        let h = headers(&[("expires", "Fri, 22 May 2026 13:00:00 GMT")]);
        let v = CacheValidators::from_headers(&h, t0());
        assert_eq!(v.fresh_for, Some(Duration::from_secs(3600)));
    }

    #[test]
    fn expires_in_past_yields_zero_fresh_for() {
        let h = headers(&[("expires", "Mon, 01 Jan 1990 00:00:00 GMT")]);
        let v = CacheValidators::from_headers(&h, t0());
        assert_eq!(v.fresh_for, Some(Duration::ZERO));
    }

    #[test]
    fn malformed_max_age_is_ignored() {
        let h = headers(&[("cache-control", "max-age=banana")]);
        let v = CacheValidators::from_headers(&h, t0());
        assert_eq!(v.fresh_for, None);
    }

    #[test]
    fn negative_max_age_is_ignored() {
        let h = headers(&[("cache-control", "max-age=-30")]);
        let v = CacheValidators::from_headers(&h, t0());
        // u64::parse rejects "-30" so this is treated as absent.
        assert_eq!(v.fresh_for, None);
    }

    #[test]
    fn malformed_expires_is_ignored() {
        let h = headers(&[("expires", "not a date")]);
        let v = CacheValidators::from_headers(&h, t0());
        assert_eq!(v.fresh_for, None);
    }

    #[test]
    fn directives_are_case_insensitive() {
        let h = headers(&[("cache-control", "MAX-AGE=42, NO-CACHE")]);
        let v = CacheValidators::from_headers(&h, t0());
        assert!(v.no_cache);
        // no-cache zeroes the freshness even when max-age was set.
        assert_eq!(v.fresh_for, Some(Duration::ZERO));
    }

    #[test]
    fn empty_directive_segments_skipped() {
        let h = headers(&[("cache-control", ",,max-age=15,,")]);
        let v = CacheValidators::from_headers(&h, t0());
        assert_eq!(v.fresh_for, Some(Duration::from_secs(15)));
    }

    #[test]
    fn whitespace_only_etag_is_treated_as_absent() {
        let h = headers(&[("etag", "   ")]);
        let v = CacheValidators::from_headers(&h, t0());
        assert!(v.etag.is_none());
        assert!(!v.has_validator());
    }

    #[test]
    fn rfc850_date_format_parsed() {
        // chrono parses with a 2-digit year extended to 19xx.
        let h = headers(&[("expires", "Sunday, 22-May-94 12:00:00 GMT")]);
        let v = CacheValidators::from_headers(&h, t0());
        // In the past, so fresh_for is zero (not None).
        assert_eq!(v.fresh_for, Some(Duration::ZERO));
    }
}
