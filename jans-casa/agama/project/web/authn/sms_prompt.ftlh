<#import "../commons.ftlh" as com>

<@com.main>

<section class="pa4 shadow-4 bg-blank gray cust-section">

    <h2 class="f3 dark-blue2">${labels("casa.2fa_verification")}</h2>

    <div class="flex flex-column justify-center items-center pa3">
        <img src="${webCtx.contextPath}/img/phone-ver.png" />
        <span class="db w5 tc f7-cust pt2">${labels("casa.sms.choose")}</span>
    </div>

    <form method="post" enctype="application/x-www-form-urlencoded">
    
        <ul class="list-group f7-cust tracked pt1 mb3">
            <#list phones as phone>
                <#assign idx = phone?index>
                
                <li class="list-group-item hover-bg-light-gray gray hover-dark-gray pv1">
                        <input type="radio" name="phoneIdx" class="mr1" value="${idx}" <#if idx == 0>checked</#if>>
                        <span class="mb1">${phone}</span>
                </li>
            </#list>
        </ul>

        <div class="flex justify-end mt2">
            <input type="submit" class="f7-cust bw0 br1 ph4 pv2 bg-bsgreen-success white hover-bsgreen-success hover-white cust-primary-button"
                value="${labels("casa.sms.send")}">
        </div>        
    </form>

    <#if escape!false>
        <@com.alternative />
    </#if>
</section>

</@com.main>
