[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta http-equiv="X-UA-Compatible" content="IE=edge"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Email_OTP_Form
	</title><link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2/dist/tailwind.min.css" type="text/css"></head>
<body>

  
	
	
	
	
	
  
  
	<div class="md:h-screen bg-white relative flex flex-col justify-center items-center">
	  <div class="md:border md:border-gray-300 bg-white md:shadow-lg shadow-none rounded p-10 align-middle">
		<div class="flex flex-col items-center space-y-3">
		  <div class="flex">
			<img src="https://gluu.org/wp-content/uploads/2020/12/logo.png">
		  </div>
		  <span class="text-2xl font-semi-bold leading-normal">Email OTP Form</span>
		  <!-- Error Message Block -->
		  [#if errorMessage?has_content]
		  <div role="alert" align="center" class="alert alert-danger mt-3 error">
			<p>${errorMessage} 
			</p>
		  </div>
		  [/#if]
		  <p class="leading-normal">Enter the code we've sent through Email
		  </p>
		</div>
		<form method="post" class="mt-8">
		  <div id="otp" class="flex flex-row justify-center text-center px-2 mt-5">
			<input type="text" name="passcode" id="passcode" pattern="[0-9]+" placeholder="000000" autocomplete="off" required="" autofocus="" class="bg-gray-50 w-full rounded px-3 border border-gray-300 pt-2 pb-2 focus:border-green-700">
		  </div>
		  <div class="space-y-9 mt-4">
			<div class="text-sm flex justify-center items-center">
			  <button type="submit" class="py-2 px-6 rounded text-white btn bg-green-500 hover:bg-green-600">
				Next
			  </button>
			</div>
		  </div>
		</form>
	  </div>
	</div>
  
  
</body>
<style>
	body {
	  font-family: Arial, sans-serif;
	  background-color: #f8f9fa;
	  display: flex;
	  justify-content: center;
	  align-items: center;
	  height: 100vh;
	  margin: 0;
	}
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
  </style>

</html>