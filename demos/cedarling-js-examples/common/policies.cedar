// ── Token-based policies (evaluated when JWTs from trusted issuers are present) ──

@id("viewAndCreate-token")
permit(
    principal,
    action in [TaskApp::Action::"ViewTask", TaskApp::Action::"CreateTask"],
    resource
) when {
    context has tokens && (
        context.tokens has localmockidp_userinfo_token ||
        context.tokens has localmockidp_id_token
    )
};

@id("ownerModify-token")
permit(
    principal,
    action in [TaskApp::Action::"UpdateTask", TaskApp::Action::"DeleteTask"],
    resource
) when {
    context has tokens && (
        (context.tokens has localmockidp_userinfo_token &&
         context.tokens.localmockidp_userinfo_token.hasTag("sub") &&
         context.tokens.localmockidp_userinfo_token.getTag("sub").contains(resource.owner)) ||
        (context.tokens has localmockidp_id_token &&
         context.tokens.localmockidp_id_token.hasTag("sub") &&
         context.tokens.localmockidp_id_token.getTag("sub").contains(resource.owner))
    )
};

// ── User-asserted policies (evaluated when principal is asserted directly) ──

@id("viewAndCreate-user")
permit(
    principal is TaskApp::User,
    action in [TaskApp::Action::"ViewTask", TaskApp::Action::"CreateTask"],
    resource
);

@id("ownerModify-user")
permit(
    principal is TaskApp::User,
    action in [TaskApp::Action::"UpdateTask", TaskApp::Action::"DeleteTask"],
    resource
) when {
    context has userId && resource.owner == context.userId
};
