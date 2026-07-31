// An explicit unsigned user may update or delete only its own task.
@id("modify-user")
permit(
    principal is TaskApp::User,
    action in [TaskApp::Action::"UpdateTask", TaskApp::Action::"DeleteTask"],
    resource
) when {
    context has userId && resource.owner == context.userId
};
