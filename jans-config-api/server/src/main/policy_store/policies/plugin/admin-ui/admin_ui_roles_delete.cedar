@id("admin_ui_roles_delete")
permit(
  principal,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"admin_ui_roles_delete"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/role.delete", "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.admin", "https://jans.io/oauth/jans-auth-server/config/adminui/delete-all"])
};
