import { useState, useCallback } from 'react';
import { handleUserPrompt } from "../../index";
import { LLM_API_KEY_STORAGE_KEY, LLM_PROVIDER_STORAGE_KEY, MCP_SERVER_URL } from '../types';

export const useAIOperations = (notifyOnDataChange: () => void) => {
  const [query, setQuery] = useState("");
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const send = useCallback(async () => {
    if (!query.trim()) return;
    
    // Check if API key is configured
    const results = await new Promise((resolve) => {
      chrome.storage.local.get([
        LLM_API_KEY_STORAGE_KEY,
        LLM_PROVIDER_STORAGE_KEY,
        MCP_SERVER_URL
      ], (result) => {
        resolve(result);
      });
    });
    
    const savedApiKey = results[LLM_API_KEY_STORAGE_KEY];
    const savedProvider = results[LLM_PROVIDER_STORAGE_KEY] || 'openai';
    const savedMcpUrl = results[MCP_SERVER_URL];
    
    if (!savedApiKey) {
      throw new Error("Please configure your API key first");
    }

    if (!savedMcpUrl) {
      throw new Error("Please configure MCP server URL first");
    }
    
    setLoading(true);
    setError(null);
    setResult(null);
    
    try {
      const result = await handleUserPrompt(query);
      if(result?.type === 'text') {
        setResult(result.content);  
      } else if(result?.type === 'tool_results') {
        setResult(result?.results);  
        if(result?.results?.[0]?.notifyOnDataChange) {
          notifyOnDataChange();
        }
      } else {
        setResult(result);
      }
      return result;
    } catch (err: any) {
      console.error("Error:", err);
      setError(err.message || "An error occurred while processing your request");
      throw err;
    } finally {
      setLoading(false);
    }
  }, [query, notifyOnDataChange]);

  const handleKeyPress = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      send();
    }
  }, [send]);

  const resetQuery = useCallback(() => {
    setQuery("");
    setResult(null);
    setError(null);
  }, []);

  return {
    query,
    setQuery,
    result,
    loading,
    error,
    send,
    handleKeyPress,
    resetQuery
  };
};