package provider

import (
	"context"
	"errors"
	"fmt"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

var (
	signingAlgs    = []string{"HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512"}
	encryptionAlgs = []string{"RSA1_5", "RSA-OAEP", "A128KW", "A256KW"}
	encryptionEnc  = []string{"A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM"}
)

func Provider() *schema.Provider {
	return &schema.Provider{

		// Schema is the schema for the configuration of this provider. If this
		// provider has no configuration, this can be omitted.
		//
		// The keys of this map are the configuration keys, and the value is
		// the schema describing the value of the configuration.
		Schema: map[string]*schema.Schema{
			"client_id": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The client ID, part of authentication for accessing the Jans APIs.",
				DefaultFunc: schema.EnvDefaultFunc("JANS_CLIENT_ID", nil),
			},
			"client_secret": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The client secret, part of the authentication for accessing the Jans APIs.",
				DefaultFunc: schema.EnvDefaultFunc("JANS_CLIENT_SECRET", nil),
			},
			"url": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The URL for the Jans server.",
				DefaultFunc: schema.EnvDefaultFunc("JANS_URL", nil),
			},
			"insecure_client": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Flag disabling SSL verification when talking to the server. This is useful for testing with self-signed certificates.",
				DefaultFunc: schema.EnvDefaultFunc("JANS_INSECURE_CLIENT", false),
			},
		},

		// ResourcesMap is the list of available resources that this provider
		// can manage, along with their Resource structure defining their
		// own schemas and CRUD operations.
		//
		// Provider automatically handles routing operations such as Apply,
		// Diff, etc. to the proper resource.
		ResourcesMap: map[string]*schema.Resource{
			"jans_admin_ui_permission":              resourceAdminUIPermission(),
			"jans_admin_ui_role":                    resourceAdminUIRole(),
			"jans_admin_ui_role_permission_mapping": resourceAdminUIRolePermissionMapping(),
			"jans_agama_flow":                       resourceAgamaFlow(),
			"jans_app_configuration":                resourceAppConfiguration(),
			"jans_attribute":                        resourceAttribute(),
			"jans_cache_configuration":              resourceCacheConfiguration(),
			"jans_custom_user":                      resourceCustomUser(),
			"jans_default_authentication_method":    resourceDefaultAuthenticationMethod(),
			"jans_fido_device":                      resourceFidoDevice(),
			"jans_fido2_configuration":              resourceFido2Configuration(),
			"jans_fido2_device":                     resourceFido2Device(),
			"jans_group":                            resourceGroup(),
			"jans_json_web_key":                     resourceJsonWebKey(),
			"jans_ldap_database_configuration":      resourceLDAPDatabaseConfiguration(),
			"jans_logging_configuration":            resourceLoggingConfiguration(),
			"jans_oidc_client":                      resourceOidcClient(),
			"jans_organization":                     resourceOrganization(),
			"jans_scim_app_configuration":           resourceScimAppConfiguration(),
			"jans_scope":                            resourceScope(),
			"jans_script":                           resourceScript(),
			"jans_smtp_configuration":               resourceSmtpConfiguration(),
			"jans_uma_resource":                     resourceUMAResource(),
			"jans_user":                             resourceUser(),
		},

		// DataSourcesMap is the collection of available data sources that
		// this provider implements, with a Resource instance defining
		// the schema and Read operation of each.
		//
		// Resource instances for data sources must have a Read function
		// and must *not* implement Create, Update or Delete.
		DataSourcesMap: map[string]*schema.Resource{
			"jans_fido2_configuration":     dataSourceFido2Configuration(),
			"jans_persistence_config":      dataSourcePersistenceConfiguration(),
			"jans_schema":                  dataSourceSchema(),
			"jans_service_provider_config": dataSourceServiceProviderConfig(),
		},

		// ConfigureContextFunc is a function for configuring the provider. If the
		// provider doesn't need to be configured, this can be omitted. This function
		// receives a context.Context that will cancel when Terraform sends a
		// cancellation signal. This function can yield Diagnostics.
		ConfigureContextFunc: providerConfigure,
	}

}

func providerConfigure(ctx context.Context, data *schema.ResourceData) (interface{}, diag.Diagnostics) {
	clientId := data.Get("client_id").(string)
	clientSecret := data.Get("client_secret").(string)
	insecureClient := data.Get("insecure_client").(bool)

	var url string

	if v, ok := data.GetOk("url"); ok {
		url = v.(string)
	}

	// Warning or errors can be collected in a slice type
	var diags diag.Diagnostics

	if url == "" {
		diags = append(diags, diag.Diagnostic{
			Severity: diag.Error,
			Summary:  "No server URL found",
			Detail:   "Unable to connect to Janssen APIs without a valid URL.",
		})
		return nil, diags
	}

	if clientId == "" {
		diags = append(diags, diag.Diagnostic{
			Severity: diag.Error,
			Summary:  "No clientID found",
			Detail:   "Unable to connect to Janssen APIs without valid credentials.",
		})
		return nil, diags
	}

	if insecureClient {
		c, err := jans.NewInsecureClient(url, clientId, clientSecret)
		if err != nil {
			diags = append(diags, diag.Diagnostic{
				Severity: diag.Error,
				Summary:  "Unable to create Janssen client",
				Detail:   "Unable to authenticate user to talk to Janssen APIs",
			})

			return nil, diags
		}
		return c, diags
	}

	c, err := jans.NewClient(url, clientId, clientSecret)
	if err != nil {
		diags = append(diags, diag.Diagnostic{
			Severity: diag.Error,
			Summary:  "Unable to create Janssen client",
			Detail:   "Unable to authenticate user to talk to Janssen APIs",
		})

		return nil, diags
	}

	return c, diags

}

// resourceBlockCreate is used with resources that do not have a create method. Mostly those
// are configurations for the instance that exist only once and instead of creating, users
// have to import them.
func resourceBlockCreate(ctx context.Context, d *schema.ResourceData, m any) diag.Diagnostics {
	var diags diag.Diagnostics

	diags = append(diags, diag.Diagnostic{
		Severity: diag.Error,
		Summary:  "Create not supported",
		Detail:   "This resource does not support create operations. Please import it instead.",
	})

	return diags
}

// resourceUntrackOnDelete is used with resources that do not have a delete method. Mostly those
// are configurations for the instance that exist only once. If such a resource is deleted from
// the Terraoform configuration, it will be untracked from the state file.
func resourceUntrackOnDelete(ctx context.Context, d *schema.ResourceData, m any) diag.Diagnostics {
	var diags diag.Diagnostics

	id := d.Id()
	msg := fmt.Sprintf("The resource '%s' cannot be deleted. Instead, it is removed from the state file and will not be tracked by Terraform, until it's imported again.", id)
	diags = append(diags, diag.Diagnostic{
		Severity: diag.Warning,
		Summary:  "Delete not supported",
		Detail:   msg,
	})

	d.SetId("")

	return diags
}

func handleNotFoundError(ctx context.Context, err error, d *schema.ResourceData) diag.Diagnostics {

	var diags diag.Diagnostics

	if err == nil {
		return diags
	}

	if errors.Is(err, jans.ErrorNotFound) {
		tflog.Warn(ctx, "Removing resource from state as it no longer exists", map[string]interface{}{
			"id": d.Id(),
		})
		d.SetId("")
		return diags
	}

	return diag.FromErr(err)
}
