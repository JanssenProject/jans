package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/moabu/terraform-provider-jans/jans"
)

func dataSourceSchema() *schema.Resource {

	return &schema.Resource{
		Description: "Data source that provides information about a specific supported schema. " +
			"Schemas have nested attributes, however, Terraform does not support recursive data structures. " +
			"Because of this, the attributes are limitted to 3 levels. ",
		ReadContext: dataSourceSchemaRead,
		Schema: map[string]*schema.Schema{
			"id": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The unique identifier for the group.",
			},
			"schemas": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A list of URIs of the schemas used to define the attributes of the group.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"meta": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A complex type that contains meta attributes associated with the resource.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"resource_type": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The resource type of the group.",
						},
						"location": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The URI of the group.",
						},
						"created": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The date and time the group was created.",
						},
						"last_modified": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The date and time the group was last modified.",
						},
					},
				},
			},
			"name": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "The name of the schema.",
			},
			"description": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "The description of the schema.",
			},
			"attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of attributes of the schema.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"name": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The name of the attribute.",
						},
						"type": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The type of the attribute.",
						},
						"multi_valued": {
							Type:        schema.TypeBool,
							Computed:    true,
							Description: "Indicates if the attribute can hold multiple values.",
						},
						"description": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The description of the attribute.",
						},
						"required": {
							Type:        schema.TypeBool,
							Computed:    true,
							Description: "Indicates if the attribute is required.",
						},
						"canonical_values": {
							Type:        schema.TypeList,
							Computed:    true,
							Description: "A list of canonical values of the attribute.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"case_exact": {
							Type:        schema.TypeBool,
							Computed:    true,
							Description: "Indicates if the attribute is case sensitive.",
						},
						"mutability": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Indicates if the attribute is read-only, read-write, or immutable.",
						},
						"returned": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Indicates when the attribute is returned in a response.",
						},
						"uniqueness": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Indicates if the attribute is unique.",
						},
						"reference_types": {
							Type:        schema.TypeList,
							Computed:    true,
							Description: "A list of reference types of the attribute.",
							Elem: &schema.Schema{
								Type: schema.TypeString,
							},
						},
						"sub_attributes": {
							Type:        schema.TypeList,
							Optional:    true,
							Description: "A list of sub-attributes of the attribute.",
							Elem: &schema.Resource{
								Schema: map[string]*schema.Schema{
									"name": {
										Type:        schema.TypeString,
										Computed:    true,
										Description: "The name of the attribute.",
									},
									"type": {
										Type:        schema.TypeString,
										Computed:    true,
										Description: "The type of the attribute.",
									},
									"multi_valued": {
										Type:        schema.TypeBool,
										Computed:    true,
										Description: "Indicates if the attribute can hold multiple values.",
									},
									"description": {
										Type:        schema.TypeString,
										Computed:    true,
										Description: "The description of the attribute.",
									},
									"required": {
										Type:        schema.TypeBool,
										Computed:    true,
										Description: "Indicates if the attribute is required.",
									},
									"canonical_values": {
										Type:        schema.TypeList,
										Computed:    true,
										Description: "A list of canonical values of the attribute.",
										Elem: &schema.Schema{
											Type: schema.TypeString,
										},
									},
									"case_exact": {
										Type:        schema.TypeBool,
										Computed:    true,
										Description: "Indicates if the attribute is case sensitive.",
									},
									"mutability": {
										Type:        schema.TypeString,
										Computed:    true,
										Description: "Indicates if the attribute is read-only, read-write, or immutable.",
									},
									"reference_types": {
										Type:        schema.TypeList,
										Computed:    true,
										Description: "A list of reference types of the attribute.",
										Elem: &schema.Schema{
											Type: schema.TypeString,
										},
									},
									"returned": {
										Type:        schema.TypeString,
										Computed:    true,
										Description: "Indicates when the attribute is returned in a response.",
									},
									"uniqueness": {
										Type:        schema.TypeString,
										Computed:    true,
										Description: "Indicates if the attribute is unique.",
									},
									"sub_attributes": {
										Type:        schema.TypeList,
										Optional:    true,
										Description: "A list of sub-attributes of the attribute.",
										Elem: &schema.Resource{
											Schema: map[string]*schema.Schema{
												"name": {
													Type:        schema.TypeString,
													Computed:    true,
													Description: "The name of the attribute.",
												},
												"type": {
													Type:        schema.TypeString,
													Computed:    true,
													Description: "The type of the attribute.",
												},
												"multi_valued": {
													Type:        schema.TypeBool,
													Computed:    true,
													Description: "Indicates if the attribute can hold multiple values.",
												},
												"description": {
													Type:        schema.TypeString,
													Computed:    true,
													Description: "The description of the attribute.",
												},
												"required": {
													Type:        schema.TypeBool,
													Computed:    true,
													Description: "Indicates if the attribute is required.",
												},
												"canonical_values": {
													Type:        schema.TypeList,
													Computed:    true,
													Description: "A list of canonical values of the attribute.",
													Elem: &schema.Schema{
														Type: schema.TypeString,
													},
												},
												"case_exact": {
													Type:        schema.TypeBool,
													Computed:    true,
													Description: "Indicates if the attribute is case sensitive.",
												},
												"mutability": {
													Type:        schema.TypeString,
													Computed:    true,
													Description: "Indicates if the attribute is read-only, read-write, or immutable.",
												},
												"returned": {
													Type:        schema.TypeString,
													Computed:    true,
													Description: "Indicates when the attribute is returned in a response.",
												},
												"reference_types": {
													Type:        schema.TypeList,
													Computed:    true,
													Description: "A list of reference types of the attribute.",
													Elem: &schema.Schema{
														Type: schema.TypeString,
													},
												},
												"uniqueness": {
													Type:        schema.TypeString,
													Computed:    true,
													Description: "Indicates if the attribute is unique.",
												},
												// "sub_attributes": {
												// 	Type:        schema.TypeList,
												// 	Optional:    true,
												// 	Description: "A list of sub-attributes of the attribute.",
												// },
											},
										},
									},
								},
							},
						},
					},
				},
			},
		},
	}
}

func dataSourceSchemaRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	id := d.Get("id").(string)

	schema, err := c.GetSchema(ctx, id)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, schema); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(schema.ID)

	return nil
}
