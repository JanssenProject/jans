import React, { useState } from 'react'
import { v4 as uuidv4 } from 'uuid';
import '../static/css/options.css'
import '../static/css/alerts.css';
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
import { OpenIDConfiguration, LogoutOptions, LoginDetails } from './types';
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
            const message = error instanceof Error ? error.message : String(error);
            setSnackbar({ open: true, message: 'Copy failed: ' + message });
        }
    };

    /**
 * Main logout function with improved error handling and structure
 */
    async function logout(options: LogoutOptions = {}): Promise<void> {
        const { forceSilentLogout = false, notifyOnComplete = true } = options;

        setLoading(true);

        try {
            // Get login details
            const loginDetails = await getStoredLoginDetails();

            if (!loginDetails || !loginDetails.id_token) {
                console.warn('No login details found, nothing to logout');
                if (notifyOnComplete) notifyOnDataChange("true");
                return;
            }

            // Decode ID token to get issuer
            const idToken = loginDetails.id_token;
            const payload = jwtDecode<{ iss: string;[key: string]: any }>(idToken);
            const issuerUrl = payload.iss;

            // Get OpenID configuration for this issuer
            const openidConfiguration = await getOpenIDConfigurationByIssuer(issuerUrl);

            if (!openidConfiguration) {
                throw new Error(`OpenID Configuration not found for issuer: ${issuerUrl}`);
            }

            // Check if end_session_endpoint is available
            if (!openidConfiguration.end_session_endpoint) {
                console.warn('No end_session_endpoint available, performing local logout only');
                await performLocalLogout();
                if (notifyOnComplete) notifyOnDataChange("true");
                return;
            }

            // Remove login details first (immediate local logout)
            await removeLoginDetails();

            // Perform remote logout
            await performRemoteLogout(idToken, openidConfiguration, forceSilentLogout);

            console.log("Logged out successfully.");

        } catch (error) {
            console.error("Logout error:", error);

            // Capture login details before cleanup for potential silent logout
            let savedIdToken: string | null = null;
            let savedOpenIdConfig: OpenIDConfiguration | null = null;
            try {
                const loginDetails = await getStoredLoginDetails();
                if (loginDetails?.id_token) {
                    savedIdToken = loginDetails.id_token;
                    const payload = jwtDecode<{ iss: string }>(loginDetails.id_token);
                    savedOpenIdConfig = await getOpenIDConfigurationByIssuer(payload.iss);
                }
            } catch (captureError) {
                console.error("Failed to capture logout data:", captureError);
            }

            // Fallback: Always ensure local data is cleaned up
            try {
                await performLocalLogout();
            } catch (cleanupError) {
                console.error("Cleanup error:", cleanupError);
            }

            if (savedIdToken && savedOpenIdConfig?.end_session_endpoint) {
                try {
                    await performSilentLogout(savedIdToken, savedOpenIdConfig);
                } catch (silentError) {
                    console.error("Silent logout failed:", silentError);
                }
            }
        } finally {
            setLoading(false);
            if (notifyOnComplete) notifyOnDataChange("true");
        }
    }

    /**
     * Get stored login details
     */
    async function getStoredLoginDetails(): Promise<LoginDetails | null> {
        return new Promise((resolve) => {
            chrome.storage.local.get(["loginDetails"], (result) => {
                if (chrome.runtime.lastError) {
                    console.error("Error reading login details:", chrome.runtime.lastError);
                    resolve(null);
                    return;
                }
                resolve(result.loginDetails || null);
            });
        });
    }

    /**
     * Get OpenID configuration by issuer URL
     */
    async function getOpenIDConfigurationByIssuer(issuerUrl: string): Promise<OpenIDConfiguration | null> {
        const openidConfigurations: OpenIDConfiguration[] = await new Promise((resolve) => {
            chrome.storage.local.get(["openidConfigurations"], (result) => {
                if (chrome.runtime.lastError) {
                    console.error("Error reading OpenID configurations:", chrome.runtime.lastError);
                    resolve([]);
                    return;
                }
                resolve(result?.openidConfigurations ? result.openidConfigurations : []);
            })
        });

        const existingIndex = openidConfigurations.findIndex(c =>
            c.issuer === issuerUrl
        )

        if (existingIndex >= 0) {
            // Return existing configuration
            return openidConfigurations[existingIndex];
        }

        return null;
    }

    /**
     * Remove login details from storage
     */
    async function removeLoginDetails(): Promise<void> {
        return new Promise((resolve, reject) => {
            chrome.storage.local.remove(["loginDetails"], () => {
                if (chrome.runtime.lastError) {
                    reject(chrome.runtime.lastError);
                } else {
                    resolve();
                }
            });
        });
    }

    /**
 * Perform comprehensive local logout
 */
    async function performLocalLogout(): Promise<void> {
        const itemsToRemove = [
            "loginDetails"
        ];

        return new Promise((resolve, reject) => {
            chrome.storage.local.remove(itemsToRemove, () => {
                if (chrome.runtime.lastError) {
                    reject(chrome.runtime.lastError);
                } else {
                    console.log("Local logout completed");
                    resolve();
                }
            });
        });
    }

    /**
     * Perform remote logout via end_session_endpoint
     */
    async function performRemoteLogout(
        idToken: string,
        openidConfiguration: OpenIDConfiguration,
        forceSilent: boolean = false
    ): Promise<void> {
        // If forced silent or no interactive logout needed, use silent logout
        if (forceSilent) {
            return performSilentLogout(idToken, openidConfiguration);
        }

        // Interactive logout with web auth flow
        return new Promise((resolve, reject) => {
            const logoutUrl = buildLogoutUrl(idToken, openidConfiguration);

            chrome.identity.launchWebAuthFlow(
                {
                    url: logoutUrl,
                    interactive: true
                },
                (responseUrl) => {
                    if (chrome.runtime.lastError) {
                        console.warn("Interactive logout failed, trying silent:", chrome.runtime.lastError);
                        // Fallback to silent logout
                        performSilentLogout(idToken, openidConfiguration)
                            .then(resolve)
                            .catch(reject);
                    } else {
                        console.log("Interactive logout completed");
                        resolve();
                    }
                }
            );
        });
    }

    /**
     * Perform silent logout (no user interaction)
     */
    async function performSilentLogout(
        idToken: string,
        openidConfiguration: OpenIDConfiguration
    ): Promise<void> {
        const logoutUrl = buildLogoutUrl(idToken, openidConfiguration);

        try {
            const response = await fetch(logoutUrl, {
                method: 'GET',
                credentials: 'include',
                mode: 'no-cors' // Use no-cors to avoid CORS issues
            });

            console.log("Silent logout request sent");
        } catch (error) {
            console.warn("Silent logout request failed:", error);
            // Silent logout is best effort, don't throw
        }
    }

    /**
     * Build logout URL with parameters
     */
    function buildLogoutUrl(
        idToken: string,
        openidConfiguration: OpenIDConfiguration,
        clientId?: string
    ): string {
        const params = new URLSearchParams({
            state: uuidv4(),
            id_token_hint: idToken
        });

        // Add post_logout_redirect_uri if available
        const redirectUri = chrome.identity.getRedirectURL('logout');
        if (redirectUri) {
            params.append('post_logout_redirect_uri', redirectUri);
        }

        // Add optional parameters
        const optionalParams = {
            client_id: clientId,
            ui_locales: navigator.language,
            logout_hint: idToken
        };

        Object.entries(optionalParams).forEach(([key, value]) => {
            if (value) params.append(key, value);
        });

        return `${openidConfiguration.end_session_endpoint}?${params.toString()}`;
    }


    return (
        <div className="box">
            <UseSnackbar isSnackbarOpen={snackbar.open} handleSnackbar={(open) => setSnackbar({ ...snackbar, open })} message={snackbar.message} />
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
            <Button variant="contained" id="logoutButton" color="success" onClick={() => logout({ forceSilentLogout: false, notifyOnComplete: true })}>Logout</Button>
        </div>
    )
};

export default UserDetails;