<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head> <style> body {font-size:larger} input{font-size:medium;margin-top:0.5rem} input[type="submit"]{font-size:large;padding:0 1rem 0 1rem} </style> </head>
<body>
    <#if !first>
        <p>
            Thanks for adding <b>${name}</b>. If you want to add another referral fill the form below,
            otherwise click <a href="javascript:document.forms.endForm.submit()">here</a>.
        </p>
        <hr>    
        <form name="endForm" method="post" enctype="application/x-www-form-urlencoded">
        </form>
    </#if>

    <h2>Refer a friend and get rewarded!</h2>
        
    <form method="post" enctype="application/x-www-form-urlencoded">
        <div>
            <label for="name">Name</label>
            <input type="text" id="name" name="name" required autofocus>
        </div>
        <div>
            <label for="email">E-mail</label>
            <input type="email" id="email" name="email" required>
        </div>

        <div>
            <br />
            <input type="submit" value="Submit">
        </div>
    </form>
</body>
</html>
