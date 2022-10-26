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
			.btn-my-outline:hover {
			    background-color:#efefef;
			    border-radius: 0;
			}
		</style>
    </head>
<body>

<div class="d-flex flex-column align-items-center justify-content-between min-vh-100 w-100">
    <header class="d-flex w-100 justify-content-between border-bottom">
        <img id="logo" src="https://gluu.org/wp-content/uploads/2021/02/janssen-project-transparent-630px-182px-300x86.png" />
    </header>
    
    <div class="row col-sm-10 col-md-5 mb-5 pb-3">

        <div class="border border-1 rounded mb-3 p-5">
			<p class="fs-4 mb-5">Sign in using:</p>

			<form method="post" enctype="application/x-www-form-urlencoded">            
                <#if providers?keys?size gt 0>
                    <div class="row px-2">
                    
                        <#assign x=0>
                        <#list providers as id, provider>
                            <#if provider.enabled!true>

                                <#if x%2 == 0><hr class="m-0"></#if>
                                <#assign x++>

                                <div class="w-50 my-2">
                                    <button type="submit" class="btn fs-5 btn-my-outline" name="providerId" value="${id}">
                                        <img src="${provider.logoImg!"none.png"}" style="max-height:3rem">
                                        ${provider.displayName!id}
                                    </button>
                                </div>
                            </#if>
                        </#list>
                        <hr class="m-0">
                    </div>
                </#if>

				<div class="row mt-5">
					<div class="col-md-12 d-flex justify-content-end">
						<input type="submit" class="btn btn-success px-4" value="Already have an account?">
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
