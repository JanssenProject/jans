import React from "react";
import { createRoot } from "react-dom/client";
import Options from "./options";

function init() {
    const appContainer = document.createElement('div')
    document.body.appendChild(appContainer)
    if (!appContainer) {
        throw new Error("Can not find AppContainer");
    }
    const root = createRoot(appContainer)
    console.log(appContainer)
    removeLoginDetails();
    root.render(<Options />);
}

function removeLoginDetails() {

    chrome.storage.local.remove(["loginDetails"], function () {
        var error = chrome.runtime.lastError;
        if (error) {
            console.error(error);
        } else {
            console.log('Removed login details.');
        }
    });
}

init();
