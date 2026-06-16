import Anthropic from '@anthropic-ai/sdk';
import { LLMClient } from './LLMClient';

/**
 * Claude (Anthropic) LLM client.
 *
 * Exposes the same `chatCompletion` shape as the other clients (OpenAI-style
 * messages and tools in, a normalized `{ message, tool_calls }` object out) so
 * the rest of the agent code can treat every provider uniformly.
 */
export class ClaudeClient implements LLMClient {
  private client: Anthropic | null = null;

  async initialize(apiKey: string): Promise<void> {
    this.client = new Anthropic({
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
      throw new Error("Claude client not initialized. Call initialize() first.");
    }

    const model = params.model || 'claude-opus-4-8';

    // Anthropic takes the system prompt as a top-level parameter, not as a
    // message. Split it out and map the rest to user/assistant turns.
    const systemPrompt = params.messages
      .filter(m => m.role === 'system')
      .map(m => m.content)
      .join('\n\n');

    const messages = params.messages
      .filter(m => m.role !== 'system')
      .map(m => ({
        role: m.role === 'assistant' ? 'assistant' : 'user',
        content: m.content
      })) as Anthropic.MessageParam[];

    // Convert OpenAI-style tools to Anthropic tool definitions.
    const tools = params.tools?.map(tool => ({
      name: tool.function.name,
      description: tool.function.description,
      input_schema: tool.function.parameters
    })) as Anthropic.Tool[] | undefined;

    const response = await this.client.messages.create({
      model,
      max_tokens: 16000,
      system: systemPrompt || undefined,
      messages,
      tools,
      tool_choice: tools && tools.length > 0 ? { type: 'auto' } : undefined
    });

    // Normalize the Anthropic response to the OpenAI-style shape the agent
    // expects: a `tool_calls` array with `function.name` / `function.arguments`.
    const textContent = response.content
      .filter((block): block is Anthropic.TextBlock => block.type === 'text')
      .map(block => block.text)
      .join('');

    const toolCalls = response.content
      .filter((block): block is Anthropic.ToolUseBlock => block.type === 'tool_use')
      .map(block => ({
        id: block.id,
        type: 'function',
        function: {
          name: block.name,
          arguments: JSON.stringify(block.input)
        }
      }));

    return {
      message: {
        role: 'assistant',
        content: textContent,
        tool_calls: toolCalls
      },
      tool_calls: toolCalls
    };
  }
}
