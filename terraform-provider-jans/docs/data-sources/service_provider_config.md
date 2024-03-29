---
# generated by https://github.com/hashicorp/terraform-plugin-docs
page_title: "jans_service_provider_config Data Source - terraform-provider-jans"
subcategory: ""
description: |-
  Data source for retrieving the persistence configured in the Janssen server
---

# jans_service_provider_config (Data Source)

Data source for retrieving the persistence configured in the Janssen server

## Example Usage

```terraform
data "jans_service_provider_config" "config" {
	id = "urn:ietf:params:scim:schemas:core:2.0:Group"
}

output "group_attribute" {
  value = data.jans_schema.group.attributes[0].name
}
```

<!-- schema generated by tfplugindocs -->
## Schema

### Read-Only

- `authentication_schemes` (List of Object) A complex type that specifies the authentication schemes that are supported for the SCIM API. (see [below for nested schema](#nestedatt--authentication_schemes))
- `bulk` (List of Object) A complex type that specifies bulk configuration options. (see [below for nested schema](#nestedatt--bulk))
- `change_password` (List of Object) A complex type that specifies change password configuration options. (see [below for nested schema](#nestedatt--change_password))
- `documentation_uri` (String) The URL of the service provider's human-readable help documentation.
- `etag` (List of Object) The ETag value of the SCIM service provider's configuration. (see [below for nested schema](#nestedatt--etag))
- `filter` (List of Object) A complex type that specifies filter configuration options. (see [below for nested schema](#nestedatt--filter))
- `id` (String) The ID of this resource.
- `meta` (List of Object) A complex type that contains meta attributes associated with the resource. (see [below for nested schema](#nestedatt--meta))
- `patch` (List of Object) A complex type that specifies PATCH configuration options. (see [below for nested schema](#nestedatt--patch))
- `schemas` (List of String) A list of URIs of the schemas used to define the attributes of the group.
- `sort` (List of Object) A complex type that specifies sort configuration options. (see [below for nested schema](#nestedatt--sort))

<a id="nestedatt--authentication_schemes"></a>
### Nested Schema for `authentication_schemes`

Read-Only:

- `description` (String)
- `document_uri` (String)
- `name` (String)
- `primary` (Boolean)
- `spec_uri` (String)
- `type` (String)


<a id="nestedatt--bulk"></a>
### Nested Schema for `bulk`

Read-Only:

- `max_operations` (Number)
- `max_payload_size` (Number)
- `supported` (Boolean)


<a id="nestedatt--change_password"></a>
### Nested Schema for `change_password`

Read-Only:

- `supported` (Boolean)


<a id="nestedatt--etag"></a>
### Nested Schema for `etag`

Read-Only:

- `supported` (Boolean)


<a id="nestedatt--filter"></a>
### Nested Schema for `filter`

Read-Only:

- `max_results` (Number)
- `supported` (Boolean)


<a id="nestedatt--meta"></a>
### Nested Schema for `meta`

Read-Only:

- `created` (String)
- `last_modified` (String)
- `location` (String)
- `resource_type` (String)


<a id="nestedatt--patch"></a>
### Nested Schema for `patch`

Read-Only:

- `supported` (Boolean)


<a id="nestedatt--sort"></a>
### Nested Schema for `sort`

Read-Only:

- `supported` (Boolean)
