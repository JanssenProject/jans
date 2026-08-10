@id("session_delete")
permit(
  principal,
  action in [Jans::Action::"DELETE"],
  resource == Jans::HTTP_Request::"session_delete"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/jans-auth-server/session.delete", "revoke_session", "https://jans.io/oauth/jans-auth-server/session.admin", "https://jans.io/oauth/config/delete-all"])
};
