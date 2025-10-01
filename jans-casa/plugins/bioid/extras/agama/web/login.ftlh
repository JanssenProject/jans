<#import "commons.ftlh" as com>
    <@com.main>
    <form id="username_form" method="POST">
        <div class="mb-3 row">
            <div class="col-md-8"><label for="username" class="col-md-4 col-form-label">Username:</label><input
                    type="text" id="username" name="username" class="form-control"></div>
        </div>
        <div class="mb-3 row">
            <div class="col-md-8"><label for="password" class="col-md-4 col-form-label">Password:</label><input
                    type="password" id="password" name="password" class="form-control"></div>
        </div>

        <div class="row">
            <div class="col-md-12 d-flex justify-content-end"><input type="submit" id="login" value="Next"
                    class="btn btn-success"></div>
        </div>
    </form>
    </@com.main>
