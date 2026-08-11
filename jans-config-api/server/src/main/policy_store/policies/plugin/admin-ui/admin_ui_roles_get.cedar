@id("admin_ui_roles_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"admin_ui_roles_get"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/role.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write", "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.admin", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};
