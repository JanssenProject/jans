package jans

import (
	"context"
	"fmt"
)

// MemcachedConfiguration is the configuration for a memchached cache.
type MemcachedConfiguration struct {
	Servers                 string `schema:"servers" json:"servers"`
	MaxOperationQueueLength int    `schema:"max_operation_queue_length" json:"maxOperationQueueLength"`
	BufferSize              int    `schema:"buffer_size" json:"bufferSize"`
	DefaultPutExpiration    int    `schema:"default_put_expiration" json:"defaultPutExpiration"`
	ConnectionFactoryType   string `schema:"connection_factory_type" json:"connectionFactoryType"`
}

// RedisConfiguration is the configuration for a redis cache.
type RedisConfiguration struct {
	RedisProviderType       string `schema:"redis_provider_type" json:"redisProviderType"`
	Servers                 string `schema:"servers" json:"servers"`
	Password                string `schema:"password" json:"password"`
	DefaultPutExpiration    int    `schema:"default_put_expiration" json:"defaultPutExpiration"`
	SentinelMasterGroupName string `schema:"sentinel_master_group_name" json:"sentinelMasterGroupName"`
	UseSSL                  bool   `schema:"use_ssl" json:"useSSL"`
	SslTrustStoreFilePath   string `schema:"ssl_trust_store_file_path" json:"sslTrustStoreFilePath"`
	SslTrustStorePassword   string `schema:"ssl_trust_store_password" json:"sslTrustStorePassword"`
	MaxIdleConnections      int    `schema:"max_idle_connections" json:"maxIdleConnections"`
	MaxTotalConnections     int    `schema:"max_total_connections" json:"maxTotalConnections"`
	ConnectionTimeout       int    `schema:"connection_timeout" json:"connectionTimeout"`
	SoTimeout               int    `schema:"so_timeout" json:"soTimeout"`
	MaxRetryAttempts        int    `schema:"max_retry_attempts" json:"maxRetryAttempts"`
}

// InMemoryCacheConfiguration is the configuration for an in-memory cache.
type InMemoryCacheConfiguration struct {
	DefaultPutExpiration int `schema:"default_put_expiration" json:"defaultPutExpiration"`
}

// NativePersistenceConfiguration is the configuration for a native persistence cache.
type NativePersistenceConfiguration struct {
	DefaultPutExpiration             int  `schema:"default_put_expiration" json:"defaultPutExpiration"`
	DefaultCleanupBatchSize          int  `schema:"default_cleanup_batch_size" json:"defaultCleanupBatchSize"`
	DeleteExpiredOnGetRequest        bool `schema:"delete_expired_on_get_request" json:"deleteExpiredOnGetRequest"`
	DisableAttemptUpdateBeforeInsert bool `schema:"disable_attempt_update_before_insert" json:"disableAttemptUpdateBeforeInsert"`
}

// CacheConfiguration controls the configuration of the cache.
type CacheConfiguration struct {
	CacheProviderType              string                         `schema:"cache_provider_type" json:"cacheProviderType"`
	MemcachedConfiguration         MemcachedConfiguration         `schema:"memcached_configuration" json:"memcachedConfiguration"`
	RedisConfiguration             RedisConfiguration             `schema:"redis_configuration" json:"redisConfiguration"`
	InMemoryConfiguration          InMemoryCacheConfiguration     `schema:"in_memory_configuration" json:"inMemoryConfiguration"`
	NativePersistenceConfiguration NativePersistenceConfiguration `schema:"native_persistence_configuration" json:"nativePersistenceConfiguration"`
}

// GetCacheConfiguration fetches the cache configuration from the server.
func (c *Client) GetCacheConfiguration(ctx context.Context) (*CacheConfiguration, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/cache.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &CacheConfiguration{}

	if err := c.get(ctx, "/jans-config-api/api/v1/config/cache", token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// PatchCacheConfiguration uses the provided list of patch requests to update
// the cache configuration.
func (c *Client) PatchCacheConfiguration(ctx context.Context, patches []PatchRequest) (*CacheConfiguration, error) {

	if len(patches) == 0 {
		return c.GetCacheConfiguration(ctx)
	}

	orig, err := c.GetCacheConfiguration(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to get cache configuration: %w", err)
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/cache.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	updates, err := createPatchesDiff(orig, patches)
	if err != nil {
		return nil, fmt.Errorf("failed to create patches: %w", err)
	}

	if len(updates) == 0 {
		return c.GetCacheConfiguration(ctx)
	}

	if err := c.patch(ctx, "/jans-config-api/api/v1/config/cache", token, updates); err != nil {
		return nil, fmt.Errorf("patch request failed: %w", err)
	}

	return c.GetCacheConfiguration(ctx)
}
