@id("stat_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"stat_get"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope == jans_stat
  &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/stats.readonly", "https://jans.io/oauth/config/stats.admin", "https://jans.io/oauth/config/read-all", "https://jans.io/oauth/config/write-all"])
};
