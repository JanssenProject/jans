<#ftl output_format="JSON">
<#function escStr str>
    <#return str?replace('\\', '\\\\')?replace('\n', '\\n')?replace('\r', '\\r')?replace('\t', '\\t')?replace('"', '\\"')>
</#function>
