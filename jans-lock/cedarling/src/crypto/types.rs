#[derive(serde::Deserialize, Debug, Clone, PartialEq, Eq, PartialOrd, Ord)]
pub(crate) struct TrustedIssuer {
	pub(crate) name: String,
	pub(crate) openid_configuration_endpoint: String,
	pub(crate) description: String,
}
