package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceJsonWebKey() *schema.Resource {

	return &schema.Resource{
		CreateContext: resourceJsonWebKeyCreate,
		ReadContext:   resourceJsonWebKeyRead,
		UpdateContext: resourceJsonWebKeyUpdate,
		DeleteContext: resourceJsonWebKeyDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Name of the key.",
			},
			"descr": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "key description.",
			},
			"kid": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The unique identifier for the key.",
			},
			"kty": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The family of cryptographic algorithms used with the key.",
			},
			"use": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "How the key was meant to be used; sig represents the signature.",
			},
			"alg": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The specific cryptographic algorithm used with the key.",
			},
			"crv": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `The crv member identifies the cryptographic curve used with the key. Values defined 
						by this specification are P-256, P-384 and P-521. Additional crv values MAY be used, provided 
						they are understood by implementations using that Elliptic Curve key. The crv value is case 
						sensitive.`,
			},
			"exp": {
				Type:        schema.TypeInt,
				Required:    true,
				Description: "Contains the token expiration timestamp",
			},
			"x5c": {
				Type:     schema.TypeList,
				Optional: true,
				Description: `The x.509 certificate chain. The first entry in the array is the certificate to use 
						for token verification; the other certificates can be used to verify this first certificate.`,
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"n": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The modulus for the RSA public key.",
			},
			"e": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The exponent for the RSA public key.",
			},
			"x": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `The x member contains the x coordinate for the elliptic curve point. It is represented 
						as the base64url encoding of the coordinate's big endian representation.`,
			},
			"y": {
				Type:     schema.TypeString,
				Optional: true,
				Description: `The y member contains the y coordinate for the elliptic curve point. It is represented 
						as the base64url encoding of the coordinate's big endian representation.`,
			},
		},
	}
}

func resourceJsonWebKeyCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var jwk jans.JsonWebKey
	if err := fromSchemaResource(d, &jwk); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new JsonWebKey")
	newJwk, err := c.CreateJsonWebKey(ctx, &jwk)
	if err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New JsonWebKey created", map[string]interface{}{"kid": newJwk.Kid})

	d.SetId(newJwk.Kid)

	return resourceJsonWebKeyRead(ctx, d, meta)
}

func resourceJsonWebKeyRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	kid := d.Id()
	jwk, err := c.GetJsonWebKey(ctx, kid)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, jwk); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(jwk.Kid)

	return diags

}

func resourceJsonWebKeyUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var jwk jans.JsonWebKey
	if err := fromSchemaResource(d, &jwk); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating JsonWebKey", map[string]interface{}{"kid": jwk.Kid})
	if _, err := c.UpdateJsonWebKey(ctx, &jwk); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "JsonWebKey updated", map[string]interface{}{"kid": jwk.Kid})

	return resourceJsonWebKeyRead(ctx, d, meta)
}

func resourceJsonWebKeyDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	kid := d.Id()
	tflog.Debug(ctx, "Deleting JsonWebKey", map[string]interface{}{"kid": kid})
	if err := c.DeleteJsonWebKey(ctx, kid); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "JsonWebKey deleted", map[string]interface{}{"kid": kid})

	return resourceJsonWebKeyRead(ctx, d, meta)
}
