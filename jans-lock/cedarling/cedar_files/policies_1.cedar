permit (
  principal,
  action == Jans::Action::"Execute",
  resource
)
when {
  principal is Jans::User && principal.email.domain == "admin-ui-test.gluu.org" 
};
