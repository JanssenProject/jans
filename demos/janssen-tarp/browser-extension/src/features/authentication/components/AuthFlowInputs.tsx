import * as React from 'react';
import qs from 'qs';
import axios from 'axios';
import Utils from '../../../options/Utils';
import { v4 as uuidv4 } from 'uuid';
import { ILooseObject } from '../../../shared/types';
import { ClientDetails } from '../type/Authentication';
import { Spinner } from '../../../shared/components/Common';
import { MultiSelectDropdown } from '../../../shared/components/multiSelect/MultiSelectDropdown';
import { Dropdown } from '../../../shared/components/Dropdown';
import { LabelWithTooltip } from '../../../shared/components/Common';
// ── Types ──────────────────────────────────────────────────────────────────────

type Option = { name: string; label?: string; create?: boolean };

type AuthFlowInputsProps = {
  isOpen: boolean;
  handleDialog: (isOpen: boolean) => void;
  client: ClientDetails;
  notifyOnDataChange: () => void;
};

// ── Main Component ─────────────────────────────────────────────────────────────

export default function AuthFlowInputs({
  isOpen,
  handleDialog,
  client,
  notifyOnDataChange,
}: AuthFlowInputsProps) {
  const [errorMessage, setErrorMessage] = React.useState('');
  const [additionalParamError, setAdditionalParamError] = React.useState('');
  const [displayToken, setDisplayToken] = React.useState(false);
  const [additionalParams, setAdditionalParams] = React.useState(client.additionalParams ?? '');
  const [loading, setLoading] = React.useState(false);
  const [acrValueOptions, setAcrValueOptions] = React.useState<Option[]>([]);
  const [selectedAcr, setSelectedAcr] = React.useState<Option | null>(null);
  const [selectedScopes, setSelectedScopes] = React.useState<Option[]>([]);
  const [scopeOptions, setScopeOptions] = React.useState<Option[]>([]);

  // Populate options from client
  React.useEffect(() => {
    const scopes = String(client?.scope ?? '').split(' ').filter(Boolean);
    const acr = Array.isArray(client?.acrValuesSupported) ? client.acrValuesSupported : [];
    setAcrValueOptions(acr.map((s: string) => ({ name: s })));
    setScopeOptions(scopes.map((s) => ({ name: s })));
  }, []);

  const handleClose = () => {
    handleDialog(false);
    setErrorMessage('');
  };

  // ── Validation ─────────────────────────────────────────────────────────────

  const validateJson = (value: string) => {
    setAdditionalParamError('');
    if (!value.trim()) return true;
    try {
      const parsed = JSON.parse(value);
      if (
        parsed === null ||
        Array.isArray(parsed) ||
        typeof parsed !== 'object'
      ) {
        setAdditionalParamError('Additional params must be a JSON object.');
        return false;
      }
      return true;
    } catch {
      setAdditionalParamError('Error in parsing JSON.');
      return false;
    }
  };

  // ── Auth flow ──────────────────────────────────────────────────────────────

  const triggerCodeFlow = async () => {
    if (!validateJson(additionalParams)) return;

    try {
      setLoading(true);
      setErrorMessage('');

      const redirectUrl = Array.isArray(client?.redirectUris) ? client.redirectUris[0] : undefined;
      const { secret, hashed } = await Utils.generateRandomChallengePair();

      let scopes = selectedScopes.map((s) => s.name).join(' ');
      if (!scopes) scopes = String(client?.scope ?? '');

      const options: ILooseObject = {
        scope: scopes,
        response_type: Array.isArray(client?.responseType) ? client.responseType[0] : undefined,
        redirect_uri: redirectUrl,
        client_id: client?.clientId,
        code_challenge_method: 'S256',
        code_challenge: hashed,
        nonce: uuidv4(),
      };

      if (selectedAcr) options.acr_values = selectedAcr.name;

      let authzUrl = `${client?.authorizationEndpoint}?${qs.stringify(options)}`;

      if (additionalParams.trim()) {
        const updatedClient = { ...client, additionalParams: additionalParams.trim() };
        chrome.storage.local.get(['oidcClients'], (result: { oidcClients?: any[] }) => {
          if (result.oidcClients) {
            const clientArr = result.oidcClients.map((obj) =>
              obj.clientId === updatedClient.clientId
                ? { ...obj, additionalParams: updatedClient.additionalParams }
                : obj
            );
            chrome.storage.local.set({ oidcClients: clientArr });
          }
        });

        const parsed = JSON.parse(additionalParams);
        Object.keys(parsed).forEach((key) => {
          authzUrl += `&${encodeURIComponent(key)}=${encodeURIComponent(parsed[key])}`;
        });
      }

      const resultUrl: string = await new Promise((resolve, reject) => {
        chrome.identity.launchWebAuthFlow({ url: authzUrl, interactive: true }, (responseUrl) => {
          if (chrome.runtime.lastError || !responseUrl)
            reject(new Error(chrome.runtime.lastError?.message || 'No redirect URL'));
          else resolve(responseUrl);
        });
      });

      if (resultUrl) {
        const urlParams = new URLSearchParams(new URL(resultUrl).search);
        const code = urlParams.get('code');
        const errorDesc = urlParams.get('error_description');
        if (errorDesc) throw new Error(errorDesc);
        if (!code) throw new Error('Error in authentication. The authorization-code is null.');

        const tokenReqData = qs.stringify({
          redirect_uri: redirectUrl,
          grant_type: 'authorization_code',
          code_verifier: secret,
          client_id: client?.clientId,
          code,
          scope: scopes,
        });

        const tokenResponse = await axios({
          method: 'POST',
          headers: {
            'content-type': 'application/x-www-form-urlencoded',
            Authorization: 'Basic ' + btoa(`${client?.clientId}:${client?.clientSecret}`),
          },
          data: tokenReqData,
          url: client.tokenEndpoint,
        });

        if (!tokenResponse?.data?.access_token) {
          throw new Error(
            `Error in authentication. Token response does not contain access_token. ${tokenResponse?.data?.error_description || tokenResponse?.data?.error || ''}`
          );
        }

        const userInfoResponse = await axios({
          method: 'GET',
          headers: { Authorization: `Bearer ${tokenResponse.data.access_token}` },
          url: client.userinfoEndpoint,
        });

        await chrome.storage.local.set({
          loginDetails: {
            access_token: tokenResponse.data.access_token,
            userDetails: userInfoResponse.data,
            id_token: tokenResponse.data.id_token,
            displayToken,
          },
        });

        notifyOnDataChange();
        handleClose();
      }
    } catch (err) {
      console.error(err);
      setErrorMessage(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  };

  // ── Render ─────────────────────────────────────────────────────────────────

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-[2px]" />

      {/* Modal */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-flow-dialog-title"
        className="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 p-8"
      >

        {loading && <Spinner />}

        {/* Close */}
        <button
          type="button"
          onClick={handleClose}
          className="absolute top-5 right-5 text-slate-400 hover:text-slate-700 transition-colors"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} className="w-5 h-5">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>

        {/* Title */}
        <h2 className="text-2xl font-bold text-[#1a3a2a] leading-tight mb-1">
          Authentication Flow Inputs
        </h2>
        <p className="text-slate-500 text-sm mb-6">
          Enter inputs (optional) before initiating authentication flow.
        </p>

        {/* Error alert */}
        {errorMessage && (
          <div className="mb-5 flex items-start gap-2 border border-red-300 bg-red-50 text-red-800 rounded-lg px-4 py-3 text-sm">
            <span className="font-bold mt-0.5">✕</span>
            <span>{errorMessage}</span>
          </div>
        )}

        <div className="flex flex-col gap-5">

          {/* ── Additional Params ── */}
          <div>
            <label className="block mb-1.5">
              <LabelWithTooltip
                label="Additional Params:"
                tip='Optional JSON key/value pairs that will be added to the authorization request (as query params). Example: {"prompt":"login","max_age":300}'
              />
            </label>
            <input
              type="text"
              autoFocus
              defaultValue={client.additionalParams ?? ''}
              placeholder='e.g. {"paramOne": "valueOne", "paramTwo": "valueTwo"}'
              onBlur={(e) => {
                const val = e.target.value;
                if (validateJson(val)) setAdditionalParams(val);
              }}
              className={`w-full border rounded-lg px-4 py-3 text-sm text-slate-700 placeholder-slate-400
                focus:outline-none focus:ring-2 focus:border-[#1a6b3c] transition-all bg-slate-50/60
                ${additionalParamError
                  ? 'border-red-400 focus:ring-red-200'
                  : 'border-slate-200 focus:ring-[#1a6b3c]/30'
                }`}
            />
            {additionalParamError && (
              <p className="mt-1.5 text-xs text-red-600">{additionalParamError}</p>
            )}
          </div>

          {/* ── Acr Values ── */}
          <div>
            <label className="block mb-1.5">
              <LabelWithTooltip
                label="Acr Values:"
                tip="Optional. Requested ACR value(s) for authentication (e.g. urn:mace:incommon:iap:silver). Typically only one is used."
              />
            </label>
            <Dropdown
              options={acrValueOptions}
              selected={selectedAcr}
              onSelect={setSelectedAcr}
              placeholder='Select ACR value'
            />
          </div>

          {/* ── Scope ── */}
          <div>
            <label className="block mb-1.5">
              <LabelWithTooltip
                label="Scope:"
                tip="Optional. Scopes to request in the authorization flow (e.g. openid, profile, email). If omitted, the client's default scopes are used."
              />
            </label>
            <MultiSelectDropdown
              options={scopeOptions}
              selected={selectedScopes}
              onChange={setSelectedScopes}
              placeholder="Select scopes"
            />
            <p className="mt-1 text-xs text-slate-400">Type scope and Press Enter to add a scope.</p>
          </div>

          {/* ── Display tokens checkbox ── */}
          <label className="flex items-center gap-3 cursor-pointer select-none group">
            <input
              type="checkbox"
              checked={displayToken}
              onChange={(e) => setDisplayToken(e.target.checked)}
              className="sr-only peer"
            />
            <span
              aria-hidden="true"
              className={`w-5 h-5 flex-shrink-0 rounded flex items-center justify-center border-2 transition-colors
                ${displayToken
                  ? 'bg-[#22a05a] border-[#22a05a]'
                  : 'bg-white border-slate-300 group-hover:border-[`#22a05a`] peer-focus:ring-2 peer-focus:ring-[`#1a6b3c`]/30'
                }`}
            >
              {displayToken && (
                <svg viewBox="0 0 12 10" fill="none" className="w-3 h-3">
                  <path d="M1 5l3.5 3.5L11 1" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              )}
            </span>
            <span className="flex items-center gap-1.5 text-sm font-medium text-[#1a3a2a]">
              Display tokens after authentication
              <span className="relative inline-flex items-center">
                {/* Inline tooltip for checkbox label */}
                <span className="group/tip relative">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}
                    className="w-4 h-4 text-slate-400 hover:text-slate-600 transition-colors cursor-pointer">
                    <circle cx="12" cy="12" r="10" />
                    <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
                    <line x1="12" y1="17" x2="12.01" y2="17" />
                  </svg>
                  <span className="absolute z-50 left-6 top-1/2 -translate-y-1/2 w-64 bg-[#1a3a2a] text-white text-xs
                    rounded-lg px-3 py-2.5 shadow-xl leading-relaxed pointer-events-none
                    opacity-0 group-hover/tip:opacity-100 transition-opacity">
                    If enabled, the UI will show the access token, user-info and ID token after a successful login (useful for debugging).
                  </span>
                </span>
              </span>
            </span>
          </label>
        </div>

        {/* ── Actions ── */}
        <div className="mt-8 flex items-center gap-3">
          <button
            type="button"
            onClick={triggerCodeFlow}
            disabled={loading}
            className="px-6 py-2.5 text-sm font-semibold text-white bg-[#22a05a]
              hover:bg-[#1a8a4a] active:bg-[#167a40] disabled:opacity-60 disabled:cursor-not-allowed
              rounded-lg transition-colors shadow-sm shadow-green-200"
          >
            Trigger Auth Flow
          </button>
          <button
            type="button"
            onClick={handleClose}
            className="px-5 py-2.5 text-sm font-semibold text-slate-600 border border-slate-300
              rounded-lg hover:bg-slate-50 active:bg-slate-100 transition-colors"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}