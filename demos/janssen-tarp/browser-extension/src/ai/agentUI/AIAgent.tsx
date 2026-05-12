import React, { useCallback, useState } from 'react';
import { useSettings } from './hooks/useSettings';
import { useAIOperations } from './hooks/useAIOperations';
import SettingsDialog from './components/SettingsDialog';
import StatusPanel from './components/StatusPanel';
import AIResponse from './components/AIResponse';
import { AIAgentProps } from './types';

const AIAgent: React.FC<AIAgentProps> = ({ notifyOnDataChange }) => {
  const [settingsOpen, setSettingsOpen] = useState(false);

  const {
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
    handleApiKeyChange,
    handleProviderChange,
    handleModelChange,
    handleCustomModelChange,
    handleMcpUrlChange,
    getCurrentProviderConfig,
    saveSettings,
    clearSettings,
    testConnection,
    closeSnackbar,
    setShowApiKey,
  } = useSettings();

  const {
    query,
    setQuery,
    result,
    loading,
    error,
    send,
    handleKeyPress,
  } = useAIOperations(notifyOnDataChange);

  const handleSend = useCallback(async () => {
    try {
      await send();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err);
      if (
        message.includes('configure your API key') ||
        message.includes('configure MCP server')
      ) {
        setSettingsOpen(true);
      }
    }
  }, [send]);

  const handleSaveSettings = useCallback(async () => {
    const success = await saveSettings();
    if (success) setTimeout(() => setSettingsOpen(false), 1500);
  }, [saveSettings]);

  const getCurrentModelName = useCallback(() => {
    const providerConfig = getCurrentProviderConfig();
    const modelConfig = providerConfig.models.find((m: any) => m.value === model);
    return modelConfig ? modelConfig.label : model;
  }, [getCurrentProviderConfig, model]);

  const isReady = apiKeyValid && connectionStatus === 'connected';

  return (
    <>
      {/* ── Page body ── */}
      <div className="rounded-2xl bg-white p-8 shadow-sm border border-gray-100">

        {/* ── Page header ── */}
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-3xl font-bold text-gray-900">AI Assistant</h1>
          <button
            onClick={() => setSettingsOpen(true)}
            className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white px-5 py-2.5 rounded-lg text-sm font-semibold transition-colors"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="12" cy="12" r="3" />
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
            Settings
          </button>
        </div>

        {/* ── Status Panel ── */}
        <StatusPanel
          apiKeyValid={apiKeyValid}
          provider={provider}
          modelName={getCurrentModelName()}
          providerLabel={getCurrentProviderConfig().label}
          mcpServerUrl={mcpServerUrl}
          connectionStatus={connectionStatus}
          onTestConnection={testConnection}
        />

        {/* ── Query input box ── */}
        <div className="mt-5 mb-3">
          <div className="border border-gray-300 rounded-xl focus-within:ring-2 focus-within:ring-green-500 focus-within:border-transparent transition overflow-hidden">
            {/* Floating label */}
            <div className="px-4 pt-3 pb-0">
              <label className="text-sm font-medium text-gray-700">
                What would you like to do in Tarp?
              </label>
            </div>

            {/* Textarea */}
            <textarea
              rows={3}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={handleKeyPress}
              disabled={loading || !isReady}
              placeholder={
                isReady
                  ? `Describe what you want to accomplish... (using ${getCurrentProviderConfig().label} - ${getCurrentModelName()})`
                  : 'Configure settings to start using the assistant...'
              }
              className="w-full resize-none px-4 py-2 text-sm text-gray-700 placeholder-gray-400 outline-none bg-white disabled:cursor-default"
            />

            {/* Toolbar row */}
            <div className="flex items-center justify-between px-3 pb-3 pt-1">
              <div className="flex items-center gap-2">
                {/* Plus button */}
                <button
                  disabled
                  className="w-8 h-8 flex items-center justify-center border border-gray-300 rounded-lg text-gray-400 disabled:opacity-60"
                >
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                </button>
                {/* Spinner/options button */}
                <button
                  disabled
                  className="w-8 h-8 flex items-center justify-center border border-gray-300 rounded-lg text-gray-400 disabled:opacity-60"
                >
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                  </svg>
                </button>
              </div>

              {/* Send arrow button */}
              <button
                onClick={handleSend}
                disabled={loading || !query.trim() || !isReady}
                className="w-10 h-10 flex items-center justify-center bg-gray-900 hover:bg-gray-700 disabled:opacity-60 disabled:cursor-not-allowed text-white rounded-xl transition-colors"
              >
                {loading ? (
                  <svg
                    className="animate-spin"
                    width="17"
                    height="17"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                  >
                    <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                  </svg>
                ) : (
                  <svg
                    width="17"
                    height="17"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <line x1="5" y1="12" x2="19" y2="12" />
                    <polyline points="12 5 19 12 12 19" />
                  </svg>
                )}
              </button>
            </div>
          </div>

          {/* Caption row */}
          <div className="flex items-center justify-between mt-2 px-1">
            <p className="text-xs text-gray-500">
              Press <strong>Ctrl+Enter</strong> to send • Using{' '}
              <strong>{getCurrentProviderConfig().label}</strong> ·{' '}
              <strong>{getCurrentModelName()}</strong>
            </p>
            {!isReady && (
              <p className="text-xs text-amber-700 flex items-center gap-1 font-medium">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2L1 21h22L12 2zm0 3.5L20.5 19h-17L12 5.5zM11 10v4h2v-4h-2zm0 6v2h2v-2h-2z" />
                </svg>
                {!apiKeyValid ? 'API key not configured' : 'MCP server not connected'}
              </p>
            )}
          </div>
        </div>

        {/* ── AI Response ── */}
        <AIResponse
          result={result}
          loading={loading}
          error={error}
          provider={provider}
          modelName={getCurrentModelName()}
          providerLabel={getCurrentProviderConfig().label}
        />

        {/* ── Tip panel ── */}
        <div className="flex items-start gap-3 bg-amber-50 border border-amber-200 rounded-xl p-4 mt-2">
          <div className="w-9 h-9 bg-amber-100 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5">
            <svg width="20" height="14" viewBox="0 0 24 16" fill="none">
              <path
                d="M5 14a4 4 0 0 1 0-8 5.5 5.5 0 0 1 11 0 3 3 0 0 1 0 8H5z"
                fill="#d97706"
              />
            </svg>
          </div>
          <p className="text-sm text-gray-700 leading-relaxed">
            <strong>Tip:</strong> The AI assistant can help you with various tasks in Tarp.
            Describe what you want to accomplish in natural language.{' '}
            {!isReady ? (
              <button
                onClick={() => setSettingsOpen(true)}
                className="text-amber-700 font-bold underline hover:text-amber-800 transition-colors"
              >
                {!apiKeyValid
                  ? 'Configure your API key to start using the assistant!'
                  : 'Connect to MCP server to start using the assistant!'}
              </button>
            ) : (
              <span className="text-green-700 font-bold ml-1">
                Ready to use {getCurrentProviderConfig().label} - {getCurrentModelName()}
              </span>
            )}
          </p>
        </div>
      </div>


      {/* ── Settings Dialog ── */}
      <SettingsDialog
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        apiKey={apiKey}
        showApiKey={showApiKey}
        provider={provider}
        model={model}
        customModel={customModel}
        mcpServerUrl={mcpServerUrl}
        connectionStatus={connectionStatus}
        apiKeyError={apiKeyError}
        modelError={modelError}
        mcpUrlError={mcpUrlError}
        onApiKeyChange={handleApiKeyChange}
        onToggleShowApiKey={() => setShowApiKey(!showApiKey)}
        onProviderChange={handleProviderChange}
        onModelChange={handleModelChange}
        onCustomModelChange={handleCustomModelChange}
        onMcpUrlChange={handleMcpUrlChange}
        onTestConnection={testConnection}
        onSaveSettings={handleSaveSettings}
        onClearSettings={clearSettings}
      />

      {/* ── Toast snackbar ── */}
      {snackbar.open && (
        <div
          className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-3 rounded-xl shadow-lg text-white text-sm font-medium ${snackbar.severity === 'success'
            ? 'bg-green-600'
            : snackbar.severity === 'error'
              ? 'bg-red-600'
              : snackbar.severity === 'warning'
                ? 'bg-amber-500'
                : 'bg-blue-600'
            }`}
        >
          <span>{snackbar.message}</span>
          <button
            onClick={closeSnackbar}
            className="ml-1 opacity-80 hover:opacity-100 transition-opacity"
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
      )}
    </>
  );
};

export default AIAgent;