@id("scopes_delete")
permit(
  principal,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"scopes_delete"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/scopes.delete", "https://jans.io/oauth/config/scopes.admin" ,"https://jans.io/oauth/config/delete-all"])
};
