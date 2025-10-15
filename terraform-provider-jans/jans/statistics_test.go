package jans

import (
        "context"
        "encoding/json"
        "net/http"
        "net/http/httptest"
        "reflect"
        "testing"
)

func TestClient_GetStatistics(t *testing.T) {
        tests := []struct {
                name         string
                month        string
                startMonth   string
                endMonth     string
                format       string
                responseBody StatisticsResponse
                expectedErr  bool
        }{
                {
                        name:  "successful statistics retrieval with month",
                        month: "202410",
                        responseBody: StatisticsResponse{
                                {"date": "2024-10-13", "metric": "users", "value": float64(100)},
                        },
                        expectedErr: false,
                },
                {
                        name:       "successful statistics retrieval with date range",
                        startMonth: "202401",
                        endMonth:   "202403",
                        responseBody: StatisticsResponse{
                                {"date": "2024-01-01", "metric": "users", "value": float64(80)},
                                {"date": "2024-02-01", "metric": "users", "value": float64(90)},
                        },
                        expectedErr: false,
                },
                {
                        name:         "empty statistics",
                        month:        "202412",
                        responseBody: StatisticsResponse{},
                        expectedErr:  false,
                },
                {
                        name:   "statistics with custom format",
                        month:  "202410",
                        format: "json",
                        responseBody: StatisticsResponse{
                                {"month": "2024-10", "data": []interface{}{map[string]interface{}{"metric": "users", "value": float64(100)}}},
                        },
                        expectedErr: false,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                // Handle statistics requests
                                if r.URL.Path != "/jans-config-api/api/v1/stat" {
                                        t.Errorf("Expected path '/jans-config-api/api/v1/stat', got %s", r.URL.Path)
                                }

                                if r.Method != http.MethodGet {
                                        t.Errorf("Expected GET method, got %s", r.Method)
                                }

                                // Verify query parameters
                                if tt.month != "" {
                                        if r.URL.Query().Get("month") != tt.month {
                                                t.Errorf("Expected month %s, got %s", tt.month, r.URL.Query().Get("month"))
                                        }
                                }

                                if tt.startMonth != "" {
                                        if r.URL.Query().Get("start_month") != tt.startMonth {
                                                t.Errorf("Expected start_month %s, got %s", tt.startMonth, r.URL.Query().Get("start_month"))
                                        }
                                }

                                if tt.endMonth != "" {
                                        if r.URL.Query().Get("end_month") != tt.endMonth {
                                                t.Errorf("Expected end_month %s, got %s", tt.endMonth, r.URL.Query().Get("end_month"))
                                        }
                                }

                                if tt.format != "" {
                                        if r.URL.Query().Get("format") != tt.format {
                                                t.Errorf("Expected format %s, got %s", tt.format, r.URL.Query().Get("format"))
                                        }
                                }

                                w.Header().Set("Content-Type", "application/json")
                                json.NewEncoder(w).Encode(tt.responseBody)
                        }))
                        defer server.Close()

                        client, err := NewInsecureClient(server.URL, "test-client-id", "test-client-secret")
                        if err != nil {
                                t.Fatalf("Failed to create client: %v", err)
                        }

                        result, err := client.GetStatistics(context.Background(), tt.month, tt.startMonth, tt.endMonth, tt.format)

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }

                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }

                        if !tt.expectedErr && result != nil {
                                if len(*result) != len(tt.responseBody) {
                                        t.Errorf("Expected %d statistics entries, got %d", len(tt.responseBody), len(*result))
                                }
                                if !reflect.DeepEqual(*result, tt.responseBody) {
                                        t.Errorf("Expected response body %v, got %v", tt.responseBody, *result)
                                }
                        }
                })
        }
}

func TestClient_GetStatistics_ErrorCases(t *testing.T) {
        tests := []struct {
                name         string
                month        string
                responseCode int
                expectedErr  bool
        }{
                {
                        name:         "Unauthorized (401)",
                        month:        "202410",
                        responseCode: http.StatusUnauthorized,
                        expectedErr:  true,
                },
                {
                        name:         "Not found (404)",
                        month:        "999999",
                        responseCode: http.StatusNotFound,
                        expectedErr:  true,
                },
                {
                        name:         "Server error (500)",
                        month:        "202410",
                        responseCode: http.StatusInternalServerError,
                        expectedErr:  true,
                },
        }

        for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                        server := httptest.NewServer(createMockOAuthHandler(func(w http.ResponseWriter, r *http.Request) {
                                // Handle statistics requests with error responses
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

                        _, err = client.GetStatistics(context.Background(), tt.month, "", "", "")

                        if tt.expectedErr && err == nil {
                                t.Error("Expected error, got nil")
                        }

                        if !tt.expectedErr && err != nil {
                                t.Errorf("Unexpected error: %v", err)
                        }
                })
        }
}
