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
    
    <div class="row col-sm-10 col-md-5 mb-5 pb-3">

        <div class="border border-1 rounded mb-3 p-5">
            <p class="fs-4 mb-5">Check your e-mail</p>

            <#if !(matches!true)>
                <p class="fs-6 text-danger mb-3">Wrong code entered</p>
            </#if>

            <form method="post" enctype="application/x-www-form-urlencoded">
                <div class="mb-1 row">
                    <label for="passcode" class="col-md-5 col-form-label">Enter the passcode code sent</label>
                    <div class="col-md-7">
                        <input type="text" class="form-control" id="passcode" name="passcode">
                        <#-- Pressing the enter key on this field triggers submission via the FIRST submit button found in the page -->
                    </div>
                </div>
                <div class="mb-4 row">
                    <div class="col-md-12 d-flex justify-content-end">
                        <input type="submit" class="btn btn-success px-4" value="Continue">
                    </div>
                </div>
                <div class="row">
                    <div class="d-flex justify-content-center">
                        Didn't get a message?
                        <button type="submit" class="btn btn-link p-0 ps-1" id="resend" name="resend" value="">resend code</button>
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
