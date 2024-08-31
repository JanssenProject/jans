use base64::prelude::*;
use cedar_policy::Policy;
use serde::de::Error as SerdeError;
use std::collections::BTreeMap;

pub(crate) type TrustedIssuers = BTreeMap<String, TrustedIssuer>;

#[derive(Debug, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PolicyStoreEntry {
	#[serde(deserialize_with = "parse_schema")]
	pub schema: cedar_policy::Schema,
	pub trusted_issuers: TrustedIssuers,
	#[serde(deserialize_with = "parse_policies")]
	pub policies: cedar_policy::PolicySet,
}

fn parse_schema<'de, D>(deserializer: D) -> Result<cedar_policy::Schema, D::Error>
where
	D: serde::Deserializer<'de>,
{
	let source = <String as serde::Deserialize>::deserialize(deserializer)?;
	let decoded_result: Result<Vec<u8>, D::Error> =
		BASE64_STANDARD.decode(source.as_str()).map_err(|err| {
			serde::de::Error::custom(format!(
				"unable to parse Schema source as valid base64: {}, data: {}",
				err.to_string(),
				&source,
			))
		});
	let decoded = match decoded_result {
		Ok(v) => v,
		//if we failed try to decode with NO_PAD
		Err(origin_err) => BASE64_STANDARD_NO_PAD
			.decode(source.as_str())
			.map_err(|_err| origin_err)?,
	};

	let (schema, warnings) = cedar_policy::Schema::from_cedarschema_file(decoded.as_slice())
		.map_err(|err| {
			serde::de::Error::custom(format!(
				"unable to parse Schema in Human Readable cedar format: {}",
				err.to_string()
			))
		})?;

	for warning in warnings {
		log::warn!("Schema Parser generated warning: {:?}", warning);
	}

	Ok(schema)
}

fn parse_policies<'de, D>(deserializer: D) -> Result<cedar_policy::PolicySet, D::Error>
where
	D: serde::Deserializer<'de>,
{
	let policies = <BTreeMap<String, String> as serde::Deserialize>::deserialize(deserializer)?;

	let policies = policies
		.into_iter()
		.map(|(id, s)| {
			BASE64_STANDARD
				.decode(s)
				.map_err(|err| {
					SerdeError::custom(format!(
						"unable to parse Policy source as valid base64: {}",
						err
					))
				})
				.and_then(|decoded| {
					String::from_utf8(decoded).map_err(|err| {
						SerdeError::custom(format!(
							"unable to convert decoded base64 to UTF-8 string: {}",
							err
						))
					})
				})
				.and_then(|policy_src| {
					Policy::parse(Some(id), policy_src).map_err(|err| {
						SerdeError::custom(format!("unable to parse Policy from string: {}", err))
					})
				})
		})
		.collect::<Result<Vec<Policy>, D::Error>>()?;

	Ok(cedar_policy::PolicySet::from_policies(policies).unwrap())
}

#[allow(dead_code)]
#[derive(serde::Deserialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]

pub struct TrustedIssuer {
	pub name: Option<String>,
	pub openid_configuration_endpoint: String,

	pub access_tokens: AccessTokenConfig,
	pub id_tokens: IdTokenConfig,
	pub userinfo_tokens: UserInfoTokenConfig,
}

#[allow(dead_code)]
#[derive(serde::Deserialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct AccessTokenConfig {
	pub trusted: bool,
}

#[allow(dead_code)]
#[derive(serde::Deserialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct IdTokenConfig {
	pub trusted: bool,
	pub principal_identifier: Option<String>,
}

#[allow(dead_code)]
#[derive(serde::Deserialize, Debug, Clone)]
#[serde(rename_all = "camelCase")]
pub struct UserInfoTokenConfig {
	pub trusted: bool,
	pub role_mapping: Option<String>,
}
