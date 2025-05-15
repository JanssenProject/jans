// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::io::Read;

use crate::jwt::validator::ProcessedJwt;

use super::ParseStatusListError;
use base64::prelude::{BASE64_URL_SAFE_NO_PAD, Engine};
use flate2::read::ZlibDecoder;
use serde::Deserialize;

#[derive(Debug, PartialEq)]
pub struct StatusList {
    /// The number of bits used to encode a single status
    bit_size: StatusBitSize,
    /// The status list
    list: Vec<u8>,
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

    // TODO: check for out of bounds
    pub fn get_status(&self, index: usize) -> JwtStatus {
        let scale = (8 / self.bit_size.0) as usize;

        let byte_idx = index / scale;
        let byte = self.list.get(byte_idx).unwrap();

        let bit_idx = (index % scale) as u8;

        let status = get_status_from_byte(*byte, self.bit_size.0, bit_idx);

        status.into()
    }
}

/// Status list JWT from an IDP's status list endpoint
#[derive(Debug, Deserialize, PartialEq)]
pub struct StatusListJwt {
    sub: String,
    iat: u32,
    #[serde(default)]
    exp: Option<u32>,
    #[serde(default)]
    ttl: Option<u32>,
    status_list: StatusListClaim,
    aggregation_uri: Option<String>,
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

impl TryFrom<ProcessedJwt<'_>> for StatusList {
    type Error = ParseStatusListError;

    fn try_from(jwt: ProcessedJwt) -> Result<Self, Self::Error> {
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

fn get_status_from_byte(byte: u8, bit_size: u8, bit_idx: u8) -> u8 {
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
    println!("offset: {offset}");
    let status = (byte >> offset) & mask;

    status
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
struct StatusBitSize(u8);

impl TryFrom<u8> for StatusBitSize {
    type Error = ParseStatusListError;

    fn try_from(bit: u8) -> Result<Self, Self::Error> {
        match bit {
            1 | 2 | 4 | 8 => Ok(Self(bit)),
            bit => Err(ParseStatusListError::InvalidBitSize(bit)),
        }
    }
}

#[cfg(test)]
mod test {
    use std::io::Write;

    use crate::jwt::test_utils::{MockServer, TokenTypeHeader};

    use super::*;
    use flate2::{Compression, write::ZlibEncoder};
    use jsonwebtoken::{Algorithm, Validation, decode};
    use serde_json::json;
    use test_utils::assert_eq;

    fn compress_and_encode(data: &[u8]) -> String {
        let mut encoder = ZlibEncoder::new(Vec::new(), Compression::best());
        encoder.write_all(data).expect("compress data");
        let compressed = encoder.finish().expect("finish compression");
        BASE64_URL_SAFE_NO_PAD.encode(compressed)
    }

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

    #[tokio::test]
    async fn test_status_list_jwt_deserialization() {
        let mut server = MockServer::new_with_defaults()
            .await
            .expect("initialize mock server");

        let status_list_jwt = server
            .generate_token_string_hs256(
                TokenTypeHeader::StatusListJwt,
                &json!({
                    "sub": "https://demoexample.jans.io/jans-auth/restv1/status_list",
                    "nbf": 1745295186,
                    "status_list": {
                      "bits": 2,
                      "lst": "eNoDAAAAAAE"
                    },
                    "iss": "https://demoexample.jans.io",
                    "exp": 1745295786,
                    "iat": 1745295186,
                    "ttl": 600
                }),
            )
            .expect("create status list jwt");

        let mut validation = Validation::new(Algorithm::HS256);
        validation.validate_exp = false;

        let decoding_key = server.jwt_decoding_key().unwrap();
        let status_list = decode::<StatusListJwt>(&status_list_jwt, &decoding_key, &validation)
            .expect("decode status list JWT")
            .claims;

        assert_eq!(status_list, StatusListJwt {
            sub: "https://demoexample.jans.io/jans-auth/restv1/status_list".into(),
            iat: 1745295186,
            exp: Some(1745295786),
            ttl: Some(600),
            status_list: StatusListClaim {
                bits: 2,
                list: "eNoDAAAAAAE".into(),
            },
            aggregation_uri: None,
        });
    }
}
