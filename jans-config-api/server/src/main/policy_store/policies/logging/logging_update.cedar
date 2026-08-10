@id("logging_update")
permit(
  principal,
  action in [Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"logging_update"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/logging.write", "https://jans.io/oauth/config/logging.admin", "https://jans.io/oauth/config/write-all"])
};
