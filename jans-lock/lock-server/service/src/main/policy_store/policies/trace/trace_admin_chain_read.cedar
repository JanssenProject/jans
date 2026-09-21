@id("lock_audit_trace_admin_chain_read")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"lock_audit_trace_admin_chain_read"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.contains("https://jans.io/oauth/lock/trace.admin")
};