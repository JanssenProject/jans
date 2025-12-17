
export interface ApiKey {
    id: string;
    provider: string;
    model: string;
    key: string;
    createdAt: string;
    lastUsed?: string;
  }
  
  export interface DatabaseSchema {
    apiKeys: ApiKey[];
  }
  
  export const defaultData: DatabaseSchema = {
    apiKeys: []
  };