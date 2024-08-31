use cedar_policy::{
	Authorizer, Context, Entities, EntitiesError, EntityUid, ParseErrors, PolicySet, Request,
	Response,
};
use jwt::JWTDecoder;

mod jwt_data_handler;
pub use jwt_data_handler::AuthzInputRaw;
use jwt_data_handler::{AuthzInputEntitiesError, DecodeTokensError, JWTData};
pub(crate) mod jwt_tokens;
mod policy_store;
use policy_store::{PolicyStoreEntry, TrustedIssuers};

pub(crate) mod exp_parsers;

use std::str::FromStr;

#[derive(serde::Deserialize, serde::Serialize, Debug)]
#[serde(tag = "strategy")]
#[serde(rename_all = "kebab-case")]
#[serde(rename_all_fields = "camelCase")]
pub enum PolicyStoreConfig {
	Local,
}

#[derive(thiserror::Error, Debug)]
pub enum GetPolicyError {
	#[error("could not parse policy form json: {0}")]
	ParseJson(#[from] serde_json::Error),
}

impl PolicyStoreConfig {
	fn get_policy(self) -> Result<PolicyStoreEntry, GetPolicyError> {
		match self {
			Self::Local => Self::get_local_policy(),
		}
	}

	fn get_local_policy() -> Result<PolicyStoreEntry, GetPolicyError> {
		let policy_raw = include_str!("../../policy-store/local.json");
		let policy: PolicyStoreEntry = serde_json::from_str(policy_raw)?;
		Ok(policy)
	}
}

pub struct Authz {
	app_name: Option<String>,
	jwt_dec: JWTDecoder,
	policy: PolicySet,
	schema: cedar_policy::Schema,
	trusted_issuers: TrustedIssuers,
}

#[derive(thiserror::Error, Debug)]
pub enum AuthzNewError {
	#[error("could not get policy store: {0}")]
	PolicyStore(#[from] GetPolicyError),
	#[error("could not parse entities: {0}")]
	Entities(#[from] EntitiesError),
}

pub struct AuthzConfig {
	pub app_name: Option<String>,
	pub decoder: JWTDecoder,
	pub policy: PolicyStoreConfig,
}

impl Authz {
	pub fn new(config: AuthzConfig) -> Result<Authz, AuthzNewError> {
		let policy_store = config.policy.get_policy()?;

		Ok(Authz {
			app_name: config.app_name,
			jwt_dec: config.decoder,
			policy: policy_store.policies,
			schema: policy_store.schema,
			trusted_issuers: policy_store.trusted_issuers,
		})
	}
}

#[derive(thiserror::Error, Debug)]
pub enum HandleError {
	#[error("could not parse input data json from string: {0}")]
	InputJsonParse(serde_json::Error),
	#[error("could not decode jwt tokens: {0}")]
	DecodeTokens(#[from] DecodeTokensError),

	#[error("could not parse action: {0}")]
	Action(ParseErrors),
	#[error("could not parse resource from json: {0}")]
	Resource(cedar_policy::ParseErrors),
	#[error("could not get entities from input: {0}")]
	AuthzInputEntities(#[from] AuthzInputEntitiesError),
	#[error("could not add entities values to entities list: {0}")]
	AddEntities(#[from] EntitiesError),
	#[error("could not create context: {0}")]
	Context(cedar_policy::ContextJsonError),
	#[error("could not create request type: {0}")]
	Request(String),
}

impl Authz {
	pub fn handle_raw_input(&self, data: &str) -> Result<Response, HandleError> {
		let input = jwt_data_handler::AuthzInputRaw::parse_raw(data)
			.map_err(HandleError::InputJsonParse)?;

		self.handle(input)
	}

	pub fn handle(&self, input: AuthzInputRaw) -> Result<Response, HandleError> {
		let decoded_input = input.decode_tokens(&self.jwt_dec)?;
		let params = decoded_input.chedar_params;
		let action = EntityUid::from_str(params.action.as_str()).map_err(HandleError::Action)?;

		let resource = params
			.resource
			.entity_uid()
			.map_err(HandleError::Resource)?;

		let entities_box = self.get_entities(decoded_input.jwt)?;

		let principal = entities_box.user_entity_uid;

		let context = Context::from_json_value(params.context, Some((&self.schema, &action)))
			.map_err(HandleError::Context)?;

		log::debug!("create cedar-policy request principal: {principal} action: {action} resource: {resource}");

		let request: Request = Request::new(
			Some(principal),
			Some(action),
			Some(resource),
			context,
			Some(&self.schema),
		)
		.map_err(|err| HandleError::Request(err.to_string()))?;

		let authorizer = Authorizer::new();
		let decision = authorizer.is_authorized(&request, &self.policy, &entities_box.entities);
		Ok(decision)
	}

	pub fn get_entities(&self, data: JWTData) -> Result<EntitiesBox, HandleError> {
		// TODO: add entities from trust store about issuers (like in cedarling)

		let jwt_entities = data.entities(self.app_name.as_deref())?;

		let entities = Entities::empty().add_entities(jwt_entities.entities, Some(&self.schema))?;
		Ok(EntitiesBox {
			entities: entities,
			user_entity_uid: jwt_entities.user_entity_uid,
		})
	}
}

pub struct EntitiesBox {
	pub entities: Entities,
	pub user_entity_uid: EntityUid,
}
