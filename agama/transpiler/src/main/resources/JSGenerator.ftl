<#ftl output_format="JavaScript">
<#--
- This templates generates valid JS code that should be run in non-strict mode
- Only one function is created and must not contain inner functions
- Any functions called here are implemented in file util.js
- An initial underscore in variables and function names prevent flow writers to use variables with the same names in their DSL code
-->
//Generated at ${.now?iso_utc}
function ${flow.@id}<#recurse flow>
}

<#macro header>
(
<#if .node.configs?size = 0>_p<#else>${.node.configs.short_var}</#if>
<#if .node.inputs?size gt 0>
    , ${.node.inputs.short_var?join(", ")}
</#if>
) {
const _basePath = ${.node.base.STRING}
let _it = null, _it2 = null
<#-- idx is accessible to flow writers (it's not underscore-prefixed). It allows to access the status of loops -->
let idx = [], _items = []
</#macro>

<#macro statement>
    <#recurse>
</#macro>

<#macro assignment><@util_preassign node=.node /> ${.node.expression}
</#macro>

<#macro rrf_call>
    <#local hasbool = .node.BOOL?size gt 0>

    <#if .node.variable?size = 0>
        _it = {}
    <#else>
        _it = ${.node.variable}
    </#if>

    _it = _renderReplyFetch(_basePath, ${.node.STRING}, ${hasbool?then(.node.BOOL, "false")}, _it)
    <#-- See FlowService#continueFlow > scriptCtx#resumeContinuation
    string parsing via JS was preferred over returning a Java Map directly because it feels more
    natural for RRF to return a native JS object; it creates the illusion there was no Java involved -->
    _it2 = JSON.parse(_it.second)
    _it = _it.first
    if (!_isNil(_it)) return _abort(_it, _it2)

    <@util_preassign node=.node /> _it2
    <#-- Clear temp variables to make serialization lighter (in the next RRF call) -->
    _it = _it2 = null
</#macro>

<#macro action_call>
    <#local catch=.node.preassign_catch?size gt 0>

    <#if catch>
try {
    var ${.node.preassign_catch.short_var} = null
    </#if>
    <@util_preassign node=.node /> _actionCall(
    <#if .node.static_call?size gt 0>
        null, false, "${.node.static_call.qname}", "${.node.static_call.ALPHANUM}"
        , <@util_argslist node=.node.static_call />
    <#else>
        ${.node.oo_call.variable}, true, null, "${.node.oo_call.ALPHANUM}"
        , <@util_argslist node=.node.oo_call />
    </#if>    
    )

    <#if catch>
} catch (_e) {
    ${.node.preassign_catch.short_var} = _e.javaException
}
    </#if>
</#macro>

<#macro flow_call>
    <#if .node.variable?size gt 0>
        _it = ${.node.variable}
    <#else>
        _it = "${.node.qname}"
    </#if>
_it = _flowCall(_it, _basePath, <@util_url_overrides node=.node.overrides/>, <@util_argslist node=.node />)
if (_it === undefined) return
if (_it.bubbleUp) return _it.value 
    <@util_preassign node=.node /> _it.value
</#macro>

<#macro rfac>
<#if .node.variable?size = 0>
    _it = ${.node.STRING}
<#else>
    _it = ${.node.variable}
</#if>
    <@util_preassign node=.node /> _redirectFetchAtCallback(_it)
</#macro>

<#macro finish>
<#if .node.variable?size gt 0>
    _it = ${.node.variable}
<#elseif .node.BOOL?size gt 0>
    _it = ${.node.BOOL}
<#else>
    _it = ${.node.STRING}
</#if>
return _finish(_it)
</#macro>

<#macro loopy>
    <#if .node.variable?size = 0>
_it = ${.node.UINT}
    <#else>
_it = ${.node.variable}
    </#if>
_ensureNumber(_it, "Number of iterations passed to Repeat is invalid")

idx.push(0)
for (let _times = _it; _times > 0; _times--) {
<@util_loop_body node=.node />
    idx[idx.length - 1]++
}
_it = idx.pop()

<#if .node.preassign?size gt 0>
<@util_preassign node=.node /> _it
</#if>

</#macro>

<#macro loop>
_it = _iterable(${.node.variable}, "Variable to iterate over is not map, list, or string")

_items.push(_it)
idx.push(0)
for (let _item of _items[_items.length - 1]) {
    var ${.node.short_var} = _item
<@util_loop_body node=.node />
    idx[idx.length - 1]++
}
_items.pop()
_it = idx.pop()

<#if .node.preassign?size gt 0>
<@util_preassign node=.node /> _it
</#if>

</#macro>

<#macro ifelse>
if (<#recurse .node.caseof>) {
    <#list .node.statement as st>
        <#recurse st>
    </#list>
}
<@util_else node=.node.elseblock />
</#macro>

<#macro quit_stmt>
if (<#recurse .node.caseof>) break

<#list .node.statement as st>
    <#recurse st>
</#list>
</#macro>

<#macro choice>
    <#list .node.option as case>
        <#if case?index gt 0>else </#if>
if (_equals(${.node.simple_expr }, ${case.simple_expr})) {
        <#list case.statement as st>
            <#recurse st>
        </#list>
}
    </#list>
<@util_else node=.node.elseblock />
</#macro>

<#macro boolean_expr>
    <#if .node.NOT?size gt 0>!</#if>
_equals(${.node.simple_expr[0]}, ${.node.simple_expr[1]})
</#macro>

<#macro boolean_op_expr>
    <#if .node.AND?size = 0>
||
    <#else>
&&
    </#if>
    <#recurse>
</#macro>

<#macro log>
_log(<@util_argslist node=.node />)
</#macro>

<#macro util_loop_body node>
    <#list .node.statement as st>
        <#recurse st>
    </#list>

    <#if .node.quit_stmt?size gt 0><#visit .node.quit_stmt></#if>
</#macro>

<#macro util_else node>
<#if node?size gt 0>
else {
    <#recurse node>
}
</#if>
</#macro>

<#macro util_preassign node>
    <#local var = "" >
    <#if node.preassign?size = 0>
        <#if node.preassign_catch?size gt 0 && node.preassign_catch.variable?size gt 0>
            <#local var = node.preassign_catch.variable >
        </#if>
    <#else>
        <#local var = node.preassign.variable >
    </#if>
    <#if var?length gt 0>
<#if var?index_of(".") == -1 && var?index_of("[") == -1>var </#if>${var} =
    </#if> 
</#macro>

<#macro util_argslist node>[ ${node.argument.simple_expr?join(", ")} ]</#macro>

<#macro util_url_overrides node>[ ${node.STRING?join(", ")} ]</#macro>

<#macro @element></#macro>
