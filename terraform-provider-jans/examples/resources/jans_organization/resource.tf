resource "jans_organization" "global" {
  description        = "Welcome to Acme"
  display_name       = "Janssen"
  manager_group      = "inum=60B7,ou=groups,o=jans"
  member             = "null"
  organization       = "jans"
  organization_title = "Acme"
  short_name         = "Janssen"
  theme_color        = "166309"
}