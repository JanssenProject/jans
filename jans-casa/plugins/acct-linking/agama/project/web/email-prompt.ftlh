<#import "../kz1vc3/commons.ftlh" as com>
<@com.main>

    <section class="pa4 shadow-4 bg-blank gray cust-section">
        <h2 class="f4 w5 dark-blue2 pb4">Please provide an e-mail address to proceed</h2>

        <form method="post" enctype="application/x-www-form-urlencoded">
            <div class="relative w5 mt1 pb2">            
                <input type="email" name="email" class="focused-text w-100 pb1 dark-gray" autocomplete="off" required autofocus>
                <label class="focused-label-big">Your e-mail</label>
            </div>
            <div class="flex justify-end mt2">
                <input class="f7-cust bw0 br1 ph4 pv2 bg-bsgreen-success white hover-bsgreen-success hover-white cust-primary-button"
                    type="submit" value="Continue">
            </div>
        </form>
    </section>
    
</@com.main>
