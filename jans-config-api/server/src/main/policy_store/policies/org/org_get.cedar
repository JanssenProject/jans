@id("org_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"org_get"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/organization.readonly", "https://jans.io/oauth/config/organization.write", "https://jans.io/oauth/config/organization.admin", "https://jans.io/oauth/config/read-all", "https://jans.io/oauth/config/write-all"])
};
