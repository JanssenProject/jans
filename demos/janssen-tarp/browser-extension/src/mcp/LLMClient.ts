export interface LLMClient {
  chatCompletion(params: {
    model?: string;
    messages: Array<{ role: string; content: string }>;
    tools?: Array<any>;
    tool_choice?: string;
  }): Promise<any>;
}

export enum LLMProvider {
  OPENAI = "openai",
  GEMINI = "gemini",
  DEEPSEEK = "deepseek"
}

export class LLMClientFactory {
  static async createClient(provider: LLMProvider): Promise<LLMClient> {
    switch (provider) {
      case LLMProvider.OPENAI: {
        const { OpenAIClient } = await import('./OpenAIClient');
        return new OpenAIClient();
      }
      case LLMProvider.GEMINI: {
        const { GeminiClient } = await import('./GeminiClient');
        return new GeminiClient();
      }
      case LLMProvider.DEEPSEEK: {
        const { DeepSeekClient } = await import('./DeepSeekClient');
        return new DeepSeekClient();
      }
      default:
        throw new Error(`Unsupported LLM provider: ${provider}`);
    }
  }
}