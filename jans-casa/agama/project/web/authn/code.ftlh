<#import "../commons.ftlh" as com>

<@com.main>

<section class="pa4 shadow-4 bg-blank gray cust-section">

    <h2 class="f3 dark-blue2">${labels("casa.2fa_verification")}</h2>

    <#if !(matches!true)>
        <p class="mw5 dark-red tc pv2 ph0 ma0 f6">${labels("casa.otp.wrong")}</p>
    </#if>

    <div class="flex flex-column items-center pa3">
        <img src="${webCtx.contextPath}/img/ver_code.png" />
        <div class="db w5 tc f7-cust">${labels("casa.otp.title")}</div>
    </div>    

    <form method="post" enctype="application/x-www-form-urlencoded" class="pt2">
        <div class="relative w5 mt1 pb2">            
            <input type="text" class="focused-text w-100 pb1 dark-gray" name="passcode"
                        pattern="[0-9]+" autocomplete="off" required autofocus>
            <#-- Pressing the enter key on this field triggers submission via the FIRST submit button found in the page -->
            <label class="focused-label-big">${labels("casa.otp.enter")}</label>
        </div>
        <div class="flex justify-end mt2">
            <input type="submit" class="f7-cust bw0 br1 ph4 pv2 bg-bsgreen-success white hover-bsgreen-success hover-white cust-primary-button"
                value="${labels("casa.login")}">
        </div>        
    </form>

    <#if escape!false>
        <@com.alternative />
    </#if>
</section>

</@com.main>
