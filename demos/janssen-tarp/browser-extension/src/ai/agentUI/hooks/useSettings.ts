import { useState, useEffect, useCallback } from 'react';
import {
  LLM_MODEL_STORAGE_KEY,
  LLM_PROVIDER_STORAGE_KEY,
  MCP_SERVER_URL,
  ConnectionStatus,
  SnackbarState
} from '../types';
import { LLM_PROVIDERS, DEFAULT_MODEL, DEFAULT_PROVIDER, DEFAULT_MCP_URL } from '../constants';
import { mcpApiService } from '../../service/MCPAPIService';

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
  const [loadingApiKey, setLoadingApiKey] = useState(false);

  const loadApiKeyFromMCP = useCallback(async (providerName: string, modelName: string, skipConnectionCheck = false) => {
    if (!mcpServerUrl) return;
    if (!skipConnectionCheck && connectionStatus !== "connected") return;

    setLoadingApiKey(true);
    try {
      const apiKeyData = await mcpApiService.findApiKeyByProvider(providerName, modelName);

      if (apiKeyData) {
        setApiKey(apiKeyData.key);
        mcpApiService.setCurrentApiKeyId(apiKeyData.id || null);
        validateApiKey(apiKeyData.key, providerName, modelName);
      } else {
        setApiKey("");
        mcpApiService.setCurrentApiKeyId(null);
        setApiKeyValid(false);
      }
    } catch (error) {
      console.error("Failed to load API key from MCP server:", error);
      setApiKey("");
      mcpApiService.setCurrentApiKeyId(null);
      setApiKeyValid(false);
    } finally {
      setLoadingApiKey(false);
    }
  }, [mcpServerUrl, connectionStatus]);

  // Load settings on component mount
  useEffect(() => {
    const initializeSettings = async () => {
      // Load settings from chrome storage
      const results = await new Promise<Record<string, string>>((resolve) => {
        chrome.storage.local.get([
          LLM_MODEL_STORAGE_KEY,
          LLM_PROVIDER_STORAGE_KEY,
          MCP_SERVER_URL
        ], resolve);
      });

      const savedModel = results[LLM_MODEL_STORAGE_KEY];
      const savedProvider = results[LLM_PROVIDER_STORAGE_KEY];
      const savedMcpUrl = results[MCP_SERVER_URL] || DEFAULT_MCP_URL;

      // Determine which MCP URL to use
      const mcpUrlToUse = savedMcpUrl;
      const hasSavedSettings = savedProvider && savedModel;

      // Set MCP URL and test connection
      setMcpServerUrl(mcpUrlToUse);
      mcpApiService.setBaseUrl(mcpUrlToUse);
      validateMcpUrl(mcpUrlToUse);

      const isConnected = await testMCPConnection(mcpUrlToUse);
      if (!isConnected) return;

      // Determine provider and model to use
      let providerToUse = savedProvider;
      let modelToUse = savedModel;

      if (!hasSavedSettings) {
        // Try to get from MCP server if no saved settings
        try {
          const apiKeys = await mcpApiService.getApiKeys();
          if (apiKeys.count > 0) {
            providerToUse = apiKeys.keys[0].provider;
            modelToUse = apiKeys.keys[0].model;
          } else {
            // Fallback to defaults
            providerToUse = DEFAULT_PROVIDER;
            modelToUse = DEFAULT_MODEL;
          }
          // Save other settings to chrome storage
          await chrome.storage.local.set({
            [LLM_MODEL_STORAGE_KEY]: modelToUse,
            [LLM_PROVIDER_STORAGE_KEY]: providerToUse,
            [MCP_SERVER_URL]: mcpServerUrl
          });
        } catch (error) {
          console.error("Failed to get API keys from MCP:", error);
          setSnackbar({
            open: true,
            message: error instanceof Error ? error.message : "Failed to get API keys from MCP",
            severity: "error"
          });
          providerToUse = DEFAULT_PROVIDER;
          modelToUse = DEFAULT_MODEL;
        }
      }

      // Set provider and model
      if (providerToUse) setProvider(providerToUse);
      if (modelToUse) setModel(modelToUse);

      // Load API key from MCP if we have a provider
      if (providerToUse && isConnected) {
        await initLoadApiKeyFromMCP(providerToUse, modelToUse);
      }
    };

    initializeSettings();
  }, []); 

  // For initialization, skip connection check
  const initLoadApiKeyFromMCP = useCallback(
    (providerName: string, modelName: string) =>
      loadApiKeyFromMCP(providerName, modelName, true),
    [loadApiKeyFromMCP]
  );

  // Validate API key based on provider
  const validateApiKey = useCallback((key: string, currentProvider: string = provider, currentModel: string = model) => {
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

  const handleProviderChange = useCallback(async (newProvider: string) => {
    setProvider(newProvider);

    // Reset model to default for new provider
    const providerConfig = LLM_PROVIDERS.find(p => p.value === newProvider);
    if (providerConfig && providerConfig.models.length > 0) {
      setModel(providerConfig.models[0].value);
    }

    // Load API key for new provider from MCP server
    if (mcpServerUrl && connectionStatus === "connected") {
      await loadApiKeyFromMCP(newProvider, providerConfig.models[0].value);
    }
  }, [mcpServerUrl, connectionStatus, loadApiKeyFromMCP]);

  const handleModelChange = useCallback(async (newModel: string) => {
    setModel(newModel);
    setModelError("");

    if (model === 'custom' && newModel !== 'custom') {
      setCustomModel("");
    }
    // Load API key for new provider from MCP server
    if (mcpServerUrl && connectionStatus === "connected") {
      await loadApiKeyFromMCP(null, newModel);
    }
  }, [model]);

  const handleMcpUrlChange = useCallback(async (newUrl: string) => {
    setMcpServerUrl(newUrl);
    const isValid = validateMcpUrl(newUrl);

    if (isValid) {
      mcpApiService.setBaseUrl(newUrl);
      await testMCPConnection(newUrl);
    }
  }, [validateMcpUrl, testMCPConnection]);

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
      // Save API key to MCP server
      const currentApiKeyId = mcpApiService.getCurrentApiKeyId();

      if (currentApiKeyId) {
        // Update existing API key
        await mcpApiService.updateApiKey(currentApiKeyId, apiKey);
      } else {
        // Create new API key
        const newApiKey = await mcpApiService.createApiKey(provider, model, apiKey);
        mcpApiService.setCurrentApiKeyId(newApiKey.id || null);
      }

      // Save other settings to chrome storage
      await chrome.storage.local.set({
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
    } catch (error: any) {
      setSnackbar({
        open: true,
        message: error.message || "Failed to save settings",
        severity: "error"
      });
      return false;
    }
  }, [apiKey, validateSettings, getFinalModelName, provider, mcpServerUrl, testMCPConnection]);

  const clearSettings = useCallback(async () => {
    try {
      // Delete API key from MCP server
      const currentApiKeyId = mcpApiService.getCurrentApiKeyId();
      if (currentApiKeyId) {
        await mcpApiService.deleteApiKey(currentApiKeyId);
      }

      // Clear other settings from chrome storage
      await chrome.storage.local.remove([
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
      mcpApiService.setCurrentApiKeyId(null);

      setSnackbar({
        open: true,
        message: "All settings cleared",
        severity: "info"
      });
    } catch (error: any) {
      setSnackbar({
        open: true,
        message: error.message || "Failed to clear settings",
        severity: "error"
      });
    }
  }, []);

  const testConnection = useCallback(async () => {
    const success = await testMCPConnection(mcpServerUrl);
    if (success) {
      setSnackbar({
        open: true,
        message: "Successfully connected to MCP server!",
        severity: "success"
      });

      // Load API key after successful connection
      await loadApiKeyFromMCP(provider, model);
    } else {
      setSnackbar({
        open: true,
        message: "Failed to connect to MCP server. Please check the URL and ensure the server is running.",
        severity: "error"
      });
    }
  }, [mcpServerUrl, testMCPConnection, provider, loadApiKeyFromMCP]);

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
    loadingApiKey,
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