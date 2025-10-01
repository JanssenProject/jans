<#import "../commons.ftlh" as com>
<@com.main>

<script src="${webCtx.contextPath}/auth/fido2/js/base64js.js">
</script>
<script src="${webCtx.contextPath}/auth/fido2/js/base64url.js">
</script>
<script src="${webCtx.contextPath}/auth/fido2/js/webauthn.js">
</script>

<script>
function setStatus(statusText) {
    document.getElementById("status").textContent = statusText
}

function setError(msg) {
    document.getElementById("error").textContent = msg
}

function startAssertion() {
    
    let request = ${assertion?no_esc}
    setError("")
    setStatus("${labels("casa.fido.wait")?no_esc}")
    
    webauthn.getAssertion(request)
        .then(data => {
                document.getElementById("retry").style.display = "none"
                document.getElementById("tokenResponse").value = JSON.stringify(webauthn.responseToObject(data))
                document.forms[0].submit()
            })
        .catch(err => {
                let name = err.name
                
                let message = ""
    
                if (name === "NotAllowedError") {
                    //Credential not recognized. 
                    message = "${labels("casa.fido.notAllowed")?no_esc}"
                } else if (name === "AbortError") {
                    message = "${labels("casa.fido.abort")?no_esc}"
                } else {
                    message = "${labels("casa.fido.error")?no_esc}"
                    
                    if (err.message) {
                        console.log(err.message)
                    } else if (err.messages) {
                        console.log(err.messages)
                    }
                }
                console.log(name)
                setStatus("")
                setError(message)
                document.getElementById("retry").style.display = "block"
            })
}

window.onload = (event) => {    
    setTimeout(startAssertion, 500)
}
</script>

<section class="pa4 shadow-4 bg-blank gray cust-section">
    <h2 class="f3 dark-blue2">${labels("casa.2fa_verification")}</h2>

    <div class="tc f7 mt2" id="status"></div>
    
    <div class="mw5 dark-red pv3 ph0 ma0 f7-cust" id="error"></div>
    
    <div class="tc green hover-green f7-cust pl3 dn" id="retry">
        <a href="javascript:startAssertion()" class="btn-link link-secondary">${labels("casa.fido.retry")}</a>
    </div>

    <div class="flex flex-column items-center pa3">
        <p class="f4 tc">${labels("casa.fido.insert")}</p>
        <img class="w4" src="${webCtx.contextPath}/img/securitykey.jpg" />
        <div class="db w5 tc f7-cust pv3">${labels("casa.fido.tap_reinsert")}</div>
    </div>
    
    <form method="post" enctype="application/x-www-form-urlencoded">
        <input type="hidden" id="tokenResponse" name="tokenResponse">
    </form>
        
    <#if escape!false>
        <@com.alternative />
    </#if>
                
</section>

</@com.main>
