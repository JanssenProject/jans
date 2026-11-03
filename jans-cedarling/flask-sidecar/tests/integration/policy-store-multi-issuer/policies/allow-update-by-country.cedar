@id("allow-update-by-country")
permit(
    principal,
    action in [Jans::Action::"Update"],
    resource is Jans::Issue
) when {
    context has tokens.janstestissuer_userinfo_token &&
    context.tokens.janstestissuer_userinfo_token.hasTag("country") &&
    context.tokens.janstestissuer_userinfo_token.getTag("country").contains(resource.country)
};
