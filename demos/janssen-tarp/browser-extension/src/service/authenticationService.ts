
import { ILooseObject } from '../options/ILooseObject';
import qs from 'qs';
import axios from 'axios';

export default class AuthenticationService {
    constructor() {
    }

    public async invokeAuthFlow(authzUrl): Promise<string> {
        try {
            console.log('Obtained autorization URL: ' + authzUrl)

            const resultUrl: string = await new Promise((resolve, reject) => {
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

            if (resultUrl) {
                const urlParams = new URLSearchParams(new URL(resultUrl).search)
                const code = urlParams.get('code')
                const errorDesc = urlParams.get('error_description')
                if (errorDesc != null) {
                    throw errorDesc;
                }
                console.log('code:' + code)
                return code;
            }
            return null;
        } catch (err) {
            throw err;
        }
    }

    public async getAccessToken(code, client, codeVerifier): Promise<ILooseObject> {
        try {
            const tokenReqData = qs.stringify({
                redirect_uri: client?.redirectUris[0],
                grant_type: 'authorization_code',
                code_verifier: codeVerifier,
                client_id: client?.clientId,
                code,
                scope: client?.scope
            })

            const tokenReqOptions = {
                method: 'POST',
                headers: { 'content-type': 'application/x-www-form-urlencoded', 'Authorization': 'Basic ' + btoa(`${client?.clientId}:${client?.clientSecret}`) },
                data: tokenReqData,
                url: client.tokenEndpoint,
            };

            const tokenResponse = await axios(tokenReqOptions);
            return tokenResponse;
        } catch (err) {
            throw err;
        }
    }
    public async getUserInfo(tokenResponse, client): Promise<ILooseObject> {
        try {
        const userInfoOptions = {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${tokenResponse.data.access_token}` },
            url: client.userinfoEndpoint,
          };

          const userInfoResponse = await axios(userInfoOptions);
          return userInfoResponse;
        } catch (err) {
            throw err;
        }
    }
}