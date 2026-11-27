// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use base64::{Engine, prelude::BASE64_URL_SAFE_NO_PAD};
use jsonwebtoken::Algorithm;
use serde::Deserialize;
use serde_json::Value;

/// A decoded but not validated JWT.
///
/// We need to decode the header and the `iss` claim in order to fetch the right
/// validation key.
#[derive(Debug, PartialEq)]
pub(crate) struct DecodedJwt {
    pub header: DecodedJwtHeader,
    pub claims: DecodedJwtClaims,
}

/// See [`RFC 7515 Section 4.1`] for the registered header parameter names.
///
/// [`RFC 7515 Section 4.1`]: https://datatracker.ietf.org/doc/html/rfc7515#section-4.1
#[derive(Debug, Deserialize, PartialEq)]
pub(crate) struct DecodedJwtHeader {
    pub typ: Option<String>,
    pub cty: Option<String>,
    pub kid: Option<String>,
    pub alg: Algorithm,
}

/// See [`RFC 7419 Section 4.1`] for the registered claim names.
///
/// [`RFC 7515 Section 4.1`]: https://datatracker.ietf.org/doc/html/rfc7519#section-4.1
#[derive(Debug, Deserialize, PartialEq)]
pub(crate) struct DecodedJwtClaims {
    #[serde(flatten)]
    pub inner: Value,
}

pub(crate) fn decode_jwt(jwt: &str) -> Result<DecodedJwt, DecodeJwtError> {
    let parts = EncodedJwtParts::split(jwt)?;
    let header = decode_header_part(parts.header)?;

    let decoded_claims = BASE64_URL_SAFE_NO_PAD
        .decode(parts.claims)
        .map_err(DecodeJwtError::DecodeClaims)?;
    let claims = serde_json::from_slice::<DecodedJwtClaims>(&decoded_claims)
        .map_err(DecodeJwtError::DeserializeClaims)?;

    Ok(DecodedJwt { header, claims })
}

/// Decodes only the header, for a caller whose validator decodes the claims.
pub(crate) fn decode_jwt_header(jwt: &str) -> Result<DecodedJwtHeader, DecodeJwtError> {
    decode_header_part(EncodedJwtParts::split(jwt)?.header)
}

/// The encoded header and claims of a JWT in the form `header.payload.signature`.
struct EncodedJwtParts<'a> {
    header: &'a str,
    claims: &'a str,
}

impl<'a> EncodedJwtParts<'a> {
    fn split(jwt: &'a str) -> Result<Self, DecodeJwtError> {
        let mut parts = jwt.split('.');
        match (parts.next(), parts.next(), parts.next(), parts.next()) {
            (Some(header), Some(claims), Some(_signature), None) => Ok(Self { header, claims }),
            _ => Err(DecodeJwtError::InvalidJwtFormat),
        }
    }
}

fn decode_header_part(encoded_header: &str) -> Result<DecodedJwtHeader, DecodeJwtError> {
    let decoded_header = BASE64_URL_SAFE_NO_PAD
        .decode(encoded_header)
        .map_err(DecodeJwtError::DecodeHeader)?;
    serde_json::from_slice::<DecodedJwtHeader>(&decoded_header)
        .map_err(DecodeJwtError::DeserializeHeader)
}

#[derive(Debug, thiserror::Error)]
pub enum DecodeJwtError {
    #[error("the JWT is not in the form 'header.payload.signature'")]
    InvalidJwtFormat,
    #[error("error while decoding the JWT's header: {0}")]
    DecodeHeader(#[source] base64::DecodeError),
    #[error("error while deserializing the JWT's header: {0}")]
    DeserializeHeader(#[source] serde_json::Error),
    #[error("error while decoding the JWT's claims: {0}")]
    DecodeClaims(#[source] base64::DecodeError),
    #[error("error while deserializing the JWT's claims: {0}")]
    DeserializeClaims(#[source] serde_json::Error),
}

#[cfg(test)]
mod test {
    use super::*;
    use serde_json::json;
    use test_utils::assert_eq;

    #[test]
    fn can_decode_jwt() {
        let jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        let jwt = decode_jwt(jwt).expect("decode jwt successfully");

        assert_eq!(
            jwt,
            DecodedJwt {
                header: DecodedJwtHeader {
                    typ: Some("JWT".into()),
                    alg: Algorithm::HS256,
                    cty: None,
                    kid: None,
                },
                claims: DecodedJwtClaims {
                    inner: json!({
                        "sub": "1234567890",
                        "name": "John Doe",
                        "iat": 1_516_239_022,
                    })
                }
            }
        );
    }

    #[test]
    fn errors_on_invalid_jwt_format() {
        let cases = [
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c.e30",
        ];

        for (i, jwt) in cases.into_iter().enumerate() {
            let err = decode_jwt(jwt).expect_err("should error while decoding jwt");
            assert!(
                matches!(err, DecodeJwtError::InvalidJwtFormat),
                "failed assertion in case: {i}"
            );

            let err = decode_jwt_header(jwt).expect_err("should error while decoding the header");
            assert!(
                matches!(err, DecodeJwtError::InvalidJwtFormat),
                "a header is only read from a JWT in the form header.payload.signature, case: {i}"
            );
        }
    }

    /// The header is read without the claims, which the validator decodes later.
    #[test]
    fn decodes_header_without_claims() {
        let jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.not-base64!.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        let header = decode_jwt_header(jwt).expect("the header decodes on its own");

        assert_eq!(
            header,
            DecodedJwtHeader {
                typ: Some("JWT".into()),
                alg: Algorithm::HS256,
                cty: None,
                kid: None,
            },
            "the header is decoded even though the claims are not"
        );
    }
}
