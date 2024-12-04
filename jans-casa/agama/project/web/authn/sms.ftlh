<#import "../commons.ftlh" as com>

<@com.main>

<section class="pa4 shadow-4 bg-blank gray cust-section">

    <h2 class="f3 dark-blue2">2-step verification</h2>

    <#if !(matches!true)>
        <p class="mw5 dark-red tc pv2 ph0 ma0 f6">Wrong code entered</p>
    </#if>

    <div class="flex justify-center pa3">
        <img src="${webCtx.contextPath}/img/phone-ver.png" />
    </div> 

    <form method="post" enctype="application/x-www-form-urlencoded" class="pt2">
        <div class="relative w5 mt2 pb2">            
            <input type="text" class="focused-text w-100 pb1 dark-gray" id="passcode" name="passcode"
                        pattern="[0-9]+" autocomplete="off" required autofocus>
            <#-- Pressing the enter key on this field triggers submission via the FIRST submit button found in the page -->
            <label class="focused-label-big">Enter the code sent via SMS</label>
        </div>
        <div class="flex justify-end mt2">
            <input type="submit" class="f7-cust bw0 br1 ph4 pv2 bg-bsgreen-success white hover-bsgreen-success hover-white cust-primary-button"
                value="Login">
        </div>        
    </form>

    <#if escape!false>
        <@com.alternative />
    </#if>
</section>

</@com.main>
