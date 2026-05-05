import React, { useState } from "react";
import { v4 as uuidv4 } from "uuid";
import { JsonEditor } from "json-edit-react";
import { jwtDecode } from "jwt-decode";
import {
  Copy,
  Plus,
  Minus,
  LogOut,
} from "lucide-react";
import { Spinner } from "../../../shared/components/Common";
import {
  OpenIDConfiguration,
  LogoutOptions,
  LoginDetails,
  IJWT,
} from "../../../shared/types";

type UserDetailsProps = {
  data?: any;
  notifyOnDataChange: (value: string) => void;
};

const UserDetails = ({
  data,
  notifyOnDataChange,
}: UserDetailsProps) => {
  const [loading, setLoading] = useState(false);
  const [showPayloadIdToken, setShowPayloadIdToken] =
    useState(false);
  const [showPayloadAT, setShowPayloadAT] =
    useState(false);
  const [showPayloadUI, setShowPayloadUI] =
    useState(false);

  const [expanded, setExpanded] = useState({
    access: true,
    id: false,
    user: false,
  });

  const [snackbar, setSnackbar] = useState('');

  const [decodedTokens, setDecodedTokens] =
    useState<{
      access_token: IJWT;
      userinfo_token: IJWT;
      id_token: IJWT;
    }>({
      access_token: { header: {}, payload: {} },
      userinfo_token: { header: {}, payload: {} },
      id_token: { header: {}, payload: {} },
    });

  const [jwtTokens, setJwtTokens] = useState({
    access_token: "",
    userinfo_token: "",
    id_token: "",
  });

  React.useEffect(() => {
    if (data) {
      setDecodedTokens({
        access_token: decodeJWT(data.access_token),
        userinfo_token: decodeJWT(data.userDetails),
        id_token: decodeJWT(data.id_token),
      });

      setJwtTokens({
        access_token: data.access_token,
        userinfo_token: data.userDetails,
        id_token: data.id_token,
      });
    }
  }, [data]);

  const decodeJWT = (token?: string): IJWT => {
    try {
      if (!token)
        return { header: {}, payload: {} };

      const payload =
        jwtDecode<Record<string, unknown>>(token);

      const header = jwtDecode<
        Record<string, unknown>
      >(token, { header: true });

      return { header, payload };
    } catch {
      return { header: {}, payload: {} };
    }
  };

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(
        JSON.stringify(jwtTokens, null, 2)
      );

      setSnackbar("Token JSON copied successfully");
      setTimeout(() => setSnackbar(''), 3000);
    } catch (error: any) {
      setSnackbar('Copy failed: ' + String(error) || "Failed to copy");
    }
  };

  async function logout(
    options: LogoutOptions = {}
  ) {
    const {
      forceSilentLogout = false,
      notifyOnComplete = true,
    } = options;

    setLoading(true);

    try {
      const loginDetails =
        await getStoredLoginDetails();

      if (!loginDetails?.id_token) return;

      const payload = jwtDecode<{ iss: string }>(
        loginDetails.id_token
      );

      const config =
        await getOpenIDConfigurationByIssuer(
          payload.iss
        );

      if (!config) return;

      await removeLoginDetails();

      await performRemoteLogout(
        loginDetails.id_token,
        config,
        forceSilentLogout
      );
    } catch {
      await performLocalLogout();
    } finally {
      setLoading(false);

      if (notifyOnComplete) {
        notifyOnDataChange("true");
      }
    }
  }

  async function getStoredLoginDetails(): Promise<LoginDetails | null> {
    return new Promise((resolve) => {
      chrome.storage.local.get(
        ["loginDetails"],
        (result) => {
          resolve(result.loginDetails || null);
        }
      );
    });
  }

  async function getOpenIDConfigurationByIssuer(
    issuerUrl: string
  ): Promise<OpenIDConfiguration | null> {
    return new Promise((resolve) => {
      chrome.storage.local.get(
        ["openidConfigurations"],
        (result) => {
          const configs =
            result.openidConfigurations || [];

          const found = configs.find(
            (c: OpenIDConfiguration) =>
              c.issuer === issuerUrl
          );

          resolve(found || null);
        }
      );
    });
  }

  async function removeLoginDetails() {
    return new Promise<void>((resolve) => {
      chrome.storage.local.remove(
        ["loginDetails"],
        () => resolve()
      );
    });
  }

  async function performLocalLogout() {
    return new Promise<void>((resolve) => {
      chrome.storage.local.remove(
        ["loginDetails"],
        () => resolve()
      );
    });
  }

  async function performRemoteLogout(
    idToken: string,
    config: OpenIDConfiguration,
    forceSilent = false
  ) {
    if (forceSilent) {
      return performSilentLogout(idToken, config);
    }

    return new Promise<void>((resolve) => {
      chrome.identity.launchWebAuthFlow(
        {
          url: buildLogoutUrl(idToken, config),
          interactive: true,
        },
        () => resolve()
      );
    });
  }

  async function performSilentLogout(
    idToken: string,
    config: OpenIDConfiguration
  ) {
    try {
      await fetch(
        buildLogoutUrl(idToken, config),
        {
          method: "GET",
          credentials: "include",
          mode: "no-cors",
        }
      );
    } catch { }
  }

  function buildLogoutUrl(
    idToken: string,
    config: OpenIDConfiguration
  ) {
    const params = new URLSearchParams({
      state: uuidv4(),
      id_token_hint: idToken,
      post_logout_redirect_uri:
        chrome.identity.getRedirectURL(
          "logout"
        ),
      ui_locales: navigator.language,
      logout_hint: idToken,
    });

    return `${config.end_session_endpoint}?${params.toString()}`;
  }

  const TokenCard = ({
    title,
    expandedState,
    showPayload,
    setShowPayload,
    rawToken,
    decoded,
  }: {
    title: string;
    expandedState: keyof typeof expanded;
    showPayload: boolean;
    setShowPayload: (v: boolean) => void;
    rawToken: string;
    decoded: IJWT;
  }) => (
    <div className="mb-4 overflow-hidden rounded-xl border border-gray-200 bg-white">
      {/* Header */}
      <button
        onClick={() =>
          setExpanded((prev) => ({
            ...prev,
            [expandedState]:
              !prev[expandedState],
          }))
        }
        className="flex w-full items-center justify-between px-6 py-5 text-left hover:bg-gray-50"
      >
        <span className="text-xl font-semibold text-gray-900">
          {title}
        </span>

        <span className="flex h-9 w-9 items-center justify-center rounded-full bg-gray-900 text-white">
          {expanded[expandedState] ? (
            <Minus size={16} />
          ) : (
            <Plus size={16} />
          )}
        </span>
      </button>

      {/* Content */}
      {expanded[expandedState] && (
        <div className="border-t border-gray-200 bg-gray-50 px-6 py-5">
          <div className="rounded-xl border border-gray-200 bg-white p-5">
            <div className="break-all text-sm leading-7 text-gray-600">
              {showPayload ? (
                <>
                  <JsonEditor
                    collapse
                    viewOnly
                    data={decoded.header}
                    rootName="header"
                  />
                  <JsonEditor
                    collapse
                    viewOnly
                    data={decoded.payload}
                    rootName="payload"
                  />
                </>
              ) : (
                rawToken
              )}
            </div>

            <button
              onClick={() =>
                setShowPayload(!showPayload)
              }
              className="mt-4 text-sm font-medium text-emerald-600 hover:underline"
            >
              {showPayload
                ? "Show JWT"
                : "Show Payload"}
            </button>
          </div>
        </div>
      )}
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      {/* Snackbar */}
      {!!snackbar && (
        <div
          role="status"
          aria-live="polite"
          aria-atomic="true" 
          className="fixed bottom-6 left-1/2 -translate-x-1/2 px-6 py-3 bg-[#002B49] text-white text-sm font-medium rounded-full shadow-2xl flex items-center gap-3 z-[60] animate-bounce">
          {snackbar}
        </div>
      )}

      <div className="relative mx-auto max-w-5xl rounded-2xl border border-gray-200 bg-white p-10">
        {/* Loader Overlay */}
        {loading && (
          <div className="absolute inset-0 z-20 flex items-center justify-center rounded-2xl bg-white/70 backdrop-blur-sm">
            <Spinner />
          </div>
        )}

        {/* Header */}
        <div className="mb-8 flex items-center justify-between">
          <h1 className="text-4xl font-bold text-gray-900">
            User Details
          </h1>

          <div className="flex items-center gap-3">
            <button
              onClick={copyToClipboard}
              className="flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
            >
              <Copy size={15} />
              Copy
            </button>

            <button
              onClick={() =>
                logout({
                  forceSilentLogout: false,
                  notifyOnComplete: true,
                })
              }
              className="flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm text-white hover:bg-red-700"
            >
              <LogOut size={15} />
              Logout
            </button>
          </div>
        </div>

        {/* Tokens */}
        {data?.displayToken && (
          <>
            <TokenCard
              title="Access Token"
              expandedState="access"
              showPayload={showPayloadAT}
              setShowPayload={setShowPayloadAT}
              rawToken={data?.access_token}
              decoded={decodedTokens.access_token}
            />

            <TokenCard
              title="ID Token"
              expandedState="id"
              showPayload={showPayloadIdToken}
              setShowPayload={setShowPayloadIdToken}
              rawToken={data?.id_token}
              decoded={decodedTokens.id_token}
            />
          </>
        )}

        <TokenCard
          title="User Details"
          expandedState="user"
          showPayload={showPayloadUI}
          setShowPayload={setShowPayloadUI}
          rawToken={data?.userDetails}
          decoded={decodedTokens.userinfo_token}
        />

        {/* Logout */}
        <div className="mt-8 flex justify-end">
          <button
            onClick={() =>
              logout({
                forceSilentLogout: false,
                notifyOnComplete: true,
              })
            }
            className="flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm text-white hover:bg-red-700"
          >
            <LogOut size={16} />
            Logout
          </button>
        </div>
      </div>
    </div>
  );
};

export default UserDetails;