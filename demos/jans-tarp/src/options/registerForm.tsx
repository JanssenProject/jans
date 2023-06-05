import React, { useState, KeyboardEventHandler } from 'react'
import axios from 'axios';
import './options.css'
import { v4 as uuidv4 } from 'uuid';
import { WindmillSpinner } from 'react-spinner-overlay'
import CreatableSelect from 'react-select/creatable';

const components = {
    DropdownIndicator: null,
};

interface Option {
    readonly label: string;
    readonly value: string;
}

const createOption = (label: string) => ({
    label,
    value: label,
});

const RegisterForm = (data) => {
    const [issuer, setIssuer] = useState("");
    const [acrValues, setAcrValues] = useState(['basic']);
    const [error, setError] = useState("");
    const [scope, setScope] = useState(['openid']);
    const [loading, setLoading] = useState(false);
    const [inputValueScope, setInputValueScope] = useState('');
    const [inputValueAcr, setInputValueAcr] = useState('');
    const [scopeOption, setScopeOption] = useState<readonly Option[]>([createOption('openid')]);
    const [acrOption, setAcrOption] = useState<readonly Option[]>([createOption('basic')]);

    const handleScopeKeyDown: KeyboardEventHandler = (event) => {
        console.log(JSON.stringify(scopeOption))
        if (!inputValueScope) return;
        switch (event.key) {
            case 'Enter':
            case 'Tab':
                setScopeOption((prev) => [...prev, createOption(inputValueScope)]);
                setScope((prev) => [...prev, inputValueScope])
                setInputValueScope('');
                event.preventDefault();
        }
    };

    const handleAcrValueKeyDown: KeyboardEventHandler = (event) => {
        console.log(JSON.stringify(acrOption))
        if (!inputValueAcr) return;
        switch (event.key) {
            case 'Enter':
            case 'Tab':
                setAcrOption((prev) => [createOption(inputValueAcr)]);
                setAcrValues((prev) => [inputValueAcr])
                setInputValueAcr('');
                event.preventDefault();
        }
    };
    

    function updateInputValue(event) {
        if (event.target.id === 'issuer') {
            setIssuer(event.target.value);
        }
    }

    function validateState() {
        console.log(scope);
        let errorField = ''
        if (issuer === '') {
            errorField += 'issuer ';
        }
        if (scope.length === 0) {
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
                    default_acr_values: acrValues,
                    scope: scope,
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

            <label><b>Acr Values</b><span className="required">*</span> (Type and press enter) :</label>

            <CreatableSelect
                components={components}
                inputValue={inputValueAcr}
                isClearable
                isMulti
                menuIsOpen={false}
                onChange={(newValue) => setAcrOption(newValue)}
                onInputChange={(newValue) => setInputValueAcr(newValue)}
                onKeyDown={handleAcrValueKeyDown}
                placeholder="Type something and press enter..."
                value={acrOption}
                className="typeahead"
            />

            <label><b>Scopes</b><span className="required">*</span> (Type and press enter) :</label>

            <CreatableSelect
                components={components}
                inputValue={inputValueScope}
                isClearable
                isMulti
                menuIsOpen={false}
                onChange={(newValue) => setScopeOption(newValue)}
                onInputChange={(newValue) => setInputValueScope(newValue)}
                onKeyDown={handleScopeKeyDown}
                placeholder="Type something and press enter..."
                value={scopeOption}
                className="typeahead"
            />
    
            <legend><span className="error">{error}</span></legend>
            <button id="sbmtButton" onClick={registerClient}>Register</button>
        </div>
    )
};

export default RegisterForm;