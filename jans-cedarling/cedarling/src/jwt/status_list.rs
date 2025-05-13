// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The JWT status list validation is implemented as described in this [IETF spec](https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-02.html#name-referenced-token).

mod error;
#[cfg(test)]
mod ietf_test_samples;

pub use error::*;

use base64::prelude::{BASE64_URL_SAFE_NO_PAD, Engine};
use flate2::read::ZlibDecoder;
use serde::Deserialize;
use std::io::Read;

struct StatusList {
    /// The number of bits used to encode a single status
    bit_size: StatusBitSize,
    /// The status list
    list: Vec<u8>,
}

#[derive(Clone, Copy)]
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

/// See: https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-10.html#name-status-types
#[derive(Debug, PartialEq)]
enum StatusKind {
    Valid,
    Invalid,
    Suspended,
    /// Status Type values in the range 0x0B until 0x0F are permanently reserved as
    /// application specific.
    ///
    /// For Cedarling's, use-case we simply ignore these.
    Custom(u8),
}

impl From<u8> for StatusKind {
    fn from(value: u8) -> Self {
        match value {
            0 => Self::Valid,
            1 => Self::Invalid,
            2 => Self::Suspended,
            _ => Self::Custom(value),
        }
    }
}

#[derive(Debug, Deserialize, PartialEq)]
struct StatusListJwt {
    sub: String,
    iat: u32,
    #[serde(default)]
    exp: Option<u32>,
    #[serde(default)]
    ttl: Option<u32>,
    status_list: StatusListClaim,
    aggregation_uri: Option<String>,
}

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

    pub fn get_status(&self, index: usize) -> StatusKind {
        let scale = (8 / self.bit_size.0) as usize;

        let byte_idx = index / scale;
        let byte = self.list.get(byte_idx).unwrap();

        let bit_idx = (index % scale) as u8;

        let status = get_status_from_byte(*byte, self.bit_size.0, bit_idx);

        status.into()
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

#[cfg(test)]
mod test {
    use std::io::Write;

    use super::*;
    use flate2::{Compression, write::ZlibEncoder};
    use jsonwebtoken::{Algorithm, DecodingKey, Validation, decode, jwk::Jwk};
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

    #[test]
    fn test_status_list_jwt_deserialization() {
        let jwt = "eyJraWQiOiJjb25uZWN0X2M3ZTVkOTI1LWYwNzktNGMwYy05ZDViLTI2NmEwOGMyODM1Y19zaWdfcnMyNTYiLCJ0eXAiOiJzdGF0dXNsaXN0K2p3dCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJodHRwczovL2RlbW9leGFtcGxlLmphbnMuaW8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCIsIm5iZiI6MTc0NTI5NTE4Niwic3RhdHVzX2xpc3QiOnsiYml0cyI6MiwibHN0IjoiZU5vREFBQUFBQUUifSwiaXNzIjoiaHR0cHM6Ly9kZW1vZXhhbXBsZS5qYW5zLmlvIiwiZXhwIjoxNzQ1Mjk1Nzg2LCJpYXQiOjE3NDUyOTUxODYsInR0bCI6NjAwfQ.UfGieWUVa87Vj-kbs0r3OLjfYoK9krvCbFyGW6Qg9Cskwf2QlMtbDKhBa_AITPilSqPkqenVWsBE8ekPRlDE7DQWkaTM2imCuG27ho7LtEDcJyU3VPkScWfF_n3QqNtNMZLVl84qGzRfDd9uN_sV0W6FpZcQc7tapE7DpBRmfBL_xNugNM3D-tyW1Jh5hQo6HrjuHJtUpIVZLL434ACq78NTPL7RBXW13-Hmf740-7VMTyrmgZ1AQaUeZ6U56OCmzeseFC7q-E8pC7QDC39eG9sLkRLWClkdAYGjtufW31bgiubzXqCPDLHuyqCPn3bAOORxcg9xByae4Q1FSAt6AA";
        let jwk: Jwk = serde_json::from_value(json!({
            "kty": "RSA",
            "e": "AQAB",
            "use": "sig",
            "key_ops_type": [
                "connect"
            ],
            "kid": "connect_c7e5d925-f079-4c0c-9d5b-266a08c2835c_sig_rs256",
            "x5c": [
                "MIIDCTCCAfGgAwIBAgIgdgYsM0BXs0dQwwAOrGEOz4K+GaIpcPt+adIHB3K9y8QwDQYJKoZIhvcNAQELBQAwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yNTA0MjIwNDEwMzhaFw0yNTA0MjQwNDEwNDZaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCrfZ0qoQlTAMKkO3HzPmti0EaGUnMBZNPm/bHmHHD7wy79vI4jQdoOtlfZigQ+2FEqbwaqvvoRBQ9sY/UKt7CrqTSSmtPKFHFiJfGoaDWKdgjQdCadr9Jf7H2h6jhYbN9b92KYxUH+KdV0uRomBmIri0zjTy3d+WnsBAjhTfhUB0rSrKMorudzuDzixMzRRwAiddTU2AoCVg9RafHUgNp9yLgGnsoNtKmekmInH+TTydSVLH/JSH+L1F9yzIFVtsJd33iqhOGEYHpjJIOQYMR6DbFs62vecmlYaUAsTP152sE/Mi9cakoXTSKKC6ifFiV4U0ul5DpIIAbc7ywYVOcVAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQsFAAOCAQEAokW5zM3SuItETk3Aag/iK57+PXMxAkYjiwWfCNf70iY/TT+4cXdHR3ZDzpHzqmMhjn4FhcpsswlvO2e1qCoNuUddcsivFmIegBf/Y4SUuCgndjj4097PDjgog9DxZAlbuQZu8SY9bzQvrLFsamenP/PNfb6ZoLMY6cmXGfaUsVuJ20TxXUVEx38vKv1/w/E3+h6hLnDtuwoDA86WzM42pAmPNK+qtC6/bZDQxcV2QcS0X7JsBkdAMTmM5lmUZSVYRJrmQPRqXkYmbtlvv9+SOplCnF6iWhVXHNtXM70T04cGYhrqzoBgykTENO4O5o9E479kaxYfiIxsv/lmG3halQ=="
            ],
            "alg": "RS256",
            "n": "q32dKqEJUwDCpDtx8z5rYtBGhlJzAWTT5v2x5hxw-8Mu_byOI0HaDrZX2YoEPthRKm8Gqr76EQUPbGP1Crewq6k0kprTyhRxYiXxqGg1inYI0HQmna_SX-x9oeo4WGzfW_dimMVB_inVdLkaJgZiK4tM408t3flp7AQI4U34VAdK0qyjKK7nc7g84sTM0UcAInXU1NgKAlYPUWnx1IDafci4Bp7KDbSpnpJiJx_k08nUlSx_yUh_i9RfcsyBVbbCXd94qoThhGB6YySDkGDEeg2xbOtr3nJpWGlALEz9edrBPzIvXGpKF00iiguonxYleFNLpeQ6SCAG3O8sGFTnFQ"
        })).expect("deserialize Jwk");
        let key = DecodingKey::from_jwk(&jwk).expect("create decoding key");

        let mut validation = Validation::new(Algorithm::RS256);
        validation.sub = Some("https://demoexample.jans.io/jans-auth/restv1/status_list".into());
        validation.validate_exp = false;

        let status_list = decode::<StatusListJwt>(jwt, &key, &validation)
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
