import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';
import MCPService from './service/MCPService';
import { LLMClientFactory } from './llm/LLMClient';
import { LLMProviderType } from './helper/Constants'
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

// ==================== SERVICE FUNCTIONS ====================
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

async function initializeLLMClient(): Promise<any> {
  if (llmClient) return llmClient;

  try {
    const provider = await ConfigurationManager.getProvider();
    const apiKey = await ConfigurationManager.getApiKey();

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

async function handleStartAuthFlow(args: any): Promise<ToolCallResult> {
  try {
    const oidcClient = await mcpService.getClientByClientId(args.client_id);
    if (!oidcClient) {
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

// ==================== MAIN FUNCTION ====================

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
        content: message.content || 
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