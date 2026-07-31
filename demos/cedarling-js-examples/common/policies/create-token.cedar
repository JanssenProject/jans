// A verified UserInfo subject may create a task only for itself.
@id("create-token")
permit(
    principal,
    action == TaskApp::Action::"CreateTask",
    resource
) when {
    context has tokens &&
    context.tokens has localmockidp_userinfo_token &&
    context.tokens.localmockidp_userinfo_token.hasTag("sub") &&
    context.tokens.localmockidp_userinfo_token.getTag("sub").contains(resource.owner)
};
