package jans

import (
        "context"
        "encoding/json"
        "net/http"
        "net/http/httptest"
        "testing"
)

func TestClient_SearchTokens(t *testing.T) {
        tests := []struct {
                name         string
                pattern      string
                limit        int
                startIndex   int
                responseBody TokenEntityPagedResult
                expectedErr  bool
        }{
                {
                        name:       "successful token search with results",
                        pattern:    "test",
                        limit:      10,
                        startIndex: 0,
                        responseBody: TokenEntityPagedResult{
                                Start:        0,
                                TotalEntries: 2,
                                EntriesCount: 2,
                                Entries: []TokenEntity{
                                        {
                                                TokenCode:      "abc123token",
                                                ClientId:       "client-123",
                                                TokenType:      "access_token",
                                                GrantType:      "client_credentials",
                                                CreationDate:   "2025-10-13T12:00:00",
                                                ExpirationDate: "2025-10-13T13:00:00",
                                                Scope:          "read write",
                                        },
                                        {
                                                TokenCode:      "xyz789token",
                                                ClientId:       "client-456",
                                                TokenType:      "refresh_token",
                                                GrantType:      "authorization_code",
                                                CreationDate:   "2025-10-13T12:10:00",
                                                ExpirationDate: "2025-10-14T12:10:00",
                                                Scope:          "read",
                                        },
                                },
                        },
                        expectedErr: false,
                },
                {
                        name:       "empty token search results",
                        pattern:    "",
                        limit:      10,
                        startIndex: 0,
                        responseBody: TokenEntityPagedResult{
                                Start:        0,
                                TotalEntries: 0,
                                EntriesCount: 0,
                                Entries:      []TokenEntity{},
                        },
                        expectedErr: false,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                // Handle token search requests
                                if r.URL.Path != "/jans-config-api/api/v1/token/search" {
                                        t.Errorf("Expected path '/jans-config-api/api/v1/token/search', got %s", r.URL.Path)
                                }

                                if r.Method != http.MethodGet {
                                        t.Errorf("Expected GET method, got %s", r.Method)
                                }

                                // Verify query parameters
                                if tt.pattern != "" && r.URL.Query().Get("pattern") != tt.pattern {
                                        t.Errorf("Expected pattern %s, got %s", tt.pattern, r.URL.Query().Get("pattern"))
                                }

                                w.Header().Set("Content-Type", "application/json")
                                json.NewEncoder(w).Encode(tt.responseBody)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        result, err := client.SearchTokens(context.Background(), tt.pattern, tt.limit, tt.startIndex)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }

                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }

                        if !tt.expectedErr && result != nil {
                                if result.TotalEntries != tt.responseBody.TotalEntries {
                                        t.Errorf("Expected %d total entries, got %d", tt.responseBody.TotalEntries, result.TotalEntries)
                                }
                                if len(result.Entries) != len(tt.responseBody.Entries) {
                                        t.Errorf("Expected %d entries, got %d", len(tt.responseBody.Entries), len(result.Entries))
                                }
                        }
                })
        }
}

func TestClient_GetTokensByClient(t *testing.T) {
        tests := []struct {
                name         string
                clientId     string
                responseBody TokenEntityPagedResult
                expectedErr  bool
        }{
                {
                        name:     "successful tokens retrieval by client ID",
                        clientId: "client-123",
                        responseBody: TokenEntityPagedResult{
                                Start:        0,
                                TotalEntries: 1,
                                EntriesCount: 1,
                                Entries: []TokenEntity{
                                        {
                                                TokenCode:      "abc123token",
                                                ClientId:       "client-123",
                                                TokenType:      "access_token",
                                                GrantType:      "client_credentials",
                                                CreationDate:   "2025-10-13T12:00:00",
                                                ExpirationDate: "2025-10-13T13:00:00",
                                                Scope:          "read write",
                                        },
                                },
                        },
                        expectedErr: false,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                // Handle tokens by client ID requests
                                expectedPath := "/jans-config-api/api/v1/token/client/" + tt.clientId
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

                        result, err := client.GetTokensByClient(context.Background(), tt.clientId)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }

                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }

                        if !tt.expectedErr && result != nil {
                                if len(result.Entries) != len(tt.responseBody.Entries) {
                                        t.Errorf("Expected %d entries, got %d", len(tt.responseBody.Entries), len(result.Entries))
                                }
                        }
                })
        }
}

func TestClient_RevokeToken(t *testing.T) {
        tests := []struct {
                name        string
                tokenCode   string
                expectedErr bool
        }{
                {
                        name:        "successful token revocation",
                        tokenCode:   "abc123token",
                        expectedErr: false,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                // Handle token revocation requests
                                expectedPath := "/jans-config-api/api/v1/token/revoke/" + tt.tokenCode
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

                        err = client.RevokeToken(context.Background(), tt.tokenCode)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }

                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }
}

func TestClient_SearchTokens_ErrorCases(t *testing.T) {
        tests := []struct {
                name         string
                pattern      string
                responseCode int
                expectedErr  bool
        }{
                {
                        name:         "Unauthorized (401)",
                        pattern:      "test",
                        responseCode: http.StatusUnauthorized,
                        expectedErr:  true,
                },
                {
                        name:         "Not found (404)",
                        pattern:      "nonexistent",
                        responseCode: http.StatusNotFound,
                        expectedErr:  true,
                },
                {
                        name:         "Server error (500)",
                        pattern:      "test",
                        responseCode: http.StatusInternalServerError,
                        expectedErr:  true,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
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

                        _, err = client.SearchTokens(context.Background(), tt.pattern, 10, 0)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }

                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }
}

func TestClient_GetTokensByClient_ErrorCases(t *testing.T) {
        tests := []struct {
                name         string
                clientId     string
                responseCode int
                expectedErr  bool
        }{
                {
                        name:         "Unauthorized (401)",
                        clientId:     "test-client",
                        responseCode: http.StatusUnauthorized,
                        expectedErr:  true,
                },
                {
                        name:         "Not found (404)",
                        clientId:     "nonexistent-client",
                        responseCode: http.StatusNotFound,
                        expectedErr:  true,
                },
                {
                        name:         "Server error (500)",
                        clientId:     "test-client",
                        responseCode: http.StatusInternalServerError,
                        expectedErr:  true,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
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

                        _, err = client.GetTokensByClient(context.Background(), tt.clientId)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }

                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }
}

func TestClient_RevokeToken_ErrorCases(t *testing.T) {
        tests := []struct {
                name         string
                tokenCode    string
                responseCode int
                expectedErr  bool
        }{
                {
                        name:         "Token not found (404)",
                        tokenCode:    "nonexistent-token",
                        responseCode: http.StatusNotFound,
                        expectedErr:  true,
                },
                {
                        name:         "Unauthorized (401)",
                        tokenCode:    "test-token",
                        responseCode: http.StatusUnauthorized,
                        expectedErr:  true,
                },
                {
                        name:         "Server error (500)",
                        tokenCode:    "test-token",
                        responseCode: http.StatusInternalServerError,
                        expectedErr:  true,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
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

                        err = client.RevokeToken(context.Background(), tt.tokenCode)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }

                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }
}
