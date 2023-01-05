<#ftl output_format="JSON">
<#import "json_template.ftl" as jt>
{
"success": ${success?c},
<#if success>
    "finish_post_url": "${webCtx.contextPath}/postlogin.htm"
<#else>
    "error": "${jt.escStr(error!"")}"
</#if>
}
