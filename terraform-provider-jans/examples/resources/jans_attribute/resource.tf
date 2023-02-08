resource "jans_attribute" "country_attribute" {
  admin_can_access          = true
  admin_can_edit            = true
  admin_can_view            = true
  claim_name                = "country"
  data_type                 = "STRING"
  description               = "Country"
  display_name              = "Country"
  edit_type                 = [
      "USER",
      "ADMIN",
  ]
  name                      = "c"
  origin                    = "jansPerson"
  saml1_uri                 = "urn:mace:dir:attribute-def:c"
  saml2_uri                 = "urn:oid:2.5.4.6"
  urn                       = "urn:jans:dir:attribute-def:c"
  user_can_access           = true
  user_can_edit             = true
  user_can_view             = true
  view_type                 = [
      "USER",
      "ADMIN",
  ]
  status = "ACTIVE"
}