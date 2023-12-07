package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceGroup() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource represents a group resource. See section 4.2 of RFC 7643",
		CreateContext: resourceGroupCreate,
		ReadContext:   resourceGroupRead,
		UpdateContext: resourceGroupUpdate,
		DeleteContext: resourceGroupDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"id": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "The unique identifier for the group.",
			},
			"display_name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The name of the group.",
			},
			"schemas": {
				Type:        schema.TypeList,
				Optional:    true,
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
			"members": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of members of the group.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"value": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "The ID of the member.",
						},
						"type": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "The type of the member.",
						},
						"display": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "The display name of the member.",
						},
						"ref": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "The URI of the member.",
						},
					},
				},
			},
		},
	}
}

func resourceGroupCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var group jans.Group
	if err := fromSchemaResource(d, &group); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new Group")
	newGroup, err := c.CreateGroup(ctx, &group)
	if err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New Group created", map[string]interface{}{"id": newGroup.ID})

	d.SetId(newGroup.ID)

	return resourceGroupRead(ctx, d, meta)
}

func resourceGroupRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	id := d.Id()
	group, err := c.GetGroup(ctx, id)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, group); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(group.ID)

	return diags

}

func resourceGroupUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var group jans.Group
	if err := fromSchemaResource(d, &group); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating Group", map[string]interface{}{"id": group.ID})
	if _, err := c.UpdateGroup(ctx, &group); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Group updated", map[string]interface{}{"id": group.ID})

	return resourceGroupRead(ctx, d, meta)
}

func resourceGroupDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	id := d.Id()
	tflog.Debug(ctx, "Deleting Group", map[string]interface{}{"id": id})
	if err := c.DeleteGroup(ctx, id); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Group deleted", map[string]interface{}{"id": id})

	return resourceGroupRead(ctx, d, meta)
}
