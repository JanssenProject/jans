<#import "template.ftlh" as main>

<@main.root pageTitle=success?then("Redirecting you...", "There was an error :(")>
    <#if success>

        <h1 class="fs-2">Almost done!</h1>
        
        <p class="my-4">Redirecting you...
        
        <form action="${webCtx.contextPath}/postlogin.htm" method="post">
            <noscript>
                <p>Your browser does not seem to support Javascript. Click on the button below to be redirected
                <p><input type="submit" class="btn btn-success px-4" value="Continue">
            </noscript>
        </form>
        
        <script>
            function submit() {
                document.forms[0].submit()
            }
            setTimeout(submit, 1000)
        </script>

    <#else>

        <h1 class="fs-2">Authentication failed</h1>
        
        <p class="my-4">${error!""}
        
        <p>Try again later

    </#if>
</@main.root>
