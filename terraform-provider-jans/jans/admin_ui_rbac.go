package jans

import (
	"context"
	"fmt"
)

type AdminUIRole struct {
	Role        string `schema:"role" json:"role,omitempty"`
	Description string `schema:"description" json:"description,omitempty"`
	Deletable   bool   `schema:"deletable" json:"deletable,omitempty"`
}

type AdminUIPermission struct {
	Permission               string `schema:"permission" json:"permission,omitempty"`
	Description              string `schema:"description" json:"description,omitempty"`
	DefaultPermissionInToken bool   `schema:"default_permission_in_token" json:"defaultPermissionInToken,omitempty"`
}

type AdminUIRolePermissionMapping struct {
	Role        string   `schema:"role" json:"role,omitempty"`
	Permissions []string `schema:"permissions" json:"permissions,omitempty"`
}

// Roles

// GetAdminUIRoles returns all AdminUI user roles currently configured in the server.
func (c *Client) GetAdminUIRoles(ctx context.Context) ([]AdminUIRole, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := []AdminUIRole{}

	if err := c.get(ctx, "/jans-config-api/admin-ui/adminUIRoles", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// GetAdminUIRole returns an AdminUI user role by its name. Since there is no
// dedicated endpoint for this, we have to iterate over all roles.
func (c *Client) GetAdminUIRole(ctx context.Context, roleID string) (*AdminUIRole, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := AdminUIRole{}

	if err := c.get(ctx, "/jans-config-api/admin-ui/adminUIRoles/"+roleID, token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return &ret, nil
}

// CreateAdminUIRole creates a new AdminUI user role.
func (c *Client) CreateAdminUIRole(ctx context.Context, role *AdminUIRole) error {

	if role == nil {
		return fmt.Errorf("role is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.post(ctx, "/jans-config-api/admin-ui/adminUIRoles", token, role, nil); err != nil {
		return fmt.Errorf("post request failed: %w", err)
	}

	return nil
}

// UpdateAdminUIRole updates an already existing AdminUI user role.
func (c *Client) UpdateAdminUIRole(ctx context.Context, role *AdminUIRole) error {

	if role == nil {
		return fmt.Errorf("role is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.put(ctx, "/jans-config-api/admin-ui/adminUIRoles", token, role, nil); err != nil {
		return fmt.Errorf("put request failed: %w", err)
	}

	return nil
}

// DeleteAdminUIRole deletes an already existing AdminUI user role.
func (c *Client) DeleteAdminUIRole(ctx context.Context, roleID string) error {

	if roleID == "" {
		return fmt.Errorf("role is nil")
	}

	role := AdminUIRole{
		Role: roleID,
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/role.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.deleteEntity(ctx, "/jans-config-api/admin-ui/adminUIRoles/"+roleID, token, role); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}

// Permissions

// GetAdminUIPermissions returns all AdminUI user permissions currently configured in the server.
func (c *Client) GetAdminUIPermissions(ctx context.Context) ([]AdminUIPermission, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := []AdminUIPermission{}

	if err := c.get(ctx, "/jans-config-api/admin-ui/adminUIPermissions", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// GetAdminUIPermission returns an AdminUI user permission by its name. Since there is no dedicated
// endpoint for this, we have to iterate over all permissions.
func (c *Client) GetAdminUIPermission(ctx context.Context, permissionID string) (*AdminUIPermission, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := AdminUIPermission{}

	if err := c.get(ctx, "/jans-config-api/admin-ui/adminUIPermissions/"+permissionID, token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return &ret, nil
}

// CreateAdminRole creates a new AdminUI user permission.
func (c *Client) CreateAdminUIPermission(ctx context.Context, permission *AdminUIPermission) error {

	if permission == nil {
		return fmt.Errorf("permission is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.post(ctx, "/jans-config-api/admin-ui/adminUIPermissions", token, permission, nil); err != nil {
		return fmt.Errorf("post request failed: %w", err)
	}

	return nil
}

// UpdateAdminUIPermission updates an already existing AdminUI user permission.
func (c *Client) UpdateAdminUIPermission(ctx context.Context, permission *AdminUIPermission) error {

	if permission == nil {
		return fmt.Errorf("permission is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.put(ctx, "/jans-config-api/admin-ui/adminUIPermissions", token, permission, nil); err != nil {
		return fmt.Errorf("put request failed: %w", err)
	}

	return nil
}

// DeleteAdminUIPermission deletes an already existing AdminUI user permission.
func (c *Client) DeleteAdminUIPermission(ctx context.Context, permissionID string) error {

	if permissionID == "" {
		return fmt.Errorf("permission is empty")
	}

	permission := AdminUIPermission{
		Permission: permissionID,
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/permission.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.deleteEntity(ctx, "/jans-config-api/admin-ui/adminUIPermissions/"+permissionID, token, permission); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}

// Role Permissions Mappings

// GetAdminUIRolePermissionMappings returns all AdminUI user role permission
// mappings currently configured in the server.
func (c *Client) GetAdminUIRolePermissionMappings(ctx context.Context) ([]AdminUIRolePermissionMapping, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := []AdminUIRolePermissionMapping{}

	if err := c.get(ctx, "/jans-config-api/admin-ui/adminUIRolePermissionsMapping", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// GetAdminUIRolePermissionMapping returns an AdminUI user role permission mapping
// by its name. Since there is no dedicated endpoint for this, we have to iterate
// over all existing mappings and find the one with the same role.
func (c *Client) GetAdminUIRolePermissionMapping(ctx context.Context, roleID string) (*AdminUIRolePermissionMapping, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := AdminUIRolePermissionMapping{}

	if err := c.get(ctx, "/jans-config-api/admin-ui/adminUIRolePermissionsMapping/"+roleID, token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return &ret, nil
}

// CreateAdminUIRolePermissionMapping creates a new AdminUI user role permission mapping.
func (c *Client) CreateAdminUIRolePermissionMapping(ctx context.Context, rolePermissionMapping *AdminUIRolePermissionMapping) error {

	if rolePermissionMapping == nil {
		return fmt.Errorf("rolePermissionMapping is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.post(ctx, "/jans-config-api/admin-ui/adminUIRolePermissionsMapping", token, rolePermissionMapping, nil); err != nil {
		return fmt.Errorf("post request failed: %w", err)
	}

	return nil
}

// UpdateAdminUIRolePermissionMapping updates an already existing AdminUI user
// role permission mapping.
func (c *Client) UpdateAdminUIRolePermissionMapping(ctx context.Context, rolePermissionMapping *AdminUIRolePermissionMapping) error {

	if rolePermissionMapping == nil {
		return fmt.Errorf("rolePermissionMapping is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	ret := &AdminUIRolePermissionMapping{}

	if err := c.put(ctx, "/jans-config-api/admin-ui/adminUIRolePermissionsMapping", token, rolePermissionMapping, ret); err != nil {
		return fmt.Errorf("put request failed: %w", err)
	}

	return nil
}

// DeleteAdminUIRolePermissionMapping deletes an already existing AdminUI user
// role permission mapping.
func (c *Client) DeleteAdminUIRolePermissionMapping(ctx context.Context, roleID string) error {

	if roleID == "" {
		return fmt.Errorf("role is empty")
	}

	rolePermissionMapping, err := c.GetAdminUIRolePermissionMapping(ctx, roleID)
	if err != nil {
		return err
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/adminui/user/rolePermissionMapping.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.deleteEntity(ctx, "/jans-config-api/admin-ui/adminUIRolePermissionsMapping/"+roleID, token, rolePermissionMapping); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
