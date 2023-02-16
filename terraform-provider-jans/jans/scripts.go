package jans

import (
	"context"
	"fmt"
)

// ScriptError defines possible errors that are assosiated with a script.
type ScriptError struct {
	RaisedAt   string `schema:"raised_at" json:"raisedAt,omitempty"`
	StackTrace string `schema:"stack_trace" json:"stackTrace,omitempty"`
}

// SimpleExtendedCustomProperty is a custom property that can be used as
// configuration for a script.
type SimpleExtendedCustomProperty struct {
	Value1      string `schema:"value1" json:"value1,omitempty"`
	Value2      string `schema:"value2" json:"value2,omitempty"`
	Description string `schema:"description" json:"description,omitempty"`
	Hide        bool   `schema:"hide" json:"hide,omitempty"`
}

// SimpleCustomProperty is a custom property that can be used as
// modelu configuration for a script.
type SimpleCustomProperty struct {
	Value1      string `schema:"value1" json:"value1,omitempty"`
	Value2      string `schema:"value2" json:"value2,omitempty"`
	Description string `schema:"description" json:"description,omitempty"`
}

// Script represents a script that can be used to extend the default
// functionality of the Janssen server.
type Script struct {
	Dn                      string                         `schema:"dn" json:"dn,omitempty"`
	Inum                    string                         `schema:"inum" json:"inum,omitempty"`
	Name                    string                         `schema:"name" json:"name,omitempty"`
	Aliases                 []string                       `schema:"aliases" json:"aliases,omitempty"`
	Description             string                         `schema:"description" json:"description,omitempty"`
	Script                  string                         `schema:"script" json:"script,omitempty"`
	ScriptType              string                         `schema:"script_type" json:"scriptType,omitempty"`
	ProgrammingLanguage     string                         `schema:"programming_language" json:"programmingLanguage,omitempty"`
	ModuleProperties        []SimpleCustomProperty         `schema:"module_properties" json:"moduleProperties,omitempty"`
	ConfigurationProperties []SimpleExtendedCustomProperty `schema:"configuration_properties" json:"configurationProperties,omitempty"`
	Level                   int                            `schema:"level" json:"level,omitempty"`
	Revision                int                            `schema:"revision" json:"revision,omitempty"`
	Enabled                 bool                           `schema:"enabled" json:"enabled,omitempty"`
	ScriptError             ScriptError                    `schema:"script_error" json:"scriptError,omitempty"`
	Modified                bool                           `schema:"modified" json:"modified,omitempty"`
	Internal                bool                           `schema:"internal" json:"internal,omitempty"`
	LocationType            string                         `schema:"location_type" json:"locationType,omitempty"`
	BaseDN                  string                         `schema:"base_dn" json:"baseDN,omitempty"`
}

// GetScripts returns all currently configured scripts.
func (c *Client) GetScripts(ctx context.Context) ([]Script, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/scripts.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type response struct {
		Scripts []Script `json:"entries"`
	}

	ret := response{}

	if err := c.get(ctx, "/jans-config-api/api/v1/config/scripts", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret.Scripts, nil
}

// GetScript returns a script by its inum.
func (c *Client) GetScript(ctx context.Context, inum string) (*Script, error) {

	if inum == "" {
		return nil, fmt.Errorf("inum is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/scripts.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Script{}

	if err := c.get(ctx, "/jans-config-api/api/v1/config/scripts/inum/"+inum, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateScript creates a new script
func (c *Client) CreateScript(ctx context.Context, script *Script) (*Script, error) {

	if script == nil {
		return nil, fmt.Errorf("script is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/scripts.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Script{}

	if err := c.post(ctx, "/jans-config-api/api/v1/config/scripts", token, script, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// UpdateScript updates an already existing script
func (c *Client) UpdateScript(ctx context.Context, script *Script) (*Script, error) {

	if script == nil {
		return nil, fmt.Errorf("script is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/scripts.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	// we can either use PUT on /scripts or PATCH on /scripts/{inum}

	ret := &Script{}

	if err := c.put(ctx, "/jans-config-api/api/v1/config/scripts/", token, script, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteScript deletes an already existing script
func (c *Client) DeleteScript(ctx context.Context, inum string) error {

	if inum == "" {
		return fmt.Errorf("inum is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/scripts.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/config/scripts/"+inum, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
