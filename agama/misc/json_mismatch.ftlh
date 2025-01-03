<#ftl output_format="JSON">
<#import "json_template.ftl" as jt>
{
    <#if flowName??>
        "flowQName": "${flowName}", 
        <#if message??>"retake_get_url": "${webCtx.contextPath}${message}"</#if>,
        "restart_post_url": "${webCtx.restartUrl}"
    <#else>
        "title": "Resource not found",
        "message": "${jt.escStr(message!"")}"        
    </#if>
}
