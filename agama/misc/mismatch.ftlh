<#import "template.ftlh" as main>
<@main.root>

    <#if flowName??>
        <h1 class="fs-2">Want to continue with your authentication process?</h1>
        
        <p class="my-4"><#if message??>If so, click <a href="${webCtx.contextPath}${message}">here</a>
            to return where you left off.</#if>
    
        <p>To start all over again, click the button below.
        
        <form action="${webCtx.restartUrl}" method="post">
            <input type="submit" value="Restart" class="btn btn-secondary px-4">
        </form>
    <#else>
        <h1 class="fs-2">Resource not found</h1>
    
        <p class="my-4">${message!""}        
    </#if>

</@main.root>
