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

// Returns an array of logs and clears the buffer
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
