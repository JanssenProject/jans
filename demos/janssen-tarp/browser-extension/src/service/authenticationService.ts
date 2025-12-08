
import { ILooseObject } from '../options/ILooseObject';
import qs from 'qs';
import axios from 'axios';

export default class AuthenticationService {
    constructor() {
    }

    public async invokeAuthFlow(authzUrl: string): Promise<string> {
        try {
            console.log('Obtained autorization URL: ' + authzUrl)

            const resultUrl = await new Promise<string>((resolve, reject) => {
                chrome.identity.launchWebAuthFlow({
                    url: authzUrl,
                    interactive: true
                }, (responseUrl) => {
                    if (chrome.runtime.lastError || !responseUrl) {
                        const errorMessage = chrome.runtime.lastError?.message || "No redirect URL";
                        console.error("Authentication failed:", errorMessage);
                        reject(new Error(errorMessage))
                    } else {
                        resolve(responseUrl)
                    }
                });
            });

            const urlParams = new URLSearchParams(new URL(resultUrl).search);
            const errorDesc = urlParams.get('error_description');
            if (errorDesc) {
                throw new Error(errorDesc);
            }

            const code = urlParams.get('code');
            if (!code) {
                throw new Error('Missing authorization code in redirect URL');
            }

            return code;
        } catch (err) {
            throw err;
        }
    }

    public async getAccessToken(
        code: string,
        client: ILooseObject,
        codeVerifier: string
    ): Promise<ILooseObject> {
        try {
            if (!client?.clientId || !client?.clientSecret || !client?.tokenEndpoint) {
                throw new Error('Missing client configuration for token request');
            }

            const tokenReqData = qs.stringify({
                redirect_uri: client.redirectUris?.[0],
                grant_type: 'authorization_code',
                code_verifier: codeVerifier,
                client_id: client.clientId,
                code,
                scope: client.scope,
            });

            const tokenReqOptions = {
                method: 'POST',
                headers: {
                    'content-type': 'application/x-www-form-urlencoded',
                    Authorization: 'Basic ' + btoa(`${client.clientId}:${client.clientSecret}`),
                },
                data: tokenReqData,
                url: client.tokenEndpoint,
            };

            const tokenResponse = await axios(tokenReqOptions);
            return tokenResponse.data as ILooseObject;
        } catch (err) {
            throw err;
        }
    }

    public async getUserInfo(
        tokenResponse: ILooseObject,
        client: ILooseObject
    ): Promise<ILooseObject> {
        try {
            const accessToken = tokenResponse?.access_token;
            if (!accessToken) {
                throw new Error('Missing access_token for userinfo request');
            }

            if (!client?.userinfoEndpoint) {
                throw new Error('Missing userinfoEndpoint in client configuration');
            }

            const userInfoOptions = {
                method: 'GET',
                headers: { Authorization: `Bearer ${accessToken}` },
                url: client.userinfoEndpoint,
            };

            const userInfoResponse = await axios(userInfoOptions);
            return userInfoResponse.data as ILooseObject;
        } catch (err) {
            throw err;
        }
    }
}