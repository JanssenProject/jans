<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head> <style> body {font-size:larger} input{font-size:medium;margin-top:0.5rem} input[type="submit"]{font-size:large;padding:0 1rem 0 1rem} </style> </head>
<body>
    <h2>${name}, enter the code sent to your registered phone</h2>
    <p style="color:red">
        <#if failed>
            Wrong code!
        </#if>
    </p>
    <form method="post" enctype="application/x-www-form-urlencoded">
        <input type="text" name="code" maxlength="6" required autofocus>
        <input type="submit" value="Continue">
    </form>
</body>
</html>
