@id("fido2_registration_delete")
permit(
  principal,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"fido2_registration_delete"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/fido2.delete","https://jans.io/oauth/config/fido2.admin","https://jans.io/oauth/config/delete-all"])
};
