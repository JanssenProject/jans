package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

// scriptLookupResultSchema is the (read-only) summary returned by the custom
// script lookup data sources. It exposes the identifying and metadata fields a
// caller needs; it intentionally does not mirror the full script resource
// schema (module/configuration properties are omitted).
func scriptLookupResultSchema() map[string]*schema.Schema {
	return map[string]*schema.Schema{
		"dn":                   {Type: schema.TypeString, Computed: true, Description: "Distinguished name of the script."},
		"inum":                 {Type: schema.TypeString, Computed: true, Description: "XRI i-number, used as the unique identifier of the script."},
		"name":                 {Type: schema.TypeString, Computed: true, Description: "Name of the script."},
		"aliases":              {Type: schema.TypeList, Computed: true, Description: "Aliases of the script.", Elem: &schema.Schema{Type: schema.TypeString}},
		"description":          {Type: schema.TypeString, Computed: true, Description: "Description of the script."},
		"script":               {Type: schema.TypeString, Computed: true, Description: "The actual script source."},
		"script_type":          {Type: schema.TypeString, Computed: true, Description: "The type of the script."},
		"programming_language": {Type: schema.TypeString, Computed: true, Description: "Programming language the script is written in."},
		"level":                {Type: schema.TypeInt, Computed: true, Description: "Level of the script."},
		"revision":             {Type: schema.TypeInt, Computed: true, Description: "Revision of the script."},
		"enabled":              {Type: schema.TypeBool, Computed: true, Description: "Whether the script is enabled."},
		"location_type":        {Type: schema.TypeString, Computed: true, Description: "Where the script is stored (e.g. db, file)."},
		"location_path":        {Type: schema.TypeString, Computed: true, Description: "Path to the script when stored on the file system."},
	}
}

func flattenScriptLookup(s jans.Script) map[string]interface{} {
	return map[string]interface{}{
		"dn":                   s.Dn,
		"inum":                 s.Inum,
		"name":                 s.Name,
		"aliases":              s.Aliases,
		"description":          s.Description,
		"script":               s.Script,
		"script_type":          s.ScriptType,
		"programming_language": s.ProgrammingLanguage,
		"level":                s.Level,
		"revision":             s.Revision,
		"enabled":              s.Enabled,
		"location_type":        s.LocationType,
		"location_path":        s.LocationPath,
	}
}

func dataSourceScriptByName() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for looking up a custom script by its name (GET /config/scripts/name/{name}).",
		ReadContext: dataSourceScriptByNameRead,
		Schema: addSchemaField(scriptLookupResultSchema(), "name", &schema.Schema{
			Type:        schema.TypeString,
			Required:    true,
			Description: "The name of the script to look up.",
		}),
	}
}

func dataSourceScriptByNameRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	name := d.Get("name").(string)
	script, err := c.GetScriptByName(ctx, name)
	if err != nil {
		return diag.FromErr(err)
	}

	for k, v := range flattenScriptLookup(*script) {
		if err := d.Set(k, v); err != nil {
			return diag.FromErr(err)
		}
	}
	d.SetId(script.Inum)

	return nil
}

func dataSourceScriptsByType() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for looking up custom scripts by their type (GET /config/scripts/type/{type}).",
		ReadContext: dataSourceScriptsByTypeRead,
		Schema: map[string]*schema.Schema{
			"script_type": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The script type to look up (e.g. person_authentication, introspection).",
			},
			"scripts": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "The scripts of the given type.",
				Elem:        &schema.Resource{Schema: scriptLookupResultSchema()},
			},
		},
	}
}

func dataSourceScriptsByTypeRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	scriptType := d.Get("script_type").(string)
	scripts, err := c.GetScriptsByType(ctx, scriptType)
	if err != nil {
		return diag.FromErr(err)
	}

	out := make([]interface{}, 0, len(scripts))
	for _, s := range scripts {
		out = append(out, flattenScriptLookup(s))
	}

	if err := d.Set("scripts", out); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(scriptType)

	return nil
}

// addSchemaField returns a copy of base with the extra field added under key.
func addSchemaField(base map[string]*schema.Schema, key string, field *schema.Schema) map[string]*schema.Schema {
	out := make(map[string]*schema.Schema, len(base)+1)
	for k, v := range base {
		out[k] = v
	}
	out[key] = field
	return out
}
