import React, { useState, useEffect, KeyboardEventHandler } from 'react'
import { v4 as uuidv4 } from 'uuid';
import axios from 'axios';
import qs from 'qs';
import './options.css';
import { WindmillSpinner } from 'react-spinner-overlay';
import Select from 'react-select';
import { IOption } from './IOption';
import { ILooseObject } from './ILooseObject';
import moment from 'moment';
const components = {
    DropdownIndicator: null,
};

const createOption = (label: string) => ({
    label,
    value: label,
});

const OIDCClientDetails = (data) => {
    const [additionalParam, setAdditionalParam] = useState("");
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(false);
    const [displayToken, setDisplayToken] = useState(false);
    const [acrValueOption, setAcrValueOption] = useState<IOption | null>();
    const [acrValueOptions, setAcrValueOptions] = useState<readonly IOption[]>([]);

    useEffect(() => {
        (async () => {

            const opConfiguration: string = await new Promise((resolve, reject) => {
                chrome.storage.local.get(["opConfiguration"], (result) => {
                    resolve(JSON.stringify(result));
                });
            });

            const supportedAcrValuesString = JSON.parse(opConfiguration)?.opConfiguration?.acr_values_supported;
            setAcrValueOptions(supportedAcrValuesString.map((ele) => createOption(ele)));

            const oidcClient: string = await new Promise((resolve, reject) => {
                chrome.storage.local.get(["oidcClient"]).then(async (result) => {
                    resolve(JSON.stringify(result));
                });
            });
            const showClientExpiry = JSON.parse(oidcClient)?.oidcClient?.showClientExpiry;
            if (showClientExpiry) {
                const expireAt = JSON.parse(oidcClient)?.oidcClient?.expire_at;

                const lifetime = Math.floor((expireAt - moment().toDate().getTime()) / 1000);
                if (lifetime <= 0) {
                    setMessage('This client is expired. Please reset and register a new client.')
                } else {
                    setMessage('The client will expire in ' + secondsToDhms(lifetime))
                }
            }

        })();
    }, [])

    function customLaunchWebAuthFlow(options, callback) {
        var requestId = Math.random().toString(36).substring(2);
        var redirectUrl = options.redirectUrl;
        var authUrl = options.url + "&state=" + requestId;
        chrome.tabs.create({ url: authUrl }, function (tab) {
            var intervalId = setInterval(function () {
                chrome.tabs.get(tab.id, function (currentTab) {
                    if (!currentTab) {
                        clearInterval(intervalId);
                        chrome.tabs.remove(tab.id);
                        callback(undefined, new Error('Authorization tab was closed.'));
                    }
                });
            }, 1000);

            chrome.tabs.onUpdated.addListener(function listener(tabId, changeInfo, tab) {
                if (!!chrome.runtime.lastError) {
                    chrome.tabs.remove(tabId);
                    callback(undefined, chrome.runtime.lastError);
                    chrome.tabs.onUpdated.removeListener(listener);
                }
                if (tabId === tab?.id && changeInfo?.status === "complete") {
                    chrome.tabs.sendMessage(tab.id, { requestId: requestId }, function (response) {
                        clearInterval(intervalId);
                        chrome.tabs.query({ active: true, lastFocusedWindow: true }, tabs => {
                            let url = tabs[0].url;
                            const urlParams = new URLSearchParams(new URL(url).search)
                            const code = urlParams.get('code')
                            if (code != null && areUrlsEqual(url, redirectUrl)) {
                                callback(url, undefined);
                                chrome.tabs.remove(tab.id);
                                setLoading(false);
                                chrome.tabs.onUpdated.removeListener(listener);
                            }
                        });
                    });
                }
            })
        });
    }

    function areUrlsEqual(url1, url2) {
        // Create URL objects for comparison

        const parsedUrl1 = new URL(url1);
        const parsedUrl2 = new URL(url2);

        // Compare individual components
        return (
            parsedUrl1.protocol === parsedUrl2.protocol &&
            parsedUrl1.host === parsedUrl2.host &&
            parsedUrl1.pathname === parsedUrl2.pathname &&
            parsedUrl1.port === parsedUrl2.port
        );
    }

    async function triggerCodeFlowButton() {
        setLoading(true);
        const redirectUrl = data.data.redirect_uri[0];
        const { secret, hashed } = await generateRandomChallengePair();
        chrome.storage.local.get(["oidcClient"]).then(async (result) => {
            if (!!result.oidcClient) {

                let options: ILooseObject = {
                    scope: result?.oidcClient?.scope,
                    response_type: result?.oidcClient?.response_type[0],
                    redirect_uri: redirectUrl,
                    client_id: result?.oidcClient?.client_id,
                    code_challenge_method: 'S256',
                    code_challenge: hashed,
                    nonce: uuidv4(),
                };

                if (!!acrValueOption && !isEmpty(acrValueOption.value)) {
                    options.acr_values = acrValueOption.value;
                }

                let authzUrl = `${result.oidcClient.authorization_endpoint}?${qs.stringify(options)}`;

                if (!!additionalParam && additionalParam.trim() != '') {
                    result.oidcClient.additionalParams = additionalParam.trim();

                    chrome.storage.local.set({ oidcClient: result.oidcClient });

                    let additionalParamJSON = JSON.parse(additionalParam)
                    console.log('Processing additional parameters');
                    Object.keys(additionalParamJSON).forEach(key => {
                        console.log(key + "~~~" + additionalParamJSON[key]);
                        authzUrl += `&${key}=${additionalParamJSON[key]}`
                    });
                }
                console.log('Obtained autorization URL: ' + authzUrl)

                const resultUrl: string = await new Promise((resolve, reject) => {
                    customLaunchWebAuthFlow({
                        url: authzUrl,
                        redirectUrl: redirectUrl
                    }, (callbackUrl, error) => {
                        if (!!error) {
                            console.error('Error in executing auth url: ', error)
                            logout()
                            reject(error)
                        } else {
                            console.log('Callback Url: ', callbackUrl)
                            resolve(callbackUrl);
                        }
                    });
                });

                if (resultUrl) {
                    const urlParams = new URLSearchParams(new URL(resultUrl).search)
                    const code = urlParams.get('code')
                    console.log('code:' + code)
                    const opConfig: string = await new Promise((resolve, reject) => {
                        chrome.storage.local.get(["opConfiguration"], (result) => {
                            resolve(JSON.stringify(result));
                        });
                    });

                    const tokenReqData = qs.stringify({
                        redirect_uri: redirectUrl,
                        grant_type: 'authorization_code',
                        code_verifier: secret,
                        client_id: result.oidcClient.client_id,
                        code,
                        scope: result.oidcClient.scope
                    })

                    const tokenReqOptions = {
                        method: 'POST',
                        headers: { 'content-type': 'application/x-www-form-urlencoded', 'Authorization': 'Basic ' + btoa(`${result.oidcClient.client_id}:${result.oidcClient.client_secret}`) },
                        data: tokenReqData,
                        url: JSON.parse(opConfig).opConfiguration.token_endpoint,
                    };

                    const tokenResponse = await axios(tokenReqOptions);

                    if (
                        tokenResponse &&
                        tokenResponse.data &&
                        tokenResponse.data.access_token
                    ) {
                        console.log('tokenResponse:' + JSON.stringify(tokenResponse))

                        const userInfoOptions = {
                            method: 'GET',
                            headers: { 'Authorization': `Bearer ${tokenResponse.data.access_token}` },
                            url: JSON.parse(opConfig).opConfiguration.userinfo_endpoint,
                        };

                        const userInfoResponse = await axios(userInfoOptions);

                        chrome.storage.local.set({
                            loginDetails: {
                                'access_token': tokenResponse.data.access_token,
                                'userDetails': userInfoResponse.data,
                                'id_token': tokenResponse.data.id_token,
                                'displayToken': displayToken,
                            }
                        }).then(async () => {
                            console.log("userDetails: " + JSON.stringify(userInfoResponse.data));
                            setLoading(false);
                        });
                    }
                }
            }
        });
    }

    function logout() {
        chrome.identity.clearAllCachedAuthTokens(async () => {

            const loginDetails: string = await new Promise((resolve, reject) => {
                chrome.storage.local.get(["loginDetails"], (result) => {
                    resolve(JSON.stringify(result));
                });
            });

            const openidConfiguration: string = await new Promise((resolve, reject) => { chrome.storage.local.get(["opConfiguration"], (result) => { resolve(JSON.stringify(result)); }) });

            chrome.storage.local.remove(["loginDetails"], function () {
                var error = chrome.runtime.lastError;
                if (error) {
                    console.error(error);
                } else {
                    window.location.href = `${JSON.parse(openidConfiguration).opConfiguration.end_session_endpoint}?state=${uuidv4()}&post_logout_redirect_uri=${chrome.runtime.getURL('options.html')}&id_token_hint=${JSON.parse(loginDetails).loginDetails.id_token}`
                }
            });
        });
    }

    async function resetClient() {
        chrome.storage.local.remove(["oidcClient", "opConfiguration"], function () {
            var error = chrome.runtime.lastError;
            if (error) {
                console.error(error);
            }
        });
    }

    async function getOpenidConfiguration(issuer) {
        try {
            const endpoint = issuer + '/.well-known/openid-configuration';
            const oidcConfigOptions = {
                method: 'GET',
                url: endpoint,
            };
            const response = await axios(oidcConfigOptions);
            return await response;
        } catch (err) {
            console.error(err)
        }
    }

    async function generateRandomChallengePair() {
        const secret = await generateRandomString();
        const encryt = await sha256(secret);
        const hashed = base64URLEncode(encryt);
        return { secret, hashed };
    }

    function base64URLEncode(a) {
        var str = "";
        var bytes = new Uint8Array(a);
        var len = bytes.byteLength;
        for (var i = 0; i < len; i++) {
            str += String.fromCharCode(bytes[i]);
        }

        return btoa(str)
            .replace(/\+/g, "-")
            .replace(/\//g, "_")
            .replace(/=+$/, "");

    }

    function dec2hex(dec) {
        return ('0' + dec.toString(16)).substr(-2)
    }

    function generateRandomString() {
        var array = new Uint32Array(56 / 2);
        window.crypto.getRandomValues(array);
        return Array.from(array, dec2hex).join('');
    }

    async function sha256(plain) { // returns promise ArrayBuffer
        const encoder = new TextEncoder();
        const data = await encoder.encode(plain);
        return window.crypto.subtle.digest('SHA-256', data);
    }

    function updateInputValue(event) {
        if (event.target.id === 'additionalParam') {
            setAdditionalParam(event.target.value);
        }
    }

    function isEmpty(value) {
        return (value == null || value.length === 0);
    }

    function secondsToDhms(seconds) {
        seconds = Number(seconds)
        var d = Math.floor(seconds / (3600 * 24))
        var h = Math.floor((seconds % (3600 * 24)) / 3600)
        var m = Math.floor((seconds % 3600) / 60)
        var s = Math.floor(seconds % 60)
        // console.log(d, h, m, s)
        var dDisplay = d > 0 ? d + (d == 1 ? " day, " : " days, ") : ""
        var hDisplay = h > 0 ? h + (h == 1 ? " hour, " : " hours, ") : ""
        var mDisplay = m > 0 ? m + (m == 1 ? " minute, " : " minutes, ") : ""
        var sDisplay = s > 0 ? s + (s == 1 ? " second" : " seconds") : ""
        return dDisplay + hDisplay + mDisplay + sDisplay
    }

    return (

        <div className="box">
            <>
                <legend><span className="number">O</span> Registered Client</legend>
                {message.length > 0 ? <legend><span className="redFont">{message}</span></legend> : ""}

                <WindmillSpinner loading={loading} color="#00ced1" />
                <label><b>OP Host:</b></label>
                <input type="text" id="opHost" name="opHost" value={data.data.op_host} disabled />

                <label><b>Client Id:</b></label>
                <input type="text" id="clientId" name="clientId" value={data.data.client_id} disabled />

                <label><b>Client Secret:</b></label>
                <input type="text" id="clientSecret" name="clientSecret" value={data.data.client_secret} disabled />

                <label><b>Additional Params:</b></label>
                <input type="text" id="additionalParam" name="additionalParam" value={additionalParam} onChange={updateInputValue}
                    placeholder='e.g. {"paramOne": "valueOne", "paramTwo": "valueTwo"}' autoComplete="off" className="inputText inputStyle" />

                <label><b>Acr Value:</b></label>
                <Select
                    inputId="acrValues"
                    className="basic-single inputText"
                    classNamePrefix="select"
                    isClearable={true}
                    isSearchable={true}
                    name="color"
                    onChange={(newValue) => setAcrValueOption(newValue)}
                    options={acrValueOptions}
                />

                <label><input type="checkbox" onChange={() => setDisplayToken(!displayToken)} /><b>Display Access Token and ID Token after authentication</b></label>

                <button id="trigCodeFlowButton" onClick={triggerCodeFlowButton}>Trigger Auth Code Flow</button>
                <button id="resetButton" onClick={resetClient}>Reset</button>
            </>
        </div >
    )
};

export default OIDCClientDetails;