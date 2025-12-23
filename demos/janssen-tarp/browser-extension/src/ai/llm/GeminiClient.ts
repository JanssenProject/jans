import { GoogleGenerativeAI, Content } from "@google/generative-ai";
import { LLMClient } from './LLMClient';

export class GeminiClient implements LLMClient {
  private genAI: GoogleGenerativeAI | null = null;
  private modelName: string = 'gemini-1.5-pro';

  async initialize(apiKey: string, modelName?: string): Promise<void> {
    this.genAI = new GoogleGenerativeAI(apiKey);
    if (modelName) {
      this.modelName = modelName;
    }
  }

  async chatCompletion(params: {
    model?: string;
    messages: Array<{ role: string; content: string }>;
    tools?: Array<any>;
    tool_choice?: string;
  }): Promise<any> {
    if (!this.genAI) {
      throw new Error("Gemini client not initialized. Call initialize() first.");
    }

    const modelName = params.model || this.modelName;
    const model = this.genAI.getGenerativeModel({
      model: modelName,
      generationConfig: {
        temperature: 0.1,
      }
    });

    // Convert messages format for Gemini
    const geminiMessages = this.convertMessagesToGeminiFormat(params.messages);

    // Prepare function declarations if tools are provided
    const functionDeclarations = params.tools ? params.tools.map(tool => ({
      name: tool.function.name,
      description: tool.function.description,
      parameters: tool.function.parameters
    })) : undefined;

    // Start a chat session
    const chat = model.startChat({
      history: geminiMessages.slice(0, -1), // All except the last message
      generationConfig: {
        temperature: 0.1,
      },
      tools: functionDeclarations ? [{ functionDeclarations }] : undefined,
    });

    if (geminiMessages.length === 0) {
      throw new Error("No messages to send to Gemini");
    }

    const lastMessage = geminiMessages[geminiMessages.length - 1];

    if (!lastMessage.parts?.[0]?.text) {
      throw new Error("Last message has no text content");
    }

    const result = await chat.sendMessage(lastMessage.parts[0].text);
    const response = result.response;

    // Extract function calls from response
    const toolCalls = this.extractFunctionCallsFromResponse(response);

    return {
      message: {
        role: 'assistant',
        content: response.text(),
        tool_calls: toolCalls
      },
      tool_calls: toolCalls
    };
  }

  private convertMessagesToGeminiFormat(messages: Array<{ role: string; content: string }>): Content[] {
    const geminiContents: Content[] = [];

    for (const message of messages) {
      let role: 'user' | 'model';

      switch (message.role) {
        case 'user':
        case 'system':
          role = 'user';
          break;
        case 'assistant':
          role = 'model';
          break;
        default:
          role = 'user';
      }

      // Combine system messages with the first user message
      if (message.role === 'system') {
        if (geminiContents.length === 0) {
          geminiContents.push({
            role: 'user',
            parts: [{ text: message.content }]
          });
        } else if (geminiContents[0].role === 'user') {
          geminiContents[0].parts[0].text = message.content + '\n\n' + geminiContents[0].parts[0].text;
        }
      } else {
        geminiContents.push({
          role: role,
          parts: [{ text: message.content }]
        });
      }
    }

    return geminiContents;
  }

  private extractFunctionCallsFromResponse(response: any): any[] {
    const toolCalls: any[] = [];

    // Check for function calls in the response
    if (response.functionCall) {
      toolCalls.push({
        type: "function",
        function: {
          name: response.functionCall.name,
          arguments: JSON.stringify(response.functionCall.args)
        }
      });
    }

    // Also check for function calls in candidate content
    if (response.candidates && response.candidates[0]?.content?.parts) {
      for (const part of response.candidates[0].content.parts) {
        if (part.functionCall) {
          toolCalls.push({
            type: "function",
            function: {
              name: part.functionCall.name,
              arguments: JSON.stringify(part.functionCall.args)
            }
          });
        }
      }
    }

    return toolCalls;
  }
}