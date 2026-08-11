@id("admin_ui_configuration_update")
permit(
  principal,
  action in [Jans::Action::"POST", Jans::Action::"PUT"],
  resource == Jans::HTTP_Request::"admin_ui_configuration_update"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/config/adminui/properties.readonly"])
};
