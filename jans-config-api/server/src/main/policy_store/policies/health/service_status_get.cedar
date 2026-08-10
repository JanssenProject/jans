@id("service_status_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"service_status_get"
)when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.contains("https://jans.io/oauth/config/data.readonly", "https://jans.io/oauth/config/health.admin", "https://jans.io/oauth/config/read-all")
};

