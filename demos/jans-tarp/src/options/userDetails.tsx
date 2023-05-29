import React, { useState, useEffect } from 'react'
import { v4 as uuidv4 } from 'uuid';
import './options.css'

const UserDetails = (data) => {
    //const [userDetails, setUserDetails] = useState("");

    /*chrome.storage.local.get(["loginDetails"], (result) => {
        setUserDetails(result.userDetails);
    });*/

    function logout() {
        chrome.identity.clearAllCachedAuthTokens(async () => {

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
                    //document.getElementById('userDetailsSpan').innerHTML = ''
                    //setShowContent([]);
                    //showDiv(['oidcClientDetails']);
                    //hideDiv(['userDetailsDiv', 'registerForm']);
                    window.location.href = `${JSON.parse(openidConfiguration).opConfiguration.end_session_endpoint}?state=${uuidv4()}&post_logout_redirect_uri=${chrome.runtime.getURL('options.html')}&id_token_hint=${JSON.parse(loginDetails).loginDetails.id_token}`
                    //checkDB();
                }
            });
        });
    }

    return (
        <div className="box">
            <div className="w3-panel w3-pale-yellow w3-border">
                <strong>Note!</strong> The extension will clear session on closing the tab or on opening the extension
                again.
                <br />
            </div>
            <legend><span className="number">O</span> User Details:</legend>
            <hr />
            <span id="userDetailsSpan">
                {!!data.data ? JSON.stringify(data.data.userDetails) : ''}
            </span>
            <hr />
            <button id="logoutButton" onClick={logout}>Logout</button>
        </div>
    )
};

export default UserDetails;