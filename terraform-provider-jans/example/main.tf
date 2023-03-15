terraform {
  required_version = ">= 0.12.0"
  required_providers {
    janssen = {
      source = "terraform.local/janssen/terraform-provider-jans"
      version = ">= 0.1.0"
    }
  }
}

provider "janssen" {
  url = "http://localhost:8080"
  api_key = "secret"
}