import {STORAGE_KEYS, LLMProvider, DEFAULT_VALUES} from './Constants'
import StorageHelper from './StorageHelper'

export default class ConfigurationManager {
    static async getProvider(): Promise<LLMProvider> {
      const result = await StorageHelper.get<LLMProvider>(
        STORAGE_KEYS.LLM_PROVIDER, 
        DEFAULT_VALUES.PROVIDER
      );
      return result.data!;
    }
  
    static async getApiKey(): Promise<string> {
      const result = await StorageHelper.get<string>(STORAGE_KEYS.LLM_API_KEY, "");
      return result.data!;
    }
  
    static async getModel(): Promise<string> {
      const result = await StorageHelper.get<string>(STORAGE_KEYS.LLM_MODEL, DEFAULT_VALUES.MODEL);
      return result.data!;
    }
  
    static async getMCPServerUrl(): Promise<string> {
      const result = await StorageHelper.get<string>(STORAGE_KEYS.MCP_SERVER_URL, "");
      return result.data!;
    }
  
    static async getCodeVerifier(): Promise<string> {
      const result = await StorageHelper.get<string>(STORAGE_KEYS.CODE_VERIFIER, "");
      return result.data!;
    }
  
    static async updateProvider(newProvider: LLMProvider, llmClient: any): Promise<void> {
      const result = await StorageHelper.set(STORAGE_KEYS.LLM_PROVIDER, newProvider);
      if (result.success) {
        llmClient = null; // Reset client for reinitialization
      } else {
        throw new Error(`Failed to update provider: ${result.error}`);
      }
    }
  
    static async updateApiKey(newApiKey: string, llmClient: any): Promise<void> {
      const result = await StorageHelper.set(STORAGE_KEYS.LLM_API_KEY, newApiKey);
      if (!result.success) {
        throw new Error(`Failed to update API key: ${result.error}`);
      }
      llmClient = null; // Reset client for reinitialization
    }
  
    static async updateModel(newModel: string): Promise<void> {
      const result = await StorageHelper.set(STORAGE_KEYS.LLM_MODEL, newModel);
      if (!result.success) {
        throw new Error(`Failed to update model: ${result.error}`);
      }
    }
  }