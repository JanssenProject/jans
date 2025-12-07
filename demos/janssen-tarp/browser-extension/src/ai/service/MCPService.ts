
import { ILooseObject } from '../../options/ILooseObject';

export default class MCPService {

  constructor() {
  }
      
    public async getClientByClientId(clientId): Promise<ILooseObject> {
        return new Promise((resolve, reject) => {
            chrome.storage.local.get(["oidcClients"], (result) => {
                if (chrome.runtime.lastError) {
                    reject(chrome.runtime.lastError);
                } else {
                    let clientArr = []
                    if (!!result.oidcClients) {
                        clientArr = result.oidcClients;
                    }
                    const record = clientArr.find(item => item.clientId === clientId);

                    resolve(record || {});
                }
            });
        });
    }

    public async saveClientInTarpStorage(registrationResp) {
        const issuer = new URL(registrationResp?.structuredContent?.registration_client_uri).origin;
        const discoveryRes = await fetch(`${issuer}/.well-known/openid-configuration`);
        if (!discoveryRes.ok) throw new Error("Failed to fetch OIDC metadata");

        const didcoveryMetadata = await discoveryRes.json() as ILooseObject;
        if (registrationResp == undefined) return;
        chrome.storage.local.get(["oidcClients"], (result) => {
            let clientArr = []
            if (!!result.oidcClients) {
                clientArr = result.oidcClients;
            }

            clientArr.push({
                'opHost': issuer,
                'clientId': registrationResp?.structuredContent?.client_id,
                'clientSecret': registrationResp?.structuredContent?.client_secret,
                'scope': registrationResp?.structuredContent?.scope,
                'redirectUris': registrationResp?.structuredContent?.redirect_uris,
                'authorizationEndpoint': didcoveryMetadata?.authorization_endpoint,
                'tokenEndpoint': didcoveryMetadata?.token_endpoint,
                'userinfoEndpoint': didcoveryMetadata?.userinfo_endpoint,
                'acrValuesSupported': didcoveryMetadata?.acr_values_supported,
                'endSessionEndpoint': didcoveryMetadata?.end_session_endpoint,
                'responseType': registrationResp?.structuredContent?.response_types,
                'postLogoutRedirectUris': registrationResp?.structuredContent?.post_logout_redirect_uris,
                'expireAt': undefined,
                'showClientExpiry': false

            });
            chrome.storage.local.set({ oidcClients: clientArr });
        });

        console.log('OIDC client registered successfully!')
        console.log("oidcClient is set for client_id: " + registrationResp?.structuredContent?.client_id);
    }

    public createSystemPrompt(): string {
        return `
      You are an assistant that uses MCP tools to perform OIDC operations.
      
      When user asks:
      - "register OIDC client" → call registerOIDCClient
      - "login", "authenticate", "start auth" → call startAuthFlow
      - "token", "exchange", "get access token", "userinfo" → call exchangeToken
      
      RULES:
      - Only extract parameters the user explicitly provides.
      - Do NOT guess.
      - Do NOT invent defaults.
      - Do NOT fill redirect_uri or PKCE params.
      These will be provided by the MCP client automatically.`;
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

}