// MultiIssuerAuthzForm.tsx
import React from 'react';
import { JsonEditor } from 'json-edit-react';
import { Copy, HelpCircle } from 'lucide-react';
import initWasm, {
  init,
  Cedarling,
  MultiIssuerAuthorizeResult,
} from '@janssenproject/cedarling_wasm';
import { SuccessAlert } from '../../../shared/components/Common';
import Utils from '../../../options/Utils';

interface CedarlingMultiIssuerAuthzProps {
  data: any;
}

type TokenObj = { mapping: string; payload: string };
type TokenSelection = { accessToken: boolean; userInfo: boolean; idToken: boolean };
type FormFields = {
  tokens: TokenObj[];
  action: string;
  context: Record<string, unknown>;
  resource: Record<string, unknown>;
};

export default function MultiIssuerAuthzForm({ data }: CedarlingMultiIssuerAuthzProps) {
  const [logType, setLogType] = React.useState('Decision');
  const [authzResult, setAuthzResult] = React.useState('');
  const [authzLogs, setAuthzLogs] = React.useState('');
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  const [uiMessage, setUiMessage] = React.useState('');
  const [expanded, setExpanded] = React.useState(true);
  const [formFields, setFormFields] = React.useState<FormFields>({
    tokens: [],
    action: '',
    context: {},
    resource: {},
  });
  const [tokenSelection, setTokenSelection] = React.useState<TokenSelection>({
    accessToken: false,
    userInfo: false,
    idToken: false,
  });
  const [loginDetails, setLoginDetails] = React.useState<{
    access_token?: string;
    id_token?: string;
    userDetails?: string;
  } | null>(null);

  React.useEffect(() => {
    setLoginDetails(data?.loginDetails ?? null);
  }, [data?.loginDetails]);

  React.useEffect(() => {
    chrome.storage.local.get(['multiIssueAuthz'], (result) => {
      if (result?.multiIssueAuthz) {
        setFormFields({
          tokens: result.multiIssueAuthz.tokens ?? [],
          action: result.multiIssueAuthz.action ?? '',
          context: result.multiIssueAuthz.context ?? {},
          resource: result.multiIssueAuthz.resource ?? {},
        });
      }
    });
  }, []);

  const triggerCedarlingAuthzRequest = async () => {
    setUiMessage('');
    setAuthzResult('');
    setAuthzLogs('');
    setIsSubmitting(true);
    const reqObj = await createCedarlingAuthzRequestObj();
    chrome.storage.local.get(['cedarlingConfig'], async (cedarlingConfig) => {
      let instance: Cedarling | null = null;
      try {
        const config = cedarlingConfig?.cedarlingConfig?.[0];
        if (!config) {
          setAuthzResult('Error: No Cedarling configuration found.');
          setUiMessage('No Cedarling configuration found. Add a bootstrap configuration first.');
          setIsSubmitting(false);
          return;
        }
        await initWasm();
        instance = await init(config);
        const result: MultiIssuerAuthorizeResult = await instance.authorize_multi_issuer(reqObj);
        setAuthzResult(result.json_string());
        try {
          const logs = await instance.get_logs_by_request_id_and_tag(result.request_id, logType);
          if (logs.length !== 0) {
            setAuthzLogs(logs.map((log: unknown) => JSON.stringify(log, null, 2)).join('\n'));
          }
        } catch (logErr) {
          setAuthzLogs(`Failed to fetch logs: ${String(logErr)}`);
        }
      } catch (err: unknown) {
        setAuthzResult(String(err));
        if (instance) {
          const logs = await instance.pop_logs();
          if (logs.length !== 0) {
            setAuthzLogs(logs.map((log: unknown) => JSON.stringify(log, null, 2)).join('\n'));
          }
        }
        setUiMessage('Authorization failed. Check the error output.');
      } finally {
        setIsSubmitting(false);
      }
    });
  };

  const addTokens = async () => {
    const tokenAliasMap = {
      accessToken: 'AccessToken_namespace::Access_token_entity',
      userInfo: 'UserInfoToken_namespace::Userinfo_entity',
      idToken: 'IDToken_namespace::Id_token_entity',
    };
    setFormFields((prev) => {
      let updatedTokens = [...prev.tokens];
      const tokenPayloadMap = {
        accessToken: loginDetails?.access_token,
        idToken: loginDetails?.id_token,
        userInfo: loginDetails?.userDetails,
      };
      (Object.keys(tokenAliasMap) as (keyof typeof tokenAliasMap)[]).forEach((key) => {
        const mapping = tokenAliasMap[key];
        if (tokenSelection[key]) {
          const payload = tokenPayloadMap[key];
          if (!payload) return;
          const nextToken: TokenObj = {
            mapping,
            payload: typeof payload === 'string' ? payload : JSON.stringify(payload),
          };
          const index = updatedTokens.findIndex((t) => t.mapping === mapping);
          if (index >= 0) updatedTokens[index] = nextToken;
          else updatedTokens.push(nextToken);
        } else {
          updatedTokens = updatedTokens.filter((t) => t.mapping !== mapping);
        }
      });
      return { ...prev, tokens: updatedTokens };
    });
  };

  const createCedarlingAuthzRequestObj = async () => {
    const reqObj = {
      tokens: formFields.tokens,
      action: formFields.action,
      context: formFields.context,
      resource: formFields.resource,
    };
    chrome.storage.local.set({ multiIssueAuthz: reqObj });
    return reqObj;
  };

  const resetInputs = () => {
    setUiMessage('');
    setFormFields({ tokens: [], action: '', context: {}, resource: {} });
    setTokenSelection({
      accessToken: false,
      userInfo: false,
      idToken: false,
   });
    chrome.storage.local.remove('multiIssueAuthz');
  };

  const canSubmit = React.useMemo(() => {
    const hasAction = !!formFields.action?.trim();
    const hasTokens = Array.isArray(formFields.tokens) && formFields.tokens.length > 0;
    const hasResource = !!formFields.resource && Object.keys(formFields.resource).length > 0;
    return hasAction && hasTokens && hasResource && !isSubmitting;
  }, [formFields.action, formFields.tokens, formFields.resource, isSubmitting]);

  const copyText = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setUiMessage('Copied to clipboard.');
    } catch {
      setUiMessage('Copy failed.');
    }
  };

  if (!data || data?.length === 0) return null;

  const logTabs = ['Decision', 'System', 'Metric'];

  return (
    <div className="space-y-4">
      {/* ── Main card ── */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">

        {/* Card header */}
        <div className="px-6 py-5 border-b border-gray-100">
          <div className="flex items-start justify-between">
            <div>
              <h2 className="text-xl font-bold text-gray-900">
                Cedarling Multi-Issuer Authorization
              </h2>
              <p className="text-sm text-gray-500 mt-1">
                Build an authz request from tokens, action, resource, and context — then run
                authorization.
              </p>
            </div>
            <span
              className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border ${isSubmitting
                ? 'border-amber-300 text-amber-700 bg-amber-50'
                : 'border-green-300 text-green-700 bg-green-50'
                }`}
            >
              {isSubmitting ? 'Running...' : 'Ready'}
            </span>
          </div>
        </div>

        {/* Request builder accordion */}
        <div>
          <button
            onClick={() => setExpanded(!expanded)}
            className="w-full flex items-center justify-between px-6 py-4 text-left hover:bg-gray-50 transition-colors"
          >
            <span className="font-semibold text-gray-800 text-sm">Request builder</span>
            <span className="flex items-center justify-center w-7 h-7 bg-gray-900 text-white rounded-full text-lg leading-none select-none">
              {expanded ? '−' : '+'}
            </span>
          </button>

          {expanded && (
            <div className="px-6 pb-6 space-y-5 border-t border-gray-100 pt-4">

              {/* Info bar */}
              <div className="flex items-start gap-2 px-4 py-3 bg-blue-50 border border-blue-100 rounded-lg text-sm text-blue-800">
                <SuccessAlert>
                  <span>
                    Required:
                    <span className="font-medium">tokens + action + resource</span>
                    {' '}Add at least 1 token mapping, an action, and a non-empty resource.
                  </span>
                </SuccessAlert>
              </div>

              {/* ── Tokens from auth flow (shown only when login tokens exist) ── */}
              {(loginDetails?.access_token ||
                loginDetails?.id_token ||
                loginDetails?.userDetails) && (
                  <div>
                    <p className="text-sm font-medium text-gray-700 mb-2">
                      Add tokens from Auth Flow
                    </p>
                    <div className="flex flex-wrap gap-4 mb-3">
                      {(['accessToken', 'userInfo', 'idToken'] as const).map((key) => {
                        const labels: Record<typeof key, string> = {
                          accessToken: 'Access Token',
                          userInfo: 'Userinfo Token',
                          idToken: 'ID Token',
                        };
                        return (
                          <label key={key} className="flex items-center gap-2 cursor-pointer">
                            <input
                              type="checkbox"
                              checked={tokenSelection[key]}
                              onChange={() =>
                                setTokenSelection((prev) => ({ ...prev, [key]: !prev[key] }))
                              }
                              className="w-4 h-4 accent-green-600 rounded"
                            />
                            <span className="text-sm text-gray-700">{labels[key]}</span>
                          </label>
                        );
                      })}
                    </div>
                    <button
                      onClick={addTokens}
                      className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white text-sm font-medium rounded-full transition-colors"
                    >
                      Add selected tokens to mapping
                    </button>
                  </div>
                )}

              {/* ── Issuer-to-Token Mapping ── */}
              <div>
                <div className="flex items-center gap-1.5 mb-2">
                  <label className="text-sm font-medium text-gray-700">
                    Issuer-to-Token Mapping:
                  </label>
                  <HelpCircle size={14} className="text-pink-500" />
                </div>
                <div className="border border-gray-200 rounded-lg p-3">
                  <JsonEditor
                    data={formFields.tokens}
                    setData={(e: any) => setFormFields((prev) => ({ ...prev, tokens: e }))}
                    rootName="tokens"
                  />
                </div>
              </div>

              {/* ── Action ── */}
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

              {/* ── Resource ── */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Resource:</label>
                <div className="border border-gray-200 rounded-lg p-3">
                  <JsonEditor
                    data={formFields.resource}
                    rootName="resource"
                    setData={(e: unknown) =>
                      setFormFields((prev) => ({
                        ...prev,
                        resource: (e ?? {}) as Record<string, unknown>,
                      }))
                    }
                  />
                </div>
              </div>

              {/* ── Context ── */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Context:</label>
                <div className="border border-gray-200 rounded-lg p-3">
                  <JsonEditor
                    data={formFields.context}
                    setData={(e: unknown) =>
                      setFormFields((prev) => ({
                        ...prev,
                        context: (e ?? {}) as Record<string, unknown>,
                      }))
                    }
                    rootName="context"
                  />
                </div>
              </div>

              {/* ── Log tag tabs ── */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Log tag</label>
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

              {/* Inline status message */}
              {uiMessage && (
                <div className="flex items-start gap-2 px-4 py-3 bg-blue-50 border border-blue-100 rounded-lg text-sm text-blue-800">
                  <span>ℹ {uiMessage}</span>
                </div>
              )}

              {/* ── Form actions ── */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <button
                    onClick={triggerCedarlingAuthzRequest}
                    disabled={!canSubmit}
                    className="px-5 py-2.5 bg-green-600 hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-sm font-medium rounded-lg transition-colors"
                  >
                    {isSubmitting ? 'Running...' : 'Run authorization'}
                  </button>
                  <button
                    onClick={resetInputs}
                    disabled={isSubmitting}
                    className="px-5 py-2.5 border border-gray-300 text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed text-sm font-medium rounded-lg hover:bg-gray-50 transition-colors"
                  >
                    Reset
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Result card ── */}
      {!!authzResult && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <details open>
            <summary className="px-6 py-4 cursor-pointer hover:bg-gray-50 transition-colors border-b border-gray-100 list-none">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-gray-800 text-sm">Result</span>
                <button
                  onClick={(e) => {
                    e.preventDefault();
                    copyText(authzResult);
                  }}
                  aria-label="Copy result"
                  className="p-1 text-gray-500 hover:text-gray-700 transition-colors"
                  title="Copy result"
                >
                  <Copy size={14} aria-hidden="true" />
                </button>
              </div>
            </summary>
            <div className="px-6 pb-5 pt-4 space-y-3">
              {Utils.isJSON(authzResult) ? (
                <>
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-medium text-gray-700">Decision</span>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${JSON.parse(authzResult).decision
                        ? 'bg-green-100 text-green-700'
                        : 'bg-red-100 text-red-700'
                        }`}
                    >
                      {JSON.parse(authzResult).decision ? 'True' : 'False'}
                    </span>
                  </div>
                  <JsonEditor
                    data={JSON.parse(authzResult)}
                    rootName="result"
                    viewOnly={true}
                  />
                </>
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
                Clear result
              </button>
            </div>
          </details>
        </div>
      )}

      {/* ── Logs card ── */}
      {!!authzLogs && (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <details>
            <summary className="px-6 py-4 cursor-pointer hover:bg-gray-50 transition-colors border-b border-gray-100 list-none">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-gray-800 text-sm">Logs</span>
                <button
                  onClick={(e) => {
                    e.preventDefault();
                    copyText(authzLogs);
                  }}
                  aria-label="Copy logs"
                  className="p-1 text-gray-500 hover:text-gray-700 transition-colors"
                  title="Copy logs"
                >
                  <Copy size={14} aria-hidden="true" />
                </button>
              </div>
            </summary>
            <div className="px-6 pb-5 pt-4 space-y-3">
              <textarea
                className="w-full h-56 px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:outline-none"
                value={authzLogs}
                readOnly
              />
              <button
                onClick={() => setAuthzLogs('')}
                className="text-sm text-green-600 hover:text-green-700 font-medium"
              >
                Clear logs
              </button>
            </div>
          </details>
        </div>
      )}
    </div>
  );
}