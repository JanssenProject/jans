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
mod error;
#[cfg(test)]
mod ietf_test_samples;

pub use cache::*;
pub use error::*;

use super::validation::ValidatedJwt;
use base64::prelude::{BASE64_URL_SAFE_NO_PAD, Engine};
use flate2::read::ZlibDecoder;
use serde::Deserialize;
use std::fmt::Display;
use std::io::Read;

#[derive(Debug, PartialEq, Clone)]
pub struct StatusList {
    /// The number of bits used to encode a single status
    pub bit_size: StatusBitSize,
    /// The status list
    pub list: Vec<u8>,
}

impl StatusList {
    pub fn parse(encoded: &str, bits: u8) -> Result<Self, ParseStatusListError> {
        let list = BASE64_URL_SAFE_NO_PAD.decode(encoded)?;
        let mut decoder = ZlibDecoder::new(list.as_slice());
        let mut list = Vec::new();
        decoder.read_to_end(&mut list)?;
        Ok(Self {
            bit_size: bits.try_into()?,
            list,
        })
    }

    /// Returns the status of the jwt at the given index (`idx`)
    ///
    /// Validation rules can be found in [`IETF Status List Spec. sec. 8.3 v10`]
    ///
    /// [`IETF Status List Spec. sec. 8.3 v10`]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-10.html#section-8.3
    pub fn get_status(&self, index: usize) -> Result<JwtStatus, JwtStatusError> {
        let scale = (8 / self.bit_size.0) as usize;

        let byte_idx = index / scale;
        let byte = self
            .list
            .get(byte_idx)
            .ok_or(JwtStatusError::StatusListIdxOutOfBounds)?;

        let bit_idx = (index % scale) as u8;

        let status = get_status_from_byte(*byte, self.bit_size, bit_idx);

        Ok(status.into())
    }
}

/// Status list JWT from an IDP's status list endpoint
#[derive(Debug, Deserialize, PartialEq)]
pub struct StatusListJwtStr(pub String);

impl StatusListJwtStr {
    pub fn new(jwt_str: String) -> Self {
        Self(jwt_str)
    }
}

/// Status list JWT from an IDP's status list endpoint
#[derive(Debug, Deserialize, PartialEq)]
pub struct StatusListJwt {
    sub: String,
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

impl TryFrom<ValidatedJwt> for StatusList {
    type Error = ParseStatusListError;

    fn try_from(jwt: ValidatedJwt) -> Result<Self, Self::Error> {
        let list = jwt
            .claims
            .get("lst")
            .ok_or(ParseStatusListError::JwtMissingListClaim)?
            .to_string();

        let bits = jwt
            .claims
            .get("bits")
            .ok_or(ParseStatusListError::JwtMissingBitsClaim)?;
        let bits = bits
            .as_u64()
            .ok_or(ParseStatusListError::JwtInvalidBitsType(bits.clone()))?;

        Self::parse(&list, bits as u8)
    }
}

/// Retrieves the status byte from the given byte
///
/// # Panics
///
/// This function panics if the `bit_size` is invalid... though that shouldn't happen
/// if [`StatusBitSize`] is initialized properly.
fn get_status_from_byte(byte: u8, bit_size: StatusBitSize, bit_idx: u8) -> u8 {
    let bit_size = bit_size.0;
    let mask = match bit_size {
        1 => 0b0000_0001,
        2 => 0b0000_0011,
        4 => 0b0000_1111,
        8 => return byte,
        _ => unimplemented!(
            "status bit size can only be 1, 2, 4, or 8. See: https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/10/"
        ),
    };

    let offset = bit_idx * bit_size;

    (byte >> offset) & mask
}

/// See: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-10.html#name-status-types
#[derive(Debug, PartialEq)]
pub enum JwtStatus {
    Valid,
    Invalid,
    Suspended,
    /// Status Type values in the range 0x0B until 0x0F are permanently reserved as
    /// application specific.
    ///
    /// For Cedarling's, use-case we simply ignore these.
    Custom(u8),
}

impl JwtStatus {
    /// Helper function to figure out if the JWT is stull valid based on it's status
    pub fn is_valid(&self) -> bool {
        match self {
            JwtStatus::Valid | JwtStatus::Custom(_) => true,
            JwtStatus::Invalid | JwtStatus::Suspended => false,
        }
    }
}

impl Display for JwtStatus {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            JwtStatus::Valid => write!(f, "valid"),
            JwtStatus::Invalid => write!(f, "invalid"),
            JwtStatus::Suspended => write!(f, "suspended"),
            JwtStatus::Custom(status) => write!(f, "custom ({status})"),
        }
    }
}

impl From<u8> for JwtStatus {
    fn from(value: u8) -> Self {
        match value {
            0 => Self::Valid,
            1 => Self::Invalid,
            2 => Self::Suspended,
            _ => Self::Custom(value),
        }
    }
}

/// Newtype that enforces valid status bit size.
///
/// Use [`StatusBitSize::try_from<u8>`] to initialize.
#[derive(Debug, PartialEq, Clone, Copy)]
pub struct StatusBitSize(u8);

impl From<StatusBitSize> for u8 {
    fn from(value: StatusBitSize) -> Self {
        value.0
    }
}

impl TryFrom<u8> for StatusBitSize {
    type Error = ParseStatusListError;

    fn try_from(bit: u8) -> Result<Self, Self::Error> {
        match bit {
            1 | 2 | 4 | 8 => Ok(Self(bit)),
            bit => Err(ParseStatusListError::InvalidBitSize(bit)),
        }
    }
}

/// compresses and encodes a status list into a string you would expect to find
/// in the statuslist JWT's `lst` claim. See example in the [`status list spec sec. 5.1 v10`]
///
/// [`status list spec sec. 5.1 v10`]: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-10.html#section-5.1
#[cfg(test)]
pub fn compress_and_encode(status_list: &[u8]) -> String {
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

    #[test]
    fn test_status_list_decompression() {
        let cases = [
            (
                vec![
                    0b10111001, // 0xb9
                    0b10100011, // 0xa3
                ],
                "eNrbuRgAAhcBXQ",
            ),
            (
                vec![
                    0b11001001, // 0xc9
                    0b01000100, // 0x44
                    0b11111001, // 0xf9
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
        assert_eq!(status_list.ttl, Some(600));
        assert_eq!(
            status_list.status_list,
            StatusListClaim {
                bits: bits.into(),
                list: compress_and_encode(&lst)
            }
        );
    }
}
