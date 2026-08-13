[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Welcome
	</title></head>
<body>

  
	
	
	
  
  
	<div align="center" class="registration-container shadow">
	  <div class="flex">
		<img src="https://gluu.org/wp-content/uploads/2020/12/logo.png" id="inh1m">
	  </div>
	  <h1>WELCOME
	  </h1>
	  <div class="separator">
	  </div>
	  <p class="message">
		You've successfully Re-set your password!!!
		<br>
	  </p>
	  <form method="post" id="myForm" class="my-8">
		<div class="space-y-9">
		  <div class="button">
			<button type="submit" class="btn">Continue</button>
		  </div>
		</div>
	  </form>
	</div>
  
  
  
</body>
<style>
	body {
	  font-family: Arial, sans-serif;
	  text-align: center;
	  margin-top: 50px;
	}
	h1 {
	  color: #00c853;
	  font-size: 36px;
	  font-weight: bold;
	}
	.separator {
	  border-top: 1px solid #d0d0d0;
	  width: 80%;
	  margin: 10px auto;
	}
	.message {
	  font-size: 18px;
	  color: #000;
	}
	.registration-container {
	  max-width: 600px;
	  margin: auto;
	  border: 1px solid #28a745;
	  padding: 20px;
	  border-radius: 5px;
	}
	.header {
	  background: #28a745;
	  padding: 10px;
	  color: white;
	  font-size: 18px;
	  font-weight: bold;
	  border-radius: 5px 5px 0 0;
	  display: flex;
	  align-items: center;
	}
	.header i {
	  margin-right: 10px;
	}
	.form-label {
	  font-weight: bold;
	}
	.required {
	  color: red;
	}
	.button {
	  display: flex;
	  justify-content: center;
	  gap: 20px;
	  margin-top: 20px;
	}
	.btn {
	  padding: 10px 20px;
	  font-size: 16px;
	  color: white;
	  background-color: #00c853;
	  border: none;
	  border-radius: 5px;
	  cursor: pointer;
	  transition: background 0.3s ease;
	  text-decoration: none;
	}
	.btn:hover {
	  background-color: #009624;
	}
  </style>
<script>
	window.onload = function () {
	  var form = document.querySelector("form");
	};
  </script>
</html>