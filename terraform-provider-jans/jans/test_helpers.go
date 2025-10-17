package jans

import (
        "io"
        "net/http"
        "net/url"
        "os"
        "strings"
)

// GenerateMetadataReader creates a metadata reader with the correct host domain
// dynamically replaced from the template
func GenerateMetadataReader(hostURL string) (io.Reader, error) {
        // Parse the host URL to get just the domain
        parsedURL, err := url.Parse(hostURL)
        if err != nil {
                return nil, err
        }

        host := parsedURL.Host

        // Read the template file
        templateBytes, err := os.ReadFile("testdata/metadata.xml.template")
        if err != nil {
                return nil, err
        }

        // Replace the placeholder with the actual host
        metadataContent := strings.ReplaceAll(string(templateBytes), "{{HOST}}", host)

        return strings.NewReader(metadataContent), nil
}

// GetCurrentHost extracts the host from environment variables or test configuration
func GetCurrentHost() string {
        if hostURL := os.Getenv("JANS_URL"); hostURL != "" {
                return hostURL
        }
        // Fallback for tests - will use current environment
        return "https://moabu-singular-bee.gluu.info"
}

// createMockOAuthHandler creates an HTTP handler that handles OAuth token requests
// by returning the requested scope back in the response. This is useful for unit tests.
func createMockOAuthHandler(apiHandler func(w http.ResponseWriter, r *http.Request)) http.HandlerFunc {
        return func(w http.ResponseWriter, r *http.Request) {
                // Handle OAuth token requests
                if r.URL.Path == "/jans-auth/restv1/token" {
                        // Parse the requested scope from the form
                        if err := r.ParseForm(); err != nil {
                                w.WriteHeader(http.StatusBadRequest)
                                return
                        }
                        
                        requestedScope := r.FormValue("scope")
                        
                        // Return a token response with the requested scope
                        w.Header().Set("Content-Type", "application/json")
                        w.Write([]byte(`{"access_token":"test-token","token_type":"Bearer","expires_in":3600,"scope":"` + requestedScope + `"}`))
                        return
                }
                
                // Let the provided handler handle other requests
                apiHandler(w, r)
        }
}
