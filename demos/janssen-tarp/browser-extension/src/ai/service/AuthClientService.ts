
import { ILooseObject } from '../../shared/types';

export default class AuthClientService {

  constructor() {
  }

  public async getClientByClientId(clientId: string): Promise<ILooseObject> {
    return new Promise((resolve, reject) => {
      chrome.storage.local.get(["oidcClients"], (result) => {
        if (chrome.runtime.lastError) {
          reject(chrome.runtime.lastError);
        } else {
          const clientArr: ILooseObject[] = Array.isArray(result.oidcClients)
            ? (result.oidcClients as ILooseObject[])
            : [];
          const record = clientArr.find((item: ILooseObject) => item.clientId === clientId);

          resolve(record || {});
        }
      });
    });
  }

  public async saveClientInTarpStorage(registrationResp: ILooseObject): Promise<void> {
    const structured = registrationResp?.structuredContent as ILooseObject | undefined;
    const registrationClientUri = structured?.registration_client_uri as string | undefined;
    if (!registrationClientUri) {
      throw new Error("Invalid registration response");
    }
    const issuer = new URL(registrationClientUri).origin;
    const discoveryRes = await fetch(`${issuer}/.well-known/openid-configuration`);
    if (!discoveryRes.ok) throw new Error("Failed to fetch OIDC metadata");

    const discoveryMetadata = await discoveryRes.json() as ILooseObject;

    if (!structured?.client_id || !structured?.client_secret) {
      throw new Error("Registration response missing required client credentials");
    }

    return new Promise((resolve, reject) => {
      chrome.storage.local.get(["oidcClients", "openidConfigurations"], (result) => {
        if (chrome.runtime.lastError) {
          reject(chrome.runtime.lastError);
          return;
        }
        let clientArr = []
        if (!!result.oidcClients) {
          clientArr = result.oidcClients;
        }

        clientArr.push({
          'id': structured?.client_id,
          'opHost': issuer,
          'clientId': structured?.client_id,
          'clientSecret': structured?.client_secret,
          'scope': structured?.scope,
          'redirectUris': structured?.redirect_uris,
          'authorizationEndpoint': discoveryMetadata?.authorization_endpoint,
          'tokenEndpoint': discoveryMetadata?.token_endpoint,
          'userinfoEndpoint': discoveryMetadata?.userinfo_endpoint,
          'acrValuesSupported': discoveryMetadata?.acr_values_supported,
          'endSessionEndpoint': discoveryMetadata?.end_session_endpoint,
          'responseType': structured?.response_types,
          'postLogoutRedirectUris': [chrome.identity.getRedirectURL('logout')],
          'expireAt': undefined,
          'showClientExpiry': false,
          'registrationDate': Date.now(),
          'openidConfiguration': discoveryMetadata,
        });

        // Logout looks up the OpenID configuration by issuer to find the
        // end_session_endpoint, so persist it the same way the manual
        // registration flow does. Without this, logout silently aborts.
        const configs: ILooseObject[] = Array.isArray(result.openidConfigurations)
          ? result.openidConfigurations
          : [];
        const idx = configs.findIndex((c) => c.issuer === discoveryMetadata.issuer);
        if (idx >= 0) configs[idx] = discoveryMetadata; else configs.push(discoveryMetadata);

        chrome.storage.local.set({ oidcClients: clientArr, openidConfigurations: configs }, () => {
          if (chrome.runtime.lastError) {
            reject(chrome.runtime.lastError);
          } else {
            console.log('OIDC client registered successfully!');
            resolve();
          }
        });
      });
    });
  }

  public createSystemPrompt(): string {
      return `
      You are an assistant for the Janssen Tarp extension that uses MCP tools to perform OIDC operations.

      You can do two things:
      1. Have a normal conversation (greetings, questions, explanations about what you can do).
      2. Call an MCP tool, but ONLY when the user clearly asks for an OIDC operation AND provides the required details.

      Call a tool only in these cases:
      - registerOIDCClient → user asks to "register an OIDC client" AND provides an issuer URL.
      - startAuthFlow → user asks to "login" / "authenticate" / "start auth" AND provides a client_id.
      - exchangeToken → user asks to exchange a code / get a token AND provides the required code details.

      DO NOT call any tool when:
      - The message is a greeting or small talk (e.g. "hi", "hello", "how are you", "thanks").
      - The user is asking what you can do or how something works.
      - The required parameters for the tool are not present in the user's message.

      In all of those cases, reply in plain natural language. If the user seems to want an OIDC
      operation but hasn't given the needed details, ask them for the specific missing information
      (for example, the issuer URL or client_id) instead of calling a tool.

      PARAMETER RULES (when you do call a tool):
      - Only use parameter values the user explicitly provides.
      - Do NOT guess, invent, or use placeholder/example values.
      - Do NOT fill redirect_uri or PKCE params — the MCP client provides those automatically.`;
    }

  public createOIDCTools(): any[] {
      return [
        {
          type: "function" as const,
          function: {
            name: "registerOIDCClient",
            description: "Register an OIDC client using MCP server",
            parameters: {
              type: "object",
              properties: {
                issuer: {
                  type: "string",
                  description: "The OIDC issuer URL (e.g., https://op-host.gluu.org)"
                },
                scopes: {
                  type: "array",
                  items: { type: "string" },
                  description: "Array of OIDC scopes (e.g., openid, profile)"
                },
                response_types: {
                  type: "array",
                  items: { type: "string" },
                  description: "Array of response types (e.g., code)"
                }
              },
              required: ["issuer", "scopes", "response_types"]
            }
          }
        },
        {
          type: "function" as const,
          function: {
            name: "startAuthFlow",
            description: "Start OIDC Authorization Code Flow (generate URL)",
            parameters: {
              type: "object",
              properties: {
                issuer: { type: "string", nullable: true },
                client_id: { type: "string" },
              },
              required: ["issuer", "client_id"]
            }
          }
        },
        {
          type: "function" as const,
          function: {
            name: "exchangeToken",
            description: "Exchange authorization code for tokens",
            parameters: {
              type: "object",
              properties: {
                issuer: { type: "string" },
                client_id: { type: "string" },
                client_secret: { type: "string", nullable: true },
                code_verifier: { type: "string" },
                code: { type: "string" },
                redirect_uri: { type: "string" }
              },
              required: ["issuer", "client_id", "code_verifier", "code", "redirect_uri"]
            }
          }
        }
      ];
    }

  /**
   * Validate a tool call's arguments against the tool's required parameters.
   *
   * Small / local models often emit a tool call with missing or empty required
   * fields (e.g. calling registerOIDCClient on a plain "hi"). We use this to
   * reject such calls and prompt the user for the missing details instead of
   * executing a half-formed operation.
   */
  public validateToolCall(name: string, args: any): { valid: boolean; missing: string[] } {
    const tool = this.createOIDCTools().find(t => t.function.name === name);
    if (!tool) {
      return { valid: false, missing: [] };
    }

    const required: string[] = tool.function.parameters?.required ?? [];
    const isEmpty = (v: any): boolean =>
      v === undefined ||
      v === null ||
      (typeof v === "string" && v.trim() === "") ||
      (Array.isArray(v) && v.length === 0);

    const missing = required.filter(field => !args || isEmpty(args[field]));
    return { valid: missing.length === 0, missing };
  }

}