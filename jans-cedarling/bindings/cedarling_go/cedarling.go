// Package cedarling_go provides Golang bindings for the [cedarling] project
//
// [cedarling]: https://docs.jans.io/stable/cedarling/
package cedarling_go

//if you want to use static linking, you need to compile with `go build -tags=static`.

/*
// For static linking: #cgo LDFLAGS: ./lcedarling_go.a
// For dynamic linking: #cgo LDFLAGS: -L. -lcedarling_go
// to use static linking use tag `static`: go build -tags=static .
#cgo static LDFLAGS: ./libcedarling_go.a
#cgo !static LDFLAGS: -L. -lcedarling_go
*/

// #cgo LDFLAGS: -L. -lcedarling_go
import "C"

import (
	"encoding/json"
	"runtime"
	"time"

	"github.com/JanssenProject/jans/jans-cedarling/bindings/cedarling_go/internal"
)

// Struct representing a cedarling instance
type Cedarling struct {
	instance_id uint
}

func cedarlingFinalizer(cedarling *Cedarling) {
	internal.DropInstance(cedarling.instance_id)
}

// Creates cedarling instance given bootstrap configuration
func NewCedarling(bootstrap_config map[string]any) (*Cedarling, error) {
	instance_id, err := internal.NewInstance(bootstrap_config)
	if err != nil {
		return nil, err
	}
	instance := &Cedarling{instance_id: instance_id}
	runtime.SetFinalizer(instance, cedarlingFinalizer)

	return instance, nil
}

// Creates cedarling instance from environment variables. If bootstrap configuration is provided,
// they will override the corresponding environment variables.
func NewCedarlingWithEnv(bootstrap_config *map[string]any) (*Cedarling, error) {
	instance_id, err := internal.NewInstanceWithEnv(bootstrap_config)
	if err != nil {
		return nil, err
	}
	instance := &Cedarling{instance_id: instance_id}
	runtime.SetFinalizer(instance, cedarlingFinalizer)
	return instance, nil
}

// Executes an authorization request.
func (c *Cedarling) Authorize(request Request) (AuthorizeResult, error) {
	request_json, err := json.Marshal(request)
	if err != nil {
		return AuthorizeResult{}, err
	}

	result := internal.CallAuthorize(c.instance_id, string(request_json))
	err = result.Error()
	if err != nil {
		return AuthorizeResult{}, err
	}

	var authorize_result AuthorizeResult
	err = json.Unmarshal([]byte(result.JsonValue()), &authorize_result)
	if err != nil {
		return AuthorizeResult{}, err
	}

	return authorize_result, nil
}

// Executes an unsigned authorization request (raw data for principle)
func (c *Cedarling) AuthorizeUnsigned(request RequestUnsigned) (AuthorizeResult, error) {
	request_json, err := json.Marshal(request)
	if err != nil {
		return AuthorizeResult{}, err
	}
	result := internal.CallAuthorizeUnsigned(c.instance_id, string(request_json))
	err = result.Error()
	if err != nil {
		return AuthorizeResult{}, err
	}
	var authorize_result AuthorizeResult
	err = json.Unmarshal([]byte(result.JsonValue()), &authorize_result)
	if err != nil {
		return AuthorizeResult{}, err
	}
	return authorize_result, nil
}

func (c *Cedarling) AuthorizeMultiIssuer(request AuthorizeMultiIssuerRequest) (MultiIssuerAuthorizeResult, error) {
	request_json, err := json.Marshal(request)
	if err != nil {
		return MultiIssuerAuthorizeResult{}, err
	}
	result := internal.CallAuthorizeMultiIssuer(c.instance_id, string(request_json))
	err = result.Error()
	if err != nil {
		return MultiIssuerAuthorizeResult{}, err
	}
	var authorize_result MultiIssuerAuthorizeResult
	err = json.Unmarshal([]byte(result.JsonValue()), &authorize_result)
	if err != nil {
		return MultiIssuerAuthorizeResult{}, err
	}
	return authorize_result, nil
}

func (c *Cedarling) PopLogs() []string {
	return internal.CallPopLogs(c.instance_id)
}

// Retrieves specific log by ID
func (c *Cedarling) GetLogById(id string) string {
	return internal.CallGetLogById(c.instance_id, id)
}

// Returns an array of log IDs
func (c *Cedarling) GetLogIds() []string {
	return internal.CallGetLogIds(c.instance_id)
}

// Retrieves specific log by tag i.e. "info"
func (c *Cedarling) GetLogsByTag(tag string) []string {
	return internal.CallGetLogsByTag(c.instance_id, tag)
}

// Retrieves specific log by request ID obtained from authorization request
func (c *Cedarling) GetLogsByRequestId(request_id string) []string {
	return internal.CallGetLogsByRequestId(c.instance_id, request_id)
}

// Retrieves specific log by request ID and tag
func (c *Cedarling) GetLogsByRequestIdAndTag(request_id string, tag string) []string {
	return internal.CallGetLogsByRequestIdAndTag(c.instance_id, request_id, tag)
}

// Closes the cedarling instance
func (c *Cedarling) ShutDown() {
	internal.CallShutDown(c.instance_id)
}

// PushDataCtx stores a value in the data store with an optional TTL.
// If ttl is nil or zero, the default TTL from configuration is used.
// The value can be any JSON-serializable Go value.
func (c *Cedarling) PushDataCtx(key string, value any, ttl *time.Duration) error {
	value_json, err := json.Marshal(value)
	if err != nil {
		return err
	}

	var ttl_nanos int64 = 0
	if ttl != nil && *ttl > 0 {
		// Use nanoseconds to preserve full sub-second precision
		ttl_nanos = ttl.Nanoseconds()
	}

	result := internal.CallPushDataCtx(c.instance_id, key, string(value_json), ttl_nanos)
	return result.Error()
}

// GetDataCtx retrieves a value from the data store by key.
// Returns nil if the key doesn't exist or the entry has expired.
func (c *Cedarling) GetDataCtx(key string) (any, error) {
	result := internal.CallGetDataCtx(c.instance_id, key)
	err := result.Error()
	if err != nil {
		return nil, err
	}

	json_value := result.JsonValue()
	if json_value == "" || json_value == "null" {
		return nil, nil
	}

	var value any
	err = json.Unmarshal([]byte(json_value), &value)
	if err != nil {
		return nil, err
	}

	return value, nil
}

// GetDataEntryCtx retrieves a data entry with full metadata by key.
// Returns nil if the key doesn't exist or the entry has expired.
func (c *Cedarling) GetDataEntryCtx(key string) (*DataEntry, error) {
	result := internal.CallGetDataEntryCtx(c.instance_id, key)
	err := result.Error()
	if err != nil {
		return nil, err
	}

	json_value := result.JsonValue()
	if json_value == "" || json_value == "null" {
		return nil, nil
	}

	var entry DataEntry
	err = json.Unmarshal([]byte(json_value), &entry)
	if err != nil {
		return nil, err
	}

	return &entry, nil
}

// RemoveDataCtx removes a value from the data store by key.
// Returns true if the key existed and was removed, false otherwise.
func (c *Cedarling) RemoveDataCtx(key string) (bool, error) {
	result := internal.CallRemoveDataCtx(c.instance_id, key)
	err := result.Error()
	if err != nil {
		return false, err
	}

	var removed bool
	err = json.Unmarshal([]byte(result.JsonValue()), &removed)
	if err != nil {
		return false, err
	}

	return removed, nil
}

// ClearDataCtx removes all entries from the data store.
func (c *Cedarling) ClearDataCtx() error {
	result := internal.CallClearDataCtx(c.instance_id)
	return result.Error()
}

// ListDataCtx returns all entries in the data store with their metadata.
func (c *Cedarling) ListDataCtx() ([]DataEntry, error) {
	result := internal.CallListDataCtx(c.instance_id)
	err := result.Error()
	if err != nil {
		return nil, err
	}

	var entries []DataEntry
	err = json.Unmarshal([]byte(result.JsonValue()), &entries)
	if err != nil {
		return nil, err
	}

	return entries, nil
}

// GetStatsCtx returns statistics about the data store.
func (c *Cedarling) GetStatsCtx() (*DataStoreStats, error) {
	result := internal.CallGetStatsCtx(c.instance_id)
	err := result.Error()
	if err != nil {
		return nil, err
	}

	var stats DataStoreStats
	err = json.Unmarshal([]byte(result.JsonValue()), &stats)
	if err != nil {
		return nil, err
	}

	return &stats, nil
}
