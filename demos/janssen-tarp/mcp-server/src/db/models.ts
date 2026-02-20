/**
 * Represents an API key entry in the database.
 * Timestamps should be in ISO 8601 format (e.g., "2025-12-17T10:30:00Z")
*/
export interface ApiKey {
    id: string;
    provider: string;
    model: string;
    key: string;
    /** ISO 8601 timestamp */
    createdAt: string;
    /** ISO 8601 timestamp */
    lastUsed?: string;
  }
  
  export interface DatabaseSchema {
    apiKeys: ApiKey[];
  }
  
  export const defaultData: DatabaseSchema = {
    apiKeys: []
  };