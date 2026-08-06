@id("attribute_delete")
permit(
  principal,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"/jans-config-api/api/v1/attributes"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/attributes.delete","https://jans.io/oauth/config/delete-all"])
};