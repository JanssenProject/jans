package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/moabu/terraform-provider-jans/jans"
)

func resourceCacheConfiguration() *schema.Resource {

	return &schema.Resource{
		Description: "Resource for managing the cache configuration for the Janssen server. This resource cannot be " +
			"created or delete, only imported and updated.",
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceCacheConfigurationRead,
		UpdateContext: resourceCacheConfigurationUpdate,
		DeleteContext: resourceUntrackOnDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"cache_provider_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The cache Provider Type.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"IN_MEMORY", "MEMCHACHED", "REDIS", "NATIVE_PERSISTENCE"}
					return validateEnum(v, enums)
				},
			},
			"memcached_configuration": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "The memcached configuration.",
				MaxItems:    1,
				Elem:        resourceMemcachedCacheConfiguration(),
			},
			"redis_configuration": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "The redis configuration.",
				MaxItems:    1,
				Elem:        resourceRedisCacheConfiguration(),
			},
			"in_memory_configuration": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "The redis configuration.",
				MaxItems:    1,
				Elem:        resourceInMemoryCacheConfiguration(),
			},
			"native_persistence_configuration": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "The redis configuration.",
				MaxItems:    1,
				Elem:        resourceNativePersistenceCacheConfiguration(),
			},
		},
	}
}

func resourceInMemoryCacheConfiguration() *schema.Resource {

	return &schema.Resource{
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"default_put_expiration": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "defaultPutExpiration timeout value.",
			},
		},
	}
}

func resourceMemcachedCacheConfiguration() *schema.Resource {

	return &schema.Resource{
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"servers": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Server details separated by spaces.",
			},
			"max_operation_queue_length": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Maximum operation Queue Length.",
			},
			"buffer_size": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Buffer Size.",
			},
			"default_put_expiration": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Expiration timeout value.",
			},
			"connection_factory_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The MemcachedConnectionFactoryType Type.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"DEFAULT", "BINARY"}
					return validateEnum(v, enums)
				},
			},
		},
	}
}

func resourceRedisCacheConfiguration() *schema.Resource {

	return &schema.Resource{
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"redis_provider_type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Type of connection.",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"STANDALONE", "CLUSTER", "SHARDED", "SENTINEL"}
					return validateEnum(v, enums)
				},
			},
			"servers": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "server details separated by comma e.g. 'server1:8080server2:8081'.",
			},
			"password": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Redis password.",
			},
			"default_put_expiration": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "defaultPutExpiration timeout value.",
			},
			"sentinel_master_group_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Sentinel Master Group Name (required if SENTINEL type of connection is selected).",
			},
			"use_ssl": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Enable SSL communication between Gluu Server and Redis cache.",
			},
			"ssl_trust_store_file_path": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Directory Path to Trust Store.",
			},
			"max_idle_connections": {
				Type:     schema.TypeInt,
				Optional: true,
				Description: `The cap on the number of \idle\ instances in the pool. If max idle is set too low on heavily 
						loaded systems it is possible you will see objects being destroyed and almost immediately new objects 
						being created. This is a result of the active threads momentarily returning objects faster than they 
						are requesting them causing the number of idle objects to rise above max idle. The best value for max 
						idle for heavily loaded system will vary but the default is a good starting point.`,
			},
			"max_total_connections": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "The number of maximum connection instances in the pool.",
			},
			"connection_timeout": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Connection time out.",
			},
			"so_timeout": {
				Type:     schema.TypeInt,
				Optional: true,
				Description: `With this option set to a non-zero timeout a read() call on the InputStream associated 
						with this Socket will block for only this amount of time. If the timeout expires a 
						java.net.SocketTimeoutException is raised though the Socket is still valid. The option must be 
						enabled prior to entering the blocking operation to have effect. The timeout must be > 0. A timeout 
						of zero is interpreted as an infinite timeout.`,
			},
			"max_retry_attempts": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Maximum retry attempts in case of failure.",
			},
		},
	}
}

func resourceNativePersistenceCacheConfiguration() *schema.Resource {

	return &schema.Resource{
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"default_put_expiration": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "defaultPutExpiration timeout value.",
			},
			"default_cleanup_batch_size": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "defaultCleanupBatchSize page size.",
			},
			"delete_expired_on_get_request": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "",
			},
			"disable_attempt_update_before_insert": {
				Type:     schema.TypeBool,
				Optional: true,
			},
		},
	}
}

func resourceCacheConfigurationRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	cacheConfig, err := c.GetCacheConfiguration(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, cacheConfig); err != nil {
		return diag.FromErr(err)
	}

	d.SetId("jans_cache_configuration")

	return diags
}

func resourceCacheConfigurationUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var cacheConfig jans.CacheConfiguration
	if err := fromSchemaResource(d, &cacheConfig); err != nil {
		return diag.FromErr(err)
	}

	if err := c.UpdateCacheConfiguration(ctx, &cacheConfig); err != nil {
		return diag.FromErr(err)
	}

	return resourceCacheConfigurationRead(ctx, d, meta)
}
