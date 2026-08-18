[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta http-equiv="X-UA-Compatible" content="IE=edge"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Basic login
	</title><link rel="stylesheet" href="style.css" type="text/css"></head>
<body class="gl-dark">

	<div class="gl-page">
	  <div class="gl-card">
		<img src="https://gluu.org/wp-content/uploads/2020/12/logo.png" id="inh1m" alt="Gluu" class="gl-logo">
		<h1 class="gl-title">Welcome</h1>

		[#if errorMessage??]
		<div role="alert" class="gl-alert gl-alert-error">${errorMessage}</div>
		[/#if]
		[#if lockMessage??]
		<div role="alert" class="gl-alert gl-alert-warn">${lockMessage}</div>
		[/#if]

		<form method="post">
		  <div class="gl-field">
			<label for="username">Username</label>
			<input type="text" id="username" value="${uid!''}" name="username" autofocus="">
		  </div>
		  <div class="gl-field">
			<label for="password">Password</label>
			<input type="password" id="password" name="password">
		  </div>
		  <button type="submit" class="gl-btn">Submit</button>
		</form>
	  </div>
	</div>

</body>

<script>
	window.onload = function () {
	  var form = document.querySelector("form");
	  form.onsubmit = handleFormValidation;
	};
	function handleFormValidation() {
	  var username = document.getElementById('username').value
	  if (username === '') {
		alert('Username is required')
		return false
	  }
	  var password = document.getElementById('password').value
	  if (password === '') {
		alert('Password is required')
		return false
	  }
	  return true
	}
  </script>
</html>
