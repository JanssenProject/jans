@id("smtp_config_delete")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"smtp_config_delete"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/smtp.delete","https://jans.io/oauth/config/delete-all", "https://jans.io/oauth/config/smtp.admin"])
};
