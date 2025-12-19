import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';
import MCPService from './service/MCPService';
import { LLMClientFactory } from './llm/LLMClient';
import { v4 as uuidv4 } from 'uuid';
import Utils from '../options/Utils';
import AuthenticationService from '../service/authenticationService';
import { ILooseObject } from "../options/ILooseObject";
import StorageHelper from './helper/StorageHelper'
import ConfigurationManager from './helper/ConfigurationManager'
import { STORAGE_KEYS, DEFAULT_VALUES } from './helper/Constants';

// Services
const mcpService = new MCPService();
const authenticationService = new AuthenticationService();

// MCP Client initialization
const client = new Client(
  {
    name: "jans-tarp-mcp-client",
    version: "1.0.0"
  },
  {
    capabilities: {
      sampling: {
        tools: {},
      },
    }
  }
);

// State
let isConnected = false;
let llmClient: any = null;

// Type definitions
interface ToolCallResult {
  tool: string;
  result?: any;
  error?: string;
  notifyOnDataChange?: boolean;
}

interface OIDCClientRegistrationArgs {
  issuer: string;
  scopes: string[];
  response_types: string[];
  redirect_uris?: string[];
  token_endpoint_auth_method?: string;
  userinfo_signed_response_alg?: string;
  jansInclClaimsInIdTkn?: string;
}

/**
 * Persist OIDC login tokens and user information to storage under the LOGIN_DETAILS key.
 *
 * @param tokenResponse - Token response object containing at least `access_token` and `id_token`
 * @param userInfoResponse - User info object (claims/profile) obtained from the OIDC userinfo endpoint
 * @param displayToken - Whether the token should be marked for display in the UI
 */
async function saveLoginDetailsInStorage(
  tokenResponse: any,
  userInfoResponse: any,
  displayToken: boolean
): Promise<void> {
  await StorageHelper.set(STORAGE_KEYS.LOGIN_DETAILS, {
    access_token: tokenResponse.access_token,
    userDetails: userInfoResponse,
    id_token: tokenResponse.id_token,
    displayToken
  });
}

/**
 * Ensure an LLM client is created and initialized, then return the client instance.
 *
 * @returns The initialized LLM client instance.
 * @throws If no API key is configured for the selected provider, or if client creation or initialization fails.
 */
async function initializeLLMClient(): Promise<any> {

  try {
    const provider = await ConfigurationManager.getProvider();
    const apiKey = await ConfigurationManager.getApiKey();

    if (llmClient) return llmClient;

    if (!apiKey) {
      throw new Error(`API key not found for ${provider}. Please configure your API key.`);
    }

    llmClient = await LLMClientFactory.createClient(provider);
    await llmClient.initialize(apiKey);

    return llmClient;
  } catch (error) {
    console.error("Failed to initialize LLM client:", error);
    throw error;
  }
}

/**
 * Establishes and caches a connection to the MCP server.
 *
 * Initializes the MCP client using the configured MCP server URL and marks the module as connected.
 *
 * @throws If the MCP server URL is not configured.
 * @throws If establishing the connection to the MCP server fails.
 */
async function initializeMCPClient(): Promise<void> {
  if (isConnected) return;

  try {
    const mcpServerUrl = await ConfigurationManager.getMCPServerUrl();
    if (!mcpServerUrl) {
      throw new Error("MCP server URL not configured");
    }

    const transport = new StreamableHTTPClientTransport(
      new URL(`${mcpServerUrl}/mcp`)
    );
    await client.connect(transport);
    isConnected = true;
    console.log("MCP client connected successfully");
  } catch (error) {
    console.error("Failed to connect to MCP server:", error);
    throw error;
  }
}

/**
 * Registers an OIDC client with the MCP tool and persists the resulting client record.
 *
 * Enhances the provided registration parameters with a Chrome extension redirect URI, standard token auth method, and signing settings, calls the MCP `registerOIDCClient` tool, and saves the returned client information in TARP storage.
 *
 * @param args - OIDC client registration parameters (issuer, scopes, response types, client metadata, etc.)
 * @returns A ToolCallResult containing the MCP tool response for the registered OIDC client
 */
async function handleRegisterOIDCClient(args: OIDCClientRegistrationArgs): Promise<ToolCallResult> {
  const enhancedArgs: OIDCClientRegistrationArgs = {
    ...args,
    redirect_uris: [chrome.identity.getRedirectURL()],
    token_endpoint_auth_method: "client_secret_basic",
    userinfo_signed_response_alg: "RS256",
    jansInclClaimsInIdTkn: "true"
  };

  console.log("Calling MCP tool with arguments:", enhancedArgs);

  const toolResult = await client.callTool({
    name: "registerOIDCClient",
    arguments: enhancedArgs as ILooseObject,
  });

  await mcpService.saveClientInTarpStorage(toolResult);

  return {
    tool: "registerOIDCClient",
    result: toolResult
  };
}

/**
 * Initiates an OIDC authorization flow for the specified client, completes the login (PKCE + token exchange),
 * persists login details, and returns the authenticated user's information.
 *
 * @param args - Arguments for starting the auth flow; must include `client_id` and may include additional
 *               OIDC start parameters accepted by the MCP `startAuthFlow` tool.
 * @returns A ToolCallResult whose `result` is the user info object on success and whose `error` is an error
 *          message on failure. The `tool` field is `"startAuthFlow"`. When successful, `notifyOnDataChange`
 *          is set to `true`.
 */
async function handleStartAuthFlow(args: any): Promise<ToolCallResult> {
  try {
    const oidcClient = await mcpService.getClientByClientId(args.client_id);
    if (!oidcClient?.clientId) {
      throw new Error(`Client with ID ${args.client_id} not found`);
    }

    const { secret, hashed } = await Utils.generateRandomChallengePair();

    const authArgs = {
      ...args,
      scope: oidcClient.scope,
      response_type: oidcClient.responseType?.[0] || 'code',
      redirect_uri: oidcClient.redirectUris?.[0],
      code_challenge: hashed,
      code_challenge_method: DEFAULT_VALUES.CODE_CHALLENGE_METHOD,
      nonce: uuidv4()
    };

    // Store code_verifier for token exchange
    await StorageHelper.set(STORAGE_KEYS.CODE_VERIFIER, secret);

    const toolResult = await client.callTool({
      name: "startAuthFlow",
      arguments: authArgs,
    }) as ILooseObject;

    const authorizationUrl = toolResult?.structuredContent?.authorization_url;
    if (!authorizationUrl) {
      throw new Error("No authorization URL returned from MCP server");
    }

    const code = await authenticationService.invokeAuthFlow(authorizationUrl);
    if (!code) {
      throw new Error("Authentication failed: authorization code is null or empty");
    }

    const tokenResponse = await authenticationService.getAccessToken(
      code,
      oidcClient,
      secret
    );

    if (!tokenResponse?.access_token) {
      const errorMsg = tokenResponse?.error_description ||
        tokenResponse?.error ||
        "Unknown error";
      throw new Error(`Token exchange failed: ${errorMsg}`);
    }

    const userInfoResponse = await authenticationService.getUserInfo(
      tokenResponse,
      oidcClient
    );

    await saveLoginDetailsInStorage(tokenResponse, userInfoResponse, true);

    return {
      tool: "startAuthFlow",
      result: userInfoResponse,
      notifyOnDataChange: true
    };
  } catch (error) {
    console.error("Error in startAuthFlow:", error);
    return {
      tool: "startAuthFlow",
      error: error instanceof Error ? error.message : "Unknown error occurred"
    };
  }
}

/**
 * Dispatches an array of LLM tool calls to their corresponding handlers and collects results.
 *
 * Processes each entry of `toolCalls` that has type `"function"`, parses its arguments,
 * invokes the matching handler (e.g., `registerOIDCClient`, `startAuthFlow`), and records
 * a `ToolCallResult` for each call. Individual handler errors are caught and returned as
 * per-call error results rather than thrown.
 *
 * @param toolCalls - Array of tool call objects produced by the LLM; expected entries include a `type` of `"function"` and a `function` object with `name` and `arguments`.
 * @returns An array of `ToolCallResult` objects representing success or error for each processed tool call.
 */
async function processToolCalls(toolCalls: any[]): Promise<ToolCallResult[]> {
  const results: ToolCallResult[] = [];

  for (const toolCall of toolCalls) {
    if (toolCall.type !== "function") continue;

    const toolName = toolCall.function.name;
    let args: any;

    try {
      args = typeof toolCall.function.arguments === 'string'
        ? JSON.parse(toolCall.function.arguments)
        : toolCall.function.arguments;

      let result: ToolCallResult;

      switch (toolName) {
        case "registerOIDCClient":
          result = await handleRegisterOIDCClient(args);
          break;
        case "startAuthFlow":
          result = await handleStartAuthFlow(args);
          break;
        case "exchangeToken":
          // Implement exchangeToken handler
          result = {
            tool: toolName,
            error: "exchangeToken not yet implemented"
          };
          break;
        default:
          result = {
            tool: toolName,
            error: `Unknown tool: ${toolName}`
          };
      }

      results.push(result);
    } catch (error) {
      console.error(`Error processing tool call ${toolName}:`, error);
      results.push({
        tool: toolName,
        error: error instanceof Error ? error.message : "Unknown error occurred"
      });
    }
  }

  return results;
}

/**
 * Process a user prompt with the LLM and MCP, invoking OIDC tools when requested and returning the resulting response or tool results.
 *
 * @param prompt - The user's input text describing the desired action or OIDC information (e.g., issuer, client_id, scopes).
 * @returns An object describing the outcome:
 *  - When no tools are invoked: `{ type: "text", content: string }` with a natural-language reply or guidance.
 *  - When tools are invoked: `{ type: "tool_results", results: ToolCallResult[] }` containing the processed tool outcomes.
 *  - On failure: `{ type: "error", content: string }` with an error message.
 */

export async function handleUserPrompt(prompt: string) {
  try {
    // Initialize clients
    llmClient = await initializeLLMClient();
    await initializeMCPClient();

    // Get configuration
    const model = await ConfigurationManager.getModel();

    // Call LLM
    const response = await llmClient.chatCompletion({
      model,
      messages: [
        { role: "system", content: mcpService.createSystemPrompt() },
        { role: "user", content: prompt }
      ],
      tools: mcpService.createOIDCTools(),
      tool_choice: "auto"
    });

    const message = response.choices?.[0]?.message || response.message;
    const toolCalls = response.tool_calls || message?.tool_calls;

    if (!toolCalls || toolCalls.length === 0) {
      return {
        type: "text",
        content: message?.content ||
          "I can only help with OIDC operations. Please provide details like issuer, client_id, or scopes."
      };
    }

    const results = await processToolCalls(toolCalls);

    return {
      type: "tool_results",
      results
    };
  } catch (error) {
    console.error("Error in handleUserPrompt:", error);
    return {
      type: "error",
      content: error instanceof Error ? error.message : "An unexpected error occurred"
    };
  }
}

// ==================== EXPORTED FUNCTIONS ====================
export const updateProvider = ConfigurationManager.updateProvider;
export const updateApiKey = ConfigurationManager.updateApiKey;
export const updateModel = ConfigurationManager.updateModel;