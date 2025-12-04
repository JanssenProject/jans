import express from "express";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import Utils from "./utils"
import { z } from "zod";
import axios from "axios";
import crypto from "crypto";

const app = express();
app.use(express.json());

interface OIDCDiscoveryMetadata {
    registration_endpoint?: string;
    [key: string]: unknown;
}

const server = new McpServer({ name: "jans-tarp-mcp-server", version: "1.0.0" });

// Tool: registerOIDCClient (existing + store)
server.registerTool(
    "registerOIDCClient",
    {
        description: "Registers an OIDC client using dynamic registration endpoint",
        inputSchema: z.object({
            issuer: z.string().url(),
            redirect_uris: z.array(z.string().url()),
            scopes: z.array(z.string()),
            response_types: z.array(z.string())
        }),
        outputSchema: z.object({ result: z.any() })
    },
    async ({ issuer, redirect_uris, scopes, response_types }) => {

        // Discover OIDC metadata
        const discoveryRes = await fetch(`${issuer}/.well-known/openid-configuration`);
        if (!discoveryRes.ok) throw new Error("Failed to fetch OIDC metadata");


        const metadata = await discoveryRes.json() as OIDCDiscoveryMetadata;
        if (!metadata.registration_endpoint) throw new Error("Provider does not support dynamic registration");


        // Register client
        const registrationRes = await fetch(metadata.registration_endpoint, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                redirect_uris,
                scope: scopes.join(" "),
                response_types
            })
        });

        const registrationJson = await registrationRes.json() as Record<string, unknown>;

        return {
            content: [{ type: "text", text: `OIDC client registered successfully` }],
            structuredContent: registrationJson
        };
    }
);

// Tool: startAuthFlow
server.registerTool(
  "startAuthFlow",
  {
    description: "Generate authorization URL for a registered client_id (PKCE + state)",
    inputSchema: z.object({
      issuer: z.string(),
      client_id: z.string(),
      scope: z.string(),
      response_type: z.string(),
      redirect_uri: z.string(),
      code_challenge_method: z.string(),
      code_challenge: z.string(),
      nonce: z.string(),
    }),
    outputSchema: z.object({
      authorization_url: z.string(),
      redirect_uri: z.string(),
      code_verifier: z.string(),
      state: z.string(),
      issuer: z.string()
    })
  },
  async ({ issuer, client_id, scope, response_type, redirect_uri, code_challenge_method, nonce}) => {
    //const client = registeredClients[client_id];
    //if (!client) throw new Error("client_id not found. Register it first with registerOIDCClient");
    console.log("code_challenge_method==========================="+code_challenge_method)
    // discover OIDC metadata
    const discoveryRes = await axios.get(`${issuer}/.well-known/openid-configuration`);
    const metadata = discoveryRes.data;
    if (!metadata.authorization_endpoint) throw new Error("Provider missing authorization_endpoint");

    // PKCE
    const code_verifier = Utils.base64url(crypto.randomBytes(32));
    const code_challenge = Utils.codeChallengeFromVerifier(code_verifier);

    const state = Utils.base64url(crypto.randomBytes(16));
    //const redirect_uri = client.redirect_uris[0]; // assume first registered redirect uri

    const params = new URLSearchParams({
      response_type,
      client_id,
      redirect_uri,
      scope,
      state,
      code_challenge,
      code_challenge_method,
      nonce
    });

    const authorization_url = `${metadata.authorization_endpoint}?${params.toString()}`;
    console.log("authorization_url==========================="+authorization_url)
    // Return code_verifier so the extension can save it (for exchange)
    // Also return state so extension can map verifier -> state
    return {
      content: [
        {
          type: "text",
          text: "Generated authorization URL. See structured content for details."
        }
      ],
      structuredContent: { authorization_url, redirect_uri, code_verifier, state, issuer }
    };
  }
);

// Tool: exchangeToken
server.registerTool(
  "TokenExchange",
  {
    description: "Exchange authorization code for tokens and call userinfo",
    inputSchema: z.object({
      issuer: z.string().url(),
      code: z.string(),
      client_id: z.string(),
      client_secret: z.string().optional(),
      code_verifier: z.string(),
      redirect_uri: z.string()
    }),
    outputSchema: z.object({
      tokens: z.any(),
      userinfo: z.any()
    })
  },
   async ({ issuer, code, client_id, client_secret, code_verifier, redirect_uri }) => {
    //const client = registeredClients[client_id];
    //if (!client) throw new Error("client_id not found");

    // discover endpoints
    const discoveryRes = await axios.get(`${issuer}/.well-known/openid-configuration`);
    const metadata = discoveryRes.data;
    if (!metadata.token_endpoint) throw new Error("Provider missing token_endpoint");

    // Build token request. For public clients, client_secret is usually not present.
    const tokenParams = new URLSearchParams({
      grant_type: "authorization_code",
      code,
      redirect_uri,
      client_id,
      code_verifier
    });

    // If registration includes client_secret, include basic auth
     const headers: Record<string, string> = { "Content-Type": "application/x-www-form-urlencoded" };
    if (client_secret) {
      const cred = Buffer.from(`${client_id}:${client_secret}`).toString('base64');
      headers["Authorization"] = `Basic ${cred}`;
    }

    const tokenResp = await axios.post(metadata.token_endpoint, tokenParams.toString(), { headers });
    const tokens = tokenResp.data;

    // call userinfo if available
    let userinfo = null;
    if (metadata.userinfo_endpoint && tokens.access_token) {
      const uiResp = await axios.get(metadata.userinfo_endpoint, {
        headers: { Authorization: `Bearer ${tokens.access_token}` }
      });
      userinfo = uiResp.data;
    }

     return {
      content: [
        { type: "text", text: "Exchanged code for token then fetch userinfo using token. See structured content." }
      ],
      structuredContent: { tokens, userinfo }
     };
  }
);

// Create HTTP transport for handling HTTP requests
const transport = new StreamableHTTPServerTransport({
  sessionIdGenerator: undefined, // Stateless mode
  enableJsonResponse: true, // Return JSON responses instead of SSE
});

// Connect the server to the transport (after registering tools)
// 5. Connect the MCP Server to the Transport
async function startMcpServer() {
    await server.connect(transport);
    console.log("MCP Server connected to StreamableHTTPServerTransport.");
  }

app.post("/mcp", async (req, res) => {
  /*
  console.log(req.body)
  console.log(JSON.stringify(req.body.params.capabilities.sampling))
  const result = await transport.handleRequest(req, res, req.body);
  console.log(result);
  res.json(result);
*/
  try {
    //await server.connect(transport);
    //await server.connect(transport);
    const result = await transport.handleRequest(req, res, req.body);
    console.log(result);
    //res.json(result);
  } catch (error) {
    console.error('Error handling MCP request:', error);
    if (!res.headersSent) {
      res.status(500).json({
        jsonrpc: '2.0',
        error: { code: -32603, message: 'Internal server error' },
        id: null,
      });
    }
  }
});

app.get("/", (req, res) => {
    console.log("Server is running!")
  //const result = await transport.handleRequest(req, res, req.body);
  res.json({ message: "Server is running!" });
});

const port = parseInt(process.env.PORT || "3001");
app.listen(port, () => {
    console.log(`MCP server running on http://localhost:${port}/mcp`);
    startMcpServer(); // Connect MCP server after Express starts
  }).on("error", (error) => {
    console.error("Server error:", error);
    process.exit(1);
  });