<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head> <style> body {font-size:larger} input{font-size:medium;margin-top:0.5rem} input[type="submit"]{font-size:large;padding:0 1rem 0 1rem} </style> </head>
<body>
    <h1>Guess a number between 0 and 5</h1>
    <p style="color:red">
        <#if failed>
            Wrong number!
        </#if>
    </p>
    <form method="post" enctype="application/x-www-form-urlencoded">
        <input type="number" name="guess" step="1" max="5" min="0" required autofocus>
        <input type="submit" value="I feel lucky!">
    </form>
</body>
</html>
