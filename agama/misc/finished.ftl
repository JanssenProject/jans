<#ftl output_format="HTML">
<!DOCTYPE html>
<html>
    <head>
		<title><#if success>Redirecting you...<#else>Error :(</#if></title>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
		<meta name="viewport" content="width=device-width, initial-scale=1" />
		<#if !success>
		    <link rel="icon" href="${webCtx.contextPath}/servlet/favicon" type="image/x-icon" />
		</#if>
	</head>
<body>

<#if success>

<h1>Almost done!</h1>

<p>Redirecting you...

<form action="${webCtx.contextPath}/postlogin.htm" method="post">
    <noscript>
        <p>Your browser does not support Javascript. Click on the button below to be redirected
        <input type="submit" class="btn btn-success px-4" value="Continue">
    </noscript>
</form>

<script>
    function submit() {
        document.forms[0].submit()
    }
    setTimeout(submit, 1000)
</script>

<#else>

<h1>Authentication failed</h1>

<p>${error!""}

<p>Try again later

</#if>

</body>
</html>
