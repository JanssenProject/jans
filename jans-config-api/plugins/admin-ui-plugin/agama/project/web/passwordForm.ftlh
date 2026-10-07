[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta http-equiv="X-UA-Compatible" content="IE=edge"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Reset_Password_Form
	</title><link rel="stylesheet" href="style.css" type="text/css"></head>
<body class="gl-dark">

	<div class="gl-page">
	  <div class="gl-card">
		<img src="https://gluu.org/wp-content/uploads/2020/12/logo.png" alt="Gluu" class="gl-logo">
		<h1 class="gl-title">Reset Password Form</h1>
		<p class="gl-text">Reset your Password</p>

		[#if errorMessage?has_content]
		<div role="alert" class="gl-alert gl-alert-error">${errorMessage}</div>
		[/#if]

		<form method="post">
		  <div class="gl-field">
			<label for="password">Password</label>
			<input type="password" id="password" name="password" autofocus="" required="">
		  </div>
		  <div class="gl-field">
			<label for="confirmPassword">Confirm Password</label>
			<input type="password" id="confirmPassword" name="confirmPassword" required="">
		  </div>
		  <button type="submit" class="gl-btn">Submit</button>
		</form>
	  </div>
	</div>

</body>

</html>
