@id("configuser_update")
permit(
  principal,
  action in [Jans::Action::"POST", Jans::Action::"PUT", Jans::Action::"PATCH"],
  resource == Jans::HTTP_Request::"configuser_update"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/user.write", "https://jans.io/oauth/config/user.admin", "https://jans.io/oauth/config/write-all"])
};
