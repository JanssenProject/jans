package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceAdminUIWebhook() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing AdminUI webhooks.",
		CreateContext: resourceAdminUIWebhookCreate,
		ReadContext:   resourceAdminUIWebhookRead,
		UpdateContext: resourceAdminUIWebhookUpdate,
		DeleteContext: resourceAdminUIWebhookDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "DN of the webhook.",
			},
			"inum": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Unique identifier of the webhook.",
			},
			"display_name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Human-readable name of the webhook.",
			},
			"description": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Description of the webhook.",
			},
			"url": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "URL the webhook invokes.",
			},
			"http_request_body_string": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Request body sent by the webhook, as a raw string.",
			},
			"http_method": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "HTTP method used when invoking the webhook.",
			},
			"jans_enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Whether the webhook is enabled.",
			},
			"http_headers": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "HTTP headers sent when invoking the webhook.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"key": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Header name.",
						},
						"value": {
							Type:        schema.TypeString,
							Optional:    true,
							Description: "Header value.",
						},
					},
				},
			},
			"aui_feature_ids": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "AdminUI feature ids this webhook is bound to.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"http_request_body": {
				Type:        schema.TypeMap,
				Optional:    true,
				Description: "Request body sent by the webhook, as key/value pairs.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"base_dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Base DN of the webhook.",
			},
		},
	}
}

func resourceAdminUIWebhookCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var webhook jans.WebhookEntry
	if err := fromSchemaResource(d, &webhook); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new AdminUI webhook")
	created, err := c.CreateAdminUIWebhook(ctx, &webhook)
	if err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New AdminUI webhook created", map[string]interface{}{"inum": created.Inum})

	d.SetId(created.Inum)

	return resourceAdminUIWebhookRead(ctx, d, meta)
}

func resourceAdminUIWebhookRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	inum := d.Id()
	webhook, err := c.GetAdminUIWebhook(ctx, inum)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, webhook); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(webhook.Inum)

	return diags
}

func resourceAdminUIWebhookUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var webhook jans.WebhookEntry
	if err := fromSchemaResource(d, &webhook); err != nil {
		return diag.FromErr(err)
	}
	webhook.Inum = d.Id()

	tflog.Debug(ctx, "Updating AdminUI webhook", map[string]interface{}{"inum": webhook.Inum})
	if _, err := c.UpdateAdminUIWebhook(ctx, &webhook); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "AdminUI webhook updated", map[string]interface{}{"inum": webhook.Inum})

	return resourceAdminUIWebhookRead(ctx, d, meta)
}

func resourceAdminUIWebhookDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	inum := d.Id()
	tflog.Debug(ctx, "Deleting AdminUI webhook", map[string]interface{}{"inum": inum})
	if err := c.DeleteAdminUIWebhook(ctx, inum); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "AdminUI webhook deleted", map[string]interface{}{"inum": inum})

	d.SetId("")

	return nil
}
