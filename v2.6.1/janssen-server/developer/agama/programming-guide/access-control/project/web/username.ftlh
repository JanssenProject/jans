<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head> <style> body {font-size:larger} input{font-size:medium;margin-top:0.5rem} input[type="submit"]{font-size:large;padding:0 1rem 0 1rem} </style> </head>
<body>
    <h2>Please enter your username:</h2>
    <p style="color:red">
        <#if showError>
            Check your username was entered correctly
        </#if>
    </p>

    <form method="post" enctype="application/x-www-form-urlencoded">
        <input type="text" name="username" required autofocus>
        <input type="submit" value="Continue">
    </form>
</body>
</html>
