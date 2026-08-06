@id("fido2_config_update")
permit(
  principal,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"fido2_config_update"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/fido2.admin", "https://jans.io/oauth/config/write-all"])
};
