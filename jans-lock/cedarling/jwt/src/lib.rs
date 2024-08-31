use std::string::FromUtf8Error;

use base64::prelude::*;

// TODO: implement
pub struct JWTValidationConfig {}

pub enum JWTDecoder {
	WithValidation(JWTValidationConfig),
	WithoutValidation,
}

impl JWTDecoder {
	pub fn new_without_validation() -> Self {
		Self::WithoutValidation
	}
}

impl JWTDecoder {
	pub fn decode<T: serde::de::DeserializeOwned>(&self, jwt: &str) -> Result<T, DecodeError> {
		match self {
			JWTDecoder::WithValidation(_config) => todo!(),
			JWTDecoder::WithoutValidation => decode_jwt_without_validation(jwt),
		}
	}
}

#[derive(thiserror::Error, Debug)]
pub enum DecodeError {
	#[error("Malformed JWT provided")]
	MalformedJWT,
	#[error("Unable to decode base64 JWT value: {0}, payload: {1}")]
	UnableToDecodeBase64(base64::DecodeError, String),
	#[error("Unable to convert decoded base64 JWT value to string: {0}")]
	UnableToString(#[from] FromUtf8Error),
	#[error("Unable to parse JWT JSON data: {0}, payload: {1}")]
	UnableToParseJson(serde_json::Error, String),
}

pub fn decode_jwt_without_validation<T: serde::de::DeserializeOwned>(
	jwt: &str,
) -> Result<T, DecodeError> {
	let payload_base64 = jwt.split('.').nth(1).ok_or(DecodeError::MalformedJWT)?;
	let payload_json = BASE64_STANDARD_NO_PAD
		.decode(payload_base64)
		.map_err(|err| DecodeError::UnableToDecodeBase64(err, payload_base64.to_owned()))?;

	let payload_json = String::from_utf8(payload_json)?;
	Ok(serde_json::from_str(payload_json.as_str())
		.map_err(|err| DecodeError::UnableToParseJson(err, payload_json.to_owned()))?)
}
