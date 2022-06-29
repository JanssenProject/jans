<#macro root pageTitle="">

<!doctype html>
<html>
    <head>
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<#if pageTitle?length gt 0><title>${pageTitle}</title></#if>
		<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet"
		    integrity="sha384-1BmE4kWBq78iYhFldvKuhfTAU6auU8tT94WrHftjDbrCEXSU1oBoqyl2QvZ6jIW3" crossorigin="anonymous">        
		<style>
			#logo {
				max-height: 3.25rem;
				margin: 0.5rem;
			}
		</style>
	</head>

<body>

    <div class="d-flex flex-column align-items-center justify-content-between min-vh-100 w-100">
        <header class="d-flex w-100 justify-content-between border-bottom">
            <img id="logo" src="https://gluu.org/wp-content/uploads/2021/02/janssen-project-transparent-630px-182px-300x86.png" />
            <!-- https://gluu.org/wp-content/uploads/2020/12/icon-2.png -->
            <div>
                <#-- some content aligned to the right may go here -->
            </div>
        </header>
        <div class="row col-sm-12 col-md-7 mb-5 pb-5">
            <#-- paste contents provided by specific pages -->
            <#nested>
        </div>
        <footer class="d-flex flex-column align-items-center w-100 pb-2">
            <hr class="w-75">
            <#-- your footer here -->
        </footer>
    </div>

</body>
</html>

</#macro>
