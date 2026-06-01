@id("allow_teacher")
permit(
  principal is Jans::User,
  action,
  resource
)
when {
  principal has role &&
  principal.role.contains("Teacher")
};