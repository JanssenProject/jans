// Any explicitly declared TaskApp user may view the task collection.
@id("view-user")
permit(
    principal is TaskApp::User,
    action == TaskApp::Action::"ViewTask",
    resource
);
