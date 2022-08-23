<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="icon" href="${webCtx.contextPath}/servlet/favicon" type="image/x-icon">
		<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet"
		    integrity="sha384-1BmE4kWBq78iYhFldvKuhfTAU6auU8tT94WrHftjDbrCEXSU1oBoqyl2QvZ6jIW3" crossorigin="anonymous">        
		<style>
			#logo {
				max-height: 3rem;
				margin: 0.5rem;
			}
		</style>
    </head>
<body>

<div class="d-flex flex-column align-items-center justify-content-between min-vh-100 w-100">
    <header class="d-flex w-100 justify-content-between border-bottom">
        <img id="logo" src="https://gluu.org/wp-content/uploads/2021/02/janssen-project-transparent-630px-182px-300x86.png" />
    </header>
    
    <div class="row col-sm-10 col-md-5 my-3">

        <div class="border border-1 rounded p-5">
			<p class="fs-4 mb-5">Create your account</p>

            <p class="fs-6 text-danger mb-4">${error!}</p>

			<form method="post" enctype="application/x-www-form-urlencoded">
				<div class="mb-3 row">
					<label for="name" class="col-md-3 col-form-label">First name</label>
					<div class="col-md-9">
						<input type="text" class="form-control" name="name" id="name" value="${name!}" required>
					</div>
				</div>
				<div class="mb-3 row">
					<label for="username" class="col-md-3 col-form-label">Username</label>
					<div class="col-md-9">
						<input type="text" class="form-control" name="username" id="username"
						    value="${username!}" required>
					</div>
				</div>
				<div class="mb-3 row">
					<label for="pwd" class="col-md-3 col-form-label">Choose a password</label>
					<div class="col-md-9">
						<input type="password" class="form-control" id="pwd" name="pwd" required>
					</div>
				</div>
				<div class="mb-3 row">
					<label for="pwd2" class="col-md-3 col-form-label">Confirm password</label>
					<div class="col-md-9">
						<input type="password" class="form-control" id="pwd2" name="pwd2" required>
					</div>
				</div>
				<div class="mb-3 row">
					<label for="email" class="col-md-3 col-form-label">Your e-mail</label>
					<div class="col-md-9">
						<input type="email" class="form-control" id="email" name="email"
						    placeHolder="name@example.com" value="${email!}" required>
					</div>
				</div>
				<div class="row">
					<div class="col-md-12 d-flex justify-content-end">
						<input type="submit" class="btn btn-success px-4" value="Register">
					</div>
				</div>
			</form>
		</div>

    </div>
    <footer class="d-flex flex-column align-items-center w-100 pb-2">
        <hr class="w-75">
    </footer>
</div>

</body>
</html>
