// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Bulk request bodies (Lock MVP §11.2): a JSON array of signed records.
//!
//! The core splits the body itself, so each record reaches `SignedAssertion::parse` as its
//! exact text. A host that parsed the body with its own JSON library and serialized each
//! record again would merge duplicate members before the strict parser could see them, and
//! would lose the bytes Lock must store.

use std::borrow::Borrow;
use std::cell::Cell;
use std::fmt;

use serde::de::{Deserializer as _, IgnoredAny, SeqAccess, Visitor};
use serde_json::value::RawValue;
use thiserror::Error;

use super::{ParseSignedAssertionError, SignedAssertion};
use crate::json::JsonError;
use crate::limits::LimitKind;
use crate::{Limits, SizeLimitError};

/// Splits a bulk body into records and parses each one. The outer `Result` is about the
/// body (Lock answers `400`); each inner one is a per-record accept or reject.
pub fn parse_batch(
    body: &str,
    limits: &Limits,
) -> Result<Vec<Result<SignedAssertion, ParseSignedAssertionError>>, ParseBatchError> {
    if body.len() > limits.max_batch_bytes() {
        return Err(SizeLimitError::new(LimitKind::BatchBytes, limits.max_batch_bytes()).into());
    }
    let records = split_records(body, limits.max_batch_records())?;
    Ok(records
        .into_iter()
        .map(|record| SignedAssertion::parse(record.get(), limits))
        .collect())
}

/// The bulk body for `records`: their own text joined into an array, never serialized
/// again. Refuses a batch above the limits Lock applies, so the producer splits it first.
/// Takes owned or shared records (`SignedAssertion`, `Arc<SignedAssertion>`, references), so
/// a caller never copies records just to join them.
pub fn batch_body<R: Borrow<SignedAssertion>>(
    records: &[R],
    limits: &Limits,
) -> Result<String, SizeLimitError> {
    if records.len() > limits.max_batch_records() {
        return Err(SizeLimitError::new(
            LimitKind::BatchRecords,
            limits.max_batch_records(),
        ));
    }
    let mut body = String::from("[");
    for (index, record) in records.iter().enumerate() {
        if index > 0 {
            body.push(',');
        }
        body.push_str(record.borrow().as_str());
    }
    body.push(']');
    if body.len() > limits.max_batch_bytes() {
        return Err(SizeLimitError::new(
            LimitKind::BatchBytes,
            limits.max_batch_bytes(),
        ));
    }
    Ok(body)
}

/// Why `parse_batch` rejected the body as a whole. Per-record failures are not here: they are
/// the inner results.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum ParseBatchError {
    #[error(transparent)]
    SizeLimitExceeded(#[from] SizeLimitError),
    #[error(transparent)]
    Json(#[from] JsonError),
    #[error("bulk body is not a JSON array of records")]
    NotAnArray,
}

impl ParseBatchError {
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::SizeLimitExceeded(e) => e.code(),
            Self::Json(e) => e.code(),
            Self::NotAnArray => "INVALID_BATCH",
        }
    }
}

/// The exact text of each array element. Element syntax is checked here; everything else
/// about a record is `SignedAssertion::parse`'s job.
fn split_records(body: &str, max_records: usize) -> Result<Vec<&RawValue>, ParseBatchError> {
    let invalid = |err: serde_json::Error| JsonError::Invalid {
        line: err.line(),
        column: err.column(),
    };
    let starts_as_array = body
        .trim_start_matches([' ', '\t', '\n', '\r'])
        .starts_with('[');
    if !starts_as_array {
        // Valid JSON of another kind is a shape error; anything else is a syntax error.
        return Err(match serde_json::from_str::<IgnoredAny>(body) {
            Ok(_) => ParseBatchError::NotAnArray,
            Err(err) => invalid(err).into(),
        });
    }
    let too_many = Cell::new(false);
    let mut deserializer = serde_json::Deserializer::from_str(body);
    let records = deserializer
        .deserialize_seq(RecordsVisitor {
            max_records,
            too_many: &too_many,
        })
        .map_err(|err| {
            if too_many.get() {
                ParseBatchError::from(SizeLimitError::new(LimitKind::BatchRecords, max_records))
            } else {
                invalid(err).into()
            }
        })?;
    deserializer.end().map_err(invalid)?;
    Ok(records)
}

struct RecordsVisitor<'a> {
    max_records: usize,
    /// Set when the count bound stops parsing, so the error can say so.
    too_many: &'a Cell<bool>,
}

impl<'de> Visitor<'de> for RecordsVisitor<'_> {
    type Value = Vec<&'de RawValue>;

    fn expecting(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("a JSON array of records")
    }

    fn visit_seq<A: SeqAccess<'de>>(self, mut seq: A) -> Result<Self::Value, A::Error> {
        let mut records = Vec::new();
        while let Some(record) = seq.next_element::<&'de RawValue>()? {
            if records.len() == self.max_records {
                self.too_many.set(true);
                return Err(serde::de::Error::custom("too many records"));
            }
            records.push(record);
        }
        Ok(records)
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Arc;

    use super::{ParseBatchError, batch_body, parse_batch};
    use crate::assertion::{ParseSignedAssertionError, SignedAssertion, UnsignedAssertion};
    use crate::json::JsonError;
    use crate::limits::LimitKind;
    use crate::signature::Ed25519Signer;
    use crate::{Limits, SizeLimitError};

    fn record(record_id: &str) -> SignedAssertion {
        let unsigned = format!(
            r#"{{"producer":"cedarling-fleet-1","kid":"key-2026-01","record_id":"{record_id}"}}"#
        );
        UnsignedAssertion::parse(&unsigned, &Limits::default())
            .expect("test record is well formed")
            .sign(
                &Ed25519Signer::from_secret_bytes("key-2026-01", &[7; 32])
                    .expect("test secret is 32 bytes"),
            )
            .expect("in-memory signer signs")
    }

    /// A signed record as another producer might format it: spaces, other member order.
    fn pretty(signed: &SignedAssertion) -> String {
        format!(
            "{{ \"record_id\": \"{}\",\n  \"kid\": \"key-2026-01\", \"producer\": \"cedarling-fleet-1\", \"signature\": \"{}\" }}",
            signed.identity().record_id(),
            signed.signature()
        )
    }

    fn parse(body: &str) -> Vec<Result<SignedAssertion, ParseSignedAssertionError>> {
        parse_batch(body, &Limits::default()).expect("the batch envelope is valid")
    }

    #[test]
    fn keeps_the_exact_text_of_every_record() {
        let first = pretty(&record("r-1"));
        let second = pretty(&record("r-2"));
        let body = format!("[\n  {first} ,{second}\n]");
        let records: Vec<String> = parse(&body)
            .into_iter()
            .map(|result| result.expect("each record is valid").as_str().to_owned())
            .collect();
        assert_eq!(
            records,
            [first, second],
            "each record is its own text, untouched by the envelope"
        );
    }

    #[test]
    fn rejects_records_one_by_one() {
        let valid = pretty(&record("r-1"));
        // The duplicate stays inside the record text, so the record parser still sees it.
        let duplicate_kid = valid.replacen(
            "\"kid\": \"key-2026-01\",",
            "\"kid\": \"key-2026-01\", \"kid\": \"key-other\",",
            1,
        );
        let body = format!(
            r#"[{valid}, {{"producer":"p","kid":"k","record_id":"r"}}, [], {duplicate_kid}]"#
        );
        let results = parse(&body);
        assert_eq!(results.len(), 4, "one result per record");
        assert_eq!(
            results[0].as_ref().map(SignedAssertion::as_str),
            Ok(valid.as_str()),
            "a valid record is accepted next to invalid ones"
        );
        assert_eq!(
            results[1..],
            [
                Err(ParseSignedAssertionError::MissingSignature),
                Err(ParseSignedAssertionError::NotAnObject),
                Err(ParseSignedAssertionError::Json(JsonError::DuplicateMember)),
            ],
            "each invalid record carries its own reason"
        );
    }

    #[test]
    fn accepts_an_empty_batch() {
        assert!(parse(" [ ] ").is_empty(), "no records, no results");
    }

    struct EnvelopeCase {
        name: &'static str,
        body: String,
        limits: Limits,
        error: ParseBatchError,
    }

    #[test]
    fn rejects_an_invalid_envelope() {
        let one = pretty(&record("r-1"));
        let two = format!("[{one},{one}]");
        let cases = [
            EnvelopeCase {
                name: "object instead of array",
                body: one.clone(),
                limits: Limits::default(),
                error: ParseBatchError::NotAnArray,
            },
            EnvelopeCase {
                name: "too many records",
                body: two.clone(),
                limits: Limits::default().with_max_batch_records(1),
                error: ParseBatchError::SizeLimitExceeded(SizeLimitError::new(
                    LimitKind::BatchRecords,
                    1,
                )),
            },
            EnvelopeCase {
                name: "body too large",
                body: two.clone(),
                limits: Limits::default().with_max_batch_bytes(two.len() - 1),
                error: ParseBatchError::SizeLimitExceeded(SizeLimitError::new(
                    LimitKind::BatchBytes,
                    two.len() - 1,
                )),
            },
        ];
        for case in cases {
            assert_eq!(
                parse_batch(&case.body, &case.limits),
                Err(case.error),
                "reason for {}",
                case.name
            );
        }
        parse_batch(
            &two,
            &Limits::default()
                .with_max_batch_records(2)
                .with_max_batch_bytes(two.len()),
        )
        .expect("a batch of exactly the limits is accepted");
        // The record text spans two lines, so both failures are on line 2 of the body.
        for (name, body) in [
            ("broken JSON", format!("[{one},")),
            ("trailing data", format!("[{one}] []")),
        ] {
            assert!(
                matches!(
                    parse_batch(&body, &Limits::default()),
                    Err(ParseBatchError::Json(JsonError::Invalid { line: 2, .. }))
                ),
                "{name} is invalid JSON on line 2 of the body"
            );
        }
    }

    #[test]
    fn deeply_nested_records_are_rejected_alone_without_overflowing() {
        let nested = |levels: usize| format!("{}{}", "[".repeat(levels), "]".repeat(levels));
        // 10 000 levels fit one record's byte limit: the record parser stops at depth 32.
        // 2 million levels fit the default 8 MiB body but not one record: splitting the
        // body must not recurse, and the record fails on its size.
        let body = format!("[{},{}]", nested(10_000), nested(2_000_000));
        assert_eq!(
            parse(&body),
            [
                Err(ParseSignedAssertionError::Json(JsonError::LimitExceeded(
                    SizeLimitError::new(LimitKind::Depth, 32)
                ))),
                Err(ParseSignedAssertionError::SizeLimitExceeded(
                    SizeLimitError::new(LimitKind::InputBytes, 256 * 1024)
                )),
            ],
            "each record fails with SIZE_LIMIT_EXCEEDED; the batch itself is fine"
        );
    }

    #[test]
    fn batch_body_joins_records_and_round_trips() {
        let records = [record("r-1"), record("r-2")];
        let body = batch_body(&records, &Limits::default()).expect("two records fit");
        assert_eq!(
            body,
            format!("[{},{}]", records[0].as_str(), records[1].as_str()),
            "the records' own text, joined; nothing is serialized again"
        );
        let parsed: Vec<SignedAssertion> = parse(&body)
            .into_iter()
            .map(|result| result.expect("own record parses"))
            .collect();
        assert_eq!(
            parsed, records,
            "parse_batch reads back what batch_body wrote"
        );
        assert_eq!(
            batch_body::<SignedAssertion>(&[], &Limits::default()),
            Ok("[]".to_owned()),
            "an empty batch is an empty array"
        );
    }

    #[test]
    fn batch_body_takes_shared_records_without_copying_them() {
        // Bindings hold records behind `Arc`; joining them must not need owned values.
        let records = [Arc::new(record("r-1")), Arc::new(record("r-2"))];
        assert_eq!(
            batch_body(&records, &Limits::default()),
            Ok(format!("[{},{}]", records[0].as_str(), records[1].as_str())),
            "shared records are joined like owned ones"
        );
    }

    #[test]
    fn batch_body_refuses_a_batch_lock_would_refuse() {
        let records = [record("r-1"), record("r-2")];
        let len = batch_body(&records, &Limits::default())
            .expect("two records fit")
            .len();
        assert_eq!(
            batch_body(&records, &Limits::default().with_max_batch_records(1)),
            Err(SizeLimitError::new(LimitKind::BatchRecords, 1)),
            "too many records"
        );
        assert_eq!(
            batch_body(&records, &Limits::default().with_max_batch_bytes(len - 1)),
            Err(SizeLimitError::new(LimitKind::BatchBytes, len - 1)),
            "body too large"
        );
    }

    #[test]
    fn not_an_array_has_its_own_code() {
        assert_eq!(
            ParseBatchError::NotAnArray.code(),
            "INVALID_BATCH",
            "our code, not in §10"
        );
        assert_eq!(
            ParseBatchError::Json(JsonError::DuplicateMember).code(),
            "DUPLICATE_MEMBER",
            "JSON failures keep their code"
        );
    }
}
