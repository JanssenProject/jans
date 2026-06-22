import React from 'react';
import { X, Copy, List } from 'lucide-react';
import { JsonEditor } from 'json-edit-react';
import initWasm, { init } from '@janssenproject/cedarling_wasm';
import { v4 as uuidv4 } from 'uuid';
import Utils from '../../../options/Utils';
import cedarlingBootstrapJson from '../cedarlingBootstrap.json';

interface AddCedarlingConfigProps {
  newData: any;
  handleDialog: (isOpen: boolean) => void;
  isOpen: boolean;
}

export default function AddCedarlingConfig({ isOpen, handleDialog, newData }: AddCedarlingConfigProps) {
  const [bootstrap, setBootstrap] = React.useState(newData);
  const [errorMessage, setErrorMessage] = React.useState('');
  const [loading, setLoading] = React.useState(false);
  const [inputSelection, setInputSelection] = React.useState<'json' | 'url'>('json');
  const [showConfiguration, setShowConfiguration] = React.useState(false);
  const [showConfigurationButton, setShowConfigurationButton] = React.useState(true);
  const [snackbarMsg, setSnackbarMsg] = React.useState('');

  React.useEffect(() => {
    if (!isOpen) return;
    setErrorMessage('');
    setLoading(false);
    if (Utils.isEmpty(newData) || Object.keys(newData).length === 0) {
      setBootstrap({});
      setShowConfiguration(true);
      setShowConfigurationButton(true);
    } else {
      setBootstrap(newData);
      setShowConfiguration(false);
      setShowConfigurationButton(false);
    }
  }, [isOpen, newData]);

  if (!isOpen) return null;

  const handleClose = () => {
    setInputSelection('json');
    handleDialog(false);
  };

  const copyToClipboard = () => {
    try {
      navigator.clipboard.writeText(JSON.stringify(bootstrap, null, 2));
      setSnackbarMsg('JSON copied to clipboard!');
      setTimeout(() => setSnackbarMsg(''), 3000);
    } catch (err) {
      setSnackbarMsg('Copy failed: ' + String(err));
    }
  };

  const isJsonValid = async (data: unknown) => {
    setErrorMessage('');
    try {
      setBootstrap(JSON.parse(JSON.stringify(data)));
      return true;
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      setErrorMessage(`Invalid input: ${msg}`);
      return false;
    }
  };

  const saveBootstrap = async () => {
    try {
      setLoading(true);
      if (!(await isJsonValid(bootstrap))) return;
      await initWasm();
      await init(bootstrap);
      chrome.storage.local.get(['cedarlingConfig'], ({ cedarlingConfig = [] }) => {
        const updatedConfig = [{ ...bootstrap, id: uuidv4() }];
        chrome.storage.local.set({ cedarlingConfig: updatedConfig }, handleClose);
      });
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      setErrorMessage('Error in adding bootstrap. ' + msg);
    }
    setLoading(false);
  };

  return (
    <>
      {/* Backdrop */}
      <div className="fixed inset-0 bg-black/40 z-40 flex items-center justify-center backdrop-blur-sm">
        {/* Dialog Container */}
        <div
          className="relative bg-white rounded-xl shadow-2xl w-full max-w-2xl mx-4 overflow-hidden"
          onClick={(e) => e.stopPropagation()}
        >
          {loading && (
            <div className="absolute inset-0 bg-white/60 flex items-center justify-center z-50">
              <div className="w-10 h-10 border-4 border-[#00B06E] border-t-transparent rounded-full animate-spin" />
            </div>
          )}

          {/* Header */}
          <div className="flex items-center justify-between px-8 pt-8 pb-2">
            <div>
              <h2 className="text-2xl font-bold text-[#002B49]">Add Cedarling Configuration</h2>
              <p className="text-sm text-gray-500 mt-1">Submit below details</p>
            </div>
            <button onClick={handleClose} className="text-gray-400 hover:text-gray-600 transition-colors">
              <X size={24} />
            </button>
          </div>

          <div className="px-8 py-4 space-y-6">
            {/* Input Toggle + Copy Group */}
            <div className="flex items-center justify-between">
              <div className="flex bg-gray-100 p-1 rounded-lg">
                <button
                  onClick={() => setInputSelection('json')}
                  className={`px-6 py-1.5 text-xs font-bold rounded-md transition-all ${inputSelection === 'json' ? 'bg-[#00B06E] text-white shadow-sm' : 'text-gray-500 hover:text-gray-700'
                    }`}
                >
                  JSON
                </button>
                <button
                  onClick={() => setInputSelection('url')}
                  className={`px-6 py-1.5 text-xs font-bold rounded-md transition-all ${inputSelection === 'url' ? 'bg-[#00B06E] text-white shadow-sm' : 'text-gray-500 hover:text-gray-700'
                    }`}
                >
                  URL
                </button>
              </div>

              {inputSelection === 'json' && (
                <button
                  onClick={copyToClipboard}
                  className="flex items-center gap-2 px-3 py-1.5 border border-gray-200 rounded-lg text-xs font-medium text-gray-600 hover:bg-gray-50"
                >
                  <Copy size={14} />
                  Copy
                </button>
              )}
            </div>

            {/* Config Button Toggle */}
            {inputSelection === 'json' && showConfigurationButton && (
              <div>
                <button
                  onClick={() => {
                    setBootstrap(showConfiguration ? cedarlingBootstrapJson : {});
                    setShowConfiguration(!showConfiguration);
                  }}
                  className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-600 hover:bg-gray-50 font-medium"
                >
                  <List size={18} />
                  {showConfiguration ? 'Remove Minimal Configuration' : 'Add Minimal Configuration'}
                </button>
              </div>
            )}

            {/* JSON Editor Window */}
            {inputSelection === 'json' ? (
              <div className="border border-gray-200 rounded-xl bg-gray-50/30 overflow-hidden">
                <style>{`
                  .jer-confirm-buttons {
                    position: absolute !important;
                    top: 4px !important;
                    right: 4px !important;
                    bottom: unset !important;
                  }
                  .jer-collection-inner {
                    position: relative !important;
                  }
                `}
                </style>
                <div className="max-h-80 overflow-y-auto p-2">
                  <JsonEditor
                    data={bootstrap}
                    setData={setBootstrap}
                    rootName="bootstrapConfig"
                    restrictEdit={false}
                    restrictAdd={false}
                    restrictDelete={false}
                  />
                </div>
              </div>
            ) : (
              <input
                type="text"
                placeholder="Enter Configuration URL"
                className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-[#00B06E] focus:border-transparent outline-none transition-all"
              />
            )}

            {errorMessage && (
              <p className="text-red-500 text-sm font-medium bg-red-50 p-3 rounded-lg border border-red-100">
                {errorMessage}
              </p>
            )}
          </div>

          {/* Footer Action */}
          <div className="px-8 pb-8">
            <button
              onClick={saveBootstrap}
              disabled={loading}
              className="w-24 py-2.5 bg-[#00B06E] hover:bg-[#00965e] text-white font-bold rounded-lg transition-all shadow-lg shadow-green-200 disabled:opacity-50"
            >
              Save
            </button>
          </div>
        </div>
      </div>

      {/* Snackbar */}
      {!!snackbarMsg && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 px-6 py-3 bg-[#002B49] text-white text-sm font-medium rounded-full shadow-2xl flex items-center gap-3 z-[60] animate-bounce">
          {snackbarMsg}
        </div>
      )}
    </>
  );
}