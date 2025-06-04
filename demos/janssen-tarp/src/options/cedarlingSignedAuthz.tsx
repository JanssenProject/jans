import React, { useState } from 'react'
import './options.css'
import './alerts.css';
import { JsonEditor } from 'json-edit-react'
import Box from '@mui/material/Box';
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
import Stack from '@mui/material/Stack';
import initWasm, { init, Cedarling, AuthorizeResult } from '@janssenproject/cedarling_wasm';
import Utils from './Utils';
const CedarlingSignedAuthz = ({ data }) => {
    const [formFields, setFormFields] = useState({ action: "", context: {}, resource: {} });
    const [tokenSelection, setTokenSelection] = useState({ accessToken: false, userInfo: false, idToken: false });
    const [cedarlingBootstrapPresent, setCedarlingBootstrapPresent] = React.useState(false);
    const [authzResult, setAuthzResult] = useState("")
    const [authzLogs, setAuthzLogs] = useState("")
    const [logType, setLogType] = useState('Decision');
    const [cedarlingConfig, setCedarlingConfig] = React.useState([]);
    const [loginDetails, setLoginDetails] = React.useState({access_token: "", id_token: "", userDetails: ""});

    React.useEffect(() => {
        if (!Utils.isEmpty(data?.cedarlingConfig) && data?.cedarlingConfig.length !== 0) {
            setCedarlingConfig(data?.cedarlingConfig);
            setCedarlingBootstrapPresent((!Utils.isEmpty(data?.cedarlingConfig) && data?.cedarlingConfig.length !== 0));
        }
        setLoginDetails(data?.loginDetails);

        chrome.storage.local.get(["authzRequest"], (authzRequest) => {
            if (!Utils.isEmpty(authzRequest) && Object.keys(authzRequest).length !== 0) {
                setFormFields({
                    action: authzRequest.authzRequest.action,
                    context: authzRequest.authzRequest.context,
                    resource: authzRequest.authzRequest.resource
                });
            }
        });
    }, [data])

    const triggerCedarlingAuthzRequest = async () => {
        setAuthzResult("");
        setAuthzLogs("");
        let reqObj = await createCedarlingAuthzRequestObj();
            let instance: Cedarling;
            try {
                if (Object.keys(cedarlingConfig).length !== 0) {
                    await initWasm();
                    instance = await init(!Utils.isEmpty(cedarlingConfig) ? cedarlingConfig[0] : undefined);
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
    }

    const createCedarlingAuthzRequestObj = async () => {
        const reqObj = {
            tokens: {
                ...(tokenSelection.accessToken && { access_token: loginDetails?.access_token }),
                ...(tokenSelection.idToken && { id_token: loginDetails?.id_token }),
                ...(tokenSelection.userInfo && { userinfo_token: loginDetails?.userDetails }),
            },
            action: formFields.action,
            context: formFields.context,
            resource: formFields.resource,
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

    const resetInputs = () => {
        setFormFields({
            action: "",
            context: {},
            resource: {}
        });
        setTokenSelection({ accessToken: false, userInfo: false, idToken: false });
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
                            <FormControlLabel control={<Checkbox color="success" checked={tokenSelection.accessToken} onChange={() => setTokenSelection((prev) => ({ ...prev, accessToken: !prev.accessToken }))} />} label="Access Token" />
                            <FormControlLabel control={<Checkbox color="success" checked={tokenSelection.userInfo} onChange={() => setTokenSelection((prev) => ({ ...prev, userInfo: !prev.userInfo }))} />} label="Userinfo Token" />
                            <FormControlLabel control={<Checkbox color="success" checked={tokenSelection.idToken} onChange={() => setTokenSelection((prev) => ({ ...prev, idToken: !prev.idToken }))} />} label="Id Token" />

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
                                value={formFields.action}
                                onChange={(e) => {
                                    const { name, value } = e.target;
                                    setFormFields((prev) => ({
                                        ...prev,
                                        [name]: value
                                    }));
                                }}
                            />
                            <InputLabel id="resource-value-label">Resource</InputLabel>
                            <JsonEditor
                                data={formFields.resource}
                                setData={(e) => {
                                    setFormFields((prev) => ({
                                        ...prev,
                                        ["resource"]: e
                                    }));
                                }}
                                rootName="resource" />
                            <InputLabel id="context-value-label">Context</InputLabel>
                            <JsonEditor
                                data={formFields.context}
                                setData={(e) => {
                                    setFormFields((prev) => ({
                                        ...prev,
                                        ["context"]: e
                                    }));
                                }}
                                rootName="context" />

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
                            <Stack direction="row" spacing={2}>
                                <Button variant="outlined" color="success" onClick={triggerCedarlingAuthzRequest}>Cedarling Authz Request</Button>
                                <Button variant="outlined" color="success" onClick={() => resetInputs()}>Reset</Button>
                            </Stack>
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
                                {Utils.isJSON(authzResult) ?
                                    <>
                                    <Box sx={{ p: 2 }}>
                                    <Typography component="span">Decision</Typography>
                                    { JSON.parse(authzResult).decision ?
                                        <Typography variant="h5" sx={{ color: 'green' }}>True</Typography> :
                                        <Typography variant="h5" sx={{ color: 'red' }}>False</Typography>
                                    }
                                    </Box>
                                    <Box>
                                        <JsonEditor data={JSON.parse(authzResult)} rootName="result" viewOnly={true} />
                                    </Box>
                                    </> :
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
                                    />}
                                <div>
                                    <Button variant="text" color="success" onClick={() => setAuthzResult('')}>Reset</Button>
                                </div>
                            </AccordionDetails>
                        </Accordion> :
                        ''}
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