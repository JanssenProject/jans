package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceAdminUIPermission() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing permissions for the AdminUI.",
		CreateContext: resourceAdminUIPermissionCreate,
		ReadContext:   resourceAdminUIPermissionRead,
		UpdateContext: resourceAdminUIPermissionUpdate,
		DeleteContext: resourceAdminUIPermissionDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"permission": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Permission",
			},
			"default_permission_in_token": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Default permission in token",
			},
			"description": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Permission description",
			},
		},
	}
}

func resourceAdminUIPermissionCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var permission jans.AdminUIPermission
	if err := fromSchemaResource(d, &permission); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new AdminUI permission")
	if err := c.CreateAdminUIPermission(ctx, &permission); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New AdminUI permission created", map[string]interface{}{"permission": permission.Permission})

	d.SetId(permission.Permission)

	return resourceAdminUIPermissionRead(ctx, d, meta)
}

func resourceAdminUIPermissionRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	permissionId := d.Id()

	permission, err := c.GetAdminUIPermission(ctx, permissionId)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, permission); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(permissionId)

	return diags
}

func resourceAdminUIPermissionUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var permission jans.AdminUIPermission
	if err := fromSchemaResource(d, &permission); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating AdminUI permission", map[string]interface{}{"permission": permission.Permission})
	if err := c.UpdateAdminUIPermission(ctx, &permission); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "AdminUI permission updated", map[string]interface{}{"permission": permission.Permission})

	return resourceAdminUIPermissionRead(ctx, d, meta)
}

func resourceAdminUIPermissionDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	permission := d.Id()
	tflog.Debug(ctx, "Deleting AdminUI permission", map[string]interface{}{"permission": permission})
	if err := c.DeleteAdminUIPermission(ctx, permission); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "AdminUI permission deleted", map[string]interface{}{"permission": permission})

	return resourceAdminUIPermissionRead(ctx, d, meta)
}
