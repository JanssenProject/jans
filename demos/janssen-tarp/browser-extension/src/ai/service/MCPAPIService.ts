// services/mcpApi.ts
import { MCP_API_BASE_URL, MCP_KEYS_ENDPOINT } from '../agentUI/types';

export interface ApiKeyData {
  id?: string;
  provider: string;
  model: string;
  key: string;
  createdAt?: string;
  lastUsed?: string;
}

class MCPApiService {
  private baseUrl: string;
  private currentApiKeyId: string | null = null;

  constructor(baseUrl: string = MCP_API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  async setBaseUrl(url: string) {
    this.baseUrl = url;
  }

  setCurrentApiKeyId(id: string | null) {
    this.currentApiKeyId = id;
  }

  getCurrentApiKeyId(): string | null {
    return this.currentApiKeyId;
  }

  // Get all API keys (without full key values)
  async getApiKeys(): Promise<{ count: number; keys: Array<Omit<ApiKeyData, 'key'>> }> {
    const response = await fetch(`${this.baseUrl}${MCP_KEYS_ENDPOINT}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch API keys: ${response.statusText}`);
    }
    return response.json();
  }

  // Get a specific API key by ID (returns full key)
  async getApiKey(id: string): Promise<ApiKeyData> {
    const response = await fetch(`${this.baseUrl}${MCP_KEYS_ENDPOINT}/${id}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch API key: ${response.statusText}`);
    }
    return response.json();
  }

  // Create/Store a new API key
  async createApiKey(provider: string, model: string, key: string): Promise<ApiKeyData> {
    const response = await fetch(`${this.baseUrl}${MCP_KEYS_ENDPOINT}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ provider, model, key }),
    });

    if (!response.ok) {
      if (response.status === 409) {
        throw new Error('API key already exists for this provider');
      }
      throw new Error(`Failed to create API key: ${response.statusText}`);
    }

    return response.json();
  }

  // Update an existing API key
  async updateApiKey(id: string, key: string): Promise<ApiKeyData> {
    // For simplicity, we'll delete and recreate
    const provider = await this.getProviderFromId(id);
    const model = await this.getModelFromId(id);
    await this.deleteApiKey(id);
    return this.createApiKey(provider || 'unknown', model || 'unknown', key);
  }

  // Delete an API key by ID
  async deleteApiKey(id: string): Promise<{ success: boolean; message: string }> {
    const response = await fetch(`${this.baseUrl}${MCP_KEYS_ENDPOINT}/${id}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('API key not found');
      }
      throw new Error(`Failed to delete API key: ${response.statusText}`);
    }

    if (this.currentApiKeyId === id) {
      this.currentApiKeyId = null;
    }

    return response.json();
  }

  // Delete all API keys for a provider
  async deleteApiKeysByProvider(provider: string): Promise<{ success: boolean; message: string; deletedCount: number }> {
    const response = await fetch(`${this.baseUrl}${MCP_KEYS_ENDPOINT}/provider/${provider}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('No API keys found for this provider');
      }
      throw new Error(`Failed to delete API keys: ${response.statusText}`);
    }

    this.currentApiKeyId = null;

    return response.json();
  }

  // Helper to find API key for a specific provider
  async findApiKeyByProvider(provider: string, model: string): Promise<ApiKeyData | null> {
    try {
      const { keys } = await this.getApiKeys();
      // Dynamic query based on what's provided
      let keyPreview;

      if (provider && model) {
        // Query by both provider AND model
        keyPreview = keys.find(k => (k.provider === provider && k.model === model));
      } else if (provider) {
        // Query only by provider (model not provided)
        keyPreview = keys.find(k => (k.provider === provider));
      } else {
        // Query only by model (provider not provided)
        keyPreview = keys.find(k => (k.model === model));
      }

      //const keyPreview = keys.find(key => (key.provider === provider && key.model === model));

      if (!keyPreview?.id) return null;

      // Fetch the full key using the ID
      return await this.getApiKey(keyPreview.id);
    } catch (error) {
      console.error("Failed to find API key:", error);
      return null;
    }
  }

  // Helper to get provider from key ID (you might need to adjust this based on your data structure)
  async getProviderFromId(id: string): Promise<string> {
    // This would require fetching the key first
    const { provider } = await this.getApiKey(id);
    return provider;
  }

  async getModelFromId(id: string): Promise<string> {
    // This would require fetching the key first
    const { model } = await this.getApiKey(id);
    return model;
  }
}

export const mcpApiService = new MCPApiService();