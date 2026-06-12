@id("AuditorCanOnlyRead")
permit (
  principal,
  action in [Jans::Action::"Read"],
  resource in Jans::Application::"dashboard"
) when {
    context has tokens.jans_userinfo_token &&
    context.tokens.jans_userinfo_token.hasTag("role") &&
    context.tokens.jans_userinfo_token.getTag("role").contains("auditor")
};