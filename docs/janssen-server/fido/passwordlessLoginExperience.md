*Seamless Passwordless and Usernameless Login*

- Conditional UI eliminates the need for both passwords and usernames.
- Users don’t have to recall their registered email or username.
- Browsers provide autofill suggestions, displaying the correct email or username along with the appropriate passkey.
- Conditional UI allows websites to initiate a passkey/WebAuthn request alongside a standard password prompt.
- The effectiveness of Conditional UI depends on the compatibility of the user’s device and browser. Read more here(https://passkeys.dev/device-support/)


Sequence diagram 

```
User->RP: isConditionalMediationAvailable()
RP->FIDO Server: POST start Conditional UI
FIDO Server->RP: PublicKeyCredentialRequestOptions
User->RP: credentials.get(PublicKeyCredentialRequestOptions + mediation: conditional)
RP->User: show autofill selection, \nread browser cookie and find allowed credentials \n(does not contain username)
User->RP: select passkey from autofill & authenticate (e.g. Face ID, Touch ID)
RP->FIDO Server: Authenticator response
FIDO Server->RP: Logged in

```

Implementing a usernameless flow for a web-app (RP):

Use the Person authentication script as a guideline: https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/person_authentication/fido2-external-authenticator/Fido2ExternalAuthenticator.py

1. During Registraion: At the time of Credential registration, a cookie called 'allowList' should be added.
Content of the allowList is as follows, note that we are not storing usernames or email ids:
 ```
 [{ id: ...., type: 'public-key', transports: ['usb', 'ble', 'nfc']}]
 
 ```
 
Python code for the same can be noted in the Person Authentication Script - https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/person_authentication/fido2-external-authenticator/Fido2ExternalAuthenticator.py

2. Authentication :

In Assertion Request, set RP id and Allow credentials fetched from the cookie `allowList`
```
assertionRequest = AssertionOptions()
assertionRequest.setRpId(domain)
assertionRequest.setAllowCredentials(Arrays.asList(allowList))
assertionResponse = assertionService.authenticate(assertionRequest).readEntity(java.lang.String)
```                
				
3. In the login web page, here is an .xhtml sample
```
 <h:inputText placeholder="#{msgs['login.username']}" id="username" name="username" required="true" value="#{credentials.username}" >
    <f:passThroughAttribute name="autocomplete" value="username webauthn"/>
 </h:inputText>
 
```																
on submit, do the following:
javascript code : 
```

if ( window.PublicKeyCredential &amp;&amp; PublicKeyCredential.isConditionalMediationAvailable)
{

  // Step A: call webauthn.js's getAssertionConditional method with AssertionOptions ( rpId, allowList)
  // Step B: create an io.jans.fido2.model.assertion.AssertionResult object using step A and call io.jans.fido2.client.AssertionService's verify() method
             In otherwords call the FIDO server's API "/result" for Assertion 
}

```
				