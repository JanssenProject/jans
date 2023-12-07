package provider

import (
	"context"
	"errors"
	"fmt"
	"io"
	"os"

	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/resource"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceAgamaDeployment() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing agama authentication flow deployments.",
		CreateContext: resourceAgamaDeploymentCreate,
		ReadContext:   resourceAgamaDeploymentRead,
		UpdateContext: resourceBlockUpdate,
		DeleteContext: resourceAgamaDeploymentDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"id": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Agama deployment ID",
			},
			"dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Agama deployment DN",
			},
			"base_dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Agama deployment base DN",
			},
			"name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Agama project name",
				ForceNew:    true,
			},
			"created_at": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Agama deployment creation time",
			},
			"deployment_file": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Path to the deployment file (in zip format)",
			},
			"autoconfigure": {
				Type:     schema.TypeBool,
				Optional: true,
				Default:  false,
				Description: `Passing 'true' will make this project be configured with the sample configurations
				found in the provided binary archive. This param should rarely be passed: use only in controlled 
				environments where the archive is not shared with third parties`,
			},
			"task_active": {
				Type:        schema.TypeBool,
				Computed:    true,
				Description: "Boolean value with default value false.",
			},
		},
	}
}

func resourceAgamaDeploymentCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	name := d.Get("name").(string)
	fileName := d.Get("deployment_file").(string)
	autoconfig := d.Get("autoconfigure").(bool)

	// check if file exists and can be accessed
	if _, err := os.Stat(fileName); err != nil {
		return diag.FromErr(err)
	}

	// read file
	deploymentFile, err := os.Open(fileName)
	if err != nil {
		return diag.FromErr(err)
	}
	defer deploymentFile.Close()

	// read file into byte array
	contents, err := io.ReadAll(deploymentFile)
	if err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new agama deployment")
	if err := c.CreateAgamaDeployment(ctx, name, autoconfig, contents); err != nil {
		return diag.FromErr(err)
	}
	if err := waitForAgamaDeploymetCreation(ctx, c, name); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New agama deployment created", map[string]interface{}{"dn": name})

	d.SetId(name)
	d.Set("name", name)

	return resourceAgamaDeploymentRead(ctx, d, meta)
}

func resourceAgamaDeploymentRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	dn := d.Id()
	deployment, err := c.GetAgamaDeployment(ctx, dn)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	// if err := toSchemaResource(d, deployment); err != nil {
	// 	return diag.FromErr(err)
	// }
	d.SetId(deployment.Name)
	d.Set("name", deployment.Name)

	return diags
}

// func resourceAgamaDeploymentUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

// 	c := meta.(*jans.Client)

// 	var deployment jans.AgamaDeployment
// 	if err := fromSchemaResource(d, &deployment); err != nil {
// 		return diag.FromErr(err)
// 	}
// 	tflog.Debug(ctx, "Updating agama deployment", map[string]interface{}{"dn": deployment.Dn})
// 	if err := c.UpdateAgamaDeployment(ctx, &deployment); err != nil {
// 		return diag.FromErr(err)
// 	}
// 	tflog.Debug(ctx, "Agama deployment updated", map[string]interface{}{"dn": deployment.Dn})

// 	return resourceAgamaDeploymentRead(ctx, d, meta)
// }

func resourceAgamaDeploymentDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	name := d.Id()
	tflog.Debug(ctx, "Deleting agama deployment", map[string]interface{}{"dn": name})
	if err := c.DeleteAgamaDeployment(ctx, name); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Agama deployment deleted", map[string]interface{}{"dn": name})

	return resourceAgamaDeploymentRead(ctx, d, meta)
}

func waitForAgamaDeploymetCreation(ctx context.Context, client *jans.Client, name string) error {

	err := resource.RetryContext(ctx, defaultTimeout, func() *resource.RetryError {
		_, err := client.GetAgamaDeployment(ctx, name)
		if err != nil {
			if !errors.Is(err, jans.ErrorNotFound) {
				return resource.NonRetryableError(err)
			}
			return resource.RetryableError(fmt.Errorf("%q: deployment still creating", name))
		}
		return nil
	})
	if isResourceTimeOut(err) {
		_, err := client.GetAgamaDeployment(ctx, name)
		if err != nil {
			if errors.Is(err, jans.ErrorNotFound) {
				return fmt.Errorf("agama deployment '%s' not found", name)
			}
			return fmt.Errorf("error getting info for agama deployment '%s': %w", name, err)
		}
		return nil
	}
	if err != nil {
		return fmt.Errorf("error waiting for agama deployment creation: %s", err)
	}
	return nil
}
