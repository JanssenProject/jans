// Any successfully verified local UserInfo token may view the task collection.
@id("view-token")
permit(
    principal,
    action == TaskApp::Action::"ViewTask",
    resource
) when {
    context has tokens &&
    context.tokens has localmockidp_userinfo_token
};
