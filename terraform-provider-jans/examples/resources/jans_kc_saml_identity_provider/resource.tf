
resource "jans_kc_saml_identity_provider" "example" {
  name         = "example-saml-idp"
  display_name = "Example SAML Identity Provider"
  enabled      = true
  
  metadata_file = "/path/to/metadata.xml"
  
  config = {
    entity_id = "https://idp.example.com"
    sso_service_url = "https://idp.example.com/sso"
    name_id_policy_format = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"
    want_assertions_signed = true
    want_assertions_encrypted = false
    signature_algorithm = "RSA_SHA256"
  }
}
