@id("jwks_config_update")
permit(
  principal,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"jwks_config_update"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/jwks.write", "https://jans.io/oauth/config/jwks.admin", "https://jans.io/oauth/config/write-all"])
};
