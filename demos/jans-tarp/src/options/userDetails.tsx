import React, { useState, useEffect } from 'react'
import { v4 as uuidv4 } from 'uuid';
import './options.css'
import './alerts.css';
import { WindmillSpinner } from 'react-spinner-overlay'

const UserDetails = ({data, notifyOnDataChange}) => {
    const [loading, setLoading] = useState(false);
    const [showMoreIdToken, setShowMoreIdToken] = useState(false);
    const [showMoreAT, setShowMoreAT] = useState(false);
    const [showMoreUI, setShowMoreUI] = useState(false);
    async function logout() {
        setLoading(true);
        try {
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
                        window.location.href = `${JSON.parse(openidConfiguration).opConfiguration.end_session_endpoint}?state=${uuidv4()}&post_logout_redirect_uri=${chrome.runtime.getURL('options.html')}&id_token_hint=${JSON.parse(loginDetails).loginDetails.id_token}`
                    }
                });
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
            {data?.displayToken ?
                <>
                    <div className="alert alert-success alert-dismissable fade in">
                        <strong>Access Token</strong>
                        <p>{showMoreAT ? (!!data ? data?.access_token : '') : (!!data ? data?.access_token.substring(0, 250).concat(' ...') : '')}</p>
                        <a href="#" onClick={() => setShowMoreAT(!showMoreAT)}>{showMoreAT ? "Show less" : "Show more"}</a>
                    </div>
                    <div className="alert alert-success alert-dismissable fade in">
                        <strong>Id Token</strong>
                        <p>{showMoreIdToken ? (!!data ? data?.id_token : '') : (!!data ? data?.id_token.substring(0, 250).concat(' ...') : '')}</p>
                        <a href="#" onClick={() => setShowMoreIdToken(!showMoreIdToken)}>{showMoreIdToken ? "Show less" : "Show more"}</a>
                    </div>
                </>
                : ''}
            <div className="alert alert-success alert-dismissable fade in">
                <strong>User Details</strong>
                <p>{showMoreUI ? (!!data ? data?.userDetails : '') : (!!data ? data?.userDetails.substring(0, 250).concat(' ...') : '')}</p>
                <a href="#" onClick={() => setShowMoreUI(!showMoreUI)}>{showMoreUI ? "Show less" : "Show more"}</a>
            </div>
            <hr />
            <button id="logoutButton" onClick={logout}>Logout</button>
        </div>
    )
};

export default UserDetails;