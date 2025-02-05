<#ftl output_format="JSON">
<#import "json_template.ftl" as jt>
{
"success": ${success?c},
<#if success>
    "post_finish_url": 
        <#if (data.post_finish_url)??>"${webCtx.contextPath}/${data.post_finish_url}"<#else>null</#if>
<#else>
    "error": "${jt.escStr(error!"")}"
</#if>
}
