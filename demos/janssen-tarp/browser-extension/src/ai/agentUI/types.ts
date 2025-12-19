import type { ReactNode } from 'react';

export interface LLMProvider {
    value: string;
    label: string;
    icon: ReactNode;
    description: string;
    apiKeyFormat: string;
    apiKeyPlaceholder: string;
    apiKeyValidation: (key: string) => boolean;
    apiKeyValidationMessage: string;
    models: Model[];
  }
  
  export interface Model {
    value: string;
    label: string;
    description: string;
  }
  
  export interface AIAgentProps {
    notifyOnDataChange: () => void;
  }
  
  export type ConnectionStatus = "disconnected" | "connecting" | "connected";
  export type AlertSeverity = "success" | "error" | "warning" | "info";
  
  export interface SnackbarState {
    open: boolean;
    message: string;
    severity: AlertSeverity;
  }
  
  // Storage keys
  export const LLM_MODEL_STORAGE_KEY = 'llm_model';
  export const LLM_PROVIDER_STORAGE_KEY = 'llm_provider';
  export const MCP_SERVER_URL = 'mcp_server_url';
  export const MCP_API_BASE_URL = 'http://localhost:3001'; // or your MCP server URL
  export const MCP_KEYS_ENDPOINT = '/api/keys';