# Terraform Provider for the Janssen Project

This repo contains the terraform provider for the [Janssen Project](https://github.com/JanssenProject/jans).

## Issues

For issues please open at the [Janssen Project](https://github.com/JanssenProject/jans) and label with `comp-terraform-provider-jans`.

## PRs

Please contribute by raising a PR against the `terraform-provider-jans` folder inside the [Janssen Project](https://github.com/JanssenProject/jans).

## Documentation

For a complete documentation on how to use this provider, please refer to the [docs](/docs/).

## Installation

This provider can be installed automatically using Terraform >=0.13 by using the `terraform` configuration block:

```hcl
terraform {
  required_providers {
    janssen = {
      source = "janssenproject/janssen"
      version = ">= 1.0.0"
    }
  }
}
```

## Configuration

The provider must be configured using the following variables:

* `url` - The URL of the Janssen server to connect to
* `client_id` - The ID of the OIDC client which will be used to authenticate to the Janssen server
* `client_secret` - The secret of the OIDC client which will be used to authenticate to the Janssen server

If any of those 3 parameters is not provided, the provider will not be able to connect to the Janssen server.

Optionally, users can also set the following variables:

* `insecure_client` - If set to `true`, the provider will not verify the TLS certificate of the Janssen server. This is useful for testing purposes and should not be used in production, unless absolutely unavoidable.
