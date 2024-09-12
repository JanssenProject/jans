use cedar_policy::{
	Authorizer, Context, Entities, EntitiesError, EntityUid, ParseErrors, PolicySet, Request,
	Response,
};
use jwt_engine::JWTDecoder;

mod jwt_data_handler;
use jwt_data_handler::{AuthzInputEntitiesError, DecodeTokensError, JWTData};
pub use jwt_data_handler::{AuthzRequest, CedarParams, ResourceData};
pub(crate) mod jwt_tokens;
use init_engine::policy_store::TrustedIssuers;
use init_engine::{BootstrapConfig, TokenMapper};

pub(crate) mod exp_parsers;

use std::str::FromStr;

pub struct Authz {
	application_name: Option<String>,
	token_mapper: TokenMapper,

	jwt_dec: JWTDecoder,
	policy: PolicySet,
	schema: cedar_policy::Schema,

	#[allow(dead_code)] // it will be fixed after adding handling the trusted store
	trusted_issuers: TrustedIssuers,
}

#[derive(thiserror::Error, Debug)]
pub enum AuthzNewError {
	// TODO: currently we don't have a way to handle the error
	// but it should be in the future
}

impl Authz {
	pub fn new(config: BootstrapConfig) -> Result<Authz, AuthzNewError> {
		let jwt_decoder = config.get_jwt_decoder();
		let policy_store = config.policy_store;

		Ok(Authz {
			jwt_dec: jwt_decoder,
			policy: policy_store.policies,
			schema: policy_store.schema,
			trusted_issuers: policy_store.trusted_issuers,

			application_name: config.application_name,
			token_mapper: config.token_mapper,
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
	AddEntities(#[from] EntitiesError), //this case was not covered by unit tests, because need to break creation of entities on previous steps
	#[error("could not create context: {0}")]
	Context(cedar_policy::ContextJsonError),
	#[error("could not create request type: {0}")]
	Request(String), //this case was not covered by unit tests, because all fields are created in previous steps according to the schema
}

impl Authz {
	pub fn handle_raw_input(&self, data: &str) -> Result<Response, HandleError> {
		let input =
			jwt_data_handler::AuthzRequest::parse_raw(data).map_err(HandleError::InputJsonParse)?;

		self.handle(input)
	}

	pub fn handle(&self, input: AuthzRequest) -> Result<Response, HandleError> {
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

	pub fn is_authorized(&self, input: AuthzRequest) -> Result<bool, HandleError> {
		let decision = self.handle(input)?;
		Ok(match decision.decision() {
			cedar_policy::Decision::Allow => true,
			cedar_policy::Decision::Deny => false,
		})
	}

	pub fn get_entities(&self, data: JWTData) -> Result<EntitiesBox, HandleError> {
		// TODO: add entities from trust store about issuers (like in cedarling)

		let jwt_entities = data.entities(self.application_name.as_deref(), &self.token_mapper)?;

		let entities = Entities::empty().add_entities(jwt_entities.entities, Some(&self.schema))?;
		Ok(EntitiesBox {
			entities,
			user_entity_uid: jwt_entities.user_entity_uid,
		})
	}
}

pub struct EntitiesBox {
	pub entities: Entities,
	pub user_entity_uid: EntityUid,
}
