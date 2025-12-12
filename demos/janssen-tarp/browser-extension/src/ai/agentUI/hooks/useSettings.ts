import { useState, useEffect, useCallback } from 'react';
import {
  LLM_API_KEY_STORAGE_KEY,
  LLM_MODEL_STORAGE_KEY,
  LLM_PROVIDER_STORAGE_KEY,
  MCP_SERVER_URL,
  ConnectionStatus,
  SnackbarState
} from '../types';
import { LLM_PROVIDERS, DEFAULT_MODEL, DEFAULT_PROVIDER, DEFAULT_MCP_URL } from '../constants';

interface StorageResult {
  [LLM_API_KEY_STORAGE_KEY]?: string;
  [LLM_MODEL_STORAGE_KEY]?: string;
  [LLM_PROVIDER_STORAGE_KEY]?: string;
  [MCP_SERVER_URL]?: string;
}

export const useSettings = () => {
  const [apiKey, setApiKey] = useState("");
  const [showApiKey, setShowApiKey] = useState(false);
  const [apiKeyError, setApiKeyError] = useState("");
  const [apiKeyValid, setApiKeyValid] = useState(false);
  const [provider, setProvider] = useState(DEFAULT_PROVIDER);
  const [model, setModel] = useState(DEFAULT_MODEL);
  const [mcpServerUrl, setMcpServerUrl] = useState(DEFAULT_MCP_URL);
  const [customModel, setCustomModel] = useState("");
  const [modelError, setModelError] = useState("");
  const [mcpUrlError, setMcpUrlError] = useState("");
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>("disconnected");
  const [snackbar, setSnackbar] = useState<SnackbarState>({
    open: false,
    message: "",
    severity: "success"
  });

  // Load settings from storage on component mount
  useEffect(() => {
    const initialize = async () => {
      const results = await new Promise<StorageResult>((resolve) => {
        chrome.storage.local.get([
          LLM_API_KEY_STORAGE_KEY,
          LLM_MODEL_STORAGE_KEY,
          LLM_PROVIDER_STORAGE_KEY,
          MCP_SERVER_URL
        ], (result) => {
          resolve(result);
        });
      });

      const savedApiKey = results[LLM_API_KEY_STORAGE_KEY];
      if (savedApiKey) {
        setApiKey(savedApiKey);
        validateApiKey(savedApiKey, results[LLM_PROVIDER_STORAGE_KEY] || provider);
      }

      const savedModel = results[LLM_MODEL_STORAGE_KEY];
      if (savedModel) {
        setModel(savedModel);
      }

      const savedProvider = results[LLM_PROVIDER_STORAGE_KEY];
      if (savedProvider) {
        setProvider(savedProvider);
      }

      const savedMcpUrl = results[MCP_SERVER_URL];
      if (savedMcpUrl) {
        setMcpServerUrl(savedMcpUrl);
        validateMcpUrl(savedMcpUrl);
      }

      // Test MCP connection on load
      if (savedMcpUrl) {
        testMCPConnection(savedMcpUrl);
      }
    };
    initialize();
  }, []);

  // Validate API key based on provider
  const validateApiKey = useCallback((key: string, currentProvider: string = provider) => {
    const providerConfig = LLM_PROVIDERS.find(p => p.value === currentProvider);
    if (!providerConfig) return false;

    const isValid = providerConfig.apiKeyValidation(key);
    setApiKeyValid(isValid);

    if (!isValid && key) {
      setApiKeyError(providerConfig.apiKeyValidationMessage);
    } else {
      setApiKeyError("");
    }

    return isValid;
  }, [provider]);

  // Validate MCP Server URL
  const validateMcpUrl = useCallback((url: string) => {
    try {
      const urlObj = new URL(url);
      const isValid = urlObj.protocol === 'http:' || urlObj.protocol === 'https:';
      setMcpUrlError(isValid ? "" : "URL must use http:// or https:// protocol");
      return isValid;
    } catch (e) {
      setMcpUrlError("Please enter a valid URL (e.g., http://localhost:3001)");
      return false;
    }
  }, []);

  // Test MCP connection
  const testMCPConnection = useCallback(async (url: string) => {
    setConnectionStatus("connecting");
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);
      const response = await fetch(url + '/', {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (response.ok) {
        setConnectionStatus("connected");
        return true;
      } else {
        setConnectionStatus("disconnected");
        return false;
      }
    } catch (error) {
      setConnectionStatus("disconnected");
      return false;
    }
  }, []);

  const handleApiKeyChange = useCallback((newKey: string) => {
    setApiKey(newKey);
    validateApiKey(newKey);
  }, [validateApiKey]);

  const handleProviderChange = useCallback((newProvider: string) => {
    setProvider(newProvider);
    // Reset model to default for new provider
    const providerConfig = LLM_PROVIDERS.find(p => p.value === newProvider);
    if (providerConfig && providerConfig.models.length > 0) {
      setModel(providerConfig.models[0].value);
    }
    // Re-validate API key with new provider
    validateApiKey(apiKey, newProvider);
  }, [apiKey, validateApiKey]);

  const handleModelChange = useCallback((newModel: string) => {
    setModel(newModel);
    setModelError("");

    if (model === 'custom' && newModel !== 'custom') {
      setCustomModel("");
    }
  }, [model]);

  const handleMcpUrlChange = useCallback((newUrl: string) => {
    setMcpServerUrl(newUrl);
    validateMcpUrl(newUrl);
  }, [validateMcpUrl]);

  const handleCustomModelChange = useCallback((value: string) => {
    setCustomModel(value);

    if (value && value.trim()) {
      if (value.length < 3) {
        setModelError("Model name must be at least 3 characters");
      } else {
        setModelError("");
      }
    } else {
      setModelError("Please enter a model name");
    }
  }, []);

  const getCurrentProviderConfig = useCallback(() => {
    return LLM_PROVIDERS.find(p => p.value === provider) || LLM_PROVIDERS[0];
  }, [provider]);

  const getAvailableModels = useCallback(() => {
    const providerConfig = getCurrentProviderConfig();
    return providerConfig.models;
  }, [getCurrentProviderConfig]);

  const getFinalModelName = useCallback(() => {
    return model === 'custom' ? customModel.trim() : model;
  }, [model, customModel]);

  const validateSettings = useCallback(() => {
    const isApiKeyValid = validateApiKey(apiKey);
    const mcpUrlValid = validateMcpUrl(mcpServerUrl);

    let modelValid = true;
    if (model === 'custom' && (!customModel || customModel.trim().length < 3)) {
      setModelError("Please enter a valid model name");
      modelValid = false;
    }

    return isApiKeyValid && modelValid && mcpUrlValid;
  }, [apiKey, validateApiKey, mcpServerUrl, validateMcpUrl, model, customModel]);

  const saveSettings = useCallback(async () => {
    if (!apiKey.trim()) {
      setSnackbar({
        open: true,
        message: "Please enter an API key",
        severity: "error"
      });
      return;
    }

    if (!validateSettings()) {
      setSnackbar({
        open: true,
        message: "Please fix the errors in the form",
        severity: "error"
      });
      return;
    }

    try {
      await chrome.storage.local.set({
        [LLM_API_KEY_STORAGE_KEY]: apiKey,
        [LLM_MODEL_STORAGE_KEY]: getFinalModelName(),
        [LLM_PROVIDER_STORAGE_KEY]: provider,
        [MCP_SERVER_URL]: mcpServerUrl
      });

      await testMCPConnection(mcpServerUrl);

      setSnackbar({
        open: true,
        message: "Settings saved successfully!",
        severity: "success"
      });

      setModel(getFinalModelName());

      return true;
    } catch (error) {
      setSnackbar({
        open: true,
        message: "Failed to save settings to storage",
        severity: "error"
      });
      return false;
    }
  }, [apiKey, validateSettings, getFinalModelName, provider, mcpServerUrl, testMCPConnection]);

  const clearSettings = useCallback(async () => {
    await chrome.storage.local.remove([
      LLM_API_KEY_STORAGE_KEY,
      LLM_MODEL_STORAGE_KEY,
      LLM_PROVIDER_STORAGE_KEY,
      MCP_SERVER_URL
    ]);

    setApiKey("");
    setApiKeyValid(false);
    setApiKeyError("");
    setProvider(DEFAULT_PROVIDER);
    setModel(DEFAULT_MODEL);
    setCustomModel("");
    setModelError("");
    setMcpServerUrl(DEFAULT_MCP_URL);
    setMcpUrlError("");
    setConnectionStatus("disconnected");

    setSnackbar({
      open: true,
      message: "All settings cleared",
      severity: "info"
    });
  }, []);

  const testConnection = useCallback(async () => {
    const success = await testMCPConnection(mcpServerUrl);
    if (success) {
      setSnackbar({
        open: true,
        message: "Successfully connected to MCP server!",
        severity: "success"
      });
    } else {
      setSnackbar({
        open: true,
        message: "Failed to connect to MCP server. Please check the URL and ensure the server is running.",
        severity: "error"
      });
    }
  }, [mcpServerUrl, testMCPConnection]);

  const closeSnackbar = useCallback(() => {
    setSnackbar(prev => ({ ...prev, open: false }));
  }, []);

  return {
    apiKey,
    showApiKey,
    apiKeyError,
    apiKeyValid,
    provider,
    model,
    customModel,
    mcpServerUrl,
    modelError,
    mcpUrlError,
    connectionStatus,
    snackbar,
    setApiKey,
    setShowApiKey,
    setSnackbar,
    handleApiKeyChange,
    handleProviderChange,
    handleModelChange,
    handleCustomModelChange,
    handleMcpUrlChange,
    getCurrentProviderConfig,
    getAvailableModels,
    getFinalModelName,
    saveSettings,
    clearSettings,
    testConnection,
    closeSnackbar,
    validateSettings
  };
};