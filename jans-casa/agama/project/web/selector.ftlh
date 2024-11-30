<#import "commons.ftlh" as com>

<@com.main>

<script src="${cache.casa_assets.contextPath}/scripts/font-awesome-5.12.1.all.min.js"></script>

<section class="pa4 shadow-4 bg-blank gray cust-section">

    <h2 class="f3 dark-blue2">Try another way to sign in</h2>

    <#if message??>
        <p id="alert" class="w5 dark-red pv2 ph0 ma0 f7-cust">${message}</p>
    </#if>

    <div class="pt3 mw6-ns">
        <table class="table table-hover table-sm">
            <tbody class="table-striped">
                <#list enrolledMethods as method>

                    <#if (selector[method])??>
                        <#assign s = selector[method]>
                    <#else>
                        <#assign s = { "icon": "<i class=\"far fa-question-circle\" data-fa-transform=\"shrink-2\"></i>", "text": "???"}>
                    </#if>
                    
                    <tr class="pointer" onClick="javascript:select('${method}')">
                        <td class="w3">
                            <div class="f1 bsgreen">${s.icon?no_esc}</div>
                        </td>
                        <td class="w5" style="vertical-align:middle">
                            <span class="db pa1 f6 dark-blue2 mb0">${s.text?no_esc}</span>
                        </td>
                    </tr>
                </#list>
            </tbody>
        </table>
    </div>

    <form method="post" enctype="application/x-www-form-urlencoded">
        <input type="hidden" id="selection" name="selection" value="">      
    </form>

</section>

<script>
    function select(m) {
        document.getElementById("selection").value = m;
        document.forms[0].submit();
    }

    function hideAlert() {
        let alert = document.getElementById("alert")
        if (alert) {
            alert.remove();
        }
    }

    window.onload = (event) => {
        setTimeout(hideAlert, 3200)
    }
</script>

</@com.main>
