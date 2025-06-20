
resource "jans_oidc_client" "example" {
  client_name = "Example OIDC Client"
  client_id   = "example-client-id"
  client_secret = "example-client-secret"
  
  application_type = "web"
  
  redirect_uris = [
    "https://example.com/callback",
    "https://example.com/redirect"
  ]
  
  response_types = ["code"]
  grant_types = ["authorization_code", "refresh_token"]
  
  scope = ["openid", "profile", "email"]
  
  token_endpoint_auth_method = "client_secret_basic"
  
  logo_uri = "https://example.com/logo.png"
  client_uri = "https://example.com"
  policy_uri = "https://example.com/policy"
  tos_uri = "https://example.com/tos"
  
  jwks_uri = "https://example.com/.well-known/jwks.json"
  
  subject_type = "public"
  id_token_signed_response_alg = "RS256"
  
  access_token_lifetime = 3600
  refresh_token_lifetime = 86400
}
