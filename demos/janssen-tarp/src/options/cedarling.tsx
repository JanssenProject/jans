import React from 'react';
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
import AddCedarlingConfig from './addCedarlingConfig';
import CedarlingUnsignedAuthz from './cedarlingUnsignedAuthz';
import CedarlingSignedAuthz from './cedarlingSignedAuthz';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import HelpDrawer from './helpDrawer'
import Alert from '@mui/material/Alert';
import { JsonEditor } from 'json-edit-react'
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';

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

export default function CedarlingMgmt({ data, notifyOnDataChange, isLoggedIn }) {
    const [modelOpen, setModelOpen] = React.useState(false);
    const [drawerOpen, setDrawerOpen] = React.useState(false);
    const [screenType, setScreenType] = React.useState("config");
    const [cedarlingConfig, setCedarlingConfig] = React.useState([]);

    React.useEffect(() => { 
        setCedarlingConfig(data?.cedarlingConfig)
    }, [data])

    const handleDialog = (isOpen) => {
        setModelOpen(isOpen);
        notifyOnDataChange();
    };

    const handleDrawer = (isOpen) => {
        setDrawerOpen(isOpen);
    };

    const handleScreenChange = (
        event: React.MouseEvent<HTMLElement>,
        newScreenType: string,
    ) => {
        setScreenType(newScreenType);
    };

    return (
        <Container maxWidth="lg">
                <AddCedarlingConfig isOpen={modelOpen} handleDialog={handleDialog} newData={{}} />
                <HelpDrawer isOpen={drawerOpen} handleDrawer={handleDrawer} />
                <Stack direction="column" spacing={2} sx={{ mb: 1 }}>
                    <Stack direction="row" spacing={2} sx={{ mb: 1 }} style={{ display: 'flex', justifyContent: 'flex-end' }}>
                        {(cedarlingConfig === undefined || cedarlingConfig?.length == 0) ?
                            <Button color="success" variant="outlined" startIcon={<AddIcon />} onClick={() => setModelOpen(true)} style={{ maxWidth: '200px' }}>
                                Add Configurations
                            </Button> : ''}
                    </Stack>
                    {(cedarlingConfig === undefined || cedarlingConfig?.length == 0) ? '' :
                        <Box maxWidth="md">
                            <ToggleButtonGroup
                                color="primary"
                                value={screenType}
                                exclusive
                                onChange={handleScreenChange}
                                aria-label="Platform"
                            >
                                <ToggleButton value="config">Bootstrap Configuration</ToggleButton>
                                <ToggleButton value="unsignedAuthz">Cedarling Unsigned Authz Form</ToggleButton>
                                {isLoggedIn &&
                                    <ToggleButton value="signedAuthz">Cedarling Signed Authz Form</ToggleButton>
                                }
                            </ToggleButtonGroup>
                        </Box>
                    }
                    {screenType === 'config' &&
                        (<TableContainer component={Paper}>
                            <Table aria-label="collapsible table">
                                <TableHead>
                                    <TableRow>
                                        <StyledTableCell />
                                        <StyledTableCell>Bootstrap Configuration</StyledTableCell>
                                        <StyledTableCell align="right">Action</StyledTableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {(cedarlingConfig === undefined || cedarlingConfig?.length == 0) ?
                                        <TableCell colSpan={6}><Alert severity="warning">No Records to show.</Alert></TableCell> :
                                        cedarlingConfig.map((row, index) => (<Row key={index} row={row} notifyOnDataChange={notifyOnDataChange} />))
                                    }
                                </TableBody>
                            </Table>
                        </TableContainer>)}
                        {(cedarlingConfig === undefined || cedarlingConfig?.length == 0) ? '' :
                            (
                                <>
                                    {screenType === 'unsignedAuthz' &&
                                        <CedarlingUnsignedAuthz data={data} />}
                                    {(screenType === 'signedAuthz' && isLoggedIn)&&
                                        <CedarlingSignedAuthz data={data} />}
                                </>
                            )
                        }
                </Stack>
        </Container>
    );
}