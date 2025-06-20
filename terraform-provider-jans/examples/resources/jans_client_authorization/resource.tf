
resource "jans_client_authorization" "example" {
  inum        = "1800.abcd1234-5678-90ef-ghij-klmnopqrstuv"
  dn          = "inum=1800.abcd1234-5678-90ef-ghij-klmnopqrstuv,ou=clients,o=jans"
  client_id   = "example-client-id"
  scopes      = ["openid", "profile", "email"]
  attributes  = {
    displayName = "Example Client Authorization"
    description = "Example client authorization for testing"
  }
}
