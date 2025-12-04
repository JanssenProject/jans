
interface OIDCDiscoveryMetadata {
    registration_endpoint?: string;
    [key: string]: unknown;
}
interface OIDCClient {
    registration_endpoint?: string;
    [key: string]: unknown;
}
export default class MCPService {
    constructor() {
    }
    public async getClientByClientId(clientId): Promise<OIDCClient> {
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

    const didcoveryMetadata = await discoveryRes.json() as OIDCDiscoveryMetadata;

    if (registrationResp !== undefined) {
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

    } else {
        //setErrorMessage(REGISTRATION_ERROR)
    }
}
}