package provider

import "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"

// computedSchemaFromResource deep-copies a resource schema map into a
// data-source schema whose every attribute is Computed. Write-only concerns
// such as validation, defaults, Required/Optional and MaxItems are dropped,
// while the Sensitive flag is preserved so secret values stay masked.
func computedSchemaFromResource(in map[string]*schema.Schema) map[string]*schema.Schema {
	out := make(map[string]*schema.Schema, len(in))
	for k, v := range in {
		out[k] = computedSchema(v)
	}
	return out
}

func computedSchema(in *schema.Schema) *schema.Schema {
	cp := &schema.Schema{
		Type:        in.Type,
		Description: in.Description,
		Computed:    true,
		Sensitive:   in.Sensitive,
	}

	switch elem := in.Elem.(type) {
	case *schema.Schema:
		cp.Elem = &schema.Schema{Type: elem.Type}
	case *schema.Resource:
		cp.Elem = &schema.Resource{Schema: computedSchemaFromResource(elem.Schema)}
	}

	return cp
}
