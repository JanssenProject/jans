<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
  <#if section="header">
    ${msg("jans.redirect.to-jans")}
  <#elseif section="form">
    <form id="kc-jans-login-form" class="${properties.kcFormClass!}" action="${jansLoginUrl}" method="get">

       <!-- form parameters (hidden) -->
       <#list openIdAuthParams?keys as paramname>
         <input type="hidden" name="${paramname}" value="${openIdAuthParams[paramname]}"/>
       </#list>
       <!-- end of form parameters -->
       <div class="${properties.kcFormGroupClass!}">
         <div id="kc-form-buttoms" class="${properties.kcFormButtonsClass!}">
          <div class="${properties.kcFormButtonsWrapperClass!}">
            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" 
                   name="login" id="kc-login" type="submit" value="${msg('jans.redirect.too-long-click-here')}"/>
          </div>
         </div>
       </div>
    </form>
    
    <!-- javascript for auth redirection -->
    <script language="javascript">
       function redirectToJansAuth() {
           const loginform = document.getElementById("kc-jans-login-form");
           if(loginform != null) {
               loginform.submit();
           }
       }
       document.addEventListener("DOMContentLoaded", function() {
         setTimeout(redirectToJansAuth,3000);
       });
    </script>
    <!-- end javascript for auth redirection-->
  </#if>
</@layout.registrationLayout>