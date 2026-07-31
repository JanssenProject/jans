// A verified UserInfo subject may update or delete only its own task.
@id("modify-token")
permit(
    principal,
    action in [TaskApp::Action::"UpdateTask", TaskApp::Action::"DeleteTask"],
    resource
) when {
    context has tokens &&
    context.tokens has localmockidp_userinfo_token &&
    context.tokens.localmockidp_userinfo_token.hasTag("sub") &&
    context.tokens.localmockidp_userinfo_token.getTag("sub").contains(resource.owner)
};
