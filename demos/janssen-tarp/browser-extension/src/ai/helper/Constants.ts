// Constants
export const STORAGE_KEYS = {
    LLM_API_KEY: 'llm_api_key',
    LLM_MODEL: 'llm_model',
    LLM_PROVIDER: 'llm_provider',
    OLLAMA_BASE_URL: 'ollama_base_url',
    MCP_SERVER_URL: 'mcp_server_url',
    CODE_VERIFIER: 'code_verifier',
    LOGIN_DETAILS: 'loginDetails'
  } as const;
  
  export enum LLMProviderType {
    OPENAI = "openai",
    CLAUDE = "claude",
    OLLAMA = "ollama"
  }

  // Providers that run locally / require no API key. The user configures only
  // the model (and, for Ollama, the server endpoint).
  const KEYLESS_PROVIDERS: readonly LLMProviderType[] = [LLMProviderType.OLLAMA];

  export function providerRequiresApiKey(provider: string): boolean {
    return !KEYLESS_PROVIDERS.includes(provider as LLMProviderType);
  }

  export const DEFAULT_VALUES = {
    MODEL: 'gpt-4o-mini',
    PROVIDER: LLMProviderType.OPENAI,
    OLLAMA_BASE_URL: 'http://localhost:11434/v1',
    CODE_CHALLENGE_METHOD: 'S256'
  } as const;
  
