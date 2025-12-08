import React from 'react';
import OpenAIIcon from '@mui/icons-material/Cloud';
import GeminiIcon from '@mui/icons-material/SmartToy';
import DeepSeekIcon from '@mui/icons-material/TravelExplore';
import { LLMProvider } from './types';

export const LLM_PROVIDERS: LLMProvider[] = [
  { 
    value: 'openai', 
    label: 'OpenAI', 
    icon: <OpenAIIcon />,
    description: 'GPT models from OpenAI',
    apiKeyFormat: 'sk-...',
    apiKeyPlaceholder: 'sk-...',
    apiKeyValidation: (key: string) => key.startsWith('sk-') && key.length > 40,
    apiKeyValidationMessage: "API key should start with 'sk-' and be at least 40 characters",
    models: [
      { value: 'gpt-4o', label: 'GPT-4o', description: 'Latest and most capable model' },
      { value: 'gpt-4o-mini', label: 'GPT-4o-mini', description: 'Fast and cost-effective GPT-4o variant' },
      { value: 'gpt-4-turbo', label: 'GPT-4 Turbo', description: 'High intelligence with 128K context' },
      { value: 'gpt-4', label: 'GPT-4', description: 'Original GPT-4 model' },
      { value: 'gpt-3.5-turbo', label: 'GPT-3.5 Turbo', description: 'Fast and cost-effective' },
      { value: 'gpt-3.5-turbo-16k', label: 'GPT-3.5 Turbo 16K', description: 'Larger context window' },
    ]
  },
  { 
    value: 'gemini',
    label: 'Google Gemini',
    icon: <GeminiIcon />,
    description: 'Google\'s Gemini models',
    apiKeyFormat: 'AIza...',
    apiKeyPlaceholder: 'AIza...',
    apiKeyValidation: (key: string) => key.startsWith('AIza') && key.length > 30,
    apiKeyValidationMessage: "API key should start with 'AIza' and be at least 30 characters",
    models: [
      { value: 'gemini-1.5-pro', label: 'Gemini 1.5 Pro', description: 'Most capable Gemini model' },
      { value: 'gemini-1.5-flash', label: 'Gemini 1.5 Flash', description: 'Fast and efficient model' },
      { value: 'gemini-pro', label: 'Gemini Pro', description: 'Original Gemini Pro model' },
    ]
  },
  { 
    value: 'deepseek', 
    label: 'DeepSeek', 
    icon: <DeepSeekIcon />,
    description: 'DeepSeek AI models',
    apiKeyFormat: 'Bearer ...',
    apiKeyPlaceholder: 'Enter your DeepSeek API key',
    apiKeyValidation: (key: string) => key.length > 10,
    apiKeyValidationMessage: "API key should be at least 10 characters",
    models: [
      { value: 'deepseek-chat', label: 'DeepSeek Chat', description: 'Main chat model' },
      { value: 'deepseek-coder', label: 'DeepSeek Coder', description: 'Specialized for coding' },
    ]
  }
];

export const PROVIDER_ICONS = Object.fromEntries(
    LLM_PROVIDERS.map(p => [p.value, p.icon])
  ) as Record<string, JSX.Element>;

export const DEFAULT_MODEL = 'gpt-4o-mini';
export const DEFAULT_PROVIDER = 'openai';
export const DEFAULT_MCP_URL = 'http://localhost:3001';

