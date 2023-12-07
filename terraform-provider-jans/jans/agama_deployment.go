package jans

import (
	"context"
	"fmt"
)

type ProjectMetadata struct {
	ProjectName string `json:"projectName" schema:"project_name"`
	Author      string `json:"author" schema:"author"`
	Type        string `json:"type" schema:"type"`
	Description string `json:"description" schema:"description"`
	// Configs     []string `json:"configs" schema:"configs"`
	NoDirectLaunch []string `json:"noDirectLaunch" schema:"no_direct_launch"`
}

type DeploymentDetails struct {
	Folders       []string `json:"folders" schema:"folders"`
	Libs          []string `json:"libs" schema:"libs"`
	Autoconfigure bool     `json:"autoconfigure" schema:"autoconfigure"`
	// FlowsError []string `json:"flowsError" schema:"flows_error"`
	Error           string          `json:"error" schema:"error"`
	ProjectMetadata ProjectMetadata `json:"projectMetadata" schema:"project_metadata"`
}

type AgamaDeployment struct {
	Name       string            `json:"name" schema:"name"`
	Dn         string            `json:"dn" schema:"dn"`
	ID         string            `json:"id" schema:"id"`
	CreatedAt  string            `json:"createdAt" schema:"created_at"`
	TaskActive bool              `json:"taskActive" schema:"task_active"`
	FinishedAt string            `json:"finishedAt" schema:"finished_at"`
	Assets     string            `json:"assets" schema:"assets"`
	Details    DeploymentDetails `json:"details" schema:"details"`
	BaseDn     string            `json:"baseDn" schema:"base_dn"`
}

// GetAgamaDeployments returns all currently configured Agama deployments.
func (c *Client) GetAgamaDeployments(ctx context.Context) ([]AgamaDeployment, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/agama.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type response struct {
		Entries    []AgamaDeployment `json:"entries"`
		Count      int               `json:"entriesCount"`
		TotalItems int               `json:"totalItems"`
	}
	ret := response{}

	if err := c.get(ctx, "/jans-config-api/api/v1/agama-deployment", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret.Entries, nil
}

// GetAgamaDeployment returns the Agama deployment with the given qname.
func (c *Client) GetAgamaDeployment(ctx context.Context, qname string) (*AgamaDeployment, error) {

	if qname == "" {
		return nil, fmt.Errorf("qname is empty")
	}

	// workaround for GET returning empty response

	deployments, err := c.GetAgamaDeployments(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to get deployments: %w", err)
	}

	for _, d := range deployments {
		if d.Details.ProjectMetadata.ProjectName == qname {
			d.Name = qname
			return &d, nil
		}
	}

	return nil, fmt.Errorf("agama deployment not found: %w", ErrorNotFound)

	// // fallback to GET
	// token, err := c.getToken(ctx, "https://jans.io/oauth/config/agama.readonly")
	// if err != nil {
	// 	return nil, fmt.Errorf("failed to get token: %w", err)
	// }

	// ret := &AgamaDeployment{}

	// if err := c.get(ctx, "/jans-config-api/api/v1/agama-deployment/"+qname, token, ret); err != nil {
	// 	return nil, fmt.Errorf("get request failed: %w", err)
	// }

	// ret.Name = qname

	// return ret, nil
}

// CreateAgamaDeployment creates a new Agama flow.
func (c *Client) CreateAgamaDeployment(ctx context.Context, name string, autoconfig bool, data []byte) error {

	if name == "" {
		return fmt.Errorf("agama project name may not be empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/agama.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	url := "/jans-config-api/api/v1/agama-deployment/" + name
	if autoconfig {
		url += "?autoconfigure=true"
	}

	if err := c.postZipFile(ctx, url, token, data, nil); err != nil {
		return fmt.Errorf("post request failed: %w", err)
	}

	return nil
}

// // UpdateAgamaDeployment updates an already existing agama deployment.
// func (c *Client) UpdateAgamaDeployment(ctx context.Context, name string, data []byte) error {

// 	if name == "" {
// 		return fmt.Errorf("agama project name may not be empty")
// 	}

// 	token, err := c.getToken(ctx, "https://jans.io/oauth/config/agama.write")
// 	if err != nil {
// 		return fmt.Errorf("failed to get token: %w", err)
// 	}

// 	// first update the flow attributes
// 	if err := c.patch(ctx, "/jans-config-api/api/v1/agama/"+flow.Name, token, patches); err != nil {
// 		return fmt.Errorf("patch request failed: %w", err)
// 	}

// 	return nil
// }

// DeleteAgamaFlow deletes an already existing Agama flow.
func (c *Client) DeleteAgamaDeployment(ctx context.Context, qname string) error {

	if qname == "" {
		return fmt.Errorf("qname is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/agama.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/agama-deployment/"+qname, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
