// UnsignedAuthzForm.tsx
import React from 'react';
import { JsonEditor } from 'json-edit-react';
import initWasm, { init, Cedarling, AuthorizeResult } from '@janssenproject/cedarling_wasm';
import Utils from '../../../options/Utils';

interface UnsignedAuthzFormProps {
  data: any;
}

export default function UnsignedAuthzForm({ data }: UnsignedAuthzFormProps) {
  const [logType, setLogType] = React.useState('Decision');
  const [authzResult, setAuthzResult] = React.useState('');
  const [authzLogs, setAuthzLogs] = React.useState('');
  const [expanded, setExpanded] = React.useState(true);
  const [formFields, setFormFields] = React.useState<{
    principals: unknown[];
    action: string;
    context: Record<string, unknown>;
    resource: Record<string, unknown>;
  }>({
    principals: [],
    action: '',
    context: {},
    resource: {},
  });

  React.useEffect(() => {
    chrome.storage.local.get(['authzRequest_unsigned'], (result) => {
      const savedRequest = result?.authzRequest_unsigned;
      if (savedRequest) {
        setFormFields({
          principals: savedRequest.principals ?? [],
          action: savedRequest.action ?? '',
          context: savedRequest.context ?? {},
          resource: savedRequest.resource ?? {},
        });
      }
    });
  }, []);

  const triggerCedarlingAuthzRequest = async () => {
    setAuthzResult('');
    setAuthzLogs('');
    const reqObj = await createCedarlingAuthzRequestObj();
    chrome.storage.local.get(['cedarlingConfig'], async (cedarlingConfig) => {
      let instance: Cedarling | null = null;
      try {
        if (Object.keys(cedarlingConfig).length !== 0) {
          await initWasm();
          instance = await init(
            !Utils.isEmpty(cedarlingConfig?.cedarlingConfig)
              ? cedarlingConfig?.cedarlingConfig[0]
              : undefined
          );
          const result: AuthorizeResult = await instance.authorize_unsigned(reqObj);
          const logs = await instance.get_logs_by_request_id_and_tag(result.request_id, logType);
          setAuthzResult(result.json_string());
          if (logs.length !== 0) {
            setAuthzLogs(logs.map((log: unknown) => JSON.stringify(log, null, 2)).toString());
          }
        }
      } catch (err: unknown) {
        setAuthzResult(String(err));
        if (instance) {
          const logs = await instance.pop_logs();
          if (logs.length !== 0) {
            setAuthzLogs(logs.map((log: unknown) => JSON.stringify(log, null, 2)).toString());
          }
        }
      }
    });
  };

  const createCedarlingAuthzRequestObj = async () => {
    const reqObj = {
      principals: formFields.principals,
      action: formFields.action,
      context: formFields.context,
      resource: formFields.resource,
    };
    chrome.storage.local.set({ authzRequest_unsigned: reqObj });
    return reqObj;
  };

  const resetInputs = () => {
    const emptyRequest = { principals: [], action: '', context: {}, resource: {} };
    setFormFields(emptyRequest);
    chrome.storage.local.set({ authzRequest_unsigned: emptyRequest });
  };

  if (!data || data?.length === 0) return null;

  const logTabs = ['Decision', 'System', 'Metric'];

  return (
    <div className="space-y-4">
      {/* Main accordion */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {/* Card header */}
        <div className="px-6 py-5 border-b border-gray-100">
          <div className="flex items-start justify-between">
            <div>
              <h2 className="text-xl font-bold text-gray-900">
                Cedarling Unsigned Authorization
              </h2>
              <p className="text-sm text-gray-500 mt-1">
                Build an Unsigned authz request from principals, action, resource, and context — then run
                authorization.
              </p>
            </div>
          </div>
        </div>
        {/* Accordion header */}
        <button
          onClick={() => setExpanded(!expanded)}
          className="w-full flex items-center justify-between px-5 py-4 text-left hover:bg-gray-50 transition-colors"
        >
          <span className="font-semibold text-gray-800 text-sm">
            Request builder
          </span>
          <span className="flex items-center justify-center w-7 h-7 bg-gray-900 text-white rounded-full text-lg leading-none">
            −
          </span>
        </button>

        {expanded && (
          <div className="px-5 pb-5 space-y-5 border-t border-gray-100">
            {/* Principals */}
            <div className="pt-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">Principals:</label>
              <div className="border border-gray-200 rounded-lg p-3">
                <JsonEditor
                  data={formFields.principals}
                  setData={(e: any) => setFormFields((prev) => ({ ...prev, principals: e }))}
                  rootName="principals"
                />
              </div>
            </div>

            {/* Action */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Action:</label>
              <input
                type="text"
                name="action"
                placeholder="Enter Here"
                value={formFields.action}
                onChange={(e) =>
                  setFormFields((prev) => ({ ...prev, action: e.target.value }))
                }
                className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm text-gray-700 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent transition"
              />
            </div>

            {/* Resource */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Resource:</label>
              <div className="border border-gray-200 rounded-lg p-3">
                <JsonEditor
                  data={formFields.resource}
                  rootName="resource"
                  setData={(e: unknown) =>
                    setFormFields((prev) => ({ ...prev, resource: (e ?? {}) as Record<string, unknown> }))
                  }
                />
              </div>
            </div>

            {/* Context */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Context:</label>
              <div className="border border-gray-200 rounded-lg p-3">
                <JsonEditor
                  data={formFields.context}
                  setData={(e: unknown) =>
                    setFormFields((prev) => ({ ...prev, context: (e ?? {}) as Record<string, unknown> }))
                  }
                  rootName="context"
                />
              </div>
            </div>

            {/* Log Type */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Log Type:</label>
              <div className="flex rounded-lg border border-gray-200 overflow-hidden w-fit">
                {logTabs.map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setLogType(tab)}
                    className={`px-5 py-2 text-sm font-medium transition-colors ${logType === tab
                        ? 'bg-green-600 text-white'
                        : 'bg-white text-gray-600 hover:bg-gray-50'
                      }`}
                  >
                    {tab}
                  </button>
                ))}
              </div>
            </div>

            <hr className="border-gray-200" />

            {/* Actions */}
            <div className="flex items-center gap-3">
              <button
                onClick={triggerCedarlingAuthzRequest}
                className="px-5 py-2.5 bg-green-600 hover:bg-green-700 text-white text-sm font-medium rounded-lg transition-colors"
              >
                Cedarling Authz Request
              </button>
              <button
                onClick={resetInputs}
                className="px-5 py-2.5 border border-gray-300 text-gray-700 text-sm font-medium rounded-lg hover:bg-gray-50 transition-colors"
              >
                Reset
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Result */}
      {!!authzResult && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <details open>
            <summary className="px-5 py-4 font-semibold text-gray-800 text-sm cursor-pointer hover:bg-gray-50 transition-colors">
              Cedarling Authz Result
            </summary>
            <div className="px-5 pb-5 border-t border-gray-100 pt-4 space-y-3">
              {Utils.isJSON(authzResult) ? (
                (() => {
                  const parsed = JSON.parse(authzResult);
                  return (
                    <>
                      <div className="flex items-center gap-3">
                        <span className="text-sm font-medium text-gray-700">Decision</span>
                        {parsed.decision ? (
                          <span className="text-green-600 text-xl font-bold">True</span>
                        ) : (
                          <span className="text-red-500 text-xl font-bold">False</span>
                        )}
                      </div>
                      <JsonEditor data={parsed} rootName="result" viewOnly={true} />
                    </>
                  );
                })()
              ) : (
                <textarea
                  className="w-full h-48 px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:outline-none"
                  value={authzResult}
                  readOnly
                />
              )}
              <button
                onClick={() => setAuthzResult('')}
                className="text-sm text-green-600 hover:text-green-700 font-medium"
              >
                Reset
              </button>
            </div>
          </details>
        </div>
      )}

      {/* Logs */}
      {!!authzLogs && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <details>
            <summary className="px-5 py-4 font-semibold text-gray-800 text-sm cursor-pointer hover:bg-gray-50 transition-colors">
              Cedarling Authz Logs
            </summary>
            <div className="px-5 pb-5 border-t border-gray-100 pt-4 space-y-3">
              <textarea
                className="w-full h-48 px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:outline-none"
                value={authzLogs}
                readOnly
              />
              <button
                onClick={() => setAuthzLogs('')}
                className="text-sm text-green-600 hover:text-green-700 font-medium"
              >
                Reset
              </button>
            </div>
          </details>
        </div>
      )}
    </div>
  );
}