#!/bin/bash

# ============================================================================
# Jans OIDC Clients Viewer - MCP Server Runtime Configuration
# ============================================================================
# 
# INSTRUCTIONS:
# 1. Replace the placeholder values below with your actual Jans server details
# 2. Save this file
# 3. Make it executable: chmod +x run-mcp-server.sh
# 4. Run: ./run-mcp-server.sh
#
# ============================================================================

# TODO: Replace with your actual Jans Config API base URL (without /jans-config-api path)
# Example: https://my-jans-server.company.com
export JANS_CONFIG_API_URL="https://ossdhaval-sharp-swift.gluu.info"

# TODO: Replace with your actual OAuth2 access token
# The token must have scope: https://jans.io/oauth/config/openid/clients.readonly
export JANS_OAUTH_ACCESS_TOKEN="602c7e69-923a-4cc4-b4d7-e6e60480dd35"

# ============================================================================
# Run the MCP server with MCP Inspector
# ============================================================================
echo "Starting Jans OIDC Clients Viewer..."
echo "API URL: $JANS_CONFIG_API_URL"
echo "Token: ${JANS_OAUTH_ACCESS_TOKEN:0:20}..." # Show only first 20 chars for security
echo ""

# Option 1: Run with MCP Inspector (for testing/debugging)
npx @modelcontextprotocol/inspector java -jar target/mcp-jans-oidc-viewer-2.0.0.jar -dev

# Option 2: Run standalone (uncomment to use)
# java -jar target/mcp-jans-oidc-viewer-2.0.0.jar -dev
