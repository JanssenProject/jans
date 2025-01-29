import * as React from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell, { tableCellClasses } from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Edit from '@mui/icons-material/Edit';
import { pink, green } from '@mui/material/colors';
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

export default function Cedarling({ data, notifyOnDataChange, isOidcClientRegistered }) {
    const [modelOpen, setModelOpen] = React.useState(false);
    const [drawerOpen, setDrawerOpen] = React.useState(false);
    const [oidcClientRegistered, setOidcClientRegistered] = React.useState(false);


    React.useEffect(() => {
        setOidcClientRegistered(isOidcClientRegistered)
    }, [isOidcClientRegistered]);

    const handleDialog = (isOpen) => {
        setModelOpen(isOpen);
        notifyOnDataChange();
    };

    const handleDrawer = (isOpen) => {
        setDrawerOpen(isOpen);
    };

    return (
        <Container maxWidth="lg">
            {oidcClientRegistered ?
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
                    </Stack>
                </> :
                <Alert severity="warning">At least one OIDC client must be registered in Jans-TARP to add Cedarling configuration.</Alert>
            }
        </Container>
    );
}