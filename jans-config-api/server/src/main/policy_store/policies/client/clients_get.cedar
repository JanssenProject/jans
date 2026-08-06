@id("client_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/openid/clients"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/openid/clients.readonly","https://jans.io/oauth/config/openid-read","https://jans.io/oauth/config/read-all"])
};
