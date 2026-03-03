import OpenAI from 'openai';
import { LLMClient } from './LLMClient';

export class DeepSeekClient implements LLMClient {
  private client: OpenAI | null = null;

  async initialize(apiKey: string): Promise<void> {
    // DeepSeek uses OpenAI-compatible API
    this.client = new OpenAI({
      apiKey,
      baseURL: 'https://api.deepseek.com/v1', // DeepSeek API endpoint
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
      throw new Error("DeepSeek client not initialized. Call initialize() first.");
    }

    const model = params.model || 'deepseek-chat';

    const response = await this.client.chat.completions.create({
      model,
      messages: params.messages as OpenAI.ChatCompletionMessageParam[],
      tools: params.tools as OpenAI.ChatCompletionTool[] | undefined,
      tool_choice: (params.tool_choice ?? 'auto') as OpenAI.ChatCompletionToolChoiceOption,
    });

    return response;
  }
}