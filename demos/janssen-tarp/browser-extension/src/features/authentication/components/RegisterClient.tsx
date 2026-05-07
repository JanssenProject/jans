import * as React from 'react';
import { v4 as uuidv4 } from 'uuid';
import axios from 'axios';
import { RegistrationRequest, OIDCClient, OpenIDConfiguration } from '../../../shared/types';
import type { Moment } from 'moment';
import moment from 'moment';
import { Spinner } from '../../../shared/components/Common';
import { LabelWithTooltip } from '../../../shared/components/Common';
// ── Types ─────────────────────────────────────────────────────────────────────

type ScopeOption = { id?: string; name: string; label?: string; create?: boolean };
type AlertSeverity = 'success' | 'error' | 'warning' | 'info';

type RegisterClientProps = {
  isOpen: boolean;
  handleDialog: (isOpen: boolean) => void;
};

// ── Sub-components ─────────────────────────────────────────────────────────────

/** Alert banner */
const AlertBanner = ({ severity, message }: { severity: AlertSeverity; message: string }) => {
  const styles: Record<AlertSeverity, string> = {
    success: 'bg-emerald-50 border-emerald-300 text-emerald-800',
    error: 'bg-red-50   border-red-300   text-red-800',
    warning: 'bg-amber-50 border-amber-300 text-amber-800',
    info: 'bg-blue-50  border-blue-300  text-blue-800',
  };
  const icons: Record<AlertSeverity, string> = {
    success: '✓', error: '✕', warning: '⚠', info: 'ℹ',
  };
  return (
    <div className={`flex items-start gap-2 border rounded-lg px-4 py-3 text-sm ${styles[severity]}`}>
      <span className="font-bold mt-0.5">{icons[severity]}</span>
      <span>{message}</span>
    </div>
  );
};

/** Scope tag pill */
const Tag = ({ name, onRemove }: { name: string; onRemove: () => void }) => (
  <span className="inline-flex items-center gap-1 bg-[#d1fae5] text-[#065f46] text-xs font-semibold px-2.5 py-1 rounded-full">
    {name}
    <button
      type="button"
      onClick={onRemove}
      className="ml-0.5 hover:text-red-500 transition-colors leading-none"
    >
      ×
    </button>
  </span>
);

// ── Main Component ─────────────────────────────────────────────────────────────

export default function RegisterClient({ isOpen, handleDialog }: RegisterClientProps) {
  const [selectedScopes, setSelectedScopes] = React.useState<ScopeOption[]>([{ name: 'openid' }]);
  const [scopeInput, setScopeInput] = React.useState('');
  const [expireAt, setExpireAt] = React.useState<Moment | null>(null);
  const [issuer, setIssuer] = React.useState<string | null>(null);
  const [clientId, setClientId] = React.useState<string | null>(null);
  const [clientSecret, setClientSecret] = React.useState<string | null>(null);
  const [issuerError, setIssuerError] = React.useState('');
  const [alert, setAlert] = React.useState('');
  const [alertSeverity, setAlertSeverity] = React.useState<AlertSeverity>('success');
  const [loading, setLoading] = React.useState(false);
  const [addExistingClient, setAddExistingClient] = React.useState(false);
  const REGISTRATION_ERROR = 'Error in registration. Check web console for logs.';
  const [redirectUrlCopied, setRedirectUrlCopied] = React.useState(false);


  // Reset state when dialog opens
  React.useEffect(() => {
    if (isOpen) {
      setIssuerError('');
      setAlert('');
      setLoading(false);
      setSelectedScopes([{ name: 'openid' }]);
      setExpireAt(null);
      setIssuer(null);
      setClientId(null);
      setClientSecret(null);
      setAddExistingClient(false);
    }
  }, [isOpen]);

  const handleClose = () => {
    setAddExistingClient(false);
    handleDialog(false);
  };

  // ── Helpers ────────────────────────────────────────────────────────────────

  const generateOpenIdConfigurationURL = (raw: string): string => {
    let url = raw.trim();
    if (!url.includes('http')) url = 'https://' + url;
    if (!url.includes('/.well-known/openid-configuration')) {
      url = url.replace(/\/$/, '') + '/.well-known/openid-configuration';
    }
    return url;
  };

  const getOpenidConfiguration = async (endpoint: string) => {
    try {
      setAlert('');
      return await axios({ method: 'GET', url: endpoint });
    } catch (err) {
      console.error(err);
    }
  };

  const registerOIDCClient = async (endpoint: string, payload: RegistrationRequest) => {
    try {
      setAlert('');
      return await axios({
        method: 'POST',
        url: endpoint,
        headers: { 'content-type': 'application/json' },
        data: JSON.stringify(payload),
      });
    } catch (err) {
      console.error('Error registering OIDC client:', err);
      setAlert('Error in registering OIDC client. Check error log on console.');
      setAlertSeverity('error');
    }
  };

  const copyRedirectUrl = async () => {
    try {
      await navigator.clipboard.writeText(chrome.identity.getRedirectURL());
      setRedirectUrlCopied(true);
      setTimeout(() => setRedirectUrlCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy redirect URL:', err);
    }
  };

  // ── Issuer validation (on blur) ────────────────────────────────────────────

  const validateIssuer = async (e: React.FocusEvent<HTMLInputElement>) => {
    setIssuerError('');
    const raw = e.target.value.trim();
    if (!raw) return;

    setLoading(true);
    const configUrl = generateOpenIdConfigurationURL(raw);
    try {
      const resp = await getOpenidConfiguration(configUrl);
      if (!resp?.data?.issuer) {
        setIssuerError('Invalid input. Either enter correct Issuer or OpenID Configuration URL.');
        e.target.value = '';
      } else {
        setIssuer(configUrl);
      }
    } catch {
      setIssuerError('Invalid input. Either enter correct Issuer or OpenID Configuration URL.');
      e.target.value = '';
    }
    setLoading(false);
  };

  // ── Scope management ───────────────────────────────────────────────────────

  const addScope = (name: string) => {
    const trimmed = name.trim();
    if (!trimmed) return;
    if (selectedScopes.some((s) => s.name === trimmed)) return;
    setSelectedScopes((prev) => [...prev, { name: trimmed }]);
  };

  const removeScope = (name: string) => {
    setSelectedScopes((prev) => prev.filter((s) => s.name !== name));
  };

  const handleScopeKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      addScope(scopeInput);
      setScopeInput('');
    }
  };

  // ── Storage helpers ────────────────────────────────────────────────────────

  const storeOpenIDConfiguration = (config: OpenIDConfiguration): Promise<void> =>
    new Promise((resolve, reject) => {
      chrome.storage.local.get(['openidConfigurations'], (result) => {
        if (chrome.runtime.lastError) { reject(chrome.runtime.lastError); return; }
        const configs: OpenIDConfiguration[] = result.openidConfigurations || [];
        const idx = configs.findIndex((c) => c.issuer === config.issuer);
        if (idx >= 0) configs[idx] = config; else configs.push(config);
        chrome.storage.local.set({ openidConfigurations: configs }, () => {
          chrome.runtime.lastError ? reject(chrome.runtime.lastError) : resolve();
        });
      });
    });

  const storeOIDCClient = (client: OIDCClient): Promise<void> =>
    new Promise((resolve, reject) => {
      chrome.storage.local.get(['oidcClients'], (result) => {
        if (chrome.runtime.lastError) { reject(chrome.runtime.lastError); return; }
        const clients: OIDCClient[] = result.oidcClients || [];
        const idx = clients.findIndex((c) => c.clientId === client.clientId && c.opHost === client.opHost);
        if (idx >= 0) clients[idx] = client; else clients.push(client);
        clients.sort((a, b) => b.registrationDate - a.registrationDate);
        chrome.storage.local.set({ oidcClients: clients }, () => {
          chrome.runtime.lastError ? reject(chrome.runtime.lastError) : resolve();
        });
      });
    });

  const resolveOpenIDConfig = async (issuerValue: string) => {
    let opConfigurationEndpoint: string;
    let issuerUrl: string;
    if (issuerValue.includes('.well-known/openid-configuration')) {
      opConfigurationEndpoint = issuerValue;
      issuerUrl = issuerValue.replace(/\/?\.well-known\/openid-configuration\/?$/, '');
    } else {
      issuerUrl = issuerValue.replace(/\/$/, '');
      opConfigurationEndpoint = `${issuerUrl}/.well-known/openid-configuration`;
    }
    const openidConfig = await getOpenidConfiguration(opConfigurationEndpoint);
    if (!openidConfig?.data) {
      setAlert('Error in fetching OpenID configuration!'); setAlertSeverity('error');
      return null;
    }
    return { issuerUrl, configData: openidConfig.data };
  };

  // ── Register ───────────────────────────────────────────────────────────────

  const addClient = async (): Promise<void> => {
    try {
      setLoading(true);

      if (!issuer) {
        setIssuerError('Issuer cannot be left blank. Either enter correct Issuer or OpenID Configuration URL.');
        return;
      }

      if (addExistingClient && !clientId?.trim()) {
        setAlert('Client ID is required when adding an existing client.');
        setAlertSeverity('error');
        return;
      }

      if (addExistingClient && !clientSecret?.trim()) {
        setAlert('Client Secret is required when adding an existing client.');
        setAlertSeverity('error');
        return;
      }

      const resolved = await resolveOpenIDConfig(issuer);
      if (!resolved) return;
      const { issuerUrl, configData } = resolved;

      const scopes = selectedScopes.map((s) => s.name).join(' ');

      await storeOpenIDConfiguration(configData);

      const newClient: OIDCClient = {
        id: uuidv4(),
        opHost: issuerUrl,
        clientId: clientId,
        clientSecret: clientSecret,
        scope: scopes,
        redirectUris: [chrome.identity.getRedirectURL()],
        authorizationEndpoint: configData.authorization_endpoint,
        tokenEndpoint: configData.token_endpoint,
        userinfoEndpoint: configData.userinfo_endpoint,
        acrValuesSupported: configData.acr_values_supported,
        endSessionEndpoint: configData.end_session_endpoint,
        responseType: ['code'],
        postLogoutRedirectUris: [chrome.identity.getRedirectURL('logout')],
        expireAt: expireAt ? expireAt.valueOf() : undefined,
        showClientExpiry: !!expireAt,
        registrationDate: Date.now(),
        openidConfiguration: configData,
      };
      await storeOIDCClient(newClient);

      setAlert('Added Client successfully into Tarp!');
      setAlertSeverity('success');
      setAddExistingClient(false);
      handleClose();
    } catch (err) {
      console.error('Error in adding Client into Tarp:', err);
      setAlert(err instanceof Error ? err.message : REGISTRATION_ERROR);
      setAlertSeverity('error');
    } finally {
      setLoading(false);
    }
  };

  const registerClient = async (): Promise<void> => {
    try {
      setLoading(true);

      if (!issuer) {
        setIssuerError('Issuer cannot be left blank. Either enter correct Issuer or OpenID Configuration URL.');
        return;
      }

      const resolved = await resolveOpenIDConfig(issuer);
      if (!resolved) return;
      const { issuerUrl, configData } = resolved;

      const scopes = selectedScopes.map((s) => s.name).join(' ');

      if (!configData.registration_endpoint) {
        setAlert('OpenID configuration does not contain a registration endpoint'); setAlertSeverity('error'); return;
      }

      await storeOpenIDConfiguration(configData);

      const clientId = `janssen-tarp-${uuidv4()}`;

      const registerObj: RegistrationRequest = {
        redirect_uris: [chrome.identity.getRedirectURL()],
        scope: scopes,
        post_logout_redirect_uris: [chrome.identity.getRedirectURL('logout')],
        response_types: ['code'],
        grant_types: ['authorization_code'],
        application_type: 'web',
        client_name: clientId,
        token_endpoint_auth_method: 'client_secret_basic',
        access_token_as_jwt: true,
        userinfo_signed_response_alg: 'RS256',
        jansInclClaimsInIdTkn: 'true',
        access_token_lifetime: 86400,
      };

      if (expireAt) {
        const lifetime = Math.floor((expireAt.valueOf() - Date.now()) / 1000);
        if (lifetime > 0) registerObj.lifetime = lifetime;
      }

      const registrationResp = await registerOIDCClient(configData.registration_endpoint, registerObj);

      if (!registrationResp?.data) {
        setAlert(REGISTRATION_ERROR); setAlertSeverity('error'); return;
      }

      const newClient: OIDCClient = {
        id: uuidv4(),
        opHost: issuerUrl,
        clientId: registrationResp.data.client_id,
        clientSecret: registrationResp.data.client_secret,
        scope: registerObj.scope,
        redirectUris: registerObj.redirect_uris,
        authorizationEndpoint: configData.authorization_endpoint,
        tokenEndpoint: configData.token_endpoint,
        userinfoEndpoint: configData.userinfo_endpoint,
        acrValuesSupported: configData.acr_values_supported,
        endSessionEndpoint: configData.end_session_endpoint,
        responseType: registerObj.response_types,
        postLogoutRedirectUris: registerObj.post_logout_redirect_uris,
        expireAt: expireAt ? expireAt.valueOf() : undefined,
        showClientExpiry: !!expireAt,
        registrationDate: Date.now(),
        openidConfiguration: configData,
      };

      await storeOIDCClient(newClient);

      setAlert('Registration successful!');
      setAlertSeverity('success');
      handleClose();
    } catch (err) {
      console.error('Client registration failed:', err);
      setAlert(err instanceof Error ? err.message : REGISTRATION_ERROR);
      setAlertSeverity('error');
    } finally {
      setLoading(false);
    }
  };

  // ── Render ─────────────────────────────────────────────────────────────────

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-[2px]"
        onClick={handleClose}
      />

      {/* Modal card */}
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 p-8 max-h-[90vh] overflow-y-auto">

        {/* Loading overlay */}
        {loading && <Spinner />}

        {/* Close button */}
        <button
          type="button"
          onClick={handleClose}
          className="absolute top-5 right-5 text-slate-400 hover:text-slate-700 transition-colors"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} className="w-5 h-5">
            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>

        {/* Title */}
        <h2 className="text-2xl font-bold text-[#1a3a2a] mb-1">Register OIDC Client</h2>
        <p className="text-slate-500 text-sm mb-6">Submit below details to create a new OIDC client.</p>

        {/* Alert */}
        {alert && (
          <div className="mb-5">
            <AlertBanner severity={alertSeverity} message={alert} />
          </div>
        )}

        <div className="flex flex-col gap-5">
          {/* ── Info panel ── */}
          {addExistingClient && (
            <div className="bg-blue-50 border border-green-200 rounded-xl p-4">
              <div className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-green-500 flex items-center justify-center flex-shrink-0 mt-0.5">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="white">
                    <path d="M12 2a10 10 0 1 0 0 20A10 10 0 0 0 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
                  </svg>
                </div>
                <div className="text-sm text-green-800 space-y-1">
                  <p className="font-semibold">The OpenID client should have the following configuration to work properly with Tarp:</p>
                  <ul className="space-y-0.5 text-sm">
                    <li>
                      · <strong>Grant Types</strong>: Should include <code>authorization_code</code>.
                    </li>
                    <li>
                      · <strong>Response Types</strong>: Should include <code>code</code>.
                    </li>
                    <li>
                      · <strong>Userinfo Signed Response Algorithm</strong>: Should be set to a valid algorithm (e.g., RS256).
                    </li>
                    <li>
                      · <strong>Access Token as JWT</strong>: Should be set to <code>true</code> so that Tarp can decode the access token and show its claims.
                    </li>
                    <li>
                      · <strong>Jans Include Claims In Id Token</strong>: Can be set to <code>true</code> if required for Cedarling authorization.
                    </li>
                    <li className="flex items-center gap-2 flex-wrap">
                      · <strong>Redirect URIs</strong>: Need to include the Tarp redirect URI in your OIDC client.
                      <button
                        type="button"
                        onClick={copyRedirectUrl}
                        className={`inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded-md border transition-all
      ${redirectUrlCopied
                            ? 'bg-green-100 border-green-400 text-green-700'
                            : 'bg-white border-green-300 text-green-700 hover:bg-green-50'
                          }`}
                      >
                        {redirectUrlCopied ? (
                          <>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5} className="w-3 h-3">
                              <path d="M20 6L9 17l-5-5" strokeLinecap="round" strokeLinejoin="round" />
                            </svg>
                            Copied!
                          </>
                        ) : (
                          <>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} className="w-3 h-3">
                              <rect x="9" y="9" width="13" height="13" rx="2" /><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1" />
                            </svg>
                            Copy Redirect URI
                          </>
                        )}
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          )}

          {/* ── Issuer ── */}
          <div>
            <label htmlFor="issuer" className="block mb-1.5">
              <LabelWithTooltip
                label="Issuer *"
                tip="Your OpenID Provider base URL (or its /.well-known/openid-configuration). We'll discover endpoints and register a new client."
              />
            </label>
            <input
              id="issuer"
              type="text"
              autoFocus
              placeholder="https://your-op.example.com"
              onBlur={validateIssuer}
              className={`w-full border rounded-lg px-4 py-3 text-sm text-slate-700 placeholder-slate-400
                focus:outline-none focus:ring-2 focus:border-[#1a6b3c] transition-all bg-slate-50/60
                ${issuerError
                  ? 'border-red-400 focus:ring-red-200'
                  : 'border-slate-200 focus:ring-[#1a6b3c]/30'
                }`}
            />
            {issuerError && (
              <p className="mt-1.5 text-xs text-red-600">{issuerError}</p>
            )}
          </div>
          {/* ── Scopes ── */}
          <div>
            <label htmlFor="scopes" className="block mb-1.5">
              <LabelWithTooltip
                label="Scopes"
                tip="Requested OAuth/OIDC scopes for this client (e.g. openid, profile, email). Type a scope and press Enter to add it."
              />
            </label>

            {/* Scopes */}
            <div
              className="min-h-[48px] w-full border border-slate-200 rounded-lg px-3 py-2 flex flex-wrap gap-1.5
                focus-within:ring-2 focus-within:ring-[#1a6b3c]/30 focus-within:border-[#1a6b3c]
                transition-all bg-slate-50/60 cursor-text"
              onClick={() => document.getElementById('scopes')?.focus()}
            >
              {selectedScopes.map((s) => (
                <Tag key={s.name} name={s.name} onRemove={() => removeScope(s.name)} />
              ))}
              <input
                id="scopes"
                type="text"
                value={scopeInput}
                onChange={(e) => setScopeInput(e.target.value)}
                onKeyDown={handleScopeKeyDown}
                onBlur={() => { if (scopeInput.trim()) { addScope(scopeInput); setScopeInput(''); } }}
                placeholder={selectedScopes.length === 0 ? 'e.g. openid profile email' : ''}
                className="flex-1 min-w-[120px] bg-transparent text-sm text-slate-700 placeholder-slate-400
                  focus:outline-none py-0.5"
              />
            </div>
            <p className="mt-1 text-xs text-slate-400">Type scope and Press Enter to add a scope.</p>
          </div>
          {/* -- add existing client -- */}
          <label className="flex items-center gap-3 cursor-pointer select-none group">
            <input
              type="checkbox"
              checked={addExistingClient}
              onChange={(e) => {
                setAddExistingClient(e.target.checked);
                if (e.target.checked) setExpireAt(null);
              }}
              className="sr-only peer"
            />
            <span
              aria-hidden="true"
              className={`w-5 h-5 flex-shrink-0 rounded flex items-center justify-center border-2 transition-colors
                ${addExistingClient
                  ? 'bg-[#22a05a] border-[#22a05a]'
                  : 'bg-white border-slate-300 group-hover:border-[`#22a05a`] peer-focus:ring-2 peer-focus:ring-[`#1a6b3c`]/30'
                }`}
            >
              {addExistingClient && (
                <svg viewBox="0 0 12 10" fill="none" className="w-3 h-3">
                  <path d="M1 5l3.5 3.5L11 1" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              )}
            </span>
            <span className="flex items-center gap-1.5 text-sm font-medium text-[#1a3a2a]">
              Add an existing client
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
                    If your OP does not support Dynamic Client Registration, you can manually add an existing client to Tarp.
                  </span>
                </span>
              </span>
            </span>
          </label>
          {addExistingClient && (
            <>
              <div>
                <label htmlFor="clientId" className="block mb-1.5">
                  <LabelWithTooltip
                    label="Client ID *"
                    tip="The client ID of the existing client you want to add."
                  />
                </label>
                <input
                  id="clientId"
                  type="text"
                  onChange={(e) => setClientId(e.target.value)}
                  placeholder="Enter existing client ID"
                  // onBlur={validateIssuer}
                  className={`w-full border rounded-lg px-4 py-3 text-sm text-slate-700 placeholder-slate-400
                focus:outline-none focus:ring-2 focus:border-[#1a6b3c] transition-all bg-slate-50/60`}
                />
              </div>

              <div>
                <label htmlFor="clientSecret" className="block mb-1.5">
                  <LabelWithTooltip
                    label="Client Secret *"
                    tip="The client secret of the existing client you want to add."
                  />
                </label>
                <input
                  id="clientSecret"
                  type="password"
                  onChange={(e) => setClientSecret(e.target.value)}
                  placeholder="Enter existing client secret"
                  className={`w-full border rounded-lg px-4 py-3 text-sm text-slate-700 placeholder-slate-400
                focus:outline-none focus:ring-2 focus:border-[#1a6b3c] transition-all bg-slate-50/60`}
                />
              </div>
            </>
          )}
          {/* ── Client Expiry Date ── */}
          {!addExistingClient && (

            <div>
              <label htmlFor="expiry" className="block mb-1.5">
                <LabelWithTooltip
                  label="Client Expiry Date"
                  tip="Optional. If set, the client will be marked as expired after this date/time."
                />
              </label>
              <div className="relative">
                <input
                  id="expiry"
                  type="datetime-local"
                  min={moment().format('YYYY-MM-DDTHH:mm')}
                  onChange={(e) =>
                    setExpireAt(e.target.value ? moment(e.target.value) : null)
                  }
                  className="w-full border border-slate-200 rounded-lg px-4 py-3 pr-10 text-sm text-slate-700
                  focus:outline-none focus:ring-2 focus:ring-[#1a6b3c]/30 focus:border-[#1a6b3c]
                  transition-all bg-slate-50/60 appearance-none"
                />
              </div>
            </div>
          )}
        </div>
        {/* Alert */}
        {alert && (
          <div className="mb-5 py-5">
            <AlertBanner severity={alertSeverity} message={alert} />
          </div>
        )}
        {/* ── Actions ── */}
        <div className="mt-8 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={handleClose}
            className="px-5 py-2.5 text-sm font-semibold text-slate-600 border border-slate-300
              rounded-lg hover:bg-slate-50 active:bg-slate-100 transition-colors"
          >
            Cancel
          </button>
          {addExistingClient ? (
            <button
              type="button"
              onClick={addClient}
              disabled={loading}
              className="px-6 py-2.5 text-sm font-semibold text-white bg-[#22a05a]
              hover:bg-[#1a8a4a] active:bg-[#167a40] disabled:opacity-60 disabled:cursor-not-allowed
              rounded-lg transition-colors shadow-sm shadow-green-200"
            >
              Add Client
            </button>
          ) : (
            <button
              type="button"
              onClick={registerClient}
              disabled={loading}
              className="px-6 py-2.5 text-sm font-semibold text-white bg-[#22a05a]
              hover:bg-[#1a8a4a] active:bg-[#167a40] disabled:opacity-60 disabled:cursor-not-allowed
              rounded-lg transition-colors shadow-sm shadow-green-200"
            >
              Register
            </button>
          )}
        </div>
      </div >
    </div >
  );
}