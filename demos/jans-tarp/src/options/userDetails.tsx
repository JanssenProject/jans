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
const UserDetails = ({ data, notifyOnDataChange }) => {
    const [loading, setLoading] = useState(false);
    const [showPayloadIdToken, setShowPayloadIdToken] = useState(false);
    const [showPayloadAT, setShowPayloadAT] = useState(false);
    const [showPayloadUI, setShowPayloadUI] = useState(false);
    const [decodedTokens, setDecodedTokens] = React.useState<{
        accessToken: IJWT;
        userInfoToken: IJWT;
        idToken: IJWT;
    }>({
        accessToken: { header: {}, payload: {} },
        userInfoToken: { header: {}, payload: {} },
        idToken: { header: {}, payload: {} },
    });

    React.useEffect(() => {

        if (data) {
            setDecodedTokens({
                accessToken: decodeJWT(data.access_token),
                userInfoToken: decodeJWT(data.userDetails),
                idToken: decodeJWT(data.id_token),
            });
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
            <div className="w3-panel w3-pale-yellow w3-border">
                <WindmillSpinner loading={loading} color="#00ced1" />
                <br />
            </div>
            <legend><span className="number">O</span> User Details:</legend>
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
                                        <JsonEditor collapse={true} viewOnly={true} data={decodedTokens.accessToken.header} rootName="header" />
                                        <JsonEditor data={decodedTokens.accessToken.payload} collapse={true} viewOnly={true} rootName="payload" />
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
                                        <JsonEditor collapse={true} viewOnly={true} data={decodedTokens.idToken.header} rootName="header" />
                                        <JsonEditor data={decodedTokens.idToken.payload} collapse={true} viewOnly={true} rootName="payload" />
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
                                <JsonEditor collapse={true} viewOnly={true} data={decodedTokens.userInfoToken.header} rootName="header" />
                                <JsonEditor data={decodedTokens.userInfoToken.payload} collapse={true} viewOnly={true} rootName="payload" />
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