import React, { useState, useEffect } from 'react'
import { v4 as uuidv4 } from 'uuid';
import './options.css'
import './alerts.css';
import { WindmillSpinner } from 'react-spinner-overlay'
import { JsonEditor } from 'json-edit-react'
import TextField from '@mui/material/TextField';
import InputLabel from '@mui/material/InputLabel';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import Button from '@mui/material/Button';
import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import Typography from '@mui/material/Typography';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import initWasm, { init, Cedarling, AuthorizeResult } from '@janssenproject/cedarling_wasm';
import { jwtDecode } from "jwt-decode";
import { IJWT } from './IJWT';
import Utils from './Utils';
const UserDetails = ({ data, notifyOnDataChange }) => {
    const [loading, setLoading] = useState(false);
    const [showPayloadIdToken, setShowPayloadIdToken] = useState(false);
    const [showPayloadAT, setShowPayloadAT] = useState(false);
    const [showPayloadUI, setShowPayloadUI] = useState(false);
    const [context, setContext] = React.useState({});
    const [action, setAction] = React.useState("");
    const [tokenSelection, setTokenSelection] = useState({ accessToken: false, userInfo: false, idToken: false });
    const [decodedTokens, setDecodedTokens] = React.useState<{
    accessToken: IJWT;
    userInfoToken: IJWT;
    idToken: IJWT;
    }>({
        accessToken: { header: {}, payload: {} },
        userInfoToken: { header: {}, payload: {} },
        idToken: { header: {}, payload: {} },
    });

    const [resource, setResource] = React.useState({});
    const [cedarlingBootstrapPresent, setCedarlingBootstrapPresent] = React.useState(false);
    const [authzResult, setAuthzResult] = React.useState("")
    const [authzLogs, setAuthzLogs] = React.useState("")
    const [logType, setLogType] = React.useState('Decision');

    React.useEffect(() => {
        chrome.storage.local.get(["authzRequest"], (authzRequest) => {
            if (!Utils.isEmpty(authzRequest) && Object.keys(authzRequest).length !== 0) {
                setContext(authzRequest.authzRequest.context);
                setAction(authzRequest.authzRequest.action);
                setResource(authzRequest.authzRequest.resource);
            }
        });
        chrome.storage.local.get(["cedarlingConfig"], async (cedarlingConfig) => {
            setCedarlingBootstrapPresent(false);
            if (Object.keys(cedarlingConfig).length !== 0 && !Utils.isEmpty(cedarlingConfig?.cedarlingConfig)) {
                setCedarlingBootstrapPresent(true);
            }
        });
        if (data) {
            setDecodedTokens({
                accessToken: decodeJWT(data.access_token),
                userInfoToken: decodeJWT(data.userDetails),
                idToken: decodeJWT(data.id_token),
            });
        }
    }, [data])

    const decodeJWT = (token: string | undefined): IJWT => {
        try {
            if (!token) return { header: {}, payload: {} }; // Handle undefined token
            const payload = jwtDecode(token);
            const header = jwtDecode(token, { header: true });
            return { header, payload };
        } catch (error) {
            console.error("Error decoding JWT:", error);
            return { header: {}, payload: {} }; // Return empty object on error
        }
    };
    
    const triggerCedarlingAuthzRequest = async () => {
        setAuthzResult("");
        setAuthzLogs("");
        let reqObj = await createCedarlingAuthzRequestObj();
        chrome.storage.local.get(["cedarlingConfig"], async (cedarlingConfig) => {
            let instance: Cedarling;
            try {
                if (Object.keys(cedarlingConfig).length !== 0) {
                    await initWasm();
                    instance = await init(!Utils.isEmpty(cedarlingConfig?.cedarlingConfig) ? cedarlingConfig?.cedarlingConfig[0] : undefined);
                    let result: AuthorizeResult = await instance.authorize(reqObj);
                    let logs = await instance.get_logs_by_request_id_and_tag(result.request_id, logType);
                    setAuthzResult(result.json_string())
                    if (logs.length != 0) {
                        let pretty_logs = logs.map(log => JSON.stringify(log, null, 2));
                        setAuthzLogs(pretty_logs.toString());
                    }

                }
            } catch (err) {
                setAuthzResult(err.toString());
                console.log("err:", err);
                let logs = await instance.pop_logs();
                if (logs.length != 0) {
                    let pretty_logs = logs.map(log => JSON.stringify(log, null, 2));
                    setAuthzLogs(pretty_logs.toString());
                }
            }

        });

    }

    const createCedarlingAuthzRequestObj = async () => {
        const reqObj = {
            tokens: {
                ...(tokenSelection.accessToken && { access_token: data?.access_token}),
                ...(tokenSelection.idToken && { id_token: data?.id_token}),
                ...(tokenSelection.userInfo && { userinfo_token: data?.userDetails}),
            },
            action,
            context,
            resource,
        };

        chrome.storage.local.set({ authzRequest: reqObj });
        return reqObj;
    };

    async function logout() {
        setLoading(true);
        try {
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
        } catch (err) {
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
                    fetch(`${JSON.parse(openidConfiguration).opConfiguration.end_session_endpoint}?state=${uuidv4()}&post_logout_redirect_uri=${chrome.runtime.getURL('options.html')}&id_token_hint=${JSON.parse(loginDetails).loginDetails.id_token}`)
                }
            });

        }
        setLoading(false);
        notifyOnDataChange("true");
    }

    const handleLogTypeChange = (
        event: React.MouseEvent<HTMLElement>,
        newLogType: string,
      ) => {
        setLogType(newLogType);
      };

    return (
        <div className="box">
            <div className="w3-panel w3-pale-yellow w3-border">
                <WindmillSpinner loading={loading} color="#00ced1" />
                <br />
            </div>
            <legend><span className="number">O</span> User Details:</legend>
            <hr />
            {data?.displayToken ?
                <>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<ExpandMoreIcon />}
                            aria-controls="panel1-content"
                            id="panel1-header"
                        >
                            <Typography component="span"><strong>Access Token</strong></Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <div className="alert alert-success alert-dismissable fade in">
                                <p>{showPayloadAT ? (!!data ? 
                                    <>
                                        <JsonEditor collapse={true} viewOnly={true} data={decodedTokens.accessToken.header} rootName="header" />
                                        <JsonEditor data={decodedTokens.accessToken.payload} collapse={true} viewOnly={true} rootName="payload" />
                                    </> 
                                : '') : (!!data ? data?.access_token : '')}</p>
                                <a href="#!" onClick={() => setShowPayloadAT(!showPayloadAT)}>{showPayloadAT ? "Show JWT" : "Show Payload"}</a>
                            </div>
                        </AccordionDetails>
                    </Accordion>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<ExpandMoreIcon />}
                            aria-controls="panel1-content"
                            id="panel1-header"
                        >
                            <Typography component="span"><strong>Id Token</strong></Typography>
                        </AccordionSummary>
                        <AccordionDetails>

                            <div className="alert alert-success alert-dismissable fade in">
                                <p>{showPayloadIdToken ? (!!data ? 
                                    <>
                                        <JsonEditor collapse={true} viewOnly={true} data={decodedTokens.idToken.header} rootName="header" />
                                        <JsonEditor data={decodedTokens.idToken.payload} collapse={true} viewOnly={true} rootName="payload" />
                                    </>
                                    : '') : (!!data ? data?.id_token : '')}</p>
                                <a href="#!" onClick={() => setShowPayloadIdToken(!showPayloadIdToken)}>{showPayloadIdToken ? "Show JWT" : "Show Payload"}</a>
                            </div>
                        </AccordionDetails>
                    </Accordion>
                </>
                : ''}
            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon />}
                    aria-controls="panel1-content"
                    id="panel1-header"
                >
                    <Typography component="span"><strong>User Details</strong></Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <div className="alert alert-success alert-dismissable fade in">
                        <p>{showPayloadUI ? (!!data ? 
                            <>
                                <JsonEditor collapse={true} viewOnly={true} data={decodedTokens.userInfoToken.header} rootName="header" />
                                <JsonEditor data={decodedTokens.userInfoToken.payload} collapse={true} viewOnly={true} rootName="payload" />
                            </>
                            : '') : (!!data ? data?.userDetails : '')}</p>
                        <a href="#!" onClick={() => setShowPayloadUI(!showPayloadUI)}>{showPayloadUI ? "Show JWT" : "Show Payload"}</a>
                    </div>
                </AccordionDetails>
            </Accordion>
            {cedarlingBootstrapPresent ? 
            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon />}
                    aria-controls="panel1-content"
                    id="panel1-header"
                >
                    <Typography component="span"><strong>Cedarling Authz Request Form</strong></Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                        <InputLabel id="principal-value-label">Principal</InputLabel>
                        <FormControlLabel control={<Checkbox color="success" onChange={() => setTokenSelection((prev) => ({ ...prev, accessToken: !prev.accessToken }))} />} label="Access Token" />
                        <FormControlLabel control={<Checkbox color="success" onChange={() => setTokenSelection((prev) => ({ ...prev, userInfo: !prev.userInfo }))} />} label="Userinfo Token" />
                        <FormControlLabel control={<Checkbox color="success" onChange={() => setTokenSelection((prev) => ({ ...prev, idToken: !prev.idToken }))} />} label="Id Token" />

                        <TextField
                            autoFocus
                            required
                            margin="dense"
                            id="action"
                            name="action"
                            label="Action"
                            type="text"
                            fullWidth
                            variant="outlined"
                            value={action}
                            onChange={(e) => {
                                setAction(e.target.value);
                            }}
                        />
                        <InputLabel id="resource-value-label">Resource</InputLabel>
                        <JsonEditor data={resource} setData={setResource} rootName="resource" />
                        <InputLabel id="context-value-label">Context</InputLabel>
                        <JsonEditor data={context} setData={setContext} rootName="context" />

                        <InputLabel id="principal-value-label">Log Type</InputLabel>
                        <ToggleButtonGroup
                            color="primary"
                            value={logType}
                            exclusive
                            onChange={handleLogTypeChange}
                            aria-label="Platform"
                            >
                            <ToggleButton value="Decision">Decision</ToggleButton>
                            <ToggleButton value="System">System</ToggleButton>
                            <ToggleButton value="Metric">Metric</ToggleButton>
                        </ToggleButtonGroup>
                        <hr />
                        <Button variant="outlined" color="success" onClick={triggerCedarlingAuthzRequest}>Cedarling Authz Request</Button>
                    </div>
                </AccordionDetails>
            </Accordion> : ''}
            {!!authzResult ?
                <Accordion defaultExpanded>
                    <AccordionSummary
                        expandIcon={<ExpandMoreIcon />}
                        aria-controls="panel1-content"
                        id="panel1-header"
                    >
                        <Typography component="span">Cedarling Authz Result</Typography>
                    </AccordionSummary>
                    <AccordionDetails>
                        <TextField
                            autoFocus
                            required
                            margin="dense"
                            id="authzResult"
                            name="authzResult"
                            label="Authz Result"
                            rows={12}
                            multiline
                            type="text"
                            fullWidth
                            variant="outlined"
                            value={authzResult}
                        />
                        <Button variant="text" color="success" onClick={() => setAuthzResult('')}>Reset</Button>
                    </AccordionDetails>
                </Accordion> : ''}
            {!!authzLogs ?
                <Accordion>
                    <AccordionSummary
                        expandIcon={<ExpandMoreIcon />}
                        aria-controls="panel2-content"
                        id="panel2-header"
                    >
                        <Typography component="span">Cedarling Authz Logs</Typography>
                    </AccordionSummary>
                    <AccordionDetails>
                        <TextField
                            autoFocus
                            required
                            margin="dense"
                            id="authzLogs"
                            name="authzLogs"
                            label="Authz Logs"
                            rows={12}
                            multiline
                            type="text"
                            fullWidth
                            variant="outlined"
                            value={authzLogs}
                        />
                        <Button variant="text" color="success" onClick={() => setAuthzLogs('')}>Reset</Button>
                    </AccordionDetails>
                </Accordion> : ''}

            <hr />
            <Button variant="contained" id="logoutButton" color="success" onClick={logout}>Logout</Button>
        </div>
    )
};

export default UserDetails;