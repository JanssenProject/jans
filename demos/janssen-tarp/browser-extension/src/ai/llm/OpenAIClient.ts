import OpenAI from 'openai';
import { LLMClient } from './LLMClient';

export class OpenAIClient implements LLMClient {
  private client: OpenAI | null = null;

  async initialize(apiKey: string): Promise<void> {
    this.client = new OpenAI({
      apiKey,
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
      throw new Error("OpenAI client not initialized. Call initialize() first.");
    }

    const model = params.model || 'gpt-4o-mini';

    const response = await this.client.chat.completions.create({
      model,
      // Cast to the OpenAI SDK types to satisfy the overload.
      messages: params.messages as OpenAI.ChatCompletionMessageParam[],
      tools: params.tools as OpenAI.ChatCompletionTool[] | undefined,
      tool_choice: (params.tool_choice ?? 'auto') as OpenAI.ChatCompletionToolChoiceOption,
    });

    return response;
  }
}