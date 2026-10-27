<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head> <style> body {font-size:larger} input[type="submit"]{font-size:large;padding:0 1rem 0 1rem} </style> </head>
<body>
    <h1>Thank you!</h1>
    
    <p>
        We'll try not to send spam to your friends:
            
        <#list referrals as friend>
            ${friend.name}<#sep>, </#sep>
        </#list>
    </p>

    <form method="post" enctype="application/x-www-form-urlencoded">
        <input type="submit" value="Continue">
    </form>
</body>
</html>
