import * as React from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell, { tableCellClasses } from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import OfflineBoltIcon from '@mui/icons-material/OfflineBolt';
import { pink, green } from '@mui/material/colors';
import Grid from '@mui/material/Grid';
import moment from 'moment';
import { styled } from '@mui/material/styles';
import Paper from '@mui/material/Paper';
import AddIcon from '@mui/icons-material/Add';
import Container from '@mui/material/Container';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import RegisterClient from './registerClient'
import AuthFlowInputs from './authFlowInputs'
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import HighlightOffIcon from '@mui/icons-material/HighlightOff';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import LiveHelpIcon from '@mui/icons-material/LiveHelp';
import HelpDrawer from './helpDrawer'
import Alert from '@mui/material/Alert';
const StyledTableCell = styled(TableCell)(({ theme }) => ({
    [`&.${tableCellClasses.head}`]: {
        backgroundColor: theme.palette.common.black,
        color: theme.palette.common.white,
    },
    [`&.${tableCellClasses.body}`]: {
        fontSize: 14,
    },
}));

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

function Row(props: { row: ReturnType<typeof createData>, notifyOnDataChange }) {
    const { row, notifyOnDataChange } = props;
    const [open, setOpen] = React.useState(false);
    const lifetime = Math.floor((row.expireAt - moment().toDate().getTime()) / 1000);

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
            <TableRow sx={{ '& > *': { borderBottom: 'unset' } }}>
                <TableCell>
                    <Tooltip title="Delete Client from jans-tarp">
                        <IconButton aria-label="Delete">
                            <DeleteForeverOutlinedIcon sx={{ color: pink[500] }} onClick={resetClient} />
                        </IconButton>
                    </Tooltip>
                </TableCell>
                <TableCell component="th" scope="row" align="left">
                    {row.opHost}
                </TableCell>
                <TableCell align="left" component="th" scope="row">{row.clientId}</TableCell>
                <TableCell align="left" component="th" scope="row">{row.clientSecret}</TableCell>
                <TableCell align="left" component="th" scope="row">
                    {row.showClientExpiry ? (!(lifetime <= 0) ? <CheckCircleOutlineIcon sx={{ color: green[500] }} /> : <HighlightOffIcon sx={{ color: pink[500] }} />) : <CheckCircleOutlineIcon sx={{ color: green[500] }} />}
                </TableCell>
                <TableCell component="th" scope="row">
                    <Grid item xs={8}>
                        <Tooltip title="Trigger authentication flow">
                            <IconButton aria-label="Trigger Auth Flow">
                                <OfflineBoltIcon
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
                <Stack direction="row" spacing={2} sx={{ mb: 1 }} style={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <Button color="success" variant="outlined" startIcon={<AddIcon />} onClick={() => setModelOpen(true)} style={{ maxWidth: '200px' }}>
                        Add Client
                    </Button>
                </Stack>
                <TableContainer component={Paper}>
                    <Table aria-label="collapsible table">
                        <TableHead>
                            <TableRow>
                                <StyledTableCell />
                                <StyledTableCell>Issuer</StyledTableCell>
                                <StyledTableCell>Client Id</StyledTableCell>
                                <StyledTableCell>Client Secret</StyledTableCell>
                                <StyledTableCell>Is Active</StyledTableCell>
                                <StyledTableCell align="right">Action</StyledTableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {(data === undefined || data?.length == 0) ?
                                <TableCell colSpan={6}><Alert severity="warning">No Records to show.</Alert></TableCell> :
                                data.map((row) => (<Row key={row?.clientId} row={row} notifyOnDataChange={notifyOnDataChange} />))
                            }
                        </TableBody>
                    </Table>
                </TableContainer>
                <Button color="success" variant="contained" startIcon={<LiveHelpIcon />} onClick={() => handleDrawer(true)} style={{ maxWidth: '150px', float: 'left' }}>
                    Help?
                </Button>
            </Stack>
        </Container>
    );
}