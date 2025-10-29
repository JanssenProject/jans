# Configure the Janssen Provider
terraform {
  required_providers {
    jans = {
      source = "jans/jans"
    }
  }
}

provider "jans" {
  url           = "https://moabu-noble-sawfish.gluu.info"
  client_id     = var.client_id
  client_secret = var.client_secret
  insecure_client = true  # Only for development/testing
}

# Data source to search tokens with pagination
data "jans_tokens" "recent_tokens" {
  limit = 10
}

# Output the tokens
output "tokens" {
  value = data.jans_tokens.recent_tokens.tokens
  sensitive = true  # Token information is sensitive
}

# Output token count
output "token_count" {
  value = length(data.jans_tokens.recent_tokens.tokens)
}

# Output access tokens only
output "access_tokens" {
  value = [
    for token in data.jans_tokens.recent_tokens.tokens :
    {
      code       = token.token_code
      client_id  = token.client_id
      grant_type = token.grant_type
      created    = token.creation_date
    } if token.token_type == "access_token"
  ]
  sensitive = true
  description = "Only access tokens"
}

# Example: Search for tokens by client ID pattern
data "jans_tokens" "client_tokens" {
  limit = 50
}

output "client_specific_tokens" {
  value = [
    for token in data.jans_tokens.client_tokens.tokens :
    token if can(regex("9876baac", token.client_id))
  ]
  sensitive = true
  description = "Tokens for specific client"
}
