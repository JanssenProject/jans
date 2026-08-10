@id("logging_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"logging_get"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/logging.readonly","https://jans.io/oauth/config/logging.write", "https://jans.io/oauth/config/logging.admin", "https://jans.io/oauth/config/read-all", "https://jans.io/oauth/config/write-all"])
};
