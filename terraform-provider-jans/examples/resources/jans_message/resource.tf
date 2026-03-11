resource "jans_message" "example" {
  provider_type = "REDIS"
  
  redis_configuration {
    servers = "localhost:6379"
  }
}
