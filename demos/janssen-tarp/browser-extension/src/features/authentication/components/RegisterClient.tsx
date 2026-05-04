import * as React from 'react';
import { v4 as uuidv4 } from 'uuid';
import axios from 'axios';
import { RegistrationRequest, OIDCClient, OpenIDConfiguration } from '../../../shared/types';
import type { Moment } from 'moment';
import moment from 'moment';
import { Spinner} from '../../../shared/components/Common';
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
    error:   'bg-red-50   border-red-300   text-red-800',
    warning: 'bg-amber-50 border-amber-300 text-amber-800',
    info:    'bg-blue-50  border-blue-300  text-blue-800',
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
const ScopeTag = ({ name, onRemove }: { name: string; onRemove: () => void }) => (
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
  const [issuerError, setIssuerError] = React.useState('');
  const [alert, setAlert] = React.useState('');
  const [alertSeverity, setAlertSeverity] = React.useState<AlertSeverity>('success');
  const [loading, setLoading] = React.useState(false);

  const REGISTRATION_ERROR = 'Error in registration. Check web console for logs.';

  // Reset state when dialog opens
  React.useEffect(() => {
    if (isOpen) {
      setIssuerError('');
      setAlert('');
      setLoading(false);
      setSelectedScopes([{ name: 'openid' }]);
      setExpireAt(null);
      setIssuer(null);
    }
  }, [isOpen]);

  const handleClose = () => {
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

  // ── Register ───────────────────────────────────────────────────────────────

  const registerClient = async (): Promise<void> => {
    try {
      setLoading(true);

      if (!issuer) {
        setIssuerError('Issuer cannot be left blank. Either enter correct Issuer or OpenID Configuration URL.');
        return;
      }

      let opConfigurationEndpoint: string;
      let issuerUrl: string;

      if (issuer.includes('.well-known/openid-configuration')) {
        opConfigurationEndpoint = issuer;
        issuerUrl = issuer.replace(/\/?\.well-known\/openid-configuration\/?$/, '');
      } else {
        issuerUrl = issuer.replace(/\/$/, '');
        opConfigurationEndpoint = `${issuerUrl}/.well-known/openid-configuration`;
      }

      const scopes = selectedScopes.map((s) => s.name).join(' ');
      const openidConfig = await getOpenidConfiguration(opConfigurationEndpoint);

      if (!openidConfig?.data) {
        setAlert('Error in fetching OpenID configuration!'); setAlertSeverity('error'); return;
      }

      const configData = openidConfig.data;

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
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 p-8">

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

          {/* ── Client Expiry Date ── */}
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

          {/* ── Scopes ── */}
          <div>
            <label htmlFor="scopes" className="block mb-1.5">
              <LabelWithTooltip
                label="Scopes"
                tip="Requested OAuth/OIDC scopes for this client (e.g. openid, profile, email). Type a scope and press Enter to add it."
              />
            </label>

            {/* Tag container */}
            <div
              className="min-h-[48px] w-full border border-slate-200 rounded-lg px-3 py-2 flex flex-wrap gap-1.5
                focus-within:ring-2 focus-within:ring-[#1a6b3c]/30 focus-within:border-[#1a6b3c]
                transition-all bg-slate-50/60 cursor-text"
              onClick={() => document.getElementById('scopes')?.focus()}
            >
              {selectedScopes.map((s) => (
                <ScopeTag key={s.name} name={s.name} onRemove={() => removeScope(s.name)} />
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
        </div>

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
        </div>
      </div>
    </div>
  );
}