<#import "../kz1vc3/commons.ftlh" as com>

<@com.main>

<section class="pa4 shadow-4 bg-blank gray cust-section">

    <h2 class="f3 dark-blue2">2-step verification</h2>

    <div class="flex flex-column items-center pa3">
        <i class="fas fa-envelope f1"></i>
        <div class="f7-cust pv2 db w5 tc">Choose an e-mail to send you a verification passcode:</div>
    </div>

    <form method="post" enctype="application/x-www-form-urlencoded">
    
        <ul class="f7-cust list-group tracked pt1 mb3">
            <#list email as emails>
                <#assign idx = email?index>
                
                <li class="list-group-item hover-bg-light-gray gray hover-dark-gray pv1">
                    <input type="radio" name="emailIdx" class="mr1" value="${idx}" <#if idx == 0>checked</#if>>
                    <span class="mb1">${email}</span>
                </li>
            </#list>
        </ul>

        <div class="flex justify-end mt2">
            <input class="f7-cust bw0 br1 ph4 pv2 bg-bsgreen-success white hover-bsgreen-success hover-white cust-primary-button"
                type="submit" value="Continue">
        </div>        
    </form>

</section>

</@com.main>
