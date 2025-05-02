<#import "../../commons.ftlh" as com>

<@com.main>

<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.4/jquery.min.js"
        integrity="sha512-pumBsjNRGGqkPzKHndZMaAG+bir374sORyzM3uulLV14lN5LyykqNk8eEeUlUkB3U0M4FApyaHraT65ihJhDpQ=="
        crossorigin="anonymous" referrerpolicy="no-referrer"></script>
<script src="${webCtx.contextPath}/js/gluu-auth.js"></script>

<script>

function setError(msg) {
    document.getElementById("error").textContent = msg
}

function onSignInCallback(result) {

    console.log("Result: " + result)
    if (result == "success") {
        document.forms[0].submit()
    } else {
        let msg = "${labels("casa.sg.error")?no_esc}"
        if (result == "timeout") {
            msg = "${labels("casa.sg.timeout")?no_esc}"
        }
        setError(msg/* + ". Try the links below for alternatives"*/)
    }

}

window.onload = (event) => {
    gluu_auth.startSessionChecker(onSignInCallback, ${timeout});
}

</script>

<section class="pa4 shadow-4 bg-blank gray cust-section">

    <h2 class="f4 dark-blue2">${labels("casa.sg.sent")}</h2>
        
    <p class="tc dark-red pv2 ph0 ma0 f7-cust" id="error"></p>
   
    <div align="center" class="mb4"><img class="w-25" src="sg.jpg"></div>

    <form method="post" enctype="application/x-www-form-urlencoded">
       <p class="f6">${labels("casa.sg.waitAuthn1")?no_esc}</p>
       <p class="f6 mb4">
            ${labels("casa.sg.noNotification")}&nbsp;
            <button type="submit" name="scan-QR"
                class="pa0 bw0 br0 blue hover-dark-blue underline-hover bg-transparent cust-link-button f7-cust">${labels("casa.sg.scanQRInstead")}</button>
       </p>
    </form>
        
    <#if escape!false>
        <@com.alternative />
    </#if>
</section>

</@com.main>
