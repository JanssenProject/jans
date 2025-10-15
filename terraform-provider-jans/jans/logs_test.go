package jans

import (
        "context"
        "encoding/json"
        "net/http"
        "net/http/httptest"
        "testing"
)

func TestClient_GetAuditLogs(t *testing.T) {
        tests := []struct {
                name         string
                pattern      string
                startIndex   int
                limit        int
                startDate    string
                endDate      string
                responseBody LogPagedResult
                expectedErr  bool
        }{
                {
                        name:       "successful audit logs retrieval",
                        pattern:    "test",
                        startIndex: 0,
                        limit:      50,
                        startDate:  "2023-01-01",
                        endDate:    "2023-12-31",
                        responseBody: LogPagedResult{
                                Start:             0,
                                TotalEntriesCount: 100,
                                EntriesCount:      50,
                                Entries:           []string{"log entry 1", "log entry 2"},
                        },
                        expectedErr: false,
                },
                {
                        name:         "empty parameters",
                        responseBody: LogPagedResult{},
                        expectedErr:  false,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                // Handle audit log requests
                                if r.URL.Path != "/jans-config-api/api/v1/audit" {
                                        t.Errorf("Expected path '/jans-config-api/api/v1/audit', got %s", r.URL.Path)
                                }

                                if r.Method != http.MethodGet {
                                        t.Errorf("Expected GET method, got %s", r.Method)
                                }

                                // Check query parameters
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

                        result, err := client.GetAuditLogs(context.Background(), tt.pattern, tt.startIndex, tt.limit, tt.startDate, tt.endDate)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }

                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }

                        if !tt.expectedErr && result != nil {
                                if result.Start != tt.responseBody.Start {
                                        t.Errorf("Expected start %d, got %d", tt.responseBody.Start, result.Start)
                                }
                                if result.TotalEntriesCount != tt.responseBody.TotalEntriesCount {
                                        t.Errorf("Expected totalEntriesCount %d, got %d", tt.responseBody.TotalEntriesCount, result.TotalEntriesCount)
                                }
                        }
                })
        }
}

func TestClient_GetAuditLogs_InvalidClient(t *testing.T) {
        client, err := NewInsecureClient("", "", "")
        if err != nil {
                t.Fatalf("Failed to create client: %v", err)
        }

        _, err = client.GetAuditLogs(context.Background(), "", 0, 0, "", "")
        if err == nil {
                t.Error("Expected error for invalid client configuration, got nil")
        }
}

func TestClient_GetAuditLogs_ErrorCases(t *testing.T) {
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

                        _, err = client.GetAuditLogs(context.Background(), tt.pattern, 0, 50, "", "")

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }

                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }
}
