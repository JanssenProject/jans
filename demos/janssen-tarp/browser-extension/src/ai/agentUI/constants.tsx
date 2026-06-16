import React from "react";
import {
  Cloud,
  Bot,
  Compass,
} from "lucide-react";

import {
  LLMProvider,
  ConnectionStatus,
  AlertSeverity,
} from "./types";

export const LLM_PROVIDERS: LLMProvider[] = [
  {
    value: "openai",
    label: "OpenAI",
    icon: <Cloud className="h-5 w-5 text-blue-500" />,
    description: "GPT models from OpenAI",
    apiKeyFormat: "sk-...",
    apiKeyPlaceholder: "sk-...",
    apiKeyValidation: (key: string) =>
      key.startsWith("sk-") && key.length > 40,
    apiKeyValidationMessage:
      "API key should start with 'sk-' and be at least 40 characters",
    models: [
      {
        value: "gpt-4o-mini",
        label: "GPT-4o-mini",
        description:
          "Fast and cost-effective GPT-4o variant",
      },
      {
        value: "gpt-4o",
        label: "GPT-4o",
        description:
          "Latest and most capable model",
      },
      {
        value: "gpt-4-turbo",
        label: "GPT-4 Turbo",
        description:
          "High intelligence with 128K context",
      },
      {
        value: "gpt-4",
        label: "GPT-4",
        description:
          "Original GPT-4 model",
      },
      {
        value: "gpt-3.5-turbo",
        label: "GPT-3.5 Turbo",
        description:
          "Fast and cost-effective",
      },
      {
        value: "gpt-3.5-turbo-16k",
        label: "GPT-3.5 Turbo 16K",
        description:
          "Larger context window",
      },
    ],
  },

  {
    value: "claude",
    label: "Anthropic Claude",
    icon: <Bot className="h-5 w-5 text-orange-500" />,
    description: "Claude models from Anthropic",
    apiKeyFormat: "sk-ant-...",
    apiKeyPlaceholder: "sk-ant-...",
    apiKeyValidation: (key: string) =>
      key.startsWith("sk-ant-") && key.length > 40,
    apiKeyValidationMessage:
      "API key should start with 'sk-ant-' and be at least 40 characters",
    models: [
      {
        value: "claude-opus-4-8",
        label: "Claude Opus 4.8",
        description:
          "Most capable Opus model",
      },
      {
        value: "claude-opus-4-7",
        label: "Claude Opus 4.7",
        description:
          "Previous-generation Opus; highly autonomous",
      },
      {
        value: "claude-opus-4-6",
        label: "Claude Opus 4.6",
        description:
          "Older Opus with adaptive thinking",
      },
      {
        value: "claude-opus-4-5",
        label: "Claude Opus 4.5",
        description:
          "Legacy Opus model",
      },
      {
        value: "claude-sonnet-4-6",
        label: "Claude Sonnet 4.6",
        description:
          "Best balance of speed and intelligence",
      },
      {
        value: "claude-sonnet-4-5",
        label: "Claude Sonnet 4.5",
        description:
          "Legacy Sonnet model",
      },
      {
        value: "claude-haiku-4-5",
        label: "Claude Haiku 4.5",
        description:
          "Fastest and most cost-effective",
      },
    ],
  },

  {
    value: "ollama",
    label: "Ollama",
    icon: <Compass className="h-5 w-5 text-green-500" />,
    description: "Local models via Ollama (no API key)",
    requiresApiKey: false,
    hasBaseUrl: true,
    apiKeyFormat: "Not required",
    apiKeyPlaceholder: "Not required for Ollama",
    apiKeyValidation: () => true,
    apiKeyValidationMessage: "",
    models: [
      {
        value: "llama3.1",
        label: "Llama 3.1",
        description: "Meta's Llama 3.1 model",
      },
      {
        value: "llama3.2",
        label: "Llama 3.2",
        description: "Meta's Llama 3.2 model",
      },
      {
        value: "qwen2.5",
        label: "Qwen 2.5",
        description: "Alibaba's Qwen 2.5 model",
      },
      {
        value: "mistral",
        label: "Mistral",
        description: "Mistral 7B model",
      },
    ],
  },
];

export const PROVIDER_ICONS = Object.fromEntries(
  LLM_PROVIDERS.map((p) => [p.value, p.icon])
) as Record<string, React.ReactElement>;

export const DEFAULT_MODEL = "gpt-4o-mini";
export const DEFAULT_PROVIDER = "openai";
export const DEFAULT_MCP_URL =
  "http://localhost:3001";

export function getProviderColor(
  provider: string
): string {
  switch (provider) {
    case "openai":
      return "#3b82f6"; // blue-500
    case "claude":
      return "#f59e0b"; // amber-500
    case "ollama":
      return "#22c55e"; // green-500
    default:
      return "#9ca3af"; // gray-400
  }
}

export function getConnectionStatusSeverity(
  status: ConnectionStatus
): AlertSeverity {
  switch (status) {
    case "connected":
      return "success";
    case "connecting":
      return "warning";
    case "disconnected":
      return "error";
    default:
      return "info";
  }
}

export function getConnectionStatusText(
  status: ConnectionStatus
): string {
  switch (status) {
    case "connected":
      return "Connected";
    case "connecting":
      return "Connecting...";
    case "disconnected":
      return "Disconnected";
    default:
      return "Unknown";
  }
}