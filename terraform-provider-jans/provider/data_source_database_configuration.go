package provider

import (
        "context"
        "encoding/json"
        "sort"

        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
        "github.com/jans/terraform-provider-jans/jans"
)

func dataSourceDatabaseConfiguration() *schema.Resource {
        return &schema.Resource{
                Description: "Data source for retrieving database schema configuration.",
                ReadContext: dataSourceDatabaseConfigurationRead,
                Schema: map[string]*schema.Schema{
                        "tables": {
                                Type:        schema.TypeList,
                                Computed:    true,
                                Description: "List of database tables and their schema.",
                                Elem: &schema.Resource{
                                        Schema: map[string]*schema.Schema{
                                                "name": {
                                                        Type:        schema.TypeString,
                                                        Computed:    true,
                                                        Description: "Table name.",
                                                },
                                                "fields": {
                                                        Type:        schema.TypeList,
                                                        Computed:    true,
                                                        Description: "List of fields in the table.",
                                                        Elem: &schema.Resource{
                                                                Schema: map[string]*schema.Schema{
                                                                        "name": {
                                                                                Type:        schema.TypeString,
                                                                                Computed:    true,
                                                                                Description: "Field name in the database.",
                                                                        },
                                                                        "def_name": {
                                                                                Type:        schema.TypeString,
                                                                                Computed:    true,
                                                                                Description: "Default/definition name of the field.",
                                                                        },
                                                                        "type": {
                                                                                Type:        schema.TypeString,
                                                                                Computed:    true,
                                                                                Description: "Field data type (varchar, timestamp, jsonb, etc.).",
                                                                        },
                                                                        "multi_valued": {
                                                                                Type:        schema.TypeBool,
                                                                                Computed:    true,
                                                                                Description: "Whether the field supports multiple values.",
                                                                        },
                                                                },
                                                        },
                                                },
                                        },
                                },
                        },
                        "schema_json": {
                                Type:        schema.TypeString,
                                Computed:    true,
                                Description: "Full database schema as JSON string for advanced processing.",
                        },
                },
        }
}

func dataSourceDatabaseConfigurationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)

        dbSchema, err := c.GetDatabaseSchema(ctx)
        if err != nil {
                return diag.FromErr(err)
        }

        d.SetId("database_configuration")

        tableNames := make([]string, 0, len(dbSchema))
        for tableName := range dbSchema {
                tableNames = append(tableNames, tableName)
        }
        sort.Strings(tableNames)

        tables := make([]map[string]interface{}, 0, len(dbSchema))
        for _, tableName := range tableNames {
                tableFields := dbSchema[tableName]

                fieldNames := make([]string, 0, len(tableFields))
                for fieldName := range tableFields {
                        fieldNames = append(fieldNames, fieldName)
                }
                sort.Strings(fieldNames)

                fields := make([]map[string]interface{}, 0, len(tableFields))
                for _, fieldName := range fieldNames {
                        field := tableFields[fieldName]
                        fields = append(fields, map[string]interface{}{
                                "name":         field.Name,
                                "def_name":     field.DefName,
                                "type":         field.Type,
                                "multi_valued": field.MultiValued,
                        })
                }
                tables = append(tables, map[string]interface{}{
                        "name":   tableName,
                        "fields": fields,
                })
        }

        if err := d.Set("tables", tables); err != nil {
                return diag.FromErr(err)
        }

        schemaJSON, err := json.Marshal(dbSchema)
        if err != nil {
                return diag.FromErr(err)
        }

        if err := d.Set("schema_json", string(schemaJSON)); err != nil {
                return diag.FromErr(err)
        }

        return nil
}
