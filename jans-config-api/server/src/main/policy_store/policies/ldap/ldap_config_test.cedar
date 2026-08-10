@id("ldap_config_test")
permit(
  principal,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"ldap_config_test"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/database/ldap.readonly", "https://jans.io/oauth/config/database/ldap.write", "https://jans.io/oauth/config/database.admin", "https://jans.io/oauth/config/read-all"])
};
