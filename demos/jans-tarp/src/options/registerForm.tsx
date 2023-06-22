import React, { useState, KeyboardEventHandler } from 'react'
import axios from 'axios';
import './options.css'
import { v4 as uuidv4 } from 'uuid';
import { WindmillSpinner } from 'react-spinner-overlay'
import CreatableSelect from 'react-select/creatable';
import { IOption } from './IOption';
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import moment from 'moment';
import { ILooseObject } from './ILooseObject';
const components = {
    DropdownIndicator: null,
};

const createOption = (label: string) => ({
    label,
    value: label,
});

const RegisterForm = (data) => {
    const [error, setError] = useState("");
    const [pageLoading, setPageLoading] = useState(false);
    const [inputValueIssuer, setInputValueIssuer] = useState('');
    const [inputValueScope, setInputValueScope] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [issuerOption, setIssuerOption] = useState<readonly IOption[]>([]);
    const [scopeOption, setScopeOption] = useState<readonly IOption[]>([createOption('openid')]);
    const [clientExpiryDate, setClientExpiryDate] = useState(moment().add(1, 'days').toDate());
    
    const handleKeyDown: KeyboardEventHandler = async (event) => {
        const inputId = (event.target as HTMLInputElement).id;
        if (inputId === 'issuer') {
            if (!inputValueIssuer) return;
            switch (event.key) {
                case 'Enter':
                case 'Tab':
                    setError('');
                    if (await validateIssuerOnEnter(inputValueIssuer)) {
                        setIssuerOption([createOption(inputValueIssuer)]);
                        setIsLoading(false);
                        setPageLoading(false);
                        setInputValueIssuer('');
                        event.preventDefault();
                    } else {
                        setIsLoading(false);
                        setPageLoading(false);
                        setError('Invalid input. Either enter correct Issuer or OpenID Configuration URL.')
                    }
            }
        } else if (inputId === 'scope') {
            if (!inputValueScope) return;
            switch (event.key) {
                case 'Enter':
                case 'Tab':
                    setScopeOption((prev) => [...prev, createOption(inputValueScope)]);
                    setInputValueScope('');
                    event.preventDefault();
            }

        }
    };

    function generateOpenIdConfigurationURL(issuer) {
        if (issuer.length === 0) {
            return '';
        }
        if (!issuer.includes('/.well-known/openid-configuration')) {
            issuer = issuer + '/.well-known/openid-configuration';
        }

        if (!issuer.includes('https') || !issuer.includes('http')) {
            issuer = 'https://' + issuer;
        }
        return issuer;
    }

    async function validateIssuerOnEnter(issuer) {
        setIsLoading(true);
        setPageLoading(true);
        if (issuer.length === 0) {
            return false;
        }
        issuer = generateOpenIdConfigurationURL(issuer);
        try {
            const opConfiguration = await getOpenidConfiguration(issuer);
            if (!opConfiguration || !opConfiguration.data.issuer) {
                return false;
            }
            return true;
        } catch (err) {
            return false;
        }
    }

    function validate() {
        let errorField = ''

        if (issuerOption.length === 0) {
            errorField += 'issuer ';
        }
        if (scopeOption.length === 0) {
            errorField += 'scope ';
        }
        if(!clientExpiryDate) {
            errorField += 'client-expiry ';
        }
        if (errorField.trim() !== '') {
            setError('The following fields are mandatory: ' + errorField);
            return false;
        }

        return true;
    }

    async function registerClient() {
        if (validate()) {
            try {
                setPageLoading(true);
                const response = await register()
                if (response.result !== 'success') {
                    setError('Error in registration.');
                }
                setPageLoading(false);
            } catch (err) {
                console.error(err)
            }
        }
    }

    async function register() {
        try {
            const opConfigurationEndpoint = generateOpenIdConfigurationURL(issuerOption.map((iss) => iss.value)[0]);
            const opConfigurationEndpointURL = new URL(opConfigurationEndpoint);
            const issuer = opConfigurationEndpointURL.protocol + '//' + opConfigurationEndpointURL.hostname;
            const scope = scopeOption.map((ele) => ele.value).join(" ");
            const openapiConfig = await getOpenidConfiguration(opConfigurationEndpoint);

            if (openapiConfig != undefined) {
                chrome.storage.local.set({ opConfiguration: openapiConfig.data }).then(() => {
                    console.log("openapiConfig is set to " + openapiConfig);
                });

                const registrationUrl = openapiConfig.data.registration_endpoint;

                var registerObj: ILooseObject = {
                    issuer: issuer,
                    redirect_uris: [issuer],
                    scope: scope,
                    post_logout_redirect_uris: [chrome.runtime.getURL('options.html')],
                    response_types: ['code'],
                    grant_types: ['authorization_code', 'client_credentials'],
                    application_type: 'web',
                    client_name: 'Gluu-RP-' + uuidv4(),
                    token_endpoint_auth_method: 'client_secret_basic',
                    lifetime: ((clientExpiryDate.getTime() - moment().toDate().getTime())/1000)
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
                            'authorization_endpoint': openapiConfig.data.authorization_endpoint,
                            'response_type': registerObj.response_types,
                            'post_logout_redirect_uris': registerObj.post_logout_redirect_uris,
                            'expire_at': clientExpiryDate.getTime()

                        }
                    })
                    console.log('OIDC client registered successfully!')
                    console.log("oidcClient is set for client_id: " + registrationResp.data.client_id);

                    return await { result: "success", message: "Regstration successful!" };

                } else {
                    return await { result: "error", message: "Error in registration!" };
                }
            } else {
                return await { result: "error", message: "Error in fetching Openid configuration!" };
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
            console.error('Error in fetching Openid configuration: ' + err)
        }
    }

    async function getOpenidConfiguration(opConfigurationEndpoint) {
        try {
            const oidcConfigOptions = {
                method: 'GET',
                url: opConfigurationEndpoint,
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
            <WindmillSpinner loading={pageLoading} color="#00ced1" />
            <label><b>Issuer</b><span className="required">*</span> <span style={{ fontSize: 12 }}>(Enter OpenID Provider URL and press ENTER to validate)</span> :</label>
            <CreatableSelect
                inputId="issuer"
                components={components}
                inputValue={inputValueIssuer}
                isClearable
                isMulti
                isLoading={isLoading}
                menuIsOpen={false}
                onChange={(newValue) => setIssuerOption(newValue)}
                onInputChange={(newValue) => setInputValueIssuer(newValue)}
                onKeyDown={handleKeyDown}
                placeholder="Type something and press enter..."
                value={issuerOption}
                className="inputText"
            />

            <label><b>Client expiry date</b><span className="required">*</span> <span style={{ fontSize: 12 }}>(Select the date)</span> :</label>
            <DatePicker
                showTimeSelect
                selected={clientExpiryDate}
                onChange={(date) => setClientExpiryDate(date)}
                minDate={new Date()}
                className="inputText inputStyle"
                dateFormat="yyyy/MM/dd h:mm aa"
            />

            <label><b>Scopes</b><span className="required">*</span> <span style={{ fontSize: 12 }}>(Type and press enter)</span> :</label>

            <CreatableSelect
                inputId='scope'
                components={components}
                inputValue={inputValueScope}
                isClearable
                isMulti
                menuIsOpen={false}
                onChange={(newValue) => setScopeOption(newValue)}
                onInputChange={(newValue) => setInputValueScope(newValue)}
                onKeyDown={handleKeyDown}
                placeholder="Type something and press enter..."
                value={scopeOption}
                className="inputText"
            />

            <legend><span className="error">{error}</span></legend>
            <button id="sbmtButton" onClick={registerClient}>Register</button>
        </div>
    )
};

export default RegisterForm;