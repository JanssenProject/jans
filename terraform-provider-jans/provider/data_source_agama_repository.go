package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceAgamaRepository() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for retrieving Agama repositories.",
		ReadContext: dataSourceAgamaRepositoryRead,
		Schema: map[string]*schema.Schema{
			"repositories": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "List of Agama repositories.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"name": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Name of the repository.",
						},
						"description": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Description of the repository.",
						},
						"url": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "URL of the repository.",
						},
						"metadata": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Repository metadata as JSON string.",
						},
					},
				},
			},
		},
	}
}

func dataSourceAgamaRepositoryRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	repos, err := c.GetAgamaRepositories(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	d.SetId("agama_repositories")

	repoList := make([]map[string]interface{}, len(repos))
	for i, repo := range repos {
		repoList[i] = map[string]interface{}{
			"name":        repo.Name,
			"description": repo.Description,
			"url":         repo.URL,
			"metadata":    string(repo.Metadata),
		}
	}

	if err := d.Set("repositories", repoList); err != nil {
		return diag.FromErr(err)
	}

	return nil
}
