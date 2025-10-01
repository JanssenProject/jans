<#ftl output_format="JSON">
<#import "json_template.ftl" as jt>
{
    "title": "An unexpected error ocurred",    
    "message": "${jt.escStr(message!'')}"
}
