@id("agama_repo_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"agama_repo_get"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/agama-repo.readonly", "https://jans.io/oauth/config/agama-repo.write", "https://jans.io/oauth/config/agama.admin", "https://jans.io/oauth/config/read-all", "https://jans.io/oauth/config/write-all"])
};
