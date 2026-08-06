@id("fido2_config_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"fido2_config_get"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.readonly","https://jans.io/oauth/config/fido2.write","https://jans.io/oauth/config/fido2.admin"])
};
