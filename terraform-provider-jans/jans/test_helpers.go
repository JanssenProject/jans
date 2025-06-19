
package jans

import (
	"io"
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
