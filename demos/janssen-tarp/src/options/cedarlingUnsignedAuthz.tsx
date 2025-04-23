import React from 'react';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Button from '@mui/material/Button';
import { JsonEditor } from 'json-edit-react'
import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import Typography from '@mui/material/Typography';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import TextField from '@mui/material/TextField';
import InputLabel from '@mui/material/InputLabel';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import initWasm, { init, Cedarling, AuthorizeResult } from '@janssenproject/cedarling_wasm';
import Utils from './Utils';

export default function CedarlingUnsignedAuthz({ data }) {
    const [context, setContext] = React.useState({});
    const [resource, setResource] = React.useState({});
    const [principals, setPrincipals] = React.useState([]);
    const [action, setAction] = React.useState("");
    const [logType, setLogType] = React.useState('Decision');
    const [authzResult, setAuthzResult] = React.useState("");
    const [authzLogs, setAuthzLogs] = React.useState("");

    React.useEffect(() => {
        chrome.storage.local.get(["authzRequest_unsigned"], (result) => {
            if (!Utils.isEmpty(result) && Object.keys(result).length !== 0) {
                setContext(result.authzRequest_unsigned.context);
                setAction(result.authzRequest_unsigned.action);
                setResource(result.authzRequest_unsigned.resource);
                setPrincipals(result.authzRequest_unsigned.principals);
            }
        });
    }, [data]);

    const handleLogTypeChange = (
        event: React.MouseEvent<HTMLElement>,
        newLogType: string,
    ) => {
        setLogType(newLogType);
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
                    let result: AuthorizeResult = await instance.authorize_unsigned(reqObj);
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

    };

    const createCedarlingAuthzRequestObj = async () => {
        const reqObj = {
            principals,
            action,
            context,
            resource,
        };

        chrome.storage.local.set({ authzRequest_unsigned: reqObj });
        return reqObj;
    };

    return (
        <Container maxWidth="lg">
            {(data === undefined || data?.length == 0) ? '' :
                <div className="box">
                    <Accordion defaultExpanded>
                        <AccordionSummary
                            expandIcon={<ExpandMoreIcon />}
                            aria-controls="panel1-content"
                            id="panel1-header"
                        >
                            <Typography component="span"><strong>Cedarling Unsigned Authz Request Form</strong></Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                                <InputLabel id="principal-value-label">Principals</InputLabel>
                                <JsonEditor data={principals} setData={setPrincipals} rootName="principals" />

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
                    </Accordion>
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
                                    <Box>
                                        <JsonEditor data={JSON.parse(authzResult)} rootName="result" viewOnly={true} />
                                    </Box> :
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
                </div>}
        </Container >
    );
}