package jans

import (
        "context"
        "encoding/json"
        "errors"
        "net/http"
        "net/http/httptest"
        "testing"

        "github.com/google/go-cmp/cmp"
)

func TestAdminUIRoles(t *testing.T) {

        if skipKnownFailures {
                t.SkipNow()
        }

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

        if skipKnownFailures {
                t.SkipNow()
        }

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

        if skipKnownFailures {
                t.SkipNow()
        }

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
                t.Fatal(err)
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

// Unit tests for Admin UI RBAC functions

func TestClient_GetAdminUIRoles(t *testing.T) {
        tests := []struct {
                name         string
                responseBody []AdminUIRole
                expectedErr  bool
        }{
                {
                        name: "successful roles retrieval",
                        responseBody: []AdminUIRole{
                                {Role: "admin", Description: "Admin role", Deletable: false},
                                {Role: "user", Description: "User role", Deletable: true},
                        },
                        expectedErr: false,
                },
                {
                        name:         "empty roles list",
                        responseBody: []AdminUIRole{},
                        expectedErr:  false,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                if r.URL.Path != "/jans-config-api/admin-ui/adminUIRoles" {
                                        t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIRoles', got %s", r.URL.Path)
                                }
                                if r.Method != http.MethodGet {
                                        t.Errorf("Expected GET method, got %s", r.Method)
                                }
                                w.Header().Set("Content-Type", "application/json")
                                json.NewEncoder(w).Encode(tt.responseBody)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        result, err := client.GetAdminUIRoles(context.Background())

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }
                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                        if !tt.expectedErr && len(result) != len(tt.responseBody) {
                                t.Errorf("Expected %d roles, got %d", len(tt.responseBody), len(result))
                        }
                })
        }

        // Negative path: server error
        t.Run("server error (500)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        if r.URL.Path != "/jans-config-api/admin-ui/adminUIRoles" {
                                t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIRoles', got %s", r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusInternalServerError)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                _, err = client.GetAdminUIRoles(context.Background())

                if err == nil {
                        t.Error("Expected error for 500 status, got nil")
                }
        })

        // Negative path: invalid JSON
        t.Run("invalid JSON response", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        if r.URL.Path != "/jans-config-api/admin-ui/adminUIRoles" {
                                t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIRoles', got %s", r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        w.Header().Set("Content-Type", "application/json")
                        w.Write([]byte("{invalid json}"))
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                _, err = client.GetAdminUIRoles(context.Background())

                if err == nil {
                        t.Error("Expected decoding error for invalid JSON, got nil")
                }
        })
}

func TestClient_GetAdminUIRole(t *testing.T) {
        tests := []struct {
                name         string
                roleID       string
                responseBody AdminUIRole
                expectedErr  bool
        }{
                {
                        name:   "successful role retrieval",
                        roleID: "admin",
                        responseBody: AdminUIRole{
                                Role:        "admin",
                                Description: "Admin role",
                                Deletable:   false,
                        },
                        expectedErr: false,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                expectedPath := "/jans-config-api/admin-ui/adminUIRoles/" + tt.roleID
                                if r.URL.Path != expectedPath {
                                        t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                                }
                                if r.Method != http.MethodGet {
                                        t.Errorf("Expected GET method, got %s", r.Method)
                                }
                                w.Header().Set("Content-Type", "application/json")
                                json.NewEncoder(w).Encode(tt.responseBody)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        result, err := client.GetAdminUIRole(context.Background(), tt.roleID)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }
                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                        if !tt.expectedErr && result.Role != tt.responseBody.Role {
                                t.Errorf("Expected role %s, got %s", tt.responseBody.Role, result.Role)
                        }
                })
        }

        // Negative path: server error
        t.Run("server error (500)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIRoles/test-role"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusInternalServerError)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                _, err = client.GetAdminUIRole(context.Background(), "test-role")

                if err == nil {
                        t.Error("Expected error for 500 status, got nil")
                }
        })

        // Negative path: invalid JSON
        t.Run("invalid JSON response", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIRoles/test-role"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        w.Header().Set("Content-Type", "application/json")
                        w.Write([]byte("{invalid json}"))
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                _, err = client.GetAdminUIRole(context.Background(), "test-role")

                if err == nil {
                        t.Error("Expected decoding error for invalid JSON, got nil")
                }
        })

        // Negative path: not found (404)
        t.Run("role not found (404)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIRoles/nonexistent"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusNotFound)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                _, err = client.GetAdminUIRole(context.Background(), "nonexistent")

                if !errors.Is(err, ErrorNotFound) {
                        t.Errorf("Expected ErrorNotFound, got %v", err)
                }
        })
}


func TestClient_CreateAdminUIRole(t *testing.T) {
        tests := []struct {
                name        string
                role        *AdminUIRole
                expectedErr bool
        }{
                {
                        name: "successful role creation",
                        role: &AdminUIRole{
                                Role:        "test-role",
                                Description: "Test role",
                                Deletable:   true,
                        },
                        expectedErr: false,
                },
                {
                        name:        "nil role",
                        role:        nil,
                        expectedErr: true,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                if r.URL.Path != "/jans-config-api/admin-ui/adminUIRoles" {
                                        t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIRoles', got %s", r.URL.Path)
                                }
                                if r.Method != http.MethodPost {
                                        t.Errorf("Expected POST method, got %s", r.Method)
                                }
                                w.WriteHeader(http.StatusCreated)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        err = client.CreateAdminUIRole(context.Background(), tt.role)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }
                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }
}

func TestClient_UpdateAdminUIRole(t *testing.T) {
        tests := []struct {
                name        string
                role        *AdminUIRole
                expectedErr bool
        }{
                {
                        name: "successful role update",
                        role: &AdminUIRole{
                                Role:        "test-role",
                                Description: "Updated description",
                                Deletable:   true,
                        },
                        expectedErr: false,
                },
                {
                        name:        "nil role",
                        role:        nil,
                        expectedErr: true,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                if r.URL.Path != "/jans-config-api/admin-ui/adminUIRoles" {
                                        t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIRoles', got %s", r.URL.Path)
                                }
                                if r.Method != http.MethodPut {
                                        t.Errorf("Expected PUT method, got %s", r.Method)
                                }
                                w.WriteHeader(http.StatusOK)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        err = client.UpdateAdminUIRole(context.Background(), tt.role)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }
                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }
}

func TestClient_DeleteAdminUIRole(t *testing.T) {
        tests := []struct {
                name        string
                roleID      string
                expectedErr bool
        }{
                {
                        name:        "successful role deletion",
                        roleID:      "test-role",
                        expectedErr: false,
                },
                {
                        name:        "empty role ID",
                        roleID:      "",
                        expectedErr: true,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                expectedPath := "/jans-config-api/admin-ui/adminUIRoles/" + tt.roleID
                                if r.URL.Path != expectedPath {
                                        t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                                }
                                if r.Method != http.MethodDelete {
                                        t.Errorf("Expected DELETE method, got %s", r.Method)
                                }
                                w.WriteHeader(http.StatusNoContent)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        err = client.DeleteAdminUIRole(context.Background(), tt.roleID)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }
                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }

        // Negative path: not found (404)
        t.Run("role not found (404)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIRoles/nonexistent"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method != http.MethodDelete {
                                t.Errorf("Expected DELETE method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusNotFound)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                err = client.DeleteAdminUIRole(context.Background(), "nonexistent")

                if !errors.Is(err, ErrorNotFound) {
                        t.Errorf("Expected ErrorNotFound, got %v", err)
                }
        })
}

// Permission tests

func TestClient_GetAdminUIPermissions(t *testing.T) {
        tests := []struct {
                name         string
                responseBody []AdminUIPermission
                expectedErr  bool
        }{
                {
                        name: "successful permissions retrieval",
                        responseBody: []AdminUIPermission{
                                {Permission: "read", Description: "Read permission"},
                                {Permission: "write", Description: "Write permission"},
                        },
                        expectedErr: false,
                },
                {
                        name:         "empty permissions list",
                        responseBody: []AdminUIPermission{},
                        expectedErr:  false,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                if r.URL.Path != "/jans-config-api/admin-ui/adminUIPermissions" {
                                        t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIPermissions', got %s", r.URL.Path)
                                }
                                if r.Method != http.MethodGet {
                                        t.Errorf("Expected GET method, got %s", r.Method)
                                }
                                w.Header().Set("Content-Type", "application/json")
                                json.NewEncoder(w).Encode(tt.responseBody)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        result, err := client.GetAdminUIPermissions(context.Background())

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }
                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                        if !tt.expectedErr && len(result) != len(tt.responseBody) {
                                t.Errorf("Expected %d permissions, got %d", len(tt.responseBody), len(result))
                        }
                })
        }

        // Negative path: server error
        t.Run("server error (500)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        if r.URL.Path != "/jans-config-api/admin-ui/adminUIPermissions" {
                                t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIPermissions', got %s", r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusInternalServerError)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                _, err = client.GetAdminUIPermissions(context.Background())

                if err == nil {
                        t.Error("Expected error for 500 status, got nil")
                }
        })

        // Negative path: invalid JSON
        t.Run("invalid JSON response", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        if r.URL.Path != "/jans-config-api/admin-ui/adminUIPermissions" {
                                t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIPermissions', got %s", r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        w.Header().Set("Content-Type", "application/json")
                        w.Write([]byte("{invalid json}"))
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                _, err = client.GetAdminUIPermissions(context.Background())

                if err == nil {
                        t.Error("Expected decoding error for invalid JSON, got nil")
                }
        })
}

func TestClient_GetAdminUIPermission(t *testing.T) {
        tests := []struct {
                name         string
                permissionID string
                responseBody AdminUIPermission
                expectedErr  bool
        }{
                {
                        name:         "successful permission retrieval",
                        permissionID: "read",
                        responseBody: AdminUIPermission{
                                Permission:  "read",
                                Description: "Read permission",
                        },
                        expectedErr: false,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                expectedPath := "/jans-config-api/admin-ui/adminUIPermissions/" + tt.permissionID
                                if r.URL.Path != expectedPath {
                                        t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                                }
                                if r.Method != http.MethodGet {
                                        t.Errorf("Expected GET method, got %s", r.Method)
                                }
                                w.Header().Set("Content-Type", "application/json")
                                json.NewEncoder(w).Encode(tt.responseBody)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        result, err := client.GetAdminUIPermission(context.Background(), tt.permissionID)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }
                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                        if !tt.expectedErr && result.Permission != tt.responseBody.Permission {
                                t.Errorf("Expected permission %s, got %s", tt.responseBody.Permission, result.Permission)
                        }
                })
        }

        // Negative path: server error
        t.Run("server error (500)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIPermissions/test-permission"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusInternalServerError)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                _, err = client.GetAdminUIPermission(context.Background(), "test-permission")

                if err == nil {
                        t.Error("Expected error for 500 status, got nil")
                }
        })

        // Negative path: invalid JSON
        t.Run("invalid JSON response", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIPermissions/test-permission"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        w.Header().Set("Content-Type", "application/json")
                        w.Write([]byte("{invalid json}"))
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                _, err = client.GetAdminUIPermission(context.Background(), "test-permission")

                if err == nil {
                        t.Error("Expected decoding error for invalid JSON, got nil")
                }
        })

        // Negative path: not found (404)
        t.Run("permission not found (404)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIPermissions/nonexistent"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusNotFound)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                _, err = client.GetAdminUIPermission(context.Background(), "nonexistent")

                if !errors.Is(err, ErrorNotFound) {
                        t.Errorf("Expected ErrorNotFound, got %v", err)
                }
        })
}

func TestClient_CreateAdminUIPermission(t *testing.T) {
        tests := []struct {
                name        string
                permission  *AdminUIPermission
                expectedErr bool
        }{
                {
                        name: "successful permission creation",
                        permission: &AdminUIPermission{
                                Permission:  "test-permission",
                                Description: "Test permission",
                        },
                        expectedErr: false,
                },
                {
                        name:        "nil permission",
                        permission:  nil,
                        expectedErr: true,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                if r.URL.Path != "/jans-config-api/admin-ui/adminUIPermissions" {
                                        t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIPermissions', got %s", r.URL.Path)
                                }
                                if r.Method != http.MethodPost {
                                        t.Errorf("Expected POST method, got %s", r.Method)
                                }
                                w.WriteHeader(http.StatusCreated)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        err = client.CreateAdminUIPermission(context.Background(), tt.permission)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }
                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }

        // Negative path: server error
        t.Run("server error (500)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        if r.URL.Path != "/jans-config-api/admin-ui/adminUIPermissions" {
                                t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIPermissions', got %s", r.URL.Path)
                        }
                        if r.Method != http.MethodPost {
                                t.Errorf("Expected POST method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusInternalServerError)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                permission := &AdminUIPermission{Permission: "test", Description: "Test"}
                err = client.CreateAdminUIPermission(context.Background(), permission)

                if err == nil {
                        t.Error("Expected error for 500 status, got nil")
                }
        })
}

func TestClient_UpdateAdminUIPermission(t *testing.T) {
        tests := []struct {
                name        string
                permission  *AdminUIPermission
                expectedErr bool
        }{
                {
                        name: "successful permission update",
                        permission: &AdminUIPermission{
                                Permission:  "test-permission",
                                Description: "Updated description",
                        },
                        expectedErr: false,
                },
                {
                        name:        "nil permission",
                        permission:  nil,
                        expectedErr: true,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                if r.URL.Path != "/jans-config-api/admin-ui/adminUIPermissions" {
                                        t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIPermissions', got %s", r.URL.Path)
                                }
                                if r.Method != http.MethodPut {
                                        t.Errorf("Expected PUT method, got %s", r.Method)
                                }
                                w.WriteHeader(http.StatusOK)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        err = client.UpdateAdminUIPermission(context.Background(), tt.permission)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }
                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }

        // Negative path: server error
        t.Run("server error (500)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        if r.URL.Path != "/jans-config-api/admin-ui/adminUIPermissions" {
                                t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIPermissions', got %s", r.URL.Path)
                        }
                        if r.Method != http.MethodPut {
                                t.Errorf("Expected PUT method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusInternalServerError)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                permission := &AdminUIPermission{Permission: "test", Description: "Updated"}
                err = client.UpdateAdminUIPermission(context.Background(), permission)

                if err == nil {
                        t.Error("Expected error for 500 status, got nil")
                }
        })
}

func TestClient_DeleteAdminUIPermission(t *testing.T) {
        tests := []struct {
                name         string
                permissionID string
                expectedErr  bool
        }{
                {
                        name:         "successful permission deletion",
                        permissionID: "test-permission",
                        expectedErr:  false,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                expectedPath := "/jans-config-api/admin-ui/adminUIPermissions/" + tt.permissionID
                                if r.URL.Path != expectedPath {
                                        t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                                }
                                if r.Method != http.MethodDelete {
                                        t.Errorf("Expected DELETE method, got %s", r.Method)
                                }
                                w.WriteHeader(http.StatusNoContent)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        err = client.DeleteAdminUIPermission(context.Background(), tt.permissionID)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }
                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }

        // Negative path: server error
        t.Run("server error (500)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIPermissions/test-permission"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method != http.MethodDelete {
                                t.Errorf("Expected DELETE method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusInternalServerError)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                err = client.DeleteAdminUIPermission(context.Background(), "test-permission")

                if err == nil {
                        t.Error("Expected error for 500 status, got nil")
                }
        })

        // Negative path: not found (404)
        t.Run("permission not found (404)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIPermissions/nonexistent"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method != http.MethodDelete {
                                t.Errorf("Expected DELETE method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusNotFound)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                err = client.DeleteAdminUIPermission(context.Background(), "nonexistent")

                if !errors.Is(err, ErrorNotFound) {
                        t.Errorf("Expected ErrorNotFound, got %v", err)
                }
        })
}

// Role-Permission Mapping tests

func TestClient_GetAdminUIRolePermissionMappings(t *testing.T) {
        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                if r.URL.Path != "/jans-config-api/admin-ui/adminUIRolePermissionsMapping" {
                        t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIRolePermissionsMapping', got %s", r.URL.Path)
                }
                if r.Method != http.MethodGet {
                        t.Errorf("Expected GET method, got %s", r.Method)
                }
                mappings := []AdminUIRolePermissionMapping{
                        {Role: "admin", Permissions: []string{"read", "write"}},
                }
                json.NewEncoder(w).Encode(mappings)
        }))
        defer server.Close()

        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
        result, err := client.GetAdminUIRolePermissionMappings(context.Background())

        if err != nil {
                t.Errorf("Unexpected error: %v", err)
        }
        if len(result) != 1 {
                t.Errorf("Expected 1 mapping, got %d", len(result))
        }
}

func TestClient_GetAdminUIRolePermissionMapping(t *testing.T) {
        t.Run("successful mapping retrieval", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIRolePermissionsMapping/admin"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        mapping := AdminUIRolePermissionMapping{Role: "admin", Permissions: []string{"read"}}
                        json.NewEncoder(w).Encode(mapping)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                result, err := client.GetAdminUIRolePermissionMapping(context.Background(), "admin")

                if err != nil {
                        t.Errorf("Unexpected error: %v", err)
                }
                if result.Role != "admin" {
                        t.Errorf("Expected role 'admin', got %s", result.Role)
                }
        })

        t.Run("mapping not found (404)", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIRolePermissionsMapping/nonexistent"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method != http.MethodGet {
                                t.Errorf("Expected GET method, got %s", r.Method)
                        }
                        w.WriteHeader(http.StatusNotFound)
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                _, err = client.GetAdminUIRolePermissionMapping(context.Background(), "nonexistent")

                if !errors.Is(err, ErrorNotFound) {
                        t.Errorf("Expected ErrorNotFound, got %v", err)
                }
        })
}

func TestClient_CreateAdminUIRolePermissionMapping(t *testing.T) {
        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                if r.URL.Path != "/jans-config-api/admin-ui/adminUIRolePermissionsMapping" {
                        t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIRolePermissionsMapping', got %s", r.URL.Path)
                }
                if r.Method != http.MethodPost {
                        t.Errorf("Expected POST method, got %s", r.Method)
                }
                w.WriteHeader(http.StatusCreated)
        }))
        defer server.Close()

        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
        mapping := &AdminUIRolePermissionMapping{Role: "test", Permissions: []string{"read"}}
        err = client.CreateAdminUIRolePermissionMapping(context.Background(), mapping)

        if err != nil {
                t.Errorf("Unexpected error: %v", err)
        }
}

func TestClient_UpdateAdminUIRolePermissionMapping(t *testing.T) {
        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                if r.URL.Path != "/jans-config-api/admin-ui/adminUIRolePermissionsMapping" {
                        t.Errorf("Expected path '/jans-config-api/admin-ui/adminUIRolePermissionsMapping', got %s", r.URL.Path)
                }
                if r.Method != http.MethodPut {
                        t.Errorf("Expected PUT method, got %s", r.Method)
                }
                w.WriteHeader(http.StatusOK)
        }))
        defer server.Close()

        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
        mapping := &AdminUIRolePermissionMapping{Role: "test", Permissions: []string{"read", "write"}}
        err = client.UpdateAdminUIRolePermissionMapping(context.Background(), mapping)

        if err != nil {
                t.Errorf("Unexpected error: %v", err)
        }
}

func TestClient_DeleteAdminUIRolePermissionMapping(t *testing.T) {
        // Happy path: GET returns mapping, DELETE succeeds
        t.Run("successful deletion with GET-before-DELETE", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIRolePermissionsMapping/test"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        // DELETE operation first does a GET to verify existence, then DELETE
                        if r.Method == http.MethodGet {
                                mapping := AdminUIRolePermissionMapping{Role: "test", Permissions: []string{"read"}}
                                json.NewEncoder(w).Encode(mapping)
                        } else if r.Method == http.MethodDelete {
                                w.WriteHeader(http.StatusNoContent)
                        } else {
                                t.Errorf("Expected GET or DELETE method, got %s", r.Method)
                        }
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                err = client.DeleteAdminUIRolePermissionMapping(context.Background(), "test")

                if err != nil {
                        t.Errorf("Unexpected error: %v", err)
                }
        })

        // Negative path: GET returns 404, DELETE should not be called
        t.Run("mapping not found (404) during GET validation", func(t *testing.T) {
                server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                        expectedPath := "/jans-config-api/admin-ui/adminUIRolePermissionsMapping/nonexistent"
                        if r.URL.Path != expectedPath {
                                t.Errorf("Expected path '%s', got %s", expectedPath, r.URL.Path)
                        }
                        if r.Method == http.MethodGet {
                                // Return 404 for the GET validation check
                                w.WriteHeader(http.StatusNotFound)
                        } else if r.Method == http.MethodDelete {
                                // DELETE should never be called if GET returned 404
                                t.Error("DELETE should not be called when GET returns 404")
                                w.WriteHeader(http.StatusNoContent)
                        } else {
                                t.Errorf("Expected GET or DELETE method, got %s", r.Method)
                        }
                }))
                defer server.Close()

                client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
		if err != nil {
			t.Fatalf("Failed to create client: %v", err)
		}
                err = client.DeleteAdminUIRolePermissionMapping(context.Background(), "nonexistent")

                if !errors.Is(err, ErrorNotFound) {
                        t.Errorf("Expected ErrorNotFound from GET validation, got %v", err)
                }
        })
}
