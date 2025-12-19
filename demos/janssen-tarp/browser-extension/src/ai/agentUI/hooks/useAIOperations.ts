import { useState, useCallback } from 'react';
import { handleUserPrompt } from "../../index";
import { LLM_MODEL_STORAGE_KEY, LLM_PROVIDER_STORAGE_KEY, MCP_SERVER_URL } from '../types';
import { mcpApiService } from '../../service/MCPAPIService';

export const useAIOperations = (notifyOnDataChange: () => void) => {
  const [query, setQuery] = useState("");
  const [result, setResult] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const send = useCallback(async () => {
    if (!query.trim()) return;

    setLoading(true);
    setError(null);
    setResult(null);
    try {
      // Get settings from chrome storage
      const results = await new Promise((resolve) => {
        chrome.storage.local.get([
          LLM_MODEL_STORAGE_KEY,
          LLM_PROVIDER_STORAGE_KEY,
          MCP_SERVER_URL
        ], (result) => {
          resolve(result);
        });
      });

      const savedModel = results[LLM_MODEL_STORAGE_KEY];
      const savedProvider = results[LLM_PROVIDER_STORAGE_KEY] || 'openai';
      const savedMcpUrl = results[MCP_SERVER_URL];

      if (!savedMcpUrl) {
        throw new Error("Please configure MCP server URL first");
      }

      // Fetch API key from MCP server
      mcpApiService.setBaseUrl(savedMcpUrl);
      const apiKeyData = await mcpApiService.findApiKeyByProvider(savedProvider, savedModel);

      if (!apiKeyData) {
        throw new Error(`Please configure your ${savedProvider} API key first in Settings`);
      }

    } catch (error: any) {
      if (error.message.includes('Failed to fetch')) {
        throw new Error("Cannot connect to MCP server. Please check if the server is running.");
      }
      throw error;
    }

    try {
      // You need to modify handleUserPrompt to get API key from MCP server
      // For now, we'll pass null and let it handle the error
      const result = await handleUserPrompt(query);
      if (result?.type === 'text') {
        setResult(result.content);
      } else if (result?.type === 'tool_results') {
        setResult(result?.results);
        if (result?.results?.[0]?.notifyOnDataChange) {
          notifyOnDataChange();
        }
      } else {
        setResult(result);
      }
      return result;
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "An error occurred";
      if (message.includes('Failed to fetch')) {
        setError("Cannot connect to MCP server. Please check if the server is running.");
      } else {
        setError(message);
      }
      console.error("Error:", err);
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