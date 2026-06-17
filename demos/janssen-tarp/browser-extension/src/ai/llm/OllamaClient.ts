import OpenAI from 'openai';
import { LLMClient } from './LLMClient';

export class OllamaClient implements LLMClient {
  private client: OpenAI | null = null;

  async initialize(apiKey: string, baseURL?: string): Promise<void> {
    // Ollama exposes an OpenAI-compatible API. A key is not required for a
    // local server, so fall back to a placeholder when none is provided.
    this.client = new OpenAI({
      apiKey: apiKey || 'ollama',
      baseURL: baseURL || 'http://localhost:11434/v1', // Ollama API endpoint
      dangerouslyAllowBrowser: true
    });
  }

  async chatCompletion(params: {
    model?: string;
    messages: Array<{ role: string; content: string }>;
    tools?: Array<any>;
    tool_choice?: string;
  }): Promise<any> {
    if (!this.client) {
      throw new Error("Ollama client not initialized. Call initialize() first.");
    }

    const model = params.model || 'llama3.1';

    const response = await this.client.chat.completions.create({
      model,
      messages: params.messages as OpenAI.ChatCompletionMessageParam[],
      tools: params.tools as OpenAI.ChatCompletionTool[] | undefined,
      tool_choice: (params.tool_choice ?? 'auto') as OpenAI.ChatCompletionToolChoiceOption,
    });

    return response;
  }
}
