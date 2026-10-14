@id("allow-update-by-org")
permit(
    principal,
    action in [Jans::Action::"Update"],
    resource is Jans::Issue
) when {
    context has tokens.janstestissuer_access_token &&
    context.tokens.janstestissuer_access_token.hasTag("org_id") &&
    context.tokens.janstestissuer_access_token.getTag("org_id").contains(resource.org_id)
};
