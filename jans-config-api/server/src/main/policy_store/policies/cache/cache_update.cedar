@id("cache_update")
permit(
  principal,
  action in [Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/config/cache"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/cache.write", "https://jans.io/oauth/config/write-all"])
};
