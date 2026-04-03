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
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import initWasm, { init, Cedarling, MultiIssuerAuthorizeResult } from '@janssenproject/cedarling_wasm';
import Utils from '../../../options/Utils';
import Stack from '@mui/material/Stack';
import Tooltip from "@mui/material/Tooltip";
import HelpIcon from '@mui/icons-material/Help';
import { pink } from '@mui/material/colors';
import Paper from '@mui/material/Paper';
import Divider from '@mui/material/Divider';
import Alert from '@mui/material/Alert';
import Chip from '@mui/material/Chip';
import { labelWithTooltip } from '../../../shared/components/Common';
import IconButton from '@mui/material/IconButton';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CircularProgress from '@mui/material/CircularProgress';

interface CedarlingMultiIssuerAuthzProps {
    data: any; // or define the actual shape of cedarlingConfig data
}

type TokenObj = {
    mapping: string;
    payload: string;
};

type TokenSelection = {
    accessToken: boolean;
    userInfo: boolean;
    idToken: boolean;
};

type FormFields = {
    tokens: TokenObj[];
    action: string;
    context: Record<string, unknown>;
    resource: Record<string, unknown>;
};

export default function MultiIssuerAuthzForm({ data }: CedarlingMultiIssuerAuthzProps) {
    const [logType, setLogType] = React.useState('Decision');
    const [authzResult, setAuthzResult] = React.useState("");
    const [authzLogs, setAuthzLogs] = React.useState("");
    const [isSubmitting, setIsSubmitting] = React.useState(false);
    const [uiMessage, setUiMessage] = React.useState<string>("");
    const [formFields, setFormFields] = React.useState<FormFields>({ tokens: [], action: "", context: {}, resource: {} });
    const [tokenSelection, setTokenSelection] = React.useState<TokenSelection>({ accessToken: false, userInfo: false, idToken: false });
    const [loginDetails, setLoginDetails] = React.useState<{
            access_token?: string;
            id_token?: string;
            userDetails?: string;
        } | null>(null);

    React.useEffect(() => {
        setLoginDetails(data?.loginDetails ?? null);
    }, [data?.loginDetails]);

    React.useEffect(() => {
        chrome.storage.local.get(["multiIssueAuthz"], (result) => {
            if (result?.multiIssueAuthz) {
                setFormFields({
                    tokens: result.multiIssueAuthz.tokens ?? [],
                    action: result.multiIssueAuthz.action ?? "",
                    context: result.multiIssueAuthz.context ?? {},
                    resource: result.multiIssueAuthz.resource ?? {},
                });
            }
        });
    }, []);

    const handleLogTypeChange = (
        event: React.MouseEvent<HTMLElement>,
        newLogType: string | null,
    ) => {
        if (newLogType) {
            setLogType(newLogType);
        }
    };

    const triggerCedarlingAuthzRequest = async () => {
        setUiMessage("");
        setAuthzResult("");
        setAuthzLogs("");
        setIsSubmitting(true);
        let reqObj = await createCedarlingAuthzRequestObj();
        chrome.storage.local.get(["cedarlingConfig"], async (cedarlingConfig) => {
            let instance: Cedarling | null = null;
            try {
                const config = cedarlingConfig?.cedarlingConfig?.[0];
                if (!config) {
                    setAuthzResult("Error: No Cedarling configuration found. Please add a configuration first.");
                    setUiMessage("No Cedarling configuration found. Add a bootstrap configuration in the Cedarling tab.");
                    setIsSubmitting(false);
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
                setUiMessage("Authorization failed. Check the error output (and logs if available).");
            } finally {
                setIsSubmitting(false);
            }

        });

    };

    const addTokens = async () => {
        const tokenAliasMap = {
            accessToken: "AccessToken_namespace::Access_token_entity",
            userInfo: "UserInfoToken_namespace::Userinfo_entity",
            idToken: "IDToken_namespace::Id_token_entity"
        };

        setFormFields((prev) => {
            let updatedTokens = [...prev.tokens];

            const tokenPayloadMap = {
                accessToken: loginDetails?.access_token,
                idToken: loginDetails?.id_token,
                userInfo: loginDetails?.userDetails
            };

            (Object.keys(tokenAliasMap) as (keyof typeof tokenAliasMap)[]).forEach((key) => {
                const mapping = tokenAliasMap[key];

                if (tokenSelection[key]) {
                    const payload = tokenPayloadMap[key];
                    if (!payload) {
                        return;
                    }

                    // TokenObj.payload is a string (JWT or JSON). If userDetails is an object, stringify it.
                    const nextToken: TokenObj = {
                        mapping,
                        payload: typeof payload === "string" ? payload : JSON.stringify(payload),
                    };
                    const index = updatedTokens.findIndex((t) => t.mapping === mapping);

                    if (index >= 0) {
                        updatedTokens[index] = nextToken;
                    } else {
                        updatedTokens.push(nextToken);
                    }
                } else {
                    updatedTokens = updatedTokens.filter((t) => t.mapping !== mapping);
                }
            });

            return {
                ...prev,
                tokens: updatedTokens
            };
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
        setUiMessage("");
        setFormFields({
            tokens: [],
            action: "",
            context: {},
            resource: {}
        });
        chrome.storage.local.remove("multiIssueAuthz");
    };

    const canSubmit = React.useMemo(() => {
        const hasAction = !!formFields.action && formFields.action.trim().length > 0;
        const hasTokens = Array.isArray(formFields.tokens) && formFields.tokens.length > 0;
        const hasResource = !!formFields.resource && Object.keys(formFields.resource).length > 0;
        return hasAction && hasTokens && hasResource && !isSubmitting;
    }, [formFields.action, formFields.tokens, formFields.resource, isSubmitting]);

    const copyText = async (text: string) => {
        try {
            await navigator.clipboard.writeText(text);
            setUiMessage("Copied to clipboard.");
        } catch {
            setUiMessage("Copy failed. Your browser blocked clipboard access.");
        }
    };

    return (
        <Container maxWidth="lg">
            {(data === undefined || data?.length == 0) ? '' :
                <Paper elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                    <Box sx={{ p: { xs: 2, sm: 3 } }}>
                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ xs: 'flex-start', sm: 'center' }} justifyContent="space-between">
                            <Box>
                                <Typography variant="h6" sx={{ fontWeight: 650 }}>
                                    Cedarling Multi-Issuer Authorization
                                </Typography>
                                <Typography variant="body2" color="text.secondary">
                                    Build an authz request from tokens, action, resource, and context — then run authorization.
                                </Typography>
                            </Box>
                            <Stack direction="row" spacing={1} alignItems="center">
                                <Chip size="small" label={isSubmitting ? "Running..." : "Ready"} color={isSubmitting ? "warning" : "success"} variant="outlined" />
                                {isSubmitting && <CircularProgress size={18} />}
                            </Stack>
                        </Stack>

                        <Divider sx={{ my: 2 }} />

                        {!!uiMessage && (
                            <Alert severity="info" sx={{ mb: 2 }}>
                                {uiMessage}
                            </Alert>
                        )}

                        <Accordion defaultExpanded disableGutters elevation={0} sx={{ '&:before': { display: 'none' } }}>
                            <AccordionSummary
                                expandIcon={<ExpandMoreIcon />}
                                aria-controls="panel1-content"
                                id="panel1-header"
                            >
                                <Typography component="span" sx={{ fontWeight: 600 }}>Request builder</Typography>
                            </AccordionSummary>
                            <AccordionDetails>
                                <Stack spacing={2}>
                                {(loginDetails?.access_token || loginDetails?.id_token || loginDetails?.userDetails) &&
                                    <>
                                        <Box>
                                            <Typography variant="subtitle2" sx={{ mb: 1 }}>
                                                Add tokens from Auth Flow
                                            </Typography>
                                            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ xs: 'flex-start', sm: 'center' }}>
                                                <FormControlLabel control={<Checkbox color="success" checked={tokenSelection.accessToken} onChange={() => setTokenSelection((prev) => ({ ...prev, accessToken: !prev.accessToken }))} />} label="Access Token" />
                                                <FormControlLabel control={<Checkbox color="success" checked={tokenSelection.userInfo} onChange={() => setTokenSelection((prev) => ({ ...prev, userInfo: !prev.userInfo }))} />} label="Userinfo Token" />
                                                <FormControlLabel control={<Checkbox color="success" checked={tokenSelection.idToken} onChange={() => setTokenSelection((prev) => ({ ...prev, idToken: !prev.idToken }))} />} label="ID Token" />
                                            </Stack>
                                            <Button
                                                variant="contained"
                                                color="success"
                                                onClick={addTokens}
                                                sx={{ mt: 1, textTransform: 'none', borderRadius: 999 }}
                                            >
                                                Add selected tokens to mapping
                                            </Button>
                                        </Box>
                                    </>
                                }
                                {labelWithTooltip(
                                    <Typography
                                        variant="caption"
                                        sx={{ display: "block", mb: 0.5, color: "text.secondary" }}
                                    >
                                        Example JSON format
                                    </Typography>,
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
                                )}
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
                                    setData={(e: FormFields["resource"]) => {
                                        setFormFields((prev) => ({
                                            ...prev,
                                            resource: e
                                        }));
                                    }} />
                                <InputLabel id="context-value-label">Context</InputLabel>
                                <JsonEditor
                                    data={formFields.context}
                                    setData={(e: FormFields["context"]) => {
                                        setFormFields((prev) => ({
                                            ...prev,
                                            context: e
                                        }));
                                    }}
                                    rootName="context" />

                                <Box>
                                    <Typography variant="subtitle2" sx={{ mb: 1 }}>
                                        Log tag
                                    </Typography>
                                    <ToggleButtonGroup
                                        color="primary"
                                        value={logType}
                                        exclusive
                                        onChange={handleLogTypeChange}
                                        aria-label="Log type"
                                    >
                                        <ToggleButton value="Decision" sx={{ textTransform: 'none' }}>Decision</ToggleButton>
                                        <ToggleButton value="System" sx={{ textTransform: 'none' }}>System</ToggleButton>
                                        <ToggleButton value="Metric" sx={{ textTransform: 'none' }}>Metric</ToggleButton>
                                    </ToggleButtonGroup>
                                </Box>

                                <Divider />

                                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} justifyContent="space-between" alignItems={{ xs: 'stretch', sm: 'center' }}>
                                    <Typography variant="caption" color="text.secondary">
                                        Required: tokens + action + resource
                                    </Typography>
                                    <Stack direction="row" spacing={1} justifyContent={{ xs: 'flex-start', sm: 'flex-end' }}>
                                        <Button
                                            variant="outlined"
                                            color="inherit"
                                            onClick={resetInputs}
                                            disabled={isSubmitting}
                                            sx={{ textTransform: 'none', borderRadius: 999 }}
                                        >
                                            Reset
                                        </Button>
                                        <Button
                                            variant="contained"
                                            color="success"
                                            onClick={triggerCedarlingAuthzRequest}
                                            disabled={!canSubmit}
                                            sx={{ textTransform: 'none', borderRadius: 999 }}
                                        >
                                            Run authorization
                                        </Button>
                                    </Stack>
                                </Stack>
                                {!canSubmit && !isSubmitting && (
                                    <Typography variant="caption" color="text.secondary">
                                        Add at least 1 token mapping, an action, and a non-empty resource.
                                    </Typography>
                                )}
                                </Stack>
                            </AccordionDetails>
                        </Accordion>

                        {(!!authzResult || !!authzLogs) && <Divider sx={{ my: 2 }} />}

                        {!!authzResult && (
                            <Accordion defaultExpanded disableGutters elevation={0} sx={{ '&:before': { display: 'none' } }}>
                                <AccordionSummary
                                    expandIcon={<ExpandMoreIcon />}
                                    aria-controls="panel-result-content"
                                    id="panel-result-header"
                                >
                                    <Stack direction="row" spacing={1} alignItems="center" sx={{ width: '100%' }} justifyContent="space-between">
                                        <Typography component="span" sx={{ fontWeight: 600 }}>Result</Typography>
                                        <Tooltip title="Copy result JSON/text">
                                            <IconButton
                                                size="small"
                                                onClick={() => copyText(authzResult)}
                                                aria-label="Copy result"
                                            >
                                                <ContentCopyIcon fontSize="small" />
                                            </IconButton>
                                        </Tooltip>
                                    </Stack>
                                </AccordionSummary>
                                <AccordionDetails>
                                    {Utils.isJSON(authzResult) ? (
                                        <>
                                            <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', mb: 1 }}>
                                                <Typography variant="subtitle2">Decision</Typography>
                                                {JSON.parse(authzResult).decision ? (
                                                    <Chip label="True" color="success" size="small" />
                                                ) : (
                                                    <Chip label="False" color="error" size="small" />
                                                )}
                                            </Box>
                                            <JsonEditor data={JSON.parse(authzResult)} rootName="result" viewOnly={true} />
                                        </>
                                    ) : (
                                        <TextField
                                            autoFocus
                                            margin="dense"
                                            id="authzResult"
                                            name="authzResult"
                                            label="Authz Result"
                                            rows={10}
                                            multiline
                                            type="text"
                                            fullWidth
                                            variant="outlined"
                                            value={authzResult}
                                        />
                                    )}
                                    <Button
                                        variant="text"
                                        color="inherit"
                                        onClick={() => setAuthzResult('')}
                                        sx={{ mt: 1, textTransform: 'none' }}
                                    >
                                        Clear result
                                    </Button>
                                </AccordionDetails>
                            </Accordion>
                        )}

                        {!!authzLogs && (
                            <Accordion disableGutters elevation={0} sx={{ '&:before': { display: 'none' } }}>
                                <AccordionSummary
                                    expandIcon={<ExpandMoreIcon />}
                                    aria-controls="panel-logs-content"
                                    id="panel-logs-header"
                                >
                                    <Stack direction="row" spacing={1} alignItems="center" sx={{ width: '100%' }} justifyContent="space-between">
                                        <Typography component="span" sx={{ fontWeight: 600 }}>Logs</Typography>
                                        <Stack direction="row" spacing={0.5} alignItems="center">
                                            <Tooltip title="Copy logs">
                                                <IconButton
                                                    size="small"
                                                    onClick={() => copyText(authzLogs)}
                                                    aria-label="Copy logs"
                                                >
                                                    <ContentCopyIcon fontSize="small" />
                                                </IconButton>
                                            </Tooltip>
                                        </Stack>
                                    </Stack>
                                </AccordionSummary>
                                <AccordionDetails>
                                    <TextField
                                        autoFocus
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
                                        InputProps={{
                                            sx: { fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace' }
                                        }}
                                    />
                                    <Button
                                        variant="text"
                                        color="inherit"
                                        onClick={() => setAuthzLogs('')}
                                        sx={{ mt: 1, textTransform: 'none' }}
                                    >
                                        Clear logs
                                    </Button>
                                </AccordionDetails>
                            </Accordion>
                        )}
                    </Box>
                </Paper>}
        </Container >
    );
}
