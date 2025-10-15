package provider

import (
        "context"

        "github.com/hashicorp/go-cty/cty"
        "github.com/hashicorp/terraform-plugin-log/tflog"
        "github.com/hashicorp/terraform-plugin-sdk/v2/diag"
        "github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
        "github.com/jans/terraform-provider-jans/jans"
)

func resourceMessage() *schema.Resource {
        return &schema.Resource{
                Description:   "Resource for managing Message.",
                CreateContext: resourceMessageCreate,
                ReadContext:   resourceMessageRead,
                UpdateContext: resourceMessageUpdate,
                DeleteContext: resourceMessageDelete,
                Importer: &schema.ResourceImporter{
                        StateContext: schema.ImportStatePassthroughContext,
                },
                Schema: map[string]*schema.Schema{
                        "id": {
                                Type:        schema.TypeString,
                                Computed:    true,
                                Description: "Message ID.",
                        },
                        "key": {
                                Type:        schema.TypeString,
                                Required:    true,
                                Description: "Message key.",
                        },
                        "value": {
                                Type:        schema.TypeString,
                                Required:    true,
                                Description: "Message value.",
                        },
                        "language": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Message language.",
                        },
                        "application": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Message application.",
                        },
                },
        }
}

func resourceMessageCreate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)
        var message jans.Message

        if err := fromSchemaResource(d, &message); err != nil {
                return diag.FromErr(err)
        }

        tflog.Debug(ctx, "Creating new Message")
        createdMessage, err := c.CreateMessage(ctx, &message)
        if err != nil {
                return diag.FromErr(err)
        }

        d.SetId(createdMessage.ID)
        tflog.Debug(ctx, "New Message created", map[string]interface{}{"id": createdMessage.ID})

        return resourceMessageRead(ctx, d, meta)
}

func resourceMessageRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)

        message, err := c.GetMessage(ctx, d.Id())
        if err != nil {
                return handleNotFoundError(ctx, err, d)
        }

        if err := toSchemaResource(d, message); err != nil {
                return diag.FromErr(err)
        }

        tflog.Debug(ctx, "Message read")
        return nil
}

func resourceMessageUpdate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)

        var message jans.Message
        if err := fromSchemaResource(d, &message); err != nil {
                return diag.FromErr(err)
        }
        message.ID = d.Id()

        updatedMessage, err := c.UpdateMessage(ctx, &message)
        if err != nil {
                return diag.FromErr(err)
        }

        tflog.Debug(ctx, "Message updated", map[string]interface{}{"id": updatedMessage.ID})
        return resourceMessageRead(ctx, d, meta)
}

func resourceMessageDelete(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
        c := meta.(*jans.Client)

        if err := c.DeleteMessage(ctx, d.Id()); err != nil {
                return diag.FromErr(err)
        }

        tflog.Debug(ctx, "Message deleted")
        return nil
}

func resourceMessageConfiguration() *schema.Resource {
        return &schema.Resource{
                Description:   "Resource for managing Message Configuration.",
                CreateContext: resourceMessageConfigurationCreate,
                ReadContext:   resourceMessageConfigurationRead,
                UpdateContext: resourceMessageConfigurationUpdate,
                DeleteContext: resourceUntrackOnDelete,
                Importer: &schema.ResourceImporter{
                        StateContext: schema.ImportStatePassthroughContext,
                },
                Schema: map[string]*schema.Schema{
                        "message_provider_type": {
                                Type:     schema.TypeString,
                                Required: true,
                                ValidateDiagFunc: func(v interface{}, _ cty.Path) diag.Diagnostics {

                                        enums := []string{"DISABLED", "REDIS", "POSTGRES"}
                                        return validateEnum(v, enums)
                                },
                                Description: "Message provider type.",
                        },
                        "postgres_configuration": {
                                Type:        schema.TypeList,
                                Required:    true,
                                Description: "Postgres configuration.",
                                Elem:        resourcePostgresConfiguration(),
                                MaxItems:    1,
                        },
                        "redis_configuration": {
                                Type:        schema.TypeList,
                                Required:    true,
                                Description: "Postgres configuration.",
                                Elem:        resourceRedisConfiguration(),
                                MaxItems:    1,
                        },
                },
        }
}

func resourceNullConfiguration() *schema.Resource {
        return &schema.Resource{
                Description: "Resource for managing Null Configuration.",
        }
}

func resourcePostgresConfiguration() *schema.Resource {
        return &schema.Resource{
                Description: "Resource for managing Postgres Configuration.",
                Schema: map[string]*schema.Schema{
                        "driver_class_name": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Driver class name.",
                        },
                        "db_schema_name": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Name of the database schema.",
                        },
                        "connection_uri": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Connection URI of the database.",
                        },
                        "auth_user_name": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Username for authenticating.",
                        },
                        "auth_user_password": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Password for authenticating.",
                        },
                        "connection_pool_max_total": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Maximum number of connections.",
                        },
                        "connection_pool_max_idle": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Maximum number of idle connections.",
                        },
                        "connection_pool_min_idle": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Minimum number of idle connections.",
                        },
                        "message_wait_millis": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Time to wait for a message.",
                        },
                        "message_sleep_thread_millis": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Time to sleep for a message.",
                        },
                },
        }
}

func resourceRedisConfiguration() *schema.Resource {
        return &schema.Resource{
                Description: "Resource for managing Redis Configuration.",
                Schema: map[string]*schema.Schema{
                        "redis_provider_type": {
                                Type:     schema.TypeString,
                                Optional: true,
                                ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

                                        enums := []string{"STANDALONE", "CLUSTER", "SHARED", "SENTINEL"}
                                        return validateEnum(v, enums)
                                },
                                Description: "Redis provider type.",
                        },
                        "servers": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Redis servers.",
                        },
                        "default_put_expiration": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Default put expiration.",
                        },
                        "sentinel_master_group_name": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Sentinel master group name.",
                        },
                        "password": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "Password for authenticating.",
                        },
                        "use_ssl": {
                                Type:        schema.TypeBool,
                                Optional:    true,
                                Description: "Whether to use SSL.",
                        },
                        "ssl_trust_store_file_path": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "SSL trust store file path.",
                        },
                        "ssl_trust_store_password": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "SSL trust store password.",
                        },
                        "ssl_key_store_file_path": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "SSL key store file path.",
                        },
                        "ssl_key_store_password": {
                                Type:        schema.TypeString,
                                Optional:    true,
                                Description: "SSL key store password.",
                        },
                        "max_idle_connections": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Maximum number of idle connections.",
                        },
                        "max_total_connections": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Maximum number of connections.",
                        },
                        "connection_timeout": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Connection timeout.",
                        },
                        "so_timeout": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "SO timeout.",
                        },
                        "max_retry_attempts": {
                                Type:        schema.TypeInt,
                                Optional:    true,
                                Description: "Maximum number of retry attempts.",
                        },
                },
        }
}

func resourceMessageConfigurationCreate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {

        c := meta.(*jans.Client)
        var message jans.MessageConfiguration

        if err := fromSchemaResource(d, &message); err != nil {
                return diag.FromErr(err)
        }

        tflog.Debug(ctx, "Creating new Message Configuration")
        if message.MessageProviderType == "NULL" {
                return diag.Errorf("Message provider type cannot be NULL")
        } else if message.MessageProviderType == "REDIS" && message.RedisConfiguration != nil {
                if _, err := c.CreateMessageRedis(ctx, message.RedisConfiguration); err != nil {
                        return diag.FromErr(err)
                }
        } else if message.MessageProviderType == "POSTGRES" && message.PostgresConfiguration != nil {
                if _, err := c.CreateMessagePostgres(ctx, message.PostgresConfiguration); err != nil {
                        return diag.FromErr(err)
                }
        }

        tflog.Debug(ctx, "New Message Configuration created", map[string]interface{}{"message_provider_type": message.MessageProviderType})

        return resourceMessageConfigurationRead(ctx, d, meta)
}

func resourceMessageConfigurationRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {

        c := meta.(*jans.Client)

        message, err := c.GetMessageConfiguration(ctx)
        if err != nil {
                return handleNotFoundError(ctx, err, d)
        }

        if err := toSchemaResource(d, message); err != nil {
                return diag.FromErr(err)
        }

        tflog.Debug(ctx, "Message Configuration read")

        return nil
}

func resourceMessageConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {

        c := meta.(*jans.Client)

        var message jans.MessageConfiguration
        patches, err := patchFromResourceData(d, &message)
        if err != nil {
                return diag.FromErr(err)
        }

        if _, err := c.PatchMessage(ctx, patches); err != nil {
                return diag.FromErr(err)
        }

        tflog.Debug(ctx, "Message Configuration updated", map[string]interface{}{"message_provider_type": message.MessageProviderType})

        return resourceMessageConfigurationRead(ctx, d, meta)
}
