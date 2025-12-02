import express from "express";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { z } from "zod";
import axios from "axios";

const app = express();
app.use(express.json());

interface OIDCDiscoveryMetadata {
    registration_endpoint?: string;
    [key: string]: unknown;
}

//const server = new Server({ name: "oidc-registration-server" });
const server = new McpServer({ name: "simple-math-server", version: "1.0.0" });

// Register tools BEFORE connecting to transport
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
        console.log(issuer);
        console.log(redirect_uris);
        console.log(scopes);
        console.log(response_types);
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