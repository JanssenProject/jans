import * as React from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import OfflineBoltIcon from '@mui/icons-material/OfflineBolt';
import { pink, green } from '@mui/material/colors';
import Paper from '@mui/material/Paper';
import AddIcon from '@mui/icons-material/Add';
import Container from '@mui/material/Container';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import RegisterClient from './RegisterClient';
import AuthFlowInputs from './AuthFlowInputs';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import HighlightOffIcon from '@mui/icons-material/HighlightOff';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import LiveHelpIcon from '@mui/icons-material/LiveHelp';
import HelpDrawer from '../../../options/helpDrawer';
import Alert from '@mui/material/Alert';
import StyledTableCell from '../../../shared/components/StyledTableCell';

function createData(
    opHost: string,
    clientId: string,
    clientSecret: string,
    showClientExpiry: boolean,
    expireAt: number,
    scope: string,
    redirectUris: string[],
    authorizationEndpoint: string,
    tokenEndpoint: string,
    userinfoEndpoint: string,
    endSessionEndpoint: string,
    responseType: string,
    postLogoutRedirectUris: string[],
    acrValuesSupported: string[],

) {
    return {
        opHost,
        clientId,
        clientSecret,
        showClientExpiry,
        expireAt,
        scope,
        redirectUris,
        authorizationEndpoint,
        tokenEndpoint,
        userinfoEndpoint,
        endSessionEndpoint,
        responseType,
        postLogoutRedirectUris,
        acrValuesSupported,
    };
}

/**
 * Render a table row representing an OIDC client with controls to delete the client and trigger an authentication flow.
 *
 * @param props.row - OIDC client data (as returned by `createData`) used to populate the row's cells.
 * @param props.notifyOnDataChange - Callback invoked after client data changes (for example, after deletion or when the auth flow is triggered).
 * @returns A JSX element containing the table row and its action controls.
 */
function Row(props: { row: ReturnType<typeof createData>, notifyOnDataChange }) {
    const { row, notifyOnDataChange } = props;
    const [open, setOpen] = React.useState(false);
    const lifetime = Math.floor((row.expireAt - Date.now()) / 1000);

    const handleDialog = (isOpen) => {
        setOpen(isOpen);
    };

    async function resetClient() {
        chrome.storage.local.get(["oidcClients"], (result) => {
            let clientArr = []
            if (!!result.oidcClients) {
                clientArr = result.oidcClients;
                chrome.storage.local.set({ oidcClients: clientArr.filter(obj => obj.clientId !== row.clientId) });
            }
        });
        notifyOnDataChange();
    }

    return (
        <React.Fragment>
            <AuthFlowInputs isOpen={open} handleDialog={handleDialog} client={row} notifyOnDataChange={notifyOnDataChange} />
            <TableRow
                hover
                sx={{
                    '& > *': { borderBottom: 'unset' },
                }}
            >
                <TableCell>
                    <Tooltip title="Delete Client from janssen-tarp">
                        <IconButton aria-label="Delete">
                            <DeleteForeverOutlinedIcon sx={{ color: pink[500] }} onClick={resetClient} />
                        </IconButton>
                    </Tooltip>
                </TableCell>
                <TableCell component="th" scope="row" align="left">
                    {row.opHost}
                </TableCell>
                <TableCell align="left" component="th" scope="row">{row.clientId}</TableCell>
                <TableCell align="left" component="th" scope="row">
                    <Tooltip title={row.clientSecret}>
                        <span>{row.clientSecret}</span>
                    </Tooltip>
                </TableCell>
                <TableCell align="left" component="th" scope="row">
                    {row.showClientExpiry ? (!(lifetime <= 0) ? <CheckCircleOutlineIcon sx={{ color: green[500] }} /> : <HighlightOffIcon sx={{ color: pink[500] }} />) : <CheckCircleOutlineIcon sx={{ color: green[500] }} />}
                </TableCell>
                <TableCell component="th" scope="row">
                    <Tooltip title="Trigger authentication flow">
                    <IconButton
                            aria-label="Trigger Auth Flow"
                            onClick={() => {
                                setOpen(true);
                                notifyOnDataChange();
                            }}
                        >
                            <OfflineBoltIcon sx={{ color: green[500] }} />
                        </IconButton>
                    </Tooltip>
                </TableCell>
            </TableRow>
        </React.Fragment>
    );
}

export default function OIDCClients({ data, notifyOnDataChange }) {
    const [modelOpen, setModelOpen] = React.useState(false);
    const [drawerOpen, setDrawerOpen] = React.useState(false);
    const handleDialog = (isOpen) => {
        setModelOpen(isOpen);
        notifyOnDataChange();
    };

    const handleDrawer = (isOpen) => {
        setDrawerOpen(isOpen);
    };

    return (
        <Container maxWidth="lg">
            <RegisterClient isOpen={modelOpen} handleDialog={handleDialog} />
            <HelpDrawer isOpen={drawerOpen} handleDrawer={handleDrawer} />
            <Stack direction="column" spacing={2} sx={{ mb: 1 }}>
                <Stack
                    direction="row"
                    justifyContent="space-between"
                    alignItems="center"
                    sx={{ mb: 1 }}
                >
                    <div>
                        <Typography variant="h6" sx={{ mb: 0.5 }}>
                            OIDC Clients
                        </Typography>
                    </div>
                    <Button
                        color="success"
                        variant="outlined"
                        startIcon={<AddIcon />}
                        onClick={() => setModelOpen(true)}
                        sx={{ borderRadius: 999, textTransform: 'none', maxWidth: 200 }}
                    >
                        Add client
                    </Button>
                </Stack>
                <TableContainer component={Paper} sx={{ borderRadius: 2, boxShadow: 1 }}>
                    <Table aria-label="collapsible table">
                        <TableHead>
                            <TableRow>
                                <StyledTableCell />
                                <StyledTableCell>Issuer</StyledTableCell>
                                <StyledTableCell>Client ID</StyledTableCell>
                                <StyledTableCell>Client Secret</StyledTableCell>
                                <StyledTableCell>Active</StyledTableCell>
                                <StyledTableCell align="right">Action</StyledTableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {(data === undefined || data?.length == 0)
                                ? (
                                    <TableRow>
                                        <TableCell colSpan={6}>
                                            <Alert severity="info">
                                                No clients configured yet. Use &quot;Add client&quot; to get started.
                                            </Alert>
                                        </TableCell>
                                    </TableRow>
                                ) : (
                                    data.map((row) => (
                                        <Row
                                            key={`${row?.opHost}-${row?.clientId}`}
                                            row={row}
                                            notifyOnDataChange={notifyOnDataChange}
                                        />
                                    ))
                                )}
                        </TableBody>
                    </Table>
                </TableContainer>
                <Button
                    color="secondary"
                    variant="outlined"
                    startIcon={<LiveHelpIcon />}
                    onClick={() => handleDrawer(true)}
                    sx={{ maxWidth: 180, alignSelf: 'flex-start', textTransform: 'none' }}
                >
                    Need help?
                </Button>
            </Stack>
        </Container>
    );
}
