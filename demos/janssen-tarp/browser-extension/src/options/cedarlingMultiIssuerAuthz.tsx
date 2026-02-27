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
import initWasm, { init, Cedarling, MultiIssuerAuthorizeResult } from '@janssenproject/cedarling_wasm';
import Utils from './Utils';
import Stack from '@mui/material/Stack';
import Tooltip from "@mui/material/Tooltip";
import HelpIcon from '@mui/icons-material/Help';
import { pink } from '@mui/material/colors';

interface CedarlingMultiIssuerAuthzProps {
    data: any; // or define the actual shape of cedarlingConfig data
}

export default function CedarlingMultiIssuerAuthz({ data }: CedarlingMultiIssuerAuthzProps) {
    const [logType, setLogType] = React.useState('Decision');
    const [authzResult, setAuthzResult] = React.useState("");
    const [authzLogs, setAuthzLogs] = React.useState("");
    const [formFields, setFormFields] = React.useState({ tokens: [], action: "", context: {}, resource: {} });

    React.useEffect(() => {
        chrome.storage.local.get(["multiIssueAuthz"], (result) => {
            if (result?.multiIssueAuthz) {
                setFormFields({
                    tokens: result.multiIssueAuthz.tokens,
                    action: result.multiIssueAuthz.action,
                    context: result.multiIssueAuthz.context,
                    resource: result.multiIssueAuthz.resource
                });
            }
        });
    }, [data]);

    const handleLogTypeChange = (
        event: React.MouseEvent<HTMLElement>,
        newLogType: string | null,
    ) => {
        if (newLogType) {
            setLogType(newLogType);
        }
    };

    const triggerCedarlingAuthzRequest = async () => {
        setAuthzResult("");
        setAuthzLogs("");
        let reqObj = await createCedarlingAuthzRequestObj();
        chrome.storage.local.get(["cedarlingConfig"], async (cedarlingConfig) => {
            let instance: Cedarling | null = null;
            try {
                const config = cedarlingConfig?.cedarlingConfig?.[0];
                if (!config) {
                    setAuthzResult("Error: No Cedarling configuration found. Please add a configuration first.");
                    return;
                }
                await initWasm();
                instance = await init(config);
                const result: MultiIssuerAuthorizeResult = await instance.authorize_multi_issuer(reqObj);
                setAuthzResult(result.json_string());

                try {
                    const logs = await instance.get_logs_by_request_id_and_tag(result.request_id, logType);
                    if (logs.length !== 0) {
                        const prettyLogs = logs.map((log) => JSON.stringify(log, null, 2));
                        setAuthzLogs(prettyLogs.join("\n"));
                    }
                } catch (logErr) {
                    setAuthzLogs(`Failed to fetch logs: ${String(logErr)}`);
                }
            } catch (err) {
                setAuthzResult(err.toString());
                console.log("err:", err);
                if (instance) {
                    const logs = await instance.pop_logs();
                    if (logs.length !== 0) {
                        const prettyLogs = logs.map((log) => JSON.stringify(log, null, 2));
                        setAuthzLogs(prettyLogs.join("\n"));
                    }
                }
            }

        });

    };

    const createCedarlingAuthzRequestObj = async () => {
        const reqObj = {
            tokens: formFields.tokens,
            action: formFields.action,
            context: formFields.context,
            resource: formFields.resource,
        };
        chrome.storage.local.set({ multiIssueAuthz: reqObj });
        return reqObj;
    };

    const resetInputs = () => {
        setFormFields({
            tokens: [],
            action: "",
            context: {},
            resource: {}
        });
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
                            <Typography component="span"><strong>Cedarling Multi-Issuer Authz Request Form</strong></Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                                <Tooltip
                                    placement="bottom-start"
                                    title={
                                        <Box sx={{ maxWidth: 480 }}>
                                            <Typography
                                                variant="caption"
                                                sx={{ display: "block", mb: 0.5, color: "text.secondary" }}
                                            >
                                                Example JSON format
                                            </Typography>

                                            <Box
                                                component="pre"
                                                sx={{
                                                    margin: 0,
                                                    padding: 1.25,
                                                    borderRadius: 1,
                                                    backgroundColor: "grey.900",
                                                    color: "grey.100",
                                                    fontFamily: "Monospace, monospace",
                                                    fontSize: "0.75rem",
                                                    whiteSpace: "pre-wrap",
                                                    wordBreak: "break-all",
                                                }}
                                            >
                                                {`[
                                                    {
                                                        "mapping": "Namespace_Name::Token_Entity",
                                                        "payload": "<JWT_TOKEN_STRING>"
                                                    },
                                                    {
                                                        "mapping": "Acme::Access_token",
                                                        "payload": "<JWT_TOKEN_STRING>"
                                                    }
                                                ]`}
                                            </Box>
                                        </Box>
                                    }
                                >
                                    <InputLabel id="issuer-token-mapping-label">Issuer-to-Token Mapping <HelpIcon sx={{ color: pink[500], fontSize: 15 }} /></InputLabel>
                                </Tooltip>
                                <JsonEditor
                                    data={formFields.tokens}
                                    setData={(e: any) => {
                                        setFormFields((prev) => ({
                                            ...prev,
                                            ["tokens"]: e
                                        }));
                                    }}
                                    rootName="tokens"
                                />

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
                                    rootName="resource"
                                    setData={(e) => {
                                        setFormFields((prev) => ({
                                            ...prev,
                                            ["resource"]: e
                                        }));
                                    }} />
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

                                <InputLabel id="log-type-label">Log Type</InputLabel>
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
                                    <>
                                        <Box sx={{ p: 2 }}>
                                            <Typography component="span">Decision</Typography>
                                            {JSON.parse(authzResult).decision ?
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
                </div>}
        </Container >
    );
}