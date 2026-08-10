@id("configuser_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"configuser_get"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/user.readonly", "https://jans.io/oauth/config/user.write", "https://jans.io/oauth/config/read-all", "https://jans.io/oauth/config/user.admin", "https://jans.io/oauth/config/read-all"])
};
