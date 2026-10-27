[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta http-equiv="X-UA-Compatible" content="IE=edge"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Email_OTP_Form
	</title><link rel="stylesheet" href="style.css" type="text/css"></head>
<body class="gl-dark">

	<div class="gl-page">
	  <div class="gl-card">
		<img src="https://gluu.org/wp-content/uploads/2020/12/logo.png" alt="Gluu" class="gl-logo">
		<h1 class="gl-title">Email OTP Form</h1>
		<p class="gl-text">Enter the code we've sent through Email</p>

		<!-- Error Message Block -->
		[#if errorMessage?has_content]
		<div role="alert" class="gl-alert gl-alert-error">${errorMessage}</div>
		[/#if]

		<form method="post">
		  <div class="gl-field gl-otp" id="otp">
			<label for="passcode">Passcode</label>
			<input type="text" name="passcode" id="passcode" pattern="[0-9]+" placeholder="000000" autocomplete="off" required="" autofocus="">
		  </div>
		  <button type="submit" class="gl-btn">Next</button>
		</form>
	  </div>
	</div>

</body>

</html>
