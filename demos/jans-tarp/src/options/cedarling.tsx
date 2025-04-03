import React, { useRef } from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell, { tableCellClasses } from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Edit from '@mui/icons-material/Edit';
import { pink, green } from '@mui/material/colors';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import { styled } from '@mui/material/styles';
import Paper from '@mui/material/Paper';
import AddIcon from '@mui/icons-material/Add';
import Container from '@mui/material/Container';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import AddCedarlingConfig from './addCedarlingConfig'
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import HelpDrawer from './helpDrawer'
import Alert from '@mui/material/Alert';
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
const StyledTableCell = styled(TableCell)(({ theme }) => ({
    [`&.${tableCellClasses.head}`]: {
        backgroundColor: theme.palette.common.black,
        color: theme.palette.common.white,
    },
    [`&.${tableCellClasses.body}`]: {
        fontSize: 14,
    },
}));

function Row(props: { row: any, notifyOnDataChange }) {
    const { row, notifyOnDataChange } = props;
    const [open, setOpen] = React.useState(false);

    const handleDialog = (isOpen) => {
        setOpen(isOpen);
        notifyOnDataChange();
    };

    async function resetBootstrap() {
        chrome.storage.local.get(["cedarlingConfig"], (result) => {
            let cedarlingConfigArr = []
            chrome.storage.local.set({ cedarlingConfig: cedarlingConfigArr });
        });
        notifyOnDataChange();
    }

    return (
        <React.Fragment>
            <AddCedarlingConfig isOpen={open} handleDialog={handleDialog} newData={row} />
            <TableRow sx={{ '& > *': { borderBottom: 'unset' } }}>
                <TableCell>
                    <Tooltip title="Delete">
                        <IconButton aria-label="Delete">
                            <DeleteForeverOutlinedIcon sx={{ color: pink[500] }} onClick={resetBootstrap} />
                        </IconButton>
                    </Tooltip>
                </TableCell>
                <TableCell component="th" scope="row" align="left">
                    <JsonEditor
                        data={row}
                        restrictTypeSelection={true}
                        collapse={true}
                        restrictEdit={true}
                        restrictDelete={true}
                        restrictAdd={true}
                        rootName="bootstrapConfig" />
                </TableCell>

                <TableCell component="th" scope="row">
                    <Grid item xs={8}>
                        <Tooltip title="Edit">
                            <IconButton aria-label="Edit">
                                <Edit
                                    sx={{ color: green[500] }}
                                    onClick={() => {
                                        setOpen(true);
                                        notifyOnDataChange();
                                    }} />
                            </IconButton>
                        </Tooltip>
                    </Grid>
                </TableCell>
            </TableRow>
        </React.Fragment>
    );
}

export default function CedarlingMgmt({ data, notifyOnDataChange, isOidcClientRegistered }) {
    const [modelOpen, setModelOpen] = React.useState(false);
    const [drawerOpen, setDrawerOpen] = React.useState(false);
    const [oidcClientRegistered, setOidcClientRegistered] = React.useState(false);
    const [context, setContext] = React.useState({});
    const [resource, setResource] = React.useState({});
    const [principals, setPrincipals] = React.useState([]);
    const [action, setAction] = React.useState("");
    const [logType, setLogType] = React.useState('Decision');
    const [authzResult, setAuthzResult] = React.useState("");
    const [authzLogs, setAuthzLogs] = React.useState("");
    const [screenType, setScreenType] = React.useState("config");

    React.useEffect(() => {
        setOidcClientRegistered(isOidcClientRegistered)
    }, [isOidcClientRegistered]);

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

    const handleDialog = (isOpen) => {
        setModelOpen(isOpen);
        notifyOnDataChange();
    };

    const handleDrawer = (isOpen) => {
        setDrawerOpen(isOpen);
    };

    const handleLogTypeChange = (
        event: React.MouseEvent<HTMLElement>,
        newLogType: string,
    ) => {
        setLogType(newLogType);
    };

    const handleScreenChange = (
        event: React.MouseEvent<HTMLElement>,
        newScreenType: string,
    ) => {
        setScreenType(newScreenType);
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
                <>
                    <AddCedarlingConfig isOpen={modelOpen} handleDialog={handleDialog} newData={{}} />
                    <HelpDrawer isOpen={drawerOpen} handleDrawer={handleDrawer} />
                    <Stack direction="column" spacing={2} sx={{ mb: 1 }}>
                        <Stack direction="row" spacing={2} sx={{ mb: 1 }} style={{ display: 'flex', justifyContent: 'flex-end' }}>
                            {(data === undefined || data?.length == 0) ?
                                <Button color="success" variant="outlined" startIcon={<AddIcon />} onClick={() => setModelOpen(true)} style={{ maxWidth: '200px' }}>
                                    Add Configurations
                                </Button> : ''}
                        </Stack>
                        {(data === undefined || data?.length == 0) ? '' :
                            <Box maxWidth="md">
                                <ToggleButtonGroup
                                    color="primary"
                                    value={screenType}
                                    exclusive
                                    onChange={handleScreenChange}
                                    aria-label="Platform"
                                >
                                    <ToggleButton value="config">Bootstrap Configuration</ToggleButton>
                                    <ToggleButton value="authz">Cedarling Authz Form</ToggleButton>
                                </ToggleButtonGroup>
                            </Box>
                        }
                        {screenType === 'config' ?
                            <TableContainer component={Paper}>
                                <Table aria-label="collapsible table">
                                    <TableHead>
                                        <TableRow>
                                            <StyledTableCell />
                                            <StyledTableCell>Bootstrap Configuration</StyledTableCell>
                                            <StyledTableCell align="right">Action</StyledTableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {(data === undefined || data?.length == 0) ?
                                            <TableCell colSpan={6}><Alert severity="warning">No Records to show.</Alert></TableCell> :
                                            data.map((row, index) => (<Row key={index} row={row} notifyOnDataChange={notifyOnDataChange} />))
                                        }
                                    </TableBody>
                                </Table>
                            </TableContainer>
                            : <>
                                {(data === undefined || data?.length == 0) ? '' :
                                    <Box sx={{ width: "60%" }}>
                                        <Accordion defaultExpanded>
                                            <AccordionSummary
                                                expandIcon={<ExpandMoreIcon />}
                                                aria-controls="panel1-content"
                                                id="panel1-header"
                                            >
                                                <Typography component="span"><strong>Cedarling Authz Request Form</strong></Typography>
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
                                    </Box>}
                            </>}
                            <Box style={{textAlign: 'right', width: "60%"}}>© Gluu Inc. All Rights Reserved.</Box>
                    </Stack>
                </>
        </Container>
    );
}