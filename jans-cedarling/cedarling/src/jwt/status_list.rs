// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The JWT status list validation is implemented as described in this [IETF spec](https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-02.html#name-referenced-token).
//!
//! Status lists are stored on the [`StatusList`] struct and initialized using the
//! [`StatusList::parse`] function.
//!
//! To retrieve a status of a JWT, use the [`StatusList::get_status`] function and pass
//! in the JWT's status index (`idx`).

mod cache;
#[cfg(test)]
mod conformance_tests;
mod error;
#[cfg(test)]
mod ietf_test_samples;

pub(super) use cache::*;
pub(crate) use error::StatusCheckError;
pub(super) use error::*;

use super::validation::ValidatedJwt;
use crate::common::issuer_utils::IssClaim;
use base64::prelude::{BASE64_URL_SAFE_NO_PAD, Engine};
use chrono::{DateTime, SecondsFormat, Utc};
use flate2::read::ZlibDecoder;
use serde::Deserialize;
use std::fmt::Display;
use std::io::Read;
use std::sync::Arc;
use std::time::Duration;

#[derive(Debug, PartialEq, Clone)]
pub(super) struct StatusList {
    /// The number of bits used to encode a single status
    pub(super) bit_size: StatusBitSize,
    /// The status list
    pub(super) list: Vec<u8>,
}

impl StatusList {
    /// Hard safety limit on decompressed status list size to prevent zip-bombs/OOM.
    const MAX_DECOMPRESSED_SIZE: u64 = 10 * 1024 * 1024;

    pub(super) fn parse(encoded: &str, bits: u8) -> Result<Self, ParseStatusListError> {
        let compressed = BASE64_URL_SAFE_NO_PAD.decode(encoded)?;
        let decoder = ZlibDecoder::new(compressed.as_slice());

        let mut bounded_decoder = decoder.take(Self::MAX_DECOMPRESSED_SIZE);

        let mut list = Vec::new();
        bounded_decoder.read_to_end(&mut list)?;

        // Check if there is still more data in the decoder stream
        let mut extra = [0u8; 1];
        if bounded_decoder.into_inner().read(&mut extra)? != 0 {
            return Err(ParseStatusListError::DecompressedSizeExceeded(
                Self::MAX_DECOMPRESSED_SIZE,
            ));
        }

        Ok(Self {
            bit_size: bits.try_into()?,
            list,
        })
    }

    /// Returns the status at `index`; an index past the end gives no status
    /// ([draft-ietf-oauth-status-list-21 §8.3] step 6).
    ///
    /// [draft-ietf-oauth-status-list-21 §8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
    fn get_status(&self, index: usize) -> Result<JwtStatus, IdxOutOfBounds> {
        let per_byte = self.bit_size.statuses_per_byte();

        let byte = self.list.get(index / per_byte).ok_or(IdxOutOfBounds {
            len: self.list.len().saturating_mul(per_byte),
        })?;

        Ok(JwtStatus::from(
            self.bit_size.status_in_byte(*byte, index % per_byte),
        ))
    }
}

/// Status list JWT from an IDP's status list endpoint
#[derive(Debug, Deserialize, PartialEq)]
pub(super) struct StatusListJwtStr(pub String);

impl StatusListJwtStr {
    pub(super) fn new(jwt_str: String) -> Self {
        Self(jwt_str)
    }
}

/// Status list JWT from an IDP's status list endpoint
#[derive(Debug, Deserialize, PartialEq)]
pub(super) struct StatusListJwt {
    sub: String,
    #[serde(default)]
    iss: Option<IssClaim>,
    iat: u32,
    #[serde(default)]
    exp: Option<u64>,
    #[serde(default)]
    ttl: Option<u64>,
    status_list: StatusListClaim,
    aggregation_uri: Option<String>,
}

impl TryFrom<ValidatedJwt> for StatusListJwt {
    type Error = serde_json::Error;

    fn try_from(jwt: ValidatedJwt) -> Result<Self, Self::Error> {
        let status_list_jwt = serde_json::from_value::<Self>(jwt.claims)?;
        Ok(status_list_jwt)
    }
}

/// The `status_list` claim of the [`StatusListJwt`]
#[derive(Debug, Deserialize, PartialEq)]
struct StatusListClaim {
    bits: u8,
    #[serde(alias = "lst")]
    list: String,
}

impl TryFrom<StatusListJwt> for StatusList {
    type Error = ParseStatusListError;

    fn try_from(jwt: StatusListJwt) -> Result<Self, Self::Error> {
        Self::parse(&jwt.status_list.list, jwt.status_list.bits)
    }
}

impl StatusListJwt {
    /// Parses the list and keeps its expiry next to it, so the two are replaced
    /// together. `leeway_secs` is the clock-skew leeway of the validator that
    /// checked the JWT, so `exp` gets the same leeway as `nbf`.
    pub(super) fn into_cached(
        self,
        leeway_secs: u64,
    ) -> Result<CachedStatusList, ParseStatusListError> {
        let expiry = self.exp.map(|exp| StatusListExpiry { exp, leeway_secs });
        Ok(CachedStatusList {
            list: self.try_into()?,
            expiry,
        })
    }
}

/// When a status list stops being usable: its `exp` plus the clock-skew leeway
/// it was accepted with. After that it "MUST NOT be relied upon any longer"
/// ([draft-ietf-oauth-status-list-21 §13.7]).
///
/// [draft-ietf-oauth-status-list-21 §13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
#[derive(Debug, PartialEq, Clone, Copy)]
pub struct StatusListExpiry {
    exp: u64,
    leeway_secs: u64,
}

impl StatusListExpiry {
    /// The last second, in Unix time, at which the list may still be used.
    fn usable_until(self) -> u64 {
        self.exp.saturating_add(self.leeway_secs)
    }

    pub(super) fn has_passed(self, now: u64) -> bool {
        now > self.usable_until()
    }

    /// Time from `now` until `exp`, when the issuer expects a newer list to be
    /// fetched; zero once it has passed.
    pub(super) fn until_exp(self, now: u64) -> Duration {
        Duration::from_secs(self.exp.saturating_sub(now))
    }
}

impl Display for StatusListExpiry {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "`exp` {} plus {} s of clock-skew leeway",
            UnixTime(self.exp),
            self.leeway_secs
        )
    }
}

/// A Unix time in seconds, shown as an RFC 3339 date for the log.
#[derive(Debug, PartialEq, Clone, Copy)]
pub(super) struct UnixTime(u64);

impl Display for UnixTime {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match i64::try_from(self.0)
            .ok()
            .and_then(|secs| DateTime::<Utc>::from_timestamp(secs, 0))
        {
            Some(date) => write!(f, "{}", date.to_rfc3339_opts(SecondsFormat::Secs, true)),
            None => write!(f, "{}", self.0),
        }
    }
}

/// A status list Cedarling could not keep: the last refresh failed and the
/// cached list, if any, was dropped.
#[derive(Debug, Clone)]
pub struct UnavailableStatusList {
    since: UnixTime,
    cause: Arc<UpdateStatusListError>,
}

impl UnavailableStatusList {
    pub(super) fn new(since: u64, cause: Arc<UpdateStatusListError>) -> Self {
        Self {
            since: UnixTime(since),
            cause,
        }
    }

    /// When the list became unavailable.
    pub(super) fn since(&self) -> UnixTime {
        self.since
    }

    /// The most recent failure to fetch or validate the list.
    pub(super) fn cause(&self) -> &UpdateStatusListError {
        &self.cause
    }

    /// Records a later failure, keeping the time the list became unavailable.
    pub(super) fn with_cause(&self, cause: Arc<UpdateStatusListError>) -> Self {
        Self {
            since: self.since,
            cause,
        }
    }
}

/// What Cedarling holds for one status list URI.
#[derive(Debug, Clone)]
pub(super) enum StatusListState {
    Usable(CachedStatusList),
    Unavailable(UnavailableStatusList),
}

#[cfg(test)]
impl StatusListState {
    pub(super) fn as_usable(&self) -> Option<&CachedStatusList> {
        match self {
            Self::Usable(list) => Some(list),
            Self::Unavailable(_) => None,
        }
    }
}

/// A parsed status list as Cedarling caches it, with the expiry of the JWT it
/// came from.
#[derive(Debug, PartialEq, Clone)]
pub(super) struct CachedStatusList {
    list: StatusList,
    /// `None` when the JWT has no `exp`, which is RECOMMENDED but not required.
    expiry: Option<StatusListExpiry>,
}

impl CachedStatusList {
    pub(super) fn expiry(&self) -> Option<StatusListExpiry> {
        self.expiry
    }

    /// Returns the status at `index`, or an error once the list has expired:
    /// an expired list gives no statement about the token
    /// ([draft-ietf-oauth-status-list-21 §8.3] step 4).
    ///
    /// [draft-ietf-oauth-status-list-21 §8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
    fn status_at(&self, index: usize, now: u64) -> Result<JwtStatus, StatusAtError> {
        if let Some(expiry) = self.expiry.filter(|expiry| expiry.has_passed(now)) {
            return Err(StatusAtError::Expired(expiry));
        }
        Ok(self.list.get_status(index)?)
    }
}

/// A list that never expires, for tests that check statuses only.
#[cfg(test)]
impl From<StatusList> for CachedStatusList {
    fn from(list: StatusList) -> Self {
        Self { list, expiry: None }
    }
}

/// A status value from a status list, as [draft-ietf-oauth-status-list-21 §7.1]
/// defines them.
///
/// [draft-ietf-oauth-status-list-21 §7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
#[derive(Debug, PartialEq)]
pub enum JwtStatus {
    /// `0x00`
    Valid,
    /// `0x01`: the token is revoked.
    Invalid,
    /// `0x02`: the token is temporarily invalid.
    Suspended,
    /// `0x03` or `0x0C` to `0x0F`, whose processing the draft leaves to each
    /// application. Cedarling gives them no meaning, so it can make no statement
    /// about the token.
    ApplicationSpecific(u8),
    /// Any other value, which the draft reserves for future registration.
    Reserved(u8),
}

impl JwtStatus {
    /// Only `VALID` lets the token through: for any other value, including one
    /// Cedarling does not know, no statement about the token can be made and it
    /// is rejected ([draft-ietf-oauth-status-list-21 §8.3]).
    ///
    /// [draft-ietf-oauth-status-list-21 §8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
    fn is_valid(&self) -> bool {
        matches!(self, JwtStatus::Valid)
    }
}

impl Display for JwtStatus {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            JwtStatus::Valid => write!(f, "VALID (0x00)"),
            JwtStatus::Invalid => write!(f, "INVALID (0x01): the token is revoked"),
            JwtStatus::Suspended => {
                write!(f, "SUSPENDED (0x02): the token is temporarily invalid")
            },
            JwtStatus::ApplicationSpecific(status) => write!(
                f,
                "{status:#04x}, an application-specific status that Cedarling does not support"
            ),
            JwtStatus::Reserved(status) => write!(
                f,
                "{status:#04x}, a value the status list specification reserves for future \
                 registration"
            ),
        }
    }
}

impl From<u8> for JwtStatus {
    fn from(value: u8) -> Self {
        match value {
            0x00 => Self::Valid,
            0x01 => Self::Invalid,
            0x02 => Self::Suspended,
            0x03 | 0x0C..=0x0F => Self::ApplicationSpecific(value),
            _ => Self::Reserved(value),
        }
    }
}

/// The number of bits that encode one status: the `bits` of a status list, which
/// is 1, 2, 4 or 8 ([draft-ietf-oauth-status-list-21 §4.1]).
///
/// [draft-ietf-oauth-status-list-21 §4.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-4.1
#[derive(Debug, PartialEq, Clone, Copy)]
pub(crate) enum StatusBitSize {
    One,
    Two,
    Four,
    Eight,
}

impl StatusBitSize {
    fn bits(self) -> u8 {
        match self {
            Self::One => 1,
            Self::Two => 2,
            Self::Four => 4,
            Self::Eight => 8,
        }
    }

    fn statuses_per_byte(self) -> usize {
        usize::from(8 / self.bits())
    }

    /// The status at `slot` of `byte`, counting from the least significant bits
    /// ([draft-ietf-oauth-status-list-21 §4.1]).
    ///
    /// [draft-ietf-oauth-status-list-21 §4.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-4.1
    fn status_in_byte(self, byte: u8, slot: usize) -> u8 {
        let bits = self.bits();
        let mask = u8::MAX >> (8 - bits);
        // `slot` is below `statuses_per_byte`, so the shift stays below 8.
        let shift = slot * usize::from(bits);
        (byte >> shift) & mask
    }
}

impl From<StatusBitSize> for u8 {
    fn from(value: StatusBitSize) -> Self {
        value.bits()
    }
}

impl TryFrom<u8> for StatusBitSize {
    type Error = ParseStatusListError;

    fn try_from(bits: u8) -> Result<Self, Self::Error> {
        match bits {
            1 => Ok(Self::One),
            2 => Ok(Self::Two),
            4 => Ok(Self::Four),
            8 => Ok(Self::Eight),
            bits => Err(ParseStatusListError::InvalidBitSize(bits)),
        }
    }
}

/// compresses and encodes a status list into a string you would expect to find
/// in the statuslist JWT's `lst` claim. See example in the [`status list spec sec. 5.1 v10`]
///
/// [`status list spec sec. 5.1 v10`]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-10.html#section-5.1
#[cfg(test)]
pub(super) fn compress_and_encode(status_list: &[u8]) -> String {
    use flate2::{Compression, write::ZlibEncoder};
    use std::io::Write;

    let mut encoder = ZlibEncoder::new(Vec::new(), Compression::best());
    encoder.write_all(status_list).expect("compress data");
    let compressed = encoder.finish().expect("finish compression");
    BASE64_URL_SAFE_NO_PAD.encode(compressed)
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::jwt::test_utils::MockServer;
    use jsonwebtoken::{Algorithm, Validation, decode};
    use test_utils::assert_eq;

    /// The status value ranges of [draft-ietf-oauth-status-list-21 §7.1], checked
    /// at each boundary.
    ///
    /// [draft-ietf-oauth-status-list-21 §7.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-7.1
    #[test]
    fn status_values_follow_draft_21_ranges() {
        let cases = [
            (0x00, JwtStatus::Valid),
            (0x01, JwtStatus::Invalid),
            (0x02, JwtStatus::Suspended),
            (0x03, JwtStatus::ApplicationSpecific(0x03)),
            (0x04, JwtStatus::Reserved(0x04)),
            (0x0B, JwtStatus::Reserved(0x0B)),
            (0x0C, JwtStatus::ApplicationSpecific(0x0C)),
            (0x0F, JwtStatus::ApplicationSpecific(0x0F)),
            (0x10, JwtStatus::Reserved(0x10)),
            (0xFF, JwtStatus::Reserved(0xFF)),
        ];
        for (value, expected) in cases {
            assert_eq!(
                JwtStatus::from(value),
                expected,
                "status value {value:#04x} is classified by draft-21 section 7.1"
            );
        }
    }

    /// Only `VALID` lets a token through; any other value gives no statement
    /// about the token, which is then rejected ([draft-ietf-oauth-status-list-21 §8.3]).
    ///
    /// [draft-ietf-oauth-status-list-21 §8.3]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-8.3
    #[test]
    fn only_valid_status_is_valid() {
        assert!(
            JwtStatus::Valid.is_valid(),
            "VALID must let the token through"
        );
        for value in 0x01..=0xFF {
            assert!(
                !JwtStatus::from(value).is_valid(),
                "status value {value:#04x} must reject the token"
            );
        }
    }

    /// A cached list "MUST NOT be relied upon any longer" once its `exp` passes
    /// ([draft-ietf-oauth-status-list-21 §13.7]); the same clock-skew leeway as
    /// at fetch applies. A list without `exp` does not expire ([§5.1]).
    ///
    /// [draft-ietf-oauth-status-list-21 §13.7]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-13.7
    /// [§5.1]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-21.html#section-5.1
    #[test]
    fn cached_list_expires_after_exp_plus_leeway() {
        let list = StatusList {
            bit_size: 1u8.try_into().expect("valid bit size"),
            list: vec![0],
        };
        let expiry = StatusListExpiry {
            exp: 1_000,
            leeway_secs: 60,
        };
        let cached = CachedStatusList {
            list: list.clone(),
            expiry: Some(expiry),
        };

        let status = cached
            .status_at(0, 1_060)
            .expect("the list is usable through `exp` plus leeway");
        assert_eq!(
            status,
            JwtStatus::Valid,
            "index 0 holds VALID while the list is usable"
        );

        let err = cached
            .status_at(0, 1_061)
            .expect_err("the list has expired one second after `exp` plus leeway");
        assert!(
            matches!(err, StatusAtError::Expired(found) if found == expiry),
            "an expired list must report its expiry, got: {err:?}"
        );

        CachedStatusList::from(list)
            .status_at(0, u64::MAX)
            .expect("a list without `exp` never expires");
    }

    /// The admin reads times in the log as dates; a value past what a date can
    /// hold is shown as the raw number rather than lost.
    #[test]
    fn unix_time_reads_as_a_date() {
        assert_eq!(
            UnixTime(1_767_225_600).to_string(),
            "2026-01-01T00:00:00Z",
            "a Unix time is shown as an RFC 3339 date in UTC"
        );
        assert_eq!(
            UnixTime(u64::MAX).to_string(),
            u64::MAX.to_string(),
            "a time no date can hold is shown as the number"
        );
    }

    #[test]
    fn test_status_list_decompression() {
        let cases = [
            (
                vec![
                    0b1011_1001, // 0xb9
                    0b1010_0011, // 0xa3
                ],
                "eNrbuRgAAhcBXQ",
            ),
            (
                vec![
                    0b1100_1001, // 0xc9
                    0b0100_0100, // 0x44
                    0b1111_1001, // 0xf9
                ],
                "eNo76fITAAPfAgc",
            ),
        ];

        for (i, (list, expected_compressed)) in cases.into_iter().enumerate() {
            let compressed = compress_and_encode(&list);
            assert_eq!(
                compressed, expected_compressed,
                "the status list was not compessed correctly for case {i}"
            );

            let status_list = StatusList::parse(&compressed, 1).unwrap();

            assert_eq!(
                status_list.list.as_slice(),
                &list,
                "the status list was not parsed correctly for case {i}"
            );
        }
    }

    #[test]
    fn errors_on_out_of_bounds() {
        let list_with_4_statuses = StatusList {
            bit_size: 2.try_into().unwrap(),
            list: [0b1000_0000].to_vec(),
        };

        // base case
        let result = list_with_4_statuses
            .get_status(3)
            .expect("shouldn't error when getting the last status");
        assert_eq!(result, JwtStatus::Suspended);

        // error case
        list_with_4_statuses
            .get_status(4)
            .expect_err("should error when getting the last status");
    }

    #[tokio::test]
    async fn deserialize_status_list_from_jwt() {
        let bits = StatusBitSize::try_from(2).unwrap();
        let lst = [0b0110_0011];

        let mut server = MockServer::new_with_defaults()
            .await
            .expect("initialize mock server");
        server.generate_status_list_endpoint(bits, &lst, None);

        let status_list_jwt = server.status_list_jwt().await.unwrap();

        let mut validation = Validation::new(Algorithm::HS256);
        validation.validate_exp = false;

        let decoding_key = server.jwt_decoding_key().unwrap();
        let status_list = decode::<StatusListJwt>(&status_list_jwt, &decoding_key, &validation)
            .expect("decode status list JWT")
            .claims;

        assert_eq!(
            status_list.sub,
            server.status_list_endpoint().unwrap().to_string()
        );
        assert_eq!(status_list.ttl, Some(300));
        assert_eq!(
            status_list.status_list,
            StatusListClaim {
                bits: bits.into(),
                list: compress_and_encode(&lst)
            }
        );
    }
    #[test]
    fn prevent_zip_bomb() {
        let size = 10 * 1024 * 1024 + 1; // 10 MB + 1 byte
        let malicious_payload = vec![0u8; size];
        let encoded = compress_and_encode(&malicious_payload);

        let result = StatusList::parse(&encoded, 1);
        assert!(
            matches!(
                result,
                Err(ParseStatusListError::DecompressedSizeExceeded(_))
            ),
            "Oversized payload must return DecompressedSizeExceeded, but got {result:?}"
        );
    }
}
