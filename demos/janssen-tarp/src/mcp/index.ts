import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';
import MCPUtils from './MCPUtils';
import { LLMClientFactory, LLMProvider } from './LLMClient';

const LLM_API_KEY_STORAGE_KEY = 'llm_api_key';
const LLM_MODEL_STORAGE_KEY = 'llm_model';
const LLM_PROVIDER_STORAGE_KEY = 'llm_provider';

// Initialize MCP client
const client = new Client(
  {
    name: "oidc-client",
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
    const transport = new StreamableHTTPClientTransport(new URL("http://localhost:3001/mcp"));
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

    const response = await llmClient.chatCompletion({
      model: model,
      messages: [
        {
          role: "system",
          content: `You are an AI that calls the MCP tool 'registerOIDCClient' whenever user asks for OIDC client creation. Extract the parameters from the user's request.`
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
                redirect_uris: { 
                  type: "array", 
                  items: { type: "string" },
                  description: "Array of redirect URIs"
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
              required: ["issuer", "redirect_uris", "scopes", "response_types"]
            }
          }
        }
      ],
      tool_choice: "auto"
    });

    const message = response.message;
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
      if (toolCall.type === "function" && toolCall.function.name === "registerOIDCClient") {
        try {
          const args = typeof toolCall.function.arguments === 'string' 
            ? JSON.parse(toolCall.function.arguments) 
            : toolCall.function.arguments;
          
          console.log("Calling MCP tool with arguments:", args);
          
          const toolResult = await client.callTool({
            name: toolCall.function.name,
            arguments: args,
          });

          MCPUtils.generateRandomString(toolResult)
          
          results.push({
            tool: toolCall.function.name,
            result: toolResult
          });
          
          console.log("OIDC Client Registration Result:", toolResult);
        } catch (toolError) {
          console.error("Error calling MCP tool:", toolError);
          results.push({
            tool: toolCall.function.name,
            error: toolError instanceof Error ? toolError.message : "Unknown error occurred"
          });
        }
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