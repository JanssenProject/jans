
resource "jans_kc_saml_configuration" "example" {
  application_name = "example-saml-app"
  sp_hostname      = "https://sp.example.com"
  idp_hostname     = "https://idp.example.com"
  
  sp_config = {
    entity_id                = "https://sp.example.com/metadata"
    assertion_consumer_service_url = "https://sp.example.com/acs"
    single_logout_service_url      = "https://sp.example.com/sls"
  }
  
  idp_config = {
    entity_id = "https://idp.example.com/metadata"
    sso_service_url = "https://idp.example.com/sso"
    slo_service_url = "https://idp.example.com/slo"
  }
}
