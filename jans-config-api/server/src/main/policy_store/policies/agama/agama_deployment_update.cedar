@id("agama_deployment_update")
permit(
  principal,
  action in [Jans::Action::"POST"],
  resource == Jans::HTTP_Request::"agama_deployment_update"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/agama.write", "https://jans.io/oauth/config/agama.admin", "https://jans.io/oauth/config/write-all"])
};
