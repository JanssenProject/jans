import React, { useState, useEffect } from 'react'
import { v4 as uuidv4 } from 'uuid';
import './options.css'
import './alerts.css';
import { WindmillSpinner } from 'react-spinner-overlay'

const UserDetails = (data) => {
    const [loading, setLoading] = useState(false);
    const [showMore, setShowMore] = useState(false);
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
    }

    return (
        <div className="box">
            <div className="w3-panel w3-pale-yellow w3-border">
                <WindmillSpinner loading={loading} color="#00ced1" />
                <br />
            </div>
            <legend><span className="number">O</span> User Details:</legend>
            <hr />
            {data.data.displayToken ?
                <>
                    <div className="alert alert-success alert-dismissable fade in">
                        <strong>Access Token</strong>
                        <p>{!!data.data ? data.data?.access_token : ''}</p>
                    </div>
                    <div className="alert alert-success alert-dismissable fade in">
                        <strong>Id Token</strong>
                        <p>{showMore ? (!!data.data ? data.data?.id_token : '') : (!!data.data ? data.data?.id_token.substring(0, 250).concat(' ...') : '')}</p>
                        <a href="#" onClick={() => setShowMore(!showMore)}>{showMore ? "Show less" : "Show more"}</a>
                    </div>
                </>
                : ''}
            <div className="alert alert-success alert-dismissable fade in">
                <strong>User Details</strong>
                <p>{!!data.data ? JSON.stringify(data.data?.userDetails) : ''}</p>
            </div>
            <hr />
            <button id="logoutButton" onClick={logout}>Logout</button>
        </div>
    )
};

export default UserDetails;