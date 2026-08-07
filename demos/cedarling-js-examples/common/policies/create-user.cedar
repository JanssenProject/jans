// An explicit unsigned user may create a task only for its asserted identity.
@id("create-user")
permit(
    principal is TaskApp::User,
    action == TaskApp::Action::"CreateTask",
    resource
) when {
    context has userId && resource.owner == context.userId
};
