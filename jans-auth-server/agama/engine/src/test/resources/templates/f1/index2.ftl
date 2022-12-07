<#ftl output_format="HTML">
<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<body>
<p>Hi I'm Flow 2!</p>
<p>${value!""}</p>

<form method="post" enctype="application/x-www-form-urlencoded">
    <label for="name">Say something
    <input type="text" id="something" name="something" placeHolder="I don't like cloud native">
    <input type="submit" value="Continue">
</form>

</body>
</html>
