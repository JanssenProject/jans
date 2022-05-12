<#ftl output_format="HTML">
<!DOCTYPE html>
<html>
<body>

<h1>Want to continue with your authentication process?</h1>

<p><#if message??>If so, click <a href="${webCtx.contextPath}${message}">here</a> to return where you left off.</#if>
<p>To start all over again, click the button below.

<form action="${webCtx.restartUrl}" method="post">
    <input type="submit" value="Restart">
</form>

</body>
</html>