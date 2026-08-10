@id("app_version_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"app_version_get"
)when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.contains("https://jans.io/oauth/config/app-version.readonly", "https://jans.io/oauth/config/health.admin", "https://jans.io/oauth/config/read-all")
};

