@id("fido2_metrics_entries_analytics_comparison_get")
permit(
  principal,
  action in [Jans::Action::"GET"],
  resource == Jans::HTTP_Request::"fido2_metrics_entries_analytics_comparison_get"
)
when {
context has tokens &&
  context.tokens has jans_access_token &&
  context.tokens.jans_access_token has scope &&
  context.tokens.jans_access_token.scope.containsAny(["https://jans.io/oauth/config/fido2-metrics.readonly", "https://jans.io/oauth/config/fido2.admin", "https://jans.io/oauth/config/read-all", "https://jans.io/oauth/config/write-all"])
};
