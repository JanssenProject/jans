@id("scim_config_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"scim_config_get"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/scim/config.readonly", "https://jans.io/scim/config.write", "https://jans.io/oauth/config/scim.admin", "https://jans.io/oauth/config/read-all", "https://jans.io/oauth/config/write-all"])
};
