
resource "jans_kc_saml_trust_relationship" "example" {
  name         = "example-saml-tr"
  display_name = "Example SAML Trust Relationship"
  description  = "Example SAML trust relationship for testing"
  
  sp_metadata_type = "file"
  sp_metadata_file = "/path/to/sp-metadata.xml"
  
  idp_initiated_sso = true
  sp_initiated_sso  = true
  
  released_attributes = [
    "uid",
    "mail",
    "displayName"
  ]
  
  validation_status = "active"
}
