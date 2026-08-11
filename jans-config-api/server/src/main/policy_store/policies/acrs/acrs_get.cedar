@id("acr_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"acr_get"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/acrs.readonly","https://jans.io/oauth/config/acrs.write","https://jans.io/oauth/config/read-all"])
};
