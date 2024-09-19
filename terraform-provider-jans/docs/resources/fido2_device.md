---
# generated by https://github.com/hashicorp/terraform-plugin-docs
page_title: "jans_fido2_device Resource - terraform-provider-jans"
subcategory: ""
description: |-
  
---

# jans_fido2_device (Resource)





<!-- schema generated by tfplugindocs -->
## Schema

### Required

- `user_id` (String) Identifies the owner of the enrollment

### Optional

- `counter` (Number) Value used in the Fido U2F endpoints
- `creation_date` (String) Date of enrollment in ISO format
- `display_name` (String) Device name suitable for display to end-users
- `schemas` (List of String) A list of URIs of the schemas used to define the attributes of the fido2 device.
- `status` (String)

### Read-Only

- `id` (String) The unique identifier for the fido2 device.
- `meta` (List of Object) A complex type that contains meta attributes associated with the fido2 device. (see [below for nested schema](#nestedatt--meta))

<a id="nestedatt--meta"></a>
### Nested Schema for `meta`

Read-Only:

- `created` (String)
- `last_modified` (String)
- `location` (String)
- `resource_type` (String)