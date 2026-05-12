import React from 'react';
import { getConnectionStatusText } from '../constants';
import { ConnectionStatus } from '../types';

interface StatusPanelProps {
  apiKeyValid: boolean;
  provider: string;
  modelName: string;
  providerLabel: string;
  mcpServerUrl: string;
  connectionStatus: ConnectionStatus;
  onTestConnection: () => void;
}

const StatusPanel: React.FC<StatusPanelProps> = ({
  apiKeyValid,
  provider,
  modelName,
  providerLabel,
  mcpServerUrl,
  connectionStatus,
  onTestConnection,
}) => {
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

  const statusTextColor = apiKeyValid ? 'text-green-600' : 'text-amber-600';
  const statusLabel = apiKeyValid ? 'Configured' : 'Not configured';

  return (
    <div className="grid grid-cols-2 gap-4 mb-5">
      {/* ── LLM Provider card ── */}
      <div className={`flex items-start gap-4 rounded-xl border px-5 py-4 ${
        apiKeyValid
          ? 'bg-green-50 border-green-200'
          : 'bg-amber-50 border-amber-200'
      }`}>
        {/* Cloud icon */}
        <div className="w-10 h-10 rounded-lg bg-amber-100 flex items-center justify-center flex-shrink-0 mt-0.5">
          <svg width="20" height="14" viewBox="0 0 24 16" fill="none">
            <path
              d="M5 14a4 4 0 0 1 0-8 5.5 5.5 0 0 1 11 0 3 3 0 0 1 0 8H5z"
              fill="#d97706"
            />
          </svg>
        </div>

        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-gray-900">LLM Provider</p>
          <p className="text-xs text-gray-500 truncate">
            {providerLabel} · {modelName}
          </p>
          <div className="mt-2 space-y-0.5">
            <div className="flex items-center justify-between text-xs">
              <span className="text-gray-500">Status:</span>
              <span className={`font-semibold ${statusTextColor}`}>
                <span className={`inline-block w-1.5 h-1.5 rounded-full mr-1 align-middle ${
                  apiKeyValid ? 'bg-green-500' : 'bg-amber-500'
                }`} />
                {statusLabel}
              </span>
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-gray-500">Model:</span>
              <span className="font-semibold text-gray-800">{modelName}</span>
            </div>
          </div>
        </div>
      </div>

      {/* ── MCP Server card ── */}
      <div className="flex items-start gap-4 rounded-xl border bg-red-50 border-red-200 px-5 py-4">
        {/* Pulse/wave icon */}
        <div className="w-10 h-10 rounded-lg bg-red-100 flex items-center justify-center flex-shrink-0 mt-0.5">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#dc2626" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
        </div>

        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-gray-900">LLM Provider</p>
          <p className="text-xs text-gray-500 truncate">
            {providerLabel} · {modelName}
          </p>
          <div className="mt-2 space-y-1.5">
            <div className="flex items-center justify-between text-xs">
              <span className="text-gray-500">Status:</span>
              <span className={`font-semibold flex items-center gap-1 ${connectionTextColor}`}>
                <span className={`w-1.5 h-1.5 rounded-full ${connectionDotColor}`} />
                {getConnectionStatusText(connectionStatus)}
              </span>
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-gray-500">Action:</span>
              <button
                onClick={onTestConnection}
                disabled={connectionStatus === 'connecting'}
                className="border border-gray-300 rounded-md px-3 py-0.5 text-xs font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 transition-colors"
              >
                {connectionStatus === 'connecting' ? 'Testing...' : 'Test Connection'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default StatusPanel;