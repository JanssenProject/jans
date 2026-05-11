import React from 'react';
import { LLM_PROVIDERS, getConnectionStatusText } from '../constants';
import { LLMProvider, ConnectionStatus } from '../types';

interface SettingsDialogProps {
  open: boolean;
  onClose: () => void;
  apiKey: string;
  showApiKey: boolean;
  provider: string;
  model: string;
  customModel: string;
  mcpServerUrl: string;
  connectionStatus: ConnectionStatus;
  apiKeyError: string;
  modelError: string;
  mcpUrlError: string;
  onApiKeyChange: (key: string) => void;
  onToggleShowApiKey: () => void;
  onProviderChange: (provider: string) => void;
  onModelChange: (model: string) => void;
  onCustomModelChange: (model: string) => void;
  onMcpUrlChange: (url: string) => void;
  onTestConnection: () => void;
  onSaveSettings: () => void;
  onClearSettings: () => void;
}

const SettingsDialog: React.FC<SettingsDialogProps> = ({
  open,
  onClose,
  apiKey,
  showApiKey,
  provider,
  model,
  customModel,
  mcpServerUrl,
  connectionStatus,
  apiKeyError,
  modelError,
  mcpUrlError,
  onApiKeyChange,
  onToggleShowApiKey,
  onProviderChange,
  onModelChange,
  onCustomModelChange,
  onMcpUrlChange,
  onTestConnection,
  onSaveSettings,
  onClearSettings,
}) => {
  if (!open) return null;

  const getCurrentProviderConfig = (): LLMProvider =>
    LLM_PROVIDERS.find((p) => p.value === provider) || LLM_PROVIDERS[0];

  const getAvailableModels = () => getCurrentProviderConfig().models;

  const connectionDotColor =
    connectionStatus === 'connected'
      ? 'bg-green-500'
      : connectionStatus === 'connecting'
        ? 'bg-amber-500'
        : 'bg-red-500';

  const connectionTextColor =
    connectionStatus === 'connected'
      ? 'text-green-600'
      : connectionStatus === 'connecting'
        ? 'text-amber-600'
        : 'text-red-600';

  return (
    /* Backdrop */
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />

      {/* Dialog panel */}
      <div className="relative z-10 bg-white rounded-2xl shadow-2xl w-full max-w-2xl mx-4 max-h-[90vh] overflow-y-auto">

        {/* Header */}
        <div className="flex items-center justify-between px-7 pt-7 pb-5">
          <h2 className="text-2xl font-bold text-gray-900">AI Assistant Settings</h2>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-100 text-gray-500 hover:text-gray-700 transition-colors"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div className="px-7 pb-7 space-y-6">

          {/* ── AI Provider Selection ── */}
          <div>
            <p className="text-sm font-semibold text-gray-700 mb-3">AI Provider</p>
            <div className="grid grid-cols-3 gap-3">
              {LLM_PROVIDERS.map((p) => (
                <button
                  key={p.value}
                  onClick={() => onProviderChange(p.value)}
                  className={`flex flex-col items-center gap-2 px-4 py-5 rounded-xl border-2 transition-all ${provider === p.value
                      ? 'border-gray-900 bg-white shadow-sm'
                      : 'border-gray-200 bg-white hover:border-gray-300'
                    }`}
                >
                  {/* Provider icon */}
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center ${p.value === 'openai'
                      ? 'bg-green-100'
                      : p.value === 'gemini'
                        ? 'bg-blue-100'
                        : 'bg-purple-100'
                    }`}>
                    {p.value === 'openai' && (
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#16a34a" strokeWidth="1.8">
                        <circle cx="12" cy="12" r="9" />
                        <path d="M12 7v5l3 3" strokeLinecap="round" />
                      </svg>
                    )}
                    {p.value === 'gemini' && (
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#2563eb" strokeWidth="1.8">
                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                      </svg>
                    )}
                    {p.value === 'deepseek' && (
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#7c3aed" strokeWidth="1.8">
                        <path d="M12 2l7 7-7 7-7-7 7-7z" />
                        <path d="M5 15l7 7 7-7" />
                      </svg>
                    )}
                  </div>
                  <span className="text-sm font-semibold text-gray-900">{p.label}</span>
                  <span className="text-xs text-gray-500">{p.description}</span>
                </button>
              ))}
            </div>
          </div>

          {/* ── API Key ── */}
          <div>
            <p className="text-sm font-semibold text-gray-700 mb-2">
              {getCurrentProviderConfig().label} API Key:
            </p>
            <div className={`flex items-center border rounded-xl overflow-hidden transition ${apiKeyError ? 'border-red-400' : 'border-gray-300 focus-within:border-gray-400 focus-within:ring-1 focus-within:ring-gray-300'
              }`}>
              <input
                type={showApiKey ? 'text' : 'password'}
                value={apiKey}
                onChange={(e) => onApiKeyChange(e.target.value)}
                placeholder={getCurrentProviderConfig().apiKeyPlaceholder}
                className="flex-1 px-4 py-3 text-sm text-gray-700 placeholder-gray-400 outline-none bg-white"
              />
              <button
                onClick={onToggleShowApiKey}
                className="px-3 text-gray-400 hover:text-gray-600 transition-colors"
              >
                {showApiKey ? (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                    <line x1="1" y1="1" x2="23" y2="23" strokeLinecap="round" />
                  </svg>
                ) : (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                )}
              </button>
            </div>
            {apiKeyError && (
              <p className="text-xs text-red-500 mt-1">{apiKeyError}</p>
            )}
            <p className="text-xs text-gray-400 mt-1.5">
              Format: {getCurrentProviderConfig().apiKeyFormat} · Keep this private and never share it publicly.
            </p>
          </div>

          {/* ── Model Selection + MCP Server (side by side) ── */}
          <div className="grid grid-cols-2 gap-5">
            {/* Model Selection */}
            <div>
              <p className="text-sm font-semibold text-gray-700 mb-2">Model Selection:</p>
              <div className="relative">
                <select
                  value={model}
                  onChange={(e) => onModelChange(e.target.value)}
                  className="w-full appearance-none border border-gray-300 rounded-xl px-4 py-3 text-sm text-gray-700 bg-white outline-none focus:ring-1 focus:ring-gray-400 focus:border-gray-400 pr-10 transition"
                >
                  {getAvailableModels().map((m) => (
                    <option key={m.value} value={m.value}>
                      {m.label}
                    </option>
                  ))}
                  <option value="custom">Custom Model</option>
                </select>
                {/* Chevron */}
                <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-gray-400">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <polyline points="6 9 12 15 18 9" />
                  </svg>
                </span>
              </div>
              {/* Model description */}
              {(() => {
                const mc = getAvailableModels().find((m) => m.value === model);
                return mc ? (
                  <p className="text-xs text-gray-400 mt-1.5">{mc.description}</p>
                ) : null;
              })()}

              {/* Custom model input */}
              {model === 'custom' && (
                <div className="mt-3">
                  <input
                    type="text"
                    value={customModel}
                    onChange={(e) => onCustomModelChange(e.target.value)}
                    placeholder="e.g., your-company-model-name"
                    className={`w-full border rounded-xl px-4 py-3 text-sm text-gray-700 placeholder-gray-400 outline-none focus:ring-1 focus:ring-gray-400 transition ${modelError ? 'border-red-400' : 'border-gray-300'
                      }`}
                  />
                  {modelError && (
                    <p className="text-xs text-red-500 mt-1">{modelError}</p>
                  )}
                  <p className="text-xs text-gray-400 mt-1.5">
                    Enter the exact model name as defined in your API
                  </p>
                </div>
              )}
            </div>

            {/* MCP Server */}
            <div>
              <p className="text-sm font-semibold text-gray-700 mb-2">MCP Server:</p>
              <div className={`flex items-center border rounded-xl overflow-hidden transition ${mcpUrlError ? 'border-red-400' : 'border-gray-300 focus-within:border-gray-400 focus-within:ring-1 focus-within:ring-gray-300'
                }`}>
                <input
                  type="text"
                  value={mcpServerUrl}
                  onChange={(e) => onMcpUrlChange(e.target.value)}
                  placeholder="http://localhost:3001"
                  className="flex-1 px-4 py-3 text-sm text-gray-700 placeholder-gray-400 outline-none bg-white"
                />
                <button
                  onClick={onTestConnection}
                  disabled={connectionStatus === 'connecting'}
                  className="px-3 py-1 mr-2 border border-gray-300 rounded-lg text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 transition-colors"
                >
                  {connectionStatus === 'connecting' ? 'Testing...' : 'Test'}
                </button>
              </div>
              {mcpUrlError && (
                <p className="text-xs text-red-500 mt-1">{mcpUrlError}</p>
              )}
              {/* Connection status */}
              <div className="flex items-center gap-1.5 mt-2">
                <span className={`w-2 h-2 rounded-full flex-shrink-0 ${connectionDotColor}`} />
                <p className="text-xs text-gray-500">
                  Status:{' '}
                  <span className={`font-semibold ${connectionTextColor}`}>
                    {getConnectionStatusText(connectionStatus)}
                  </span>
                  {connectionStatus === 'disconnected' && (
                    <span className="text-gray-400"> – server unreachable</span>
                  )}
                </p>
              </div>
            </div>
          </div>

          {/* ── Info panel ── */}
          <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
            <div className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="white">
                  <path d="M12 2a10 10 0 1 0 0 20A10 10 0 0 0 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
                </svg>
              </div>
              <div className="text-sm text-blue-800 space-y-1">
                <p className="font-semibold">Where to get API keys:</p>
                <ul className="space-y-0.5 text-sm">
                  <li>
                    · <strong>OpenAI</strong>: Visit{' '}
                    <a href="https://platform.openai.com/api-keys" target="_blank" rel="noopener noreferrer" className="underline hover:text-blue-900">
                      platform.openai.com
                    </a>
                  </li>
                  <li>
                    · <strong>Google Gemini</strong>: Visit{' '}
                    <a href="https://makersuite.google.com/app/apikey" target="_blank" rel="noopener noreferrer" className="underline hover:text-blue-900">
                      makersuite.google.com
                    </a>
                  </li>
                  <li>
                    · <strong>DeepSeek</strong>: Visit{' '}
                    <a href="https://platform.deepseek.com/api_keys" target="_blank" rel="noopener noreferrer" className="underline hover:text-blue-900">
                      platform.deepseek.com
                    </a>
                  </li>
                </ul>
                <p className="text-blue-700 pt-1">
                  The MCP server must be running locally at the specified URL. Make sure your MCP
                  server supports the tools you want to use.
                </p>
              </div>
            </div>
          </div>

          {/* ── Footer actions ── */}
          <div className="flex items-center gap-3 pt-1">
            <button
              onClick={onSaveSettings}
              disabled={!apiKey.trim() || (model === 'custom' && !customModel.trim())}
              className="px-6 py-2.5 bg-green-600 hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-sm font-semibold rounded-lg transition-colors"
            >
              Save Settings
            </button>
            <button
              onClick={onClearSettings}
              className="px-6 py-2.5 border border-gray-300 hover:bg-gray-50 text-gray-700 text-sm font-medium rounded-lg transition-colors"
            >
              Clear All
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SettingsDialog;