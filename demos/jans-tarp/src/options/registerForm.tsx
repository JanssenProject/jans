import React, { useState, KeyboardEventHandler, useCallback, useEffect } from 'react'
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
import StyledDropzone from './StyledDropzone';
import { jwtDecode } from "jwt-decode";
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
    const [uploadSsa, setUploadSsa] = useState(false);
    const [showClientExpiry, setShowClientExpiry] = useState(false);
    const [issuerOption, setIssuerOption] = useState<readonly IOption[]>([]);
    const [scopeOption, setScopeOption] = useState<readonly IOption[]>([createOption('openid')]);
    const [clientExpiryDate, setClientExpiryDate] = useState(moment().add(1, 'days').toDate());
    const [ssaJwt, setSsaJwt] = useState(null)
    const REGISTRATION_ERROR = 'Error in registration. Check web console for logs.'

    const readJWTFile = (selectedFile) => {
        const reader = new FileReader()

        reader.onload = () => {
            const token = reader.result
            setSsaJwt(token)
        }

        const blob = new Blob([selectedFile])
        reader.readAsText(blob)
    }

    async function triggerClientRegistration() {
        try {
            if (uploadSsa) {
                const response = await processForClientRegiWithSSA()
                if (response.result !== 'success') {
                    setError(REGISTRATION_ERROR);
                }
                setPageLoading(false);
            } else if (validate()) {

                setPageLoading(true);
                const response = await processForClientRegiWithoutSSA()
                if (response.result !== 'success') {
                    setError(REGISTRATION_ERROR);
                }
                setPageLoading(false);
            }
        } catch (err) {
            console.error(err)
        }
    }

    async function processForClientRegiWithSSA() {
        try {

            const ssaJson = jwtDecode(ssaJwt)

            if (ssaJson.iss == null) {
                setError('Issuer not found in SSA.')
            }

            const issuer = ssaJson.iss

            const configurationUrl = generateOpenIdConfigurationURL(issuer)
            const openapiConfig = await getOpenidConfiguration(configurationUrl)

            if (openapiConfig != undefined) {
                chrome.storage.local.set({ opConfiguration: openapiConfig.data }).then(() => {
                    console.log("openapiConfig is set to " + openapiConfig);
                });

                const registrationUrl = openapiConfig.data.registration_endpoint;

                var registerObj: ILooseObject = {
                    redirect_uris: [issuer],
                    software_statement: ssaJwt,
                    response_types: ['code'],
                    grant_types: ['authorization_code'],
                    application_type: 'web',
                    client_name: 'jans-tarp-' + uuidv4(),
                    token_endpoint_auth_method: 'client_secret_basic',
                    post_logout_redirect_uris: [chrome.runtime.getURL('options.html')]
                }

                const registrationResp = await registerOIDCClient(registrationUrl, registerObj)

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
                            'expire_at': clientExpiryDate.getTime(),
                            'showClientExpiry': showClientExpiry

                        }
                    })
                    console.log('OIDC client registered successfully!')
                    console.log("oidcClient is set for client_id: " + registrationResp.data.client_id)

                    return await { result: "success", message: "Regstration successful!" }

                } else {
                    return await { result: "error", message: REGISTRATION_ERROR }
                }
            } else {
                return await { result: "error", message: "Error in fetching Openid configuration!" }
            }
        } catch (err) {
            console.error(err)
            return { result: "error", message: REGISTRATION_ERROR + err.message}
        }
    }

    async function processForClientRegiWithoutSSA() {
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
                    redirect_uris: [issuer],
                    scope: scope,
                    post_logout_redirect_uris: [chrome.runtime.getURL('options.html')],
                    response_types: ['code'],
                    grant_types: ['authorization_code'],
                    application_type: 'web',
                    client_name: 'jans-tarp-' + uuidv4(),
                    token_endpoint_auth_method: 'client_secret_basic'
                };

                if (showClientExpiry) {
                    registerObj.lifetime = ((clientExpiryDate.getTime() - moment().toDate().getTime()) / 1000);
                }

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
                            'expire_at': clientExpiryDate.getTime(),
                            'showClientExpiry': showClientExpiry

                        }
                    })
                    console.log('OIDC client registered successfully!')
                    console.log("oidcClient is set for client_id: " + registrationResp.data.client_id);

                    return await { result: "success", message: "Regstration successful!" };

                } else {
                    return await { result: "error", message: REGISTRATION_ERROR };
                }
            } else {
                return await { result: "error", message: "Error in fetching Openid configuration!" };
            }
        } catch (err) {
            console.error(err)
            return { result: "error", message: REGISTRATION_ERROR };
        }
    }

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

        if (!issuer.includes('https') && !issuer.includes('http')) {
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
        if (showClientExpiry && !clientExpiryDate) {
            errorField += 'client-expiry ';
        }
        if (errorField.trim() !== '') {
            setError('The following fields are mandatory: ' + errorField);
            return false;
        }

        return true;
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
            {error.length > 0 ? <legend><span className="redFont">{error}</span></legend> : ""}
            <WindmillSpinner loading={pageLoading} color="#00ced1" />
            <label><input type="checkbox" onChange={() => setUploadSsa(!uploadSsa)} /><b>Register using SSA</b></label>
            {uploadSsa ?
                <>
                    <StyledDropzone submitFile={readJWTFile} />
                </>
                :
                <>
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

                    <label><input type="checkbox" onChange={() => setShowClientExpiry(!showClientExpiry)} /><b>Set Client Expiry</b></label>
                    {showClientExpiry ?
                        <>
                            <label><b>Client expiry date</b><span className="required">*</span> <span style={{ fontSize: 12 }}>(Select the date)</span> :</label>
                            <DatePicker
                                showTimeSelect
                                selected={clientExpiryDate}
                                onChange={(date) => setClientExpiryDate(date)}
                                minDate={new Date()}
                                className="inputText inputStyle"
                                dateFormat="yyyy/MM/dd h:mm aa"
                            />
                        </> : ''}
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

                    <legend><span className="redFont">{error}</span></legend>
                </>
            }
            <button id="sbmtButton" onClick={triggerClientRegistration}>Register</button>
        </div>
    )
};

export default RegisterForm;