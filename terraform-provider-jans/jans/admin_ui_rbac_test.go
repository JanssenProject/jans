package jans

import (
	"context"
	"errors"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestAdminUIRoles(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetAdminUIRoles(ctx)
	if err != nil {
		t.Error(err)
	}
	newRole := AdminUIRole{
		Role:        "test",
		Description: "test-description",
		Deletable:   true,
	}

	if err := client.CreateAdminUIRole(ctx, &newRole); err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteAdminUIRole(ctx, newRole.Role)
	})

	loadedRole, err := client.GetAdminUIRole(ctx, newRole.Role)
	if err != nil {
		t.Fatal(err)
	}

	loadedRole.Description = "test-description-updated"
	if err = client.UpdateAdminUIRole(ctx, loadedRole); err != nil {
		t.Fatal(err)
	}

	updatedRole, err := client.GetAdminUIRole(ctx, loadedRole.Role)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(loadedRole, updatedRole); diff != "" {
		t.Errorf("Got different role after updating: %v", diff)
	}

	err = client.DeleteAdminUIRole(ctx, newRole.Role)
	if err != nil {
		t.Error(err)
	}

	_, err = client.GetAdminUIRole(ctx, "test")
	if !errors.Is(err, ErrorNotFound) {
		t.Errorf("expected 404 error, got %v", err)
	}

}

func TestAdminUIPermissions(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetAdminUIPermissions(ctx)
	if err != nil {
		t.Error(err)
	}

	newPermission := AdminUIPermission{
		Permission:  "test-permission",
		Description: "test-description",
	}

	if err := client.CreateAdminUIPermission(ctx, &newPermission); err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteAdminUIPermission(ctx, newPermission.Permission)
	})

	loadedPermission, err := client.GetAdminUIPermission(ctx, newPermission.Permission)
	if err != nil {
		t.Fatal(err)
	}

	loadedPermission.Description = "test-description-updated"
	if err = client.UpdateAdminUIPermission(ctx, loadedPermission); err != nil {
		t.Fatal(err)
	}

	updatedPermission, err := client.GetAdminUIPermission(ctx, loadedPermission.Permission)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(loadedPermission, updatedPermission); diff != "" {
		t.Errorf("Got different permission after updating: %v", diff)
	}

	if err = client.DeleteAdminUIPermission(ctx, newPermission.Permission); err != nil {
		t.Error(err)
	}

	_, err = client.GetAdminUIPermission(ctx, newPermission.Permission)
	if !errors.Is(err, ErrorNotFound) {
		t.Errorf("expected 404 error, got %v", err)
	}

}

func TestAdminUIRolePermissions(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetAdminUIRolePermissionMappings(ctx)
	if err != nil {
		t.Error(err)
	}

	role := AdminUIRole{
		Role:        "test-role2",
		Description: "test-description",
		Deletable:   true,
	}

	if err := client.CreateAdminUIRole(ctx, &role); err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteAdminUIRole(ctx, role.Role)
	})

	permission := AdminUIPermission{
		Permission:  "test-permission2",
		Description: "test-description",
	}

	if err := client.CreateAdminUIPermission(ctx, &permission); err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteAdminUIPermission(ctx, permission.Permission)
	})

	rolePermissionMapping := AdminUIRolePermissionMapping{
		Role: role.Role,
		Permissions: []string{
			permission.Permission,
		},
	}

	if err := client.CreateAdminUIRolePermissionMapping(ctx, &rolePermissionMapping); err != nil {
		t.Error(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteAdminUIRolePermissionMapping(ctx, role.Role)
	})

	if err := client.DeleteAdminUIRolePermissionMapping(ctx, role.Role); err != nil {
		t.Error(err)
	}

	_, err = client.GetAdminUIRolePermissionMapping(ctx, role.Role)
	if !errors.Is(err, ErrorNotFound) {
		t.Errorf("expected 404 error, got %v", err)
	}
}
