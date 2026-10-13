// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Lock receipt chain, profile `lock-trace-receipt-v1` (core design §7). Lock assigns the
//! sequence, time and chain head; this module types and hashes what Lock supplies.
//!
//! Every v1 rule is checked when a value is built: `ReceivedAt` holds only normalized
//! timestamps and `ReceiptPosition` only valid sequence and predecessor pairs. A
//! `ReceiptCommitment` therefore always hashes, and the link check can fail only on a
//! wrong predecessor.

use std::fmt;
use std::str::FromStr;

use thiserror::Error;

use crate::Digest;
use crate::assertion::VerifiedAssertion;
use crate::json::write_jcs_string;

pub const RECEIPT_PROFILE: &str = "tag:jans.io,2026:lock-trace-receipt-v1";

/// Assertion profile of Jans TRACE records (core design §15). The crate's outputs do not
/// depend on it; it is exported so producers and Lock spell it one way.
pub const ASSERTION_PROFILE: &str = "tag:jans.io,2026:trace-v1";

/// Largest integer JCS reproduces exactly (numbers are IEEE-754 doubles).
const MAX_SAFE_INTEGER: u64 = (1 << 53) - 1;

/// Last millisecond the `YYYY-…` format can express: 9999-12-31T23:59:59.999Z.
const MAX_RECEIVED_AT_MILLIS: u64 = 253_402_300_799_999;

const MILLIS_PER_DAY: u64 = 86_400_000;

/// Lock's receipt time, always `YYYY-MM-DDTHH:MM:SS.sssZ` for a real UTC instant, so the
/// text that is hashed is the text an independent verifier reproduces.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ReceivedAt(String);

impl ReceivedAt {
    /// Formats Lock's clock reading; the core has no clock of its own.
    pub fn from_unix_millis(unix_millis: u64) -> Result<Self, ReceivedAtRangeError> {
        if unix_millis > MAX_RECEIVED_AT_MILLIS {
            return Err(ReceivedAtRangeError { unix_millis });
        }
        let (year, month, day) = civil_from_days(unix_millis / MILLIS_PER_DAY);
        let of_day = unix_millis % MILLIS_PER_DAY;
        let (hour, minute) = (of_day / 3_600_000, of_day / 60_000 % 60);
        let (second, milli) = (of_day / 1000 % 60, of_day % 1000);
        Ok(Self(format!(
            "{year:04}-{month:02}-{day:02}T{hour:02}:{minute:02}:{second:02}.{milli:03}Z"
        )))
    }

    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl fmt::Display for ReceivedAt {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

impl FromStr for ReceivedAt {
    type Err = ParseReceivedAtError;

    /// Parses the fixed layout, then formats the instant back: only text that round-trips
    /// is accepted, which also rejects dates like February 30.
    fn from_str(text: &str) -> Result<Self, Self::Err> {
        let millis = parse_received_at_millis(text)?;
        match Self::from_unix_millis(millis) {
            Ok(value) if value.0 == text => Ok(value),
            _ => Err(ReceivedAtReason::NotAnInstant.into()),
        }
    }
}

/// Text is not a v1 receipt timestamp. A timestamp is not secret, but the reason names the
/// rule, not the text.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("received_at is not a valid receipt timestamp: {reason}")]
pub struct ParseReceivedAtError {
    reason: ReceivedAtReason,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
enum ReceivedAtReason {
    #[error("not in the form YYYY-MM-DDTHH:MM:SS.sssZ")]
    Layout,
    #[error("not a real calendar instant between 1970-01-01 and 9999-12-31")]
    NotAnInstant,
}

impl From<ReceivedAtReason> for ParseReceivedAtError {
    fn from(reason: ReceivedAtReason) -> Self {
        Self { reason }
    }
}

impl ParseReceivedAtError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        "INVALID_RECEIPT"
    }
}

/// A clock reading after 9999-12-31T23:59:59.999Z, which the receipt format cannot express.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("{unix_millis} ms is after 9999-12-31T23:59:59.999Z, the last receipt timestamp")]
pub struct ReceivedAtRangeError {
    unix_millis: u64,
}

impl ReceivedAtRangeError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        "INVALID_RECEIPT"
    }
}

/// Where a receipt sits in its evidence-domain chain. Sequence 1 names the genesis sentinel
/// and no other sequence does, so a chain can neither start without it nor restart midway.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct ReceiptPosition {
    sequence: u64,
    prev_receipt_hash: Digest,
}

impl ReceiptPosition {
    /// The first receipt of an evidence domain (core design §7.3).
    pub const GENESIS: Self = Self {
        sequence: 1,
        prev_receipt_hash: Digest::GENESIS,
    };

    /// Sequence and predecessor exactly as Lock assigned or stored them. Lock allocates the
    /// sequence (core design §7.4); this only refuses pairs outside the v1 chain.
    pub fn new(sequence: u64, prev_receipt_hash: Digest) -> Result<Self, ReceiptPositionError> {
        if !(1..=MAX_SAFE_INTEGER).contains(&sequence) {
            return Err(PositionReason::SequenceOutOfRange(sequence).into());
        }
        match (sequence == 1, prev_receipt_hash == Digest::GENESIS) {
            (true, false) => Err(PositionReason::GenesisWithPredecessor.into()),
            (false, true) => Err(PositionReason::SentinelAfterGenesis.into()),
            _ => Ok(Self {
                sequence,
                prev_receipt_hash,
            }),
        }
    }

    #[must_use]
    pub const fn sequence(&self) -> u64 {
        self.sequence
    }

    #[must_use]
    pub const fn prev_receipt_hash(&self) -> &Digest {
        &self.prev_receipt_hash
    }
}

/// Sequence and predecessor do not form a v1 chain position.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("receipt position is not valid in a v1 chain: {reason}")]
pub struct ReceiptPositionError {
    reason: PositionReason,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
enum PositionReason {
    /// Beyond 2^53-1 JCS would silently change the number.
    #[error("sequence {0} is outside 1..=2^53-1")]
    SequenceOutOfRange(u64),
    #[error("sequence 1 must name the genesis sentinel")]
    GenesisWithPredecessor,
    #[error("only sequence 1 may name the genesis sentinel")]
    SentinelAfterGenesis,
}

impl From<PositionReason> for ReceiptPositionError {
    fn from(reason: PositionReason) -> Self {
        Self { reason }
    }
}

impl ReceiptPositionError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        "INVALID_RECEIPT"
    }
}

/// The closed v1 field set hashed into a receipt (core design §7.1). Every field is already
/// valid by its type, so any value hashes.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ReceiptCommitment {
    pub evidence_domain_id: String,
    pub position: ReceiptPosition,
    pub received_at: ReceivedAt,
    pub producer_id: String,
    pub record_id: String,
    pub content_digest: Digest,
}

impl ReceiptCommitment {
    /// The commitment Lock appends for a record it just verified: producer, record id and
    /// content digest come from the verification, so a receipt cannot name a record that was
    /// not checked.
    #[must_use]
    pub fn for_assertion(
        evidence_domain_id: String,
        position: ReceiptPosition,
        received_at: ReceivedAt,
        assertion: &VerifiedAssertion,
    ) -> Self {
        Self {
            evidence_domain_id,
            position,
            received_at,
            producer_id: assertion.identity().producer().to_owned(),
            record_id: assertion.identity().record_id().to_owned(),
            content_digest: assertion.content_digest(),
        }
    }

    /// `sha256(JCS(commitment))`; the hash is not part of its own input (core design §7.2).
    #[must_use]
    pub fn receipt_hash(&self) -> Digest {
        Digest::sha256(&self.canonical_bytes())
    }

    /// Checks that this receipt names `previous_receipt_hash`, the current chain head (or
    /// `Digest::GENESIS` for an empty chain), and returns its own hash: the next head.
    pub fn verify_link(&self, previous_receipt_hash: &Digest) -> Result<Digest, ReceiptLinkError> {
        let named = self.position.prev_receipt_hash;
        if named != *previous_receipt_hash {
            return Err(ReceiptLinkError {
                named,
                expected: *previous_receipt_hash,
            });
        }
        Ok(self.receipt_hash())
    }

    /// JCS by hand: the member names are fixed and listed here in JCS order, and every value
    /// is a string or a safe integer, so no step can fail.
    fn canonical_bytes(&self) -> Vec<u8> {
        let members: [(&str, Member<'_>); 8] = [
            (
                "content_digest",
                Member::Text(&self.content_digest.to_string()),
            ),
            ("evidence_domain_id", Member::Text(&self.evidence_domain_id)),
            (
                "prev_receipt_hash",
                Member::Text(&self.position.prev_receipt_hash.to_string()),
            ),
            ("producer_id", Member::Text(&self.producer_id)),
            ("receipt_profile", Member::Text(RECEIPT_PROFILE)),
            ("receipt_sequence", Member::Integer(self.position.sequence)),
            ("received_at", Member::Text(self.received_at.as_str())),
            ("record_id", Member::Text(&self.record_id)),
        ];
        let mut out = Vec::with_capacity(512);
        out.push(b'{');
        for (index, (name, value)) in members.iter().enumerate() {
            if index > 0 {
                out.push(b',');
            }
            write_jcs_string(&mut out, name);
            out.push(b':');
            match value {
                Member::Text(text) => write_jcs_string(&mut out, text),
                // JCS writes a safe integer as its plain decimal digits.
                Member::Integer(number) => out.extend_from_slice(number.to_string().as_bytes()),
            }
        }
        out.push(b'}');
        out
    }
}

enum Member<'a> {
    Text(&'a str),
    Integer(u64),
}

/// The receipt names another predecessor than the current chain head. Digests are public,
/// so both are shown.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Error)]
#[error("receipt names predecessor {named}, but the chain head is {expected}")]
pub struct ReceiptLinkError {
    named: Digest,
    expected: Digest,
}

impl ReceiptLinkError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        "RECEIPT_LINK_MISMATCH"
    }
}

/// Reads the fixed layout into milliseconds since the epoch; `FromStr` then checks that the
/// instant formats back to the same text.
fn parse_received_at_millis(text: &str) -> Result<u64, ReceivedAtReason> {
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
        return Err(ReceivedAtReason::Layout);
    }
    // Only ASCII digits sit at these positions, and the length is exactly 24.
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
        return Err(ReceivedAtReason::NotAnInstant);
    }
    let days = days_from_civil(year, month, day).ok_or(ReceivedAtReason::NotAnInstant)?;
    Ok(days * MILLIS_PER_DAY + ((hour * 60 + minute) * 60 + second) * 1000 + milli)
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
        ParseReceivedAtError, ReceiptCommitment, ReceiptLinkError, ReceiptPosition,
        ReceiptPositionError, ReceivedAt, ReceivedAtRangeError,
    };
    use crate::Digest;

    const MAX_SAFE_INTEGER: u64 = (1 << 53) - 1;

    fn received_at(text: &str) -> ReceivedAt {
        text.parse().expect("test timestamp is normalized")
    }

    fn genesis_receipt() -> ReceiptCommitment {
        ReceiptCommitment {
            evidence_domain_id: "domain-1".to_owned(),
            position: ReceiptPosition::GENESIS,
            received_at: received_at("2026-09-17T20:15:32.123Z"),
            producer_id: "cedarling-fleet-1".to_owned(),
            record_id: "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41".to_owned(),
            content_digest: Digest::sha256(b"record"),
        }
    }

    fn position(sequence: u64, prev_receipt_hash: Digest) -> ReceiptPosition {
        ReceiptPosition::new(sequence, prev_receipt_hash).expect("test position is valid")
    }

    #[test]
    fn matches_independently_computed_value() {
        assert_eq!(
            genesis_receipt().receipt_hash().to_string(),
            "sha256:6f46f99849a5a88987dfdc81ce9b2c65aafc64f3e4a687fc78e93f38d1523908",
            "SHA-256 of the JCS receipt commitment (core design §7.2)"
        );
    }

    #[test]
    fn commitment_for_a_verified_assertion_takes_its_identity_and_digest() {
        use crate::Limits;
        use crate::assertion::{UnsignedAssertion, VerificationContext};
        use crate::key::PublicKey;
        use crate::signature::Ed25519Signer;

        let signer = Ed25519Signer::from_secret_bytes("key-2026-01", &[7; 32]);
        let unsigned = br#"{"producer":"cedarling-fleet-1","kid":"key-2026-01","record_id":"r-1"}"#;
        let verified = UnsignedAssertion::parse(unsigned, &Limits::default())
            .expect("record is well formed")
            .sign(&signer)
            .expect("in-memory signer signs")
            .verify(&VerificationContext {
                expected_producer: "cedarling-fleet-1".to_owned(),
                expected_kid: "key-2026-01".to_owned(),
                public_key: PublicKey::from_bytes(&signer.public_key_bytes())
                    .expect("signer key is valid"),
            })
            .expect("own record verifies");
        let commitment = ReceiptCommitment::for_assertion(
            "domain-1".to_owned(),
            ReceiptPosition::GENESIS,
            received_at("2026-09-17T20:15:32.123Z"),
            &verified,
        );
        assert_eq!(
            commitment,
            ReceiptCommitment {
                evidence_domain_id: "domain-1".to_owned(),
                position: ReceiptPosition::GENESIS,
                received_at: received_at("2026-09-17T20:15:32.123Z"),
                producer_id: "cedarling-fleet-1".to_owned(),
                record_id: "r-1".to_owned(),
                content_digest: verified.content_digest(),
            },
            "producer, record id and content digest come from the verified record"
        );
    }

    #[test]
    fn hashes_non_ascii_fields_like_the_canonicalizer() {
        let receipt = ReceiptCommitment {
            evidence_domain_id: "domaine-é\u{1F600}\"\\\n".to_owned(),
            ..genesis_receipt()
        };
        let expected = serde_json_canonicalizer::to_vec(&serde_json::json!({
            "receipt_profile": super::RECEIPT_PROFILE,
            "evidence_domain_id": receipt.evidence_domain_id,
            "receipt_sequence": 1,
            "received_at": receipt.received_at.as_str(),
            "producer_id": receipt.producer_id,
            "record_id": receipt.record_id,
            "content_digest": receipt.content_digest.to_string(),
            "prev_receipt_hash": Digest::GENESIS.to_string(),
        }))
        .expect("receipt commitment canonicalizes");
        assert_eq!(
            receipt.receipt_hash(),
            Digest::sha256(&expected),
            "hand-built JCS equals the canonicalizer, escapes included"
        );
    }

    struct FieldCase {
        name: &'static str,
        receipt: ReceiptCommitment,
    }

    #[test]
    fn every_field_is_bound() {
        let base = genesis_receipt();
        let base_hash = base.receipt_hash();
        let cases = [
            FieldCase {
                name: "evidence_domain_id",
                receipt: ReceiptCommitment {
                    evidence_domain_id: "domain-2".to_owned(),
                    ..base.clone()
                },
            },
            FieldCase {
                name: "received_at",
                receipt: ReceiptCommitment {
                    received_at: received_at("2026-09-17T20:15:32.124Z"),
                    ..base.clone()
                },
            },
            FieldCase {
                name: "producer_id",
                receipt: ReceiptCommitment {
                    producer_id: "cedarling-fleet-2".to_owned(),
                    ..base.clone()
                },
            },
            FieldCase {
                name: "record_id",
                receipt: ReceiptCommitment {
                    record_id: "other".to_owned(),
                    ..base.clone()
                },
            },
            FieldCase {
                name: "content_digest",
                receipt: ReceiptCommitment {
                    content_digest: Digest::sha256(b"other"),
                    ..base.clone()
                },
            },
            FieldCase {
                name: "position",
                receipt: ReceiptCommitment {
                    position: position(2, Digest::sha256(b"first receipt")),
                    ..base.clone()
                },
            },
        ];
        for case in cases {
            assert_ne!(
                case.receipt.receipt_hash(),
                base_hash,
                "changing {} changes the receipt hash",
                case.name
            );
        }
    }

    #[test]
    fn links_a_chain_and_detects_a_wrong_predecessor() {
        let first = genesis_receipt();
        let first_hash = first
            .verify_link(&Digest::GENESIS)
            .expect("genesis links to the sentinel");
        assert_eq!(
            first_hash,
            first.receipt_hash(),
            "link check returns the hash"
        );
        let second = ReceiptCommitment {
            position: position(2, first_hash),
            ..first
        };
        let second_hash = second
            .verify_link(&first_hash)
            .expect("second links to first");
        assert_eq!(second_hash, second.receipt_hash(), "the new chain head");

        let head = Digest::sha256(b"not the head");
        assert_eq!(
            second.verify_link(&head),
            Err(ReceiptLinkError {
                named: first_hash,
                expected: head,
            }),
            "a receipt naming another predecessor is a link mismatch"
        );
        assert_eq!(
            genesis_receipt().verify_link(&first_hash),
            Err(ReceiptLinkError {
                named: Digest::GENESIS,
                expected: first_hash,
            }),
            "a genesis receipt cannot follow an existing head"
        );
    }

    #[test]
    fn evidence_domains_start_independent_chains() {
        let domain_1 = genesis_receipt();
        let domain_2 = ReceiptCommitment {
            evidence_domain_id: "domain-2".to_owned(),
            ..genesis_receipt()
        };
        assert_ne!(
            domain_1
                .verify_link(&Digest::GENESIS)
                .expect("domain 1 genesis"),
            domain_2
                .verify_link(&Digest::GENESIS)
                .expect("domain 2 genesis"),
            "each domain chain starts at sequence 1 with its own hash"
        );
    }

    #[test]
    fn position_accepts_genesis_and_safe_successors() {
        assert_eq!(
            ReceiptPosition::new(1, Digest::GENESIS),
            Ok(ReceiptPosition::GENESIS),
            "sequence 1 with the sentinel is genesis"
        );
        let last = position(MAX_SAFE_INTEGER, Digest::sha256(b"previous"));
        assert_eq!(
            last.sequence(),
            MAX_SAFE_INTEGER,
            "2^53-1 is still exact in JCS"
        );
        assert_eq!(
            last.prev_receipt_hash(),
            &Digest::sha256(b"previous"),
            "predecessor is kept"
        );
    }

    struct PositionCase {
        name: &'static str,
        sequence: u64,
        prev_receipt_hash: Digest,
        reason: super::PositionReason,
    }

    #[test]
    fn position_rejects_states_outside_the_v1_chain() {
        let previous = Digest::sha256(b"previous");
        let cases = [
            PositionCase {
                name: "sequence zero",
                sequence: 0,
                prev_receipt_hash: Digest::GENESIS,
                reason: super::PositionReason::SequenceOutOfRange(0),
            },
            PositionCase {
                name: "sequence beyond 2^53-1",
                sequence: 1 << 53,
                prev_receipt_hash: previous,
                reason: super::PositionReason::SequenceOutOfRange(1 << 53),
            },
            PositionCase {
                name: "genesis with a predecessor",
                sequence: 1,
                prev_receipt_hash: previous,
                reason: super::PositionReason::GenesisWithPredecessor,
            },
            PositionCase {
                name: "sentinel after genesis",
                sequence: 2,
                prev_receipt_hash: Digest::GENESIS,
                reason: super::PositionReason::SentinelAfterGenesis,
            },
        ];
        for case in cases {
            assert_eq!(
                ReceiptPosition::new(case.sequence, case.prev_receipt_hash),
                Err(ReceiptPositionError {
                    reason: case.reason
                }),
                "reason for {}",
                case.name
            );
        }
    }

    struct TextCase {
        text: &'static str,
        reason: super::ReceivedAtReason,
    }

    #[test]
    fn received_at_rejects_text_not_normalized_to_the_profile() {
        use super::ReceivedAtReason::{Layout, NotAnInstant};
        let cases = [
            TextCase {
                text: "2026-09-17T20:15:32Z",
                reason: Layout,
            },
            TextCase {
                text: "2026-09-17T20:15:32.12Z",
                reason: Layout,
            },
            TextCase {
                text: "2026-09-17T20:15:32.1234Z",
                reason: Layout,
            },
            TextCase {
                text: "2026-09-17T20:15:32.123+00:00",
                reason: Layout,
            },
            TextCase {
                text: "2026-09-17T20:15:32.123z",
                reason: Layout,
            },
            TextCase {
                text: "2026-09-17 20:15:32.123Z",
                reason: Layout,
            },
            TextCase {
                text: "+2026-09-17T20:15:32.123Z",
                reason: Layout,
            },
            TextCase {
                text: "2026-09-17T20:15:32.12٣Z",
                reason: Layout,
            },
            TextCase {
                text: "",
                reason: Layout,
            },
            TextCase {
                text: "2026-02-30T00:00:00.000Z",
                reason: NotAnInstant,
            },
            TextCase {
                text: "2026-02-29T00:00:00.000Z",
                reason: NotAnInstant,
            },
            TextCase {
                text: "2026-09-17T24:00:00.000Z",
                reason: NotAnInstant,
            },
            TextCase {
                text: "2026-09-17T20:60:00.000Z",
                reason: NotAnInstant,
            },
            TextCase {
                text: "2026-09-17T20:15:60.000Z",
                reason: NotAnInstant,
            },
            TextCase {
                text: "2026-13-01T00:00:00.000Z",
                reason: NotAnInstant,
            },
            TextCase {
                text: "2026-00-01T00:00:00.000Z",
                reason: NotAnInstant,
            },
            TextCase {
                text: "2026-09-00T00:00:00.000Z",
                reason: NotAnInstant,
            },
            // Lock stamps the current time; u64 milliseconds start at the Unix epoch.
            TextCase {
                text: "1969-12-31T23:59:59.999Z",
                reason: NotAnInstant,
            },
        ];
        for case in cases {
            assert_eq!(
                case.text.parse::<ReceivedAt>(),
                Err(ParseReceivedAtError {
                    reason: case.reason
                }),
                "received_at {:?}",
                case.text
            );
        }
    }

    #[test]
    fn received_at_accepts_calendar_edges() {
        for text in [
            "1970-01-01T00:00:00.000Z",
            "2024-02-29T23:59:59.999Z",
            "2000-02-29T00:00:00.000Z",
            "9999-12-31T23:59:59.999Z",
        ] {
            assert_eq!(received_at(text).as_str(), text, "{text} round-trips");
        }
    }

    struct MillisCase {
        millis: u64,
        text: &'static str,
    }

    #[test]
    fn received_at_from_unix_millis() {
        let cases = [
            MillisCase {
                millis: 0,
                text: "1970-01-01T00:00:00.000Z",
            },
            MillisCase {
                millis: 951_782_400_000,
                text: "2000-02-29T00:00:00.000Z",
            },
            MillisCase {
                millis: 1_789_676_132_123,
                text: "2026-09-17T20:15:32.123Z",
            },
            MillisCase {
                millis: 253_402_300_799_999,
                text: "9999-12-31T23:59:59.999Z",
            },
        ];
        for case in cases {
            let value = ReceivedAt::from_unix_millis(case.millis).expect("year fits");
            assert_eq!(
                value.as_str(),
                case.text,
                "formatting of {} ms",
                case.millis
            );
            assert_eq!(value.to_string(), case.text, "Display is the profile text");
        }
        for millis in [253_402_300_800_000, u64::MAX] {
            assert_eq!(
                ReceivedAt::from_unix_millis(millis),
                Err(ReceivedAtRangeError {
                    unix_millis: millis
                }),
                "{millis} ms is after year 9999"
            );
        }
    }

    #[test]
    fn display_carries_the_reason_once() {
        let err = "2026-02-30T00:00:00.000Z"
            .parse::<ReceivedAt>()
            .expect_err("February 30");
        assert_eq!(
            err.to_string(),
            "received_at is not a valid receipt timestamp: not a real calendar instant \
             between 1970-01-01 and 9999-12-31",
            "`{{err}}` alone tells the operator what is wrong"
        );
        let err = ReceiptPosition::new(2, Digest::GENESIS).expect_err("sentinel after genesis");
        assert_eq!(
            err.to_string(),
            "receipt position is not valid in a v1 chain: only sequence 1 may name the \
             genesis sentinel",
            "`{{err}}` alone tells the operator what is wrong"
        );
        assert!(
            std::error::Error::source(&err).is_none(),
            "the reason is already in Display"
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
                actual: "x".parse::<ReceivedAt>().expect_err("layout").code(),
                expected: "INVALID_RECEIPT",
            },
            CodeCase {
                actual: ReceivedAt::from_unix_millis(u64::MAX)
                    .expect_err("range")
                    .code(),
                expected: "INVALID_RECEIPT",
            },
            CodeCase {
                actual: ReceiptPosition::new(0, Digest::GENESIS)
                    .expect_err("zero")
                    .code(),
                expected: "INVALID_RECEIPT",
            },
            CodeCase {
                actual: genesis_receipt()
                    .verify_link(&Digest::sha256(b"head"))
                    .expect_err("mismatch")
                    .code(),
                expected: "RECEIPT_LINK_MISMATCH",
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
