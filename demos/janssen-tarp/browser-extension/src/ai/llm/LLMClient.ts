import {LLMProviderType} from '../helper/Constants'

export interface LLMClient {
  chatCompletion(params: {
    model?: string;
    messages: Array<{ role: string; content: string }>;
    tools?: Array<any>;
    tool_choice?: string;
  }): Promise<any>;
}

export class LLMClientFactory {
  /**
   * Creates an LLM client for the specified provider.
   * Note: You must call initialize(apiKey) on the returned client before use.
   */
  static async createClient(provider: LLMProviderType): Promise<LLMClient> {
    switch (provider) {
      case LLMProviderType.OPENAI: {
        const { OpenAIClient } = await import('./OpenAIClient');
        return new OpenAIClient();
      }
      case LLMProviderType.GEMINI: {
        const { GeminiClient } = await import('./GeminiClient');
        return new GeminiClient();
      }
      case LLMProviderType.DEEPSEEK: {
        const { DeepSeekClient } = await import('./DeepSeekClient');
        return new DeepSeekClient();
      }
      default:
        throw new Error(`Unsupported LLM provider: ${provider}`);
    }
  }
}