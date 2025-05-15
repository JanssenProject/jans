terraform {
  required_version = ">= 0.12.0"
  required_providers {
    janssen = {
      source = "JanssenProject/jans"
      version = "1.4.0"
    }
  }
}

provider "jans" {
  url           = "https://test-instnace.jans.io"
  client_id     = "1800.3d29d884-e56b-47ac-83ab-b37942b83a89"
  client_secret = "Ma3egYQ5dkqS"
}