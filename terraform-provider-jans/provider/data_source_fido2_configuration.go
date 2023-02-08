package provider

import (
	"context"
	"strconv"
	"time"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/moabu/terraform-provider-jans/jans"
)

func dataSourceFido2Configuration() *schema.Resource {

	return &schema.Resource{
		Description: "Data source for retrieving the Fido2 configuration of the Janssen server",
		ReadContext: dataSourceFido2ConfigurationRead,
		Schema: map[string]*schema.Schema{
			"version": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "The version of the FIDO2 U2F core protocol to which this server conforms. The value MUST be the string 1.0.",
			},
			"issuer": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "A URI indicating the party operating the FIDO U2F server.",
			},
			"attestation": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "list of fido2 attestation endpoints.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"base_path": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "fido2 attestation endpoint.",
						},
						"options_endpoint": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "fido2 attestation options endpoint.",
						},
						"result_endpoint": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "fido2 attestation result endpoint.",
						},
					},
				},
			},
			"assertion": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "list of fido2 assertion endpoints.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"base_path": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "fido2 assertion endpoint.",
						},
						"options_endpoint": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "fido2 assertion options endpoint.",
						},
						"result_endpoint": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "fido2 assertion result endpoint.",
						},
					},
				},
			},
		},
	}
}

func dataSourceFido2ConfigurationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	config, err := c.GetFido2Config(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, config); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(strconv.FormatInt(time.Now().Unix(), 10))

	return nil
}
