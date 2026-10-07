package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceUMAResourcesByClient() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for looking up UMA resources associated with a client (GET /uma/resources/clientId/{clientId}).",
		ReadContext: dataSourceUMAResourcesByClientRead,
		Schema: map[string]*schema.Schema{
			"client_id": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The client id (inum) to look up UMA resources for.",
			},
			"resources": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "The UMA resources associated with the given client.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"inum":        {Type: schema.TypeString, Computed: true, Description: "Unique identifier of the UMA resource."},
						"id":          {Type: schema.TypeString, Computed: true, Description: "Id of the UMA resource."},
						"dn":          {Type: schema.TypeString, Computed: true, Description: "Distinguished name of the UMA resource."},
						"name":        {Type: schema.TypeString, Computed: true, Description: "Name of the UMA resource."},
						"description": {Type: schema.TypeString, Computed: true, Description: "Description of the UMA resource."},
						"scopes":      {Type: schema.TypeList, Computed: true, Elem: &schema.Schema{Type: schema.TypeString}, Description: "Scopes protecting the resource."},
						"clients":     {Type: schema.TypeList, Computed: true, Elem: &schema.Schema{Type: schema.TypeString}, Description: "Clients associated with the resource."},
					},
				},
			},
		},
	}
}

func dataSourceUMAResourcesByClientRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	clientId := d.Get("client_id").(string)
	resources, err := c.GetUMAResourcesByClient(ctx, clientId)
	if err != nil {
		return diag.FromErr(err)
	}

	out := make([]interface{}, 0, len(resources))
	for _, r := range resources {
		out = append(out, map[string]interface{}{
			"inum":        r.Inum,
			"id":          r.ID,
			"dn":          r.Dn,
			"name":        r.Name,
			"description": r.Description,
			"scopes":      r.Scopes,
			"clients":     r.Clients,
		})
	}

	if err := d.Set("resources", out); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(clientId)

	return nil
}
