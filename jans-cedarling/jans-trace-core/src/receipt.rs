// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Lock receipt chain, profile `lock-trace-receipt-v1` (core design §7). Lock assigns the
//! sequence, time and chain head; this module only checks and hashes what Lock supplies.

use serde_json::{Map, Value};
use thiserror::Error;

use crate::json::{JsonError, canonicalize_value};
use crate::{Digest, Limits};

pub const RECEIPT_PROFILE: &str = "tag:jans.io,2026:lock-trace-receipt-v1";

/// Assertion profile of Jans TRACE records (core design §15). The crate's outputs do not
/// depend on it; it is exported so producers and Lock spell it one way.
pub const ASSERTION_PROFILE: &str = "tag:jans.io,2026:trace-v1";

/// Largest integer JCS reproduces exactly (numbers are IEEE-754 doubles).
const MAX_SAFE_INTEGER: u64 = (1 << 53) - 1;

/// Last millisecond the `YYYY-…` format can express: 9999-12-31T23:59:59.999Z.
const MAX_RECEIVED_AT_MILLIS: u64 = 253_402_300_799_999;

const MILLIS_PER_DAY: u64 = 86_400_000;

/// The closed v1 field set hashed into a receipt (core design §7.1).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct ReceiptCommitment<'a> {
    pub receipt_profile: &'a str,
    pub evidence_domain_id: &'a str,
    pub receipt_sequence: u64,
    pub received_at: &'a str,
    pub producer_id: &'a str,
    pub record_id: &'a str,
    pub content_digest: &'a Digest,
    pub prev_receipt_hash: &'a Digest,
}

/// Which v1 rule a receipt breaks. All share the code `INVALID_RECEIPT`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
pub enum InvalidReceiptError {
    #[error("receipt profile is not lock-trace-receipt-v1")]
    UnknownProfile,
    #[error("receipt sequence is outside 1..=2^53-1")]
    Sequence,
    #[error("received_at is not an RFC 3339 UTC timestamp with milliseconds")]
    ReceivedAt,
    /// Sequence 1 must name the genesis sentinel, and only sequence 1 may.
    #[error("genesis sentinel does not match the receipt sequence")]
    Genesis,
}

impl InvalidReceiptError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        "INVALID_RECEIPT"
    }
}

/// Why `compute_receipt_hash` produced no hash.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
pub enum ReceiptError {
    #[error(transparent)]
    Invalid(#[from] InvalidReceiptError),
    /// Only if the JCS library refused validated strings and a safe integer; kept instead
    /// of a panic.
    #[error(transparent)]
    Json(#[from] JsonError),
}

impl ReceiptError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::Invalid(e) => e.code(),
            Self::Json(e) => e.code(),
        }
    }
}

/// Why `verify_receipt_link` rejected the receipt.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
pub enum ReceiptLinkError {
    #[error(transparent)]
    Invalid(#[from] InvalidReceiptError),
    #[error(transparent)]
    Json(#[from] JsonError),
    #[error("receipt does not name the expected predecessor")]
    LinkMismatch,
}

impl ReceiptLinkError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::Invalid(e) => e.code(),
            Self::Json(e) => e.code(),
            Self::LinkMismatch => "RECEIPT_LINK_MISMATCH",
        }
    }
}

impl From<ReceiptError> for ReceiptLinkError {
    fn from(error: ReceiptError) -> Self {
        match error {
            ReceiptError::Invalid(e) => Self::Invalid(e),
            ReceiptError::Json(e) => Self::Json(e),
        }
    }
}

/// Time beyond 9999-12-31T23:59:59.999Z, which the receipt format cannot express.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("time is beyond what the receipt timestamp format can express")]
pub struct FormatReceivedAtError;

impl FormatReceivedAtError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        "INVALID_RECEIPT"
    }
}

/// Hash of one receipt; the hash is not part of its own input (core design §7.2).
pub fn compute_receipt_hash(receipt: &ReceiptCommitment<'_>) -> Result<Digest, ReceiptError> {
    validate(receipt)?;
    let text = |value: &str| Value::String(value.to_owned());
    let mut commitment = Map::new();
    commitment.insert("receipt_profile".to_owned(), text(receipt.receipt_profile));
    commitment.insert(
        "evidence_domain_id".to_owned(),
        text(receipt.evidence_domain_id),
    );
    commitment.insert(
        "receipt_sequence".to_owned(),
        Value::from(receipt.receipt_sequence),
    );
    commitment.insert("received_at".to_owned(), text(receipt.received_at));
    commitment.insert("producer_id".to_owned(), text(receipt.producer_id));
    commitment.insert("record_id".to_owned(), text(receipt.record_id));
    commitment.insert(
        "content_digest".to_owned(),
        text(&receipt.content_digest.to_string()),
    );
    commitment.insert(
        "prev_receipt_hash".to_owned(),
        text(&receipt.prev_receipt_hash.to_string()),
    );
    let canonical = canonicalize_value(&Value::Object(commitment), &Limits::UNBOUNDED)?;
    Ok(Digest::sha256(&canonical))
}

/// Checks that `current` follows the receipt whose hash is `previous_receipt_hash` and
/// returns the hash of `current`, the next chain head.
pub fn verify_receipt_link(
    previous_receipt_hash: &Digest,
    current: &ReceiptCommitment<'_>,
) -> Result<Digest, ReceiptLinkError> {
    let hash = compute_receipt_hash(current)?;
    if current.prev_receipt_hash != previous_receipt_hash {
        return Err(ReceiptLinkError::LinkMismatch);
    }
    Ok(hash)
}

/// The only `received_at` form v1 accepts: `YYYY-MM-DDTHH:MM:SS.sssZ` in UTC.
pub fn format_received_at(unix_millis: u64) -> Result<String, FormatReceivedAtError> {
    if unix_millis > MAX_RECEIVED_AT_MILLIS {
        return Err(FormatReceivedAtError);
    }
    let (year, month, day) = civil_from_days(unix_millis / MILLIS_PER_DAY);
    let of_day = unix_millis % MILLIS_PER_DAY;
    let (hour, minute) = (of_day / 3_600_000, of_day / 60_000 % 60);
    let (second, milli) = (of_day / 1000 % 60, of_day % 1000);
    Ok(format!(
        "{year:04}-{month:02}-{day:02}T{hour:02}:{minute:02}:{second:02}.{milli:03}Z"
    ))
}

fn validate(receipt: &ReceiptCommitment<'_>) -> Result<(), InvalidReceiptError> {
    if receipt.receipt_profile != RECEIPT_PROFILE {
        return Err(InvalidReceiptError::UnknownProfile);
    }
    if !(1..=MAX_SAFE_INTEGER).contains(&receipt.receipt_sequence) {
        return Err(InvalidReceiptError::Sequence);
    }
    if !is_normalized_received_at(receipt.received_at) {
        return Err(InvalidReceiptError::ReceivedAt);
    }
    let is_first = receipt.receipt_sequence == 1;
    if is_first != (*receipt.prev_receipt_hash == Digest::GENESIS) {
        return Err(InvalidReceiptError::Genesis);
    }
    Ok(())
}

/// Parses the fixed layout, then formats the instant back: only text that round-trips is
/// normalized, which also rejects dates like February 30.
fn is_normalized_received_at(text: &str) -> bool {
    let bytes = text.as_bytes();
    let layout_ok = bytes.len() == 24
        && bytes.iter().enumerate().all(|(i, &b)| match i {
            4 | 7 => b == b'-',
            10 => b == b'T',
            13 | 16 => b == b':',
            19 => b == b'.',
            23 => b == b'Z',
            _ => b.is_ascii_digit(),
        });
    if !layout_ok {
        return false;
    }
    let number = |range: std::ops::Range<usize>| {
        bytes[range]
            .iter()
            .fold(0u64, |acc, &digit| acc * 10 + u64::from(digit - b'0'))
    };
    let (year, month, day) = (number(0..4), number(5..7), number(8..10));
    let (hour, minute, second, milli) = (
        number(11..13),
        number(14..16),
        number(17..19),
        number(20..23),
    );
    if !(1..=12).contains(&month)
        || !(1..=31).contains(&day)
        || hour > 23
        || minute > 59
        || second > 59
    {
        return false;
    }
    let Some(days) = days_from_civil(year, month, day) else {
        return false;
    };
    let millis = days * MILLIS_PER_DAY + ((hour * 60 + minute) * 60 + second) * 1000 + milli;
    format_received_at(millis).is_ok_and(|formatted| formatted == text)
}

/// Days since 1970-01-01 to a proleptic Gregorian date (H. Hinnant's `civil_from_days`,
/// restricted to non-negative days).
fn civil_from_days(days: u64) -> (u64, u64, u64) {
    let z = days + 719_468;
    let era = z / 146_097;
    let day_of_era = z - era * 146_097;
    let year_of_era =
        (day_of_era - day_of_era / 1460 + day_of_era / 36_524 - day_of_era / 146_096) / 365;
    let day_of_year = day_of_era - (365 * year_of_era + year_of_era / 4 - year_of_era / 100);
    let shifted_month = (5 * day_of_year + 2) / 153;
    let day = day_of_year - (153 * shifted_month + 2) / 5 + 1;
    let month = if shifted_month < 10 {
        shifted_month + 3
    } else {
        shifted_month - 9
    };
    let year = year_of_era + era * 400;
    (if month <= 2 { year + 1 } else { year }, month, day)
}

/// Inverse of `civil_from_days`; `None` before 1970 (and for year 0 in January–February).
/// Callers pass `month` in 1..=12 and `day` in 1..=31.
fn days_from_civil(year: u64, month: u64, day: u64) -> Option<u64> {
    let year = if month <= 2 {
        year.checked_sub(1)?
    } else {
        year
    };
    let era = year / 400;
    let year_of_era = year - era * 400;
    let shifted_month = if month > 2 { month - 3 } else { month + 9 };
    let day_of_year = (153 * shifted_month + 2) / 5 + day - 1;
    let day_of_era = year_of_era * 365 + year_of_era / 4 - year_of_era / 100 + day_of_year;
    (era * 146_097 + day_of_era).checked_sub(719_468)
}

#[cfg(test)]
mod tests {
    use super::{
        FormatReceivedAtError, InvalidReceiptError, RECEIPT_PROFILE, ReceiptCommitment,
        ReceiptError, ReceiptLinkError, compute_receipt_hash, format_received_at,
        verify_receipt_link,
    };
    use crate::Digest;

    fn genesis_receipt(content_digest: &Digest) -> ReceiptCommitment<'_> {
        ReceiptCommitment {
            receipt_profile: RECEIPT_PROFILE,
            evidence_domain_id: "domain-1",
            receipt_sequence: 1,
            received_at: "2026-09-17T20:15:32.123Z",
            producer_id: "cedarling-fleet-1",
            record_id: "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
            content_digest,
            prev_receipt_hash: &Digest::GENESIS,
        }
    }

    #[test]
    fn matches_independently_computed_value() {
        let content = Digest::sha256(b"record");
        assert_eq!(
            compute_receipt_hash(&genesis_receipt(&content))
                .expect("genesis receipt is valid")
                .to_string(),
            "sha256:6f46f99849a5a88987dfdc81ce9b2c65aafc64f3e4a687fc78e93f38d1523908",
            "SHA-256 of the JCS receipt commitment (core design §7.2)"
        );
    }

    struct FieldCase<'a> {
        name: &'static str,
        receipt: ReceiptCommitment<'a>,
    }

    #[test]
    fn every_field_is_bound() {
        let content = Digest::sha256(b"record");
        let other_content = Digest::sha256(b"other");
        let base = genesis_receipt(&content);
        let base_hash = compute_receipt_hash(&base).expect("base receipt is valid");
        let first = Digest::sha256(b"first receipt");
        let cases = [
            FieldCase {
                name: "evidence_domain_id",
                receipt: ReceiptCommitment {
                    evidence_domain_id: "domain-2",
                    ..base
                },
            },
            FieldCase {
                name: "received_at",
                receipt: ReceiptCommitment {
                    received_at: "2026-09-17T20:15:32.124Z",
                    ..base
                },
            },
            FieldCase {
                name: "producer_id",
                receipt: ReceiptCommitment {
                    producer_id: "cedarling-fleet-2",
                    ..base
                },
            },
            FieldCase {
                name: "record_id",
                receipt: ReceiptCommitment {
                    record_id: "other",
                    ..base
                },
            },
            FieldCase {
                name: "content_digest",
                receipt: ReceiptCommitment {
                    content_digest: &other_content,
                    ..base
                },
            },
            FieldCase {
                name: "receipt_sequence and prev_receipt_hash",
                receipt: ReceiptCommitment {
                    receipt_sequence: 2,
                    prev_receipt_hash: &first,
                    ..base
                },
            },
        ];
        for case in cases {
            let hash = compute_receipt_hash(&case.receipt).expect(case.name);
            assert_ne!(
                hash, base_hash,
                "changing {} changes the receipt hash",
                case.name
            );
        }
    }

    #[test]
    fn links_a_chain_and_detects_a_wrong_predecessor() {
        let content = Digest::sha256(b"record");
        let first = genesis_receipt(&content);
        let first_hash =
            verify_receipt_link(&Digest::GENESIS, &first).expect("genesis links to the sentinel");
        let second = ReceiptCommitment {
            receipt_sequence: 2,
            prev_receipt_hash: &first_hash,
            ..first
        };
        let second_hash = verify_receipt_link(&first_hash, &second).expect("second links to first");
        assert_eq!(
            second_hash,
            compute_receipt_hash(&second).expect("second is valid"),
            "link verification returns the current receipt hash"
        );

        let err = verify_receipt_link(&Digest::sha256(b"not the head"), &second)
            .expect_err("receipt names another predecessor");
        assert_eq!(
            err,
            ReceiptLinkError::LinkMismatch,
            "predecessor is checked"
        );
    }

    #[test]
    fn link_check_validates_the_receipt_first() {
        let content = Digest::sha256(b"record");
        let zero = ReceiptCommitment {
            receipt_sequence: 0,
            ..genesis_receipt(&content)
        };
        assert_eq!(
            verify_receipt_link(&Digest::sha256(b"not the head"), &zero),
            Err(ReceiptLinkError::Invalid(InvalidReceiptError::Sequence)),
            "an invalid receipt is reported as invalid, not as a link mismatch"
        );
    }

    #[test]
    fn evidence_domains_start_independent_chains() {
        let content = Digest::sha256(b"record");
        let domain_1 = genesis_receipt(&content);
        let domain_2 = ReceiptCommitment {
            evidence_domain_id: "domain-2",
            ..domain_1
        };
        let hash_1 = verify_receipt_link(&Digest::GENESIS, &domain_1).expect("domain 1 genesis");
        let hash_2 = verify_receipt_link(&Digest::GENESIS, &domain_2).expect("domain 2 genesis");
        assert_ne!(
            hash_1, hash_2,
            "each domain chain starts at sequence 1 with its own hash"
        );
    }

    struct InvalidCase<'a> {
        name: &'static str,
        receipt: ReceiptCommitment<'a>,
        reason: InvalidReceiptError,
    }

    #[test]
    fn rejects_receipts_outside_the_v1_profile() {
        let content = Digest::sha256(b"record");
        let base = genesis_receipt(&content);
        let first = Digest::sha256(b"first receipt");
        let cases = [
            InvalidCase {
                name: "unknown profile",
                receipt: ReceiptCommitment {
                    receipt_profile: "tag:jans.io,2026:lock-trace-receipt-v2",
                    ..base
                },
                reason: InvalidReceiptError::UnknownProfile,
            },
            InvalidCase {
                name: "sequence zero",
                receipt: ReceiptCommitment {
                    receipt_sequence: 0,
                    ..base
                },
                reason: InvalidReceiptError::Sequence,
            },
            InvalidCase {
                name: "sequence beyond 2^53-1",
                receipt: ReceiptCommitment {
                    receipt_sequence: 1 << 53,
                    prev_receipt_hash: &first,
                    ..base
                },
                reason: InvalidReceiptError::Sequence,
            },
            InvalidCase {
                name: "genesis with a predecessor",
                receipt: ReceiptCommitment {
                    prev_receipt_hash: &first,
                    ..base
                },
                reason: InvalidReceiptError::Genesis,
            },
            InvalidCase {
                name: "sentinel after genesis",
                receipt: ReceiptCommitment {
                    receipt_sequence: 2,
                    ..base
                },
                reason: InvalidReceiptError::Genesis,
            },
        ];
        for case in cases {
            assert_eq!(
                compute_receipt_hash(&case.receipt),
                Err(ReceiptError::Invalid(case.reason)),
                "reason for {}",
                case.name
            );
        }
        let last_safe = ReceiptCommitment {
            receipt_sequence: (1 << 53) - 1,
            prev_receipt_hash: &first,
            ..base
        };
        compute_receipt_hash(&last_safe).expect("2^53-1 is still exact in JCS");
    }

    #[test]
    fn rejects_received_at_not_normalized_to_the_profile() {
        let content = Digest::sha256(b"record");
        for received_at in [
            "2026-09-17T20:15:32Z",
            "2026-09-17T20:15:32.12Z",
            "2026-09-17T20:15:32.1234Z",
            "2026-09-17T20:15:32.123+00:00",
            "2026-09-17T20:15:32.123z",
            "2026-09-17 20:15:32.123Z",
            "2026-02-30T00:00:00.000Z",
            "2026-02-29T00:00:00.000Z",
            "2026-09-17T24:00:00.000Z",
            "2026-09-17T20:60:00.000Z",
            "2026-09-17T20:15:60.000Z",
            "2026-13-01T00:00:00.000Z",
            "2026-00-01T00:00:00.000Z",
            "2026-09-00T00:00:00.000Z",
            "+2026-09-17T20:15:32.123Z",
            "2026-09-17T20:15:32.12٣Z",
            // Lock stamps the current time; u64 milliseconds start at the Unix epoch.
            "1969-12-31T23:59:59.999Z",
            "",
        ] {
            let receipt = ReceiptCommitment {
                received_at,
                ..genesis_receipt(&content)
            };
            assert_eq!(
                compute_receipt_hash(&receipt),
                Err(ReceiptError::Invalid(InvalidReceiptError::ReceivedAt)),
                "received_at {received_at:?} is not RFC 3339 UTC with milliseconds"
            );
        }
    }

    #[test]
    fn accepts_received_at_on_calendar_edges() {
        let content = Digest::sha256(b"record");
        for received_at in [
            "1970-01-01T00:00:00.000Z",
            "2024-02-29T23:59:59.999Z",
            "2000-02-29T00:00:00.000Z",
            "9999-12-31T23:59:59.999Z",
        ] {
            let receipt = ReceiptCommitment {
                received_at,
                ..genesis_receipt(&content)
            };
            compute_receipt_hash(&receipt).expect(received_at);
        }
    }

    struct FormatCase {
        millis: u64,
        text: &'static str,
    }

    #[test]
    fn formats_received_at_as_rfc3339_utc_millis() {
        let cases = [
            FormatCase {
                millis: 0,
                text: "1970-01-01T00:00:00.000Z",
            },
            FormatCase {
                millis: 951_782_400_000,
                text: "2000-02-29T00:00:00.000Z",
            },
            FormatCase {
                millis: 1_789_676_132_123,
                text: "2026-09-17T20:15:32.123Z",
            },
            FormatCase {
                millis: 253_402_300_799_999,
                text: "9999-12-31T23:59:59.999Z",
            },
        ];
        for case in cases {
            assert_eq!(
                format_received_at(case.millis).expect("year fits in four digits"),
                case.text,
                "formatting of {} ms",
                case.millis
            );
        }
        assert_eq!(
            format_received_at(253_402_300_800_000),
            Err(FormatReceivedAtError),
            "year 10000 does not fit the profile format"
        );
        assert_eq!(
            format_received_at(u64::MAX),
            Err(FormatReceivedAtError),
            "no overflow at the top of the range"
        );
    }

    struct CodeCase {
        actual: &'static str,
        expected: &'static str,
    }

    #[test]
    fn codes_are_stable() {
        let cases = [
            CodeCase {
                actual: ReceiptError::Invalid(InvalidReceiptError::Sequence).code(),
                expected: "INVALID_RECEIPT",
            },
            CodeCase {
                actual: ReceiptLinkError::Invalid(InvalidReceiptError::Genesis).code(),
                expected: "INVALID_RECEIPT",
            },
            CodeCase {
                actual: ReceiptLinkError::LinkMismatch.code(),
                expected: "RECEIPT_LINK_MISMATCH",
            },
            CodeCase {
                actual: FormatReceivedAtError.code(),
                expected: "INVALID_RECEIPT",
            },
        ];
        for case in cases {
            assert_eq!(
                case.actual, case.expected,
                "stable code from core design §10"
            );
        }
    }
}
