<#import "../kz1vc3/commons.ftlh" as com>
<@com.main>

    <div class="f6 flex justify-center mv4">
        <div class="w-90 w-75-ns ba bw1 br2 b--light-gray flex flex-column items-center bg-blank">

            <div class="pa4 tc">
                ${labels("certauthn.enrollment." + status)}
            </div>
            <form name="login" method="post" enctype="application/x-www-form-urlencoded" class="pb4">
                <input type="submit" value="${labels("certauthn.continue")}"
                        class="f7-cust bw0 br1 ph4 pv2 bg-bsgreen-success white hover-bsgreen-success hover-white cust-primary-button">
            </form>            

        </div>
    </div>

</@com.main>
