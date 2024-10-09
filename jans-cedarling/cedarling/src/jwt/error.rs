use std::string::FromUtf8Error;

/// Error type for JWT decoding
#[derive(thiserror::Error, Debug)]
pub enum DecodeJwtError {
    /// JWT is malformed
    #[error("Malformed JWT provided")]
    MalformedJWT,

    /// Base64 value in payload is malformed
    #[error("Unable to decode base64 JWT value: {0}, payload: {1}")]
    UnableToDecodeBase64(base64::DecodeError, String),

    /// Unmarshaled JWT payload base64 value is not valid UTF-8
    #[error("Unable to convert decoded base64 JWT value to string: {0}")]
    UnableToString(#[from] FromUtf8Error),

    /// Unable to parse json in unmarshaled JWT payload string value
    #[error("Unable to parse JWT JSON data: {0}, payload: {1}")]
    UnableToParseJson(serde_json::Error, String),
}
