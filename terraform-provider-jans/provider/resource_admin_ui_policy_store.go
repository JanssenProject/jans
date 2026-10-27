package provider

import (
	"context"
	"fmt"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceAdminUIPolicyStore() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing AdminUI Cedarling policy stores.",
		CreateContext: resourceAdminUIPolicyStoreCreate,
		ReadContext:   resourceAdminUIPolicyStoreRead,
		UpdateContext: resourceAdminUIPolicyStoreUpdate,
		DeleteContext: resourceAdminUIPolicyStoreDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "DN of the policy store.",
			},
			"inum": {
				Type:        schema.TypeString,
				Optional:    true,
				Computed:    true,
				Description: "Unique identifier of the policy store.",
			},
			"displayname": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Human-readable name of the policy store.",
			},
			"description": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Description of the policy store.",
			},
			"policy_store": {
				Type:        schema.TypeString,
				Optional:    true,
				ForceNew:    true,
				Description: "Base64-encoded .cjar policy store archive. Changing it forces a new resource.",
			},
			"jans_usr_dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Computed:    true,
				Description: "DN of the user that owns the policy store.",
			},
			"jans_status": {
				Type:        schema.TypeString,
				Optional:    true,
				Computed:    true,
				Description: "Status of the policy store. Possible values are active and inactive.",
				ValidateDiagFunc: func(v any, p cty.Path) diag.Diagnostics {
					return validateEnum(v, []string{"active", "inactive"})
				},
			},
			"creation_date": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Creation date of the policy store.",
			},
			"jans_last_upd": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Last update date of the policy store.",
			},
		},
	}
}

func resourceAdminUIPolicyStoreCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var store jans.AdminUIPolicyStore
	if err := fromSchemaResource(d, &store); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new AdminUI policy store")
	if err := c.CreateAdminUIPolicyStore(ctx, &store); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New AdminUI policy store created", map[string]interface{}{"inum": store.Inum})

	if store.Inum == "" {
		return diag.FromErr(fmt.Errorf("policy store created but server returned no inum; cannot track resource"))
	}

	d.SetId(store.Inum)

	return resourceAdminUIPolicyStoreRead(ctx, d, meta)
}

func resourceAdminUIPolicyStoreRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	inum := d.Id()
	store, err := c.GetAdminUIPolicyStore(ctx, inum)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, store); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(store.Inum)

	return diags
}

func resourceAdminUIPolicyStoreUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var store jans.AdminUIPolicyStore
	if err := fromSchemaResource(d, &store); err != nil {
		return diag.FromErr(err)
	}
	store.Inum = d.Id()

	tflog.Debug(ctx, "Updating AdminUI policy store", map[string]interface{}{"inum": store.Inum})
	if err := c.UpdateAdminUIPolicyStore(ctx, &store); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "AdminUI policy store updated", map[string]interface{}{"inum": store.Inum})

	return resourceAdminUIPolicyStoreRead(ctx, d, meta)
}

func resourceAdminUIPolicyStoreDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	inum := d.Id()
	tflog.Debug(ctx, "Deleting AdminUI policy store", map[string]interface{}{"inum": inum})
	if err := c.DeleteAdminUIPolicyStore(ctx, inum); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "AdminUI policy store deleted", map[string]interface{}{"inum": inum})

	d.SetId("")

	return nil
}
