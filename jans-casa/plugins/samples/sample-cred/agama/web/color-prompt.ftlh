<#import "../kz1vc3/commons.ftlh" as com>

<@com.main>

    <section class="pa4 shadow-4 bg-blank gray cust-section">
    
        <h2 class="f3 dark-blue2">What's your favorite color?</h2>
    
        <#if !(matches!true)>
            <p class="mw5 dark-red tc pv2 ph0 ma0 f6">That's wrong!</p>
        </#if>
    
        <div class="flex justify-center pv4">
            <p class="f7-cust">We need to verify it's you, pick the right color below</p>
        </div> 
    
        <form method="post" enctype="application/x-www-form-urlencoded">
            <div class="tc w5 pb3">            
                <input type="color" name="selection">
            </div>
            <div class="flex justify-end mt2">
                <input type="submit" class="f7-cust bw0 br1 ph4 pv2 bg-bsgreen-success white hover-bsgreen-success hover-white cust-primary-button"
                    value="Login">
            </div>        
        </form>
        
        <@com.alternative />
    </section>

</@com.main>
