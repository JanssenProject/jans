package jans

import (
	"context"
	"fmt"
)

// Message represents a message entity
type Message struct {
	ID          string `schema:"id" json:"id,omitempty"`
	Key         string `schema:"key" json:"key,omitempty"`
	Value       string `schema:"value" json:"value,omitempty"`
	Language    string `schema:"language" json:"language,omitempty"`
	Application string `schema:"application" json:"application,omitempty"`
}

// GetMessages retrieves all messages
func (c *Client) GetMessages(ctx context.Context) ([]Message, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	var messages []Message
	if err = c.get(ctx, "/jans-config-api/api/v1/config/messages", token, &messages); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return messages, nil
}

// CreateMessage creates a new message
func (c *Client) CreateMessage(ctx context.Context, message *Message) (*Message, error) {
	if message == nil {
		return nil, fmt.Errorf("message is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Message{}
	if err := c.post(ctx, "/jans-config-api/api/v1/config/messages", token, message, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// GetMessage retrieves a specific message by ID
func (c *Client) GetMessage(ctx context.Context, id string) (*Message, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Message{}
	if err = c.get(ctx, "/jans-config-api/api/v1/config/messages/"+id, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// UpdateMessage updates an existing message
func (c *Client) UpdateMessage(ctx context.Context, message *Message) (*Message, error) {
	if message == nil {
		return nil, fmt.Errorf("message is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Message{}
	if err := c.put(ctx, "/jans-config-api/api/v1/config/messages/"+message.ID, token, message, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteMessage deletes a message by ID
func (c *Client) DeleteMessage(ctx context.Context, id string) error {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/config/messages/"+id, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}

type PostgresMessageConfiguration struct {
	DriverClassName        string `schema:"driver_class_name" json:"driverClassName,omitempty"`
	DbSchemaName           string `schema:"db_schema_name" json:"dbSchemaName,omitempty"`
	ConnectionUri          string `schema:"connection_uri" json:"connectionUri,omitempty"`
	AuthUserName           string `schema:"auth_user_name" json:"authUserName,omitempty"`
	AuthUserPassword       string `schema:"auth_user_password" json:"authUserPassword,omitempty"`
	ConnectionPoolMaxTotal int32  `schema:"connection_pool_max_total" json:"connectionPoolMaxTotal,omitempty"`
	ConnectionPoolMaxIdle  int32  `schema:"connection_pool_max_idle" json:"connectionPoolMaxIdle,omitempty"`
	ConnectionPoolMinIdle  int32  `schema:"connection_pool_min_idle" json:"connectionPoolMinIdle,omitempty"`
	MessageWaitMillis      int32  `schema:"message_wait_millis" json:"messageWaitMillis,omitempty"`
	MessageSleepThreadTime int32  `schema:"message_sleep_thread_millis" json:"messageSleepThreadTime,omitempty"`
}

type RedisMessageConfiguration struct {
	RedisProviderType       string `schema:"redis_provider_type" json:"redisProviderType,omitempty"`
	Servers                 string `schema:"servers" json:"servers,omitempty"`
	DefaultPutExpiration    int32  `schema:"default_put_expiration" json:"defaultPutExpiration,omitempty"`
	SentinelMasterGroupName string `schema:"sentinel_master_group_name" json:"sentinelMasterGroupName,omitempty"`
	Password                string `schema:"password" json:"password,omitempty"`
	UseSSL                  bool   `schema:"use_ssl" json:"useSSL,omitempty"`
	SslTrustStoreFilePath   string `schema:"ssl_trust_store_file_path" json:"sslTrustStoreFilePath,omitempty"`
	SslTrustStorePassword   string `schema:"ssl_trust_store_password" json:"sslTrustStorePassword,omitempty"`
	SslKeyStoreFilePath     string `schema:"ssl_key_store_file_path" json:"sslKeyStoreFilePath,omitempty"`
	SslKeyStorePassword     string `schema:"ssl_key_store_password" json:"sslKeyStorePassword,omitempty"`
	MaxIdleConnections      int32  `schema:"max_idle_connections" json:"maxIdleConnections,omitempty"`
	MaxTotalConnections     int32  `schema:"max_total_connections" json:"maxTotalConnections,omitempty"`
	ConnectionTimeout       int32  `schema:"connection_timeout" json:"connectionTimeout,omitempty"`
	SoTimeout               int32  `schema:"so_timeout" json:"soTimeout,omitempty"`
	MaxRetryAttempts        int32  `schema:"max_retry_attempts" json:"maxRetryAttempts,omitempty"`
}

type MessageConfiguration struct {
	MessageProviderType   string                        `schema:"message_provider_type" json:"messageProviderType,omitempty"`
	RedisConfiguration    *RedisMessageConfiguration    `schema:"redis_configuration" json:"redisConfiguration,omitempty"`
	PostgresConfiguration *PostgresMessageConfiguration `schema:"postgres_configuration" json:"postgresConfiguration,omitempty"`
}

func (c *Client) GetMessageConfiguration(ctx context.Context) (*MessageConfiguration, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	resp := &MessageConfiguration{}

	if err = c.get(ctx, "/jans-config-api/api/v1/config/message", token, resp); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return resp, nil
}

func (c *Client) PatchMessage(ctx context.Context, patches []PatchRequest) (*MessageConfiguration, error) {

	if len(patches) == 0 {
		return c.GetMessageConfiguration(ctx)
	}

	orig, err := c.GetMessageConfiguration(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to get fido2 configuration: %w", err)
	}

	updates, err := createPatchesDiff(orig, patches)
	if err != nil {
		return nil, fmt.Errorf("failed to create patches: %w", err)
	}

	if len(updates) == 0 {
		return orig, nil
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.patch(ctx, "/jans-config-api/api/v1/config/message", token, updates); err != nil {
		return nil, fmt.Errorf("patch request failed: %w", err)
	}

	return c.GetMessageConfiguration(ctx)
}

func (c *Client) GetMessagePostgres(ctx context.Context) (*PostgresMessageConfiguration, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	resp := &PostgresMessageConfiguration{}

	if err = c.get(ctx, "/jans-config-api/api/v1/config/message/postgres", token, resp); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return resp, nil
}

func (c *Client) CreateMessagePostgres(ctx context.Context, postgres *PostgresMessageConfiguration) (*PostgresMessageConfiguration, error) {

	if postgres == nil {
		return nil, fmt.Errorf("postgres is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &PostgresMessageConfiguration{}

	if err := c.put(ctx, "/jans-config-api/api/v1/config/message/postgres", token, postgres, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

func (c *Client) PatchMessagePostgres(ctx context.Context, patches []PatchRequest) (*PostgresMessageConfiguration, error) {

	if len(patches) == 0 {
		return c.GetMessagePostgres(ctx)
	}

	orig, err := c.GetMessagePostgres(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to get fido2 configuration: %w", err)
	}

	updates, err := createPatchesDiff(orig, patches)
	if err != nil {
		return nil, fmt.Errorf("failed to create patches: %w", err)
	}

	if len(updates) == 0 {
		return orig, nil
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.patch(ctx, "/jans-config-api/api/v1/config/message/postgres", token, updates); err != nil {
		return nil, fmt.Errorf("patch request failed: %w", err)
	}

	return c.GetMessagePostgres(ctx)
}

func (c *Client) GetMessageRedis(ctx context.Context) (*RedisMessageConfiguration, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	resp := &RedisMessageConfiguration{}

	if err = c.get(ctx, "/jans-config-api/api/v1/config/message/redis", token, resp); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return resp, nil
}

func (c *Client) CreateMessageRedis(ctx context.Context, redis *RedisMessageConfiguration) (*RedisMessageConfiguration, error) {

	if redis == nil {
		return nil, fmt.Errorf("redis is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &RedisMessageConfiguration{}

	if err := c.put(ctx, "/jans-config-api/api/v1/config/message/redis", token, redis, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

func (c *Client) PatchMessageRedis(ctx context.Context, patches []PatchRequest) (*RedisMessageConfiguration, error) {

	if len(patches) == 0 {
		return c.GetMessageRedis(ctx)
	}

	orig, err := c.GetMessageRedis(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to get fido2 configuration: %w", err)
	}

	updates, err := createPatchesDiff(orig, patches)
	if err != nil {
		return nil, fmt.Errorf("failed to create patches: %w", err)
	}

	if len(updates) == 0 {
		return orig, nil
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/message.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.patch(ctx, "/jans-config-api/api/v1/config/message/redis", token, updates); err != nil {
		return nil, fmt.Errorf("patch request failed: %w", err)
	}

	return c.GetMessageRedis(ctx)
}
