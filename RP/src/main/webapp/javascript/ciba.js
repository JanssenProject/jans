function processCibaAuthorizationResponse() {
    let clientCibaTokenDeliveryModeComponent = document.getElementById("mainForm:clientCibaTokenDeliveryMode");
    let cbAuthorizationResponseComponent = document.getElementById("mainForm:bcAuthorizationResponse");
    if (! clientCibaTokenDeliveryModeComponent || !cbAuthorizationResponseComponent) {
        alert('Invalid data, you should process the whole CIBA flow again since dynamic client registration.');
        return null;
    }
    if ( ! cbAuthorizationResponseComponent.value || ! clientCibaTokenDeliveryModeComponent.value)
        return;
    let clientCibaTokenDeliveryMode = clientCibaTokenDeliveryModeComponent.value;
    let cbAuthorizationResponse = JSON.parse(cbAuthorizationResponseComponent.value);
    if (clientCibaTokenDeliveryMode && clientCibaTokenDeliveryMode === 'PING' &&
        cbAuthorizationResponse && cbAuthorizationResponse.auth_req_id) {
        console.log('Initiating process to update every some seconds the status of the request');
        initPingProcess();
    } else {
        console.log('ANOTHER KIND OF DELIVERY MODE');
    }
}

function initPingProcess() {
    let initialDate = new Date().getTime();
    window.updaterInterval = setInterval( function() {
        if (new Date().getTime() - initialDate < 300000) {
            let updateButton = document.getElementById("mainForm:cibaPingStatusButton");
            updateButton.click();
        } else {
            console.log('Finishing the updaterInterval, because there was not any answer from the end user.');
            if (window.updaterInterval) {
                clearInterval(window.updaterInterval);
            }
        }
    }, 2000);
}


function bcAuthorizeEventProcessor(event) {
    if (event.status === 'success'  ) {
        processCibaAuthorizationResponse();
    }
}

function updatePingStatusProcessor(event) {
    if (event.status === 'success') {
        let cibaPingStatusComponent = document.getElementById("mainForm:cibaPingStatusComponent" );
        if (cibaPingStatusComponent && cibaPingStatusComponent.textContent) {
            if (cibaPingStatusComponent.textContent !== 'Waiting...') {
                console.log('Removing interval to check ciba ping status');
                if (window.updaterInterval) {
                    clearInterval(window.updaterInterval);
                }
            }
        } else {
            if (window.updaterInterval) {
                clearInterval(window.updaterInterval);
            }
        }
    }
}