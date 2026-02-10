[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta http-equiv="X-UA-Compatible" content="IE=edge"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Error Page
	</title><link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2/dist/tailwind.min.css" type="text/css"></head>
<body>

  
	
	
	
	
	
  
  
	<div class="md:h-screen bg-white relative flex flex-col justify-center items-center">
	  <div class="md:border md:border-gray-300 bg-white md:shadow-lg shadow-none rounded p-10 align-middle">
		<div class="flex flex-col items-center space-y-3">
		  <div class="flex">
			<img src="https://gluu.org/wp-content/uploads/2020/12/logo.png">
		  </div>
		  <span class="text-2xl font-semi-bold leading-normal">Error details</span>
		  [#if errorMessage?has_content]
		  <div role="alert" align="center" class="alert alert-danger mt-3 error">
			<p>${errorMessage} 
			</p>
		  </div>
		  [/#if]
		</div>
		<form method="post" id="myForm" class="my-8">
		  <div class="space-y-9">
			<div class="button">
			  <button type="submit" class="btn">Ok</button>
			</div>
		  </div>
		</form>
	  </div>
	</div>
  
  
  
</body>
<style>
	.required {
	  color: red;
	}
	.error-message {
	  color: red;
	  font-size: 13px;
	  margin-top: 5px;
	}
	.error{
	  font-weight: bold;
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