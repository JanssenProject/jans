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

import __wbg_init, { init, Cedarling, AuthorizeResult } from "wasm";

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
                    setAuthzResult(result.json_string())
                    console.log("result:", result);
                    let logs = await instance.pop_logs();
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
        
        //reqObj.tokens.access_token = 'eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiY29kZSI6IjNlMmEyMDEyLTA5OWMtNDY0Zi04OTBiLTQ0ODE2MGMyYWIyNSIsImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhdWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhY3IiOiJzaW1wbGVfcGFzc3dvcmRfYXV0aCIsIng1dCNTMjU2IjoiIiwibmJmIjoxNzMxOTUzMDMwLCJzY29wZSI6WyJyb2xlIiwib3BlbmlkIiwicHJvZmlsZSIsImVtYWlsIl0sImF1dGhfdGltZSI6MTczMTk1MzAyNywiZXhwIjoxNzMyMTIxNDYwLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6InVaVWgxaERVUW82UEZrQlBud3BHemciLCJ1c2VybmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjMwNiwidXJpIjoiaHR0cHM6Ly9qYW5zLnRlc3QvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.Pt-Y7F-hfde_WP7ZYwyvvSS11rKYQWGZXTzjH_aJKC5VPxzOjAXqI3Igr6gJLsP1aOd9WJvOPchflZYArctopXMWClbX_TxpmADqyCMsz78r4P450TaMKj-WKEa9cL5KtgnFa0fmhZ1ZWolkDTQ_M00Xr4EIvv4zf-92Wu5fOrdjmsIGFot0jt-12WxQlJFfs5qVZ9P-cDjxvQSrO1wbyKfHQ_txkl1GDATXsw5SIpC5wct92vjAVm5CJNuv_PE8dHAY-KfPTxOuDYBuWI5uA2Yjd1WUFyicbJgcmYzUSVt03xZ0kQX9dxKExwU2YnpDorfwebaAPO7G114Bkw208g';
        //reqObj.tokens.id_token = 'eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiYnhhQ1QwWlFYYnY0c2J6alNEck5pQSIsInN1YiI6InF6eG4xU2NyYjlsV3RHeFZlZE1Da3ktUWxfSUxzcFphUUE2Znl1WWt0dzAiLCJhbXIiOltdLCJpc3MiOiJodHRwczovL2FjY291bnQuZ2x1dS5vcmciLCJub25jZSI6IjI1YjJiMTZiLTMyYTItNDJkNi04YThlLWU1ZmE5YWI4ODhjMCIsInNpZCI6IjZkNDQzNzM0LWI3YTItNGVkOC05ZDNhLTE2MDZkMmY5OTI0NCIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiYXVkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwiYWNyIjoic2ltcGxlX3Bhc3N3b3JkX2F1dGgiLCJjX2hhc2giOiJWOGg0c085Tnp1TEthd1BPLTNETkxBIiwibmJmIjoxNzMxOTUzMDMwLCJhdXRoX3RpbWUiOjE3MzE5NTMwMjcsImV4cCI6MTczMTk1NjYzMCwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6ImlqTFpPMW9vUnlXcmdJbjdjSWROeUEiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjozMDcsInVyaSI6Imh0dHBzOi8vamFucy50ZXN0L2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.Nw7MRaJ5LtDak_LdEjrICgVOxDwd1p1I8WxD7IYw0_mKlIJ-J_78rGPski9p3L5ZNCpXiHtVbnhc4lJdmbh-y6mrD3_EY_AmjK50xpuf6YuUuNVtFENCSkj_irPLkIDG65HeZherWsvH0hUn4FVGv8Sw9fjny9Doi-HGHnKg9Qvphqre1U8hCphCVLQlzXAXmBkbPOC8tDwId5yigBKXP50cdqDcT-bjXf9leIdGgq0jxb57kYaFSElprLN9nUygM4RNCn9mtmo1l4IsdTlvvUb3OMAMQkRLfMkiKBjjeSF3819mYRLb3AUBaFH16ZdHFBzTSB6oA22TYpUqOLihMg';
        //reqObj.tokens.userinfo_token = 'eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInJvbGUiOlsiQ2FzYUFkbWluIl0sImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsImdpdmVuX25hbWUiOiJBZG1pbiIsIm1pZGRsZV9uYW1lIjoiQWRtaW4iLCJpbnVtIjoiYTZhNzAzMDEtYWY0OS00OTAxLTk2ODctMGJjZGNmNGUzNGZhIiwiY2xpZW50X2lkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwiYXVkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwidXBkYXRlZF9hdCI6MTczMTY5ODEzNSwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsIm5pY2tuYW1lIjoiQWRtaW4iLCJmYW1pbHlfbmFtZSI6IlVzZXIiLCJqdGkiOiJPSW4zZzFTUFNEU0tBWUR6RU5Wb3VnIiwiZW1haWwiOiJhZG1pbkBqYW5zLnRlc3QiLCJqYW5zQWRtaW5VSVJvbGUiOlsiYXBpLWFkbWluIl19.CIahQtRpoTkIQx8KttLPIKH7gvGG8OmYCMzz7wch6k792DVYQG1R7q3sS9Ema1rO5Fm_GgjOsR0yTTMKsyhHDLBwkDd3cnMLgsh2AwVFZvxtpafTlUAPfjvMAy9YTtkPcY6rNUhsYLSSOA83kt6pHdIv5nI-G6ybqgg-bLBRpwZDoOV0TulRhmuukdiuugTXHT6Bb-K3ZeYs8CwewztnxoFTSDghSzq7VZIraV8SLTBLx5_xswn9mefamyB2XNN3o6vXuMyf4BEbYSCuJ3pu6YtNgfyWwt9cF8PYe4PVLoXZuJKN-cy4qrtgy43QXPCg96jSQUJqgLb5ZL5_3udm2Q';

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
                    <div>
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