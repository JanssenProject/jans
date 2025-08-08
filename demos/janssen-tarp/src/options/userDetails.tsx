import React, { useState } from 'react'
import { v4 as uuidv4 } from 'uuid';
import './options.css'
import './alerts.css';
import { WindmillSpinner } from 'react-spinner-overlay'
import { JsonEditor } from 'json-edit-react'
import Button from '@mui/material/Button';
import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import Typography from '@mui/material/Typography';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { jwtDecode } from "jwt-decode";
import { IJWT } from './IJWT';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import { pink } from '@mui/material/colors';
import UseSnackbar from './UseSnackbar';
const UserDetails = ({ data, notifyOnDataChange }) => {
    const [loading, setLoading] = useState(false);
    const [showPayloadIdToken, setShowPayloadIdToken] = useState(false);
    const [showPayloadAT, setShowPayloadAT] = useState(false);
    const [showPayloadUI, setShowPayloadUI] = useState(false);
    const [snackbar, setSnackbar] = React.useState({ open: false, message: '' });
    const [decodedTokens, setDecodedTokens] = React.useState<{
        access_token: IJWT;
        userinfo_token: IJWT;
        id_token: IJWT;
    }>({
        access_token: { header: {}, payload: {} },
        userinfo_token: { header: {}, payload: {} },
        id_token: { header: {}, payload: {} },
    });
    const [jwtTokens, setJwtTokens] = React.useState<{
        access_token: String;
        userinfo_token: String;
        id_token: String;
    }>({
        access_token: "",
        userinfo_token: "",
        id_token: "",
    });

    React.useEffect(() => {

        if (data) {
            setDecodedTokens({
                access_token: decodeJWT(data.access_token),
                userinfo_token: decodeJWT(data.userDetails),
                id_token: decodeJWT(data.id_token),
            });
            setJwtTokens({
                access_token: data.access_token,
                userinfo_token: data.userDetails,
                id_token: data.id_token,
            })
        }
    }, [data])

    const decodeJWT = (token: string | undefined): IJWT => {
        try {
            if (!token) return { header: {}, payload: {} }; // Handle undefined token
            const payload = jwtDecode(token);
            const header = jwtDecode(token, { header: true });
            return { header, payload };
        } catch (error) {
            console.error("Error decoding JWT:", error);
            return { header: {}, payload: {} }; // Return empty object on error
        }
    };

    const copyToClipboard = () => {
        try {
          const jsonString = JSON.stringify(jwtTokens, null, 2); // pretty print
          navigator.clipboard.writeText(jsonString);
          setSnackbar({ open: true, message: 'Token JSON copied to clipboard!' });
        } catch (error) {
          setSnackbar({ open: true, message: 'Copy failed: ' + error.message });
        }
      };

    async function logout() {
        setLoading(true);
        try {

            const loginDetails: string = await new Promise((resolve, reject) => {
                chrome.storage.local.get(["loginDetails"], (result) => {
                    resolve(JSON.stringify(result));
                });
            });

            const openidConfiguration: string = await new Promise((resolve, reject) => { chrome.storage.local.get(["opConfiguration"], (result) => { resolve(JSON.stringify(result)); }) });

            chrome.storage.local.remove(["loginDetails"], function () {
                var error = chrome.runtime.lastError;
                if (error) {
                    console.error(error);
                } else {
                    const logoutUrl = `${JSON.parse(openidConfiguration).opConfiguration.end_session_endpoint}?state=${uuidv4()}&post_logout_redirect_uri=${chrome.identity.getRedirectURL('logout')}&id_token_hint=${JSON.parse(loginDetails).loginDetails.id_token}`
                    chrome.identity.launchWebAuthFlow(
                        {
                            url: logoutUrl,
                            interactive: true
                        },
                        (responseUrl) => {
                            if (chrome.runtime.lastError) {
                                console.error("Logout error:", chrome.runtime.lastError);
                            } else {
                                console.log("Logged out successfully."); 
                                chrome.storage.local.remove("tokens");
                            }
                        }
                    );

                }
            });
        } catch (err) {
            const loginDetails: string = await new Promise((resolve, reject) => {
                chrome.storage.local.get(["loginDetails"], (result) => {
                    resolve(JSON.stringify(result));
                });
            });

            const openidConfiguration: string = await new Promise((resolve, reject) => { chrome.storage.local.get(["opConfiguration"], (result) => { resolve(JSON.stringify(result)); }) });

            chrome.storage.local.remove(["loginDetails"], function () {
                var error = chrome.runtime.lastError;
                if (error) {
                    console.error(error);
                } else {
                    fetch(`${JSON.parse(openidConfiguration).opConfiguration.end_session_endpoint}?state=${uuidv4()}&post_logout_redirect_uri=${chrome.runtime.getURL('options.html')}&id_token_hint=${JSON.parse(loginDetails).loginDetails.id_token}`)
                }
            });

        }
        setLoading(false);
        notifyOnDataChange("true");
    }

    return (
        <div className="box">
            <UseSnackbar isSnackbarOpen={snackbar.open} handleSnackbar={(open) => setSnackbar({ ...snackbar, open })} message={snackbar.message}/>
            <div className="w3-panel w3-pale-yellow w3-border">
                <WindmillSpinner loading={loading} color="#00ced1" />
                <br />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', maxWidth: '90vw' }}>
            <legend><span className="number">O</span> User Details:</legend>
            <Tooltip title="Copy tokens JSON">
                  <IconButton aria-label="Copy" style={{ maxWidth: '5vmax', float: 'right' }} onClick={copyToClipboard}>
                    <ContentCopyIcon sx={{ color: pink[500] }} />
                  </IconButton>
                </Tooltip>
                </div>
            <hr />
            
            {data?.displayToken &&
                <>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<ExpandMoreIcon />}
                            aria-controls="panel1-content"
                            id="panel1-header"
                        >
                            <Typography component="span"><strong>Access Token</strong></Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <div className="alert alert-success alert-dismissable fade in">
                                <p>{showPayloadAT ? (!!data ?
                                    <>
                                        <JsonEditor collapse={true} viewOnly={true} data={decodedTokens.access_token.header} rootName="header" />
                                        <JsonEditor data={decodedTokens.access_token.payload} collapse={true} viewOnly={true} rootName="payload" />
                                    </>
                                    : '') : (!!data ? data?.access_token : '')}</p>
                                <a href="#!" onClick={() => setShowPayloadAT(!showPayloadAT)}>{showPayloadAT ? "Show JWT" : "Show Payload"}</a>
                            </div>
                        </AccordionDetails>
                    </Accordion>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<ExpandMoreIcon />}
                            aria-controls="panel1-content"
                            id="panel1-header"
                        >
                            <Typography component="span"><strong>Id Token</strong></Typography>
                        </AccordionSummary>
                        <AccordionDetails>

                            <div className="alert alert-success alert-dismissable fade in">
                                <p>{showPayloadIdToken ? (!!data ?
                                    <>
                                        <JsonEditor collapse={true} viewOnly={true} data={decodedTokens.id_token.header} rootName="header" />
                                        <JsonEditor data={decodedTokens.id_token.payload} collapse={true} viewOnly={true} rootName="payload" />
                                    </>
                                    : '') : (!!data ? data?.id_token : '')}</p>
                                <a href="#!" onClick={() => setShowPayloadIdToken(!showPayloadIdToken)}>{showPayloadIdToken ? "Show JWT" : "Show Payload"}</a>
                            </div>
                        </AccordionDetails>
                    </Accordion>
                </>}
            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon />}
                    aria-controls="panel1-content"
                    id="panel1-header"
                >
                    <Typography component="span"><strong>User Details</strong></Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <div className="alert alert-success alert-dismissable fade in">
                        <p>{showPayloadUI ? (!!data ?
                            <>
                                <JsonEditor collapse={true} viewOnly={true} data={decodedTokens.userinfo_token.header} rootName="header" />
                                <JsonEditor data={decodedTokens.userinfo_token.payload} collapse={true} viewOnly={true} rootName="payload" />
                            </>
                            : '') : (!!data ? data?.userDetails : '')}</p>
                        <a href="#!" onClick={() => setShowPayloadUI(!showPayloadUI)}>{showPayloadUI ? "Show JWT" : "Show Payload"}</a>
                    </div>
                </AccordionDetails>
            </Accordion>
            <hr />
            <Button variant="contained" id="logoutButton" color="success" onClick={logout}>Logout</Button>
        </div>
    )
};

export default UserDetails;