import React from 'react';
import { getProviderColor } from '../constants';

interface AIResponseProps {
  result: string | any;
  loading: boolean;
  error: string | null;
  provider: string;
  modelName: string;
  providerLabel: string;
}

const AIResponse: React.FC<AIResponseProps> = ({
  result,
  loading,
  error,
  provider,
  modelName,
  providerLabel,
}) => {
  if (!result && !loading && !error) return null;

  return (
    <div className="mb-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-3">AI Response</h2>

      <div className="border border-gray-200 rounded-xl bg-gray-50 p-5 min-h-[200px]">
        {/* Loading state */}
        {loading && (
          <div className="flex items-center justify-center h-48 gap-3">
            <svg
              className="animate-spin text-gray-600"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
            >
              <path d="M21 12a9 9 0 1 1-6.219-8.56" />
            </svg>
            <p className="text-sm text-gray-600">
              Processing with {providerLabel} - {modelName}...
            </p>
          </div>
        )}

        {/* Error state */}
        {error && !loading && (
          <div className="text-red-600">
            <p className="text-base font-semibold mb-1">Error</p>
            <p className="text-sm">{error}</p>
          </div>
        )}

        {/* Result state */}
        {result && !loading && (
          <div className="max-h-96 overflow-auto">
            <p className="text-xs text-gray-400 mb-3 block">
              Response from {providerLabel} - {modelName} at{' '}
              {new Date().toLocaleTimeString()}
            </p>
            {typeof result === 'string' ? (
              <pre className="whitespace-pre-wrap break-words font-mono text-sm text-gray-800 m-0">
                {result}
              </pre>
            ) : (
              <pre className="whitespace-pre-wrap break-words font-mono text-sm text-gray-800 m-0">
                {JSON.stringify(result, null, 2)}
              </pre>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default AIResponse;