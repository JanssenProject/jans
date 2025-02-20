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
import __wbg_init, { init, Cedarling, AuthorizeResult } from '@janssenproject/cedarling_wasm';

const UserDetails = ({ data, notifyOnDataChange }) => {
    const [loading, setLoading] = useState(false);
    const [showMoreIdToken, setShowMoreIdToken] = useState(false);
    const [showMoreAT, setShowMoreAT] = useState(false);
    const [showMoreUI, setShowMoreUI] = useState(false);
    const [context, setContext] = React.useState({});
    const [action, setAction] = React.useState("");
    const [accessToken, setAccessToken] = React.useState(false);
    const [userInfoToken, setUserInfoToken] = React.useState(false);
    const [idToken, setIdToken] = React.useState(false);
    const [resource, setResource] = React.useState({});
    const [cedarlingBootstrapPresent, setCedarlingBootstrapPresent] = React.useState(false);
    const [errorMessage, setErrorMessage] = React.useState("")
    const [authzResult, setAuthzResult] = React.useState("")
    const [authzLogs, setAuthzLogs] = React.useState("")
    const [logType, setLogType] = React.useState('Decision');

    React.useEffect(() => {
        chrome.storage.local.get(["authzRequest"], (authzRequest) => {
            if (!isEmpty(authzRequest) && Object.keys(authzRequest).length !== 0) {
                setContext(authzRequest.authzRequest.context);
                setAction(authzRequest.authzRequest.action);
                setResource(authzRequest.authzRequest.resource);
            }
        });
        chrome.storage.local.get(["cedarlingConfig"], async (cedarlingConfig) => {
            setCedarlingBootstrapPresent(false);
            if (Object.keys(cedarlingConfig).length !== 0 && !isEmpty(cedarlingConfig?.cedarlingConfig)) {
                setCedarlingBootstrapPresent(true);
            }
        });
    }, [])

    const triggerCedarlingAuthzRequest = async () => {
        setAuthzResult("");
        setAuthzLogs("");
        let reqObj = await createCedarlingAuthzRequestObj();
        chrome.storage.local.get(["cedarlingConfig"], async (cedarlingConfig) => {
            let instance: Cedarling;
            try {
                if (Object.keys(cedarlingConfig).length !== 0) {
                    await __wbg_init();
                    instance = await init(!isEmpty(cedarlingConfig?.cedarlingConfig) ? cedarlingConfig?.cedarlingConfig[0] : undefined);
                    let result: AuthorizeResult = await instance.authorize(reqObj);
                    let logs = await instance.get_logs_by_request_id_and_tag(result.request_id, logType);
                    setAuthzResult(result.json_string())
                    if (logs.length != 0) {
                        let pretty_logs = logs.map(log => JSON.stringify(log, null, 2));
                        setAuthzLogs(pretty_logs.toString());
                    }

                }
            } catch (err) {
                setAuthzResult(err);
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
        let reqObj = { tokens: { access_token: '', id_token: '', userinfo_token: '' }, action: "", resource: {}, context: {} };
        if (accessToken) {
            reqObj.tokens.access_token = (!!data ? data?.access_token : '');
        }
    
        if (idToken) {
            reqObj.tokens.id_token = (!!data ? data?.id_token : '');
        }
        
        if (userInfoToken) {
            reqObj.tokens.userinfo_token = (!!data ? data?.userDetails : '');
        }
        
        reqObj.action = action;
        reqObj.context = context;
        reqObj.resource = resource;
        
        chrome.storage.local.set({ authzRequest: reqObj });
        return reqObj;
    }

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

    function isEmpty(value) {
        return (value == null || value.length === 0);
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
                                <p>{showMoreAT ? (!!data ? data?.access_token : '') : (!!data ? data?.access_token.substring(0, 250).concat(' ...') : '')}</p>
                                <a href="#" onClick={() => setShowMoreAT(!showMoreAT)}>{showMoreAT ? "Show less" : "Show more"}</a>
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
                                <p>{showMoreIdToken ? (!!data ? data?.id_token : '') : (!!data ? data?.id_token.substring(0, 250).concat(' ...') : '')}</p>
                                <a href="#" onClick={() => setShowMoreIdToken(!showMoreIdToken)}>{showMoreIdToken ? "Show less" : "Show more"}</a>
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
                        <strong>User Details</strong>
                        <p>{showMoreUI ? (!!data ? data?.userDetails : '') : (!!data ? data?.userDetails.substring(0, 250).concat(' ...') : '')}</p>
                        <a href="#" onClick={() => setShowMoreUI(!showMoreUI)}>{showMoreUI ? "Show less" : "Show more"}</a>
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
                        <FormControlLabel control={<Checkbox color="success" onChange={() => setAccessToken(!accessToken)} />} label="Access Token" />
                        <FormControlLabel control={<Checkbox color="success" onChange={() => setUserInfoToken(!userInfoToken)} />} label="Userinfo Token" />
                        <FormControlLabel control={<Checkbox color="success" onChange={() => setIdToken(!idToken)} />} label="Id Token" />

                        <TextField
                            error={errorMessage.length !== 0}
                            autoFocus
                            required
                            margin="dense"
                            id="action"
                            name="action"
                            label="Action"
                            type="text"
                            fullWidth
                            variant="outlined"
                            helperText={errorMessage}
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