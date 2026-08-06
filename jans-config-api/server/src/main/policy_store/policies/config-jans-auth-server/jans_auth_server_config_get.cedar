@id("jans_auth_server_config_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/jans-auth-server/config"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/properties.readonly","https://jans.io/oauth/jans-auth-server/config/properties.write","https://jans.io/oauth/config/read-all", "https://jans.io/oauth/config/write-all", "https://jans.io/oauth/jans-auth-server/config/properties.admin"])
};
