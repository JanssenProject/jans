
//Special widget to send data to server directly
var widget;

//This is called when the ready button is pushed
function initialize(wgt){
    if (!widget)
        widget=wgt
}

function triggerFido2Attestation(req){
    //Wait half a second to start attestation
	setTimeout(startAttestation, 1000, req)
}

function triggerFido2AttestationPA(req){
	console.error('triggerFido2AttestationPA invoked')
	startAttestation(req)
}


function startAttestation(request) {
    console.log('Executing get attestation Fido2 request'+ JSON.stringify(request))
    //setStatus('Get attestation key data.');
    //setStatus('Registration failed.');
    webauthn.createCredential(request)
        .then(data => sendBack(webauthn.responseToObject(data), "onData"))
        .catch(err => {
            console.error('Registration failed- '+ err)
            let errObj = {}
            errObj['excludeCredentials'] = request.excludeCredentials && request.excludeCredentials.length > 0
            errObj['name'] = err.name

            if (err.message) {
                errObj['message'] = err.message
            } else {
                let messages = err.messages
                if (messages && messages.length > 0) {
                    errObj['message'] = messages[0].message
                }
            }
            sendBack(errObj, "onError")
        })
}

function sendBack(obj, handlerName){
    zAu.send(new zk.Event(widget, handlerName, obj, {toServer:true}))
}
