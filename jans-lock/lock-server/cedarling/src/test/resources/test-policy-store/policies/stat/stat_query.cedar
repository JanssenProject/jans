@id("lock_stat_query")
permit(
  principal,
  action in [Jans::Action::"Search"],
  resource == Jans::HTTP_Request::"lock_stat_query"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.contains("https://jans.io/oauth/lock/stat.readonly")
};