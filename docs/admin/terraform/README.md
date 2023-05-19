---
tags:
  - administration
  - terraform
  - operations
  - automation
  - IaC
  - Infrastructure as Code
---

## Benefits of Terraform and IaC

**Consistency:** you create one terraform configuration and use it to configure environments repeatedly.

**Minimized human error:** the configuration is now `as code` and is deployed automatically.

**Fast:** with a single command, you can have all your configuration provisioned.

**Observability:** the configuration are made `as code`, you now have one place to review all the configuration and who changed what.


## Janssen Terraform provider

!!! Note
    The janssen terraform resources cannot be created or destroyed/deleted using terraform, as they are always present in a Janssen deployment. Instead, they can be imported and then updated. The creation of such a resource using `terraform apply` will result in an error. Deletion on the other hand using `terraform destroy` will result in the resource being removed from the state file and become not under terraform management.

The [Janssen](https://registry.terraform.io/providers/JanssenProject/jans/latest/docs) terraform provider is used to manage resources in a Janssen deployment. This includes all configurations, users, groups, OIDC clients, and more.

## Example - Configure Janssen

Let's have an example on `importing` the current `logging level` of a deployment and `changing` it using terraform.

1. Configure Terraform to install the required plugins for `Janssen provider`. Add this to your `.tf` file:

    ```
    terraform {
    required_providers {
        jans = {
        source = "JanssenProject/jans"
        version = "0.6.0"
        }
     }
    }
    ```

2. Now we can run `terraform init`, which will fetch the plugins needed.


2.  Have a `client_id` and `client_secret` with sufficient scopes and permissions.

3.  Configure provider section:

    ```
    provider "jans" {
        url           = "https://test-instnace.jans.io"
        client_id     = "1800.3d29d884-e56b-47ac-83ab-b37942b83a89"
        client_secret = "Ma3egYQ5dkqS"
        insecure_client = true # Optional. If set to `true`, the provider will not verify the TLS certificate of the Janssen server. This is useful for testing purposes and should not be used in production, unless absolutely unavoidable.
    }
    ```

4.  Before importing, we have to define an empty `resource`:

    ```
    resource "jans_logging_configuration" "global" {

    }
    ```

5. Import:
   `terraform import jans_logging_configuration.global global`

    Now after importing, the logging configuration is in terraform state file, i.e. terraform.tfstate, and it's under terraform management.

    `terraform state list` will output:
    ```
    jans_logging_configuration.global
    ```


    You can see the current logging configuration details using `terraform state show jans_logging_configuration.global` which will output something like that: 
    ```
    # jans_logging_configuration.global:
    resource "jans_logging_configuration" "global" {
        disable_jdk_logger          = true
        enabled_oauth_audit_logging = false
        http_logging_enabled        = false
        http_logging_exclude_paths  = []
        id                          = "jans_logging_configuration"
        logging_layout              = "text"
        logging_level               = "INFO"
    }
    ```

    As you can see the current logging level is `INFO`. We can double-check that in the `TUI`
    ![svg](../../assets/imported-logging-info.png)

6.  Add configuration to your `.tf file`
    ```
    resource "jans_logging_configuration" "global" {
    logging_level = "TRACE"
    }
    ```

7.  You can validate your terraform syntax using `terraform validate`

8.  Apply changes using `terraform apply` and enter `yes`

9.  Review the changes now using `terraform state show jans_logging_configuration.global`. 
    Now it should show `logging_level               = "TRACE"`.


10.  We can double-check the `logging level` in the TUI 
     ![svg](../../assets/changed-logging-info.png)    

!!! Note
    You can find the full list of resources you can import and manage using terraform under the `Resources` sidebar in the Janssen terraform [documentation](https://registry.terraform.io/providers/JanssenProject/jans/latest/docs)

