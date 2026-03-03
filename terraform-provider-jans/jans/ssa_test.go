package jans

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestClient_RevokeSSA(t *testing.T) {
	tests := []struct {
		name        string
		jti         string
		orgId       string
		expectedErr bool
	}{
		{
			name:        "successful SSA revocation by JTI",
			jti:         "test-jti-123",
			orgId:       "",
			expectedErr: false,
		},
		{
			name:        "successful SSA revocation by org ID",
			jti:         "",
			orgId:       "test-org-456",
			expectedErr: false,
		},
		{
			name:        "SSA revocation with both JTI and org ID",
			jti:         "test-jti-123",
			orgId:       "test-org-456",
			expectedErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
				// Handle SSA revocation requests
				if r.URL.Path != "/jans-config-api/api/v1/jans-auth-server/ssa" {
					t.Errorf("Expected path '/jans-config-api/api/v1/jans-auth-server/ssa', got %s", r.URL.Path)
				}

				if r.Method != http.MethodDelete {
					t.Errorf("Expected DELETE method, got %s", r.Method)
				}

				// Verify query parameters
				if tt.jti != "" {
					if r.URL.Query().Get("jti") != tt.jti {
						t.Errorf("Expected jti %s, got %s", tt.jti, r.URL.Query().Get("jti"))
					}
				}

				if tt.orgId != "" {
					if r.URL.Query().Get("org_id") != tt.orgId {
						t.Errorf("Expected org_id %s, got %s", tt.orgId, r.URL.Query().Get("org_id"))
					}
				}

				w.WriteHeader(http.StatusNoContent)
			}))
			defer server.Close()

			client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
			if err != nil {
				t.Fatalf("Failed to create client: %v", err)
			}

			err = client.RevokeSSA(context.Background(), tt.jti, tt.orgId)

			if tt.expectedErr && err == nil {
				t.Error("Expected error, got nil")
			}

			if !tt.expectedErr && err != nil {
				t.Errorf("Unexpected error: %v", err)
			}
		})
	}
}

func TestClient_RevokeSSA_ErrorCases(t *testing.T) {
	tests := []struct {
		name         string
		jti          string
		orgId        string
		responseCode int
		expectedErr  bool
	}{
		{
			name:         "SSA not found (404)",
			jti:          "nonexistent-jti",
			orgId:        "",
			responseCode: http.StatusNotFound,
			expectedErr:  true,
		},
		{
			name:         "Unauthorized (401)",
			jti:          "test-jti",
			orgId:        "",
			responseCode: http.StatusUnauthorized,
			expectedErr:  true,
		},
		{
			name:         "Server error (500)",
			jti:          "test-jti",
			orgId:        "",
			responseCode: http.StatusInternalServerError,
			expectedErr:  true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
				// Handle SSA revocation requests with error responses
				w.WriteHeader(tt.responseCode)
				w.Header().Set("Content-Type", "application/json")
				json.NewEncoder(w).Encode(map[string]interface{}{
					"code":    tt.responseCode,
					"message": "Error occurred",
				})
			}))
			defer server.Close()

			client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
			if err != nil {
				t.Fatalf("Failed to create client: %v", err)
			}

			err = client.RevokeSSA(context.Background(), tt.jti, tt.orgId)

			if tt.expectedErr && err == nil {
				t.Error("Expected error, got nil")
			}

			if !tt.expectedErr && err != nil {
				t.Errorf("Unexpected error: %v", err)
			}
		})
	}
}
