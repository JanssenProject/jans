@id("uma_resources_delete")
permit(
  principal,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"uma_resources_delete"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/uma.delete","https://jans.io/oauth/config/uma.admin", "https://jans.io/oauth/config/delete-all"])
};
