@id("admin_ui_permissions_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"admin_ui_permissions_get"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.readonly", "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write", "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.admin", "https://jans.io/oauth/jans-auth-server/config/adminui/read-all"])
};
