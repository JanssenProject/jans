<#import "q/" + template as ns>

<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1"> 
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet" 
        integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous">
    <title>Club Survey</title>
</head>
<body>
    <div class="container p-4">
        <div class="row">        
            <div class="col-2"> </div>
            <div class="col-8">
                <form method="post" enctype="application/x-www-form-urlencoded">
                    <@ns.main />
                    <div class="text-center p-3">
                        <#if !first>
                            <button type="submit" class="btn btn-primary" name="prev">prev</button>
                        </#if>
                        &nbsp;
                        <#if last>
                            <button type="submit" class="btn btn-primary" name="end"> end </button>
                        <#else>
                            <button type="submit" class="btn btn-primary" name="next">next</button>
                        </#if>
                    </div>
                </form>
            </div>
            <div class="col-2"> </div>
        </div>
    </div>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-FKyoEForCGlyvwx9Hj09JcYn3nv7wiPVlz7YYwJrWVcXK/BmnVDxM+D2scQbITxI" crossorigin="anonymous"></script>

    <#-- 
        For a better understanding of the below code see:
        https://freemarker.apache.org/docs/ref_directive_function.html
        https://freemarker.apache.org/docs/ref_directive_list.html
        https://freemarker.apache.org/docs/dgui_misc_autoescaping.html
        https://developer.mozilla.org/en-US/docs/Web/API/Document/querySelectorAll
        https://developer.mozilla.org/en-US/docs/Web/API/Document/getElementsByName
    -->
    <#function escStr str>
        <#-- Sanitizes a string so it is safe to be assigned to a Javascript variable -->
        <#return str?replace('\\', '\\\\')?replace('\n', '\\n')?replace('\r', '\\r')?replace('\t', '\\t')?replace('"', '\\"')?replace('</', '<\\/')>
    </#function>
    <script src="ans.js"></script>
    <script>
        window.onload = (event) => { 
            let choices = [<#list answer.choice as c>"${c}"<#sep>, </#list>]
            
            <#outputformat 'undefined'>
                let text = "${escStr(answer.text!"")}"
            </#outputformat>

            fillAnswer("option", choices, "text", text)
        }        
    </script>
</body>
</html>
