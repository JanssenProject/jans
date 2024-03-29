---
# generated by https://github.com/hashicorp/terraform-plugin-docs
page_title: "jans_custom_script_types Data Source - terraform-provider-jans"
subcategory: ""
description: |-
  Data source for retrieving supported custom script types.
---

# jans_custom_script_types (Data Source)

Data source for retrieving supported custom script types.

## Example Usage

```terraform
data "jans_custom_script_types" "script_types" {
}

output "script_type_client_registration_enabled" {
  value = contains(data.jans_custom_script_types.script_types, "client_registration")
}
```

<!-- schema generated by tfplugindocs -->
## Schema

### Read-Only

- `id` (String) The ID of this resource.
- `types` (List of String) A list of support custom script types.
