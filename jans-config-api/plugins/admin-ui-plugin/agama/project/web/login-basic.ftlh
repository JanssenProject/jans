[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta http-equiv="X-UA-Compatible" content="IE=edge"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Basic login
	</title><link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2/dist/tailwind.min.css" type="text/css"><link rel="stylesheet" href="style.css" type="text/css"></head>
<body>

  
	
	
	
	
	
	
  
  
	<div class="md:h-screen bg-white relative flex flex-col justify-center items-center">
	  <div class="md:border md:border-gray-300 bg-white md:shadow-lg shadow-none rounded p-10 align-middle loginCard">
		<div class="flex flex-col items-center space-y-3">
		  <div class="flex">
			<img src="https://gluu.org/wp-content/uploads/2020/12/logo.png" id="inh1m">
		  </div>
		  <div class="inline-flex items-center justify-center w-full">
			<hr class="w-64 h-1 my-8 bg-gray-200 border-0 rounded dark:bg-gray-700">
			<span class="absolute px-3 font-medium text-white-900 -translate-x-1-2 bg-white left-1-2 dark:text-white dark:bg-gray-900">Welcome</span>
		  </div>
		  [#if errorMessage??]
		  <div role="alert" class="p-4 mb-4 text-sm text-red-800 rounded-lg bg-red-50 dark:bg-gray-800 dark:text-red-400">
			<span class="font-medium"></span> ${errorMessage}
		  </div>
		  [/#if]
		  [#if lockMessage??]
		  <div role="alert" class="p-4 mb-4 text-sm text-yellow-800 rounded-lg bg-yellow-50 dark:bg-gray-800 dark:text-yellow-300">
			<span class="font-medium"></span>${lockMessage}
		  </div>
		  [/#if]
		</div>
		<form method="post" class="my-8">
		  <div class="relative mb-2">
			<label for="username" class="bg-gray-50label mt-2 ml-1 leading-tighter text-gray-600 text-base cursor-text">
			  Username
			</label>
			<input type="text" id="username" value="${uid!''}" name="username" autofocus="" class="bg-gray-50 w-full rounded px-3 border border-gray-300 pt-2 pb-2 focus:border-green-700">
		  </div>
		  <div class="relative mb-2">
			<label for="password" class="label mt-2 ml-1 leading-tighter text-gray-600 text-base cursor-text">
			  Password
			</label>
			<input type="password" id="password" name="password" autofocus="" class="bg-gray-50 w-full rounded px-3 border border-gray-300 pt-2 pb-2 focus:border-green-700">
		  </div>
		  <div class="space-y-9">
			<div class="text-sm flex justify-center items-center">
			  <button type="submit" class="py-2 px-6 rounded text-white btn bg-green-500 hover:bg-green-600 button-gradient">
				Submit
			  </button>
			</div>
		  </div>
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