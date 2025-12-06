import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';
import MCPService from './MCPService';
import { LLMClientFactory, LLMProvider } from './LLMClient';
import { v4 as uuidv4 } from 'uuid';
import Utils from '../options/Utils';
import AuthenticationService from '../service/authenticationService';
import { ILooseObject } from "../options/ILooseObject";

const LLM_API_KEY_STORAGE_KEY = 'llm_api_key';
const LLM_MODEL_STORAGE_KEY = 'llm_model';
const LLM_PROVIDER_STORAGE_KEY = 'llm_provider';
const MCP_SERVER_URL = 'mcp_server_url';
const mcpService = new MCPService();
const authenticationService = new AuthenticationService();
// Initialize MCP client
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

// Track connection state
let isConnected = false;
let llmClient: any = null;

async function saveLoginDetailsInStorage(tokenResponse, userInfoResponse, displayToken) {
  new Promise((resolve, reject) => {
    chrome.storage.local.set({
      loginDetails: {
        'access_token': tokenResponse.data.access_token,
        'userDetails': userInfoResponse.data,
        'id_token': tokenResponse.data.id_token,
        'displayToken': displayToken,
      }
    });
  });
}


// Function to get provider from Chrome storage
async function getCodeVerifierFromStorage(): Promise<LLMProvider> {
  return new Promise((resolve, reject) => {
    chrome.storage.local.get(["code_verifier"], (result) => {
      if (chrome.runtime.lastError) {
        reject(chrome.runtime.lastError);
      } else {
        resolve(result["code_verifier"]);
      }
    });
  });
}

// Function to get provider from Chrome storage
async function getProviderFromStorage(): Promise<LLMProvider> {
  return new Promise((resolve, reject) => {
    chrome.storage.local.get([LLM_PROVIDER_STORAGE_KEY], (result) => {
      if (chrome.runtime.lastError) {
        reject(chrome.runtime.lastError);
      } else {
        resolve((result[LLM_PROVIDER_STORAGE_KEY] as LLMProvider) || LLMProvider.OPENAI);
      }
    });
  });
}

// Function to get API key from Chrome storage
async function getApiKeyFromStorage(): Promise<string> {
  return new Promise((resolve, reject) => {
    chrome.storage.local.get([LLM_API_KEY_STORAGE_KEY], (result) => {
      if (chrome.runtime.lastError) {
        reject(chrome.runtime.lastError);
      } else {
        resolve(result[LLM_API_KEY_STORAGE_KEY] || "");
      }
    });
  });
}

// Function to get model from Chrome storage
async function getModelFromStorage(): Promise<string> {
  return new Promise((resolve, reject) => {
    chrome.storage.local.get([LLM_MODEL_STORAGE_KEY], (result) => {
      if (chrome.runtime.lastError) {
        reject(chrome.runtime.lastError);
      } else {
        resolve(result[LLM_MODEL_STORAGE_KEY] || "gpt-4o-mini");
      }
    });
  });
}

// Function to get API key from Chrome storage
async function getMCPServerUrlFromStorage(): Promise<string> {
  return new Promise((resolve, reject) => {
    chrome.storage.local.get([MCP_SERVER_URL], (result) => {
      if (chrome.runtime.lastError) {
        reject(chrome.runtime.lastError);
      } else {
        resolve(result[MCP_SERVER_URL] || "");
      }
    });
  });
}

// Initialize LLM client based on provider
async function initializeLLMClient(): Promise<any> {
  try {
    const provider = await getProviderFromStorage();
    const apiKey = await getApiKeyFromStorage();

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

// Connect to MCP server
async function initializeMCPClient() {
  if (isConnected) {
    return;
  }
  try {
    const mcpServerUrl = await getMCPServerUrlFromStorage();
    const transport = new StreamableHTTPClientTransport(new URL(`${mcpServerUrl}/mcp`));
    await client.connect(transport);
    isConnected = true;
    console.log("MCP client connected successfully");
  } catch (error) {
    console.error("Failed to connect to MCP server:", error);
    throw error;
  }
}

export async function handleUserPrompt(prompt: string) {
  try {
    // Ensure LLM client is initialized with stored API key
    if (!llmClient) {
      llmClient = await initializeLLMClient();
    }

    // Get model from storage
    const model = await getModelFromStorage();

    // Ensure MCP client is connected
    await initializeMCPClient();
    //PKCE pairs
    const { secret, hashed } = await Utils.generateRandomChallengePair();

    const response = await llmClient.chatCompletion({
      model: model,
      messages: [
        {
          role: "system",
          content: `
          You are an assistant that uses MCP tools to perform OIDC operations.

          When user asks:
          - "register OIDC client" → call registerOIDCClient
          - "login", "authenticate", "start auth" → call startAuthFlow
          - "token", "exchange", "get access token", "userinfo" → call exchangeToken

          RULES:
          - Only extract parameters the user explicitly provides.
          - Do NOT guess.
          - Do NOT invent defaults.
          - Do NOT fill redirect_uri or PKCE params.
          These will be provided by the MCP client automatically.`
        },
        { role: "user", content: prompt }
      ],
      tools: [
        {
          type: "function",
          function: {
            name: "registerOIDCClient",
            description: "Register an OIDC client using MCP server",
            parameters: {
              type: "object",
              properties: {
                issuer: {
                  type: "string",
                  description: "The OIDC issuer URL (e.g., https://op-host.gluu.org)"
                },
                scopes: {
                  type: "array",
                  items: { type: "string" },
                  description: "Array of OIDC scopes (e.g., openid, profile)"
                },
                response_types: {
                  type: "array",
                  items: { type: "string" },
                  description: "Array of response types (e.g., code)"
                }
              },
              required: ["issuer", "scopes", "response_types"]
            }
          }
        },
        {
          type: "function",
          function: {
            name: "startAuthFlow",
            description: "Start OIDC Authorization Code Flow (generate URL)",
            parameters: {
              type: "object",
              properties: {
                issuer: { type: "string", nullable: true },
                client_id: { type: "string" },
              },
              required: ["issuer", "client_id"]
            }
          }
        },
        {
          type: "function",
          function: {
            name: "exchangeToken",
            description: "Exchange authorization code for tokens",
            parameters: {
              type: "object",
              properties: {
                issuer: { type: "string" },
                client_id: { type: "string" },
                client_secret: { type: "string", nullable: true },
                code_verifier: { type: "string" },
                code: { type: "string" },
                redirect_uri: { type: "string" }
              },
              required: ["issuer", "client_id", "code_verifier", "code", "redirect_uri"]
            }
          }
        }
      ],
      tool_choice: "auto"
    });

    const message = response.choices[0].message;
    const toolCalls = response.tool_calls || message.tool_calls;

    if (!toolCalls || toolCalls.length === 0) {
      console.log("LLM did not call the OIDC registration tool");
      return {
        type: "text",
        content: message.content || response.content || "I can only help with OIDC client registration. Please provide details like issuer, redirect URIs, scopes, and response types."
      };
    }

    const results = [];

    for (const toolCall of toolCalls) {
      if (toolCall.type !== "function") continue;
      const toolName = toolCall.function.name;
      try {
        const args = typeof toolCall.function.arguments === 'string'
          ? JSON.parse(toolCall.function.arguments)
          : toolCall.function.arguments;

        if (toolName === "registerOIDCClient") {
          //adding other request params
          args.redirect_uris = [chrome.identity.getRedirectURL()];
          args.token_endpoint_auth_method = "client_secret_basic";
          args.userinfo_signed_response_alg = "RS256";
          args.jansInclClaimsInIdTkn = "true";

          console.log("Calling MCP tool with arguments:", args);

          const toolResult = await client.callTool({
            name: toolCall.function.name,
            arguments: args,
          });

          mcpService.saveClientInTarpStorage(toolResult)

          results.push({
            tool: toolCall.function.name,
            result: toolResult
          });

          console.log("OIDC Client Registration Result:", toolResult);
        } else if (toolName === "startAuthFlow") {
          const oidcClient = await mcpService.getClientByClientId(args.client_id)
          //adding other request params
          args.scope = oidcClient.scope;
          args.response_type = oidcClient.responseType[0];
          args.redirect_uri = oidcClient.redirectUris[0];
          args.code_challenge = hashed;
          args.code_challenge_method = 'S256';
          args.nonce = uuidv4();

          // Store code_verifier for token exchange
          chrome.storage.local.set({ code_verifier: secret });

          const toolResult = await client.callTool({
            name: toolCall.function.name,
            arguments: args,
          }) as ILooseObject;

          const authorizationUrl = toolResult?.structuredContent?.authorization_url;
          const code = await authenticationService.invokeAuthFlow(authorizationUrl);

          if (code == null || code === '') {
            console.log('Error in authentication. The authorization-code is null.');
            results.push({
              tool: toolCall.function.name,
              error: 'Error in authentication. The authorization-code is null.'
            });
          }
          console.log('getCodeVerifierFromStorage:' + await getCodeVerifierFromStorage())
          console.log('secret:' + secret)
          const tokenResponse = await authenticationService.getAccessToken(code, oidcClient, secret);
          if (!(tokenResponse &&
            tokenResponse.data &&
            tokenResponse.data.access_token)) {
            console.log(`Error in authentication. Token response does not contain access_token. ${tokenResponse?.data?.error_description || tokenResponse?.data?.error ||
              ''
              }`);
            results.push({
              tool: toolCall.function.name,
              error: `Error in authentication. Token response does not contain access_token. ${tokenResponse?.data?.error_description || tokenResponse?.data?.error ||
                ''
                }`
            });
          }
          const userInfoResponse = await authenticationService.getUserInfo(tokenResponse, oidcClient);

          //saveLoginDetails
          await saveLoginDetailsInStorage(tokenResponse, userInfoResponse, true);

          results.push({
            tool: toolCall.function.name,
            result: userInfoResponse,
            notifyOnDataChange: true
          });

        } else if (toolName === "exchangeToken") {

        }
      } catch (toolError) {
        console.error("Error calling MCP tool:", toolError);
        results.push({
          tool: toolCall.function.name,
          error: toolError instanceof Error ? toolError.message : "Unknown error occurred"
        });
      }
    }

    return {
      type: "tool_results",
      results: results
    };

  } catch (error) {
    console.error("Error in handleUserPrompt:", error);
    return {
      type: "error",
      content: error instanceof Error ? error.message : "An unexpected error occurred"
    };
  }
}

// Utility functions for updating settings
export async function updateProvider(newProvider: LLMProvider): Promise<void> {
  return new Promise((resolve, reject) => {
    chrome.storage.local.set({ [LLM_PROVIDER_STORAGE_KEY]: newProvider }, () => {
      if (chrome.runtime.lastError) {
        reject(chrome.runtime.lastError);
      } else {
        // Reset LLM client so it will be reinitialized with new provider
        llmClient = null;
        resolve();
      }
    });
  });
}

export async function updateApiKey(newApiKey: string): Promise<void> {
  return new Promise((resolve, reject) => {
    chrome.storage.local.set({ [LLM_API_KEY_STORAGE_KEY]: newApiKey }, () => {
      if (chrome.runtime.lastError) {
        reject(chrome.runtime.lastError);
      } else {
        // Reset LLM client so it will be reinitialized with new key
        llmClient = null;
        resolve();
      }
    });
  });
}

export async function updateModel(newModel: string): Promise<void> {
  return new Promise((resolve, reject) => {
    chrome.storage.local.set({ [LLM_MODEL_STORAGE_KEY]: newModel }, () => {
      if (chrome.runtime.lastError) {
        reject(chrome.runtime.lastError);
      } else {
        resolve();
      }
    });
  });
}