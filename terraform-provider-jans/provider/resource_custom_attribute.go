package provider

import (
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
)

func resourceCustomAttribute() *schema.Resource {

	return &schema.Resource{
		Schema: map[string]*schema.Schema{
			"name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Name of the attribute. Example: name, displayName, birthdate, email",
			},
			"multi_valued": {
				Type:        schema.TypeBool,
				Required:    true,
				Description: "Indicates if the attribute can hold multiple values.",
			},
			"values": {
				Type:        schema.TypeList,
				Required:    true,
				Description: "List of values for the attribute.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"display_value": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Display value for the attribute.",
			},
			"value": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Value for the attribute.",
			},
		},
	}
}
