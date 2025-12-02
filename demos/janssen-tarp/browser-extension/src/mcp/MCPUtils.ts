interface OIDCDiscoveryMetadata {
    registration_endpoint?: string;
    [key: string]: unknown;
}
export default class MCPUtils {
    static async generateRandomString(registrationResp) {
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
                    'redirectUris': didcoveryMetadata?.redirect_uris,
                    'authorizationEndpoint': didcoveryMetadata?.authorization_endpoint,
                    'tokenEndpoint': didcoveryMetadata?.token_endpoint,
                    'userinfoEndpoint': didcoveryMetadata?.userinfo_endpoint,
                    'acrValuesSupported': didcoveryMetadata?.acr_values_supported,
                    'endSessionEndpoint': didcoveryMetadata?.end_session_endpoint,
                    'responseType': didcoveryMetadata?.response_types,
                    'postLogoutRedirectUris': didcoveryMetadata?.post_logout_redirect_uris,
                    'expireAt': undefined,
                    'showClientExpiry': false

                });
                chrome.storage.local.set({ oidcClients: clientArr });
            });

            console.log('OIDC client registered successfully!')
            console.log("oidcClient is set for client_id: " + registrationResp.data.client_id);

        } else {
            //setErrorMessage(REGISTRATION_ERROR)
        }
    }
}