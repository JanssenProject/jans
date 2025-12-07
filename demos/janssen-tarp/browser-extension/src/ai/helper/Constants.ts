// Constants
export const STORAGE_KEYS = {
    LLM_API_KEY: 'llm_api_key',
    LLM_MODEL: 'llm_model',
    LLM_PROVIDER: 'llm_provider',
    MCP_SERVER_URL: 'mcp_server_url',
    CODE_VERIFIER: 'code_verifier',
    LOGIN_DETAILS: 'loginDetails'
  } as const;
  
  export enum LLMProvider {
    OPENAI = "openai",
    GEMINI = "gemini",
    DEEPSEEK = "deepseek"
  }

  export const DEFAULT_VALUES = {
    MODEL: 'gpt-4o-mini',
    PROVIDER: LLMProvider.OPENAI,
    CODE_CHALLENGE_METHOD: 'S256'
  } as const;
  
