import React, { useState } from 'react'
import './options.css'
import './alerts.css';
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
import Utils from './Utils';
const CedarlingSignedAuthz = ({ data }) => {
    const [context, setContext] = React.useState({});
    const [action, setAction] = React.useState("");
    const [tokenSelection, setTokenSelection] = useState({ accessToken: false, userInfo: false, idToken: false });
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
    }, [data])
    
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

    const handleLogTypeChange = (
        event: React.MouseEvent<HTMLElement>,
        newLogType: string,
      ) => {
        setLogType(newLogType);
      };

    return (
        <div className="box">
            {cedarlingBootstrapPresent ? 
            <Accordion defaultExpanded>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon />}
                    aria-controls="panel1-content"
                    id="panel1-header"
                >
                    <Typography component="span"><strong>Cedarling Signed Authz Request Form</strong></Typography>
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
        </div>
    )
};

export default CedarlingSignedAuthz;