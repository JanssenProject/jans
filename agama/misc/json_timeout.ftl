<#ftl output_format="JSON">
<#import "json_template.ftl" as jt>
{
    "timeout": true,    
    "message": "${jt.escStr(message)}"
}
