@id("admin_ui_role_to_permissions_mappings_update")
permit(
  principal,
  action in [Jans::Action::"POST", Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"admin_ui_role_to_permissions_mappings_update"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write", "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.admin", "https://jans.io/oauth/jans-auth-server/config/adminui/write-all"])
};
