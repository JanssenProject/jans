package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceSmtpConfiguration() *schema.Resource {

	return &schema.Resource{
		Description: "Resource for managing the SMTP configuration of the Janssen Server. This " +
			"resource cannot be created or deleted, only imported and updated.",
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceSmtpConfigurationRead,
		UpdateContext: resourceSmtpConfigurationUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"valid": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value with default value false.",
			},
			"host": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Hostname of the SMTP server.",
			},
			"port": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Port number of the SMTP server.",
			},
			"connect_protection": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Connect protection type. Possible values are None, StartTls, SslTls.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"None", "StartTls", "SsslTls"}

					return validateEnum(v, enums)
				},
			},
			"trust_host": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value with default value false.",
			},
			"from_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Name of the sender.",
			},
			"from_email_address": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Email Address of the Sender.",
			},
			"requires_authentication": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Boolean value with default value false. It true it will enable sender authentication.",
			},
			"smtp_authentication_account_username": {
				Type:     schema.TypeString,
				Optional: true,
			},
			"smtp_authentication_account_password": {
				Type:     schema.TypeString,
				Optional: true,
			},
			"key_store": {
				Type:     schema.TypeString,
				Optional: true,
			},
			"key_store_password": {
				Type:     schema.TypeString,
				Optional: true,
			},
			"key_store_alias": {
				Type:     schema.TypeString,
				Optional: true,
			},
			"signing_algorithm": {
				Type:     schema.TypeString,
				Optional: true,
			},
		},
	}
}

func resourceSmtpConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	smtpConfig, err := c.GetSMTPConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, smtpConfig); err != nil {
		return diag.FromErr(err)
	}

	d.SetId("jans_smtp_configuration")

	return diags
}

func resourceSmtpConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var smtpConfig jans.SMTPConfiguration
	if err := fromSchemaResource(d, &smtpConfig); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateSMTPConfiguration(ctx, &smtpConfig); err != nil {
		return diag.FromErr(err)
	}

	return resourceSmtpConfigurationRead(ctx, d, meta)
}
