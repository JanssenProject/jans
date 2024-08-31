use cedar_policy::{
	Entity, EntityAttrEvaluationError, EntityId, EntityTypeName, EntityUid, RestrictedExpression,
};
use std::{
	collections::{HashMap, HashSet},
	str::FromStr,
};

#[derive(thiserror::Error, Debug)]

pub enum ParseEmailToExpError {
	#[error("email does not have user id")]
	DoesNoId,
	#[error("email does not have domain")]
	DoesNoDomain,
	#[error("could not create construction: {0}")]
	ExpConstruction(String),
}

pub fn email_exp(email_raw: &str) -> Result<RestrictedExpression, ParseEmailToExpError> {
	// create email dict
	let mut iter = email_raw.split('@');
	let record = [
		(
			"id".to_string(),
			RestrictedExpression::new_string(
				iter.next()
					.ok_or(ParseEmailToExpError::DoesNoId)?
					.to_string(),
			),
		),
		(
			"domain".to_string(),
			RestrictedExpression::new_string(
				iter.next()
					.ok_or(ParseEmailToExpError::DoesNoDomain)?
					.to_string(),
			),
		),
	];
	let result = RestrictedExpression::new_record(record)
		.map_err(|err| ParseEmailToExpError::ExpConstruction(err.to_string()))?;
	Ok(result)
}

#[derive(thiserror::Error, Debug)]
pub enum ParseURLToExpError {
	#[error("could not parse: {0}")]
	Parse(#[from] url::ParseError),
	#[error("does not have host")]
	NoHost,
	#[error("could not create construction: {0}")]
	ExpConstruction(String),
}

pub fn url_exp(url_raw: &str) -> Result<RestrictedExpression, ParseURLToExpError> {
	let parsed_url = url::Url::parse(url_raw)?;

	let record = [
		(
			"protocol".to_string(),
			RestrictedExpression::new_string(parsed_url.scheme().to_owned()),
		),
		(
			"host".to_string(),
			RestrictedExpression::new_string(
				parsed_url
					.host()
					.ok_or(ParseURLToExpError::NoHost)?
					.to_string(),
			),
		),
		(
			"path".to_string(),
			RestrictedExpression::new_string(parsed_url.path().to_string()),
		),
	];
	let result = RestrictedExpression::new_record(record)
		.map_err(|err| ParseURLToExpError::ExpConstruction(err.to_string()))?;
	Ok(result)
}

#[derive(thiserror::Error, Debug)]
pub enum TrustedIssuerEntityError {
	#[error("could not get url exp: {0}")]
	Parse(#[from] ParseURLToExpError),

	#[error("could not create entity uid from json: {0}")]
	CreateFromJson(String),
	#[error("could not create new entity: {0}")]
	NewEntity(#[from] EntityAttrEvaluationError),
}

pub fn trusted_issuer_entity(url_raw: &str) -> Result<Entity, TrustedIssuerEntityError> {
	let id = serde_json::json!({ "__entity": { "type": "Jans::TrustedIssuer", "id": url_raw } });
	let uid = EntityUid::from_json(id)
		.map_err(|err| TrustedIssuerEntityError::CreateFromJson(err.to_string()))?;

	let attrs = HashMap::from([("issuer_entity_id".to_string(), url_exp(url_raw)?)]);

	let entity = Entity::new(uid, attrs, HashSet::with_capacity(0))?;

	Ok(entity)
}

pub fn roles_entities(roles: &[String]) -> Vec<Entity> {
	roles
		.into_iter()
		.map(|role| {
			Entity::with_uid(EntityUid::from_type_name_and_id(
				// it should newer panic
				EntityTypeName::from_str("Jans::Role").unwrap(),
				EntityId::new(role),
			))
		})
		.collect()
}
