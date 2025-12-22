interface StorageResult<T> {
  success: boolean;
  data?: T;
  error?: string;
}

export default class StorageHelper {
  
    static async get<T>(key: string, defaultValue?: T): Promise<StorageResult<T>> {
      try {
        return new Promise((resolve) => {
          chrome.storage.local.get([key], (result) => {
            if (chrome.runtime.lastError) {
              resolve({ 
                success: false, 
                error: chrome.runtime.lastError.message 
              });
            } else {
              resolve({ 
                success: true, 
                data: result[key] !== undefined ? result[key] : defaultValue 
              });
            }
          });
        });
      } catch (error) {
        return { 
          success: false, 
          error: error instanceof Error ? error.message : 'Unknown storage error' 
        };
      }
    }
  
    static async set(key: string, value: any): Promise<StorageResult<void>> {
      try {
        return new Promise((resolve) => {
          chrome.storage.local.set({ [key]: value }, () => {
            if (chrome.runtime.lastError) {
              resolve({ 
                success: false, 
                error: chrome.runtime.lastError.message 
              });
            } else {
              resolve({ success: true });
            }
          });
        });
      } catch (error) {
        return { 
          success: false, 
          error: error instanceof Error ? error.message : 'Unknown storage error' 
        };
      }
    }
  
    static async remove(key: string): Promise<StorageResult<void>> {
      try {
        return new Promise((resolve) => {
          chrome.storage.local.remove([key], () => {
            if (chrome.runtime.lastError) {
              resolve({ 
                success: false, 
                error: chrome.runtime.lastError.message 
              });
            } else {
              resolve({ success: true });
            }
          });
        });
      } catch (error) {
        return { 
          success: false, 
          error: error instanceof Error ? error.message : 'Unknown storage error' 
        };
      }
    }
  }