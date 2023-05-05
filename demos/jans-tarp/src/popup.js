'use strict';

import './popup.css';
const axios = require('axios');
const qs = require('qs');
(function () {

  onLoad();
  /**
   * The function checks if login details are stored in local storage and triggers a code flow button if
   * they are empty, then checks the database.
   */
  async function onLoad() {
    showDiv(['loadingDiv'])
    const loginDetails = await new Promise((resolve, reject) => {
      chrome.storage.local.get(["loginDetails"], (result) => {
        resolve(JSON.stringify(result));
      });
    });

    if (!isEmpty(loginDetails) && Object.keys(JSON.parse(loginDetails)).length !== 0) {
      trigCodeFlowButton()
    } else {
      checkDB();
    }
  }

  async function checkDB() {
    chrome.storage.local.get(["oidcClient"]).then((result) => {
      if (result.oidcClient != undefined) {
        console.log("OIDC Client present in DB: " + JSON.stringify(result.oidcClient));

        document.getElementById('opHost').value = result.oidcClient.op_host
        document.getElementById('clientId').value = result.oidcClient.client_id
        document.getElementById('clientSecret').value = result.oidcClient.client_secret

        showDiv(['oidcClientDetails']);
        hideDiv(['registerForm']);
      } else {
        showDiv(['registerForm']);
        hideDiv(['oidcClientDetails']);
      }
      hideDiv(['loadingDiv'])
    });
  }

  async function resetClient() {
    showDiv(['loadingDiv'])
    chrome.storage.local.remove(["oidcClient", "opConfiguration"], function () {
      var error = chrome.runtime.lastError;
      if (error) {
        console.error(error);
      } else {
        checkDB();
      }
    });
  }

  async function trigCodeFlowButton() {
    
    const redirectUrl = chrome.identity.getRedirectURL()/*chrome.runtime.getURL('redirect.html')*/
    await chrome.storage.local.get(["oidcClient"]).then(async (result) => {
      if (result.oidcClient != undefined) {
        let authzUrl = result.oidcClient.authorization_endpoint +
          '?scope=' + `${result.oidcClient.scope[0]}+profile` +
          '&acr_values=' + result.oidcClient.acr_values[0] +
          '&response_type=' + result.oidcClient.response_type[0] +
          '&redirect_uri=' + redirectUrl +//result.oidcClient.redirect_uri[0] + 
          '&client_id=' + result.oidcClient.client_id +
          '&state=' + uuidv4() +
          '&nonce=' + uuidv4();

        let additionalParams = result.oidcClient.additionalParams

        if (additionalParams != undefined && additionalParams.trim() != '') {
          let additionalParamJSON = JSON.parse(additionalParams)
          console.log('Processing additional parameters');
          Object.keys(additionalParamJSON).forEach(key => {

            console.log(key + "~~~" + additionalParamJSON[key]);
            authzUrl += `&${key}=${additionalParamJSON[key]}`
          });
        }
        console.log('Obtained autorization URL: '+authzUrl)
        const resultUrl = await new Promise((resolve, reject) => {
          chrome.identity.launchWebAuthFlow({
            url: authzUrl,
            interactive: true
          }, callbackUrl => {
            console.log('Callback Url: ', callbackUrl)
            resolve(callbackUrl);
          });
        });

        //chrome.windows.create({url: redirectUrl,focused: true, incognito: true});

        if (resultUrl) {
          showDiv(['loadingDiv'])
          const urlParams = new URLSearchParams(new URL(resultUrl).search)
          const code = urlParams.get('code')
          console.log('code:' + code)
          const opConfig = await new Promise((resolve, reject) => {
            chrome.storage.local.get(["opConfiguration"], (result) => {
              resolve(JSON.stringify(result));
            });
          });

          const tokenReqData = qs.stringify({
            redirect_uri: redirectUrl,
            grant_type: 'authorization_code',
            client_id: result.oidcClient.client_id,
            code,
            scope: result.oidcClient.scope[0]
          })

          const tokenReqOptions = {
            method: 'POST',
            headers: { 'content-type': 'application/x-www-form-urlencoded', 'Authorization': 'Basic ' + btoa(`${result.oidcClient.client_id}:${result.oidcClient.client_secret}`) },
            data: tokenReqData,
            url: JSON.parse(opConfig).opConfiguration.token_endpoint,
          };

          const tokenResponse = await axios(tokenReqOptions);

          if (
            tokenResponse &&
            tokenResponse.data &&
            tokenResponse.data.access_token
          ) {
            console.log('tokenResponse:' + JSON.stringify(tokenResponse))
            const userInfoData = qs.stringify({
              access_token: tokenResponse.data.access_token
            })
            const userInfoOptions = {
              method: 'POST',
              headers: { 'content-type': 'application/x-www-form-urlencoded', 'Authorization': `Bearer ${tokenResponse.data.access_token}` },
              data: userInfoData,
              url: JSON.parse(opConfig).opConfiguration.userinfo_endpoint,
            };

            const userInfoResponse = await axios(userInfoOptions);
            console.log('userInfoResponse:' + JSON.stringify(userInfoResponse))

            chrome.storage.local.set({
              loginDetails: {
                'access_token': tokenResponse.data.access_token,
                'userDetails': userInfoResponse.data,
                'id_token': tokenResponse.data.id_token,
              }
            }).then(async () => {
              console.log("userDetails: " + userInfoResponse.data);
              document.getElementById('userDetailsSpan').innerHTML = JSON.stringify(userInfoResponse.data)

              showDiv(['userDetailsDiv']);
              hideDiv(['registerForm', 'oidcClientDetails']);
              hideDiv(['loadingDiv'])
            });
          }
        }
      }
    });
  }

  function logout() {
    chrome.identity.clearAllCachedAuthTokens(async () => {

      const loginDetails = await new Promise((resolve, reject) => {
        chrome.storage.local.get(["loginDetails"], (result) => {
          resolve(JSON.stringify(result));
        });
      });

      const openidConfiguration = await new Promise((resolve, reject) => { chrome.storage.local.get(["opConfiguration"], (result) => { resolve(JSON.stringify(result)); })});

      chrome.storage.local.remove(["loginDetails"], function () {
        var error = chrome.runtime.lastError;
        if (error) {
          console.error(error);
        } else {
          document.getElementById('userDetailsSpan').innerHTML = ''
          showDiv(['oidcClientDetails']);
          hideDiv(['userDetailsDiv', 'registerForm']);
          window.location.href = `${JSON.parse(openidConfiguration).opConfiguration.end_session_endpoint}?state=${uuidv4()}&post_logout_redirect_uri=${chrome.runtime.getURL('options.html')}&id_token_hint=${JSON.parse(loginDetails).loginDetails.id_token}`
          checkDB();
        }
      });
    });

  }

  async function submitForm() {
    showDiv(['loadingDiv'])
    if (validateForm()) {

      var issuer = document.getElementById('issuer').value
      var acrValues = document.getElementById('acrValues').value
      var additionalParam = document.getElementById('additionalParam').value
      var scope = document.getElementById('scope').value

      var registerObj = {}
      registerObj.issuer = issuer

      registerObj.redirect_uris =  /*[chrome.runtime.getURL('redirect.html')]*/[chrome.identity.getRedirectURL()]//[redirectUri]
      registerObj.default_acr_values = [acrValues]
      registerObj.additionalParam = additionalParam
      registerObj.scope = [scope, 'profile']
      registerObj.post_logout_redirect_uris = [chrome.runtime.getURL('options.html')]
      registerObj.response_types = ['code']
      registerObj.grant_types = ['authorization_code', 'client_credentials']
      registerObj.application_type = 'web'
      registerObj.client_name = 'Gluu-RP-' + uuidv4()
      registerObj.token_endpoint_auth_method = 'client_secret_basic'
      try {
        const response = await register(issuer, registerObj)
        if (response.result == 'success') {
          checkDB();
        } else {
          document.getElementById('errorSpanTop').innerHTML = 'Error in registration.'
          document.getElementById('errorSpanBot').innerHTML = 'Error in registration.'
          hideDiv(['loadingDiv'])
        }
      } catch (err) {
        console.error(err)
      }
    }

  }

  async function register(issuer, registerObj) {
    try {
      const openapiConfig = await getOpenidConfiguration(issuer);

      if (openapiConfig != undefined) {
        await chrome.storage.local.set({ opConfiguration: openapiConfig.data }).then(() => {
          console.log("openapiConfig is set to " + openapiConfig);
        });

        const registrationUrl = openapiConfig.data.registration_endpoint;

        const registrationResp = await registerOIDCClient(registrationUrl, registerObj);

        if (registrationResp != undefined) {

          await chrome.storage.local.set({
            oidcClient: {
              'op_host': issuer,
              'client_id': registrationResp.data.client_id,
              'client_secret': registrationResp.data.client_secret,
              'scope': registerObj.scope,
              'redirect_uri': registerObj.redirect_uris,
              'acr_values': registerObj.default_acr_values,
              'authorization_endpoint': openapiConfig.data.authorization_endpoint,
              'response_type': registerObj.response_types,
              'additionalParams': registerObj.additionalParam,
              'post_logout_redirect_uris': registerObj.post_logout_redirect_uri,

            }
          })
          console.log("oidcClient is set for client_id: " + registrationResp.data.client_id);
          console.log('OIDC client registered successfully!')
          return await { result: "success", message: "Regstration successful!" };

        } else {
          return await { result: "error", message: "Error in registration!" };
        }
      } else {
        return await { result: "error", message: "Error in registration!" };
      }
    } catch (err) {
      console.error(err)
      return { result: "error", message: "Error in registration!" };
    }
    //chrome.windows.create({url: "https://admin-ui-test.gluu.org/jans-auth/authorize.htm?scope=openid+profile+user_name+email&acr_values=basic&response_type=code&redirect_uri=https%3A%2F%2Fadmin-ui-test.gluu.org%2Fadmin&state=07adc860-5ce8-4516-a15f-65f8c5b9a882&nonce=bf92bede-c1c4-43da-9cb0-83b14ecdb467&client_id=2001.bfd15f73-96cf-4ac7-a066-32c78d516c16",focused: true, incognito: true});
  }

  async function registerOIDCClient(registration_endpoint, registerObj) {
    try {
      const registerReqOptions = {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        data: JSON.stringify(registerObj),
        url: registration_endpoint,
      };

      const response = await axios(registerReqOptions);
      return await response;
    } catch (err) {
      console.error(err)
    }

  }

  async function getOpenidConfiguration(issuer) {
    try {

      const endpoint = issuer + '/.well-known/openid-configuration';
      const oidcConfigOptions = {
        method: 'GET',
        url: endpoint,
      };
      const response = await axios(oidcConfigOptions);
      return await response;
    } catch (err) {
      console.error(err)
    }

  }

  function validateForm() {
    document.getElementById('errorSpanTop').innerHTML = ''
    document.getElementById('errorSpanBot').innerHTML = ''
    var issuer = document.getElementById('issuer').value
    var acrValues = document.getElementById('acrValues').value
    var additionalParam = document.getElementById('additionalParam').value
    var scope = document.getElementById('scope').value

    var emptyField = ''
    if (issuer.trim() == '') {
      emptyField += 'issuer '

    }

    if (acrValues.trim() == '') {
      emptyField += 'acr_values '

    }
    if (scope.trim() == '') {
      emptyField += 'scope '

    }
    if (emptyField.trim() != '') {
      document.getElementById('errorSpanTop').innerHTML = '<b>The following fields are mandatory</b>: ' + emptyField
      document.getElementById('errorSpanBot').innerHTML = '<b>The following fields are mandatory</b>: ' + emptyField
      hideDiv(['loadingDiv'])
      return false;
    }
    return true;
  }

  document.addEventListener('DOMContentLoaded', function () {
    if (document.getElementById('sbmtButton') != null) {
      document.getElementById('sbmtButton').addEventListener('click', submitForm);
    }
    if (document.getElementById('resetButton') != null) {
      document.getElementById('resetButton').addEventListener('click', resetClient);
    }
    if (document.getElementById('trigCodeFlowButton') != null) {
      document.getElementById('trigCodeFlowButton').addEventListener('click', trigCodeFlowButton);
    }
    if (document.getElementById('logoutButton') != null) {
      document.getElementById('logoutButton').addEventListener('click', logout);
    }
  });

  function isEmpty(value) {
    return (value == null || value.length === 0);
  }

  function showDiv(idArray) {
    idArray.forEach(ele => {
      if (document.getElementById(ele) != null) {
        document.getElementById(ele).style.display = "block";
      }
    });
  }

  function hideDiv(idArray) {
    idArray.forEach(ele => {
      if (document.getElementById(ele) != null) {
        document.getElementById(ele).style.display = "none";
      }
    });
  }

  function uuidv4() {
    return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
      (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
  }

})();
