package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceAdminUIRolePermissionMapping() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing role permissions for the AdminUI.",
		CreateContext: resourceAdminUIRolePermissionMappingCreate,
		ReadContext:   resourceAdminUIRolePermissionMappingRead,
		UpdateContext: resourceAdminUIRolePermissionMappingUpdate,
		DeleteContext: resourceAdminUIRolePermissionMappingDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"role": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Role name",
			},
			"permissions": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Permissions",
				Elem: &schema.Schema{
					Type:        schema.TypeString,
					Description: "List of permissions for the role",
				},
			},
		},
	}
}

func resourceAdminUIRolePermissionMappingCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var permissionMapping jans.AdminUIRolePermissionMapping
	if err := fromSchemaResource(d, &permissionMapping); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new AdminUI role permission mapping")
	if err := c.CreateAdminUIRolePermissionMapping(ctx, &permissionMapping); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New AdminUI role permission mapping created", map[string]interface{}{"role": permissionMapping.Role})

	d.SetId(permissionMapping.Role)

	return resourceAdminUIRolePermissionMappingRead(ctx, d, meta)
}

func resourceAdminUIRolePermissionMappingRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	mappingID := d.Id()
	mapping, err := c.GetAdminUIRolePermissionMapping(ctx, mappingID)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, mapping); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(mapping.Role)

	return diags

}

func resourceAdminUIRolePermissionMappingUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var mapping jans.AdminUIRolePermissionMapping
	if err := fromSchemaResource(d, &mapping); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating AdminUI role permission mapping", map[string]interface{}{"role": mapping.Role})
	if err := c.UpdateAdminUIRolePermissionMapping(ctx, &mapping); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "AdminUI role permission mapping updated", map[string]interface{}{"role": mapping.Role})

	return resourceAdminUIRolePermissionMappingRead(ctx, d, meta)
}

func resourceAdminUIRolePermissionMappingDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	mappingID := d.Id()
	tflog.Debug(ctx, "Deleting AdminUI role permission mapping", map[string]interface{}{"role": mappingID})
	if err := c.DeleteAdminUIRolePermissionMapping(ctx, mappingID); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "AdminUI role permission mapping deleted", map[string]interface{}{"role": mappingID})

	return resourceAdminUIRolePermissionMappingRead(ctx, d, meta)
}
