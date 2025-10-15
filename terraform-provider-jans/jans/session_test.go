package jans

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestClient_GetSessions(t *testing.T) {
	tests := []struct {
		name         string
		responseBody SessionPagedResult
		expectedErr  bool
	}{
		{
			name: "successful sessions retrieval with data",
			responseBody: SessionPagedResult{
				Start:             0,
				TotalEntriesCount: 2,
				EntriesCount:      2,
				Entries: []SessionId{
					{
						Sid:          "test-sid-1",
						UserDn:       "uid=user1,ou=people,o=jans",
						State:        "authenticated",
						CreationDate: "2025-10-13T12:00:00",
					},
					{
						Sid:          "test-sid-2",
						UserDn:       "uid=user2,ou=people,o=jans",
						State:        "authenticated",
						CreationDate: "2025-10-13T12:05:00",
					},
				},
			},
			expectedErr: false,
		},
		{
			name: "empty sessions list",
			responseBody: SessionPagedResult{
				Start:             0,
				TotalEntriesCount: 0,
				EntriesCount:      0,
				Entries:           []SessionId{},
			},
			expectedErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
				// Handle session requests
				if r.URL.Path != "/jans-config-api/api/v1/jans-auth-server/session" {
					t.Errorf("Expected path '/jans-config-api/api/v1/jans-auth-server/session', got %s", r.URL.Path)
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

			result, err := client.GetSessions(context.Background())

			if tt.expectedErr && err == nil {
				t.Error("Expected error, got nil")
			}

			if !tt.expectedErr && err != nil {
				t.Errorf("Unexpected error: %v", err)
			}

			if !tt.expectedErr && result != nil {
				if len(result) != len(tt.responseBody.Entries) {
					t.Errorf("Expected %d sessions, got %d", len(tt.responseBody.Entries), len(result))
				}
				for i, session := range result {
					if session.Sid != tt.responseBody.Entries[i].Sid {
						t.Errorf("Session %d: expected SID %s, got %s", i, tt.responseBody.Entries[i].Sid, session.Sid)
					}
				}
			}
		})
	}
}

func TestClient_GetSession(t *testing.T) {
	tests := []struct {
		name         string
		sid          string
		responseBody SessionId
		expectedErr  bool
	}{
		{
			name: "successful session retrieval by ID",
			sid:  "test-sid-123",
			responseBody: SessionId{
				Sid:          "test-sid-123",
				UserDn:       "uid=testuser,ou=people,o=jans",
				State:        "authenticated",
				CreationDate: "2025-10-13T12:00:00",
				LastUsedAt:   "2025-10-13T12:30:00",
			},
			expectedErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
				// Handle session by ID requests
				expectedPath := "/jans-config-api/api/v1/jans-auth-server/session/sid/" + tt.sid
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

			result, err := client.GetSession(context.Background(), tt.sid)

			if tt.expectedErr && err == nil {
				t.Error("Expected error, got nil")
			}

			if !tt.expectedErr && err != nil {
				t.Errorf("Unexpected error: %v", err)
			}

			if !tt.expectedErr && result != nil {
				if result.Sid != tt.responseBody.Sid {
					t.Errorf("Expected SID %s, got %s", tt.responseBody.Sid, result.Sid)
				}
				if result.UserDn != tt.responseBody.UserDn {
					t.Errorf("Expected UserDn %s, got %s", tt.responseBody.UserDn, result.UserDn)
				}
			}
		})
	}
}

func TestClient_RevokeUserSessions(t *testing.T) {
	tests := []struct {
		name        string
		userDn      string
		expectedErr bool
	}{
		{
			name:        "successful session revocation",
			userDn:      "uid=testuser,ou=people,o=jans",
			expectedErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
				// Handle session revocation requests
				expectedPath := "/jans-config-api/api/v1/jans-auth-server/session/user/" + tt.userDn
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

			err = client.RevokeUserSessions(context.Background(), tt.userDn)

			if tt.expectedErr && err == nil {
				t.Error("Expected error, got nil")
			}

			if !tt.expectedErr && err != nil {
				t.Errorf("Unexpected error: %v", err)
			}
		})
	}
}
