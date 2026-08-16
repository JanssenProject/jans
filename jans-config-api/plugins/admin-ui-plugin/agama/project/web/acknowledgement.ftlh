[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Welcome
	</title><link rel="stylesheet" href="style.css" type="text/css"></head>
<body class="gl-dark">

	<div class="gl-page">
	  <div class="gl-card">
		<img src="https://gluu.org/wp-content/uploads/2020/12/logo.png" id="inh1m" alt="Gluu" class="gl-logo">
		<h1 class="gl-title">Welcome</h1>

		<div role="status" class="gl-alert gl-alert-success">
		  You've successfully re-set your password!
		</div>

		<form method="post" id="myForm">
		  <button type="submit" class="gl-btn">Continue</button>
		</form>
	  </div>
	</div>

</body>

<script>
	window.onload = function () {
	  var form = document.querySelector("form");
	};
  </script>
</html>
