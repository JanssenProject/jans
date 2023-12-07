package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceAdminUIRole() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing roles for the AdminUI.",
		CreateContext: resourceAdminUIRoleCreate,
		ReadContext:   resourceAdminUIRoleRead,
		UpdateContext: resourceAdminUIRoleUpdate,
		DeleteContext: resourceAdminUIRoleDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"role": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Role name",
			},
			"description": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Role description",
			},
			"deletable": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether the role can be deleted",
			},
		},
	}
}

func resourceAdminUIRoleCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var role jans.AdminUIRole
	if err := fromSchemaResource(d, &role); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new adminUI role")
	if err := c.CreateAdminUIRole(ctx, &role); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New adminUI role created", map[string]interface{}{"role": role.Role})

	d.SetId(role.Role)

	return resourceAdminUIRoleRead(ctx, d, meta)
}

func resourceAdminUIRoleRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	roleID := d.Id()
	role, err := c.GetAdminUIRole(ctx, roleID)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, role); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(role.Role)

	return diags
}

func resourceAdminUIRoleUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var role jans.AdminUIRole
	if err := fromSchemaResource(d, &role); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating admin mapping", map[string]interface{}{"role": role.Role})
	if err := c.UpdateAdminUIRole(ctx, &role); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "AdminUI role updated", map[string]interface{}{"role": role.Role})

	return resourceAdminUIRoleRead(ctx, d, meta)
}

func resourceAdminUIRoleDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	roleID := d.Id()
	tflog.Debug(ctx, "Deleting adminUI role", map[string]interface{}{"role": roleID})
	if err := c.DeleteAdminUIRole(ctx, roleID); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "AdminUI role deleted", map[string]interface{}{"role": roleID})

	return resourceAdminUIRoleRead(ctx, d, meta)
}
