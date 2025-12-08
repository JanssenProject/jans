import express from "express";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import Utils from "./utils"
import { z } from "zod";
import axios from "axios";
import crypto from "crypto";

const app = express();
app.use(express.json());

// Constants
const PORT = parseInt(process.env.PORT || "3001");
const SERVER_NAME = "jans-tarp-mcp-server";
const SERVER_VERSION = "1.0.0";

// Types
interface OIDCDiscoveryMetadata {
  registration_endpoint?: string;
  authorization_endpoint?: string;
  token_endpoint?: string;
  userinfo_endpoint?: string;
  [key: string]: unknown;
}

interface OIDCClientConfig {
  issuer: string;
  redirect_uris: string[];
  scopes: string[];
  response_types: string[];
  token_endpoint_auth_method: string;
  userinfo_signed_response_alg: string;
  jansInclClaimsInIdTkn: string;
}

// HTTP Client with retry and timeout configuration
const httpClient = axios.create({
  timeout: 10000,
  maxRedirects: 3,
  validateStatus: (status) => status >= 200 && status < 300,
});

// Cache for OIDC metadata to avoid repeated discovery calls
const metadataCache = new Map<string, {
  metadata: OIDCDiscoveryMetadata;
  timestamp: number;
}>();

const CACHE_TTL = 5 * 60 * 1000; // 5 minutes


// Helper Functions
async function discoverOIDCMetadata(issuer: string): Promise<OIDCDiscoveryMetadata> {
  const cached = metadataCache.get(issuer);
  const now = Date.now();

  if (cached && (now - cached.timestamp) < CACHE_TTL) {
    return cached.metadata;
  }

  try {
    const response = await httpClient.get(
      `${issuer.replace(/\/+$/, '')}/.well-known/openid-configuration`
    );

    if (!response.data) {
      throw new Error("Empty response from OIDC discovery endpoint");
    }

    const metadata = response.data as OIDCDiscoveryMetadata;
    metadataCache.set(issuer, { metadata, timestamp: now });
    return metadata;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      throw new Error(`OIDC discovery failed: ${error.message}`);
    }
    throw error;
  }
}

function generateRandomString(bytes: number): string {
  return Utils.base64url(crypto.randomBytes(bytes));
}

function validateIssuerUrl(issuer: string): void {
  try {
    new URL(issuer);
  } catch {
    throw new Error(`Invalid issuer URL: ${issuer}`);
  }
}

// Create MCP Server
const server = new McpServer({
  name: SERVER_NAME,
  version: SERVER_VERSION
});

// Tool: registerOIDCClient
server.registerTool(
  "registerOIDCClient",
  {
    description: "Registers an OIDC client using dynamic registration endpoint",
    inputSchema: z.object({
      issuer: z.string().url().describe("OIDC provider issuer URL"),
      redirect_uris: z.array(z.string().url()).min(1).describe("Allowed redirect URIs"),
      scopes: z.array(z.string()).min(1).describe("Requested scopes"),
      response_types: z.array(z.string()).min(1).describe("OAuth2 response types"),
      token_endpoint_auth_method: z.string().describe("Token endpoint authentication method"),
      userinfo_signed_response_alg: z.string().describe("Userinfo signed response algorithm"),
      jansInclClaimsInIdTkn: z.string().describe("Include claims in ID token setting"),
    }),
    outputSchema: z.object({
      client_id: z.string(),
      client_secret: z.string().optional(),
      registration_client_uri: z.string().optional(),
      registration_access_token: z.string().optional(),
    })
  },
  async (input: OIDCClientConfig) => {
    console.log('Inside registerOIDCClient method')
    validateIssuerUrl(input.issuer);

    const metadata = await discoverOIDCMetadata(input.issuer);

    if (!metadata.registration_endpoint) {
      throw new Error("OIDC provider does not support dynamic client registration");
    }

    const registrationData = {
      redirect_uris: input.redirect_uris,
      scope: input.scopes.join(" "),
      response_types: input.response_types,
      token_endpoint_auth_method: input.token_endpoint_auth_method,
      userinfo_signed_response_alg: input.userinfo_signed_response_alg,
      jansInclClaimsInIdTkn: input.jansInclClaimsInIdTkn,
    };

    try {
      const response = await httpClient.post(
        metadata.registration_endpoint,
        registrationData,
        {
          headers: { "Content-Type": "application/json" },
        }
      );

      const result = response.data as Record<string, unknown>;

      return {
        content: [{
          type: "text",
          text: "OIDC client registered successfully"
        }],
        structuredContent: result
      };
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(`Client registration failed: ${error.response?.data?.error_description || error.message}`);
      }
      throw error;
    }
  }
);

// Tool: startAuthFlow
server.registerTool(
  "startAuthFlow",
  {
    description: "Generate authorization URL for a registered client (PKCE + state)",
    inputSchema: z.object({
      issuer: z.string().url().describe("OIDC provider issuer URL"),
      client_id: z.string().min(1).describe("Registered client ID"),
      scope: z.string().min(1).describe("Space-separated scopes"),
      response_type: z.string().default("code").describe("OAuth2 response type"),
      redirect_uri: z.string().describe("Redirect URI for authorization response"),
      code_challenge_method: z.string().default("S256").describe("PKCE code challenge method"),
      code_challenge: z.string().min(1).describe("PKCE code challenge"),
      nonce: z.string().min(16).describe("Nonce for ID token validation"),
    }),
    outputSchema: z.object({
      authorization_url: z.string(),
      state: z.string(),
      expires_in: z.number(),
    })
  },
  async ({
    issuer,
    client_id,
    scope,
    response_type,
    redirect_uri,
    code_challenge_method,
    code_challenge,
    nonce
  }) => {
    console.log('Inside startAuthFlow method')
    validateIssuerUrl(issuer);

    const metadata = await discoverOIDCMetadata(issuer);

    if (!metadata.authorization_endpoint) {
      throw new Error("OIDC provider missing authorization endpoint");
    }

    const state = generateRandomString(16);

    const params = new URLSearchParams({
      response_type,
      client_id,
      redirect_uri,
      scope,
      state,
      code_challenge,
      code_challenge_method,
      nonce,
    });

    const authorization_url = `${metadata.authorization_endpoint}?${params.toString()}`;

    return {
      content: [{
        type: "text",
        text: "Generated authorization URL with PKCE protection"
      }],
      structuredContent: {
        authorization_url,
        state,
        expires_in: 600 // 10 minutes validity
      }
    };
  }
);

// Tool: exchangeToken
server.registerTool(
  "exchangeToken",
  {
    description: "Exchange authorization code for tokens and fetch userinfo",
    inputSchema: z.object({
      issuer: z.string().url().describe("OIDC provider issuer URL"),
      code: z.string().min(1).describe("Authorization code from redirect"),
      client_id: z.string().min(1).describe("Registered client ID"),
      client_secret: z.string().optional().describe("Client secret (if confidential)"),
      code_verifier: z.string().min(1).describe("PKCE code verifier"),
      redirect_uri: z.string().url().describe("Redirect URI used in authorization"),
    }),
    outputSchema: z.object({
      tokens: z.object({
        access_token: z.string(),
        token_type: z.string(),
        expires_in: z.number().optional(),
        refresh_token: z.string().optional(),
        id_token: z.string().optional(),
      }),
      userinfo: z.any().optional(),
    })
  },
  async ({
    issuer,
    code,
    client_id,
    client_secret,
    code_verifier,
    redirect_uri
  }) => {
    validateIssuerUrl(issuer);

    const metadata = await discoverOIDCMetadata(issuer);

    if (!metadata.token_endpoint) {
      throw new Error("OIDC provider missing token endpoint");
    }

    // Build token request
    const tokenParams = new URLSearchParams({
      grant_type: "authorization_code",
      code,
      redirect_uri,
      client_id,
      code_verifier,
    });

    // Configure authentication
    const headers: Record<string, string> = {
      "Content-Type": "application/x-www-form-urlencoded",
    };

    if (client_secret) {
      const credentials = Buffer.from(`${client_id}:${client_secret}`).toString('base64');
      headers["Authorization"] = `Basic ${credentials}`;
    }

    try {
      const response = await httpClient.post(
        metadata.token_endpoint,
        tokenParams.toString(),
        { headers }
      );

      const tokens = response.data;
      if (!tokens?.access_token || !tokens?.token_type) {
        throw new Error('Invalid token response: missing required fields');
      }

      // Fetch userinfo if endpoint exists and we have an access token
      let userinfo = null;
      if (metadata.userinfo_endpoint && tokens.access_token) {
        try {
          const userinfoResponse = await httpClient.get(
            metadata.userinfo_endpoint,
            {
              headers: {
                Authorization: `Bearer ${tokens.access_token}`,
                Accept: "application/json"
              },
            }
          );
          userinfo = userinfoResponse.data;
        } catch (error) {
          console.warn("Failed to fetch userinfo:", error);
          // Continue without userinfo - tokens are still valid
        }
      }

      return {
        content: [{
          type: "text",
          text: "Successfully exchanged code for tokens" +
            (userinfo ? " and fetched userinfo" : "")
        }],
        structuredContent: { tokens, userinfo }
      };
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const errorDetail = error.response?.data?.error_description || error.message;
        throw new Error(`Token exchange failed: ${errorDetail}`);
      }
      throw error;
    }
  }
);

// Create HTTP transport for handling HTTP requests
const transport = new StreamableHTTPServerTransport({
  sessionIdGenerator: undefined, // Stateless mode
  enableJsonResponse: true, // Return JSON responses instead of SSE
});

// Connect MCP Server to Transport
async function connectMcpServer() {
  try {
    await server.connect(transport);
    console.log("MCP Server connected to StreamableHTTPServerTransport");
  } catch (error) {
    console.error("Failed to connect MCP Server:", error);
    process.exit(1);
  }
}

// Request handler middleware
async function handleMcpRequest(req: express.Request, res: express.Response) {
  try {
    const result = await transport.handleRequest(req, res, req.body);
    console.log(result);
    // Ensure JSON response if not already sent
    //if (!res.headersSent) {
    //res.json(result);
    //}
  } catch (error) {
    console.error('Error handling MCP request:', error);

    if (!res.headersSent) {
      res.status(500).json({
        jsonrpc: '2.0',
        error: {
          code: -32603,
          message: 'Internal server error',
          data: process.env.NODE_ENV === 'development' ? String(error) : undefined
        },
        id: req.body?.id || null,
      });
    }
  }
}

// Routes
app.post("/mcp", (req, res) => handleMcpRequest(req, res));

app.get("/health", (_req, res) => {
  res.json({
    status: "healthy",
    server: SERVER_NAME,
    version: SERVER_VERSION,
    timestamp: new Date().toISOString()
  });
});

app.get("/", (_req, res) => {
  res.json({
    message: "Jans TARP MCP Server",
    endpoints: {
      mcp: "POST /mcp",
      health: "GET /health"
    }
  });
});

const httpServer = app.listen(PORT, async () => {
  console.log(`MCP server running on http://localhost:${PORT}`);

  // Connect MCP server after Express starts
  await connectMcpServer();

  console.log("Server ready to accept requests");
});

// Graceful shutdown
const shutdown = async () => {
  console.log('Shutting down gracefully...');
  httpServer.close(() => {
    console.log('HTTP server closed');
    process.exit(0);
  });
  // Force exit after timeout
  setTimeout(() => process.exit(1), 10000);
};

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);