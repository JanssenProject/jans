<html>
  <head>
    <title>Janssen Bridge :: Completing Authentication</title>
  </head>
  <body onload="complete_authentication();">
    <form method="post" action="${actionuri}" id="complete_auth">
    </form>
    <a href="#" onclick="submit_complete_auth_form();">${msg('jans.complete-auth-button')}</a>
    <script language="javascript">
     function submit_complete_auth_form() {
        const action_form = document.getElementById("complete_auth");
        if(action_form != null) {
           action_form.submit();
        }
     }

     function complete_authentication() {

        setTimeout(submit_complete_auth_form,2000);
     }
    </script>
  </body>
</html>