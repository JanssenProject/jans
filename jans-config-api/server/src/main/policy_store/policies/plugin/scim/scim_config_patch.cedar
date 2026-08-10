@id("scim_config_patch")
permit(
  principal,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"scim_config_patch"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/scim/config.write", "https://jans.io/oauth/config/scim.admin", "https://jans.io/oauth/config/write-all"])
};
