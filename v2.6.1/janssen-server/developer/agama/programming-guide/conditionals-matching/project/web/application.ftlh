<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head> <style> body{font-size:larger} input{font-size:medium;margin-top:0.5rem} input[type="submit"]{font-size:large;padding:0 1rem 0 1rem} </style> </head>
<body>
    <h1>Apply for a membership</h1>
    <form method="post" enctype="application/x-www-form-urlencoded">
        <div>
            <label for="name">Name</label>
            <input type="text" id="name" name="name" required>
        </div>
        <div>
            <label for="email">E-mail</label>
            <input type="email" id="email" name="email" required>
        </div>
        <div>
            <br />
            Interests (tick 3 at most)
        </div>
        
        <#list interests as val>
            <input type="checkbox" id="interest_${val?counter}" name="interest" value="${val}" />
            <label for="interest_${val?counter}">${val}</label>
        </#list>

        <div>
            <br />
            <input type="submit" value="Send">
        </div>
    </form>
</body>
</html>
