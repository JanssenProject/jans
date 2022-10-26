<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<body>

<#-- rendering this page crashes if neither joke nor phrase are defined -->
<p>${joke}
<p>${phrase}

<form method="post" enctype="application/x-www-form-urlencoded">
    <input type="submit" value="Continue">
</form>

</body>
</html>
