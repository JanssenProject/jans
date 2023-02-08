package provider

import (
	"context"
	"strconv"
	"time"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/moabu/terraform-provider-jans/jans"
)

func dataSourceServiceProviderConfig() *schema.Resource {

	return &schema.Resource{
		Description: "Data source for retrieving the persistence configured in the Janssen server",
		ReadContext: dataSourceServiceProviderConfigRead,
		Schema: map[string]*schema.Schema{
			"schemas": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A list of URIs of the schemas used to define the attributes of the group.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"meta": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A complex type that contains meta attributes associated with the resource.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"resource_type": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The resource type of the group.",
						},
						"location": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The URI of the group.",
						},
						"created": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The date and time the group was created.",
						},
						"last_modified": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The date and time the group was last modified.",
						},
					},
				},
			},
			"documentation_uri": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "The URL of the service provider's human-readable help documentation.",
			},
			"patch": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A complex type that specifies PATCH configuration options.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"supported": {
							Type:        schema.TypeBool,
							Computed:    true,
							Description: "A Boolean value that specifies whether the PATCH operation is supported.",
						},
					},
				},
			},
			"bulk": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A complex type that specifies bulk configuration options.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"supported": {
							Type:        schema.TypeBool,
							Computed:    true,
							Description: "A Boolean value that specifies whether the bulk operation is supported.",
						},
						"max_operations": {
							Type:        schema.TypeInt,
							Computed:    true,
							Description: "The maximum number of operations that are permitted in a single bulk request.",
						},
						"max_payload_size": {
							Type:        schema.TypeInt,
							Computed:    true,
							Description: "The maximum size of the payload in bytes that is supported for bulk operations.",
						},
					},
				},
			},
			"filter": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A complex type that specifies filter configuration options.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"supported": {
							Type:        schema.TypeBool,
							Computed:    true,
							Description: "A Boolean value that specifies whether the filter parameter is supported.",
						},
						"max_results": {
							Type:        schema.TypeInt,
							Computed:    true,
							Description: "The maximum number of resources that are returned for a list API operation if no \"count\" parameter is provided.",
						},
					},
				},
			},
			"change_password": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A complex type that specifies change password configuration options.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"supported": {
							Type:        schema.TypeBool,
							Computed:    true,
							Description: "A Boolean value that specifies whether the change password operation is supported.",
						},
					},
				},
			},
			"sort": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A complex type that specifies sort configuration options.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"supported": {
							Type:        schema.TypeBool,
							Computed:    true,
							Description: "A Boolean value that specifies whether the sort parameter is supported.",
						},
					},
				},
			},
			"etag": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "The ETag value of the SCIM service provider's configuration.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"supported": {
							Type:        schema.TypeBool,
							Computed:    true,
							Description: "A Boolean value that specifies whether the etag parameter is supported.",
						},
					},
				},
			},
			"authentication_schemes": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A complex type that specifies the authentication schemes that are supported for the SCIM API.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"type": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The authentication scheme that is used to authenticate the API requests.",
						},
						"name": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The name of the authentication scheme that is used to authenticate the API requests.",
						},
						"description": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The description of the authentication scheme that is used to authenticate the API requests.",
						},
						"spec_uri": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The URI of the specification that defines the authentication scheme that is used to authenticate the API requests.",
						},
						"document_uri": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The URI of the online documentation of the authentication scheme that is used to authenticate the API requests.",
						},
						"primary": {
							Type:        schema.TypeBool,
							Computed:    true,
							Description: "A Boolean value that specifies whether the authentication scheme that is used to authenticate the API requests is the primary authentication scheme.",
						},
					},
				},
			},
		},
	}
}

func dataSourceServiceProviderConfigRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	providerConfig, err := c.GetServiceProviderConfig(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, providerConfig); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(strconv.FormatInt(time.Now().Unix(), 10))

	return nil
}
