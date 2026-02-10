[#ftl output_format="HTML"]
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Complete Your Profile
	</title></head>
<body>

  
	
	
	
  
  
	<div class="container">
	  <h1>Complete Your Registration
	  </h1>
	  <p class="subtitle">Review your verified NID information and set a password
	  </p>
	  <form method="post" id="setupForm">
		<div class="form-group">
		  <label for="displayName">Full Name</label>
		  <input type="text" id="displayName" name="displayName" value="${displayName!''}">
		</div>
		<div class="form-group">
		  <label for="mail">Email</label>
		  <input type="email" id="mail" name="mail" value="${mail!''}">
		</div>

		<div class="form-group">
		  <label for="mobile">Phone Number</label>
		  <input type="tel" id="mobile" name="mobile" value="${mobile}">
		</div>
		<div class="form-group">
		  <label for="gender">Gender</label>
		  <input type="text" id="gender" name="gender" value="${gender}">
		</div>
		<div class="form-group">
		  <label for="birthdate">Date of Birth</label>
		  <input type="text" id="birthdate" name="birthdate" value="${birthdate}">
		</div>
		<hr>
		<div class="form-group">
		  <label for="password">Password <span class="required">*</span></label>
		  <input type="password" id="password" name="password" required="">
		  <p class="password-requirements">Minimum 8 characters, include uppercase, lowercase, and number
		  </p>
		  <p id="passwordError" class="error">Password does not meet requirements
		  </p>
		</div>
		<div class="form-group">
		  <label for="confirmPassword">Confirm Password <span class="required">*</span></label>
		  <input type="password" id="confirmPassword" name="confirmPassword" required="">
		  <p id="confirmError" class="error">Passwords do not match
		  </p>
		</div>
		<button type="submit" class="btn">Complete Setup</button>
	  </form>
	</div>
  
  
  
</body>
<style>
	* {
	  margin: 0;
	  padding: 0;
	  box-sizing: border-box;
	}
	body {
	  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
	  background: #f5f5f5;
	  padding: 20px;
	}
	.container {
	  max-width: 600px;
	  margin: 0 auto;
	  background: white;
	  border-radius: 8px;
	  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
	  padding: 40px;
	}
	h1 {
	  color: #333;
	  margin-bottom: 10px;
	  font-size: 24px;
	}
	.subtitle {
	  color: #666;
	  margin-bottom: 30px;
	  font-size: 14px;
	}
	.form-group {
	  margin-bottom: 20px;
	  position: relative;
	}
	label {
	  display: block;
	  margin-bottom: 5px;
	  color: #333;
	  font-weight: 500;
	  font-size: 14px;
	}
	input {
	  width: 100%;
	  padding: 12px;
	  border: 1px solid #ddd;
	  border-radius: 4px;
	  font-size: 14px;
	}
	input:focus {
	  outline: none;
	  border-color: #4CAF50;
	}
	.required {
	  color: #e74c3c;
	}
	.password-requirements {
	  font-size: 12px;
	  color: #666;
	  margin-top: 5px;
	}
	.btn {
	  width: 100%;
	  padding: 14px;
	  background: #4CAF50;
	  color: white;
	  border: none;
	  border-radius: 4px;
	  font-size: 16px;
	  font-weight: 500;
	  cursor: pointer;
	  margin-top: 10px;
	}
	.btn:hover {
	  background: #45a049;
	}
	.btn-secondary {
	  background: #2196F3;
	  margin-bottom: 10px;
	}
	.btn-secondary:hover {
	  background: #1976D2;
	}
	.error {
	  color: #e74c3c;
	  font-size: 13px;
	  margin-top: 5px;
	  display: none;
	}
	.info-box {
	  background: #e3f2fd;
	  border-left: 4px solid #2196F3;
	  padding: 15px;
	  margin-bottom: 25px;
	  border-radius: 4px;
	}
	.info-box p {
	  color: #1976D2;
	  font-size: 14px;
	}
  </style>
<script>
	const form = document.getElementById('setupForm');
	const password = document.getElementById('password');
	const confirmPassword = document.getElementById('confirmPassword');
	const passwordError = document.getElementById('passwordError');
	const confirmError = document.getElementById('confirmError');
	function validatePassword(pwd) {
	  return pwd.length >= 8 && 
		/[A-Z]/.test(pwd) && 
		/[a-z]/.test(pwd) && 
		/[0-9]/.test(pwd);
	}
	form.addEventListener('submit', function(e) {
	  let valid = true;
	  passwordError.style.display = 'none';
	  confirmError.style.display = 'none';
	  if (!validatePassword(password.value)) {
		passwordError.style.display = 'block';
		valid = false;
	  }
	  if (password.value !== confirmPassword.value) {
		confirmError.style.display = 'block';
		valid = false;
	  }
	  if (!valid) {
		e.preventDefault();
	  }
	}
						 );
  </script>
</html>