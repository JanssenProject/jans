@id("ldap_config_delete")
permit(
  principal,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"ldap_config_delete"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.delete", "https://jans.io/oauth/config/database.admin", "https://jans.io/oauth/config/delete-all"])
};
