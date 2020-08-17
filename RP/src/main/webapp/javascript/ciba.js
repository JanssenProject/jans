function processCibaAuthorizationResponse() {
    var clientCibaTokenDeliveryModeComponent = document.getElementById("mainForm:clientCibaTokenDeliveryMode");
    var cbAuthorizationResponseComponent = document.getElementById("mainForm:bcAuthorizationResponse");
    if (! clientCibaTokenDeliveryModeComponent || !cbAuthorizationResponseComponent) {
        alert('Invalid data, you should process the whole CIBA flow again since dynamic client registration.');
        return null;
    }
    if ( ! cbAuthorizationResponseComponent.value || ! clientCibaTokenDeliveryModeComponent.value)
        return;
    var clientCibaTokenDeliveryMode = clientCibaTokenDeliveryModeComponent.value;
    var cbAuthorizationResponse = JSON.parse(cbAuthorizationResponseComponent.value);
    if (cbAuthorizationResponse && cbAuthorizationResponse.auth_req_id) {
        console.log('Initiating process to update every some seconds the status of the request');
        if (clientCibaTokenDeliveryMode && clientCibaTokenDeliveryMode === 'PING') {
            initUpdaterProcess('cibaPingStatusButton');
        } else if ( clientCibaTokenDeliveryMode && clientCibaTokenDeliveryMode === 'PUSH' ) {
            initUpdaterProcess('cibaPushStatusButton');
        }
    }
}

function initUpdaterProcess(component) {
    if (window.updaterInterval) {
        clearInterval(window.updaterInterval);
    }
    var initialDate = new Date().getTime();
    window.updaterInterval = setInterval( function() {
        if (new Date().getTime() - initialDate < 300000) {
            var updateButton = document.getElementById(`mainForm:${component}`);
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
        var cibaPingStatusComponent = document.getElementById("mainForm:cibaPingStatusComponent" );
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

function updatePushStatusProcessor(event) {
    if (event.status === 'success') {
        var cibaPushStatusComponent = document.getElementById("mainForm:cibaPushStatusComponent" );
        if (cibaPushStatusComponent && cibaPushStatusComponent.textContent) {
            if (cibaPushStatusComponent.textContent !== 'Waiting...') {
                console.log('Removing interval to check ciba push status');
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