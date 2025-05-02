<#import "../../commons.ftlh" as com>

<@com.main>

<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.4/jquery.min.js"
        integrity="sha512-pumBsjNRGGqkPzKHndZMaAG+bir374sORyzM3uulLV14lN5LyykqNk8eEeUlUkB3U0M4FApyaHraT65ihJhDpQ=="
        crossorigin="anonymous" referrerpolicy="no-referrer"></script>
<script src="${webCtx.contextPath}/js/jquery-qrcode-0.17.0.min.js"></script>
<script src="${webCtx.contextPath}/js/gluu-auth.js"></script>

<script>

function setError(msg) {
    document.getElementById("error").textContent = msg
}

function showQR(qrRequest) {

    //visit https://larsjung.de/jquery-qrcode/ for more info...
    var options = {
        render: 'canvas',
        minVersion: 1,
        maxVersion: 40,
        ecLevel: 'L',
        left: 0,
        top: 0,
        size: 260,
        fill: '#000',
        background: null,
        text: qrRequest,
        radius: 0,
        quiet: 0,
        mode: 0,
        mSize: 0.1,
        mPosX: 0.5,
        mPosY: 0.5,
        label: null,
        fontname: 'sans',
        fontcolor: '#000',
        image: null
    }
    $("#qr_container").html("")
    $("#qr_container").qrcode(options)

}

var timeLeft = ${timeout} <= 0 ? 1 : ${timeout} 
var intervalId 

function updateTimer() {
    
    timeLeft--
    $("#time").text(timeLeft)
    if (timeLeft == 0) {
        clearInterval(intervalId)
        intervalId = null

        $("#qr_container").html("")
    }

}

function onSignInCallback(result) {

    console.log("Result: " + result)
    if (result == "success") {
        document.forms[0].submit()
    } else {
        if (result == "error") {
            setError("${labels("casa.sg.error")?no_esc}")
        }
        timeLeft = 1
        $("#timer").html("")
    }

}

window.onload = (event) => {    
    intervalId = setInterval(updateTimer, 1000)
    showQR('${request?no_esc}')
    gluu_auth.startSessionChecker(onSignInCallback, ${timeout});
}

</script>

<section class="pa4 shadow-4 bg-blank gray cust-section">

    <h2 class="f3 dark-blue2">${labels("casa.sg.scanQR")}</h2>

    <div class="tc mb2 pt2" id="qr_container"></div>
        
    <div class="mw5 dark-red tc pv2 ph0 mt3 f6" id="error"></div>

    <div class="tc mb3 dark-blue" id="timer">${labels("casa.sg.remaining")} <span id="time">-</span> ${labels("casa.sg.secondsAbbr")}</div>
    
    <p class="tc f6 mb4">${labels("casa.sg.waitAuthn2")}</p>
    
    <form method="post" enctype="application/x-www-form-urlencoded">
    </form>
        
    <#if escape!false>
        <@com.alternative />
    </#if>
</section>

</@com.main>
