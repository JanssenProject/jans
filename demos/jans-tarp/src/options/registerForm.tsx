import React, { useState } from 'react'
import axios from 'axios';
import './options.css'
import { v4 as uuidv4 } from 'uuid';
import { WindmillSpinner } from 'react-spinner-overlay'

const RegisterForm = (data) => {
    const [issuer, setIssuer] = useState("");
    const [acrValues, setAcrValues] = useState("");
    const [scope, setScope] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    function updateInputValue(event) {
        if (event.target.id === 'issuer') {
            setIssuer(event.target.value);
        }
        if (event.target.id === 'acrValues') {
            setAcrValues(event.target.value);
        }
        if (event.target.id === 'scope') {
            setScope(event.target.value);
        }
    }

    function validateState() {

        let errorField = ''
        if (issuer === '') {
            errorField += 'issuer ';
        }
        if (scope === '') {
            errorField += 'scope ';
        }
        if (errorField.trim() !== '') {
            setError('The following fields are mandatory: ' + errorField);
            return false;
        }
        return true;
    }

    async function registerClient() {
        if (validateState()) {
            try {
                setLoading(true);
                const response = await register()
                if (response.result !== 'success') {
                    setError('Error in registration.');
                }
                setLoading(false);
            } catch (err) {
                console.error(err)
            }
        }
    }

    async function register() {
        try {
            const openapiConfig = await getOpenidConfiguration(issuer);

            if (openapiConfig != undefined) {
                chrome.storage.local.set({ opConfiguration: openapiConfig.data }).then(() => {
                    console.log("openapiConfig is set to " + openapiConfig);
                });

                const registrationUrl = openapiConfig.data.registration_endpoint;

                var registerObj = {
                    issuer: issuer,
                    redirect_uris: [chrome.identity.getRedirectURL()],
                    default_acr_values: [acrValues],
                    scope: [scope],
                    post_logout_redirect_uris: [chrome.runtime.getURL('options.html')],
                    response_types: ['code'],
                    grant_types: ['authorization_code', 'client_credentials'],
                    application_type: 'web',
                    client_name: 'Gluu-RP-' + uuidv4(),
                    token_endpoint_auth_method: 'client_secret_basic'
                };

                const registrationResp = await registerOIDCClient(registrationUrl, registerObj);

                if (registrationResp !== undefined) {

                    chrome.storage.local.set({
                        oidcClient: {
                            'op_host': issuer,
                            'client_id': registrationResp.data.client_id,
                            'client_secret': registrationResp.data.client_secret,
                            'scope': registerObj.scope,
                            'redirect_uri': registerObj.redirect_uris,
                            'acr_values': registerObj.default_acr_values,
                            'authorization_endpoint': openapiConfig.data.authorization_endpoint,
                            'response_type': registerObj.response_types,
                            'post_logout_redirect_uris': registerObj.post_logout_redirect_uris,

                        }
                    })
                    console.log('OIDC client registered successfully!')
                    console.log("oidcClient is set for client_id: " + registrationResp.data.client_id);

                    return await { result: "success", message: "Regstration successful!" };

                } else {
                    return await { result: "error", message: "Error in registration!" };
                }
            } else {
                return await { result: "error", message: "Error in registration!" };
            }
        } catch (err) {
            console.error(err)
            return { result: "error", message: "Error in registration!" };
        }
    }

    async function registerOIDCClient(registration_endpoint, registerObj) {
        try {
            const registerReqOptions = {
                method: 'POST',
                headers: { 'content-type': 'application/json' },
                data: JSON.stringify(registerObj),
                url: registration_endpoint,
            };

            const response = await axios(registerReqOptions);
            return await response;
        } catch (err) {
            console.error(err)
        }
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

    return (
        <div className="box">
            <legend><span className="number">O</span> Register OIDC Client</legend>
            <legend><span className="error">{error}</span></legend>
            <WindmillSpinner loading={loading} color="#00ced1" />
            <label><b>Issuer:</b><span className="required">*</span></label>
            <input type="text" id="issuer" name="issuer" onChange={updateInputValue} value={issuer}
                placeholder="e.g. https://<op-host>" autoComplete="off" required />

            <label><b>Acr Values:</b><span className="required">*</span></label>
            <input type="text" id="acrValues" name="acrValues" onChange={updateInputValue} value={acrValues}
                placeholder="e.g. basic" autoComplete="off" required />

            <label><b>Scope:</b><span className="required">*</span></label>
            <input type="text" id="scope" name="scope" onChange={updateInputValue} value={scope}
                placeholder="e.g. openid" autoComplete="off" required />

            <legend><span className="error">{error}</span></legend>
            <button id="sbmtButton" onClick={registerClient}>Register</button>
        </div>
    )
};

export default RegisterForm;